<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { authStore } from '../../store/auth'

const router = useRouter()
const token = computed(() => authStore.getToken())

function logout() {
  authStore.clearToken()
  router.replace('/login')
}
</script>

<template>
  <div class="page">
    <div class="nav">
      <div style="width:28px"></div>
      <div class="title">个人</div>
      <div style="width:28px"></div>
    </div>

    <div class="content">
      <div class="card">
        <div class="row">
          <div class="k">登录状态</div>
          <div class="v" :class="{ ok: !!token }">{{ token ? '已登录' : '未登录' }}</div>
        </div>
        <div class="row" v-if="token">
          <div class="k">Token</div>
          <div class="v mono">{{ token.slice(0, 24) }}…</div>
        </div>
      </div>

      <button class="btn" @click="logout">退出登录</button>
    </div>

    <div class="tabbar">
      <button class="tab" @click="$router.push('/home')">🏠</button>
      <button class="tab" @click="$router.push('/devices')">🛠️</button>
      <button class="tab" @click="$router.push('/console')">💬</button>
      <button class="tab active">👤</button>
    </div>
  </div>
</template>

<style scoped>
.page { min-height:100vh; background:#fff; padding-bottom: calc(78px + var(--safe-bottom)); font-family: ui-sans-serif, system-ui; }
.nav { height:56px; display:flex; align-items:center; justify-content:space-between; padding:0 14px; border-bottom:1px solid #f1f1f1; }
.title { font-size:18px; font-weight:800; }
.content { padding:18px; display:flex; flex-direction:column; gap:14px; }
.card { border:1px solid #eee; border-radius:14px; padding:14px; }
.row { display:flex; justify-content:space-between; gap:12px; padding:10px 0; border-bottom:1px dashed #eee; }
.row:last-child { border-bottom:none; }
.k { font-weight:700; }
.v { opacity:.8; }
.v.ok { color:#0a7; opacity:1; font-weight:800; }
.mono { font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace; font-size:12px; }
.btn { width:100%; height:50px; border-radius:12px; border:none; background:#000; color:#fff; font-weight:800; cursor:pointer; }

.tabbar { position:fixed; left:0; right:0; bottom:0; display:flex; justify-content:space-around; align-items:center; height:60px; border-top:1px solid #eee; background:#fff; }
.tab { border:none; background:transparent; font-size:22px; cursor:pointer; opacity:.55; }
.tab.active { opacity:1; }
</style>
