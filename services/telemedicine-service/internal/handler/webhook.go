package handler

import (
	"crypto/hmac"
	"crypto/sha256"
	"encoding/hex"
	"encoding/json"
	"io"
	"net/http"
	"strings"
	"time"

	"github.com/aferent/telemedicine-service/internal/middleware"
	"github.com/aferent/telemedicine-service/internal/model"
)

// HandleWebhook processes callbacks from Agora (or test clients).
// Agora sends HTTP POST requests here when something happens in a video channel:
//   - A participant joins or leaves
//   - A recording is ready for download
//
// The webhook is authenticated with HMAC-SHA256 signature verification.
// The signature is in the X-Webhook-Signature header, computed over the raw body.
//
// Endpoint: POST /sessions/webhooks
// Note: This endpoint is in the gateway's public-paths list — no JWT needed.
//
//	Security comes from the HMAC signature instead.
//
// Request body (WebhookPayload):
//
//	{
//	  "type": "participant_joined",
//	  "appointmentId": "appt-001",
//	  "userId": "doctor-001",
//	  "role": "doctor"
//	}
//
// Or for recordings:
//
//	{
//	  "type": "recording_ready",
//	  "appointmentId": "appt-001",
//	  "recordingUrl": "https://...",
//	  "objectKey": "recordings/appt-001.mp4"
//	}
func (h *Handler) HandleWebhook(w http.ResponseWriter, r *http.Request) {
	body, err := io.ReadAll(r.Body)
	if err != nil {
		http.Error(w, "invalid body", http.StatusBadRequest)
		return
	}

	// Verify HMAC signature if the webhook secret is configured
	if h.Config.AgoraWebhookSecret != "" {
		sig := strings.TrimSpace(r.Header.Get("X-Webhook-Signature"))
		if !verifyHMAC(body, h.Config.AgoraWebhookSecret, sig) {
			http.Error(w, "invalid webhook signature", http.StatusUnauthorized)
			return
		}
	}

	var payload model.WebhookPayload
	if err := json.Unmarshal(body, &payload); err != nil {
		http.Error(w, "invalid json", http.StatusBadRequest)
		return
	}
	payload.Type = strings.ToLower(strings.TrimSpace(payload.Type))
	if payload.AppointmentID == "" {
		http.Error(w, "appointmentId required", http.StatusBadRequest)
		return
	}

	ctx := r.Context()
	cid := middleware.GetCorrelationID(ctx)
	now := time.Now().UTC()

	switch payload.Type {
	// Participant left the video channel
	case "participant_left", "user-left", "participant.left":
		if err := h.Participants.MarkLeft(ctx, payload.AppointmentID, payload.UserID, strings.ToUpper(payload.Role), now); err != nil {
			http.Error(w, "failed to update participant leave", http.StatusInternalServerError)
			return
		}
		_ = h.Publisher.Emit(ctx, cid, payload.AppointmentID, "telemedicine.participant.left", map[string]interface{}{
			"userId": payload.UserID,
			"role":   strings.ToUpper(payload.Role),
			"leftAt": now.Format(time.RFC3339),
		})

	// Participant joined the video channel (via Agora webhook, not the REST join endpoint)
	case "participant_joined", "user-joined", "participant.joined":
		if err := h.Participants.Upsert(ctx, payload.AppointmentID, payload.UserID, strings.ToUpper(payload.Role), now); err != nil {
			http.Error(w, "failed to update participant join", http.StatusInternalServerError)
			return
		}
		_ = h.Publisher.Emit(ctx, cid, payload.AppointmentID, "telemedicine.participant.joined", map[string]interface{}{
			"userId": payload.UserID,
			"role":   strings.ToUpper(payload.Role),
			"joined": now.Format(time.RFC3339),
		})

	// Recording is ready — Agora has finished processing the video
	// The objectKey can be used later with MinIO to retrieve the recording.
	case "recording_ready", "recording-ready", "recording.ready":
		objKey := payload.ObjectKey
		if objKey == "" {
			objKey = payload.RecordingURL
		}
		if err := h.Sessions.UpdateRecording(ctx, payload.AppointmentID, objKey); err != nil {
			http.Error(w, "failed to update recording", http.StatusInternalServerError)
			return
		}
		_ = h.Publisher.Emit(ctx, cid, payload.AppointmentID, "telemedicine.recording.ready", map[string]interface{}{
			"recordingUrl": payload.RecordingURL,
			"objectKey":    objKey,
		})

	// Unknown webhook type — log it but don't fail
	default:
		_ = h.Publisher.Emit(ctx, cid, payload.AppointmentID, "telemedicine.webhook.unknown", map[string]interface{}{
			"type": payload.Type,
		})
	}

	writeJSON(w, map[string]string{"status": "ok"})
}

// verifyHMAC checks the HMAC-SHA256 signature of a webhook body.
// This prevents attackers from sending fake webhook events.
func verifyHMAC(body []byte, secret, providedSig string) bool {
	providedSig = strings.TrimSpace(providedSig)
	if providedSig == "" {
		return false
	}
	mac := hmac.New(sha256.New, []byte(secret))
	mac.Write(body)
	expected := hex.EncodeToString(mac.Sum(nil))
	return hmac.Equal([]byte(strings.ToLower(expected)), []byte(strings.ToLower(providedSig)))
}
