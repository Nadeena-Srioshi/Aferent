package storage

import (
	"context"
	"encoding/json"
	"fmt"
	"net/url"
	"path/filepath"
	"strings"
	"time"

	"document-service/internal/config"

	"github.com/google/uuid"
	"github.com/minio/minio-go/v7"
	"github.com/minio/minio-go/v7/pkg/credentials"
)

// Client wraps the MinIO SDK client and exposes only the operations this
// service needs. All callers go through this type — no raw SDK calls elsewhere.
type Client struct {
	mc  *minio.Client
	cfg *config.Config
}

// NewClient creates the MinIO client and ensures the bucket and bucket policy
// exist. It is called once at startup.
func NewClient(cfg *config.Config) (*Client, error) {
	mc, err := minio.New(cfg.MinioEndpoint, &minio.Options{
		Creds:  credentials.NewStaticV4(cfg.MinioAccessKey, cfg.MinioSecretKey, ""),
		Secure: cfg.MinioUseSSL,
	})
	if err != nil {
		return nil, fmt.Errorf("creating minio client: %w", err)
	}

	c := &Client{mc: mc, cfg: cfg}

	if err := c.ensureBucket(context.Background()); err != nil {
		return nil, err
	}

	if err := c.applyPublicReadPolicy(context.Background()); err != nil {
		return nil, err
	}

	return c, nil
}

// ensureBucket creates the bucket if it does not already exist.
func (c *Client) ensureBucket(ctx context.Context) error {
	exists, err := c.mc.BucketExists(ctx, c.cfg.MinioBucket)
	if err != nil {
		return fmt.Errorf("checking bucket existence: %w", err)
	}
	if exists {
		return nil
	}
	if err := c.mc.MakeBucket(ctx, c.cfg.MinioBucket, minio.MakeBucketOptions{}); err != nil {
		return fmt.Errorf("creating bucket %q: %w", c.cfg.MinioBucket, err)
	}
	return nil
}

// applyPublicReadPolicy sets a bucket policy that allows anonymous GetObject
// only on objects whose path contains "/public/". This is applied once at
// startup and is idempotent — safe to call every time the service restarts.
func (c *Client) applyPublicReadPolicy(ctx context.Context) error {
	policy := map[string]any{
		"Version": "2012-10-17",
		"Statement": []map[string]any{
			{
				"Effect":    "Allow",
				"Principal": "*",
				"Action":    []string{"s3:GetObject"},
				// The wildcard covers any service-id prefix: myservice/public/...
				"Resource": []string{
					fmt.Sprintf("arn:aws:s3:::%s/*/public/*", c.cfg.MinioBucket),
				},
			},
		},
	}

	policyBytes, err := json.Marshal(policy)
	if err != nil {
		return fmt.Errorf("marshalling bucket policy: %w", err)
	}

	if err := c.mc.SetBucketPolicy(ctx, c.cfg.MinioBucket, string(policyBytes)); err != nil {
		return fmt.Errorf("applying bucket policy: %w", err)
	}

	return nil
}

// BuildObjectKey constructs the namespaced path:
//
//	{serviceID}/{visibility}/{category}/{filename}
//
// If appendUUID is true a UUID is inserted before the file extension so
// concurrent uploads of the same filename do not overwrite each other.
func BuildObjectKey(serviceID, visibility, category, filename string, appendUUID bool) string {
	if appendUUID {
		ext := filepath.Ext(filename)
		name := strings.TrimSuffix(filename, ext)
		filename = fmt.Sprintf("%s_%s%s", name, uuid.NewString(), ext)
	}
	// filepath.Join would use OS separators; MinIO always uses forward slashes.
	return fmt.Sprintf("%s/%s/%s/%s", serviceID, visibility, category, filename)
}

// PresignPut returns a presigned HTTP PUT URL the client can use to upload
// directly to MinIO. The URL expires in 15 minutes.
func (c *Client) PresignPut(ctx context.Context, objectKey string) (string, error) {
	raw, err := c.PresignPutInternal(ctx, objectKey)
	if err != nil {
		return "", err
	}

	return c.toPublicURL(raw), nil
}

// PresignPutInternal returns the raw presigned HTTP PUT URL using MINIO_ENDPOINT
// host/scheme without rewriting to MINIO_PUBLIC_HOST.
func (c *Client) PresignPutInternal(ctx context.Context, objectKey string) (string, error) {
	u, err := c.mc.PresignedPutObject(ctx, c.cfg.MinioBucket, objectKey, 15*time.Minute)
	if err != nil {
		return "", fmt.Errorf("generating presigned PUT URL: %w", err)
	}

	return u.String(), nil
}

// toPublicURL rewrites the generated URL host/scheme to MINIO_PUBLIC_HOST
// so browsers can access it from outside Docker.
func (c *Client) toPublicURL(raw string) string {
	if strings.TrimSpace(c.cfg.MinioPublicHost) == "" {
		return raw
	}

	signedURL, err := url.Parse(raw)
	if err != nil {
		return raw
	}

	publicHost := strings.TrimSpace(c.cfg.MinioPublicHost)
	if !strings.Contains(publicHost, "://") {
		publicHost = "http://" + publicHost
	}

	publicURL, err := url.Parse(publicHost)
	if err != nil || publicURL.Host == "" {
		return raw
	}

	signedURL.Scheme = publicURL.Scheme
	signedURL.Host = publicURL.Host
	return signedURL.String()
}

// PermanentURL builds the non-expiring public URL for a public object.
// It works because the bucket policy already grants anonymous read.
func (c *Client) PermanentURL(objectKey string) string {
	// Trim any trailing slash from the host to avoid double slashes.
	host := strings.TrimRight(c.cfg.MinioPublicHost, "/")
	return fmt.Sprintf("%s/%s/%s", host, c.cfg.MinioBucket, objectKey)
}

// PresignGet returns a time-limited presigned HTTP GET URL for a private object.
func (c *Client) PresignGet(ctx context.Context, objectKey string, ttl time.Duration) (string, error) {
	reqParams := make(url.Values)
	u, err := c.mc.PresignedGetObject(ctx, c.cfg.MinioBucket, objectKey, ttl, reqParams)
	if err != nil {
		return "", fmt.Errorf("generating presigned GET URL: %w", err)
	}

	return c.toPublicURL(u.String()), nil
}
