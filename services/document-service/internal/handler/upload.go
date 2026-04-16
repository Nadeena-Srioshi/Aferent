package handler

import (
	"encoding/json"
	"log"
	"net/http"
	"strings"

	"document-service/internal/middleware"
	"document-service/internal/model"
	"document-service/internal/storage"
)

// PresignUpload handles POST /upload/presign
//
// The caller sends a JSON body describing the file. The handler constructs
// the namespaced object key and returns a presigned PUT URL. For public
// files it also returns the permanent URL. For private files it returns
// the raw object key the caller must store.
type PresignUpload struct {
	store *storage.Client
}

func NewPresignUpload(store *storage.Client) *PresignUpload {
	return &PresignUpload{store: store}
}

func (h *PresignUpload) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		writeJSON(w, http.StatusMethodNotAllowed, errorBody("method not allowed"))
		return
	}

	var req model.PresignUploadRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		writeJSON(w, http.StatusBadRequest, errorBody("invalid JSON body"))
		return
	}

	// --- Validate inputs ---
	req.Visibility = strings.ToLower(strings.TrimSpace(req.Visibility))
	if req.Visibility != "public" && req.Visibility != "private" {
		writeJSON(w, http.StatusBadRequest, errorBody(`visibility must be "public" or "private"`))
		return
	}
	if strings.TrimSpace(req.Category) == "" {
		writeJSON(w, http.StatusBadRequest, errorBody("category is required"))
		return
	}
	if strings.TrimSpace(req.Filename) == "" {
		writeJSON(w, http.StatusBadRequest, errorBody("filename is required"))
		return
	}

	serviceID := middleware.ServiceIDFromContext(r.Context())

	objectKey := storage.BuildObjectKey(serviceID, req.Visibility, req.Category, req.Filename, req.AppendUUID)

	uploadURL, err := h.store.PresignPut(r.Context(), objectKey)
	if err != nil {
		log.Printf("PresignPut error: %v", err)
		writeJSON(w, http.StatusInternalServerError, errorBody("could not generate upload URL"))
		return
	}

	internalUploadURL, err := h.store.PresignPutInternal(r.Context(), objectKey)
	if err != nil {
		log.Printf("PresignPutInternal error: %v", err)
		writeJSON(w, http.StatusInternalServerError, errorBody("could not generate upload URL"))
		return
	}

	resp := model.PresignUploadResponse{
		UploadURL:         uploadURL,
		InternalUploadURL: internalUploadURL,
	}

	switch req.Visibility {
	case "public":
		resp.PermanentURL = h.store.PermanentURL(objectKey)
	case "private":
		resp.ObjectKey = objectKey
	}

	writeJSON(w, http.StatusOK, resp)
}
