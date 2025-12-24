<<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '../../services/api'
import { request } from '../../services/request'
import { authStore } from '../../store/auth'

const router = useRouter()

const username = ref('')
const password = ref('')
const loading = ref(false)

const canSubmit = computed(() => {
  return username.value.trim().length > 0 && password.value.trim().length > 0 && !loading.value
})

async function submit() {
  if (!canSubmit.value) return
  loading.value = true
  try {
    const res = await request.post(api.user.login, {
      username: username.value.trim(),
      password: password.value.trim(),
    })

    // 兼容多种登录返回：
    // - ResultMessage.success(LoginResponseDTO): data.token / data.tokenValue
    // - 直接返回 token: res.data.token
    // - 写在响应头里: satoken / authorization
    const body: any = res.data
    const data: any = body?.data
    const headerToken = (res.headers as any)?.satoken || (res.headers as any)?.authorization
    const token =
      data?.token ||
      data?.tokenValue ||
      data?.accessToken ||
      data?.saToken ||
      body?.token ||
      headerToken

    if (!token) throw new Error('登录成功但未返回 token（请检查后端登录接口返回）')

    // 统一存「裸 token」：请求拦截器会自动补 Bearer
    const tokenStr = String(token).replace(/^Bearer\s+/i, '')
    authStore.setToken(tokenStr)
    router.push('/home')
  } catch (e: any) {
    alert(e?.message || '登录失败')
  } finally {
    loading.value = false
  }
}

function goRegister() {
  router.push('/register')
}
</script>

<template>
  <div class="page">
    <div class="center">
      <div class="brand">LazyCat AI</div>
      <div class="subtitle">设备激活</div>

      <div class="form">
        <input class="input" v-model="username" placeholder="输入用户名" autocomplete="username" />
        <input class="input" v-model="password" placeholder="输入密码" type="password" autocomplete="current-password" />

        <button class="primary" :disabled="!canSubmit" @click="submit">
          {{ loading ? '登录中…' : '登录' }}
        </button>

        <button class="link" type="button" @click="goRegister">
          没有账号？去注册
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page {
  min-height: 100vh;
  background: #fff;
  font-family: ui-sans-serif, system-ui;
  display: grid;
  place-items: center;
  padding: 20px;
}

.center {
  width: 100%;
  max-width: 380px;
  text-align: center;
}

.brand {
  font-size: 34px;
  font-weight: 800;
  letter-spacing: 0.3px;
  margin-top: 10px;
}

.subtitle {
  margin-top: 10px;
  font-size: 16px;
  font-weight: 700;
  opacity: 0.85;
}

.form {
  margin-top: 36px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.input {
  height: 44px;
  border-radius: 10px;
  border: 1px solid #e9e9e9;
  padding: 0 14px;
  outline: none;
  font-size: 14px;
}

.primary {
  height: 48px;
  border-radius: 12px;
  border: none;
  background: #000;
  color: #fff;
  font-size: 16px;
  font-weight: 800;
  cursor: pointer;
  margin-top: 6px;
}

.primary:disabled {
  opacity: .35;
  cursor: not-allowed;
}

.link {
  border: none;
  background: transparent;
  cursor: pointer;
  font-size: 13px;
  opacity: .75;
  margin-top: 6px;
}
</style>
