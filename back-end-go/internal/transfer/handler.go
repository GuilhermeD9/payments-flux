package transfer

import (
	"net/http"
	"strconv"

	"github.com/go-chi/chi/v5"
	"github.com/go-playground/validator/v10"

	"github.com/GuilhermeD9/payments-flux-go/pkg/httputil"
)

// Handler contains HTTP handlers for transfer endpoints.
// Equivalent to the Java TransferController class.
type Handler struct {
	service  *Service
	validate *validator.Validate
}

// NewHandler creates a new transfer handler.
func NewHandler(service *Service) *Handler {
	return &Handler{
		service:  service,
		validate: validator.New(),
	}
}

// RegisterRoutes registers all transfer routes on the Chi router.
func (h *Handler) RegisterRoutes(r chi.Router) {
	r.Route("/v1/api/transfer", func(r chi.Router) {
		r.Post("/", h.Create)
		r.Get("/", h.FindAll)
		r.Get("/{id}", h.FindByID)
		r.Get("/sender/{id}", h.FindBySender)
		r.Get("/receiver/{id}", h.FindByReceiver)
		r.Post("/summary", h.GetFinancialSummary)
	})
}

// Create handles POST /v1/api/transfer
func (h *Handler) Create(w http.ResponseWriter, r *http.Request) {
	var req CreateRequest
	if err := httputil.DecodeJSON(r, &req); err != nil {
		httputil.RespondError(w, r, err)
		return
	}

	if errors := h.validateRequest(req); errors != nil {
		httputil.RespondValidationErrors(w, r, errors)
		return
	}

	resp, err := h.service.Create(r.Context(), req)
	if err != nil {
		httputil.RespondError(w, r, err)
		return
	}

	httputil.RespondJSON(w, http.StatusCreated, resp)
}

// FindByID handles GET /v1/api/transfer/{id}
func (h *Handler) FindByID(w http.ResponseWriter, r *http.Request) {
	id := chi.URLParam(r, "id")

	resp, err := h.service.FindByID(r.Context(), id)
	if err != nil {
		httputil.RespondError(w, r, err)
		return
	}

	httputil.RespondJSON(w, http.StatusOK, resp)
}

// FindAll handles GET /v1/api/transfer?page=0&size=10
// Equivalent to Spring's @PageableDefault(size = 10, sort = "createdAt").
func (h *Handler) FindAll(w http.ResponseWriter, r *http.Request) {
	page := int64(0)
	size := int64(10)

	if p := r.URL.Query().Get("page"); p != "" {
		if parsed, err := strconv.ParseInt(p, 10, 64); err == nil && parsed >= 0 {
			page = parsed
		}
	}
	if s := r.URL.Query().Get("size"); s != "" {
		if parsed, err := strconv.ParseInt(s, 10, 64); err == nil && parsed > 0 {
			size = parsed
		}
	}

	resp, err := h.service.FindAll(r.Context(), page, size)
	if err != nil {
		httputil.RespondError(w, r, err)
		return
	}

	httputil.RespondJSON(w, http.StatusOK, resp)
}

// FindBySender handles GET /v1/api/transfer/sender/{id}
func (h *Handler) FindBySender(w http.ResponseWriter, r *http.Request) {
	id := chi.URLParam(r, "id")
	if id == "" {
		httputil.RespondValidationErrors(w, r, map[string]string{"id": "must not be blank"})
		return
	}

	resp, err := h.service.FindBySender(r.Context(), id)
	if err != nil {
		httputil.RespondError(w, r, err)
		return
	}

	httputil.RespondJSON(w, http.StatusOK, resp)
}

// FindByReceiver handles GET /v1/api/transfer/receiver/{id}
func (h *Handler) FindByReceiver(w http.ResponseWriter, r *http.Request) {
	id := chi.URLParam(r, "id")
	if id == "" {
		httputil.RespondValidationErrors(w, r, map[string]string{"id": "must not be blank"})
		return
	}

	resp, err := h.service.FindByReceiver(r.Context(), id)
	if err != nil {
		httputil.RespondError(w, r, err)
		return
	}

	httputil.RespondJSON(w, http.StatusOK, resp)
}

// GetFinancialSummary handles POST /v1/api/transfer/summary
func (h *Handler) GetFinancialSummary(w http.ResponseWriter, r *http.Request) {
	var req FinancialSummaryRequest
	if err := httputil.DecodeJSON(r, &req); err != nil {
		httputil.RespondError(w, r, err)
		return
	}

	if errors := h.validateRequest(req); errors != nil {
		httputil.RespondValidationErrors(w, r, errors)
		return
	}

	resp, err := h.service.GetFinancialSummary(r.Context(), req)
	if err != nil {
		httputil.RespondError(w, r, err)
		return
	}

	httputil.RespondJSON(w, http.StatusOK, resp)
}

// validateRequest runs struct validation and returns field errors if any.
func (h *Handler) validateRequest(req any) map[string]string {
	err := h.validate.Struct(req)
	if err == nil {
		return nil
	}

	errors := make(map[string]string)
	for _, e := range err.(validator.ValidationErrors) {
		field := e.Field()
		switch e.Tag() {
		case "required":
			errors[field] = "must not be blank"
		case "gt":
			errors[field] = "must be greater than " + e.Param()
		default:
			errors[field] = "invalid value"
		}
	}
	return errors
}
