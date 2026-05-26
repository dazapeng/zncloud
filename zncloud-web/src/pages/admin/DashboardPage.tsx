import { useState, useEffect } from 'react'
import { getDevices } from '../../api/devices'

export default function DashboardPage() {
  const [stats, setStats] = useState({
    onlineDevices: 0,
    offlineDevices: 0,
    totalDevices: 0,
    activeSessions: 0,
  })
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    Promise.all([
      getDevices({ status: 'ONLINE', size: 1 }),
      getDevices({ status: 'OFFLINE', size: 1 }),
      getDevices({ size: 1 }),
    ])
      .then(([online, offline, total]) => {
        setStats({
          onlineDevices: online.totalElements,
          offlineDevices: offline.totalElements,
          totalDevices: total.totalElements,
          activeSessions: Math.floor(online.totalElements * 0.7), // 占位
        })
      })
      .catch(() => {
        // 占位数据
      })
      .finally(() => setLoading(false))
  }, [])

  const cards = [
    {
      label: '在线设备',
      value: stats.onlineDevices,
      color: 'text-green-600',
      bg: 'bg-green-50',
      icon: '🟢',
    },
    {
      label: '离线设备',
      value: stats.offlineDevices,
      color: 'text-gray-600',
      bg: 'bg-gray-50',
      icon: '⚪',
    },
    {
      label: '设备总数',
      value: stats.totalDevices,
      color: 'text-primary-600',
      bg: 'bg-primary-50',
      icon: '🖥️',
    },
    {
      label: '活跃会话',
      value: stats.activeSessions,
      color: 'text-blue-600',
      bg: 'bg-blue-50',
      icon: '🔗',
    },
  ]

  return (
    <div>
      <h1 className="mb-6 text-xl font-bold text-gray-900">仪表盘</h1>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {cards.map((card) => (
          <div key={card.label} className={`rounded-xl p-5 ${card.bg}`}>
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-gray-500">{card.label}</p>
                <p className={`mt-1 text-2xl font-bold ${card.color}`}>
                  {loading ? (
                    <span className="inline-block h-6 w-12 animate-pulse rounded bg-gray-200" />
                  ) : (
                    card.value
                  )}
                </p>
              </div>
              <span className="text-2xl">{card.icon}</span>
            </div>
          </div>
        ))}
      </div>

      {/* 占位图表区域 */}
      <div className="mt-8 grid gap-6 lg:grid-cols-2">
        <div className="card">
          <h3 className="text-sm font-medium text-gray-700">设备在线趋势（占位）</h3>
          <div className="mt-4 flex h-48 items-center justify-center rounded-lg bg-gray-50">
            <span className="text-sm text-gray-400">图表区域 — 待集成</span>
          </div>
        </div>
        <div className="card">
          <h3 className="text-sm font-medium text-gray-700">资源使用概览（占位）</h3>
          <div className="mt-4 flex h-48 items-center justify-center rounded-lg bg-gray-50">
            <span className="text-sm text-gray-400">图表区域 — 待集成</span>
          </div>
        </div>
      </div>

      {/* 最近活动占位 */}
      <div className="mt-6 card">
        <h3 className="text-sm font-medium text-gray-700">最近活动</h3>
        <div className="mt-4 space-y-3">
          {[1, 2, 3, 4, 5].map((i) => (
            <div key={i} className="flex items-center gap-3 text-sm text-gray-500">
              <div className="h-2 w-2 rounded-full bg-gray-300" />
              <span>活动记录项目 {i}（占位）</span>
              <span className="ml-auto text-xs text-gray-400">{i} 分钟前</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}
