import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
    plugins: [vue()],
    server: {
        proxy: {
            '/api': {
                target: 'http://localhost:8091', // 改成你后端实际端口
                changeOrigin: true,
            },
        },
    },
})
