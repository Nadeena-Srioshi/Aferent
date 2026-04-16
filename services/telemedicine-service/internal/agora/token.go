// Package agora handles Agora RTC token generation for video calls.
// Agora is the third-party video conferencing platform — similar to Twilio or Jitsi.
// Each participant needs a short-lived token to join a video channel.
package agora

import (
	"fmt"
	"strings"
	"time"

	rtctokenbuilder2 "github.com/AgoraIO-Community/go-tokenbuilder/rtctokenbuilder2"
	"github.com/google/uuid"
)

// TokenBuilder generates Agora RTC tokens for video channel access.
type TokenBuilder struct {
	AppID          string // Agora project App ID
	AppCertificate string // Agora project App Certificate
	TokenTTLSec    uint32 // how long the token is valid (seconds)
}

// BuildToken creates a signed token that lets a user join the given Agora channel.
// Returns the token string and its expiry time.
//
// If Agora credentials are not configured (empty AppID or AppCertificate),
// it returns a "dev-" prefixed fake token so the API flow can be tested
// without real Agora credentials. The video call UI will show a warning
// but the REST endpoints still work.
func (t *TokenBuilder) BuildToken(channelName, account string) (string, time.Time, error) {
	expiresAt := time.Now().UTC().Add(time.Duration(t.TokenTTLSec) * time.Second)

	// Dev fallback — allows testing the full API without Agora secrets.
	// The token starts with "dev-" so the frontend can detect this case.
	if strings.TrimSpace(t.AppID) == "" || strings.TrimSpace(t.AppCertificate) == "" {
		fallback := fmt.Sprintf("dev-%s-%d", uuid.NewString(), expiresAt.Unix())
		return fallback, expiresAt, nil
	}

	// Build a real Agora RTC token.
	// RolePublisher means the user can both send and receive audio/video.
	token, err := rtctokenbuilder2.BuildTokenWithUserAccount(
		t.AppID,
		t.AppCertificate,
		channelName,
		account,
		rtctokenbuilder2.RolePublisher,
		t.TokenTTLSec,
		t.TokenTTLSec,
	)
	if err != nil {
		return "", time.Time{}, err
	}
	return token, expiresAt, nil
}
