package transfer

import "time"

// Transfer represents a transfer document in MongoDB.
// Equivalent to the Java Transfer @Document entity.
type Transfer struct {
	ID         string    `bson:"_id,omitempty" json:"id,omitempty"`
	SenderID   string    `bson:"senderId"      json:"senderId"`
	ReceiverID string    `bson:"receiverId"     json:"receiverId"`
	Amount     float64   `bson:"amount"         json:"amount"`
	CreatedAt  time.Time `bson:"createdAt"      json:"createdAt"`
}

// CreateRequest is the DTO for transfer creation.
// Equivalent to Java TransferDTO.CreateRequest record.
type CreateRequest struct {
	SenderID   string  `json:"senderId"   validate:"required"`
	ReceiverID string  `json:"receiverId" validate:"required"`
	Amount     float64 `json:"amount"     validate:"required,gt=0"`
}

// Response is the transfer response DTO sent to clients.
// Equivalent to Java TransferDTO.Response record.
type Response struct {
	ID         string    `json:"id"`
	SenderID   string    `json:"senderId"`
	ReceiverID string    `json:"receiverId"`
	Amount     float64   `json:"amount"`
	CreatedAt  time.Time `json:"createdAt"`
}

// FinancialSummaryRequest is the request DTO for financial summary queries.
// Equivalent to Java TransferDTO.FinancialSummaryRequest record.
type FinancialSummaryRequest struct {
	StartDate string `json:"startDate" validate:"required"`
	EndDate   string `json:"endDate"   validate:"required"`
}

// FinancialSummary represents an aggregated financial summary.
// Equivalent to Java TransferDTO.FinancialSummary record.
type FinancialSummary struct {
	OperationType string  `bson:"operationType" json:"operationType"`
	TotalAmount   float64 `bson:"totalAmount"   json:"totalAmount"`
	Count         int     `bson:"count"         json:"count"`
}

// PageResponse represents a paginated response.
// Equivalent to Spring Data's Page<T>.
type PageResponse struct {
	Content       []Response `json:"content"`
	TotalElements int64      `json:"totalElements"`
	TotalPages    int64      `json:"totalPages"`
	Size          int64      `json:"size"`
	Number        int64      `json:"number"`
	First         bool       `json:"first"`
	Last          bool       `json:"last"`
	Empty         bool       `json:"empty"`
}

// ToResponse converts a Transfer entity to a Response DTO.
// Replaces MapStruct's TransferMapper.toResponse().
func (t *Transfer) ToResponse() Response {
	return Response{
		ID:         t.ID,
		SenderID:   t.SenderID,
		ReceiverID: t.ReceiverID,
		Amount:     t.Amount,
		CreatedAt:  t.CreatedAt,
	}
}

// WalletInfo is the response from the Wallet Service internal endpoint.
// Used for inter-service communication.
type WalletInfo struct {
	ID      string  `json:"id"`
	Balance float64 `json:"balance"`
}
