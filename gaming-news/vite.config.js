import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    proxy: {
      // v1 is the backend's real path; only legacy endpoints lose the /api prefix.
      '^/api/v1(?:/|$)': {
        target: process.env.GAMEBOI_DEV_API_TARGET || 'http://localhost:8080',
        changeOrigin: true
      },
      '^/api/(?:user|article|category|comment|article-like|article-favorite|article-view|comment-like|upload)(?:/|\\?|$)': {
        target: process.env.GAMEBOI_DEV_API_TARGET || 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '')
      }
    }
  },
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.js']
  }
})