package wallet

import "time"

// Wallet represents a wallet document in MongoDB.
// Equivalent to the Java Wallet @Document entity.
type Wallet struct {
	ID       string  `bson:"_id,omitempty" json:"id,omitempty"`
	FullName string  `bson:"fullName"      json:"fullName"`
	CpfCnpj string  `bson:"cpfCnpj"       json:"cpfCnpj"`
	Email    string  `bson:"email"         json:"email"`
	Password string  `bson:"password"      json:"-"` // json:"-" is equivalent to @JsonIgnore
	Balance  float64 `bson:"balance"       json:"balance"`
	Version  int64   `bson:"version"       json:"-"` // Used for optimistic locking
}

// CreateRequest is the DTO for wallet creation.
// Equivalent to Java WalletDTO.CreateRequest record.
type CreateRequest struct {
	FullName string `json:"fullName" validate:"required"`
	CpfCnpj string `json:"cpfCnpj"  validate:"required,cpfcnpj"`
	Email    string `json:"email"    validate:"required,email,max=40"`
	Password string `json:"password" validate:"required,min=6"`
}

// UpdateRequest is the DTO for wallet updates.
// Equivalent to Java WalletDTO.UpdateRequest record.
type UpdateRequest struct {
	FullName string `json:"fullName" validate:"required"`
	CpfCnpj string `json:"cpfCnpj"  validate:"required,cpfcnpj"`
	Email    string `json:"email"    validate:"required,email,max=120"`
	Password string `json:"password" validate:"required,min=6"`
}

// MoneyRequest is the DTO for deposit/withdraw operations.
// Equivalent to Java WalletDTO.MoneyRequest record.
type MoneyRequest struct {
	Amount float64 `json:"amount" validate:"required,gt=0"`
}

// Response is the wallet response DTO sent to clients.
// Equivalent to Java WalletDTO.Response record.
type Response struct {
	ID       string  `json:"id"`
	FullName string  `json:"fullName"`
	CpfCnpj string  `json:"cpfCnpj"`
	Email    string  `json:"email"`
	Balance  float64 `json:"balance"`
}

// InternalBalanceUpdateRequest is used for inter-service communication.
// The Transfer Service calls the Wallet Service to debit/credit wallets.
type InternalBalanceUpdateRequest struct {
	Amount float64 `json:"amount" validate:"required"`
}

// InternalWalletInfo is a lightweight DTO returned for inter-service wallet lookups.
type InternalWalletInfo struct {
	ID      string  `json:"id"`
	Balance float64 `json:"balance"`
}

// BalanceProjection represents a balance-only projection from MongoDB.
// Equivalent to Java WalletBalanceProjection record.
type BalanceProjection struct {
	Balance float64 `bson:"balance" json:"balance"`
}

// ToResponse converts a Wallet entity to a Response DTO.
// This replaces MapStruct's WalletMapper.toResponse() in Java.
func (w *Wallet) ToResponse() Response {
	return Response{
		ID:       w.ID,
		FullName: w.FullName,
		CpfCnpj: w.CpfCnpj,
		Email:    w.Email,
		Balance:  w.Balance,
	}
}

// ToEntity converts a CreateRequest DTO to a Wallet entity.
// This replaces MapStruct's WalletMapper.toEntity() in Java.
func (r *CreateRequest) ToEntity() Wallet {
	return Wallet{
		FullName: r.FullName,
		CpfCnpj: r.CpfCnpj,
		Email:    r.Email,
	}
}

// ApplyTo updates a Wallet entity from an UpdateRequest DTO.
// This replaces MapStruct's WalletMapper.updateEntity() in Java.
func (r *UpdateRequest) ApplyTo(w *Wallet) {
	w.FullName = r.FullName
	w.CpfCnpj = r.CpfCnpj
	w.Email = r.Email
}

// Transfer represents a transfer document in MongoDB.
// Defined here for the financial summary aggregation that may be needed.
type Transfer struct {
	ID         string    `bson:"_id,omitempty" json:"id,omitempty"`
	SenderID   string    `bson:"senderId"      json:"senderId"`
	ReceiverID string    `bson:"receiverId"     json:"receiverId"`
	Amount     float64   `bson:"amount"         json:"amount"`
	CreatedAt  time.Time `bson:"createdAt"      json:"createdAt"`
}
