<template>
  <div class="auth">
    <div class="card">
      <h1>设备扫码登录</h1>

      <p v-if="loading">正在处理...</p>
      <p v-else-if="error" class="error">{{ error }}</p>
      <p v-else>即将跳转...</p>

      <div v-if="showAction" class="actions">
        <button class="btn" @click="goLogin">去登录</button>
        <button class="btn" @click="goRegister">去注册</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import deviceService from '../../services/device'
import { authStore } from '../../store/auth'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const error = ref<string>('')
const showAction = ref(false)

const code = (route.query.code as string | undefined)?.trim()

function goLogin() {
  router.replace({ path: '/login', query: { code } })
}

function goRegister() {
  router.replace({ path: '/register', query: { code } })
}

onMounted(async () => {
  try {
    if (!code) {
      throw new Error('缺少扫码参数 code')
    }

    const res = await deviceService.scanLogin(code)
    const data = res?.data
    if (!data) {
      throw new Error('服务端返回为空')
    }

    if (data.bound && data.login) {
      // 已绑定：直接写入 token 并跳转
      authStore.setToken(data.login.token)
      authStore.setUserId(data.login.user)
      await router.replace('/home')
      return
    }

    // 未绑定：让用户去登录/注册，带着 code
    showAction.value = true
  } catch (e: any) {
    error.value = e?.message || '扫码处理失败'
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.auth {
  min-height: 100vh;
  min-height: 100dvh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
}

.card {
  width: 100%;
  max-width: 100%;
  max-width: 420px;
  border: 1px solid #eee;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

h1 {
  font-size: 18px;
  margin: 0 0 12px;
}

.error {
  color: #d33;
}

.actions {
  display: flex;
  gap: 12px;
  margin-top: 16px;
}

.btn {
  flex: 1;
  border: 1px solid #ddd;
  background: #fff;
  border-radius: 10px;
  padding: 10px 12px;
}
</style>
