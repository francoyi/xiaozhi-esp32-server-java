<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { env } from '../../config/env'
import { authStore } from '../../store/auth'
import { WsClient, type WsLogItem, type WsStatus } from '../../services/ws'

const route = useRoute()

const deviceId = ref<string>((route.params.deviceId as string) || '')
const authorization = ref<string>(authStore.getToken())
const wsUrl = ref<string>(env.wsUrl)

const status = ref<WsStatus>('idle')
const logs = ref<WsLogItem[]>([])
const msg = ref<string>('')

let client: WsClient | null = null

const canConnect = computed(() => deviceId.value.trim().length > 0 && wsUrl.value.trim().length > 0)

function addLog(dir: WsLogItem['dir'], text: string) {
  logs.value.push({ ts: Date.now(), dir, text })
  if (logs.value.length > 500) logs.value.splice(0, logs.value.length - 500)
}

const logBoxRef = ref<HTMLDivElement | null>(null)
async function scrollToBottom() {
  await nextTick()
  const el = logBoxRef.value
  if (!el) return
  el.scrollTop = el.scrollHeight
}

function connect() {
  if (!canConnect.value) return
  authStore.setToken(authorization.value.trim())

  addLog('sys', `Connecting: ${wsUrl.value}  device-id=${deviceId.value}`)
  status.value = 'connecting'

  client = new WsClient({
    wsUrl: wsUrl.value,
    deviceId: deviceId.value.trim(),
    authorization: authorization.value.trim() || undefined,
    onOpen() {
      status.value = 'open'
      addLog('sys', 'WebSocket OPEN')
      scrollToBottom()
    },
    onClose(code, reason) {
      status.value = 'closed'
      addLog('sys', `WebSocket CLOSED code=${code ?? '-'} reason=${reason ?? '-'}`)
      scrollToBottom()
    },
    onError(err) {
      status.value = 'error'
      addLog('sys', `WebSocket ERROR: ${String(err)}`)
      scrollToBottom()
    },
    onMessage(data) {
      addLog('in', data)
      scrollToBottom()
    },
  })

  client.connect()
}

function disconnect() {
  client?.close()
  client = null
  status.value = 'closed'
  addLog('sys', 'Disconnected')
  scrollToBottom()
}

function sendRaw() {
  const text = msg.value.trim()
  if (!text) return
  if (!client) return alert('未连接 WebSocket')
  try {
    client.sendText(text)
    addLog('out', text)
    msg.value = ''
    scrollToBottom()
  } catch (e: any) {
    alert(e?.message || '发送失败')
  }
}

function clearLogs() {
  logs.value = []
}

const enableMcp = ref<boolean>(false)

function sendHello() {
  if (!client || status.value !== 'open') return alert('先连接 WebSocket')
  const hello = {
    type: 'hello',
    audioParams: { format: 'opus', sampleRate: 16000, channels: 1, frameDuration: 60 },
    features: { mcp: enableMcp.value },
  }
  const text = JSON.stringify(hello)
  try {
    client.sendText(text)
    addLog('out', text)
    scrollToBottom()
  } catch (e: any) {
    alert(e?.message || '发送 hello 失败')
  }
}

function sendPing() {
  if (!client || status.value !== 'open') return alert('先连接 WebSocket')
  const ping = { type: 'ping', ts: Date.now() }
  const text = JSON.stringify(ping)
  try {
    client.sendText(text)
    addLog('out', text)
    scrollToBottom()
  } catch (e: any) {
    alert(e?.message || '发送失败')
  }
}

onBeforeUnmount(() => {
  try { client?.close() } catch {}
})

watch(
  () => route.params.deviceId,
  (v) => { if (typeof v === 'string') deviceId.value = v }
)
</script>

<template>
  <div class="page">
    <div class="nav">
      <button class="back" @click="$router.back()">‹</button>
      <div class="title">控制台</div>
      <div style="width:28px"></div>
    </div>

    <div class="content">

    <section style="border:1px solid #ddd;padding:12px;border-radius:8px;margin-bottom:12px">
      <div style="display:grid;grid-template-columns:1fr 1fr;gap:10px">
        <label>
          <div style="font-size:12px;opacity:.8">WS URL</div>
          <input v-model="wsUrl" style="width:100%;padding:8px" />
          <div style="font-size:12px;opacity:.65;margin-top:4px">默认：{{ env.wsUrl }}</div>
        </label>

        <label>
          <div style="font-size:12px;opacity:.8">Authorization（可选；后端支持 query 的 Authorization=...）</div>
          <input v-model="authorization" placeholder="token / bearer ..." style="width:100%;padding:8px" />
          <div style="font-size:12px;opacity:.65;margin-top:4px">
            目前后端鉴权逻辑注释，但这里保持兼容。
          </div>
        </label>

        <label style="grid-column:1 / -1">
          <div style="font-size:12px;opacity:.8">deviceId（必须）</div>
          <input v-model="deviceId" placeholder="AA:BB:... 或 user_chat_123" style="width:100%;padding:8px" />
          <div style="font-size:12px;opacity:.65;margin-top:4px">
            后端 afterConnectionEstablished 强制要求 device-id，否则直接 close。
          </div>
        </label>
      </div>

      <div style="display:flex;gap:8px;margin-top:10px;align-items:center;flex-wrap:wrap">
        <button @click="connect" :disabled="!canConnect || status==='open' || status==='connecting'">Connect</button>
        <button @click="disconnect" :disabled="status!=='open' && status!=='connecting'">Disconnect</button>
        <button @click="clearLogs">Clear Logs</button>

        <label style="display:flex;gap:6px;align-items:center;margin-left:8px">
          <input type="checkbox" v-model="enableMcp" />
          <span style="font-size:12px;opacity:.85">enable MCP (hello.features.mcp)</span>
        </label>

        <button @click="sendHello" :disabled="status!=='open'">Send Hello</button>
        <button @click="sendPing" :disabled="status!=='open'">Send Ping</button>

        <span style="margin-left:auto;font-size:12px;opacity:.8">
          Status: <b>{{ status }}</b>
        </span>
      </div>
    </section>

    <section style="border:1px solid #ddd;padding:12px;border-radius:8px;margin-bottom:12px">
      <div style="font-size:12px;opacity:.8;margin-bottom:6px">Send raw message (text / JSON)</div>
      <div style="display:flex;gap:8px">
        <input v-model="msg" @keyup.enter="sendRaw" placeholder='例如：{"type":"chat","text":"hi"}' style="flex:1;padding:8px" />
        <button @click="sendRaw" :disabled="status!=='open'">Send</button>
      </div>
      <div style="font-size:12px;opacity:.65;margin-top:6px">
        建议连接后先点 <b>Send Hello</b>，后端会回复 HelloMessageResp（带 sessionId / audioParams）。
      </div>
    </section>

    <section style="border:1px solid #ddd;border-radius:12px;overflow:hidden">
      <div style="padding:10px;border-bottom:1px solid #eee;font-size:12px;opacity:.8">Logs（in/out/sys）</div>
      <div ref="logBoxRef" style="height:360px;overflow:auto;padding:10px;background:#fafafa">
        <div v-for="(l, idx) in logs" :key="idx" style="margin-bottom:8px">
          <div style="font-size:12px;opacity:.7;display:flex;gap:8px;align-items:center">
            <span>{{ new Date(l.ts).toLocaleTimeString() }}</span>
            <span :style="{ fontWeight: 700, color: l.dir==='in' ? '#0a7' : l.dir==='out' ? '#06c' : '#555' }">
              {{ l.dir.toUpperCase() }}
            </span>
          </div>
          <pre style="margin:4px 0 0;white-space:pre-wrap;word-break:break-word">{{ l.text }}</pre>
        </div>

        <div v-if="logs.length===0" style="opacity:.7;font-size:12px">
          暂无日志。连接后先发 Hello。
        </div>
      </div>
    </section>

    </div>

    <div class="tabbar">
      <button class="tab" @click="$router.push('/home')">🏠</button>
      <button class="tab" @click="$router.push('/devices')">🛠️</button>
      <button class="tab active" @click="$router.push('/console')">💬</button>
      <button class="tab" @click="$router.push('/me')">👤</button>
    </div>
  </div>
</template>

<style scoped>
.page { min-height:100vh; background:#fff; padding-bottom: calc(78px + var(--safe-bottom)); font-family: ui-sans-serif, system-ui; }
.nav { height:56px; display:flex; align-items:center; justify-content:space-between; padding:0 14px; border-bottom:1px solid #f1f1f1; }
.back { border:none; background:transparent; font-size:26px; width:28px; cursor:pointer; }
.title { font-size:18px; font-weight:800; }
.content { padding: 12px 14px 0; }
.tabbar { position:fixed; left:0; right:0; bottom:0; display:flex; justify-content:space-around; align-items:center; height:60px; border-top:1px solid #eee; background:#fff; }
.tab { border:none; background:transparent; font-size:22px; cursor:pointer; opacity:.55; }
.tab.active { opacity:1; }
</style>
