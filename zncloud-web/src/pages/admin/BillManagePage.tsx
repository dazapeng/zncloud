import { useState, useEffect } from 'react'
import { useAuthStore } from '../../stores/authStore'
import {
  getCafeBills,
  exportBillsCsv,
  billStatusLabels,
  billStatusColors,
} from '../../api/settlement'
import type { BillRecord } from '../../api/settlement'

const periodTypeOptions = [
  { value: '', label: '全部周期' },
  { value: 'DAILY', label: '日报' },
  { value: 'WEEKLY', label: '周报' },
  { value: 'MONTHLY', label: '月报' },
]

export default function BillManagePage() {
  const { user } = useAuthStore()
  const [bills, setBills] = useState<BillRecord[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [periodType, setPeriodType] = useState('')
  const [exporting, setExporting] = useState(false)

  const cafeId = user?.cafeId ? String(user.cafeId) : ''

  const fetchBills = async () => {
    if (!cafeId) return
    setLoading(true)
    setError('')
    try {
      const data = await getCafeBills(cafeId, periodType || undefined)
      setBills(data)
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : '获取账单列表失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchBills()
  }, [cafeId, periodType])

  const handleExport = async () => {
    if (!cafeId) return
    setExporting(true)
    try {
      const blob = await exportBillsCsv(cafeId, periodType || undefined)
      const url = window.URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `bills_${cafeId}_${new Date().toISOString().slice(0, 10)}.csv`
      document.body.appendChild(a)
      a.click()
      document.body.removeChild(a)
      window.URL.revokeObjectURL(url)
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : '导出失败')
    } finally {
      setExporting(false)
    }
  }

  if (!cafeId) {
    return (
      <div>
        <h1 className="mb-6 text-xl font-bold text-gray-900">账单管理</h1>
        <div className="card text-center text-gray-500 py-12">
          未关联网吧信息，请联系管理员
        </div>
      </div>
    )
  }

  return (
    <div>
      <h1 className="mb-6 text-xl font-bold text-gray-900">账单管理</h1>

      {/* 筛选栏 */}
      <div className="mb-4 flex flex-wrap items-center gap-3">
        <select
          className="input-field w-auto"
          value={periodType}
          onChange={(e) => setPeriodType(e.target.value)}
        >
          {periodTypeOptions.map((opt) => (
            <option key={opt.value} value={opt.value}>
              {opt.label}
            </option>
          ))}
        </select>

        <button className="btn-primary text-sm" onClick={fetchBills}>
          查询
        </button>

        <button
          className="btn-secondary text-sm"
          onClick={handleExport}
          disabled={exporting}
        >
          {exporting ? '导出中...' : '导出 CSV'}
        </button>

        <span className="ml-auto text-sm text-gray-400">
          共 {bills.length} 条记录
        </span>
      </div>

      {/* 错误提示 */}
      {error && (
        <div className="mb-4 rounded-lg bg-red-50 p-3 text-sm text-red-600">
          {error}
          <button className="ml-2 underline" onClick={fetchBills}>
            重试
          </button>
        </div>
      )}

      {/* 表格 */}
      <div className="card !p-0 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-gray-100 bg-gray-50">
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                  周期
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                  开始日期
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                  结束日期
                </th>
                <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">
                  总收入
                </th>
                <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">
                  平台分成
                </th>
                <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">
                  网吧分成
                </th>
                <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">
                  在线时长
                </th>
                <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">
                  会话数
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                  状态
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-50">
              {loading ? (
                <tr>
                  <td colSpan={9} className="px-4 py-12 text-center text-gray-400">
                    加载中...
                  </td>
                </tr>
              ) : bills.length === 0 ? (
                <tr>
                  <td colSpan={9} className="px-4 py-12 text-center text-gray-400">
                    暂无账单数据
                  </td>
                </tr>
              ) : (
                bills.map((bill) => (
                  <tr key={bill.id} className="hover:bg-gray-50 transition-colors">
                    <td className="px-4 py-3 font-medium text-gray-900">
                      {bill.periodType === 'DAILY'
                        ? '日报'
                        : bill.periodType === 'WEEKLY'
                          ? '周报'
                          : '月报'}
                    </td>
                    <td className="px-4 py-3 text-gray-500">{bill.periodStart}</td>
                    <td className="px-4 py-3 text-gray-500">{bill.periodEnd}</td>
                    <td className="px-4 py-3 text-right text-gray-900">
                      ¥{bill.totalRevenue.toFixed(2)}
                    </td>
                    <td className="px-4 py-3 text-right text-gray-500">
                      ¥{bill.platformShare.toFixed(2)}
                    </td>
                    <td className="px-4 py-3 text-right font-medium text-green-600">
                      ¥{bill.cafeShare.toFixed(2)}
                    </td>
                    <td className="px-4 py-3 text-right text-gray-500">
                      {bill.totalOnlineHours.toFixed(1)}h
                    </td>
                    <td className="px-4 py-3 text-right text-gray-500">
                      {bill.totalSessions}
                    </td>
                    <td className="px-4 py-3">
                      <span
                        className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${
                          billStatusColors[bill.status] || 'bg-gray-100 text-gray-500'
                        }`}
                      >
                        {billStatusLabels[bill.status] || bill.status}
                      </span>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  )
}
