import api from './api'
import { request } from './request'
import type { DeviceAddParam, DeviceDTO, DeviceUpdateParam, PageInfo, ResultMessage } from '../utils/types'

export async function queryDevices(params?: Record<string, any>) {
  const res = await request.get<ResultMessage<PageInfo<DeviceDTO>>>(api.device.query, { params })
  return res.data
}

export async function addDevice(data: DeviceAddParam) {
  const res = await request.post<ResultMessage<DeviceDTO>>(api.device.add, data)
  return res.data
}

export async function updateDevice(deviceId: string, data: DeviceUpdateParam) {
  const url = `${api.device.update}/${encodeURIComponent(deviceId)}`
  const res = await request.put<ResultMessage<DeviceDTO>>(url, data)
  return res.data
}

export async function deleteDevice(deviceId: string) {
  const url = `${api.device.delete}/${encodeURIComponent(deviceId)}`
  const res = await request.delete<ResultMessage>(url)
  return res.data
}
