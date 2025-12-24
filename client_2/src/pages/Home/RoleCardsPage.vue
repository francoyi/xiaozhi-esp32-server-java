<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import type { DeviceDTO, RoleDTO } from '../../utils/types'
import { queryDevices } from '../../services/device'
import { queryRoles, deleteRole, setPublishedRoleIds } from '../../services/role'
import { env } from '../../config/env'
import { WsClient } from '../../services/ws'

type Mode = 'normal' | 'delete' | 'publish'

const router = useRouter()

// 顶部：设备列表 + 当前设备（仅用于显示连接状态，不影响角色编辑/删除/发布）
const devices = ref<DeviceDTO[]>([])
const currentDeviceId = ref<string>('')
const isConnected = ref(false)
let ws: WsClient | null = null

const roles = ref<RoleDTO[]>([])
const keyword = ref('')

const mode = ref<Mode>('normal')
const selectedIds = ref<Set<string>>(new Set())

const filteredRoles = computed(() => {
  const k = keyword.value.trim().toLowerCase()
  const list = roles.value
  if (!k) return list
  return list.filter(r => {
    const name = (r.roleName || '').toLowerCase()
    const desc = ((r as any).roleDesc || '').toLowerCase()
    return name.includes(k) || desc.includes(k)
  })
})

const currentDevice = computed(() => devices.value.find(d => d.deviceId === currentDeviceId.value) || null)

async function loadDevices() {
  const res = await queryDevices({ pageNum: 1, pageSize: 100 })
  devices.value = res?.data?.list ?? []
  if (!currentDeviceId.value && devices.value.length > 0) currentDeviceId.value = devices.value[0].deviceId
}

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
    onOpen() {
      isConnected.value = true
      try {
        ws?.sendText(JSON.stringify({
          type: 'hello',
          audioParams: { format: 'opus', sampleRate: 16000, channels: 1, frameDuration: 60 },
          features: { mcp: false },
        }))
      } catch {}
    },
    onClose() { isConnected.value = false },
    onError() { isConnected.value = false },
  })
  ws.connect()
}

const selectedCount = computed(() => selectedIds.value.size)

function isPublished(r: RoleDTO): boolean {
  const v = (r as any).published
  return v === 1 || v === '1' || v === true
}

function roleKey(r: RoleDTO) {
  return String(r.roleId)
}

function resetSelection() {
  selectedIds.value = new Set()
}

async function loadRoles() {
  const res = await queryRoles({ pageNum: 1, pageSize: 200 })
  roles.value = res?.data?.list ?? []
}

watch(currentDeviceId, (id) => {
  if (id) connectWs(id)
})

function enterDeleteMode() {
  if (mode.value === 'delete') {
    mode.value = 'normal'
    resetSelection()
    return
  }
  mode.value = 'delete'
  resetSelection()
}

function enterPublishMode() {
  if (mode.value === 'publish') {
    mode.value = 'normal'
    resetSelection()
    return
  }
  mode.value = 'publish'
  // 进入发布模式：自动勾选已发布的角色
  const s = new Set<string>()
  for (const r of roles.value) {
    if (isPublished(r)) s.add(roleKey(r))
  }
  selectedIds.value = s
}

function toggleSelect(id: string) {
  const s = new Set(selectedIds.value)
  if (s.has(id)) s.delete(id)
  else {
    // 发布模式最多 2 个
    if (mode.value === 'publish' && s.size >= 2) {
      alert('最多只能发布 2 个角色')
      return
    }
    s.add(id)
  }
  selectedIds.value = s
}

function onCardClick(r: RoleDTO) {
  const id = String(r.roleId)
  if (mode.value === 'normal') {
    router.push(`/roles/edit/${encodeURIComponent(id)}`)
    return
  }
  // delete/publish 模式下：点击卡片切换勾选
  toggleSelect(id)
}

function onCreateRole() {
  router.push('/roles/create')
}

async function confirmDelete() {
  if (selectedIds.value.size === 0) return

  // 后端规则：published=1 不允许删除；这里前端也提前挡一下
  const ids = Array.from(selectedIds.value)
  const cannot = ids.filter(id => {
    const r = roles.value.find(x => String(x.roleId) === id)
    return r ? isPublished(r) : false
  })
  if (cannot.length > 0) {
    alert('包含已发布角色，无法删除。请先取消发布后再删除。')
    return
  }

  if (!confirm(`确定删除 ${selectedIds.value.size} 个角色吗？`)) return

  const failed: Array<{ id: string; reason: string }> = []
  for (const id of ids) {
    try {
      await deleteRole(id)
    } catch (e: any) {
      failed.push({ id, reason: e?.message || '删除失败' })
    }
  }

  await loadRoles()
  resetSelection()
  mode.value = 'normal'

  if (failed.length) {
    const msg = failed.map(x => `roleId=${x.id}: ${x.reason}`).join('\n')
    alert('部分删除失败：\n' + msg)
  } else {
    alert('删除成功')
  }
}

async function confirmPublish() {
  // 覆盖式发布：0~2 个
  const ids = Array.from(selectedIds.value)
  if (ids.length > 2) {
    alert('最多只能发布 2 个角色')
    return
  }
  try {
    await setPublishedRoleIds(ids)
    await loadRoles()
    // 重新勾选已发布（以服务端为准）
    const s = new Set<string>()
    for (const r of roles.value) {
      if (isPublished(r)) s.add(roleKey(r))
    }
    selectedIds.value = s
    alert('发布设置已保存')
  } catch (e: any) {
    alert(e?.message || '发布失败')
  }
}

onMounted(async () => {
  await Promise.all([loadDevices(), loadRoles()])
  if (currentDeviceId.value) connectWs(currentDeviceId.value)
})
</script>

<template>
  <div class="page">
    <div class="fixed-header">
      <div class="topbar">
        <div class="device-title">
          <div class="device-name">{{ currentDevice?.deviceName || currentDeviceId || '—' }}</div>
          <div class="device-status">
            <span class="dot" :class="{ on: isConnected }"></span>
            <span class="status-text">{{ isConnected ? '已连接' : '未连接' }}</span>
          </div>
        </div>

        <select class="device-select" v-model="currentDeviceId">
          <option v-for="d in devices" :key="d.deviceId" :value="d.deviceId">
            {{ d.deviceName || d.deviceId }}
          </option>
        </select>

        <!-- 操作按钮：发布 / 删除（位置保持你现在的“右上角”布局） -->
        <div class="actions">
          <button class="icon-btn" type="button" @click="enterPublishMode" :class="{ on: mode === 'publish' }" title="发布">
            <span class="icon">📤</span>
          </button>
          <button class="icon-btn" type="button" @click="enterDeleteMode" :class="{ on: mode === 'delete' }" title="删除">
            <span class="icon">🗑️</span>
          </button>
        </div>
      </div>

      <div class="section-title">角色卡</div>

      <!-- 模式操作条（与截图一致：取消 + 确认） -->
      <div v-if="mode !== 'normal'" class="modebar">
        <button class="text-btn" type="button" @click="mode = 'normal'; resetSelection()">取消</button>
        <button
          v-if="mode === 'delete'"
          class="danger-btn"
          type="button"
          :disabled="selectedCount === 0"
          @click="confirmDelete"
        >
          确认删除 ({{ selectedCount }})
        </button>

        <button
          v-if="mode === 'publish'"
          class="primary-btn"
          type="button"
          @click="confirmPublish"
        >
          保存发布 ({{ selectedCount }}/2)
        </button>
      </div>

      <div class="search">
        <span class="search-icon">🔍</span>
        <input v-model="keyword" class="search-input" placeholder="角色" />
      </div>
    </div>

    <div class="scroll-area">
      <div class="grid">
        <button class="card" type="button" @click="onCreateRole" :disabled="mode !== 'normal'">
          <div class="card-img placeholder">
            <div class="plus-box"><div class="plus">+</div></div>
          </div>
          <div class="card-name">新建角色</div>
        </button>

        <button
          v-for="r in filteredRoles"
          :key="String(r.roleId)"
          class="card"
          type="button"
          @click="onCardClick(r)"
        >
          <!-- 右上角勾选框 -->
          <div v-if="mode !== 'normal'" class="select-box" @click.stop="toggleSelect(String(r.roleId))">
            <div class="checkbox" :class="{ checked: selectedIds.has(String(r.roleId)) }">
              <span v-if="selectedIds.has(String(r.roleId))" class="check">✓</span>
            </div>
          </div>

          <div class="card-img">
            <img v-if="(r as any).roleAvatar" :src="(r as any).roleAvatar" alt="" />
            <div v-else class="avatar-fallback">🎭</div>
            <div v-if="isPublished(r)" class="badge">已发布</div>
          </div>
          <div class="card-name">{{ r.roleName || ('role-' + r.roleId) }}</div>
        </button>
      </div>
    </div>

    <div class="tabbar">
      <button class="tab active" @click="$router.push('/home')">🏠</button>
      <button class="tab" @click="$router.push('/devices')">🛠️</button>
      <button class="tab" @click="$router.push('/console')">💬</button>
      <button class="tab" @click="$router.push('/me')">👤</button>
    </div>
  </div>
</template>

<style scoped>
.page {
  height: 100vh;
  background: #fff;
  font-family: ui-sans-serif, system-ui;
  display: flex;
  flex-direction: column;
}

.fixed-header {
  padding: 18px 16px 10px;
}

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
  min-width: 0;
}

.device-name {
  font-size: 14px;
  font-weight: 700;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 160px;
}

.device-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: #bbb;
}

.dot.on { background: #22c55e; }

.status-text {
  font-size: 12px;
  opacity: 0.75;
}

.device-select {
  border: none;
  outline: none;
  background: #f3f3f3;
  border-radius: 14px;
  padding: 10px 12px;
  font-size: 14px;
  max-width: 160px;
}

.section-title {
  display: inline-block;
  font-size: 18px;
  font-weight: 800;
  background: #f3f3f3;
  padding: 10px 14px;
  border-radius: 16px;
}

.actions {
  display: inline-flex;
  gap: 10px;
}

.icon-btn {
  width: 44px;
  height: 44px;
  border-radius: 14px;
  border: 1px solid #eee;
  background: #fafafa;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.icon-btn.on {
  border-color: #111;
  background: #f0f0f0;
}

.icon { font-size: 18px; }

.modebar {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 12px;
  margin: 8px 0 10px;
}

.text-btn {
  border: none;
  background: transparent;
  font-size: 14px;
}

.danger-btn {
  border: none;
  background: #e74c3c;
  color: #fff;
  padding: 10px 14px;
  border-radius: 14px;
  font-size: 14px;
}

.danger-btn:disabled { opacity: 0.5; }

.primary-btn {
  border: none;
  background: #111;
  color: #fff;
  padding: 10px 14px;
  border-radius: 14px;
  font-size: 14px;
}

.search {
  display: flex;
  align-items: center;
  gap: 10px;
  background: #f3f3f3;
  border-radius: 18px;
  padding: 12px 14px;
}

.search-icon { font-size: 18px; opacity: 0.6; }

.search-input {
  border: none;
  outline: none;
  background: transparent;
  font-size: 16px;
  width: 100%;
}

.scroll-area {
  flex: 1;
  overflow-y: auto;
  padding: 10px 16px 88px;
}

.grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.card {
  border: none;
  background: transparent;
  text-align: left;
  position: relative;
}

.card-img {
  width: 100%;
  aspect-ratio: 1 / 1;
  border-radius: 20px;
  background: #f3f3f3;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  position: relative;
}

.card-img img { width: 100%; height: 100%; object-fit: cover; }

.avatar-fallback { font-size: 34px; opacity: 0.45; }

.badge {
  position: absolute;
  left: 10px;
  top: 10px;
  background: rgba(0,0,0,0.6);
  color: #fff;
  padding: 4px 8px;
  border-radius: 999px;
  font-size: 12px;
}

.placeholder {
  background: #f3f3f3;
}

.plus-box {
  width: 72px;
  height: 72px;
  border-radius: 18px;
  border: 2px dashed #d6d6d6;
  display: flex;
  align-items: center;
  justify-content: center;
}

.plus { font-size: 36px; opacity: 0.6; }

.card-name {
  margin-top: 10px;
  font-size: 18px;
  font-weight: 700;
  text-align: center;
}

.select-box {
  position: absolute;
  right: 8px;
  top: 8px;
  z-index: 2;
}

.checkbox {
  width: 22px;
  height: 22px;
  border-radius: 999px;
  border: 2px solid #bbb;
  background: rgba(255,255,255,0.9);
  display: flex;
  align-items: center;
  justify-content: center;
}

.checkbox.checked {
  border-color: #111;
  background: #111;
}

.check {
  color: #fff;
  font-size: 14px;
  line-height: 1;
}

.tabbar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  height: 72px;
  background: #fff;
  border-top: 1px solid #eee;
  display: flex;
  align-items: center;
  justify-content: space-around;
}

.tab {
  width: 54px;
  height: 54px;
  border-radius: 18px;
  border: none;
  background: transparent;
  font-size: 22px;
}

.tab.active {
  background: #f3f3f3;
}
</style>
