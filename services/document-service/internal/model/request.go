package model

// PresignUploadRequest is the body the caller sends to POST /upload/presign.
type PresignUploadRequest struct {
	// Visibility must be "public" or "private".
	Visibility string `json:"visibility"`

	// Category is a logical grouping chosen by the caller, e.g. "avatars", "invoices".
	Category string `json:"category"`

	// Filename is the original filename, e.g. "profile.png".
	Filename string `json:"filename"`

	// AppendUUID — when true, a UUID is appended before the file extension
	// to guarantee uniqueness. The caller opts in per request.
	AppendUUID bool `json:"append_uuid"`
}

// PresignUploadResponse is returned for both public and private uploads.
// Fields that don't apply to a given visibility are omitted (omitempty).
type PresignUploadResponse struct {
	// UploadURL is the presigned PUT URL the client should upload the file to directly.
	UploadURL string `json:"upload_url"`

	// ObjectKey is the full MinIO object path. Returned for private files so
	// the calling service can store it and request a signed GET URL later.
	ObjectKey string `json:"object_key,omitempty"`

	// PermanentURL is only set for public files. It is a stable, non-expiring URL
	// because the bucket policy grants anonymous read on */public/*.
	PermanentURL string `json:"permanent_url,omitempty"`
}

// SignedURLRequest is the query-string model for GET /sign?key=...&expires=3600
type SignedURLRequest struct {
	ObjectKey string `json:"key"`
	// ExpiresIn is the TTL of the presigned GET URL in seconds. Defaults to 3600.
	ExpiresIn int `json:"expires"`
}

// SignedURLResponse wraps the presigned GET URL.
type SignedURLResponse struct {
	URL string `json:"url"`
}
