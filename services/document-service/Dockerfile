# --- Build stage ---
# Updated from 1.22 to 1.26 to match your go.mod requirements
FROM golang:1.26-alpine AS builder

WORKDIR /app

# Copy dependency manifests first so Docker caches this layer
COPY go.mod go.sum ./
RUN go mod download

COPY . .

RUN CGO_ENABLED=0 GOOS=linux go build -o /document-service ./cmd/server

# --- Runtime stage ---
FROM gcr.io/distroless/static-debian12

WORKDIR /

COPY --from=builder /document-service /document-service

EXPOSE 3009

ENTRYPOINT ["/document-service"]
