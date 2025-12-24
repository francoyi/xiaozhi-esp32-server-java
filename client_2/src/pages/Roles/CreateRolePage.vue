<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import api from '../../services/api'
import { request } from '../../services/request'
import { authStore } from '../../store/auth'
import type { ConfigDTO, RoleAddOrUpdateParam, TemplateDTO } from '../../utils/types'
import { queryConfigs } from '../../services/config'
import { queryTemplates } from '../../services/template'
import { addRole } from '../../services/role'

type VoiceOption = { label: string; value: string; lang?: string }

// 在一些手机浏览器里，speechSynthesis.getVoices() 可能返回空数组（需要等待 onvoiceschanged）
// 这里提供一个兜底列表：允许用户选择并保存 voiceName。
// Edge/Windows 常见音色名参考：Xiaoxiao / Xiaoyi / Yunxi 等。
const FALLBACK_VOICES: VoiceOption[] = [
  { label: 'Xiaoxiao (zh-CN)', value: 'zh-CN-XiaoxiaoNeural', lang: 'zh-CN' },
  { label: 'Xiaoyi (zh-CN)', value: 'zh-CN-XiaoyiNeural', lang: 'zh-CN' },
  { label: 'Yunjian (zh-CN)', value: 'zh-CN-YunjianNeural', lang: 'zh-CN' },
  { label: 'Yunxi (zh-CN)', value: 'zh-CN-YunxiNeural', lang: 'zh-CN' },
  { label: 'Yunxia (zh-CN)', value: 'zh-CN-YunxiaNeural', lang: 'zh-CN' },
  { label: 'Yunyang (zh-CN)', value: 'zh-CN-YunyangNeural', lang: 'zh-CN' },
]

const router = useRouter()

function pickUserId(body: any): string {
  const candidates = [
    body?.data?.userId,
    body?.data?.id,
    body?.data?.loginId,
    body?.data?.userInfo?.userId,
    body?.data?.userInfo?.id,
    body?.data?.saTokenInfo?.loginId,
    body?.userId,
    body?.id,
    body?.loginId,
    body?.username,
    body?.userName,
    body?.data?.username,
    body?.data?.userName,
  ]

  const hit = candidates.find(v => v !== undefined && v !== null && String(v).trim() !== '')
  return hit != null ? String(hit) : ''
}

async function ensureUserId(): Promise<string> {
  const cached = authStore.getUserId?.()
  if (cached && String(cached).trim() !== '') return String(cached)

  try {
    const res = await request.get(api.user.checkToken)
    const body = res?.data
    console.log('check-token raw:', body)

    const uid = pickUserId(body)
    if (uid) authStore.setUserId(uid)
    return uid
  } catch (e) {
    console.error('check-token failed:', e)
    return ''
  }
}


// ====== STT（语音识别）选择：固定包含 Vosk（本地） + 追加数据库 STT 配置 ======
function isVoskConfig(c: ConfigDTO): boolean {
  const p = (c.provider || '').toLowerCase()
  const n = (c.configName || '').toLowerCase()
  const d = (c.configDesc || '').toLowerCase()
  return p.includes('vosk') || n.includes('vosk') || d.includes('vosk')
}

// select 的 v-model 值：'vosk' 或 configId
const sttSelectValue = ref<string>('vosk')

// DB 中是否存在 vosk 的 config（有则优先用它的 configId）
const voskConfigId = computed(() => {
  const hit = sttConfigs.value.find(isVoskConfig)
  return hit?.configId != null ? String(hit.configId) : ''
})

// 附加的 STT 配置（排除 vosk，避免重复显示）
const sttExtraConfigs = computed(() => sttConfigs.value.filter(c => !isVoskConfig(c)))


type LoadState = 'idle' | 'loading' | 'error'
const state = ref<LoadState>('idle')
const errorMsg = ref('')

const configs = ref<ConfigDTO[]>([])
const templates = ref<TemplateDTO[]>([])

const llmConfigs = computed(() => configs.value.filter(c => (c.configType || '').toLowerCase() === 'llm'))
// 注意：Client 端不提供“语音合成(TTS)配置”选择（保持上一版 UI）
// const ttsConfigs = computed(() => configs.value.filter(c => (c.configType || '').toLowerCase() === 'tts'))
const sttConfigs = computed(() => configs.value.filter(c => (c.configType || '').toLowerCase() === 'stt'))

// Web Speech API（Edge/Chrome）本地音色列表（用于下拉框）
const voiceOptions = ref<VoiceOption[]>([])

function normalizeVoice(v: SpeechSynthesisVoice): VoiceOption {
  const lang = v.lang || ''
  // 这里用 voiceURI/name 作为 value 会更贴近浏览器本地 TTS。
  // 但你的后端/端侧更可能需要的是“云 TTS voiceName（如 zh-CN-XiaoxiaoNeural）”。
  // 为兼容两种情况：
  // - 若浏览器 voiceURI/name 中包含 'Xiaoxiao' 之类，我们仍然让 value=zh-CN-xxxNeural
  // - 否则 value 退回为 voiceURI 或 name
  const n = (v.name || '')
  const preferred = /Xiaoxiao/i.test(n) ? 'zh-CN-XiaoxiaoNeural'
    : /Xiaoyi/i.test(n) ? 'zh-CN-XiaoyiNeural'
    : /Yunjian/i.test(n) ? 'zh-CN-YunjianNeural'
    : /Yunxi/i.test(n) ? 'zh-CN-YunxiNeural'
    : /Yunxia/i.test(n) ? 'zh-CN-YunxiaNeural'
    : /Yunyang/i.test(n) ? 'zh-CN-YunyangNeural'
    : ''
  return {
    label: `${n}${lang ? ' (' + lang + ')' : ''}`,
    value: preferred || v.voiceURI || n,
    lang,
  }
}

function pickDefaultVoice(list: VoiceOption[]): string {
  // 默认优先：zh-CN + Xiaoxiao
  const xiaoxiao = list.find(v => v.lang?.toLowerCase() === 'zh-cn' && /xiaoxiao/i.test(v.label))
  if (xiaoxiao) return xiaoxiao.value
  const zh = list.find(v => v.lang?.toLowerCase() === 'zh-cn')
  if (zh) return zh.value
  return list[0]?.value || ''
}

let detachVoicesChanged: null | (() => void) = null

function loadBrowserVoicesOnce() {
  if (typeof window === 'undefined') return
  const ss = (window as any).speechSynthesis as SpeechSynthesis | undefined
  if (!ss) {
    voiceOptions.value = [...FALLBACK_VOICES]
    if (!form.voiceName) form.voiceName = pickDefaultVoice(voiceOptions.value)
    return
  }

  const fill = () => {
    const voices = ss.getVoices?.() || []
    if (voices.length > 0) {
      voiceOptions.value = voices.map(normalizeVoice)
    } else {
      voiceOptions.value = [...FALLBACK_VOICES]
    }
    if (!form.voiceName) form.voiceName = pickDefaultVoice(voiceOptions.value)
  }

  // 先尝试一次
  fill()

  // 某些浏览器需要等待 voiceschanged
  const handler = () => fill()
  try {
    ss.addEventListener?.('voiceschanged', handler)
    detachVoicesChanged = () => ss.removeEventListener?.('voiceschanged', handler)
  } catch {
    // Safari/旧环境可能没有 addEventListener
    ;(ss as any).onvoiceschanged = handler
    detachVoicesChanged = () => {
      try { (ss as any).onvoiceschanged = null } catch {}
    }
  }
}

// 表单（按你后端 RoleAddOrUpdateParam）
const form = reactive<RoleAddOrUpdateParam>({
  roleName: '',
  modelId: null,
  sttId: null,
  ttsId: null,
  voiceName: '',
  systemPrompt: '',
  // 仅前端使用：记忆类型 UI（目前只提供短期记忆 window）
  memoryTypeUi: 0 as any,
  roleDesc: '',
})

watch(sttSelectValue, (v) => {
  if (v === 'vosk') {
    // 若 DB 有 vosk 配置则用它的 configId，否则置空交给后端默认
    form.sttId = voskConfigId.value ? Number(voskConfigId.value) : null
  } else {
    form.sttId = v ? Number(v) : null
  }
}, { immediate: true })

// UI 上的“角色设定”选择：选模板后自动填充 systemPrompt
const selectedTemplateId = ref<string>('')

const canSubmit = computed(() => {
  return !!form.roleName?.trim() && form.modelId != null
})

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
  try {
    loadBrowserVoicesOnce()
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

watch(selectedTemplateId, (id) => {
  if (!id) return
  const tpl = templates.value.find(x => String(x.templateId) === String(id))
  // 兼容字段：content / promptTemplate / templateContent
  const content = (tpl as any)?.content
    ?? (tpl as any)?.promptTemplate
    ?? (tpl as any)?.templateContent
    ?? ''
  if (String(content).trim()) {
    // 选择模板时直接填充（覆盖），避免“看起来没反应”
    form.systemPrompt = String(content)
  }
})

async function submit() {
  if (!canSubmit.value) return

  try {
    const userId = await ensureUserId()
    if (!userId) throw new Error('未获取到 userId/用户名：请先登录或检查 /user/check-token 返回字段')

    await addRole({
      roleName: form.roleName?.trim(),
      roleDesc: (form as any).roleDesc || '',
      modelId: form.modelId,
      ttsId: form.ttsId,
      voiceName: form.voiceName || '',
      systemPrompt: form.systemPrompt || '',
          // 后端入库需要 userId（否则会报 Column 'userId' cannot be null）
      userId: userId,
      // 记忆类型：目前仅提供 window
      memoryType: 'window',
    })
    alert('创建成功')
    router.back('/home')
  } catch (e: any) {
    alert(e?.message || '创建失败')
  }
}

onMounted(loadAll)
onUnmounted(() => {
  detachVoicesChanged?.()
  detachVoicesChanged = null
})
</script>

<template>
  <div class="page">
    <!-- 顶部导航（返回 + 标题） -->
    <div class="nav">
      <button class="back" @click="router.back()">‹</button>
      <div class="title">创建AI角色</div>
      <div style="width: 28px"></div>
    </div>

    <div v-if="state==='loading'" class="hint">加载中…</div>
    <div v-else-if="state==='error'" class="hint err">加载失败：{{ errorMsg }}</div>

    <!-- 表单区 -->
    <div class="form">
      <div class="row">
        <div class="label">AI角色昵称</div>
        <input class="input" v-model="form.roleName" placeholder="" />
      </div>

      <div class="row">
        <div class="label">AI模型</div>
        <select class="select" v-model="form.modelId">
          <option :value="null">请选择</option>
          <option v-for="c in llmConfigs" :key="String(c.configId)" :value="c.configId">
            {{ configLabel(c) }}
          </option>
        </select>
      </div>
      
      <div class="row">
        <div class="label">语音识别</div>
        <select class="select" v-model="sttSelectValue">
          <option value="vosk">Vosk（本地）</option>
          <option v-for="c in sttExtraConfigs" :key="String(c.configId)" :value="String(c.configId)">
            {{ configLabel(c) }}
          </option>
        </select>
      </div>

      <div class="row">
        <div class="label">音色</div>
        <select class="select" v-model="form.voiceName">
          <option value="">请选择</option>
          <option v-for="v in voiceOptions" :key="v.value" :value="v.value">
            {{ v.label }}
          </option>
        </select>
      </div>
      <!-- 记忆类型：目前仅提供“短期记忆（window）” -->
      <div class="row">
        <div class="label">记忆类型</div>
        <select class="select" v-model="(form as any).memoryTypeUi">
          <option :value="0">短期记忆（window）</option>
        </select>
      </div>



      <div class="row">
        <div class="label">角色设定</div>
        <select class="select" v-model="selectedTemplateId">
          <option value="">请选择</option>
          <option v-for="t in templates" :key="String(t.templateId)" :value="String(t.templateId)">
            {{ t.templateName || ('template-' + t.templateId) }}
          </option>
        </select>
      </div>

      <div class="row textarea-row">
        <div class="label">私人设定</div>
        <textarea
            class="textarea"
            v-model="form.systemPrompt"
            placeholder="选填"
        />
      </div>
    </div>


    <!-- 底部按钮 -->
    <div class="footer">
      <button class="submit" :disabled="!canSubmit" @click="submit">
        完成创建
      </button>
    </div>
  </div>
</template>

<style scoped>
.page {
  min-height: 100vh;
  background: #fff;
  padding-bottom: 90px;
  font-family: ui-sans-serif, system-ui;
}

.nav {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 14px;
  border-bottom: 1px solid #f1f1f1;
}

.back {
  border: none;
  background: transparent;
  font-size: 26px;
  width: 28px;
  cursor: pointer;
}

.title {
  font-size: 18px;
  font-weight: 700;
}

.hint {
  padding: 10px 16px;
  font-size: 12px;
  opacity: .75;
}
.hint.err { color: #c00; opacity: 1; }

.form {
  padding: 18px 18px 0;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.row {
  display: grid;
  grid-template-columns: 90px minmax(0, 1fr);
  align-items: center;
  gap: 14px;
}

.label {
  font-weight: 700;
  color: #111;
}

.input, .select {
  height: 36px;
  border-radius: 10px;
  border: none;
  background: #f1eaea;
  padding: 0 12px;
  outline: none;
  width: 100%;
  min-width: 0;
  box-sizing: border-box;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.textarea-row {
  align-items: start;
}

.textarea {
  min-height: 180px;
  border-radius: 10px;
  border: none;
  background: #f1eaea;
  padding: 10px 12px;
  outline: none;
  resize: none;
}

.footer {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 14px 18px 20px;
  background: #fff;
  border-top: 1px solid #f1f1f1;
}

.submit {
  width: 100%;
  height: 50px;
  border-radius: 12px;
  border: none;
  background: #000;
  color: #fff;
  font-size: 16px;
  font-weight: 700;
  cursor: pointer;
}

.submit:disabled {
  opacity: .35;
  cursor: not-allowed;
}
</style>