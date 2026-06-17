# Planejamento do Front-end (Nuxt 4 + Vue 3) 🚀

Este documento apresenta o planejamento arquitetural e de telas para o front-end do **Payments Flux**, baseado na análise dos endpoints e DTOs existentes no módulo do backend.

---

## 🏗️ 1. Arquitetura Modular (Monorepo)

Para garantir escalabilidade, desacoplamento e facilidade de manutenção, adotaremos a arquitetura de **Nuxt Layers (Camadas)**. A estrutura de pastas dentro de `front-end/` será organizada da seguinte forma:

```text
front-end/
├── layers/
│   ├── core/                  # Design System, Componentes Globais e Layouts
│   │   ├── components/        # UI de uso geral (Botões, Inputs, Modais, Cards)
│   │   ├── layouts/           # Layout principal (Dashboard com Sidebar)
│   │   └── assets/css/        # Design Tokens, Variáveis e Estilos Globais
│   ├── wallets/               # Módulo de Domínio das Carteiras (Wallets)
│   │   ├── components/        # Cards de Saldo, Formulários de Depósito/Saque
│   │   └── pages/             # Listagem, Cadastro e Detalhes de Carteiras
│   └── transfers/             # Módulo de Domínio de Transferências
│       ├── components/        # Formulário de Envio, Histórico de Transações
│       └── pages/             # Nova Transferência e Dashboard Financeiro
├── nuxt.config.ts             # Configuração principal (estendendo as layers)
└── package.json
```

---

## 🎨 2. Design System & Identidade Visual

Adotaremos um visual **Moderno Premium (Glassmorphism e Dark Mode)** para passar confiabilidade e sofisticação financeira:

*   **Paleta de Cores (Aesthetics):**
    *   `Background`: Dark Coal (`#0F0F11`) e Slate Gray (`#18181C`).
    *   `Primary (Destaque)`: Emerald Green (`#10B981`) a Teal (`#14B8A6`) em gradiente.
    *   `Accent`: Electric Violet (`#8B5CF6`) para operações e transações.
    *   `Status`: Sucesso (`#10B981`), Erro/Perigo (`#EF4444`).
*   **Tipografia:** Fonte moderna como **Inter** ou **Outfit** (via Google Fonts).
*   **Efeitos Visuais:** Micro-interações em hovers, gradientes suaves nos cards de saldo e transações e bordas semitransparentes com efeitos de desfoque (backdrop blur).

---

## 📺 3. Planejamento de Telas

Com base nas APIs do Backend, estruturaremos 3 visualizações principais:

### Tela A: Dashboard Geral (Visão Consolidada)
*   **Objetivo:** Apresentar a saúde financeira geral do sistema e atalhos rápidos.
*   **Componentes baseados na API:**
    *   **Resumo Diário (`v1/api/transfer/summary`):** Gráfico interativo (de linha ou barra) mostrando o volume financeiro transacionado por dia.
    *   **Atalhos Rápidos:** Botão flutuante para realizar novas transferências, depósitos ou saques.
    *   **Feed de Últimas Transações (`v1/api/transfer`):** Lista paginada das últimas movimentações.

### Tela B: Gestão de Carteiras (Wallets)
*   **Objetivo:** Criar, editar, visualizar saldo e realizar aportes/saques nas contas.
*   **Componentes baseados na API (`v1/api/wallet`):**
    *   **Listagem de Carteiras (`GET`):** Grid de cards com efeito gradiente mostrando: Nome do titular, e-mail, CPF/CNPJ (com máscara) e Saldo.
    *   **Modal de Depósito / Saque (`POST /deposit/{id}` e `POST /withdraw/{id}`):** Formulário simples validando o `amount` para valores positivos.
    *   **Cadastro/Edição de Carteira (`POST` e `PUT /{id}`):** Formulário com campos `fullName`, `email`, `cpfCnpj` (com máscara reativa de input, limpando caracteres não-numéricos antes do envio) e `password`.
    *   **Detalhes da Carteira (`GET /{id}`):** Painel mostrando o saldo detalhado e a lista de transações em que aquela carteira participou como remetente (`/sender/{id}`) ou destinatário (`/receiver/{id}`).

### Tela C: Nova Transferência & Histórico (Transfers)
*   **Objetivo:** Movimentar saldos entre carteiras e auditar as transações.
*   **Componentes baseados na API (`v1/api/transfer`):**
    *   **Formulário de Transferência (`POST`):**
        *   *Origem (Sender)*: Dropdown com busca para selecionar a carteira pagadora.
        *   *Destino (Receiver)*: Dropdown para selecionar a carteira recebedora (não pode ser a mesma de origem).
        *   *Valor (Amount)*: Campo com máscara de dinheiro (`R$ 0,00`).
        *   *Validação no front-end:* Bloquear o envio se o valor selecionado for maior do que o saldo atual da carteira de origem.
    *   **Histórico Completo de Transações (`GET`):** Tabela paginada e filtrável contendo o ID da transação, Remetente, Destinatário, Valor e Data/Hora formatada (`createdAt`).

---

## ⚡ 4. Desafios Técnicos & Boas Práticas de Integração

1.  **CORS (Cross-Origin Resource Sharing):**
    *   Como a API está rodando com segurança pública (`anyRequest().permitAll()`), basta adicionar a configuração de CORS no Spring Boot para liberar o domínio `http://localhost:3000`.
2.  **Tratamento de Erros de Negócio:**
    *   As validações do backend (como limite de saldo ou CPFs duplicados) devem ser interceptadas no Nuxt de forma elegante utilizando `useFetch` / `$fetch` global, exibindo um componente de Toast amigável para o usuário.
3.  **Tipagem Estrita (TypeScript):**
    *   Como o backend expõe registros Java (como `WalletDTO.Response` e `TransferDTO.Response`), é altamente recomendado gerar um arquivo `.d.ts` contendo esses tipos a partir da especificação do Swagger OpenAPI (`v3/api-docs` do Spring Boot).

---

## 📝 5. Cronograma Recomendado de Implementação

*   **Fase 1 (Design Tokens & UI Base):** Configuração do CSS global, layouts principais e componentes base reutilizáveis (botões, inputs e modais).
*   **Fase 2 (Integração das Carteiras):** Implementação das telas de listagem, cadastro de carteiras e modais de depósito/saque.
*   **Fase 3 (Integração das Transferências):** Criação da tela de nova transferência com validações de saldo e a listagem de transações.
*   **Fase 4 (Dashboard & Gráficos):** Integração do resumo financeiro e montagem dos gráficos interativos.
