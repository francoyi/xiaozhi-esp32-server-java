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
  <div>
    <h2 style="margin:8px 0">Roles</h2>

    <div style="display:flex;gap:8px;margin-bottom:12px;flex-wrap:wrap">
      <button @click="openCreate">New Role</button>
      <button @click="loadAll" :disabled="state==='loading'">Refresh</button>
    </div>

    <div v-if="state==='loading'">Loading...</div>
    <div v-else-if="state==='error'" style="color:crimson">Error: {{ errorMsg }}</div>

    <table style="width:100%;border-collapse:collapse">
      <thead>
        <tr style="text-align:left;border-bottom:1px solid #ddd">
          <th style="padding:8px">roleId</th>
          <th style="padding:8px">roleName</th>
          <th style="padding:8px">modelId</th>
          <th style="padding:8px">ttsId</th>
          <th style="padding:8px">sttId</th>
          <th style="padding:8px">actions</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="r in roles" :key="String(r.roleId)" style="border-bottom:1px solid #f0f0f0">
          <td style="padding:8px;font-family:ui-monospace,SFMono-Regular">{{ r.roleId }}</td>
          <td style="padding:8px">{{ r.roleName || '-' }}</td>
          <td style="padding:8px">{{ r.modelId ?? '-' }}</td>
          <td style="padding:8px">{{ r.ttsId ?? '-' }}</td>
          <td style="padding:8px">{{ r.sttId ?? '-' }}</td>
          <td style="padding:8px;display:flex;gap:8px;flex-wrap:wrap">
            <button @click="openEdit(r)">Edit</button>
            <button @click="onDelete(r.roleId)">Delete</button>
          </td>
        </tr>

        <tr v-if="roles.length===0 && state!=='loading'">
          <td colspan="6" style="padding:12px;opacity:.7">暂无角色</td>
        </tr>
      </tbody>
    </table>

    <div v-if="showEditor"
         style="position:fixed;inset:0;background:rgba(0,0,0,.35);display:flex;align-items:center;justify-content:center;padding:16px">
      <div style="background:#fff;border-radius:10px;max-width:920px;width:100%;padding:16px">
        <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:12px">
          <strong>{{ editorTitle }}</strong>
          <button @click="showEditor=false">X</button>
        </div>

        <div style="display:grid;grid-template-columns:1fr 1fr;gap:10px">
          <label>
            <div style="font-size:12px;opacity:.8">roleName *</div>
            <input v-model="form.roleName" style="width:100%;padding:8px" />
          </label>

          <label>
            <div style="font-size:12px;opacity:.8">roleDesc</div>
            <input v-model="form.roleDesc" style="width:100%;padding:8px" />
          </label>

          <label>
            <div style="font-size:12px;opacity:.8">modelId (LLM Config)</div>
            <select v-model="form.modelId" style="width:100%;padding:8px">
              <option :value="null">-- none --</option>
              <option v-for="c in llmConfigs" :key="String(c.configId)" :value="c.configId">
                {{ configLabel(c) }}
              </option>
            </select>
          </label>

          <label>
            <div style="font-size:12px;opacity:.8">ttsId (TTS Config)</div>
            <select v-model="form.ttsId" style="width:100%;padding:8px">
              <option :value="null">-- none --</option>
              <option v-for="c in ttsConfigs" :key="String(c.configId)" :value="c.configId">
                {{ configLabel(c) }}
              </option>
            </select>
          </label>

          <label>
            <div style="font-size:12px;opacity:.8">sttId (STT Config)</div>
            <select v-model="form.sttId" style="width:100%;padding:8px">
              <option :value="null">-- none --</option>
              <option v-for="c in sttConfigs" :key="String(c.configId)" :value="c.configId">
                {{ configLabel(c) }}
              </option>
            </select>
          </label>

          <label>
            <div style="font-size:12px;opacity:.8">temperature</div>
            <input v-model.number="form.temperature" type="number" step="0.1" style="width:100%;padding:8px" />
          </label>

          <label>
            <div style="font-size:12px;opacity:.8">topP</div>
            <input v-model.number="form.topP" type="number" step="0.05" style="width:100%;padding:8px" />
          </label>

          <label>
            <div style="font-size:12px;opacity:.8">voiceName</div>
            <input v-model="form.voiceName" style="width:100%;padding:8px" />
          </label>

          <label>
            <div style="font-size:12px;opacity:.8">ttsSpeed</div>
            <input v-model.number="form.ttsSpeed" type="number" step="0.1" style="width:100%;padding:8px" />
          </label>

          <label>
            <div style="font-size:12px;opacity:.8">ttsPitch</div>
            <input v-model.number="form.ttsPitch" type="number" step="0.1" style="width:100%;padding:8px" />
          </label>

          <label style="grid-column:1 / -1">
            <div style="font-size:12px;opacity:.8">systemPrompt</div>
            <textarea v-model="form.systemPrompt" rows="4" style="width:100%;padding:8px"></textarea>
          </label>
        </div>

        <div style="display:flex;gap:8px;justify-content:flex-end;margin-top:12px">
          <button @click="save">Save</button>
        </div>

        <div style="margin-top:10px;font-size:12px;opacity:.75">
          保存 Role 后，去 <b>Devices</b> 页面把设备的 roleId 切到这个 roleId，端侧重连后生效。
        </div>
      </div>
    </div>
  </div>
</template>
