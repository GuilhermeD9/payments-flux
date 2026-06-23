package main

import (
	"context"
	"log"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/go-chi/chi/v5"

	"github.com/GuilhermeD9/payments-flux-go/internal/gateway"
	"github.com/GuilhermeD9/payments-flux-go/pkg/config"
	"github.com/GuilhermeD9/payments-flux-go/pkg/httputil"
)

func main() {
	cfg := config.Load()

	// Create reverse proxy
	proxy := gateway.NewProxy(cfg.WalletServiceURL, cfg.TransferServiceURL)

	// Set up router with middleware
	r := chi.NewRouter()
	r.Use(httputil.Recovery)
	r.Use(httputil.RequestLogger)
	r.Use(httputil.SecurityHeaders)
	r.Use(httputil.CORS)

	// Register proxy routes
	proxy.RegisterRoutes(r)

	// Start server on localhost only (security guideline)
	addr := "127.0.0.1:" + cfg.GatewayPort
	srv := &http.Server{
		Addr:         addr,
		Handler:      r,
		ReadTimeout:  30 * time.Second,
		WriteTimeout: 30 * time.Second,
		IdleTimeout:  120 * time.Second,
	}

	// Graceful shutdown
	go func() {
		log.Printf("API Gateway starting on %s", addr)
		log.Printf("  → Wallet Service:   %s", cfg.WalletServiceURL)
		log.Printf("  → Transfer Service: %s", cfg.TransferServiceURL)
		if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			log.Fatalf("API Gateway failed: %v", err)
		}
	}()

	quit := make(chan os.Signal, 1)
	signal.Notify(quit, syscall.SIGINT, syscall.SIGTERM)
	<-quit

	log.Println("API Gateway shutting down...")
	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()
	if err := srv.Shutdown(ctx); err != nil {
		log.Fatalf("API Gateway forced to shutdown: %v", err)
	}
	log.Println("API Gateway stopped")
}
