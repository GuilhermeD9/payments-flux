package wallet

import (
	"net/http"

	"github.com/go-chi/chi/v5"
	"github.com/go-playground/validator/v10"

	"github.com/GuilhermeD9/payments-flux-go/pkg/httputil"
	pkgvalidator "github.com/GuilhermeD9/payments-flux-go/pkg/validator"
)

// Handler contains HTTP handlers for wallet endpoints.
// Equivalent to the Java WalletController class.
type Handler struct {
	service  *Service
	validate *validator.Validate
}

// NewHandler creates a new wallet handler.
func NewHandler(service *Service) *Handler {
	v := validator.New()

	// Register custom CPF/CNPJ validator (equivalent to @CPFCNPJ annotation in Java)
	_ = v.RegisterValidation("cpfcnpj", func(fl validator.FieldLevel) bool {
		return pkgvalidator.IsValidCPFCNPJ(fl.Field().String())
	})

	return &Handler{service: service, validate: v}
}

// RegisterRoutes registers all wallet routes on the Chi router.
func (h *Handler) RegisterRoutes(r chi.Router) {
	r.Route("/v1/api/wallet", func(r chi.Router) {
		r.Post("/", h.Create)
		r.Get("/", h.FindAll)
		r.Get("/{id}", h.FindByID)
		r.Get("/balance/{id}", h.GetBalance)
		r.Put("/{id}", h.Update)
		r.Delete("/{id}", h.Delete)
		r.Post("/deposit/{id}", h.Deposit)
		r.Post("/withdraw/{id}", h.Withdraw)
	})

	// Internal endpoints for inter-service communication (Transfer Service → Wallet Service)
	r.Route("/internal/wallet", func(r chi.Router) {
		r.Get("/{id}", h.GetWalletInfo)
		r.Post("/{id}/debit", h.DebitBalance)
		r.Post("/{id}/credit", h.CreditBalance)
	})
}

// Create handles POST /v1/api/wallet
func (h *Handler) Create(w http.ResponseWriter, r *http.Request) {
	var req CreateRequest
	if err := httputil.DecodeJSON(r, &req); err != nil {
		httputil.RespondError(w, r, err)
		return
	}

	// Normalize CPF/CNPJ (strip non-digits, same as Java's compact constructor)
	req.CpfCnpj = pkgvalidator.StripNonDigits(req.CpfCnpj)

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

// FindByID handles GET /v1/api/wallet/{id}
func (h *Handler) FindByID(w http.ResponseWriter, r *http.Request) {
	id := chi.URLParam(r, "id")

	resp, err := h.service.FindByID(r.Context(), id)
	if err != nil {
		httputil.RespondError(w, r, err)
		return
	}

	httputil.RespondJSON(w, http.StatusOK, resp)
}

// GetBalance handles GET /v1/api/wallet/balance/{id}
func (h *Handler) GetBalance(w http.ResponseWriter, r *http.Request) {
	id := chi.URLParam(r, "id")

	balance, err := h.service.GetBalance(r.Context(), id)
	if err != nil {
		httputil.RespondError(w, r, err)
		return
	}

	httputil.RespondJSON(w, http.StatusOK, balance)
}

// FindAll handles GET /v1/api/wallet
func (h *Handler) FindAll(w http.ResponseWriter, r *http.Request) {
	responses, err := h.service.FindAll(r.Context())
	if err != nil {
		httputil.RespondError(w, r, err)
		return
	}

	httputil.RespondJSON(w, http.StatusOK, responses)
}

// Update handles PUT /v1/api/wallet/{id}
func (h *Handler) Update(w http.ResponseWriter, r *http.Request) {
	id := chi.URLParam(r, "id")

	var req UpdateRequest
	if err := httputil.DecodeJSON(r, &req); err != nil {
		httputil.RespondError(w, r, err)
		return
	}

	// Normalize CPF/CNPJ
	req.CpfCnpj = pkgvalidator.StripNonDigits(req.CpfCnpj)

	if errors := h.validateRequest(req); errors != nil {
		httputil.RespondValidationErrors(w, r, errors)
		return
	}

	resp, err := h.service.Update(r.Context(), id, req)
	if err != nil {
		httputil.RespondError(w, r, err)
		return
	}

	httputil.RespondJSON(w, http.StatusOK, resp)
}

// Delete handles DELETE /v1/api/wallet/{id}
func (h *Handler) Delete(w http.ResponseWriter, r *http.Request) {
	id := chi.URLParam(r, "id")

	if err := h.service.Delete(r.Context(), id); err != nil {
		httputil.RespondError(w, r, err)
		return
	}

	w.WriteHeader(http.StatusNoContent)
}

// Deposit handles POST /v1/api/wallet/deposit/{id}
func (h *Handler) Deposit(w http.ResponseWriter, r *http.Request) {
	id := chi.URLParam(r, "id")

	var req MoneyRequest
	if err := httputil.DecodeJSON(r, &req); err != nil {
		httputil.RespondError(w, r, err)
		return
	}

	if errors := h.validateRequest(req); errors != nil {
		httputil.RespondValidationErrors(w, r, errors)
		return
	}

	resp, err := h.service.Deposit(r.Context(), id, req)
	if err != nil {
		httputil.RespondError(w, r, err)
		return
	}

	httputil.RespondJSON(w, http.StatusOK, resp)
}

// Withdraw handles POST /v1/api/wallet/withdraw/{id}
func (h *Handler) Withdraw(w http.ResponseWriter, r *http.Request) {
	id := chi.URLParam(r, "id")

	var req MoneyRequest
	if err := httputil.DecodeJSON(r, &req); err != nil {
		httputil.RespondError(w, r, err)
		return
	}

	if errors := h.validateRequest(req); errors != nil {
		httputil.RespondValidationErrors(w, r, errors)
		return
	}

	resp, err := h.service.Withdraw(r.Context(), id, req)
	if err != nil {
		httputil.RespondError(w, r, err)
		return
	}

	httputil.RespondJSON(w, http.StatusOK, resp)
}

// GetWalletInfo handles GET /internal/wallet/{id} (inter-service)
func (h *Handler) GetWalletInfo(w http.ResponseWriter, r *http.Request) {
	id := chi.URLParam(r, "id")

	info, err := h.service.GetWalletInfo(r.Context(), id)
	if err != nil {
		httputil.RespondError(w, r, err)
		return
	}

	httputil.RespondJSON(w, http.StatusOK, info)
}

// DebitBalance handles POST /internal/wallet/{id}/debit (inter-service)
func (h *Handler) DebitBalance(w http.ResponseWriter, r *http.Request) {
	id := chi.URLParam(r, "id")

	var req InternalBalanceUpdateRequest
	if err := httputil.DecodeJSON(r, &req); err != nil {
		httputil.RespondError(w, r, err)
		return
	}

	if err := h.service.DebitBalance(r.Context(), id, req.Amount); err != nil {
		httputil.RespondError(w, r, err)
		return
	}

	w.WriteHeader(http.StatusNoContent)
}

// CreditBalance handles POST /internal/wallet/{id}/credit (inter-service)
func (h *Handler) CreditBalance(w http.ResponseWriter, r *http.Request) {
	id := chi.URLParam(r, "id")

	var req InternalBalanceUpdateRequest
	if err := httputil.DecodeJSON(r, &req); err != nil {
		httputil.RespondError(w, r, err)
		return
	}

	if err := h.service.CreditBalance(r.Context(), id, req.Amount); err != nil {
		httputil.RespondError(w, r, err)
		return
	}

	w.WriteHeader(http.StatusNoContent)
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
		case "email":
			errors[field] = "must be a well-formed email address"
		case "min":
			errors[field] = "size must be at least " + e.Param()
		case "max":
			errors[field] = "size must be at most " + e.Param()
		case "gt":
			errors[field] = "must be greater than " + e.Param()
		case "cpfcnpj":
			errors[field] = "Invalid CPF or CNPJ"
		default:
			errors[field] = "invalid value"
		}
	}
	return errors
}
