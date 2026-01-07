<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import type { ConfigDTO, RoleAddOrUpdateParam, RoleDTO } from '../../utils/types'
import { queryConfigs } from '../../services/config'
import { addRole, deleteRole, queryRoles, updateRole } from '../../services/role'

type LoadState = 'idle' | 'loading' | 'error'

const state = ref<LoadState>('idle')
const errorMsg = ref('')
const roles = ref<RoleDTO[]>([])
const configs = ref<ConfigDTO[]>([])

const llmConfigs = computed(() => configs.value.filter(c => (c.configType || '').toLowerCase() === 'llm'))
const ttsConfigs = computed(() => configs.value.filter(c => (c.configType || '').toLowerCase() === 'tts'))
const sttConfigs = computed(() => configs.value.filter(c => (c.configType || '').toLowerCase() === 'stt'))

async function loadAll() {
  state.value = 'loading'
  errorMsg.value = ''
  try {
    const [rRes, cRes] = await Promise.all([
      queryRoles({ pageNum: 1, pageSize: 100 }),
      queryConfigs({ pageNum: 1, pageSize: 200 }),
    ])
    roles.value = rRes?.data?.list ?? []
    configs.value = cRes?.data?.list ?? []
    state.value = 'idle'
  } catch (e: any) {
    state.value = 'error'
    errorMsg.value = e?.message || 'Load failed'
  }
}

onMounted(loadAll)

const showEditor = ref(false)
const editing = ref<RoleDTO | null>(null)
const form = reactive<RoleAddOrUpdateParam>({
  roleName: '',
  roleDesc: '',
  modelId: null,
  ttsId: null,
  sttId: null,
  temperature: null,
  topP: null,
  voiceName: '',
  ttsSpeed: null,
  ttsPitch: null,
  systemPrompt: '',
})

function openCreate() {
  editing.value = null
  Object.assign(form, {
    roleName: '',
    roleDesc: '',
    modelId: null,
    ttsId: null,
    sttId: null,
    temperature: null,
    topP: null,
    voiceName: '',
    ttsSpeed: null,
    ttsPitch: null,
    systemPrompt: '',
  })
  showEditor.value = true
}

function openEdit(row: RoleDTO) {
  editing.value = row
  Object.assign(form, {
    roleName: row.roleName || '',
    roleDesc: row.roleDesc || '',
    modelId: row.modelId ?? null,
    ttsId: row.ttsId ?? null,
    sttId: row.sttId ?? null,
    temperature: (row as any).temperature ?? null,
    topP: (row as any).topP ?? null,
    voiceName: (row as any).voiceName ?? '',
    ttsSpeed: (row as any).ttsSpeed ?? null,
    ttsPitch: (row as any).ttsPitch ?? null,
    systemPrompt: (row as any).systemPrompt ?? '',
  })
  showEditor.value = true
}

const editorTitle = computed(() => (editing.value ? 'Edit Role' : 'Create Role'))

async function save() {
  if (!form.roleName?.trim()) return alert('roleName 必填')
  try {
    if (editing.value) {
      await updateRole(editing.value.roleId, { ...form })
    } else {
      await addRole({ ...form })
    }
    showEditor.value = false
    await loadAll()
    alert('保存成功')
  } catch (e: any) {
    alert(e?.message || '保存失败')
  }
}

async function onDelete(roleId: string | number) {
  if (!confirm(`确定删除 role ${roleId}？`)) return
  try {
    await deleteRole(roleId)
    await loadAll()
  } catch (e: any) {
    alert(e?.message || '删除失败')
  }
}

function configLabel(c: ConfigDTO) {
  const id = c.configId
  const name = c.configName || ''
  const provider = c.provider || ''
  const model = (c as any).modelName || (c as any).model || ''
  return `${id} | ${name} | ${provider}${model ? ' | ' + model : ''}`
}
</script>

<template>
  <!-- ✅ page 作为滚动容器，并预留底部 tabbar 空间 -->
  <div class="page">
    <h2 class="title">Roles</h2>

    <div class="toolbar">
      <button @click="openCreate">New Role</button>
      <button @click="loadAll" :disabled="state==='loading'">Refresh</button>
    </div>

    <div v-if="state==='loading'">Loading...</div>
    <div v-else-if="state==='error'" class="error">Error: {{ errorMsg }}</div>

    <!-- ✅ 内容区 -->
    <div class="content">
      <table class="table">
        <thead>
        <tr>
          <th>roleId</th>
          <th>roleName</th>
          <th>modelId</th>
          <th>ttsId</th>
          <th>sttId</th>
          <th>actions</th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="r in roles" :key="String(r.roleId)">
          <td class="mono">{{ r.roleId }}</td>
          <td>{{ r.roleName || '-' }}</td>
          <td>{{ r.modelId ?? '-' }}</td>
          <td>{{ r.ttsId ?? '-' }}</td>
          <td>{{ r.sttId ?? '-' }}</td>
          <td class="actions">
            <button @click="openEdit(r)">Edit</button>
            <button @click="onDelete(r.roleId)">Delete</button>
          </td>
        </tr>

        <tr v-if="roles.length===0 && state!=='loading'">
          <td colspan="6" class="empty">暂无角色</td>
        </tr>
        </tbody>
      </table>
    </div>

    <!-- ✅ 编辑弹窗 -->
    <div v-if="showEditor" class="mask">
      <div class="modal">
        <div class="modal-header">
          <strong>{{ editorTitle }}</strong>
          <button @click="showEditor=false">X</button>
        </div>

        <div class="form-grid">
          <label>
            <div class="label">roleName *</div>
            <input v-model="form.roleName" class="input" />
          </label>

          <label>
            <div class="label">roleDesc</div>
            <input v-model="form.roleDesc" class="input" />
          </label>

          <label>
            <div class="label">modelId (LLM Config)</div>
            <select v-model="form.modelId" class="input">
              <option :value="null">-- none --</option>
              <option v-for="c in llmConfigs" :key="String(c.configId)" :value="c.configId">
                {{ configLabel(c) }}
              </option>
            </select>
          </label>

          <label>
            <div class="label">ttsId (TTS Config)</div>
            <select v-model="form.ttsId" class="input">
              <option :value="null">-- none --</option>
              <option v-for="c in ttsConfigs" :key="String(c.configId)" :value="c.configId">
                {{ configLabel(c) }}
              </option>
            </select>
          </label>

          <label>
            <div class="label">sttId (STT Config)</div>
            <select v-model="form.sttId" class="input">
              <option :value="null">-- none --</option>
              <option v-for="c in sttConfigs" :key="String(c.configId)" :value="c.configId">
                {{ configLabel(c) }}
              </option>
            </select>
          </label>

          <label>
            <div class="label">temperature</div>
            <input v-model.number="form.temperature" type="number" step="0.1" class="input" />
          </label>

          <label>
            <div class="label">topP</div>
            <input v-model.number="form.topP" type="number" step="0.05" class="input" />
          </label>

          <label>
            <div class="label">voiceName</div>
            <input v-model="form.voiceName" class="input" />
          </label>

          <label>
            <div class="label">ttsSpeed</div>
            <input v-model.number="form.ttsSpeed" type="number" step="0.1" class="input" />
          </label>

          <label>
            <div class="label">ttsPitch</div>
            <input v-model.number="form.ttsPitch" type="number" step="0.1" class="input" />
          </label>

          <label class="full">
            <div class="label">systemPrompt</div>
            <textarea v-model="form.systemPrompt" rows="4" class="input"></textarea>
          </label>
        </div>

        <div class="modal-actions">
          <button @click="save">Save</button>
        </div>

        <div class="hint">
          保存 Role 后，去 <b>Devices</b> 页面把设备的 roleId 切到这个 roleId，端侧重连后生效。
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* ✅ 你底部 tabbar 多高，就填多高；先用 64px，够用 */
.page {
  --tabbar-h: 64px;

  height: 100vh;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;

  /* ✅ 关键：给 fixed tabbar + iPhone 安全区让位 */
  padding: 12px 12px calc(var(--tabbar-h) + env(safe-area-inset-bottom)) 12px;
  box-sizing: border-box;
}

.title {
  margin: 8px 0;
}

.toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.error {
  color: crimson;
}

.content {
  width: 100%;
}

.table {
  width: 100%;
  border-collapse: collapse;
}

.table thead tr {
  text-align: left;
  border-bottom: 1px solid #ddd;
}

.table th,
.table td {
  padding: 8px;
  border-bottom: 1px solid #f0f0f0;
}

.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace;
}

.actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.empty {
  padding: 12px;
  opacity: 0.7;
}

/* 弹窗 */
.mask {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,.35);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
}

.modal {
  background: #fff;
  border-radius: 10px;
  max-width: 920px;
  width: 100%;
  padding: 16px;
  max-height: calc(100vh - 32px);
  overflow: auto;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

@media (max-width: 640px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
}

.label {
  font-size: 12px;
  opacity: 0.8;
  margin-bottom: 4px;
}

.input {
  width: 100%;
  padding: 8px;
  box-sizing: border-box;
}

.full {
  grid-column: 1 / -1;
}

.modal-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
  margin-top: 12px;
}

.hint {
  margin-top: 10px;
  font-size: 12px;
  opacity: 0.75;
}
</style>
