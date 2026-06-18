<template>
  <div class="min-h-screen bg-coal text-gray-100 flex">
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

    <main class="flex-1 p-8 overflow-y-auto">
      <slot></slot>
    </main>
  </div>
</template>