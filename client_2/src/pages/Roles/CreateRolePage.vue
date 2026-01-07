<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import type { ConfigDTO, RoleAddOrUpdateParam, TemplateDTO } from '../../utils/types'
import { queryConfigs } from '../../services/config'
import { queryTemplates } from '../../services/template'
import { addRole } from '../../services/role'

type VoiceOption = { label: string; value: string; lang?: string }

const FALLBACK_VOICES: VoiceOption[] = [
  { label: 'Xiaoxiao (zh-CN)', value: 'zh-CN-XiaoxiaoNeural', lang: 'zh-CN' },
  { label: 'Xiaoyi (zh-CN)', value: 'zh-CN-XiaoyiNeural', lang: 'zh-CN' },
  { label: 'Yunjian (zh-CN)', value: 'zh-CN-YunjianNeural', lang: 'zh-CN' },
  { label: 'Yunxi (zh-CN)', value: 'zh-CN-YunxiNeural', lang: 'zh-CN' },
  { label: 'Yunxia (zh-CN)', value: 'zh-CN-YunxiaNeural', lang: 'zh-CN' },
  { label: 'Yunyang (zh-CN)', value: 'zh-CN-YunyangNeural', lang: 'zh-CN' },
]

const router = useRouter()

// ====== STT（语音识别）选择：固定包含 Vosk（本地） + 追加数据库 STT 配置 ======
function isVoskConfig(c: ConfigDTO): boolean {
  const p = (c.provider || '').toLowerCase()
  const n = (c.configName || '').toLowerCase()
  const d = (c.configDesc || '').toLowerCase()
  return p.includes('vosk') || n.includes('vosk') || d.includes('vosk')
}

const sttSelectValue = ref<string>('vosk')

type LoadState = 'idle' | 'loading' | 'error'
const state = ref<LoadState>('idle')
const errorMsg = ref('')

const configs = ref<ConfigDTO[]>([])
const templates = ref<TemplateDTO[]>([])

const llmConfigs = computed(() => configs.value.filter(c => (c.configType || '').toLowerCase() === 'llm'))
// Client 端不提供“语音合成(TTS)配置”选择（保持上一版 UI）
// const ttsConfigs = computed(() => configs.value.filter(c => (c.configType || '').toLowerCase() === 'tts'))
const sttConfigs = computed(() => configs.value.filter(c => (c.configType || '').toLowerCase() === 'stt'))

const voskConfigId = computed(() => {
  const hit = sttConfigs.value.find(isVoskConfig)
  return hit?.configId != null ? String(hit.configId) : ''
})

const sttExtraConfigs = computed(() => sttConfigs.value.filter(c => !isVoskConfig(c)))

// 音色列表（对齐 web 端：使用 edge-tts 的 ShortName / voiceId）
const voiceOptions = ref<VoiceOption[]>([])

async function loadEdgeVoices() {
  try {
    const res = await fetch('/static/assets/edgeVoicesList.json')
    if (!res.ok) throw new Error('load edge voices failed')
    const data = await res.json()

    // 兼容 edgeVoicesList.json 结构
    // 只取中文语音，并将 value 设为 ShortName（edge-tts 只认这个）
    const list: VoiceOption[] = (Array.isArray(data) ? data : [])
      .filter((v: any) => typeof v?.Locale === 'string' && v.Locale.includes('zh') && typeof v?.ShortName === 'string')
      .sort((a: any, b: any) => String(a.Locale).localeCompare(String(b.Locale)))
      .map((v: any) => {
        const parts = String(v.ShortName).split('-')
        let name = parts[2] || ''
        if (name.endsWith('Neural')) name = name.slice(0, -6)
        return { label: `${name} (${v.Locale})`, value: String(v.ShortName), lang: String(v.Locale) }
      })

    voiceOptions.value = list.length ? list : [...FALLBACK_VOICES]
  } catch {
    voiceOptions.value = [...FALLBACK_VOICES]
  }

  // 默认值
  if (!form.voiceName) {
    form.voiceName = voiceOptions.value[0]?.value || ''
  }
}

// 表单：保持与 CreateRolePage 字段一致
const form = reactive<RoleAddOrUpdateParam>({
  roleName: '',
  roleDesc: '',
  modelId: null,
  ttsId: null,
  sttId: null,
  voiceName: '',
  systemPrompt: '',
  // UI 字段：记忆类型（0=window）
  memoryTypeUi: 0 as any,
})

watch(sttSelectValue, (v) => {
  if (v === 'vosk') {
    form.sttId = voskConfigId.value ? Number(voskConfigId.value) : null
  } else {
    form.sttId = v ? Number(v) : null
  }
}, { immediate: true })

const selectedTemplateId = ref<string>('')

watch(selectedTemplateId, (id) => {
  if (!id) return
  const t = templates.value.find(x => String(x.templateId) === String(id))
  // 兼容字段：content / promptTemplate / templateContent
  const content = (t as any)?.content ?? (t as any)?.promptTemplate ?? (t as any)?.templateContent ?? ''
  if (String(content).trim()) form.systemPrompt = String(content)
})

const canSubmit = computed(() => !!String(form.roleName || '').trim() && form.modelId != null)

function configLabel(c: ConfigDTO) {
  const id = c.configId
  const name = c.configName || ''
  const provider = c.provider || ''
  const model = (c as any).modelName || (c as any).model || ''
  return `${id} | ${name}${provider ? ' | ' + provider : ''}${model ? ' | ' + model : ''}`
}

async function loadAll() {
  state.value = 'loading'
  errorMsg.value = ''
  try {    await loadEdgeVoices()
    const [cRes, tRes] = await Promise.all([
      queryConfigs({ pageNum: 1, pageSize: 200 }),
      queryTemplates({ pageNum: 1, pageSize: 200 }),
    ])
    configs.value = cRes?.data?.list ?? []
    templates.value = tRes?.data?.list ?? []

    state.value = 'idle'
  } catch (e: any) {
    state.value = 'error'
    errorMsg.value = e?.message || '加载失败'
  }
}


async function onCreate() {
  if (!canSubmit.value) return
  try {
    const payload: any = { ...form }
    payload.memoryType = Number(payload.memoryTypeUi || 0)
    delete payload.memoryTypeUi
    // 对齐 web 端：edge 语音使用 ttsId = -1（后端会落库为 null，表示 edge）
    payload.ttsId = -1
    await addRole(payload)
    alert('已创建')
    router.replace('/home')
  } catch (e: any) {
    alert(e?.message || '创建失败')
  }
}

onMounted(loadAll)
onUnmounted(() => {})
</script>

<template>
  <div class="page">
    <div class="header">
      <button class="back" type="button" @click="router.back()">←</button>
      <div class="title">创建角色</div>
      <div class="spacer" />
    </div>

    <div v-if="state === 'loading'" class="hint">加载中…</div>
    <div v-else-if="state === 'error'" class="hint error">{{ errorMsg }}</div>

    <div v-else class="form">
      <div class="field">
        <div class="label">名称</div>
        <input v-model="form.roleName" class="input" placeholder="请输入角色名称" />
      </div>

      <div class="field">
        <div class="label">描述</div>
        <textarea v-model="form.roleDesc" class="textarea" placeholder="请输入角色描述" />
      </div>

      <div class="field">
        <div class="label">系统提示词</div>
        <textarea v-model="form.systemPrompt" class="textarea" placeholder="请输入 system prompt" />
        <div class="sub">
          <select v-model="selectedTemplateId" class="select">
            <option value="">从模板填充（可选）</option>
            <option v-for="t in templates" :key="String(t.templateId)" :value="String(t.templateId)">
              {{ t.templateName || t.templateId }}
            </option>
          </select>
        </div>
      </div>

      <div class="field">
        <div class="label">对话模型（LLM）</div>
        <select v-model="form.modelId" class="select">
          <option :value="null">请选择</option>
          <option v-for="c in llmConfigs" :key="String(c.configId)" :value="Number(c.configId)">
            {{ configLabel(c) }}
          </option>
        </select>
      </div>

      <!-- 语音合成（TTS）配置：Client 端不暴露选择项（保留数据库中的既有值即可） -->

      <div class="field">
        <div class="label">语音识别（STT）</div>
        <select v-model="sttSelectValue" class="select">
          <option value="vosk">Vosk（本地）</option>
          <option v-for="c in sttExtraConfigs" :key="String(c.configId)" :value="String(c.configId)">
            {{ configLabel(c) }}
          </option>
        </select>
      </div>

      <div class="field">
        <div class="label">音色</div>
        <select v-model="form.voiceName" class="select">
          <option value="">请选择音色</option>
          <option v-for="v in voiceOptions" :key="v.value" :value="v.value">
            {{ v.label }}
          </option>
        </select>
      </div>

      <div class="field">
        <div class="label">记忆类型</div>
        <select v-model="(form as any).memoryTypeUi" class="select">
          <option :value="0">window（短期记忆）</option>
          <option :value="1">summary（摘要记忆）</option>
          <option :value="2">long（长期记忆）</option>
        </select>
      </div>

      <div class="footer">
        <button class="primary" type="button" :disabled="!canSubmit" @click="onCreate">完成创建</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* Page layout */
.page {
  height: 100vh;
  background: #fff;
  font-family: ui-sans-serif, system-ui;

  display: flex;
  flex-direction: column;
  overflow: hidden;

  /* 给 fixed footer 让位（宁可多一点） */
  --footer-safe: calc(110px + env(safe-area-inset-bottom, 0px));
}

/* Header */
.header {
  position: sticky;
  top: 0;
  z-index: 10;
  background: #fff;

  display: flex;
  align-items: center;
  gap: 10px;

  padding: 14px 16px;
  border-bottom: 1px solid #f0f0f0;
}

.back {
  width: 36px;
  height: 36px;
  border-radius: 12px;
  border: 1px solid #eee;
  background: #fafafa;
}

.title {
  font-size: 18px;
  font-weight: 700;
}

.spacer {
  flex: 1;
}

/* States */
.hint {
  padding: 18px 16px;
  color: #666;
}

.hint.error {
  color: #c62828;
}

/* Form scroll area */
.form {
  flex: 1;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;

  padding: 14px 16px var(--footer-safe);
}

/* Fields */
.field {
  margin-bottom: 14px;
}

.label {
  font-size: 14px;
  font-weight: 700;
  margin-bottom: 8px;
}

/* Inputs (iOS: font-size >= 16px to avoid auto-zoom) */
.input,
.textarea,
.select {
  width: 100%;
  box-sizing: border-box;

  border: 1px solid #eee;
  border-radius: 14px;
  background: #fafafa;

  padding: 12px 14px;
  font-size: 16px;
  line-height: 1.2;
  outline: none;

  -webkit-text-size-adjust: 100%;
}

.textarea {
  min-height: 92px;
  resize: vertical;
}

.sub {
  margin-top: 8px;
}

/* Fixed footer */
.footer {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;

  background: #fff;
  border-top: 1px solid #f0f0f0;

  display: flex;
  gap: 12px;

  padding: 14px 16px;
  padding-bottom: calc(14px + env(safe-area-inset-bottom, 0px));
}

.danger {
  flex: 1;
  height: 44px;
  border-radius: 14px;
  border: none;

  background: #ffe9e9;
  color: #c62828;
  font-weight: 700;
}

.primary {
  flex: 2;
  height: 44px;
  border-radius: 14px;
  border: none;

  background: #111;
  color: #fff;
  font-weight: 700;
}

.primary:disabled {
  opacity: 0.4;
}
</style>

