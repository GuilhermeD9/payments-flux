// https://nuxt.com/docs/api/configuration/nuxt-config
export default defineNuxtConfig({
  compatibilityDate: '2025-07-15',
  devtools: { enabled: true },

  extends: [
    './layers/core',
    './layers/wallets',
    './layers/transfers'
  ],

  css: ['~/assets/css/tailwind.css'],
  modules: ['@nuxtjs/tailwindcss']
})
