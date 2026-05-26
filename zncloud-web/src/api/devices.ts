import client from './client'

export interface Device {
  id: number
  name: string
  deviceCode: string
  status: 'ONLINE' | 'OFFLINE' | 'MAINTENANCE' | 'DISABLED'
  cafeId?: number
  cafeName?: string
  configLevel?: string
  ipAddress?: string
  cpuUsage?: number
  memUsage?: number
  diskUsage?: number
  gpuInfo?: string
  currentSessionId?: number
  lastHeartbeat?: string
  createdAt?: string
  updatedAt?: string
}

export interface DeviceListParams {
  status?: string
  cafeId?: number
  configLevel?: string
  page?: number
  size?: number
}

export interface PageResult<T> {
  content: T[]
  totalPages: number
  totalElements: number
  page: number
  size: number
}

/** 获取设备列表 */
export async function getDevices(params: DeviceListParams): Promise<PageResult<Device>> {
  const res = await client.get<PageResult<Device>>('/devices', { params })
  return res.data
}

/** 获取设备详情 */
export async function getDevice(id: number): Promise<Device> {
  const res = await client.get<Device>(`/devices/${id}`)
  return res.data
}

/** 设备注册 */
export async function registerDevice(data: Partial<Device>): Promise<Device> {
  const res = await client.post<Device>('/devices/register', data)
  return res.data
}

/** 更新设备 */
export async function updateDevice(id: number, data: Partial<Device>): Promise<Device> {
  const res = await client.patch<Device>(`/devices/${id}`, data)
  return res.data
}

/** 批量操作设备（启用/禁用/删除） */
export async function batchUpdateDevices(ids: number[], action: string): Promise<void> {
  await client.post('/devices/batch', { ids, action })
}
