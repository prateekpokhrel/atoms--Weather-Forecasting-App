import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import { VitePWA } from 'vite-plugin-pwa'

export default defineConfig({
  plugins: [react(), tailwindcss(), VitePWA({
    registerType: 'autoUpdate',
    manifest: {
      name: 'Atmos Weather', short_name: 'Atmos', theme_color: '#17233a',
      background_color: '#b9dbf3', display: 'standalone',
      icons: [{ src: '/favicon.svg', sizes: 'any', type: 'image/svg+xml' }]
    },
    workbox: { runtimeCaching: [{ urlPattern: /^https:\/\/.*openstreetmap.*\//, handler: 'CacheFirst', options: { cacheName: 'map-tiles', expiration: { maxEntries: 120 } } }] }
  })],
})
