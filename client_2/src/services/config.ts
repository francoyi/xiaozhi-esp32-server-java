import api from './api'
import { request } from './request'
import type { ConfigAddOrUpdateParam, ConfigDTO, GetModelsParam, PageInfo, ResultMessage } from '../utils/types'

export async function queryConfigs(params?: Record<string, any>) {
  const res = await request.get<ResultMessage<PageInfo<ConfigDTO>>>(api.config.query, { params })
  return res.data
}

export async function addConfig(data: ConfigAddOrUpdateParam) {
  const res = await request.post<ResultMessage<ConfigDTO>>(api.config.add, data)
  return res.data
}

export async function updateConfig(configId: string | number, data: ConfigAddOrUpdateParam) {
  const url = `${api.config.update}/${encodeURIComponent(String(configId))}`
  const res = await request.put<ResultMessage<ConfigDTO>>(url, data)
  return res.data
}

export async function getModels(data: GetModelsParam) {
  const res = await request.post<ResultMessage<any>>(api.config.getModels, data)
  return res.data
}
