import client from './client'

export interface Session {
  id: string
  userId: number
  userPhone?: string
  userName?: string
  deviceId: string
  deviceMacAddress?: string
  cafeId?: string
  cafeName?: string
  deviceConfigLevel?: string
  pricePerHour?: number
  status: string
  startTime?: string
  endTime?: string
  durationMinutes?: number
  cost?: number
  disconnectReason?: string
  violationDetails?: string
  createTime?: string
  recentScreenshots?: Screenshot[]
}

export interface Screenshot {
  id: number
  sessionId: string
  storagePath: string
  fileSize?: number
  thumbnailPath?: string
  flagged?: boolean
  createdAt: string
  presignedUrl?: string
}

export interface RiskEvent {
  checkLogId: number
  sessionId: string
  userId?: number
  userPhone?: string
  userName?: string
  deviceId?: string
  deviceMacAddress?: string
  cafeName?: string
  checkResult: string
  riskLevel: string
  categories?: string
  confidence: number
  checkedAt: string
  screenshotId: number
  screenshotThumbnailUrl?: string
  sessionStatus?: string
  cost?: number
  durationMinutes?: number
  handled?: boolean
}

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

/** 获取会话列表 */
export async function getSessions(params: {
  userId?: number
  deviceId?: string
  status?: string
  pageNum?: number
  pageSize?: number
}): Promise<PageResult<Session>> {
  const res = await client.get<PageResult<Session>>('/sessions', { params })
  return res.data
}

/** 获取会话详情 */
export async function getSession(id: string): Promise<Session> {
  const res = await client.get<Session>(`/sessions/${id}`)
  return res.data
}

/** 创建会话 */
export async function createSession(userId: number, deviceId: string): Promise<Session> {
  const res = await client.post<Session>('/sessions', { userId, deviceId })
  return res.data
}

/** 开始会话 */
export async function startSession(id: string): Promise<Session> {
  const res = await client.post<Session>(`/sessions/${id}/start`)
  return res.data
}

/** 结束会话 */
export async function endSession(id: string): Promise<Session> {
  const res = await client.post<Session>(`/sessions/${id}/end`)
  return res.data
}

/** 断开会话 */
export async function disconnectSession(id: string, reason?: string): Promise<Session> {
  const res = await client.post<Session>(`/sessions/${id}/disconnect`, { reason })
  return res.data
}

/** 获取活跃会话数 */
export async function getActiveSessionCount(): Promise<number> {
  const res = await client.get<{ count: number }>('/sessions/active-count')
  return res.data.count
}

/** 获取风险事件列表 */
export async function getRiskEvents(params: {
  pageNum?: number
  pageSize?: number
}): Promise<PageResult<RiskEvent>> {
  const res = await client.get<PageResult<RiskEvent>>('/sessions/risk-events', { params })
  return res.data
}

/** 获取风险事件详情 */
export async function getRiskEventDetail(checkLogId: number): Promise<RiskEvent> {
  const res = await client.get<RiskEvent>(`/sessions/risk-events/${checkLogId}`)
  return res.data
}

/** 标记为误报 */
export async function markAsFalsePositive(checkLogId: number, comment?: string): Promise<void> {
  await client.post(`/sessions/risk-events/${checkLogId}/false-positive`, { comment })
}

/** 上传截图 */
export async function uploadScreenshot(sessionId: string, imageData: string, fileName?: string, fileSize?: number): Promise<{
  screenshotId: number
  pass: boolean
  riskLevel: string
  confidence: number
}> {
  const res = await client.post('/sessions/screenshots/upload', {
    sessionId,
    imageData,
    fileName,
    fileSize,
  })
  return res.data
}
