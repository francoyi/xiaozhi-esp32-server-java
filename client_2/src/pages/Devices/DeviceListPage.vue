<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import type { DeviceDTO } from '../../utils/types'
import { queryDevices, addDevice, deleteDevice } from '../../services/device'
import { env } from '../../config/env'
import { WsClient } from '../../services/ws'

const router = useRouter()

type LoadState = 'idle' | 'loading' | 'error'
const state = ref<LoadState>('idle')
const errorMsg = ref('')

const devices = ref<DeviceDTO[]>([])
const currentDeviceId = ref<string>('')

const currentDevice = computed(() => devices.value.find(d => d.deviceId === currentDeviceId.value) || null)

// 连接状态：先用 WS 探测（后面你如果做“在线状态接口”，替换这里即可）
const isConnected = ref(false)
let ws: WsClient | null = null

function disconnectWs() {
  try { ws?.close() } catch {}
  ws = null
  isConnected.value = false
}

function connectWs(deviceId: string) {
  disconnectWs()
  if (!deviceId) return
  ws = new WsClient({
    wsUrl: env.wsUrl,
    deviceId,
    onOpen() { isConnected.value = true },
    onClose() { isConnected.value = false },
    onError() { isConnected.value = false },
  })
  ws.connect()
}

async function load() {
  state.value = 'loading'
  errorMsg.value = ''
  try {
    const res = await queryDevices({ pageNum: 1, pageSize: 100 })
    devices.value = res?.data?.list ?? []
    if (!currentDeviceId.value && devices.value.length > 0) {
      currentDeviceId.value = devices.value[0].deviceId
      connectWs(currentDeviceId.value)
    } else if (currentDeviceId.value) {
      connectWs(currentDeviceId.value)
    }
    state.value = 'idle'
  } catch (e: any) {
    state.value = 'error'
    errorMsg.value = e?.message || '加载失败'
  }
}

onMounted(load)

// 绑定设备（可选：你要不要放在这个页面？先保留一个入口）
const code = ref('')
async function bindDevice() {
  if (!code.value.trim()) return alert('请输入设备验证码 code')
  try {
    await addDevice({ code: code.value.trim() })
    code.value = ''
    await load()
    alert('绑定成功')
  } catch (e: any) {
    alert(e?.message || '绑定失败')
  }
}

async function removeDevice(deviceId: string) {
  if (!confirm(`确定删除设备 ${deviceId}？`)) return
  try {
    await deleteDevice(deviceId)
    if (currentDeviceId.value === deviceId) {
      currentDeviceId.value = ''
      disconnectWs()
    }
    await load()
  } catch (e: any) {
    alert(e?.message || '删除失败')
  }
}

function openDeviceConsole(deviceId: string) {
  router.push(`/console/${encodeURIComponent(deviceId)}`)
}

function deviceTitle(d: DeviceDTO) {
  return d.deviceName || d.type || d.deviceId
}

// 设备图：后端没给就用占位（你后面可接真实图片字段）
function deviceCoverUrl(d: DeviceDTO) {
  // 你如果后端有字段比如 coverUrl/deviceAvatar，在这里返回即可
  return (d as any).coverUrl || (d as any).deviceAvatar || ''
}
</script>

<template>
  <div class="page">
    <!-- 顶部设备栏：设备名即下拉选择 -->
    <div class="topbar">
      <div class="device-title">
        <!-- 设备名称 = 下拉选择 -->
        <select
            class="device-name-select"
            v-model="currentDeviceId"
            @change="connectWs(currentDeviceId)"
        >
          <option value="" disabled>未连接设备</option>
          <option
              v-for="d in devices"
              :key="d.deviceId"
              :value="d.deviceId"
          >
            {{ d.deviceName || d.deviceId }}
          </option>
        </select>

        <div class="device-status">
          <span class="dot" :class="{ on: isConnected }"></span>
          <span class="status-text">
            {{ isConnected ? '已连接' : '未连接' }}
          </span>
        </div>
      </div>
    </div>

    <!-- 标题 -->
    <div class="section-title">设备</div>

    <!-- 绑定设备 -->
    <details class="bind-box">
      <summary>绑定新设备（可选）</summary>
      <div class="bind-row">
        <input
            class="bind-input"
            v-model="code"
            placeholder="输入设备验证码 code"
        />
        <button class="bind-btn" @click="bindDevice">绑定</button>
      </div>
    </details>

    <div v-if="state==='loading'" class="hint">加载中…</div>
    <div v-else-if="state==='error'" class="hint err">
      加载失败：{{ errorMsg }}
    </div>

    <!-- 设备卡片网格 -->
    <div class="grid">
      <button
          v-for="d in devices"
          :key="d.deviceId"
          class="card"
          type="button"
          @click="openDeviceConsole(d.deviceId)"
      >
        <div class="card-img">
          <img v-if="deviceCoverUrl(d)" :src="deviceCoverUrl(d)" />
          <div v-else class="img-fallback">📟</div>
        </div>

        <div class="card-name">{{ deviceTitle(d) }}</div>

        <div class="card-actions" @click.stop>
          <button class="mini" @click="openDeviceConsole(d.deviceId)">
            连接
          </button>
          <button class="mini danger" @click="removeDevice(d.deviceId)">
            删除
          </button>
        </div>
      </button>

      <div v-if="devices.length===0 && state!=='loading'" class="empty">
        暂无设备（或 token/baseURL 不对）
      </div>
    </div>

    <!-- 底部 TabBar -->
    <div class="tabbar">
      <button class="tab" @click="$router.push('/home')">🏠</button>
      <button class="tab active" @click="$router.push('/devices')">🛠️</button>
      <button class="tab" @click="$router.push('/console')">💬</button>
      <button class="tab" @click="$router.push('/me')">👤</button>
    </div>
  </div>
</template>


<style scoped>
.page {
  min-height: 100vh;
  min-height: 100dvh;
  padding: 18px 16px calc(78px + var(--safe-bottom));
  background: #fff;
  font-family: ui-sans-serif, system-ui;
}

/* 顶部 */
.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.device-title {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.device-name-select {
  font-size: 28px;
  font-weight: 800;
  letter-spacing: 0.2px;
  border: none;
  background: transparent;
  padding: 0;
  margin: 0;
  outline: none;
  appearance: none;
  -webkit-appearance: none;
  line-height: 1.1;
}

.device-name-select:disabled {
  opacity: .7;
}


.device-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #666;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 99px;
  background: #bbb;
}
.dot.on { background: #19c37d; }


/* 标题 */
.section-title {
  font-size: 22px;
  font-weight: 800;
  margin: 8px 0 12px;
}

/* 绑定（可选） */
.bind-box {
  margin-bottom: 12px;
  border: 1px solid #eee;
  border-radius: 12px;
  padding: 10px 12px;
  background: #fafafa;
}
.bind-row {
  display: flex;
  gap: 8px;
  margin-top: 10px;
}
.bind-input {
  flex: 1;
  border: none;
  outline: none;
  border-radius: 10px;
  background: #fff;
  padding: 10px 12px;
}
.bind-btn {
  border: none;
  border-radius: 10px;
  padding: 10px 14px;
  background: #000;
  color: #fff;
  font-weight: 700;
  cursor: pointer;
}

/* 网格 */
.grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px 18px;
  padding-top: 10px;
}

.card {
  position: relative;
  border: none;
  background: transparent;
  text-align: center;
  cursor: pointer;
}

.card-img {
  width: 100%;
  aspect-ratio: 1 / 1;
  border-radius: 18px;
  background: #f2f2f2;
  overflow: hidden;
  display: grid;
  place-items: center;
}

.card-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.img-fallback {
  font-size: 34px;
  opacity: .6;
}

.card-name {
  margin-top: 10px;
  font-size: 14px;
  font-weight: 700;
  color: #111;
}

/* 卡片右下角操作 */
.card-actions {
  position: absolute;
  right: 10px;
  bottom: 44px;
  display: flex;
  gap: 8px;
}

.mini {
  border: none;
  padding: 6px 10px;
  border-radius: 10px;
  background: rgba(0,0,0,.75);
  color: #fff;
  font-size: 12px;
  cursor: pointer;
}
.mini.danger {
  background: rgba(200,0,0,.75);
}

.empty {
  grid-column: 1 / -1;
  padding: 18px 0;
  text-align: center;
  opacity: .7;
  font-size: 13px;
}

/* TabBar */
.tabbar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  height: 62px;
  display: flex;
  align-items: center;
  justify-content: space-around;
  border-top: 1px solid #eee;
  background: #fff;
}

.tab {
  border: none;
  background: transparent;
  font-size: 22px;
  opacity: .55;
  cursor: pointer;
}
.tab.active { opacity: 1; }

.hint {
  padding: 10px 0;
  font-size: 12px;
  opacity: .75;
}
.hint.err { color: #c00; opacity: 1; }
</style>
