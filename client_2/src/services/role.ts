import api from './api'
import { request } from './request'
import type { PageInfo, ResultMessage, RoleAddOrUpdateParam, RoleDTO, TestVoiceParam } from '../utils/types'

export async function queryRoles(params?: Record<string, any>) {
  const res = await request.get<ResultMessage<PageInfo<RoleDTO>>>(api.role.query, { params })
  return res.data
}

export async function addRole(data: RoleAddOrUpdateParam) {
  const res = await request.post<ResultMessage<RoleDTO>>(api.role.add, data)
  return res.data
}

export async function updateRole(roleId: string | number, data: RoleAddOrUpdateParam) {
  const url = `${api.role.update}/${encodeURIComponent(String(roleId))}`
  const res = await request.put<ResultMessage<RoleDTO>>(url, data)
  return res.data
}

export async function deleteRole(roleId: string | number) {
  const url = `${api.role.delete}/${encodeURIComponent(String(roleId))}`
  const res = await request.delete<ResultMessage>(url)
  return res.data
}

// 覆盖式设置发布角色（最多2个）
export async function setPublishedRoleIds(roleIds: Array<string | number>) {
  const res = await request.post<ResultMessage<any>>(api.role.publish as any, { roleIds })
  return res.data
}

export async function testVoice(data: TestVoiceParam) {
  const res = await request.post<ResultMessage<any>>(api.role.testVoice, data)
  return res.data
}
