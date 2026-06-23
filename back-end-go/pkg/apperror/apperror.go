package apperror

import (
	"fmt"
	"net/http"
	"time"
)

// AppError is the base application error with an associated HTTP status code.
type AppError struct {
	Status  int    `json:"status"`
	Error   string `json:"error"`
	Message string `json:"message"`
}

// ErrorResponse is the standardized error response sent to clients.
// It mirrors the Java GlobalExceptionHandler.ErrorResponse record.
type ErrorResponse struct {
	Status    int       `json:"status"`
	Error     string    `json:"error"`
	Message   string    `json:"message"`
	Timestamp time.Time `json:"timestamp"`
	Path      string    `json:"path"`
}

// ValidationErrorResponse is sent when field-level validation fails.
// It mirrors the Java GlobalExceptionHandler.ValidationErrorResponse record.
type ValidationErrorResponse struct {
	Status    int               `json:"status"`
	Error     string            `json:"error"`
	Errors    map[string]string `json:"errors"`
	Timestamp time.Time         `json:"timestamp"`
	Path      string            `json:"path"`
}

// NotFoundError represents a resource that was not found (HTTP 404).
type NotFoundError struct {
	Resource string
	ID       string
}

func (e *NotFoundError) Error() string {
	return fmt.Sprintf("%s with id %s not found", e.Resource, e.ID)
}

func (e *NotFoundError) StatusCode() int {
	return http.StatusNotFound
}

func (e *NotFoundError) ErrorType() string {
	return "Resource Not Found"
}

// BusinessError represents a business rule violation (HTTP 400).
type BusinessError struct {
	Msg string
}

func (e *BusinessError) Error() string {
	return e.Msg
}

func (e *BusinessError) StatusCode() int {
	return http.StatusBadRequest
}

func (e *BusinessError) ErrorType() string {
	return "Business Rule Violation"
}

// ValidationError represents a validation failure (HTTP 400).
type ValidationError struct {
	Msg string
}

func (e *ValidationError) Error() string {
	return e.Msg
}

func (e *ValidationError) StatusCode() int {
	return http.StatusBadRequest
}

func (e *ValidationError) ErrorType() string {
	return "Validation Error"
}

// ConflictError represents a concurrency/conflict error (HTTP 409).
type ConflictError struct {
	Msg string
}

func (e *ConflictError) Error() string {
	return e.Msg
}

func (e *ConflictError) StatusCode() int {
	return http.StatusConflict
}

func (e *ConflictError) ErrorType() string {
	return "Conflict Error"
}

// StatusCoder is implemented by errors that carry an HTTP status code.
type StatusCoder interface {
	error
	StatusCode() int
	ErrorType() string
}

// NewErrorResponse builds a standardized ErrorResponse from a StatusCoder error.
func NewErrorResponse(err StatusCoder, path string) ErrorResponse {
	return ErrorResponse{
		Status:    err.StatusCode(),
		Error:     err.ErrorType(),
		Message:   err.Error(),
		Timestamp: time.Now(),
		Path:      path,
	}
}

// NewValidationErrorResponse builds a ValidationErrorResponse from field errors.
func NewValidationErrorResponse(errors map[string]string, path string) ValidationErrorResponse {
	return ValidationErrorResponse{
		Status:    http.StatusBadRequest,
		Error:     "Validation Failed",
		Errors:    errors,
		Timestamp: time.Now(),
		Path:      path,
	}
}
