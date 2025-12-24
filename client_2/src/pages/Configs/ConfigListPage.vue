<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import type { ConfigAddOrUpdateParam, ConfigDTO } from '../../utils/types'
import { addConfig, getModels, queryConfigs, updateConfig } from '../../services/config'

type LoadState = 'idle' | 'loading' | 'error'

const state = ref<LoadState>('idle')
const errorMsg = ref('')
const items = ref<ConfigDTO[]>([])

const pageNum = ref(1)
const pageSize = ref(50)

async function load() {
  state.value = 'loading'
  errorMsg.value = ''
  try {
    const res = await queryConfigs({ pageNum: pageNum.value, pageSize: pageSize.value })
    items.value = res?.data?.list ?? []
    state.value = 'idle'
  } catch (e: any) {
    state.value = 'error'
    errorMsg.value = e?.message || 'Load failed'
  }
}

onMounted(load)

const showEditor = ref(false)
const editing = ref<ConfigDTO | null>(null)
const form = reactive<ConfigAddOrUpdateParam>({
  configName: '',
  configType: 'llm',
  provider: '',
  baseUrl: '',
  apiKey: '',
  token: '',
  model: '',
  modelName: '',
  remark: '',
})

function openCreate() {
  editing.value = null
  Object.assign(form, {
    configName: '',
    configType: 'llm',
    provider: '',
    baseUrl: '',
    apiKey: '',
    token: '',
    model: '',
    modelName: '',
    remark: '',
  })
  showEditor.value = true
}

function openEdit(row: ConfigDTO) {
  editing.value = row
  Object.assign(form, {
    configName: row.configName || '',
    configType: row.configType || 'llm',
    provider: row.provider || '',
    baseUrl: row.baseUrl || '',
    apiKey: row.apiKey || '',
    token: (row as any).token || '',
    model: (row as any).model || '',
    modelName: (row as any).modelName || '',
    remark: row.remark || '',
  })
  showEditor.value = true
}

const editorTitle = computed(() => (editing.value ? 'Edit Config' : 'Create Config'))

async function save() {
  try {
    if (editing.value) {
      await updateConfig(editing.value.configId, { ...form })
    } else {
      await addConfig({ ...form })
    }
    showEditor.value = false
    await load()
    alert('保存成功')
  } catch (e: any) {
    alert(e?.message || '保存失败')
  }
}

const modelsText = ref('')
async function onGetModels() {
  try {
    const res = await getModels({
      provider: form.provider,
      baseUrl: form.baseUrl,
      apiKey: form.apiKey,
      token: form.token,
    })
    modelsText.value = JSON.stringify(res?.data ?? res, null, 2)
  } catch (e: any) {
    alert(e?.message || 'getModels 失败')
  }
}
</script>

<template>
  <div>
    <h2 style="margin:8px 0">Configs</h2>

    <div style="display:flex;gap:8px;margin-bottom:12px;flex-wrap:wrap">
      <button @click="openCreate">New Config</button>
      <button @click="load" :disabled="state==='loading'">Refresh</button>
    </div>

    <div v-if="state==='loading'">Loading...</div>
    <div v-else-if="state==='error'" style="color:crimson">Error: {{ errorMsg }}</div>

    <table style="width:100%;border-collapse:collapse">
      <thead>
        <tr style="text-align:left;border-bottom:1px solid #ddd">
          <th style="padding:8px">configId</th>
          <th style="padding:8px">name</th>
          <th style="padding:8px">type</th>
          <th style="padding:8px">provider</th>
          <th style="padding:8px">model</th>
          <th style="padding:8px">baseUrl</th>
          <th style="padding:8px">actions</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="c in items" :key="String(c.configId)" style="border-bottom:1px solid #f0f0f0">
          <td style="padding:8px;font-family:ui-monospace,SFMono-Regular">{{ c.configId }}</td>
          <td style="padding:8px">{{ c.configName || '-' }}</td>
          <td style="padding:8px">{{ c.configType || '-' }}</td>
          <td style="padding:8px">{{ c.provider || '-' }}</td>
          <td style="padding:8px">{{ (c as any).modelName || (c as any).model || '-' }}</td>
          <td style="padding:8px">{{ c.baseUrl || '-' }}</td>
          <td style="padding:8px;display:flex;gap:8px">
            <button @click="openEdit(c)">Edit</button>
          </td>
        </tr>

        <tr v-if="items.length===0 && state!=='loading'">
          <td colspan="7" style="padding:12px;opacity:.7">暂无配置</td>
        </tr>
      </tbody>
    </table>

    <div v-if="showEditor"
         style="position:fixed;inset:0;background:rgba(0,0,0,.35);display:flex;align-items:center;justify-content:center;padding:16px">
      <div style="background:#fff;border-radius:10px;max-width:860px;width:100%;padding:16px">
        <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:12px">
          <strong>{{ editorTitle }}</strong>
          <button @click="showEditor=false">X</button>
        </div>

        <div style="display:grid;grid-template-columns:1fr 1fr;gap:10px">
          <label>
            <div style="font-size:12px;opacity:.8">configName</div>
            <input v-model="form.configName" style="width:100%;padding:8px" />
          </label>

          <label>
            <div style="font-size:12px;opacity:.8">configType</div>
            <select v-model="form.configType" style="width:100%;padding:8px">
              <option value="llm">llm</option>
              <option value="tts">tts</option>
              <option value="stt">stt</option>
              <option value="agent">agent</option>
              <option value="other">other</option>
            </select>
          </label>

          <label>
            <div style="font-size:12px;opacity:.8">provider</div>
            <input v-model="form.provider" placeholder="openai / coze / dify ..." style="width:100%;padding:8px" />
          </label>

          <label>
            <div style="font-size:12px;opacity:.8">baseUrl</div>
            <input v-model="form.baseUrl" placeholder="https://..." style="width:100%;padding:8px" />
          </label>

          <label>
            <div style="font-size:12px;opacity:.8">apiKey</div>
            <input v-model="form.apiKey" placeholder="(可选)" style="width:100%;padding:8px" />
          </label>

          <label>
            <div style="font-size:12px;opacity:.8">token</div>
            <input v-model="form.token" placeholder="(可选)" style="width:100%;padding:8px" />
          </label>

          <label>
            <div style="font-size:12px;opacity:.8">model</div>
            <input v-model="form.model" placeholder="(可选)" style="width:100%;padding:8px" />
          </label>

          <label>
            <div style="font-size:12px;opacity:.8">modelName</div>
            <input v-model="form.modelName" placeholder="(可选)" style="width:100%;padding:8px" />
          </label>

          <label style="grid-column:1 / -1">
            <div style="font-size:12px;opacity:.8">remark</div>
            <textarea v-model="form.remark" rows="3" style="width:100%;padding:8px"></textarea>
          </label>
        </div>

        <div style="display:flex;gap:8px;justify-content:flex-end;margin-top:12px;flex-wrap:wrap">
          <button @click="onGetModels">Get Models</button>
          <button @click="save">Save</button>
        </div>

        <pre v-if="modelsText" style="margin-top:12px;max-height:260px;overflow:auto;background:#f6f6f6;padding:10px;border-radius:8px">{{ modelsText }}</pre>
      </div>
    </div>
  </div>
</template>
