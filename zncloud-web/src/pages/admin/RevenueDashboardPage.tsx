import { useState, useEffect } from 'react'
import { useAuthStore } from '../../stores/authStore'
import { getCafeReport } from '../../api/settlement'
import type { CafeSettlementReport } from '../../api/settlement'

export default function RevenueDashboardPage() {
  const { user } = useAuthStore()
  const [report, setReport] = useState<CafeSettlementReport | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  // 模拟获取用户关联的 cafeId - 实际应从用户信息或API获取
  const cafeId = user?.cafeId ? String(user.cafeId) : ''

  useEffect(() => {
    if (!cafeId) {
      setLoading(false)
      setError('未关联网吧信息，请联系管理员')
      return
    }

    setLoading(true)
    getCafeReport(cafeId)
      .then(setReport)
      .catch((err: unknown) => {
        setError(err instanceof Error ? err.message : '获取收益数据失败')
      })
      .finally(() => setLoading(false))
  }, [cafeId])

  const statsCards = report
    ? [
        {
          label: '今日收入',
          value: `¥${report.todayRevenue.toFixed(2)}`,
          sub: `网吧分成: ¥${report.todayCafeShare.toFixed(2)}`,
          color: 'text-green-600',
          bg: 'bg-green-50',
          icon: '📊',
        },
        {
          label: '本月收入',
          value: `¥${report.monthRevenue.toFixed(2)}`,
          sub: `网吧分成: ¥${report.monthCafeShare.toFixed(2)}`,
          color: 'text-blue-600',
          bg: 'bg-blue-50',
          icon: '📈',
        },
        {
          label: '累计收入',
          value: `¥${report.totalRevenue.toFixed(2)}`,
          sub: `网吧分成: ¥${report.totalCafeShare.toFixed(2)}`,
          color: 'text-purple-600',
          bg: 'bg-purple-50',
          icon: '💰',
        },
        {
          label: '可提现余额',
          value: `¥${report.withdrawableBalance.toFixed(2)}`,
          sub: `账户余额: ¥${report.accountBalance.toFixed(2)}`,
          color: 'text-orange-600',
          bg: 'bg-orange-50',
          icon: '🏦',
        },
      ]
    : []

  const detailCards = report
    ? [
        {
          label: '今日会话',
          value: report.todaySessions,
          icon: '🔄',
          bg: 'bg-cyan-50',
          color: 'text-cyan-600',
        },
        {
          label: '今日在线时长',
          value: `${report.todayOnlineHours.toFixed(1)}h`,
          icon: '⏱️',
          bg: 'bg-indigo-50',
          color: 'text-indigo-600',
        },
        {
          label: '本月会话',
          value: report.monthSessions,
          icon: '🔄',
          bg: 'bg-teal-50',
          color: 'text-teal-600',
        },
        {
          label: '本月在线时长',
          value: `${report.monthOnlineHours.toFixed(1)}h`,
          icon: '⏱️',
          bg: 'bg-rose-50',
          color: 'text-rose-600',
        },
        {
          label: '待结算金额',
          value: `¥${report.pendingSettlement.toFixed(2)}`,
          icon: '⏳',
          bg: 'bg-amber-50',
          color: 'text-amber-600',
        },
        {
          label: '分成比例',
          value: `${(report.commissionRate * 100).toFixed(1)}%`,
          icon: '📋',
          bg: 'bg-emerald-50',
          color: 'text-emerald-600',
        },
      ]
    : []

  if (loading) {
    return (
      <div>
        <h1 className="mb-6 text-xl font-bold text-gray-900">收益看板</h1>
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {[1, 2, 3, 4].map((i) => (
            <div key={i} className="card animate-pulse">
              <div className="h-4 w-20 rounded bg-gray-200" />
              <div className="mt-2 h-8 w-32 rounded bg-gray-200" />
              <div className="mt-1 h-3 w-24 rounded bg-gray-200" />
            </div>
          ))}
        </div>
      </div>
    )
  }

  if (error) {
    return (
      <div>
        <h1 className="mb-6 text-xl font-bold text-gray-900">收益看板</h1>
        <div className="card text-center text-gray-500 py-12">
          <p className="text-red-500 mb-2">{error}</p>
          <button className="btn-primary text-sm" onClick={() => window.location.reload()}>
            重试
          </button>
        </div>
      </div>
    )
  }

  return (
    <div>
      <h1 className="mb-6 text-xl font-bold text-gray-900">收益看板</h1>

      {/* 统计卡片 */}
      <div className="mb-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {statsCards.map((card) => (
          <div key={card.label} className={`rounded-xl p-5 ${card.bg}`}>
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-gray-500">{card.label}</p>
                <p className={`mt-1 text-2xl font-bold ${card.color}`}>{card.value}</p>
                <p className="mt-1 text-xs text-gray-400">{card.sub}</p>
              </div>
              <span className="text-2xl">{card.icon}</span>
            </div>
          </div>
        ))}
      </div>

      {/* 明细卡片 */}
      <div className="mb-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-6">
        {detailCards.map((card) => (
          <div key={card.label} className={`rounded-xl p-4 ${card.bg}`}>
            <div className="flex items-center justify-between">
              <div>
                <p className="text-xs text-gray-500">{card.label}</p>
                <p className={`mt-1 text-lg font-bold ${card.color}`}>{card.value}</p>
              </div>
              <span className="text-xl">{card.icon}</span>
            </div>
          </div>
        ))}
      </div>

      {/* 趋势图表区域 */}
      <div className="grid gap-6 lg:grid-cols-2">
        <div className="card">
          <h3 className="text-sm font-medium text-gray-700">收入趋势</h3>
          <div className="mt-4 flex h-48 items-center justify-center rounded-lg bg-gray-50">
            <span className="text-sm text-gray-400">图表区域 — 待集成</span>
          </div>
        </div>
        <div className="card">
          <h3 className="text-sm font-medium text-gray-700">在线时长趋势</h3>
          <div className="mt-4 flex h-48 items-center justify-center rounded-lg bg-gray-50">
            <span className="text-sm text-gray-400">图表区域 — 待集成</span>
          </div>
        </div>
      </div>

      {/* 最近结算记录 */}
      <div className="mt-6 card">
        <h3 className="text-sm font-medium text-gray-700">最近结算</h3>
        <div className="mt-4 text-center text-sm text-gray-400 py-8">
          暂无最近结算记录
        </div>
      </div>
    </div>
  )
}
