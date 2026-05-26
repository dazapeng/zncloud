import { useState, useEffect } from 'react'
import { useAuthStore } from '../../stores/authStore'
import {
  getCafeReport,
  getWithdrawals,
  createWithdrawal,
  withdrawalStatusLabels,
  withdrawalStatusColors,
} from '../../api/settlement'
import type { CafeSettlementReport, WithdrawalRecord } from '../../api/settlement'

export default function WithdrawPage() {
  const { user } = useAuthStore()
  const [report, setReport] = useState<CafeSettlementReport | null>(null)
  const [withdrawals, setWithdrawals] = useState<WithdrawalRecord[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [amount, setAmount] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [showForm, setShowForm] = useState(false)

  const cafeId = user?.cafeId ? String(user.cafeId) : ''

  const fetchData = async () => {
    if (!cafeId) {
      setLoading(false)
      return
    }
    setLoading(true)
    setError('')
    try {
      const [rpt, wds] = await Promise.all([
        getCafeReport(cafeId),
        getWithdrawals(),
      ])
      setReport(rpt)
      setWithdrawals(wds)
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : '获取数据失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchData()
  }, [cafeId])

  const handleSubmit = async () => {
    const numAmount = parseFloat(amount)
    if (isNaN(numAmount) || numAmount <= 0) {
      setError('请输入有效的提现金额')
      return
    }
    if (report && numAmount > report.withdrawableBalance) {
      setError('提现金额超过可提现余额')
      return
    }

    setSubmitting(true)
    setError('')
    try {
      await createWithdrawal({
        cafeId,
        amount: numAmount,
      })
      setAmount('')
      setShowForm(false)
      // 刷新数据
      await fetchData()
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : '提现申请失败')
    } finally {
      setSubmitting(false)
    }
  }

  if (loading) {
    return (
      <div>
        <h1 className="mb-6 text-xl font-bold text-gray-900">提现管理</h1>
        <div className="grid gap-4 sm:grid-cols-3 mb-6">
          {[1, 2, 3].map((i) => (
            <div key={i} className="card animate-pulse">
              <div className="h-4 w-20 rounded bg-gray-200" />
              <div className="mt-2 h-8 w-32 rounded bg-gray-200" />
            </div>
          ))}
        </div>
      </div>
    )
  }

  if (!cafeId) {
    return (
      <div>
        <h1 className="mb-6 text-xl font-bold text-gray-900">提现管理</h1>
        <div className="card text-center text-gray-500 py-12">
          未关联网吧信息，请联系管理员
        </div>
      </div>
    )
  }

  return (
    <div>
      <h1 className="mb-6 text-xl font-bold text-gray-900">提现管理</h1>

      {/* 余额卡片 */}
      {report && (
        <div className="mb-6 grid gap-4 sm:grid-cols-3">
          <div className="rounded-xl bg-green-50 p-5">
            <p className="text-sm text-gray-500">可提现余额</p>
            <p className="mt-1 text-2xl font-bold text-green-600">
              ¥{report.withdrawableBalance.toFixed(2)}
            </p>
          </div>
          <div className="rounded-xl bg-blue-50 p-5">
            <p className="text-sm text-gray-500">账户余额</p>
            <p className="mt-1 text-2xl font-bold text-blue-600">
              ¥{report.accountBalance.toFixed(2)}
            </p>
          </div>
          <div className="rounded-xl bg-amber-50 p-5">
            <p className="text-sm text-gray-500">待结算金额</p>
            <p className="mt-1 text-2xl font-bold text-amber-600">
              ¥{report.pendingSettlement.toFixed(2)}
            </p>
          </div>
        </div>
      )}

      {/* 错误提示 */}
      {error && (
        <div className="mb-4 rounded-lg bg-red-50 p-3 text-sm text-red-600">
          {error}
          <button className="ml-2 underline" onClick={() => setError('')}>
            关闭
          </button>
        </div>
      )}

      {/* 提现操作 */}
      <div className="mb-6">
        {!showForm ? (
          <button
            className="rounded bg-primary-600 px-5 py-2 text-sm font-medium text-white hover:bg-primary-700 transition-colors"
            onClick={() => setShowForm(true)}
            disabled={report ? report.withdrawableBalance <= 0 : true}
          >
            申请提现
          </button>
        ) : (
          <div className="card max-w-md">
            <h3 className="mb-3 text-sm font-medium text-gray-700">申请提现</h3>
            <div className="mb-3">
              <label className="mb-1 block text-xs text-gray-500">提现金额 (元)</label>
              <input
                type="number"
                className="input-field w-full"
                placeholder="输入提现金额"
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
                min="1"
                max={report?.withdrawableBalance || 0}
                step="0.01"
              />
              {report && (
                <p className="mt-1 text-xs text-gray-400">
                  可提现余额: ¥{report.withdrawableBalance.toFixed(2)}，最低提现 1 元
                </p>
              )}
            </div>
            <div className="flex gap-2">
              <button
                className="rounded bg-primary-600 px-4 py-2 text-sm text-white hover:bg-primary-700 disabled:opacity-50 transition-colors"
                onClick={handleSubmit}
                disabled={submitting}
              >
                {submitting ? '提交中...' : '确认提现'}
              </button>
              <button
                className="btn-secondary text-sm"
                onClick={() => {
                  setShowForm(false)
                  setAmount('')
                  setError('')
                }}
              >
                取消
              </button>
            </div>
          </div>
        )}
      </div>

      {/* 提现记录表格 */}
      <div className="card !p-0 overflow-hidden">
        <div className="px-4 py-3 border-b border-gray-100">
          <h3 className="text-sm font-medium text-gray-700">提现记录</h3>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-gray-100 bg-gray-50">
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                  申请时间
                </th>
                <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">
                  提现金额
                </th>
                <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">
                  手续费
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                  提现前余额
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                  状态
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                  备注
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-50">
              {withdrawals.length === 0 ? (
                <tr>
                  <td colSpan={6} className="px-4 py-12 text-center text-gray-400">
                    暂无提现记录
                  </td>
                </tr>
              ) : (
                withdrawals.map((w) => (
                  <tr key={w.id} className="hover:bg-gray-50 transition-colors">
                    <td className="px-4 py-3 text-gray-500">
                      {new Date(w.createdAt).toLocaleString('zh-CN')}
                    </td>
                    <td className="px-4 py-3 text-right font-medium text-gray-900">
                      ¥{w.amount.toFixed(2)}
                    </td>
                    <td className="px-4 py-3 text-right text-gray-500">
                      ¥{w.fee.toFixed(2)}
                    </td>
                    <td className="px-4 py-3 text-gray-500">
                      ¥{w.beforeBalance.toFixed(2)}
                    </td>
                    <td className="px-4 py-3">
                      <span
                        className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${
                          withdrawalStatusColors[w.status] || 'bg-gray-100 text-gray-500'
                        }`}
                      >
                        {withdrawalStatusLabels[w.status] || w.status}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-xs text-gray-400">
                      {w.reviewRemark || '-'}
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
