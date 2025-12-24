<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '../../services/api'
import { request } from '../../services/request'
import { authStore } from '../../store/auth'

const router = useRouter()

type Mode = 'email' | 'tel'
const mode = ref<Mode>('email')

const username = ref('')
const password = ref('')

// 注册必须的字段（对应 RegisterParam）
const email = ref('')
const tel = ref('')
const code = ref('')

const loading = ref(false)
const sending = ref(false)
const cooldown = ref(0)
let timer: any = null

const accountValue = computed(() => (mode.value === 'email' ? email.value.trim() : tel.value.trim()))

const canSendCode = computed(() => {
  return accountValue.value.length > 0 && !sending.value && cooldown.value === 0
})

const canSubmit = computed(() => {
  return (
      username.value.trim().length > 0 &&
      password.value.trim().length > 0 &&
      accountValue.value.length > 0 &&
      code.value.trim().length > 0 &&
      !loading.value
  )
})

function startCooldown(seconds = 60) {
  cooldown.value = seconds
  if (timer) clearInterval(timer)
  timer = setInterval(() => {
    cooldown.value -= 1
    if (cooldown.value <= 0) {
      cooldown.value = 0
      clearInterval(timer)
      timer = null
    }
  }, 1000)
}

/**
 * 发送验证码
 * - 邮箱：POST /user/sendEmailCaptcha  body: { email, type }
 * - 手机：POST /user/sendSmsCaptcha    body: { tel, type }
 *
 * 你后端 SendCaptchaParam 有 type：注册场景可以传 "register"
 */
async function sendCaptcha() {
  if (!canSendCode.value) return
  sending.value = true
  try {
    if (mode.value === 'email') {
      await request.post(api.user.sendEmailCaptcha, { email: email.value.trim(), type: 'register' })
    } else {
      await request.post(api.user.sendSmsCaptcha, { tel: tel.value.trim(), type: 'register' })
    }
    startCooldown(60)
    alert('验证码已发送')
  } catch (e: any) {
    alert(e?.message || '发送失败')
  } finally {
    sending.value = false
  }
}

/**
 * 注册（严格匹配后端 RegisterParam）
 * 必填：username / password / code / (email 或 tel)
 * name/email/tel 可选（这里 name 先用 username 顶一下）
 */
async function submit() {
  if (!canSubmit.value) return
  loading.value = true
  try {
    await request.post(api.user.add, {
      username: username.value.trim(),
      password: password.value.trim(),
      name: username.value.trim(),
      email: mode.value === 'email' ? email.value.trim() : null,
      tel: mode.value === 'tel' ? tel.value.trim() : null,
      code: code.value.trim(),
    })

    // 注册成功后自动登录：POST /user/login  body: { username, password }
    const loginRes = await request.post(api.user.login, {
      username: username.value.trim(),
      password: password.value.trim(),
    })

    // 你的后端：ResultMessage.success(LoginResponseDTO) => token 在 data.token
    const token = (loginRes.data as any)?.data?.token
    if (token) authStore.setToken(String(token))

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
        <!-- 用户名/密码：按你的设计图保留 -->
        <input class="input" v-model="username" placeholder="输入用户名" autocomplete="username" />
        <input class="input" v-model="password" placeholder="输入密码" type="password" autocomplete="new-password" />

        <!-- 由于后端强制验证码：最小补充（邮箱/手机号 + 验证码） -->
        <div class="mode">
          <button class="chip" :class="{ on: mode==='email' }" @click="mode='email'">邮箱</button>
          <button class="chip" :class="{ on: mode==='tel' }" @click="mode='tel'">手机</button>
        </div>

        <input v-if="mode==='email'" class="input" v-model="email" placeholder="输入邮箱" />
        <input v-else class="input" v-model="tel" placeholder="输入手机号" />

        <div class="code-row">
          <input class="input code-input" v-model="code" placeholder="输入验证码" />
          <button class="code-btn" :disabled="!canSendCode" @click="sendCaptcha">
            {{ cooldown>0 ? `${cooldown}s` : (sending ? '发送中…' : '发送验证码') }}
          </button>
        </div>

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

.mode {
  display: flex;
  gap: 10px;
  justify-content: center;
  margin-top: 4px;
}

.chip {
  border: 1px solid #e9e9e9;
  background: #fff;
  border-radius: 999px;
  padding: 8px 14px;
  cursor: pointer;
  font-weight: 700;
  opacity: .7;
}
.chip.on {
  opacity: 1;
  border-color: #000;
}

.code-row {
  display: flex;
  gap: 10px;
}
.code-input { flex: 1; }

.code-btn {
  width: 120px;
  border-radius: 10px;
  border: 1px solid #e9e9e9;
  background: #fafafa;
  cursor: pointer;
  font-weight: 700;
}
.code-btn:disabled {
  opacity: .4;
  cursor: not-allowed;
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
