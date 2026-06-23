package transfer

import (
	"context"
	"errors"
	"fmt"
	"time"

	"github.com/GuilhermeD9/payments-flux-go/pkg/apperror"
	"go.mongodb.org/mongo-driver/v2/bson"
	"go.mongodb.org/mongo-driver/v2/mongo"
	"go.mongodb.org/mongo-driver/v2/mongo/options"
)

// Repository handles MongoDB operations for transfers.
// Equivalent to the Java TransferRepository interface.
type Repository struct {
	collection *mongo.Collection
}

// NewRepository creates a new transfer repository.
func NewRepository(db *mongo.Database) *Repository {
	return &Repository{collection: db.Collection("transfers")}
}

// Save inserts a new transfer document.
func (r *Repository) Save(ctx context.Context, t *Transfer) error {
	result, err := r.collection.InsertOne(ctx, t)
	if err != nil {
		return fmt.Errorf("failed to insert transfer: %w", err)
	}
	if oid, ok := result.InsertedID.(bson.ObjectID); ok {
		t.ID = oid.Hex()
	}
	return nil
}

// FindByID retrieves a transfer by its ID.
func (r *Repository) FindByID(ctx context.Context, id string) (*Transfer, error) {
	oid, err := bson.ObjectIDFromHex(id)
	if err != nil {
		return nil, &apperror.NotFoundError{Resource: "Transfer", ID: id}
	}

	var t Transfer
	err = r.collection.FindOne(ctx, bson.M{"_id": oid}).Decode(&t)
	if err != nil {
		if errors.Is(err, mongo.ErrNoDocuments) {
			return nil, &apperror.NotFoundError{Resource: "Transfer", ID: id}
		}
		return nil, fmt.Errorf("failed to find transfer: %w", err)
	}
	return &t, nil
}

// FindAll retrieves transfers with pagination.
// Equivalent to Spring Data's findAll(Pageable).
func (r *Repository) FindAll(ctx context.Context, page, size int64) ([]Transfer, int64, error) {
	// Count total documents
	total, err := r.collection.CountDocuments(ctx, bson.M{})
	if err != nil {
		return nil, 0, fmt.Errorf("failed to count transfers: %w", err)
	}

	// Sort by createdAt descending (equivalent to @PageableDefault(sort = "createdAt"))
	opts := options.Find().
		SetSort(bson.D{{Key: "createdAt", Value: -1}}).
		SetSkip(page * size).
		SetLimit(size)

	cursor, err := r.collection.Find(ctx, bson.M{}, opts)
	if err != nil {
		return nil, 0, fmt.Errorf("failed to find transfers: %w", err)
	}

	var transfers []Transfer
	if err = cursor.All(ctx, &transfers); err != nil {
		return nil, 0, fmt.Errorf("failed to decode transfers: %w", err)
	}

	if transfers == nil {
		transfers = []Transfer{}
	}
	return transfers, total, nil
}

// FindBySenderID retrieves all transfers by sender ID.
// Equivalent to Java's @Query("{ 'senderId' : ?0 }").
func (r *Repository) FindBySenderID(ctx context.Context, senderID string) ([]Transfer, error) {
	cursor, err := r.collection.Find(ctx, bson.M{"senderId": senderID})
	if err != nil {
		return nil, fmt.Errorf("failed to find transfers by sender: %w", err)
	}

	var transfers []Transfer
	if err = cursor.All(ctx, &transfers); err != nil {
		return nil, fmt.Errorf("failed to decode transfers: %w", err)
	}

	if transfers == nil {
		transfers = []Transfer{}
	}
	return transfers, nil
}

// FindByReceiverID retrieves all transfers by receiver ID.
// Equivalent to Java's @Query("{ 'receiverId' : ?0 }").
func (r *Repository) FindByReceiverID(ctx context.Context, receiverID string) ([]Transfer, error) {
	cursor, err := r.collection.Find(ctx, bson.M{"receiverId": receiverID})
	if err != nil {
		return nil, fmt.Errorf("failed to find transfers by receiver: %w", err)
	}

	var transfers []Transfer
	if err = cursor.All(ctx, &transfers); err != nil {
		return nil, fmt.Errorf("failed to decode transfers: %w", err)
	}

	if transfers == nil {
		transfers = []Transfer{}
	}
	return transfers, nil
}

// GetFinancialSummary runs the MongoDB aggregation pipeline for financial summary.
// Equivalent to the Java @Aggregation annotation on TransferRepository.
func (r *Repository) GetFinancialSummary(ctx context.Context, startDate, endDate time.Time) ([]FinancialSummary, error) {
	pipeline := bson.A{
		bson.M{
			"$match": bson.M{
				"createdAt": bson.M{
					"$gte": startDate,
					"$lte": endDate,
				},
				"type": bson.M{
					"$in": bson.A{"TRANSFER", "DEPOSIT", "WITHDRAW"},
				},
			},
		},
		bson.M{
			"$group": bson.M{
				"_id":         "$type",
				"totalAmount": bson.M{"$sum": "$amount"},
				"count":       bson.M{"$sum": 1},
			},
		},
		bson.M{
			"$project": bson.M{
				"_id":           0,
				"operationType": "$_id",
				"totalAmount":   "$totalAmount",
				"count":         "$count",
			},
		},
	}

	cursor, err := r.collection.Aggregate(ctx, pipeline)
	if err != nil {
		return nil, fmt.Errorf("failed to run financial summary aggregation: %w", err)
	}

	var summaries []FinancialSummary
	if err = cursor.All(ctx, &summaries); err != nil {
		return nil, fmt.Errorf("failed to decode financial summary: %w", err)
	}

	if summaries == nil {
		summaries = []FinancialSummary{}
	}
	return summaries, nil
}
