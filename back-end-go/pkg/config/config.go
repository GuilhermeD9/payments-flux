package config

import "os"

// Config holds the application configuration loaded from environment variables.
type Config struct {
	MongoURI          string
	MongoDatabase     string
	RedisAddr         string
	GatewayPort       string
	WalletServicePort string
	WalletServiceURL  string
	TransferServicePort string
	TransferServiceURL  string
}

// Load reads configuration from environment variables with sensible defaults.
func Load() *Config {
	return &Config{
		MongoURI:            getEnv("MONGO_URI", "mongodb://localhost:27018"),
		MongoDatabase:       getEnv("MONGO_DATABASE", "payment_flux_test"),
		RedisAddr:           getEnv("REDIS_ADDR", "localhost:6379"),
		GatewayPort:         getEnv("GATEWAY_PORT", "8080"),
		WalletServicePort:   getEnv("WALLET_SERVICE_PORT", "8081"),
		WalletServiceURL:    getEnv("WALLET_SERVICE_URL", "http://127.0.0.1:8081"),
		TransferServicePort: getEnv("TRANSFER_SERVICE_PORT", "8082"),
		TransferServiceURL:  getEnv("TRANSFER_SERVICE_URL", "http://127.0.0.1:8082"),
	}
}

func getEnv(key, fallback string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return fallback
}
