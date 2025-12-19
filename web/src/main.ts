import './assets/main.css'
import './assets/theme.css'
import 'ant-design-vue/dist/reset.css'
import 'nprogress/nprogress.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'
import Antd from 'ant-design-vue'

import App from './App.vue'
import router from './router'
import { setupRouterGuards } from './router/guards'
import { setupErrorHandler } from './utils/errorHandler'
import { i18n } from './locales'

console.log('[BOOT] main.ts start')

const app = createApp(App)

console.log('[BOOT] after createApp')

setupErrorHandler(app)
console.log('[BOOT] after setupErrorHandler')

app.use(createPinia())
app.use(router)
app.use(Antd)
app.use(i18n)
console.log('[BOOT] after plugins')

setupRouterGuards(router)
console.log('[BOOT] after guards')

app.mount('#app')
console.log('[BOOT] mounted')
