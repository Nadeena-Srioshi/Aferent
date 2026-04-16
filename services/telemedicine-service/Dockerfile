FROM golang:1.26-alpine AS build
WORKDIR /src

COPY go.mod ./
RUN go mod download

COPY . .
RUN CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go build -o /out/telemedicine-service ./cmd/server

FROM alpine:3.21
WORKDIR /app
RUN adduser -D appuser
COPY --from=build /out/telemedicine-service /app/telemedicine-service
COPY --from=build /src/web /app/web
USER appuser
EXPOSE 3006
ENTRYPOINT ["/app/telemedicine-service"]
