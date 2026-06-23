package transfer

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"io"
	"log"
	"net/http"
	"time"

	"github.com/GuilhermeD9/payments-flux-go/pkg/apperror"
	"github.com/redis/go-redis/v9"
)

const (
	transferCachePrefix = "transfer-cache::"
	transferCacheTTL    = 5 * time.Minute
)

// Service contains the business logic for transfers.
// Equivalent to the Java TransferServiceImpl class.
type Service struct {
	repo             *Repository
	cache            *redis.Client
	walletServiceURL string
	httpClient       *http.Client
}

// NewService creates a new transfer service.
func NewService(repo *Repository, cache *redis.Client, walletServiceURL string) *Service {
	return &Service{
		repo:             repo,
		cache:            cache,
		walletServiceURL: walletServiceURL,
		httpClient: &http.Client{
			Timeout: 10 * time.Second,
		},
	}
}

// Create performs a money transfer between two wallets.
// This is the microservices version: it calls the Wallet Service via HTTP
// to debit the sender and credit the receiver.
// Equivalent to Java TransferServiceImpl.create() with @Transactional.
func (s *Service) Create(ctx context.Context, req CreateRequest) (*Response, error) {
	// Validate sender != receiver
	if req.SenderID == req.ReceiverID {
		return nil, &apperror.BusinessError{Msg: "The transferency is not be finished."}
	}

	// Verify sender exists via Wallet Service
	_, err := s.getWalletInfo(ctx, req.SenderID)
	if err != nil {
		return nil, &apperror.NotFoundError{Resource: "Wallet sender", ID: req.SenderID}
	}

	// Verify receiver exists via Wallet Service
	_, err = s.getWalletInfo(ctx, req.ReceiverID)
	if err != nil {
		return nil, &apperror.NotFoundError{Resource: "Wallet receiver", ID: req.ReceiverID}
	}

	// Debit sender via Wallet Service
	if err := s.debitWallet(ctx, req.SenderID, req.Amount); err != nil {
		return nil, err
	}

	// Credit receiver via Wallet Service
	if err := s.creditWallet(ctx, req.ReceiverID, req.Amount); err != nil {
		// TODO(security): Compensating transaction — if credit fails, we should
		// refund the sender. In a real microservices system, this would use a saga pattern.
		log.Printf("CRITICAL: credit to receiver %s failed after debiting sender %s: %v",
			req.ReceiverID, req.SenderID, err)
		// Attempt to refund sender
		if refundErr := s.creditWallet(ctx, req.SenderID, req.Amount); refundErr != nil {
			log.Printf("CRITICAL: refund to sender %s also failed: %v", req.SenderID, refundErr)
		}
		return nil, err
	}

	// Record the transfer
	transfer := &Transfer{
		SenderID:   req.SenderID,
		ReceiverID: req.ReceiverID,
		Amount:     req.Amount,
		CreatedAt:  time.Now(),
	}

	if err := s.repo.Save(ctx, transfer); err != nil {
		return nil, err
	}

	resp := transfer.ToResponse()

	// Cache the new transfer
	s.cacheTransfer(ctx, resp)

	// Evict sender/receiver list caches
	s.evictTransferListCache(ctx, req.SenderID, req.ReceiverID)

	return &resp, nil
}

// FindByID retrieves a transfer by ID with Redis caching.
// Equivalent to Java TransferServiceImpl.findById() with @Cacheable.
func (s *Service) FindByID(ctx context.Context, id string) (*Response, error) {
	// Try cache first
	cacheKey := transferCachePrefix + id
	cached, err := s.cache.Get(ctx, cacheKey).Result()
	if err == nil {
		var resp Response
		if json.Unmarshal([]byte(cached), &resp) == nil {
			return &resp, nil
		}
	}

	// Cache miss
	transfer, err := s.repo.FindByID(ctx, id)
	if err != nil {
		return nil, err
	}

	resp := transfer.ToResponse()
	s.cacheTransfer(ctx, resp)
	return &resp, nil
}

// FindAll retrieves transfers with pagination.
// Equivalent to Java TransferServiceImpl.findAll(Pageable).
func (s *Service) FindAll(ctx context.Context, page, size int64) (*PageResponse, error) {
	transfers, total, err := s.repo.FindAll(ctx, page, size)
	if err != nil {
		return nil, err
	}

	responses := make([]Response, len(transfers))
	for i, t := range transfers {
		responses[i] = t.ToResponse()
	}

	totalPages := total / size
	if total%size != 0 {
		totalPages++
	}

	return &PageResponse{
		Content:       responses,
		TotalElements: total,
		TotalPages:    totalPages,
		Size:          size,
		Number:        page,
		First:         page == 0,
		Last:          page >= totalPages-1,
		Empty:         len(responses) == 0,
	}, nil
}

// FindBySender retrieves transfers by sender ID with caching.
// Equivalent to Java TransferServiceImpl.findBySender() with @Cacheable.
func (s *Service) FindBySender(ctx context.Context, senderID string) ([]Response, error) {
	// Try cache
	cacheKey := transferCachePrefix + "sender:" + senderID
	cached, err := s.cache.Get(ctx, cacheKey).Result()
	if err == nil {
		var responses []Response
		if json.Unmarshal([]byte(cached), &responses) == nil {
			return responses, nil
		}
	}

	transfers, err := s.repo.FindBySenderID(ctx, senderID)
	if err != nil {
		return nil, err
	}

	responses := make([]Response, len(transfers))
	for i, t := range transfers {
		responses[i] = t.ToResponse()
	}

	// Cache the result
	data, _ := json.Marshal(responses)
	if cacheErr := s.cache.Set(ctx, cacheKey, data, transferCacheTTL).Err(); cacheErr != nil {
		log.Printf("WARN: failed to cache sender transfers: %v", cacheErr)
	}

	return responses, nil
}

// FindByReceiver retrieves transfers by receiver ID with caching.
// Equivalent to Java TransferServiceImpl.findByReceiver() with @Cacheable.
func (s *Service) FindByReceiver(ctx context.Context, receiverID string) ([]Response, error) {
	// Try cache
	cacheKey := transferCachePrefix + "receiver:" + receiverID
	cached, err := s.cache.Get(ctx, cacheKey).Result()
	if err == nil {
		var responses []Response
		if json.Unmarshal([]byte(cached), &responses) == nil {
			return responses, nil
		}
	}

	transfers, err := s.repo.FindByReceiverID(ctx, receiverID)
	if err != nil {
		return nil, err
	}

	responses := make([]Response, len(transfers))
	for i, t := range transfers {
		responses[i] = t.ToResponse()
	}

	// Cache the result
	data, _ := json.Marshal(responses)
	if cacheErr := s.cache.Set(ctx, cacheKey, data, transferCacheTTL).Err(); cacheErr != nil {
		log.Printf("WARN: failed to cache receiver transfers: %v", cacheErr)
	}

	return responses, nil
}

// GetFinancialSummary returns aggregated financial summary for a date range.
// Equivalent to Java TransferServiceImpl.getFinancialSummary().
func (s *Service) GetFinancialSummary(ctx context.Context, req FinancialSummaryRequest) ([]FinancialSummary, error) {
	startDate, err := time.Parse("2006-01-02", req.StartDate)
	if err != nil {
		return nil, &apperror.ValidationError{Msg: "Invalid startDate format. Use YYYY-MM-DD."}
	}
	endDate, err := time.Parse("2006-01-02", req.EndDate)
	if err != nil {
		return nil, &apperror.ValidationError{Msg: "Invalid endDate format. Use YYYY-MM-DD."}
	}

	// Set endDate to end of day
	endDate = endDate.Add(24*time.Hour - time.Nanosecond)

	return s.repo.GetFinancialSummary(ctx, startDate, endDate)
}

// --- Inter-service HTTP calls to Wallet Service ---

func (s *Service) getWalletInfo(ctx context.Context, walletID string) (*WalletInfo, error) {
	url := fmt.Sprintf("%s/internal/wallet/%s", s.walletServiceURL, walletID)

	req, err := http.NewRequestWithContext(ctx, http.MethodGet, url, nil)
	if err != nil {
		return nil, fmt.Errorf("failed to create request: %w", err)
	}

	resp, err := s.httpClient.Do(req)
	if err != nil {
		return nil, fmt.Errorf("failed to call wallet service: %w", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode == http.StatusNotFound {
		return nil, &apperror.NotFoundError{Resource: "Wallet", ID: walletID}
	}
	if resp.StatusCode != http.StatusOK {
		body, _ := io.ReadAll(resp.Body)
		return nil, fmt.Errorf("wallet service returned status %d: %s", resp.StatusCode, string(body))
	}

	var info WalletInfo
	if err := json.NewDecoder(resp.Body).Decode(&info); err != nil {
		return nil, fmt.Errorf("failed to decode wallet info: %w", err)
	}
	return &info, nil
}

func (s *Service) debitWallet(ctx context.Context, walletID string, amount float64) error {
	return s.updateWalletBalance(ctx, walletID, amount, "debit")
}

func (s *Service) creditWallet(ctx context.Context, walletID string, amount float64) error {
	return s.updateWalletBalance(ctx, walletID, amount, "credit")
}

func (s *Service) updateWalletBalance(ctx context.Context, walletID string, amount float64, operation string) error {
	url := fmt.Sprintf("%s/internal/wallet/%s/%s", s.walletServiceURL, walletID, operation)

	body := map[string]float64{"amount": amount}
	jsonBody, _ := json.Marshal(body)

	req, err := http.NewRequestWithContext(ctx, http.MethodPost, url, bytes.NewReader(jsonBody))
	if err != nil {
		return fmt.Errorf("failed to create %s request: %w", operation, err)
	}
	req.Header.Set("Content-Type", "application/json")

	resp, err := s.httpClient.Do(req)
	if err != nil {
		return fmt.Errorf("failed to call wallet service for %s: %w", operation, err)
	}
	defer resp.Body.Close()

	if resp.StatusCode == http.StatusNoContent {
		return nil
	}

	// Parse error response from wallet service
	respBody, _ := io.ReadAll(resp.Body)

	if resp.StatusCode == http.StatusNotFound {
		return &apperror.NotFoundError{Resource: "Wallet", ID: walletID}
	}
	if resp.StatusCode == http.StatusBadRequest {
		return &apperror.BusinessError{Msg: "Insufficient balance for transfer."}
	}

	return fmt.Errorf("wallet service %s returned status %d: %s", operation, resp.StatusCode, string(respBody))
}

func (s *Service) cacheTransfer(ctx context.Context, resp Response) {
	cacheKey := transferCachePrefix + resp.ID
	data, _ := json.Marshal(resp)
	if err := s.cache.Set(ctx, cacheKey, data, transferCacheTTL).Err(); err != nil {
		log.Printf("WARN: failed to cache transfer %s: %v", resp.ID, err)
	}
}

func (s *Service) evictTransferListCache(ctx context.Context, senderID, receiverID string) {
	keys := []string{
		transferCachePrefix + "sender:" + senderID,
		transferCachePrefix + "receiver:" + receiverID,
	}
	for _, key := range keys {
		if err := s.cache.Del(ctx, key).Err(); err != nil {
			log.Printf("WARN: failed to evict cache key %s: %v", key, err)
		}
	}
}
