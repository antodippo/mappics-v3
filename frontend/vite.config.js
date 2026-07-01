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
  // Vitest transforms with esbuild (the build uses oxc, which ignores this).
  // Force the automatic JSX runtime so test files don't need a React import.
  esbuild: process.env.VITEST ? { jsx: 'automatic' } : undefined,
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: './src/test/setup.js',
  },
})
