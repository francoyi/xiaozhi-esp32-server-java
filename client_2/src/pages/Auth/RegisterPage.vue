<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../../services/api'
import { request } from '../../services/request'
import { authStore } from '../../store/auth'

const router = useRouter()
const route = useRoute()

const username = ref('')
const password = ref('')
const confirmPassword = ref('')

const loading = ref(false)

const canSubmit = computed(() => {
  return username.value.trim().length > 0 && password.value.trim().length > 0 && !loading.value
})

/**
 * ✅ 极简注册：仅用户名 + 密码
 * - 调用后端扫码注册接口 /user/scan-register
 * - 返回结构与 /login 一致：ResultMessage.success(LoginResponseDTO)
 * - 若当前是扫码进入（/register?code=XXXX），注册并登录后自动 scan-bind
 */
async function submit() {
  if (!canSubmit.value) return
  if (confirmPassword.value && confirmPassword.value.trim() !== password.value.trim()) {
    alert('两次密码不一致')
    return
  }

  loading.value = true
  try {
    const res = await request.post(api.user.scanRegister, {
      username: username.value.trim(),
      password: password.value.trim(),
      confirmPassword: (confirmPassword.value || password.value).trim(),
    })

    const token = (res.data as any)?.data?.token
    const userId = (res.data as any)?.data?.userId || (res.data as any)?.data?.user?.userId
    if (token) authStore.setToken(String(token))
    if (userId) authStore.setUserId(userId)

    // 扫码进入：自动绑定设备（不影响注册主流程）
    const scanCode = (route.query.code as string) || ''
    if (scanCode) {
      try {
        await request.post(api.device.scanBind, { code: scanCode })
      } catch (e) {
        // 绑定失败不阻塞
      }
    }

    alert('注册成功')
    router.push('/home')
  } catch (e: any) {
    alert(e?.message || '注册失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="page">
    <div class="center">
      <div class="brand">LazyCat AI</div>
      <div class="subtitle">设备激活</div>

      <div class="form">
        <input class="input" v-model="username" placeholder="输入用户名" autocomplete="username" />
        <input class="input" v-model="password" placeholder="输入密码" type="password" autocomplete="new-password" />
        <input class="input" v-model="confirmPassword" placeholder="确认密码（可选）" type="password" autocomplete="new-password" />

        <button class="primary" :disabled="!canSubmit" @click="submit">
          {{ loading ? '提交中…' : '注册' }}
        </button>

        <button class="link" type="button" @click="router.push('/login')">
          已有账号？去登录
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
  margin-top: 30px;
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
