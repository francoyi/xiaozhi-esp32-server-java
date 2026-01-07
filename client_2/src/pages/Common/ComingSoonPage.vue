<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

// 允许从路由 props 传入，也允许 query 覆盖
const title = (route.meta?.title as string) || (route.params as any)?.title || (route.query.title as string) || (route as any).props?.title || '开发中'
const hint = (route.query.hint as string) || '该页面正在移动端适配中。'
</script>

<template>
  <div class="page">
    <div class="nav">
      <button class="back" @click="router.back()">‹</button>
      <div class="title">{{ title }}</div>
      <div style="width: 28px"></div>
    </div>

    <div class="content">
      <div class="box">
        <div class="big">🚧</div>
        <div class="text">{{ hint }}</div>
        <button class="btn" @click="$router.push('/home')">返回首页</button>
      </div>
    </div>

    <div class="tabbar">
      <button class="tab" @click="$router.push('/home')">🏠</button>
      <button class="tab" @click="$router.push('/devices')">🛠️</button>
      <button class="tab" @click="$router.push('/console')">💬</button>
      <button class="tab" @click="$router.push('/me')">👤</button>
    </div>
  </div>
</template>

<style scoped>
.page { min-height: 100vh;
  min-height: 100dvh; background:#fff; padding-bottom: calc(78px + var(--safe-bottom)); font-family: ui-sans-serif, system-ui; }
.nav { height: 56px; display:flex; align-items:center; justify-content:space-between; padding:0 14px; border-bottom:1px solid #f1f1f1; }
.back { border:none; background:transparent; font-size:26px; width:28px; cursor:pointer; }
.title { font-size:18px; font-weight:800; }
.content { padding: 28px 16px; }
.box { border:1px solid #eee; background:#fafafa; border-radius:14px; padding:18px; display:flex; flex-direction:column; align-items:center; gap:10px; }
.big { font-size:40px; }
.text { opacity:.8; }
.btn { border:none; border-radius:12px; padding:10px 14px; background:#000; color:#fff; font-weight:700; cursor:pointer; }

.tabbar {
  position: fixed; left: 0; right: 0; bottom: 0;
  display: flex; justify-content: space-around; align-items: center;
  height: 60px; border-top: 1px solid #eee; background: #fff;
}
.tab { border:none; background:transparent; font-size:22px; cursor:pointer; opacity:.55; }
.tab:active { transform: scale(0.96); }
</style>
