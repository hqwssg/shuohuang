import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig(({ command }) => ({
  base: command === 'build' ? '/carbon-model/' : '/',
  plugins: [vue()],
  server: {
    port: 5177,
    proxy: {
      '/api': {
        target: 'http://localhost:8082',
        changeOrigin: true
      }
    }
  }
}))
