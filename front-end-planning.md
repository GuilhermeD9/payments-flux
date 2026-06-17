# Planejamento do Front-end (Nuxt 4 + Vue 3) 🚀

Este é o roadmap passo a passo para a construção do front-end do **Payments Flux**. Como o projeto utiliza a arquitetura monorepo e Nuxt modular (com Nuxt Layers), seguiremos uma ordem lógica de construção, do básico ao avançado.

---

## 🛠️ Fase 1: Estrutura Base e Layout Global (Módulo Core)

Nesta primeira fase, criaremos a fundação do nosso projeto com a estrutura de camadas (Layers) e o layout básico do painel (Dashboard).

### Passo 1.1: Criar as pastas do Monorepo
No diretório `front-end/`, crie as pastas das três camadas que planejamos:
- `layers/core/` (Estilos globais, Layout principal, Componentes de UI básicos)
- `layers/wallets/` (Páginas e componentes do fluxo de Carteiras)
- `layers/transfers/` (Páginas e componentes do fluxo de Transferências)

Comando para criar as pastas de uma vez no terminal:
```bash
mkdir -p layers/core/components layers/core/layouts layers/wallets/pages/wallets layers/wallets/components layers/transfers/pages/transfers layers/transfers/components
```

### Passo 1.2: Configurar o `nuxt.config.ts` principal
Atualize o arquivo [front-end/nuxt.config.ts](file:///home/gui-ubuntu/projects/java/payments-flux/front-end/nuxt.config.ts) para estender (herdar) as configurações dessas camadas.
```typescript
// front-end/nuxt.config.ts
export default defineNuxtConfig({
  compatibilityDate: '2025-07-15',
  devtools: { enabled: true },
  
  // Ativa a arquitetura de camadas (Nuxt Layers)
  extends: [
    './layers/core',
    './layers/wallets',
    './layers/transfers'
  ],

  css: ['~/assets/css/tailwind.css'],
  modules: ['@nuxtjs/tailwindcss']
})
```

### Passo 1.3: Inicializar o arquivo `nuxt.config.ts` de cada camada
Cada camada precisa ter seu próprio arquivo de configuração (mesmo que vazio) para o Nuxt identificá-la.
Crie os arquivos:
1. `layers/core/nuxt.config.ts`
2. `layers/wallets/nuxt.config.ts`
3. `layers/transfers/nuxt.config.ts`

Conteúdo padrão para cada um deles:
```typescript
export default defineNuxtConfig({})
```

### Passo 1.4: Criar o Layout da Dashboard com Sidebar
Vamos criar um layout bonito com fundo escuro e uma barra lateral (Sidebar) para navegação.
Crie o arquivo `layers/core/layouts/default.vue` com o código abaixo:
```vue
<template>
  <div class="min-h-screen bg-coal text-gray-100 flex">
    <!-- Sidebar -->
    <aside class="w-64 bg-slate-gray border-r border-white/5 flex flex-col justify-between p-6">
      <div>
        <div class="flex items-center gap-3 mb-8">
          <span class="text-2xl">💳</span>
          <h1 class="text-xl font-bold bg-gradient-to-r from-emerald-400 to-teal-400 bg-clip-text text-transparent">
            PaymentsFlux
          </h1>
        </div>
        
        <nav class="space-y-2">
          <NuxtLink to="/" class="flex items-center gap-3 px-4 py-3 rounded-lg hover:bg-white/5 transition">
            <span>📊</span> Dashboard
          </NuxtLink>
          <NuxtLink to="/wallets" class="flex items-center gap-3 px-4 py-3 rounded-lg hover:bg-white/5 transition">
            <span>💼</span> Carteiras
          </NuxtLink>
          <NuxtLink to="/transfers" class="flex items-center gap-3 px-4 py-3 rounded-lg hover:bg-white/5 transition">
            <span>💸</span> Transferências
          </NuxtLink>
        </nav>
      </div>
      
      <div class="text-xs text-gray-500">
        v1.0.0 (Monorepo)
      </div>
    </aside>

    <!-- Main Content Area -->
    <main class="flex-1 p-8 overflow-y-auto">
      <slot />
    </main>
  </div>
</template>
```

---

## 💼 Fase 2: Gestão de Carteiras (Módulo Wallets)

Nesta fase, implementaremos o CRUD de Carteiras, além dos modais para depósito e saque de dinheiro.

### Passo 2.1: Criar o Composable de Conexão com a API (`useWallets.ts`)
Vamos centralizar as requisições de carteira em um composable do Nuxt (um arquivo utilitário de estado).
Crie o arquivo `layers/wallets/composables/useWallets.ts`:
```typescript
export const useWallets = () => {
  const config = useRuntimeConfig()
  const apiBase = 'http://localhost:8080/v1/api/wallet' // URL do seu Spring Boot

  const fetchWallets = async () => {
    return await $fetch<any[]>(apiBase)
  }

  const fetchWalletById = async (id: string) => {
    return await $fetch<any>(`${apiBase}/${id}`)
  }

  const createWallet = async (walletData: any) => {
    return await $fetch(apiBase, {
      method: 'POST',
      body: walletData
    })
  }

  const depositMoney = async (id: string, amount: number) => {
    return await $fetch(`${apiBase}/deposit/${id}`, {
      method: 'POST',
      body: { amount }
    })
  }

  const withdrawMoney = async (id: string, amount: number) => {
    return await $fetch(`${apiBase}/withdraw/${id}`, {
      method: 'POST',
      body: { amount }
    })
  }

  return {
    fetchWallets,
    fetchWalletById,
    createWallet,
    depositMoney,
    withdrawMoney
  }
}
```

### Passo 2.2: Criar a Página de Listagem de Carteiras
Crie a tela onde todas as carteiras serão exibidas em um grid moderno.
Crie o arquivo `layers/wallets/pages/wallets/index.vue`:
*   *Função:* Exibir as contas, saldos e botões rápidos para depositar/sacar.

### Passo 2.3: Criar a Tela de Criação de Carteira
Crie o formulário de cadastro de carteiras.
Crie o arquivo `layers/wallets/pages/wallets/create.vue`:
*   *Função:* Formulário com validação de CPF/CNPJ e envio de dados para o Spring Boot.

---

## 💸 Fase 3: Módulo de Transferências (Módulo Transfers)

Nesta fase, permitiremos enviar dinheiro entre contas e listar as transferências efetuadas.

### Passo 3.1: Criar o Composable de Transferências (`useTransfers.ts`)
Crie o arquivo `layers/transfers/composables/useTransfers.ts` com as chamadas de API:
*   `POST /v1/api/transfer` (Criar transferência)
*   `GET /v1/api/transfer` (Histórico paginado)
*   `POST /v1/api/transfer/summary` (Resumo financeiro diário)

### Passo 3.2: Criar a Tela de Envio de Transferência
Crie o formulário em `layers/transfers/pages/transfers/new.vue`:
*   *Função:* Escolher remetente (Sender), destinatário (Receiver), valor e efetivar a transação. O front-end deve checar se a carteira de origem tem saldo suficiente antes de enviar.

### Passo 3.3: Criar a Tela de Histórico de Transações
Crie o histórico em `layers/transfers/pages/transfers/index.vue`:
*   *Função:* Tabela moderna com todas as transferências salvas no MongoDB do backend.

---

## 📊 Fase 4: Dashboard e Gráficos

### Passo 4.1: Criar a Home do Painel (`pages/index.vue`)
Crie a página principal de entrada (`pages/index.vue`) na raiz do frontend ou na camada core:
*   Exibir o saldo total consolidado de todas as carteiras.
*   Exibir atalhos visuais (botões de ação rápida).
*   Mostrar um resumo diário dos valores movimentados usando uma biblioteca gráfica como `Chart.js` ou `ApexCharts`.

---

## 🚀 Como vamos trabalhar a partir de agora?
Faremos **um passo de cada vez**. 
1. Começaremos com a **Fase 1** (Criar pastas e configurar o layout global).
2. Assim que terminarmos e testarmos, avançaremos para a **Fase 2** (Módulo de carteiras).
