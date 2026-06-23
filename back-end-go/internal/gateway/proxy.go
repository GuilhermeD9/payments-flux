package gateway

import (
	"log"
	"net/http"
	"net/http/httputil"
	"net/url"

	"github.com/go-chi/chi/v5"
)

// Proxy is a reverse proxy that routes requests to the appropriate microservice.
// This is the Go equivalent of an API Gateway — it sits on port 8080 and forwards
// requests to the Wallet Service (8081) and Transfer Service (8082).
type Proxy struct {
	walletProxy   *httputil.ReverseProxy
	transferProxy *httputil.ReverseProxy
}

// NewProxy creates a new API Gateway proxy.
func NewProxy(walletServiceURL, transferServiceURL string) *Proxy {
	walletURL, err := url.Parse(walletServiceURL)
	if err != nil {
		log.Fatalf("Invalid wallet service URL: %v", err)
	}

	transferURL, err := url.Parse(transferServiceURL)
	if err != nil {
		log.Fatalf("Invalid transfer service URL: %v", err)
	}

	return &Proxy{
		walletProxy:   httputil.NewSingleHostReverseProxy(walletURL),
		transferProxy: httputil.NewSingleHostReverseProxy(transferURL),
	}
}

// RegisterRoutes registers the proxy routes on the Chi router.
func (p *Proxy) RegisterRoutes(r chi.Router) {
	// Route wallet requests to Wallet Service
	r.HandleFunc("/v1/api/wallet", p.proxyToWallet)
	r.HandleFunc("/v1/api/wallet/*", p.proxyToWallet)

	// Route transfer requests to Transfer Service
	r.HandleFunc("/v1/api/transfer", p.proxyToTransfer)
	r.HandleFunc("/v1/api/transfer/*", p.proxyToTransfer)

	// Health check endpoint
	r.Get("/health", func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusOK)
		_, _ = w.Write([]byte(`{"status":"UP"}`))
	})
}

func (p *Proxy) proxyToWallet(w http.ResponseWriter, r *http.Request) {
	log.Printf("Gateway → Wallet Service: %s %s", r.Method, r.URL.Path)
	p.walletProxy.ServeHTTP(w, r)
}

func (p *Proxy) proxyToTransfer(w http.ResponseWriter, r *http.Request) {
	log.Printf("Gateway → Transfer Service: %s %s", r.Method, r.URL.Path)
	p.transferProxy.ServeHTTP(w, r)
}
