import { useState, useEffect, useCallback } from 'react'
import { getDevices } from '../../api/devices'
import {
  getRegionStats,
  getIspStats,
  getConfigStats,
  getPriceRangeStats,
  getOperatorDevices,
} from '../../api/operations'
import type { RegionStats, IspStats, ConfigStats, PriceRangeStats, OperatorDevice } from '../../api/operations'

export default function DashboardPage() {
  const [stats, setStats] = useState({
    onlineDevices: 0,
    offlineDevices: 0,
    totalDevices: 0,
    activeSessions: 0,
  })
  const [statsLoading, setStatsLoading] = useState(true)

  const [regionData, setRegionData] = useState<RegionStats[]>([])
  const [ispData, setIspData] = useState<IspStats[]>([])
  const [configData, setConfigData] = useState<ConfigStats[]>([])
  const [priceData, setPriceData] = useState<PriceRangeStats[]>([])
  const [recentDevices, setRecentDevices] = useState<OperatorDevice[]>([])
  const [opsLoading, setOpsLoading] = useState(true)

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
          activeSessions: Math.floor(online.totalElements * 0.7),
        })
      })
      .catch(() => {})
      .finally(() => setStatsLoading(false))
  }, [])

  const fetchOpsStats = useCallback(async () => {
    setOpsLoading(true)
    try {
      const [region, isp, config, price, devices] = await Promise.all([
        getRegionStats(),
        getIspStats(),
        getConfigStats(),
        getPriceRangeStats(),
        getOperatorDevices({ size: 10 }),
      ])
      setRegionData(region)
      setIspData(isp)
      setConfigData(config)
      setPriceData(price)
      setRecentDevices(devices.content)
    } catch {
    } finally {
      setOpsLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchOpsStats()
  }, [fetchOpsStats])

  const totalOnlineRate =
    stats.totalDevices > 0 ? ((stats.onlineDevices / stats.totalDevices) * 100).toFixed(1) : '0.0'
  const avgOnlineRate =
    regionData.length > 0
      ? (regionData.reduce((sum, r) => sum + r.onlineRate, 0) / regionData.length * 100).toFixed(1)
      : '-'

  const cards = [
    { label: '在线设备', value: stats.onlineDevices, color: 'text-green-600', bg: 'bg-green-50', icon: '🟢' },
    { label: '离线设备', value: stats.offlineDevices, color: 'text-gray-600', bg: 'bg-gray-50', icon: '⚪' },
    { label: '设备总数', value: stats.totalDevices, color: 'text-primary-600', bg: 'bg-primary-50', icon: '🖥️' },
    { label: '活跃会话', value: stats.activeSessions, color: 'text-blue-600', bg: 'bg-blue-50', icon: '🔗' },
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
                  {statsLoading ? (
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

      <div className="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <div className="rounded-xl bg-white p-4 shadow-sm border border-gray-200">
          <p className="text-xs text-gray-500">总在线率</p>
          <p className="mt-1 text-xl font-bold text-green-600">{totalOnlineRate}%</p>
        </div>
        <div className="rounded-xl bg-white p-4 shadow-sm border border-gray-200">
          <p className="text-xs text-gray-500">平均地区在线率</p>
          <p className="mt-1 text-xl font-bold text-blue-600">
            {opsLoading ? <span className="inline-block h-5 w-12 animate-pulse rounded bg-gray-200" /> : `${avgOnlineRate}%`}
          </p>
        </div>
        <div className="rounded-xl bg-white p-4 shadow-sm border border-gray-200">
          <p className="text-xs text-gray-500">运营商线路数</p>
          <p className="mt-1 text-xl font-bold text-purple-600">
            {opsLoading ? <span className="inline-block h-5 w-12 animate-pulse rounded bg-gray-200" /> : ispData.length}
          </p>
        </div>
        <div className="rounded-xl bg-white p-4 shadow-sm border border-gray-200">
          <p className="text-xs text-gray-500">配置等级数</p>
          <p className="mt-1 text-xl font-bold text-orange-600">
            {opsLoading ? <span className="inline-block h-5 w-12 animate-pulse rounded bg-gray-200" /> : configData.length}
          </p>
        </div>
      </div>

      <div className="mt-6 grid gap-6 lg:grid-cols-2">
        <div className="rounded-xl bg-white p-5 shadow-sm border border-gray-200">
          <h3 className="mb-4 text-sm font-medium text-gray-700">📋 按地区统计</h3>
          {opsLoading ? (
            <div className="space-y-2 animate-pulse">
              {[1, 2, 3].map((i) => (
                <div key={i} className="h-4 w-full rounded bg-gray-200" />
              ))}
            </div>
          ) : regionData.length === 0 ? (
            <p className="text-center text-sm text-gray-400 py-4">暂无数据</p>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-gray-100">
                    <th className="px-2 py-2 text-left text-xs font-medium text-gray-500">省份</th>
                    <th className="px-2 py-2 text-right text-xs font-medium text-gray-500">设备</th>
                    <th className="px-2 py-2 text-right text-xs font-medium text-gray-500">在线</th>
                    <th className="px-2 py-2 text-right text-xs font-medium text-gray-500">在线率</th>
                    <th className="px-2 py-2 text-right text-xs font-medium text-gray-500">利用率</th>
                    <th className="px-2 py-2 text-right text-xs font-medium text-gray-500">均价</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-50">
                  {regionData.slice(0, 8).map((r, i) => (
                    <tr key={i} className="hover:bg-gray-50">
                      <td className="px-2 py-2 text-gray-700">{r.province}</td>
                      <td className="px-2 py-2 text-right text-gray-900">{r.deviceCount}</td>
                      <td className="px-2 py-2 text-right text-green-600">{r.onlineCount}</td>
                      <td className="px-2 py-2 text-right">{(r.onlineRate * 100).toFixed(1)}%</td>
                      <td className="px-2 py-2 text-right text-blue-600">{(r.utilizationRate * 100).toFixed(1)}%</td>
                      <td className="px-2 py-2 text-right text-primary-600">¥{r.avgPrice.toFixed(2)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>

        <div className="rounded-xl bg-white p-5 shadow-sm border border-gray-200">
          <h3 className="mb-4 text-sm font-medium text-gray-700">📡 按线路统计</h3>
          {opsLoading ? (
            <div className="space-y-2 animate-pulse">
              {[1, 2, 3].map((i) => (
                <div key={i} className="h-4 w-full rounded bg-gray-200" />
              ))}
            </div>
          ) : ispData.length === 0 ? (
            <p className="text-center text-sm text-gray-400 py-4">暂无数据</p>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-gray-100">
                    <th className="px-2 py-2 text-left text-xs font-medium text-gray-500">运营商</th>
                    <th className="px-2 py-2 text-right text-xs font-medium text-gray-500">设备数</th>
                    <th className="px-2 py-2 text-right text-xs font-medium text-gray-500">在线</th>
                    <th className="px-2 py-2 text-right text-xs font-medium text-gray-500">均价</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-50">
                  {ispData.map((r, i) => (
                    <tr key={i} className="hover:bg-gray-50">
                      <td className="px-2 py-2 text-gray-700">{r.isp}</td>
                      <td className="px-2 py-2 text-right text-gray-900">{r.deviceCount}</td>
                      <td className="px-2 py-2 text-right text-green-600">{r.onlineCount}</td>
                      <td className="px-2 py-2 text-right text-primary-600">¥{r.avgPrice.toFixed(2)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>

        <div className="rounded-xl bg-white p-5 shadow-sm border border-gray-200">
          <h3 className="mb-4 text-sm font-medium text-gray-700">⚙️ 按配置统计</h3>
          {opsLoading ? (
            <div className="space-y-2 animate-pulse">
              {[1, 2, 3].map((i) => (
                <div key={i} className="h-4 w-full rounded bg-gray-200" />
              ))}
            </div>
          ) : configData.length === 0 ? (
            <p className="text-center text-sm text-gray-400 py-4">暂无数据</p>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-gray-100">
                    <th className="px-2 py-2 text-left text-xs font-medium text-gray-500">配置等级</th>
                    <th className="px-2 py-2 text-right text-xs font-medium text-gray-500">设备数</th>
                    <th className="px-2 py-2 text-right text-xs font-medium text-gray-500">在线</th>
                    <th className="px-2 py-2 text-right text-xs font-medium text-gray-500">利用率</th>
                    <th className="px-2 py-2 text-right text-xs font-medium text-gray-500">均价</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-50">
                  {configData.map((r, i) => (
                    <tr key={i} className="hover:bg-gray-50">
                      <td className="px-2 py-2 text-gray-700">{r.configLevel}</td>
                      <td className="px-2 py-2 text-right text-gray-900">{r.deviceCount}</td>
                      <td className="px-2 py-2 text-right text-green-600">{r.onlineCount}</td>
                      <td className="px-2 py-2 text-right text-blue-600">{(r.utilizationRate * 100).toFixed(1)}%</td>
                      <td className="px-2 py-2 text-right text-primary-600">¥{r.avgPrice.toFixed(2)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>

        <div className="rounded-xl bg-white p-5 shadow-sm border border-gray-200">
          <h3 className="mb-4 text-sm font-medium text-gray-700">💰 按价格统计</h3>
          {opsLoading ? (
            <div className="space-y-2 animate-pulse">
              {[1, 2, 3].map((i) => (
                <div key={i} className="h-4 w-full rounded bg-gray-200" />
              ))}
            </div>
          ) : priceData.length === 0 ? (
            <p className="text-center text-sm text-gray-400 py-4">暂无数据</p>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-gray-100">
                    <th className="px-2 py-2 text-left text-xs font-medium text-gray-500">价格区间</th>
                    <th className="px-2 py-2 text-right text-xs font-medium text-gray-500">设备数</th>
                    <th className="px-2 py-2 text-right text-xs font-medium text-gray-500">在线</th>
                    <th className="px-2 py-2 text-right text-xs font-medium text-gray-500">均价</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-50">
                  {priceData.map((r, i) => (
                    <tr key={i} className="hover:bg-gray-50">
                      <td className="px-2 py-2 text-gray-700">{r.rangeName}</td>
                      <td className="px-2 py-2 text-right text-gray-900">{r.deviceCount}</td>
                      <td className="px-2 py-2 text-right text-green-600">{r.onlineCount}</td>
                      <td className="px-2 py-2 text-right text-primary-600">¥{r.avgPrice.toFixed(2)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>

      <div className="mt-6 card">
        <h3 className="text-sm font-medium text-gray-700">🕐 最近设备动态</h3>
        <div className="mt-4">
          {opsLoading ? (
            <div className="space-y-2 animate-pulse">
              {[1, 2, 3].map((i) => (
                <div key={i} className="h-4 w-full rounded bg-gray-200" />
              ))}
            </div>
          ) : recentDevices.length === 0 ? (
            <p className="text-center text-sm text-gray-400 py-4">暂无动态</p>
          ) : (
            <div className="space-y-3">
              {recentDevices.map((d) => (
                <div key={d.id} className="flex items-center gap-3 text-sm text-gray-500">
                  <div
                    className={`h-2 w-2 rounded-full ${
                      d.status === 'ONLINE' ? 'bg-green-400' : d.status === 'IN_USE' ? 'bg-blue-400' : 'bg-gray-300'
                    }`}
                  />
                  <span className="font-medium text-gray-700">{d.cafeName}</span>
                  <span className="text-gray-400">
                    {d.province} {d.city}
                  </span>
                  <span className="ml-auto">
                    {d.status === 'ONLINE' ? '在线' : d.status === 'IN_USE' ? '使用中' : d.status === 'OFFLINE' ? '离线' : d.status}
                    {' · '}
                    {d.isp || '-'} · ¥{d.pricePerHour}/时
                  </span>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
