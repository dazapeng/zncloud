import client from './client'

/* ───────────────────────────────────────────
   结算与提现 API
   ─────────────────────────────────────────── */

/** 网吧结算收益报告 */
export interface CafeSettlementReport {
  todayRevenue: number
  todayCafeShare: number
  todaySessions: number
  todayOnlineHours: number
  monthRevenue: number
  monthCafeShare: number
  monthSessions: number
  monthOnlineHours: number
  totalRevenue: number
  totalCafeShare: number
  totalSessions: number
  totalOnlineHours: number
  pendingSettlement: number
  withdrawableBalance: number
  accountBalance: number
  onlineDeviceCount: number
  commissionRate: number
}

/** 账单记录 */
export interface BillRecord {
  id: number
  periodType: 'DAILY' | 'WEEKLY' | 'MONTHLY'
  periodStart: string
  periodEnd: string
  totalRevenue: number
  platformShare: number
  cafeShare: number
  totalOnlineHours: number
  totalSessions: number
  deviceCount: number
  commissionRate: number
  status: 'PENDING' | 'CONFIRMED' | 'SETTLED'
  createdAt: string
}

/** 提现记录 */
export interface WithdrawalRecord {
  id: number
  cafeId: string
  amount: number
  beforeBalance: number
  afterBalance: number
  fee: number
  bankName: string
  bankBranch: string
  bankAccount: string
  accountHolder: string
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'COMPLETED' | 'FAILED'
  reviewRemark: string
  completedTime: string
  createdAt: string
}

/** 提现请求 */
export interface CreateWithdrawalRequest {
  cafeId: string
  amount: number
  bankName?: string
  bankBranch?: string
  bankAccount?: string
  accountHolder?: string
}

/** 获取网吧收益报告 */
export async function getCafeReport(cafeId: string): Promise<CafeSettlementReport> {
  const res = await client.get(`/settlement/cafes/${cafeId}/reports`)
  return res.data.data
}

/** 获取账单列表 */
export async function getCafeBills(cafeId: string, periodType?: string): Promise<BillRecord[]> {
  const params: Record<string, string> = {}
  if (periodType) params.periodType = periodType
  const res = await client.get(`/settlement/cafes/${cafeId}/bills`, { params })
  return res.data.data
}

/** 导出账单 CSV */
export async function exportBillsCsv(cafeId: string, periodType?: string): Promise<Blob> {
  const params: Record<string, string> = {}
  if (periodType) params.periodType = periodType
  const res = await client.get(`/settlement/cafes/${cafeId}/bills/export`, {
    params,
    responseType: 'blob',
  })
  return res.data
}

/** 创建提现申请 */
export async function createWithdrawal(data: CreateWithdrawalRequest): Promise<WithdrawalRecord> {
  const res = await client.post('/withdrawals', data)
  return res.data.data
}

/** 获取提现记录 */
export async function getWithdrawals(): Promise<WithdrawalRecord[]> {
  const res = await client.get('/withdrawals')
  return res.data.data
}

/** 账单状态标签 */
export const billStatusLabels: Record<string, string> = {
  PENDING: '待确认',
  CONFIRMED: '已确认',
  SETTLED: '已结算',
}

export const billStatusColors: Record<string, string> = {
  PENDING: 'text-yellow-600 bg-yellow-50',
  CONFIRMED: 'text-blue-600 bg-blue-50',
  SETTLED: 'text-green-600 bg-green-50',
}

/** 提现状态标签 */
export const withdrawalStatusLabels: Record<string, string> = {
  PENDING: '待审核',
  APPROVED: '审核通过',
  REJECTED: '已拒绝',
  COMPLETED: '已完成',
  FAILED: '失败',
}

export const withdrawalStatusColors: Record<string, string> = {
  PENDING: 'text-yellow-600 bg-yellow-50',
  APPROVED: 'text-blue-600 bg-blue-50',
  REJECTED: 'text-red-600 bg-red-50',
  COMPLETED: 'text-green-600 bg-green-50',
  FAILED: 'text-gray-600 bg-gray-50',
}
