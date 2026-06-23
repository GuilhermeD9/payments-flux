package wallet

import (
	"context"
	"encoding/json"
	"fmt"
	"log"
	"time"

	"github.com/GuilhermeD9/payments-flux-go/pkg/apperror"
	"github.com/redis/go-redis/v9"
	"golang.org/x/crypto/bcrypt"
)

const (
	balanceCachePrefix   = "balance-cache::"
	balanceCacheTTL      = 10 * time.Minute
)

// Service contains the business logic for wallets.
// Equivalent to the Java WalletServiceImpl class.
type Service struct {
	repo  *Repository
	cache *redis.Client
}

// NewService creates a new wallet service.
func NewService(repo *Repository, cache *redis.Client) *Service {
	return &Service{repo: repo, cache: cache}
}

// Create creates a new wallet with a hashed password and zero balance.
// Equivalent to Java WalletServiceImpl.create().
func (s *Service) Create(ctx context.Context, req CreateRequest) (*Response, error) {
	wallet := req.ToEntity()

	// Hash password with bcrypt (same as Spring Security's BCryptPasswordEncoder)
	hashedPassword, err := bcrypt.GenerateFromPassword([]byte(req.Password), bcrypt.DefaultCost)
	if err != nil {
		return nil, fmt.Errorf("failed to hash password: %w", err)
	}
	wallet.Password = string(hashedPassword)
	wallet.Balance = 0

	if err := s.repo.Save(ctx, &wallet); err != nil {
		return nil, err
	}

	resp := wallet.ToResponse()
	return &resp, nil
}

// FindByID retrieves a wallet by ID.
// Equivalent to Java WalletServiceImpl.findById().
func (s *Service) FindByID(ctx context.Context, id string) (*Response, error) {
	wallet, err := s.repo.FindByID(ctx, id)
	if err != nil {
		return nil, err
	}
	resp := wallet.ToResponse()
	return &resp, nil
}

// FindAll retrieves all wallets.
// Equivalent to Java WalletServiceImpl.findAll().
func (s *Service) FindAll(ctx context.Context) ([]Response, error) {
	wallets, err := s.repo.FindAll(ctx)
	if err != nil {
		return nil, err
	}
	responses := make([]Response, len(wallets))
	for i, w := range wallets {
		responses[i] = w.ToResponse()
	}
	return responses, nil
}

// GetBalance retrieves the balance for a wallet, with Redis caching.
// Equivalent to Java WalletServiceImpl.getBalance() with @Cacheable.
func (s *Service) GetBalance(ctx context.Context, id string) (float64, error) {
	// Try cache first
	cacheKey := balanceCachePrefix + id
	cached, err := s.cache.Get(ctx, cacheKey).Result()
	if err == nil {
		var balance float64
		if json.Unmarshal([]byte(cached), &balance) == nil {
			return balance, nil
		}
	}

	// Cache miss — fetch from DB
	projection, err := s.repo.FindBalanceByID(ctx, id)
	if err != nil {
		return 0, err
	}

	// Store in cache
	data, _ := json.Marshal(projection.Balance)
	if cacheErr := s.cache.Set(ctx, cacheKey, data, balanceCacheTTL).Err(); cacheErr != nil {
		log.Printf("WARN: failed to cache balance for %s: %v", id, cacheErr)
	}

	return projection.Balance, nil
}

// Update modifies an existing wallet.
// Equivalent to Java WalletServiceImpl.update().
func (s *Service) Update(ctx context.Context, id string, req UpdateRequest) (*Response, error) {
	wallet, err := s.repo.FindByID(ctx, id)
	if err != nil {
		return nil, err
	}

	req.ApplyTo(wallet)

	// Hash the new password
	hashedPassword, err := bcrypt.GenerateFromPassword([]byte(req.Password), bcrypt.DefaultCost)
	if err != nil {
		return nil, fmt.Errorf("failed to hash password: %w", err)
	}
	wallet.Password = string(hashedPassword)

	if err := s.repo.Update(ctx, wallet); err != nil {
		return nil, err
	}

	resp := wallet.ToResponse()
	return &resp, nil
}

// Delete removes a wallet by ID and evicts its balance cache.
// Equivalent to Java WalletServiceImpl.delete() with @CacheEvict.
func (s *Service) Delete(ctx context.Context, id string) error {
	exists, err := s.repo.ExistsByID(ctx, id)
	if err != nil {
		return err
	}
	if !exists {
		return &apperror.NotFoundError{Resource: "Wallet", ID: id}
	}

	if err := s.repo.DeleteByID(ctx, id); err != nil {
		return err
	}

	// Evict balance cache
	s.evictBalanceCache(ctx, id)
	return nil
}

// Deposit adds amount to a wallet's balance and evicts balance cache.
// Equivalent to Java WalletServiceImpl.deposit() with @CacheEvict.
func (s *Service) Deposit(ctx context.Context, id string, req MoneyRequest) (*Response, error) {
	wallet, err := s.repo.FindByID(ctx, id)
	if err != nil {
		return nil, err
	}

	wallet.Balance += req.Amount

	if err := s.repo.Update(ctx, wallet); err != nil {
		return nil, err
	}

	s.evictBalanceCache(ctx, id)

	resp := wallet.ToResponse()
	return &resp, nil
}

// Withdraw subtracts amount from a wallet's balance (with insufficient balance check).
// Equivalent to Java WalletServiceImpl.withdraw() with @CacheEvict.
func (s *Service) Withdraw(ctx context.Context, id string, req MoneyRequest) (*Response, error) {
	wallet, err := s.repo.FindByID(ctx, id)
	if err != nil {
		return nil, err
	}

	if wallet.Balance < req.Amount {
		return nil, &apperror.BusinessError{Msg: "Insufficient balance for transfer."}
	}

	wallet.Balance -= req.Amount

	if err := s.repo.Update(ctx, wallet); err != nil {
		return nil, err
	}

	s.evictBalanceCache(ctx, id)

	resp := wallet.ToResponse()
	return &resp, nil
}

// GetWalletInfo retrieves lightweight wallet info for inter-service communication.
func (s *Service) GetWalletInfo(ctx context.Context, id string) (*InternalWalletInfo, error) {
	wallet, err := s.repo.FindByID(ctx, id)
	if err != nil {
		return nil, err
	}
	return &InternalWalletInfo{
		ID:      wallet.ID,
		Balance: wallet.Balance,
	}, nil
}

// DebitBalance subtracts from balance (used by Transfer Service via HTTP).
func (s *Service) DebitBalance(ctx context.Context, id string, amount float64) error {
	wallet, err := s.repo.FindByID(ctx, id)
	if err != nil {
		return err
	}

	if wallet.Balance < amount {
		return &apperror.BusinessError{Msg: "Insufficient balance for transfer."}
	}

	wallet.Balance -= amount
	if err := s.repo.Update(ctx, wallet); err != nil {
		return err
	}

	s.evictBalanceCache(ctx, id)
	return nil
}

// CreditBalance adds to balance (used by Transfer Service via HTTP).
func (s *Service) CreditBalance(ctx context.Context, id string, amount float64) error {
	wallet, err := s.repo.FindByID(ctx, id)
	if err != nil {
		return err
	}

	wallet.Balance += amount
	if err := s.repo.Update(ctx, wallet); err != nil {
		return err
	}

	s.evictBalanceCache(ctx, id)
	return nil
}

func (s *Service) evictBalanceCache(ctx context.Context, id string) {
	cacheKey := balanceCachePrefix + id
	if err := s.cache.Del(ctx, cacheKey).Err(); err != nil {
		log.Printf("WARN: failed to evict balance cache for %s: %v", id, err)
	}
}
