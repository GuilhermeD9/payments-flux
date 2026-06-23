# Payments Flux — Back-end Go 🐹

Recriação do back-end [Java/Spring Boot](../back-end/) em **Go** com arquitetura de **microserviços**, para fins comparativos.

---

## 📐 Arquitetura

A aplicação é dividida em **3 serviços**:

```
┌─────────────────────┐
│   API Gateway       │  :8080  ← Front-end Nuxt se conecta aqui
│  (Reverse Proxy)    │
└────────┬────────────┘
         │
    ┌────┴────┐
    │         │
    ▼         ▼
┌────────┐ ┌──────────┐
│ Wallet │ │ Transfer │
│ Service│ │ Service  │
│ :8081  │ │  :8082   │
└───┬────┘ └────┬─────┘
    │           │
    ▼           ▼
┌──────────────────────┐
│  MongoDB + Redis     │  (Compartilhados)
│  :27018    :6379     │
└──────────────────────┘
```

**Comunicação inter-serviço**: O Transfer Service chama o Wallet Service via HTTP para debitar/creditar wallets durante transferências.

---

## 🚀 Como Executar

### Pré-requisitos
- **Go 1.22+**
- **Docker & Docker Compose** (para MongoDB e Redis)

### 1. Subir dependências
```bash
# Na raiz do projeto (payments-flux/)
docker compose up -d
```

### 2. Executar os serviços (3 terminais)

```bash
# Terminal 1 — Wallet Service
cd back-end-go
go run ./cmd/wallet-service

# Terminal 2 — Transfer Service
cd back-end-go
go run ./cmd/transfer-service

# Terminal 3 — API Gateway
cd back-end-go
go run ./cmd/gateway
```

Ou com o **Makefile** (build + run sequencial):
```bash
cd back-end-go
make build
make run-all
```

A API estará acessível em `http://localhost:8080`.

---

## 📡 Endpoints

Todos os endpoints são idênticos ao back-end Java:

### Wallet
| Método | Endpoint | Descrição |
|--------|----------|-----------|
| `POST` | `/v1/api/wallet` | Criar wallet |
| `GET` | `/v1/api/wallet` | Listar wallets |
| `GET` | `/v1/api/wallet/{id}` | Buscar por ID |
| `GET` | `/v1/api/wallet/balance/{id}` | Consultar saldo |
| `PUT` | `/v1/api/wallet/{id}` | Atualizar wallet |
| `DELETE` | `/v1/api/wallet/{id}` | Deletar wallet |
| `POST` | `/v1/api/wallet/deposit/{id}` | Depositar |
| `POST` | `/v1/api/wallet/withdraw/{id}` | Sacar |

### Transfer
| Método | Endpoint | Descrição |
|--------|----------|-----------|
| `POST` | `/v1/api/transfer` | Criar transferência |
| `GET` | `/v1/api/transfer` | Listar (paginado) |
| `GET` | `/v1/api/transfer/{id}` | Buscar por ID |
| `GET` | `/v1/api/transfer/sender/{id}` | Buscar por sender |
| `GET` | `/v1/api/transfer/receiver/{id}` | Buscar por receiver |
| `POST` | `/v1/api/transfer/summary` | Resumo financeiro |

---

## 🔄 Comparação Java vs Go

### Estrutura do Projeto

| Aspecto | Java / Spring Boot | Go |
|---------|-------------------|----|
| **Arquitetura** | Monolito (1 app) | Microserviços (3 binários) |
| **Framework HTTP** | Spring Web MVC | Chi Router + net/http |
| **MongoDB** | Spring Data MongoDB | mongo-driver oficial |
| **Cache Redis** | Spring Cache + annotations | go-redis (manual) |
| **Validação** | Bean Validation + anotações | go-playground/validator |
| **Mapeamento DTO** | MapStruct (code generation) | Funções manuais |
| **Exceptions** | Hierarquia de classes + @ExceptionHandler | Interface errors + middleware |
| **Segurança** | Spring Security | Middleware customizado |
| **Hash senha** | BCryptPasswordEncoder | golang.org/x/crypto/bcrypt |
| **Build** | Gradle | go build |
| **LOC** | ~650 (sem contar gerados) | ~1400 (mais explícito) |

### Filosofia

| | Java | Go |
|-|------|----|
| **Estilo** | Convention over configuration | Explícito é melhor que implícito |
| **Magia** | Annotations, AOP, proxies | Sem magia — tudo é código visível |
| **DI** | Spring IoC Container | Construtor manual (wire by hand) |
| **Erros** | Exceptions (try/catch) | Valores de retorno (if err != nil) |
| **Concorrência** | Threads (virtual threads) | Goroutines + channels |
| **Deploy** | JVM + Fat JAR (~100MB) | Binário estático (~15MB) |
| **Startup** | ~3-5s | ~50ms |

---

## 🛠 Tecnologias

- **Go 1.22**
- **Chi v5** — Router HTTP leve e idiomático
- **MongoDB Driver v2** — Driver oficial MongoDB para Go
- **go-redis v9** — Cliente Redis para Go
- **go-playground/validator v10** — Validação de structs
- **x/crypto/bcrypt** — Hash de senhas

---

## 📂 Estrutura de Pastas

```
back-end-go/
├── cmd/
│   ├── gateway/main.go          # API Gateway (proxy reverso)
│   ├── wallet-service/main.go   # Wallet Service
│   └── transfer-service/main.go # Transfer Service
├── internal/
│   ├── gateway/proxy.go         # Lógica do proxy
│   ├── wallet/
│   │   ├── model.go             # Entity + DTOs
│   │   ├── repository.go        # Acesso ao MongoDB
│   │   ├── service.go           # Lógica de negócio
│   │   └── handler.go           # HTTP handlers (controller)
│   └── transfer/
│       ├── model.go
│       ├── repository.go
│       ├── service.go
│       └── handler.go
├── pkg/
│   ├── apperror/apperror.go     # Tipos de erro customizados
│   ├── config/config.go         # Configuração via env vars
│   ├── httputil/httputil.go     # Middleware e helpers HTTP
│   └── validator/cpfcnpj.go    # Validação CPF/CNPJ
├── go.mod
├── go.sum
├── Makefile
└── README.md
```
