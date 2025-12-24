import axios, { AxiosError } from 'axios'
import { env } from '../config/env'
import { authStore } from '../store/auth'

export const request = axios.create({
  baseURL: env.apiBaseUrl,
  timeout: 20000,
})

request.interceptors.request.use((config) => {
  const token = authStore.getToken()
  if (token) {
    config.headers = config.headers ?? {}
    // 兼容不同后端：
    // 1) JWT/网关常见要求 Authorization: Bearer <token>
    // 2) Sa-Token 常见使用 satoken: <token>
    ;(config.headers as any)['Authorization'] = token.startsWith('Bearer ') ? token : `Bearer ${token}`
    ;(config.headers as any)['satoken'] = token
  }
  return config
})

request.interceptors.response.use(
  (res) => {
    // Backend usually returns { code, msg, data }. Normalize error handling here.
    const body: any = res.data
    if (body && typeof body === 'object' && 'code' in body) {
      // 兼容后端：有的用 0/200 表示成功；有的用 1 表示成功。
      const code = Number(body.code)
      if (![0, 1, 200].includes(code)) {
        const msg = body.msg || body.message || 'Request failed'
        return Promise.reject(new Error(msg))
      }
    }
    // 注意：这里返回 axios 的 Response，让上层按 res.data 取。
    return res
  },
  (err: AxiosError<any>) => {
    const status = err.response?.status
    const body: any = err.response?.data
    const msg = body?.msg || body?.message || err.message || 'Network error'

    // ✅ 统一处理登录过期/未登录：清 token 并跳转到 /login
    // 后端常见：HTTP 401，或业务 code=401
    const bizCode = typeof body?.code !== 'undefined' ? Number(body.code) : undefined
    if (status === 401 || bizCode === 401) {
      authStore.clearToken()
      // 避免在 login 页面死循环
      if (!location.pathname.startsWith('/login')) {
        const redirect = encodeURIComponent(location.pathname + location.search)
        location.href = `/login?redirect=${redirect}`
      }
      return Promise.reject(new Error('登录已过期，请重新登录'))
    }

    return Promise.reject(new Error(msg))
  }
)
