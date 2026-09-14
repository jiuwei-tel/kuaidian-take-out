import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 后端 skr-server 默认跑在 8080
const BACKEND = 'http://localhost:8080'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    host: '127.0.0.1',
    port: 5173,
    open: false,
    proxy: {
      // 走代理是为了绕开后端没有配置 CORS 的问题，前端代码里直接写 /admin 即可
      '/admin': {
        target: BACKEND,
        changeOrigin: true,
      },
      // 来单提醒的 WebSocket
      '/ws': {
        target: BACKEND,
        ws: true,
        changeOrigin: true,
      },
    },
  },
})
