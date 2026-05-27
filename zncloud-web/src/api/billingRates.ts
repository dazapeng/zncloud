import client from './client'

/* ───────────────────────────────────────────
   费率管理 API
   ─────────────────────────────────────────── */

/** 配置等级 */
export type ConfigLevel = 'ENTRY' | 'MAINSTREAM' | 'HIGH_PERFORMANCE'

export const configLevelLabels: Record<ConfigLevel, string> = {
  ENTRY: '入门级',
  MAINSTREAM: '主流级',
  HIGH_PERFORMANCE: '高性能',
}

export const configLevelColors: Record<ConfigLevel, string> = {
  ENTRY: 'text-gray-600 bg-gray-50',
  MAINSTREAM: 'text-blue-600 bg-blue-50',
  HIGH_PERFORMANCE: 'text-purple-600 bg-purple-50',
}

/** 费率状态 */
export type BillingRateStatus = 'ACTIVE' | 'INACTIVE' | 'HISTORY'

export const rateStatusLabels: Record<BillingRateStatus, string> = {
  ACTIVE: '生效中',
  INACTIVE: '已停用',
  HISTORY: '历史记录',
}

export const rateStatusColors: Record<BillingRateStatus, string> = {
  ACTIVE: 'text-green-600 bg-green-50',
  INACTIVE: 'text-gray-500 bg-gray-50',
  HISTORY: 'text-yellow-600 bg-yellow-50',
}

/** 费率响应 */
export interface BillingRateResponse {
  id: number
  cafeId: string
  configLevel: ConfigLevel
  pricePerHour: number
  discountStart: number | null
  discountEnd: number | null
  discountRate: number | null
  effectiveAt: string
  status: BillingRateStatus
  createdBy: string
  remark: string
  createdAt: string
}

/** 费率请求 */
export interface BillingRateRequest {
  cafeId: string
  configLevel: ConfigLevel
  pricePerHour: number
  discountStart?: number | null
  discountEnd?: number | null
  discountRate?: number | null
  remark?: string
}

/** 批量费率请求 */
export interface BatchRateRequest {
  cafeIds: string[]
  configLevel: ConfigLevel
  pricePerHour: number
  discountStart?: number | null
  discountEnd?: number | null
  discountRate?: number | null
  remark?: string
}

/** 网吧简略信息 */
export interface CafeBrief {
  id: string
  name: string
  province: string
  city: string
}

/* ───────────────────────────────────────────
   API 方法
   ─────────────────────────────────────────── */

/** 获取指定网吧的费率列表 */
export async function getRates(cafeId?: string): Promise<BillingRateResponse[]> {
  const params: Record<string, string> = {}
  if (cafeId) params.cafeId = cafeId
  const res = await client.get('/billing/rates', { params })
  return res.data.data
}

/** 创建费率 */
export async function createRate(request: BillingRateRequest): Promise<BillingRateResponse> {
  const res = await client.post('/billing/rates', request)
  return res.data.data
}

/** 更新指定费率 */
export async function updateRate(id: number, request: BillingRateRequest): Promise<BillingRateResponse> {
  const res = await client.put(`/billing/rates/${id}`, request)
  return res.data.data
}

/** 批量设置费率 */
export async function batchCreateRates(request: BatchRateRequest): Promise<BillingRateResponse[]> {
  const res = await client.post('/billing/rates/batch', request)
  return res.data.data
}

/** 获取费率变更历史 */
export async function getRateHistory(cafeId?: string, configLevel?: string): Promise<BillingRateResponse[]> {
  const params: Record<string, string> = {}
  if (cafeId) params.cafeId = cafeId
  if (configLevel) params.configLevel = configLevel
  const res = await client.get('/billing/rates/history', { params })
  return res.data.data
}

/** 获取所有生效中的费率 */
export async function getActiveRates(): Promise<BillingRateResponse[]> {
  const res = await client.get('/billing/rates/active')
  return res.data.data
}
