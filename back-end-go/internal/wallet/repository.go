package wallet

import (
	"context"
	"errors"
	"fmt"

	"github.com/GuilhermeD9/payments-flux-go/pkg/apperror"
	"go.mongodb.org/mongo-driver/v2/bson"
	"go.mongodb.org/mongo-driver/v2/mongo"
	"go.mongodb.org/mongo-driver/v2/mongo/options"
)

// Repository handles MongoDB operations for wallets.
// Equivalent to the Java WalletRepository interface extending MongoRepository.
type Repository struct {
	collection *mongo.Collection
}

// NewRepository creates a new wallet repository.
func NewRepository(db *mongo.Database) *Repository {
	coll := db.Collection("wallets")

	// Create unique indexes (equivalent to @Indexed(unique = true) in Java)
	indexModels := []mongo.IndexModel{
		{
			Keys:    bson.D{{Key: "cpfCnpj", Value: 1}},
			Options: options.Index().SetUnique(true),
		},
		{
			Keys:    bson.D{{Key: "email", Value: 1}},
			Options: options.Index().SetUnique(true),
		},
	}
	_, err := coll.Indexes().CreateMany(context.Background(), indexModels)
	if err != nil {
		// Log but don't crash — indexes may already exist
		fmt.Printf("WARN: failed to create wallet indexes: %v\n", err)
	}

	return &Repository{collection: coll}
}

// Save inserts a new wallet document.
func (r *Repository) Save(ctx context.Context, w *Wallet) error {
	w.Version = 1
	result, err := r.collection.InsertOne(ctx, w)
	if err != nil {
		if mongo.IsDuplicateKeyError(err) {
			return &apperror.ConflictError{Msg: "Wallet with this CPF/CNPJ or email already exists."}
		}
		return fmt.Errorf("failed to insert wallet: %w", err)
	}
	if oid, ok := result.InsertedID.(bson.ObjectID); ok {
		w.ID = oid.Hex()
	}
	return nil
}

// FindByID retrieves a wallet by its ID.
func (r *Repository) FindByID(ctx context.Context, id string) (*Wallet, error) {
	oid, err := bson.ObjectIDFromHex(id)
	if err != nil {
		return nil, &apperror.NotFoundError{Resource: "Wallet", ID: id}
	}

	var w Wallet
	err = r.collection.FindOne(ctx, bson.M{"_id": oid}).Decode(&w)
	if err != nil {
		if errors.Is(err, mongo.ErrNoDocuments) {
			return nil, &apperror.NotFoundError{Resource: "Wallet", ID: id}
		}
		return nil, fmt.Errorf("failed to find wallet: %w", err)
	}
	return &w, nil
}

// FindAll retrieves all wallets.
func (r *Repository) FindAll(ctx context.Context) ([]Wallet, error) {
	cursor, err := r.collection.Find(ctx, bson.M{})
	if err != nil {
		return nil, fmt.Errorf("failed to find wallets: %w", err)
	}

	var wallets []Wallet
	if err = cursor.All(ctx, &wallets); err != nil {
		return nil, fmt.Errorf("failed to decode wallets: %w", err)
	}

	if wallets == nil {
		wallets = []Wallet{}
	}
	return wallets, nil
}

// FindBalanceByID retrieves only the balance field for a wallet (projection).
// Equivalent to Java's @Query with fields = "{ 'balance' : 1, '_id' : 0 }".
func (r *Repository) FindBalanceByID(ctx context.Context, id string) (*BalanceProjection, error) {
	oid, err := bson.ObjectIDFromHex(id)
	if err != nil {
		return nil, &apperror.NotFoundError{Resource: "Wallet", ID: id}
	}

	opts := options.FindOne().SetProjection(bson.M{"balance": 1, "_id": 0})
	var bp BalanceProjection
	err = r.collection.FindOne(ctx, bson.M{"_id": oid}, opts).Decode(&bp)
	if err != nil {
		if errors.Is(err, mongo.ErrNoDocuments) {
			return nil, &apperror.NotFoundError{Resource: "Wallet", ID: id}
		}
		return nil, fmt.Errorf("failed to find balance: %w", err)
	}
	return &bp, nil
}

// Update saves an existing wallet with optimistic locking.
// It uses the version field to detect concurrent modifications
// (equivalent to Spring Data's @Version annotation).
func (r *Repository) Update(ctx context.Context, w *Wallet) error {
	oid, err := bson.ObjectIDFromHex(w.ID)
	if err != nil {
		return &apperror.NotFoundError{Resource: "Wallet", ID: w.ID}
	}

	currentVersion := w.Version
	w.Version++

	filter := bson.M{
		"_id":     oid,
		"version": currentVersion,
	}
	update := bson.M{
		"$set": bson.M{
			"fullName": w.FullName,
			"cpfCnpj":  w.CpfCnpj,
			"email":    w.Email,
			"password": w.Password,
			"balance":  w.Balance,
			"version":  w.Version,
		},
	}

	result, err := r.collection.UpdateOne(ctx, filter, update)
	if err != nil {
		if mongo.IsDuplicateKeyError(err) {
			return &apperror.ConflictError{Msg: "Wallet with this CPF/CNPJ or email already exists."}
		}
		return fmt.Errorf("failed to update wallet: %w", err)
	}
	if result.MatchedCount == 0 {
		return &apperror.ConflictError{Msg: "Concurrent modification detected. Please retry the operation."}
	}
	return nil
}

// ExistsById checks if a wallet exists by ID.
func (r *Repository) ExistsByID(ctx context.Context, id string) (bool, error) {
	oid, err := bson.ObjectIDFromHex(id)
	if err != nil {
		return false, nil
	}
	count, err := r.collection.CountDocuments(ctx, bson.M{"_id": oid})
	if err != nil {
		return false, fmt.Errorf("failed to check wallet existence: %w", err)
	}
	return count > 0, nil
}

// DeleteByID removes a wallet by ID.
func (r *Repository) DeleteByID(ctx context.Context, id string) error {
	oid, err := bson.ObjectIDFromHex(id)
	if err != nil {
		return &apperror.NotFoundError{Resource: "Wallet", ID: id}
	}

	result, err := r.collection.DeleteOne(ctx, bson.M{"_id": oid})
	if err != nil {
		return fmt.Errorf("failed to delete wallet: %w", err)
	}
	if result.DeletedCount == 0 {
		return &apperror.NotFoundError{Resource: "Wallet", ID: id}
	}
	return nil
}
