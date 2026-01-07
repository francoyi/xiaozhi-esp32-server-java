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
  <!-- ✅ 改成：可滚动页面容器（不强制垂直居中） -->
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
/* ✅ 关键：让页面自己滚动，避免 iOS 100vh + 键盘导致“滑不动/挡住” */
.page {
  min-height: 100vh;
  min-height: 100dvh;

  overflow-y: auto;
  -webkit-overflow-scrolling: touch;

  background: #fff;
  font-family: ui-sans-serif, system-ui;

  /* ✅ 顶部/底部安全区 + 额外留白，避免被 Safari/底部手势条影响 */
  padding: calc(24px + env(safe-area-inset-top))
  20px
  calc(28px + env(safe-area-inset-bottom))
  20px;

  box-sizing: border-box;
}

/* ✅ 只做水平居中，不强制垂直居中（让内容自然往下排，键盘弹出也不乱） */
.center {
  width: 100%;
  max-width: 380px;
  margin: 0 auto;
  text-align: center;
}

/* 视觉样式保持你的原风格 */
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
  font-size: 16px;            /* ✅ 关键：>=16px 禁止 iOS 自动放大 */
  -webkit-text-size-adjust: 100%; /* ✅ 防止 Safari 额外字体调整 */
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
  opacity: 0.35;
  cursor: not-allowed;
}

.link {
  border: none;
  background: transparent;
  cursor: pointer;
  font-size: 13px;
  opacity: 0.75;
  margin-top: 6px;
}
</style>
