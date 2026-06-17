# Payments Flux 💳

Este é um projeto modular estruturado como monorepo, contendo a API de backend em **Spring Boot** e a interface de frontend em **Nuxt 3**.

---

## 📂 Estrutura do Projeto

O repositório está organizado nas seguintes pastas:

- **[back-end/](file:///home/gui-ubuntu/projects/java/payments-flux/back-end)**: API REST desenvolvida em Java 25 com Spring Boot.
- **[front-end/](file:///home/gui-ubuntu/projects/java/payments-flux/front-end)**: Aplicação web desenvolvida com Nuxt 3 (Vue.js).
- **[docker-compose.yaml](file:///home/gui-ubuntu/projects/java/payments-flux/docker-compose.yaml)**: Orquestração de serviços locais (MongoDB e Redis).

---

## 🚀 Como Executar o Projeto Localmente

### 1. Requisitos Prévios
Certifique-se de ter instalado em sua máquina:
- **Java 25**
- **Node.js** (v18+) e **npm**
- **Docker** e **Docker Compose**

### 2. Subir o Banco de Dados e Cache
Na raiz do projeto, inicie os serviços do MongoDB e Redis através do Docker:
```bash
docker compose up -d
```

### 3. Executar o Backend
Navegue até a pasta do backend e execute a aplicação Spring Boot:
```bash
cd back-end
./gradlew bootRun
```
*A API estará acessível em `http://localhost:8080` (ou na porta configurada).*

### 4. Executar o Frontend (Nuxt)
Navegue até a pasta do frontend, instale as dependências (se ainda não o fez) e inicie o servidor de desenvolvimento:
```bash
cd front-end
npm install
npm run dev
```
*O frontend estará acessível em `http://localhost:3000`.*

---

## 🛠️ Tecnologias Utilizadas

### Backend
- **Java 25** / **Spring Boot 4.0.1**
- **Spring Security**
- **Spring Data MongoDB** & **Redis**
- **Springdoc OpenAPI (Swagger)**
- **Mapstruct** & **Lombok**

### Frontend
- **Nuxt 3** / **Vue 3**
- **TypeScript**

---

## 📌 Próximos Passos Recomendados
- Configurar o CORS no backend para aceitar requisições de `http://localhost:3000`.
- Configurar o consumo de endpoints do backend via `useFetch` no Nuxt.
