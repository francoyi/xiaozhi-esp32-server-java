import { fileURLToPath, URL } from 'node:url'
import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const backend = env.VITE_BACKEND_URL || 'http://localhost:8091'

  return {
    plugins: [vue(), vueDevTools()],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url)),
      },
    },
    server: {
      port: 8084,
      host: '0.0.0.0',
      proxy: {
        // HTTP API
        '/api': {
          target: backend,
          changeOrigin: true,
          secure: false,
          ws: true, // ✅ 允许同域升级（有些库会在 /api 下升级 ws）
        },

        // ✅ 如果你的 WS 路径是 /ws 或 /socket，请按实际路径改
        '/ws': {
          target: backend.replace(/^http/, 'ws'),
          ws: true,
          changeOrigin: true,
        },
      },
    },
  }
})
