import api from './api'
import { request } from './request'
import type { PageInfo, ResultMessage, TemplateDTO } from '../utils/types'

export async function queryTemplates(params?: Record<string, any>) {
    const res = await request.get<ResultMessage<PageInfo<TemplateDTO>>>(api.template.query, { params })
    return res.data
}
