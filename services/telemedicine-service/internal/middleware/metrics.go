package middleware

import (
	"net/http"
	"strconv"
	"time"

	"github.com/go-chi/chi/v5"
	"github.com/prometheus/client_golang/prometheus"
)

// Metrics is a middleware that records HTTP request count and latency.
// The data is exposed at /actuator/prometheus for Prometheus to scrape.
// In Grafana, you can build dashboards showing request rates, error rates, and latency.
func Metrics(reqCtr *prometheus.CounterVec, latH *prometheus.HistogramVec) func(http.Handler) http.Handler {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			start := time.Now()

			// Wrap the ResponseWriter so we can capture the status code
			rw := &statusWriter{ResponseWriter: w, status: http.StatusOK}
			next.ServeHTTP(rw, r)

			// Use the chi route pattern (e.g. "/sessions/{appointmentId}/status")
			// instead of the actual URL (e.g. "/sessions/appt-001/status")
			// so metrics are grouped by endpoint, not by individual appointment.
			path := routePattern(r)
			reqCtr.WithLabelValues(r.Method, path, strconv.Itoa(rw.status)).Inc()
			latH.WithLabelValues(r.Method, path).Observe(time.Since(start).Seconds())
		})
	}
}

// statusWriter wraps http.ResponseWriter to capture the HTTP status code.
type statusWriter struct {
	http.ResponseWriter
	status int
}

// WriteHeader captures the status code before passing it through.
func (w *statusWriter) WriteHeader(code int) {
	w.status = code
	w.ResponseWriter.WriteHeader(code)
}

// routePattern extracts the registered route pattern from chi's context.
// For example, a request to /sessions/appt-001/status returns "/sessions/{appointmentId}/status".
func routePattern(r *http.Request) string {
	if rc := chi.RouteContext(r.Context()); rc != nil {
		return rc.RoutePattern()
	}
	return r.URL.Path
}
