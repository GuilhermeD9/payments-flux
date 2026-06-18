<template>
  <div class="p-8">
    <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-6 mb-8">
      <div>
        <h1 class="text-3xl font-bold">Carteiras</h1>
        <p class="text-sm text-gray-400 mt-2">
          Veja suas contas e saldos disponíveis
        </p>
      </div>

      <NuxtLink
        to="/wallets/create"
        class="inline-flex items-center px-4 py-2 bg-emerald-500 hover:bg-emerald-600 text-white rounded-lg">
        + Criar Nova Carteira
      </NuxtLink>
    </div>

    <div v-if="wallets.length === 0" class="text-center text-gray-400 py-12">
      Nenhuma carteira criada. Crie uma nova para começar!
    </div>

    <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
      <div
        v-for="wallet in wallets"
        :key="wallet.id"
        class="bg-slate-800 p-6 rounded-2xl border border-white/10 shadow-lg hover:shadow-2xl transition">
        <div class="mb-4">
          <p class="text-sm text-gray-400">Carteira</p>
          <h2 class="text-xl font-semibold">{{ wallet.name }}</h2>
        </div>

        <p class="text-sm text-gray-400">Saldo</p>
        <p class="text-2xl font-semibold mb-6">
          {{ formatter.format(wallet.balance ?? 0) }}
        </p>

        <div class="flex flex-wrap gap-3">
          <NuxtLink
            :to="`/wallets/${wallet.id}`"
            class="text-sm text-emerald-400 hover:underline"
          >
            Ver detalhes
          </NuxtLink>

          <button
            type="button"
            class="rounded-full bg-white/10 px-4 py-2 text-white hover:bg-white/20"
          >
            Depositar
          </button>
          <button
            type="button"
            class="rounded-full bg-white/10 px-4 py-2 text-white hover:bg-white/20"
          >
            Sacar
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import  { useWallets } from '../composables/useWallets'

const wallets = ref<any[]>([])
const isLoading = ref(true)
const error = ref<string | null>(null)

const formatter = new Intl.NumberFormat('pt-BR', {
  style: 'currency',
  currency: 'BRL',
})

const { fetchWallets } = useWallets()

onMounted(async () => {
  isLoading.value = true
  error.value = null
  try {
    wallets.value = await fetchWallets()
  } catch (err: any) {
    error.value = err?.message || 'Erro ao carregar carteiras'
    wallets.value = []
  } finally {
    isLoading.value = false
  }
})
</script>
