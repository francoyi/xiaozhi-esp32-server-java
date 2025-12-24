export type ResultMessage<T = any> = {
  code?: number
  msg?: string
  message?: string
  data?: T
  [k: string]: any
}

export type PageInfo<T> = {
  list: T[]
  total: number
  pageNum?: number
  pageSize?: number
  pages?: number
}

export type DeviceDTO = {
  deviceId: string
  deviceName?: string
  type?: string
  roleId?: number | string | null
  functionNames?: string | string[] | null
  location?: string | null
  ip?: string | null
  version?: string | null
  wifiName?: string | null
  lastLogin?: string | null
  createTime?: string | null
}

export type DeviceAddParam = { code: string }

export type DeviceUpdateParam = {
  deviceName?: string
  roleId?: number | string | null
  functionNames?: string | string[] | null
  location?: string | null
}

export type ConfigDTO = {
  configId: number | string
  configName?: string
  configType?: string
  provider?: string
  baseUrl?: string
  apiKey?: string
  token?: string
  model?: string
  modelName?: string
  remark?: string
  isDefault?: number | boolean
  createTime?: string
  updateTime?: string
  [k: string]: any
}

export type ConfigAddOrUpdateParam = {
  configName?: string
  configType?: string
  provider?: string
  baseUrl?: string
  apiKey?: string
  token?: string
  model?: string
  modelName?: string
  remark?: string
  isDefault?: number | boolean
  [k: string]: any
}

export type GetModelsParam = {
  configId?: number | string
  provider?: string
  baseUrl?: string
  apiKey?: string
  token?: string
  [k: string]: any
}

export type RoleDTO = {
  roleId: number | string
  roleName?: string
  roleDesc?: string
  roleAvatar?: string
  isDefault?: number | boolean
  modelId?: number | string | null
  ttsId?: number | string | null
  sttId?: number | string | null
  temperature?: number | null
  topP?: number | null
  maxTokens?: number | null
  voiceName?: string | null
  ttsSpeed?: number | null
  ttsPitch?: number | null
  datasetId?: number | string | null
  promptTemplate?: string | null
  systemPrompt?: string | null
  [k: string]: any
}

export type RoleAddOrUpdateParam = {
  roleName?: string
  roleDesc?: string
  roleAvatar?: string
  isDefault?: number | boolean
  modelId?: number | string | null
  ttsId?: number | string | null
  sttId?: number | string | null
  temperature?: number | null
  topP?: number | null
  maxTokens?: number | null
  voiceName?: string | null
  ttsSpeed?: number | null
  ttsPitch?: number | null
  datasetId?: number | string | null
  promptTemplate?: string | null
  systemPrompt?: string | null
  [k: string]: any
  userId?: string | number | null
  published?: number | null
  memoryType?: string | null
}

export type TestVoiceParam = {
  roleId?: number | string
  ttsId?: number | string | null
  voiceName?: string | null
  text?: string
  [k: string]: any
}

export type TemplateDTO = {
  templateId: number | string
  templateName?: string
  templateType?: string
  content?: string
  remark?: string
  [k: string]: any
}
