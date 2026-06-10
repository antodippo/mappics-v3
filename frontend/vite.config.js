import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': 'http://localhost:8081',
      '/import': 'http://localhost:8081',
      '/local-images': 'http://localhost:8081',
    },
  },
})
