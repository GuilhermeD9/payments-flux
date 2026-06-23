package httputil

import (
	"encoding/json"
	"log"
	"net/http"
	"time"

	"github.com/GuilhermeD9/payments-flux-go/pkg/apperror"
)

// RespondJSON writes a JSON response with the given status code and payload.
func RespondJSON(w http.ResponseWriter, status int, payload any) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	if payload != nil {
		if err := json.NewEncoder(w).Encode(payload); err != nil {
			log.Printf("ERROR: failed to encode response: %v", err)
		}
	}
}

// RespondError handles errors by checking their type and responding appropriately.
// This is the Go equivalent of Spring's @RestControllerAdvice GlobalExceptionHandler.
func RespondError(w http.ResponseWriter, r *http.Request, err error) {
	path := r.URL.Path

	if appErr, ok := err.(apperror.StatusCoder); ok {
		resp := apperror.NewErrorResponse(appErr, path)
		RespondJSON(w, appErr.StatusCode(), resp)
		return
	}

	// Generic internal server error — don't expose internal details to client
	log.Printf("ERROR: unhandled error at %s: %v", path, err)
	resp := apperror.ErrorResponse{
		Status:    http.StatusInternalServerError,
		Error:     "Internal Server Error",
		Message:   "An unexpected error occurred.",
		Timestamp: time.Now(),
		Path:      path,
	}
	RespondJSON(w, http.StatusInternalServerError, resp)
}

// RespondValidationErrors sends a structured validation error response.
func RespondValidationErrors(w http.ResponseWriter, r *http.Request, errors map[string]string) {
	resp := apperror.NewValidationErrorResponse(errors, r.URL.Path)
	RespondJSON(w, http.StatusBadRequest, resp)
}

// DecodeJSON reads and decodes a JSON request body into the target struct.
func DecodeJSON(r *http.Request, target any) error {
	decoder := json.NewDecoder(r.Body)
	decoder.DisallowUnknownFields()
	return decoder.Decode(target)
}

// Recovery is a middleware that recovers from panics and returns a 500 error.
func Recovery(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		defer func() {
			if rec := recover(); rec != nil {
				log.Printf("PANIC recovered: %v", rec)
				resp := apperror.ErrorResponse{
					Status:    http.StatusInternalServerError,
					Error:     "Internal Server Error",
					Message:   "An unexpected error occurred.",
					Timestamp: time.Now(),
					Path:      r.URL.Path,
				}
				RespondJSON(w, http.StatusInternalServerError, resp)
			}
		}()
		next.ServeHTTP(w, r)
	})
}

// RequestLogger logs each incoming request with method, path, and duration.
func RequestLogger(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		start := time.Now()
		next.ServeHTTP(w, r)
		log.Printf("%s %s %s", r.Method, r.URL.Path, time.Since(start))
	})
}

// SecurityHeaders adds security-related HTTP headers to every response.
func SecurityHeaders(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("X-Content-Type-Options", "nosniff")
		w.Header().Set("X-Frame-Options", "DENY")
		w.Header().Set("X-XSS-Protection", "1; mode=block")
		w.Header().Set("Content-Security-Policy", "default-src 'self'")
		w.Header().Set("Permissions-Policy", "camera=(), microphone=(), geolocation=()")
		next.ServeHTTP(w, r)
	})
}

// CORS middleware allows cross-origin requests from the Nuxt frontend.
func CORS(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Access-Control-Allow-Origin", "http://localhost:3000")
		w.Header().Set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
		w.Header().Set("Access-Control-Allow-Headers", "Content-Type, Authorization")
		w.Header().Set("Access-Control-Max-Age", "86400")

		if r.Method == http.MethodOptions {
			w.WriteHeader(http.StatusNoContent)
			return
		}
		next.ServeHTTP(w, r)
	})
}
