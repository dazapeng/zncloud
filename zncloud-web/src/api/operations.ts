import client from './client'

/* ───────────────────────────────────────────
   接口定义
   ─────────────────────────────────────────── */

export interface RegionStats {
  province: string
  city: string
  district: string
  deviceCount: number
  onlineCount: number
  inUseCount: number
  offlineCount: number
  onlineRate: number
  utilizationRate: number
  avgPrice: number
  totalEarnings: number
}

export interface IspStats {
  isp: string
  deviceCount: number
  onlineCount: number
  avgPrice: number
}

export interface ConfigStats {
  configLevel: string
  deviceCount: number
  onlineCount: number
  utilizationRate: number
  avgPrice: number
}

export interface PriceRangeStats {
  rangeName: string
  deviceCount: number
  onlineCount: number
  avgPrice: number
}

export interface OperatorDevice {
  id: string
  cafeId: string
  cafeName: string
  province: string
  city: string
  district: string
  isp: string
  configLevel: string
  pricePerHour: number
  status: string
  lastOnlineAt: string
  totalOnlineHours: number
  totalEarnings: number
  cpuInfo: string
  gpuInfo: string
  memoryGb: number
}

export interface FilterOptions {
  provinces: string[]
  cities: string[]
  isps: string[]
  configLevels: string[]
}

export interface OperationsFilters {
  province?: string
  city?: string
  isp?: string
  configLevel?: string
  minPrice?: number
  maxPrice?: number
  page?: number
  size?: number
}

export interface NotificationItem {
  id: string
  title: string
  content: string
  type: 'SYSTEM' | 'MAINTENANCE' | 'PROMOTION' | 'URGENT'
  targetType: 'ALL' | 'PROVINCE' | 'CITY' | 'CAFE'
  targetValue: string
  status: 'PUBLISHED' | 'DRAFT'
  publishedAt: string
  createdAt: string
}

export interface OperationLog {
  id: string
  action: string
  details: string
  affectedCount: number
  operator: string
  createdAt: string
}

export interface PageResult<T> {
  content: T[]
  totalPages: number
  totalElements: number
  page: number
  size: number
}

/* ───────────────────────────────────────────
   API 方法
   ─────────────────────────────────────────── */

/** 按地区统计 */
export async function getRegionStats(filters?: OperationsFilters): Promise<RegionStats[]> {
  const res = await client.get<RegionStats[]>('/operations/region-stats', { params: filters })
  return res.data
}

/** 按运营商线路统计 */
export async function getIspStats(filters?: OperationsFilters): Promise<IspStats[]> {
  const res = await client.get<IspStats[]>('/operations/isp-stats', { params: filters })
  return res.data
}

/** 按配置统计 */
export async function getConfigStats(filters?: OperationsFilters): Promise<ConfigStats[]> {
  const res = await client.get<ConfigStats[]>('/operations/config-stats', { params: filters })
  return res.data
}

/** 按价格区间统计 */
export async function getPriceRangeStats(filters?: OperationsFilters): Promise<PriceRangeStats[]> {
  const res = await client.get<PriceRangeStats[]>('/operations/price-range-stats', { params: filters })
  return res.data
}

/** 获取运营设备列表 */
export async function getOperatorDevices(
  filters?: OperationsFilters,
): Promise<PageResult<OperatorDevice>> {
  const res = await client.get<PageResult<OperatorDevice>>('/operations/devices', {
    params: filters,
  })
  return res.data
}

/** 获取筛选选项 */
export async function getFilterOptions(): Promise<FilterOptions> {
  const res = await client.get<FilterOptions>('/operations/filter-options')
  return res.data
}

/** 批量上下线 */
export async function batchUpdateStatus(
  deviceIds: string[],
  action: 'ONLINE' | 'OFFLINE',
): Promise<void> {
  await client.post('/operations/batch-status', { deviceIds, action })
}

/** 批量调价 */
export async function batchAdjustPrice(
  actionType: string,
  filterCriteria: Record<string, unknown>,
  adjustmentType: 'FIXED' | 'PERCENTAGE',
  adjustmentValue: number,
): Promise<{ affectedCount: number }> {
  const res = await client.post<{ affectedCount: number }>('/operations/batch-price', {
    actionType,
    filterCriteria,
    adjustmentType,
    adjustmentValue,
  })
  return res.data
}

/** 发布通知 */
export async function publishNotification(
  title: string,
  content: string,
  type: string,
  targetType: string,
  targetValue: string,
): Promise<NotificationItem> {
  const res = await client.post<NotificationItem>('/operations/notifications', {
    title,
    content,
    type,
    targetType,
    targetValue,
  })
  return res.data
}

/** 获取通知列表 */
export async function getNotifications(): Promise<NotificationItem[]> {
  const res = await client.get<NotificationItem[]>('/operations/notifications')
  return res.data
}

/** 导出 CSV */
export async function exportCsv(filters?: OperationsFilters): Promise<Blob> {
  const res = await client.get<Blob>('/operations/export/csv', {
    params: filters,
    responseType: 'blob',
  })
  return res.data
}

/** 获取操作日志 */
export async function getOperationLogs(): Promise<OperationLog[]> {
  const res = await client.get<OperationLog[]>('/operations/logs')
  return res.data
}
