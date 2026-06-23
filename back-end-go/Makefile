.PHONY: build run-all run-gateway run-wallet run-transfer test vet clean

# Build all services
build:
	go build -o bin/gateway ./cmd/gateway
	go build -o bin/wallet-service ./cmd/wallet-service
	go build -o bin/transfer-service ./cmd/transfer-service

# Run all services (requires Docker services running)
run-all: build
	@echo "Starting all services..."
	@./bin/wallet-service &
	@sleep 1
	@./bin/transfer-service &
	@sleep 1
	@./bin/gateway
	@echo "All services stopped."

# Run individual services
run-gateway:
	go run ./cmd/gateway

run-wallet:
	go run ./cmd/wallet-service

run-transfer:
	go run ./cmd/transfer-service

# Run tests
test:
	go test ./... -v

# Run go vet
vet:
	go vet ./...

# Clean build artifacts
clean:
	rm -rf bin/
