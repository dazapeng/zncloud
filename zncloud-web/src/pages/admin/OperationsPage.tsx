import { useState, useEffect, useCallback } from 'react'
import {
  getRegionStats,
  getIspStats,
  getConfigStats,
  getPriceRangeStats,
  getOperatorDevices,
  getFilterOptions,
  getNotifications,
  getOperationLogs,
  batchUpdateStatus,
  batchAdjustPrice,
  publishNotification,
  exportCsv,
} from '../../api/operations'
import type {
  RegionStats,
  IspStats,
  ConfigStats,
  PriceRangeStats,
  OperatorDevice,
  FilterOptions,
  OperationsFilters,
  NotificationItem,
  OperationLog,
} from '../../api/operations'

/* ───────────────────────────────────────────────
   常量
   ─────────────────────────────────────────────── */

const TABS = ['运营视图', '多维统计', '批量操作', '通知管理', '数据导出'] as const

const STATUS_LABELS: Record<string, string> = {
  ONLINE: '在线',
  OFFLINE: '离线',
  MAINTENANCE: '维护中',
  DISABLED: '已禁用',
}

const STATUS_BADGE_COLORS: Record<string, string> = {
  ONLINE: 'bg-green-100 text-green-700',
  OFFLINE: 'bg-gray-100 text-gray-500',
  MAINTENANCE: 'bg-yellow-100 text-yellow-700',
  DISABLED: 'bg-red-100 text-red-700',
}

const NOTIFICATION_TYPE_LABELS: Record<string, string> = {
  SYSTEM: '系统通知',
  MAINTENANCE: '维护通知',
  PROMOTION: '促销活动',
  URGENT: '紧急通知',
}

const NOTIFICATION_TYPE_COLORS: Record<string, string> = {
  SYSTEM: 'bg-blue-100 text-blue-700',
  MAINTENANCE: 'bg-yellow-100 text-yellow-700',
  PROMOTION: 'bg-green-100 text-green-700',
  URGENT: 'bg-red-100 text-red-700',
}

const NOTIFICATION_TARGET_LABELS: Record<string, string> = {
  ALL: '全部用户',
  PROVINCE: '指定省份',
  CITY: '指定城市',
  CAFE: '指定网吧',
}

/* ───────────────────────────────────────────────
   辅助组件
   ─────────────────────────────────────────────── */

function LoadingSkeleton({ rows = 4 }: { rows?: number }) {
  return (
    <div className="space-y-3">
      {Array.from({ length: rows }).map((_, i) => (
        <div key={i} className="flex gap-4 animate-pulse">
          <div className="h-4 flex-1 rounded bg-gray-200" />
          <div className="h-4 w-20 rounded bg-gray-200" />
          <div className="h-4 w-16 rounded bg-gray-200" />
        </div>
      ))}
    </div>
  )
}

function StatusBadge({ status }: { status: string }) {
  return (
    <span
      className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${
        STATUS_BADGE_COLORS[status] || 'bg-gray-100 text-gray-500'
      }`}
    >
      {STATUS_LABELS[status] || status}
    </span>
  )
}

function formatDateTime(dateStr: string): string {
  if (!dateStr) return '-'
  try {
    return new Date(dateStr).toLocaleString('zh-CN')
  } catch {
    return dateStr
  }
}

function Pagination({
  page,
  totalPages,
  onPageChange,
}: {
  page: number
  totalPages: number
  onPageChange: (p: number) => void
}) {
  if (totalPages <= 1) return null
  return (
    <div className="mt-4 flex items-center justify-center gap-2">
      <button className="btn-secondary text-xs" disabled={page === 0} onClick={() => onPageChange(page - 1)}>
        上一页
      </button>
      <span className="text-sm text-gray-500">
        第 {page + 1} / {totalPages} 页
      </span>
      <button
        className="btn-secondary text-xs"
        disabled={page >= totalPages - 1}
        onClick={() => onPageChange(page + 1)}
      >
        下一页
      </button>
    </div>
  )
}

/* ───────────────────────────────────────────────
   主页面
   ─────────────────────────────────────────────── */

export default function OperationsPage() {
  const [activeTab, setActiveTab] = useState<string>('运营视图')

  return (
    <div>
      <h1 className="mb-6 text-xl font-bold text-gray-900">运营管理</h1>

      {/* 标签页导航 */}
      <div className="mb-6 flex flex-wrap gap-1 border-b border-gray-200">
        {TABS.map((tab) => (
          <button
            key={tab}
            onClick={() => setActiveTab(tab)}
            className={`px-4 py-2.5 text-sm font-medium transition-colors border-b-2 -mb-px ${
              activeTab === tab
                ? 'border-primary-600 text-primary-600'
                : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
            }`}
          >
            {tab}
          </button>
        ))}
      </div>

      {/* 标签页内容 */}
      {activeTab === '运营视图' && <OperationsViewTab />}
      {activeTab === '多维统计' && <MultiStatsTab />}
      {activeTab === '批量操作' && <BatchOperationsTab />}
      {activeTab === '通知管理' && <NotificationTab />}
      {activeTab === '数据导出' && <DataExportTab />}
    </div>
  )
}

/* ============================================================
   Tab 1 - 运营视图
   ============================================================ */

function OperationsViewTab() {
  const [filters, setFilters] = useState<OperationsFilters>({ page: 0, size: 15 })
  const [filterOptions, setFilterOptions] = useState<FilterOptions>({
    provinces: [],
    cities: [],
    isps: [],
    configLevels: [],
  })
  const [devices, setDevices] = useState<OperatorDevice[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [viewMode, setViewMode] = useState<'card' | 'list'>('card')

  // 本地筛选状态（搜索前）
  const [localProvince, setLocalProvince] = useState('')
  const [localCity, setLocalCity] = useState('')
  const [localIsp, setLocalIsp] = useState('')
  const [localConfig, setLocalConfig] = useState('')
  const [localMinPrice, setLocalMinPrice] = useState('')
  const [localMaxPrice, setLocalMaxPrice] = useState('')
  // 用来重新加载筛选选项
  const [filterRefreshKey, setFilterRefreshKey] = useState(0)

  // 获取筛选选项
  const fetchFilterOptions = useCallback(async () => {
    try {
      const opts = await getFilterOptions()
      setFilterOptions(opts)
    } catch {
      // 静默失败，使用空选项
    }
  }, [])

  useEffect(() => {
    fetchFilterOptions()
  }, [fetchFilterOptions, filterRefreshKey])

  // 获取设备列表
  const fetchDevices = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const res = await getOperatorDevices(filters)
      setDevices(res.content)
      setTotalPages(res.totalPages)
      setTotalElements(res.totalElements)
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : '获取设备列表失败')
    } finally {
      setLoading(false)
    }
  }, [filters])

  useEffect(() => {
    fetchDevices()
  }, [fetchDevices])

  const handleSearch = () => {
    setFilters((prev) => ({
      ...prev,
      province: localProvince || undefined,
      city: localCity || undefined,
      isp: localIsp || undefined,
      configLevel: localConfig || undefined,
      minPrice: localMinPrice ? Number(localMinPrice) : undefined,
      maxPrice: localMaxPrice ? Number(localMaxPrice) : undefined,
      page: 0,
    }))
  }

  const handleReset = () => {
    setLocalProvince('')
    setLocalCity('')
    setLocalIsp('')
    setLocalConfig('')
    setLocalMinPrice('')
    setLocalMaxPrice('')
    setFilters({ page: 0, size: 15 })
    setFilterRefreshKey((k) => k + 1)
  }

  const filteredCities = localProvince
    ? filterOptions.cities
    : filterOptions.cities

  return (
    <div>
      {/* 筛选栏 */}
      <div className="mb-4 rounded-xl bg-white p-4 shadow-sm border border-gray-200">
        <div className="flex flex-wrap items-end gap-3">
          <div>
            <label className="mb-1 block text-xs text-gray-500">省份</label>
            <select
              className="input-field w-auto min-w-[120px]"
              value={localProvince}
              onChange={(e) => {
                setLocalProvince(e.target.value)
                setLocalCity('')
              }}
            >
              <option value="">全部省份</option>
              {filterOptions.provinces.map((p) => (
                <option key={p} value={p}>
                  {p}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="mb-1 block text-xs text-gray-500">城市</label>
            <select
              className="input-field w-auto min-w-[120px]"
              value={localCity}
              onChange={(e) => setLocalCity(e.target.value)}
            >
              <option value="">全部城市</option>
              {filteredCities.map((c) => (
                <option key={c} value={c}>
                  {c}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="mb-1 block text-xs text-gray-500">运营商</label>
            <select
              className="input-field w-auto min-w-[120px]"
              value={localIsp}
              onChange={(e) => setLocalIsp(e.target.value)}
            >
              <option value="">全部线路</option>
              {filterOptions.isps.map((isp) => (
                <option key={isp} value={isp}>
                  {isp}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="mb-1 block text-xs text-gray-500">配置等级</label>
            <select
              className="input-field w-auto min-w-[120px]"
              value={localConfig}
              onChange={(e) => setLocalConfig(e.target.value)}
            >
              <option value="">全部配置</option>
              {filterOptions.configLevels.map((cl) => (
                <option key={cl} value={cl}>
                  {cl}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="mb-1 block text-xs text-gray-500">最低价格</label>
            <input
              type="number"
              className="input-field w-auto min-w-[100px]"
              placeholder="¥ 0"
              value={localMinPrice}
              onChange={(e) => setLocalMinPrice(e.target.value)}
            />
          </div>

          <div>
            <label className="mb-1 block text-xs text-gray-500">最高价格</label>
            <input
              type="number"
              className="input-field w-auto min-w-[100px]"
              placeholder="¥ 999"
              value={localMaxPrice}
              onChange={(e) => setLocalMaxPrice(e.target.value)}
            />
          </div>

          <div className="flex gap-2">
            <button className="btn-primary text-sm" onClick={handleSearch}>
              搜索
            </button>
            <button className="btn-secondary text-sm" onClick={handleReset}>
              重置
            </button>
          </div>

          <span className="text-sm text-gray-400 ml-auto">
            共 {totalElements} 台设备
          </span>
        </div>
      </div>

      {/* 视图切换 */}
      <div className="mb-4 flex items-center justify-between">
        <div className="flex gap-1 rounded-lg bg-gray-100 p-0.5">
          <button
            onClick={() => setViewMode('card')}
            className={`rounded-md px-3 py-1.5 text-xs font-medium transition-colors ${
              viewMode === 'card' ? 'bg-white text-gray-900 shadow-sm' : 'text-gray-500 hover:text-gray-700'
            }`}
          >
            卡片视图
          </button>
          <button
            onClick={() => setViewMode('list')}
            className={`rounded-md px-3 py-1.5 text-xs font-medium transition-colors ${
              viewMode === 'list' ? 'bg-white text-gray-900 shadow-sm' : 'text-gray-500 hover:text-gray-700'
            }`}
          >
            列表视图
          </button>
        </div>
      </div>

      {/* 错误提示 */}
      {error && (
        <div className="mb-4 rounded-lg bg-red-50 p-3 text-sm text-red-600">
          {error}
          <button className="ml-2 underline" onClick={fetchDevices}>
            重试
          </button>
        </div>
      )}

      {/* 卡片视图 */}
      {viewMode === 'card' && (
        <>
          {loading ? (
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
              {Array.from({ length: 6 }).map((_, i) => (
                <div key={i} className="rounded-xl bg-white p-5 shadow-sm border border-gray-200 animate-pulse">
                  <div className="h-4 w-24 rounded bg-gray-200 mb-3" />
                  <div className="h-3 w-full rounded bg-gray-100 mb-2" />
                  <div className="h-3 w-3/4 rounded bg-gray-100" />
                </div>
              ))}
            </div>
          ) : devices.length === 0 ? (
            <div className="rounded-xl bg-white p-12 text-center shadow-sm border border-gray-200">
              <p className="text-gray-400">暂无设备数据</p>
            </div>
          ) : (
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
              {devices.map((device) => (
                <div
                  key={device.id}
                  className="rounded-xl bg-white p-5 shadow-sm border border-gray-200 hover:shadow-md transition-shadow"
                >
                  <div className="flex items-start justify-between mb-3">
                    <div>
                      <h3 className="font-medium text-gray-900">{device.cafeName}</h3>
                      <p className="text-xs text-gray-400 mt-0.5">
                        {device.province} {device.city} {device.district}
                      </p>
                    </div>
                    <StatusBadge status={device.status} />
                  </div>
                  <div className="space-y-1.5 text-sm text-gray-500">
                    <div className="flex justify-between">
                      <span>运营商</span>
                      <span className="text-gray-700">{device.isp || '-'}</span>
                    </div>
                    <div className="flex justify-between">
                      <span>配置</span>
                      <span className="text-gray-700">{device.configLevel || '-'}</span>
                    </div>
                    <div className="flex justify-between">
                      <span>价格</span>
                      <span className="font-medium text-primary-600">¥{device.pricePerHour}/时</span>
                    </div>
                    <div className="flex justify-between">
                      <span>累计收益</span>
                      <span className="text-green-600">¥{device.totalEarnings?.toFixed(2) || '0.00'}</span>
                    </div>
                    <div className="flex justify-between">
                      <span>最后在线</span>
                      <span className="text-xs">{formatDateTime(device.lastOnlineAt)}</span>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </>
      )}

      {/* 列表视图 */}
      {viewMode === 'list' && (
        <div className="card !p-0 overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-gray-100 bg-gray-50">
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">省份</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">城市</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">网吧</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">运营商</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">配置</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">价格</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">状态</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">最后在线</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">收益</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-50">
                {loading ? (
                  <tr>
                    <td colSpan={9} className="px-4 py-12 text-center text-gray-400">
                      加载中...
                    </td>
                  </tr>
                ) : devices.length === 0 ? (
                  <tr>
                    <td colSpan={9} className="px-4 py-12 text-center text-gray-400">
                      暂无设备数据
                    </td>
                  </tr>
                ) : (
                  devices.map((device) => (
                    <tr key={device.id} className="hover:bg-gray-50 transition-colors">
                      <td className="px-4 py-3 text-gray-700">{device.province}</td>
                      <td className="px-4 py-3 text-gray-500">{device.city}</td>
                      <td className="px-4 py-3 font-medium text-gray-900">{device.cafeName}</td>
                      <td className="px-4 py-3 text-gray-500">{device.isp || '-'}</td>
                      <td className="px-4 py-3 text-gray-500">{device.configLevel || '-'}</td>
                      <td className="px-4 py-3 font-medium text-primary-600">¥{device.pricePerHour}</td>
                      <td className="px-4 py-3">
                        <StatusBadge status={device.status} />
                      </td>
                      <td className="px-4 py-3 text-xs text-gray-400">{formatDateTime(device.lastOnlineAt)}</td>
                      <td className="px-4 py-3 text-green-600">¥{device.totalEarnings?.toFixed(2) || '0.00'}</td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* 分页 */}
      <Pagination page={filters.page ?? 0} totalPages={totalPages} onPageChange={(p) => setFilters((prev) => ({ ...prev, page: p }))} />
    </div>
  )
}

/* ============================================================
   Tab 2 - 多维统计
   ============================================================ */

function MultiStatsTab() {
  const [regionData, setRegionData] = useState<RegionStats[]>([])
  const [ispData, setIspData] = useState<IspStats[]>([])
  const [configData, setConfigData] = useState<ConfigStats[]>([])
  const [priceData, setPriceData] = useState<PriceRangeStats[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const fetchAll = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const [region, isp, config, price] = await Promise.all([
        getRegionStats(),
        getIspStats(),
        getConfigStats(),
        getPriceRangeStats(),
      ])
      setRegionData(region)
      setIspData(isp)
      setConfigData(config)
      setPriceData(price)
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : '获取统计数据失败')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchAll()
  }, [fetchAll])

  if (error) {
    return (
      <div className="mb-4 rounded-lg bg-red-50 p-3 text-sm text-red-600">
        {error}
        <button className="ml-2 underline" onClick={fetchAll}>
          重试
        </button>
      </div>
    )
  }

  return (
    <div className="grid gap-6 lg:grid-cols-2">
      {/* 按地区统计 */}
      <div className="rounded-xl bg-white p-5 shadow-sm border border-gray-200">
        <h3 className="mb-4 text-sm font-medium text-gray-700">按地区统计</h3>
        {loading ? (
          <LoadingSkeleton rows={4} />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-gray-100">
                  <th className="px-2 py-2 text-left text-xs font-medium text-gray-500">省份</th>
                  <th className="px-2 py-2 text-left text-xs font-medium text-gray-500">城市</th>
                  <th className="px-2 py-2 text-right text-xs font-medium text-gray-500">设备数</th>
                  <th className="px-2 py-2 text-right text-xs font-medium text-gray-500">在线</th>
                  <th className="px-2 py-2 text-right text-xs font-medium text-gray-500">在线率</th>
                  <th className="px-2 py-2 text-right text-xs font-medium text-gray-500">利用率</th>
                  <th className="px-2 py-2 text-right text-xs font-medium text-gray-500">均价</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-50">
                {regionData.length === 0 ? (
                  <tr>
                    <td colSpan={7} className="px-2 py-6 text-center text-gray-400">
                      暂无数据
                    </td>
                  </tr>
                ) : (
                  regionData.map((r, i) => (
                    <tr key={i} className="hover:bg-gray-50">
                      <td className="px-2 py-2 text-gray-700">{r.province}</td>
                      <td className="px-2 py-2 text-gray-500">{r.city}</td>
                      <td className="px-2 py-2 text-right text-gray-900">{r.deviceCount}</td>
                      <td className="px-2 py-2 text-right text-green-600">{r.onlineCount}</td>
                      <td className="px-2 py-2 text-right">{(r.onlineRate * 100).toFixed(1)}%</td>
                      <td className="px-2 py-2 text-right text-blue-600">{(r.utilizationRate * 100).toFixed(1)}%</td>
                      <td className="px-2 py-2 text-right text-primary-600">¥{r.avgPrice.toFixed(2)}</td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* 按线路统计 */}
      <div className="rounded-xl bg-white p-5 shadow-sm border border-gray-200">
        <h3 className="mb-4 text-sm font-medium text-gray-700">按线路统计</h3>
        {loading ? (
          <LoadingSkeleton rows={4} />
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
                {ispData.length === 0 ? (
                  <tr>
                    <td colSpan={4} className="px-2 py-6 text-center text-gray-400">
                      暂无数据
                    </td>
                  </tr>
                ) : (
                  ispData.map((r, i) => (
                    <tr key={i} className="hover:bg-gray-50">
                      <td className="px-2 py-2 text-gray-700">{r.isp}</td>
                      <td className="px-2 py-2 text-right text-gray-900">{r.deviceCount}</td>
                      <td className="px-2 py-2 text-right text-green-600">{r.onlineCount}</td>
                      <td className="px-2 py-2 text-right text-primary-600">¥{r.avgPrice.toFixed(2)}</td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* 按配置统计 */}
      <div className="rounded-xl bg-white p-5 shadow-sm border border-gray-200">
        <h3 className="mb-4 text-sm font-medium text-gray-700">按配置统计</h3>
        {loading ? (
          <LoadingSkeleton rows={4} />
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
                {configData.length === 0 ? (
                  <tr>
                    <td colSpan={5} className="px-2 py-6 text-center text-gray-400">
                      暂无数据
                    </td>
                  </tr>
                ) : (
                  configData.map((r, i) => (
                    <tr key={i} className="hover:bg-gray-50">
                      <td className="px-2 py-2 text-gray-700">{r.configLevel}</td>
                      <td className="px-2 py-2 text-right text-gray-900">{r.deviceCount}</td>
                      <td className="px-2 py-2 text-right text-green-600">{r.onlineCount}</td>
                      <td className="px-2 py-2 text-right text-blue-600">{(r.utilizationRate * 100).toFixed(1)}%</td>
                      <td className="px-2 py-2 text-right text-primary-600">¥{r.avgPrice.toFixed(2)}</td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* 按价格统计 */}
      <div className="rounded-xl bg-white p-5 shadow-sm border border-gray-200">
        <h3 className="mb-4 text-sm font-medium text-gray-700">按价格统计</h3>
        {loading ? (
          <LoadingSkeleton rows={4} />
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
                {priceData.length === 0 ? (
                  <tr>
                    <td colSpan={4} className="px-2 py-6 text-center text-gray-400">
                      暂无数据
                    </td>
                  </tr>
                ) : (
                  priceData.map((r, i) => (
                    <tr key={i} className="hover:bg-gray-50">
                      <td className="px-2 py-2 text-gray-700">{r.rangeName}</td>
                      <td className="px-2 py-2 text-right text-gray-900">{r.deviceCount}</td>
                      <td className="px-2 py-2 text-right text-green-600">{r.onlineCount}</td>
                      <td className="px-2 py-2 text-right text-primary-600">¥{r.avgPrice.toFixed(2)}</td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  )
}

/* ============================================================
   Tab 3 - 批量操作
   ============================================================ */

function BatchOperationsTab() {
  const [devices, setDevices] = useState<OperatorDevice[]>([])
  const [loading, setLoading] = useState(true)
  const [logLoading, setLogLoading] = useState(true)
  const [error, setError] = useState('')
  const [logs, setLogs] = useState<OperationLog[]>([])

  /* 批量上下线 */
  const [selectedIds, setSelectedIds] = useState<string[]>([])
  const [batchAction, setBatchAction] = useState<'ONLINE' | 'OFFLINE'>('ONLINE')
  const [batchStatusLoading, setBatchStatusLoading] = useState(false)

  /* 批量调价 */
  const [priceActionType, setPriceActionType] = useState('region')
  const [priceFilterCriteria, setPriceFilterCriteria] = useState<Record<string, unknown>>({})
  const [priceAdjustmentType, setPriceAdjustmentType] = useState<'FIXED' | 'PERCENTAGE'>('FIXED')
  const [priceAdjustmentValue, setPriceAdjustmentValue] = useState('')
  const [pricePreviewCount, setPricePreviewCount] = useState<number | null>(null)
  const [priceLoading, setPriceLoading] = useState(false)

  const fetchDevices = useCallback(async () => {
    setLoading(true)
    try {
      const res = await getOperatorDevices({ size: 200 })
      setDevices(res.content)
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : '获取设备列表失败')
    } finally {
      setLoading(false)
    }
  }, [])

  const fetchLogs = useCallback(async () => {
    setLogLoading(true)
    try {
      const data = await getOperationLogs()
      setLogs(data)
    } catch {
      // 静默失败
    } finally {
      setLogLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchDevices()
    fetchLogs()
  }, [fetchDevices, fetchLogs])

  const toggleSelect = (id: string) => {
    setSelectedIds((prev) => (prev.includes(id) ? prev.filter((i) => i !== id) : [...prev, id]))
  }

  const toggleSelectAll = () => {
    if (selectedIds.length === devices.length) {
      setSelectedIds([])
    } else {
      setSelectedIds(devices.map((d) => d.id))
    }
  }

  const handleBatchStatus = async () => {
    if (selectedIds.length === 0) return
    setBatchStatusLoading(true)
    setError('')
    try {
      await batchUpdateStatus(selectedIds, batchAction)
      setSelectedIds([])
      fetchDevices()
      fetchLogs()
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : '批量上下线操作失败')
    } finally {
      setBatchStatusLoading(false)
    }
  }

  const handlePreviewPrice = () => {
    const criteria: Record<string, unknown> = {}
    if (priceActionType === 'region') {
      // 简单模拟：选中全部
      criteria.province = ''
    } else if (priceActionType === 'config') {
      criteria.configLevel = ''
    } else if (priceActionType === 'isp') {
      criteria.isp = ''
    }
    setPriceFilterCriteria(criteria)
    // 预览时用所有设备数量
    setPricePreviewCount(devices.length)
  }

  const handleBatchPrice = async () => {
    if (!priceAdjustmentValue) return
    setPriceLoading(true)
    setError('')
    try {
      await batchAdjustPrice(
        priceActionType,
        priceFilterCriteria,
        priceAdjustmentType,
        Number(priceAdjustmentValue),
      )
      setPriceAdjustmentValue('')
      setPricePreviewCount(null)
      fetchLogs()
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : '批量调价操作失败')
    } finally {
      setPriceLoading(false)
    }
  }

  return (
    <div className="space-y-6">
      {/* 批量上下线 */}
      <div className="rounded-xl bg-white p-5 shadow-sm border border-gray-200">
        <h3 className="mb-4 text-sm font-medium text-gray-700">批量上下线</h3>

        {error && (
          <div className="mb-3 rounded-lg bg-red-50 p-2 text-xs text-red-600">{error}</div>
        )}

        {/* 设备选择 */}
        <div className="mb-4 max-h-48 overflow-y-auto border border-gray-200 rounded-lg">
          {loading ? (
            <div className="p-4 text-center text-sm text-gray-400">加载中...</div>
          ) : (
            <table className="w-full text-sm">
              <thead className="sticky top-0 bg-gray-50">
                <tr className="border-b border-gray-100">
                  <th className="w-10 px-3 py-2 text-left">
                    <input
                      type="checkbox"
                      className="rounded border-gray-300"
                      checked={selectedIds.length === devices.length && devices.length > 0}
                      onChange={toggleSelectAll}
                    />
                  </th>
                  <th className="px-3 py-2 text-left text-xs font-medium text-gray-500">网吧</th>
                  <th className="px-3 py-2 text-left text-xs font-medium text-gray-500">地区</th>
                  <th className="px-3 py-2 text-left text-xs font-medium text-gray-500">配置</th>
                  <th className="px-3 py-2 text-left text-xs font-medium text-gray-500">价格</th>
                  <th className="px-3 py-2 text-left text-xs font-medium text-gray-500">状态</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-50">
                {devices.length === 0 ? (
                  <tr>
                    <td colSpan={6} className="px-3 py-6 text-center text-gray-400">
                      暂无设备
                    </td>
                  </tr>
                ) : (
                  devices.map((d) => (
                    <tr key={d.id} className="hover:bg-gray-50">
                      <td className="px-3 py-1.5">
                        <input
                          type="checkbox"
                          className="rounded border-gray-300"
                          checked={selectedIds.includes(d.id)}
                          onChange={() => toggleSelect(d.id)}
                        />
                      </td>
                      <td className="px-3 py-1.5 text-gray-700 text-xs">{d.cafeName}</td>
                      <td className="px-3 py-1.5 text-gray-500 text-xs">
                        {d.province} {d.city}
                      </td>
                      <td className="px-3 py-1.5 text-gray-500 text-xs">{d.configLevel || '-'}</td>
                      <td className="px-3 py-1.5 text-primary-600 text-xs">¥{d.pricePerHour}</td>
                      <td className="px-3 py-1.5">
                        <StatusBadge status={d.status} />
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          )}
        </div>

        <div className="flex flex-wrap items-center gap-3">
          <span className="text-sm text-gray-500">
            已选择 <strong className="text-primary-600">{selectedIds.length}</strong> 台设备
          </span>
          <select
            className="input-field w-auto"
            value={batchAction}
            onChange={(e) => setBatchAction(e.target.value as 'ONLINE' | 'OFFLINE')}
          >
            <option value="ONLINE">设为在线</option>
            <option value="OFFLINE">设为离线</option>
          </select>
          <button
            className="btn-primary text-sm"
            disabled={selectedIds.length === 0 || batchStatusLoading}
            onClick={handleBatchStatus}
          >
            {batchStatusLoading ? '执行中...' : '确认执行'}
          </button>
        </div>
      </div>

      {/* 批量调价 */}
      <div className="rounded-xl bg-white p-5 shadow-sm border border-gray-200">
        <h3 className="mb-4 text-sm font-medium text-gray-700">批量调价</h3>

        <div className="mb-4 flex flex-wrap items-center gap-3">
          <div>
            <label className="mb-1 block text-xs text-gray-500">选择范围</label>
            <select
              className="input-field w-auto"
              value={priceActionType}
              onChange={(e) => {
                setPriceActionType(e.target.value)
                setPricePreviewCount(null)
              }}
            >
              <option value="region">按地区</option>
              <option value="config">按配置</option>
              <option value="isp">按运营商</option>
              <option value="all">全部设备</option>
            </select>
          </div>

          <div>
            <label className="mb-1 block text-xs text-gray-500">调整方式</label>
            <select
              className="input-field w-auto"
              value={priceAdjustmentType}
              onChange={(e) => setPriceAdjustmentType(e.target.value as 'FIXED' | 'PERCENTAGE')}
            >
              <option value="FIXED">固定金额</option>
              <option value="PERCENTAGE">百分比</option>
            </select>
          </div>

          <div>
            <label className="mb-1 block text-xs text-gray-500">
              {priceAdjustmentType === 'FIXED' ? '调整金额 (¥)' : '调整百分比 (%)'}
            </label>
            <input
              type="number"
              className="input-field w-auto min-w-[120px]"
              placeholder={priceAdjustmentType === 'FIXED' ? '¥ 0.00' : '% 0'}
              value={priceAdjustmentValue}
              onChange={(e) => setPriceAdjustmentValue(e.target.value)}
            />
          </div>

          <div className="flex gap-2 items-end">
            <button
              className="btn-secondary text-sm"
              onClick={handlePreviewPrice}
              disabled={devices.length === 0}
            >
              预览影响
            </button>
            {pricePreviewCount !== null && (
              <span className="text-sm text-gray-500">
                预计影响 <strong className="text-primary-600">{pricePreviewCount}</strong> 台设备
              </span>
            )}
          </div>
        </div>

        <button
          className="btn-primary text-sm"
          disabled={!priceAdjustmentValue || priceLoading}
          onClick={handleBatchPrice}
        >
          {priceLoading ? '执行中...' : '确认调价'}
        </button>
      </div>

      {/* 操作历史 */}
      <div className="rounded-xl bg-white shadow-sm border border-gray-200 overflow-hidden">
        <div className="px-5 py-3 border-b border-gray-100">
          <h3 className="text-sm font-medium text-gray-700">操作历史</h3>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-gray-100 bg-gray-50">
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">操作</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">详情</th>
                <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">影响数量</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">操作人</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">时间</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-50">
              {logLoading ? (
                <tr>
                  <td colSpan={5} className="px-4 py-8 text-center text-gray-400">
                    加载中...
                  </td>
                </tr>
              ) : logs.length === 0 ? (
                <tr>
                  <td colSpan={5} className="px-4 py-8 text-center text-gray-400">
                    暂无操作记录
                  </td>
                </tr>
              ) : (
                logs.map((log) => (
                  <tr key={log.id} className="hover:bg-gray-50">
                    <td className="px-4 py-3">
                      <span className="inline-flex items-center rounded-full bg-primary-50 px-2 py-0.5 text-xs font-medium text-primary-700">
                        {log.action}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-gray-500">{log.details}</td>
                    <td className="px-4 py-3 text-right text-gray-900">{log.affectedCount}</td>
                    <td className="px-4 py-3 text-gray-500">{log.operator}</td>
                    <td className="px-4 py-3 text-xs text-gray-400">{formatDateTime(log.createdAt)}</td>
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

/* ============================================================
   Tab 4 - 通知管理
   ============================================================ */

function NotificationTab() {
  const [notifications, setNotifications] = useState<NotificationItem[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [showForm, setShowForm] = useState(false)

  // 表单状态
  const [formTitle, setFormTitle] = useState('')
  const [formContent, setFormContent] = useState('')
  const [formType, setFormType] = useState<'SYSTEM' | 'MAINTENANCE' | 'PROMOTION' | 'URGENT'>('SYSTEM')
  const [formTargetType, setFormTargetType] = useState<'ALL' | 'PROVINCE' | 'CITY' | 'CAFE'>('ALL')
  const [formTargetValue, setFormTargetValue] = useState('')
  const [submitting, setSubmitting] = useState(false)

  const fetchNotifications = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const data = await getNotifications()
      setNotifications(data)
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : '获取通知列表失败')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchNotifications()
  }, [fetchNotifications])

  const handleSubmit = async () => {
    if (!formTitle.trim() || !formContent.trim()) return
    setSubmitting(true)
    setError('')
    try {
      await publishNotification(
        formTitle,
        formContent,
        formType,
        formTargetType,
        formTargetValue,
      )
      setFormTitle('')
      setFormContent('')
      setFormType('SYSTEM')
      setFormTargetType('ALL')
      setFormTargetValue('')
      setShowForm(false)
      fetchNotifications()
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : '发布通知失败')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div>
      {/* 新建通知按钮 */}
      <div className="mb-4 flex items-center justify-between">
        <span className="text-sm text-gray-500">共 {notifications.length} 条通知</span>
        <button className="btn-primary text-sm" onClick={() => setShowForm(!showForm)}>
          {showForm ? '取消' : '新建通知'}
        </button>
      </div>

      {/* 错误提示 */}
      {error && (
        <div className="mb-4 rounded-lg bg-red-50 p-3 text-sm text-red-600">
          {error}
        </div>
      )}

      {/* 新建通知表单 */}
      {showForm && (
        <div className="mb-6 rounded-xl bg-white p-5 shadow-sm border border-gray-200">
          <h3 className="mb-4 text-sm font-medium text-gray-700">新建通知</h3>
          <div className="space-y-4">
            <div>
              <label className="mb-1 block text-xs text-gray-500">标题</label>
              <input
                className="input-field w-full"
                placeholder="通知标题"
                value={formTitle}
                onChange={(e) => setFormTitle(e.target.value)}
              />
            </div>
            <div>
              <label className="mb-1 block text-xs text-gray-500">内容</label>
              <textarea
                className="input-field w-full min-h-[100px]"
                placeholder="通知内容..."
                value={formContent}
                onChange={(e) => setFormContent(e.target.value)}
              />
            </div>
            <div className="grid gap-4 sm:grid-cols-2">
              <div>
                <label className="mb-1 block text-xs text-gray-500">通知类型</label>
                <select
                  className="input-field w-full"
                  value={formType}
                  onChange={(e) => setFormType(e.target.value as typeof formType)}
                >
                  <option value="SYSTEM">系统通知</option>
                  <option value="MAINTENANCE">维护通知</option>
                  <option value="PROMOTION">促销活动</option>
                  <option value="URGENT">紧急通知</option>
                </select>
              </div>
              <div>
                <label className="mb-1 block text-xs text-gray-500">目标范围</label>
                <select
                  className="input-field w-full"
                  value={formTargetType}
                  onChange={(e) => setFormTargetType(e.target.value as typeof formTargetType)}
                >
                  <option value="ALL">全部用户</option>
                  <option value="PROVINCE">指定省份</option>
                  <option value="CITY">指定城市</option>
                  <option value="CAFE">指定网吧</option>
                </select>
              </div>
            </div>
            {formTargetType !== 'ALL' && (
              <div>
                <label className="mb-1 block text-xs text-gray-500">
                  {formTargetType === 'PROVINCE' ? '省份' : formTargetType === 'CITY' ? '城市' : '网吧 ID'}
                </label>
                <input
                  className="input-field w-full"
                  placeholder={formTargetType === 'CAFE' ? '输入网吧 ID' : '输入名称'}
                  value={formTargetValue}
                  onChange={(e) => setFormTargetValue(e.target.value)}
                />
              </div>
            )}
            <div className="flex justify-end gap-2">
              <button className="btn-secondary text-sm" onClick={() => setShowForm(false)}>
                取消
              </button>
              <button
                className="btn-primary text-sm"
                disabled={!formTitle.trim() || !formContent.trim() || submitting}
                onClick={handleSubmit}
              >
                {submitting ? '发布中...' : '发布通知'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* 通知列表 */}
      <div className="space-y-3">
        {loading ? (
          <div className="rounded-xl bg-white p-8 text-center shadow-sm border border-gray-200">
            <LoadingSkeleton rows={4} />
          </div>
        ) : notifications.length === 0 ? (
          <div className="rounded-xl bg-white p-12 text-center shadow-sm border border-gray-200">
            <p className="text-gray-400">暂无通知</p>
          </div>
        ) : (
          notifications.map((n) => (
            <div key={n.id} className="rounded-xl bg-white p-5 shadow-sm border border-gray-200">
              <div className="flex items-start justify-between mb-2">
                <div className="flex items-center gap-2">
                  <h4 className="font-medium text-gray-900">{n.title}</h4>
                  <span
                    className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${NOTIFICATION_TYPE_COLORS[n.type] || 'bg-gray-100 text-gray-500'}`}
                  >
                    {NOTIFICATION_TYPE_LABELS[n.type] || n.type}
                  </span>
                  <span
                    className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${
                      n.status === 'PUBLISHED'
                        ? 'bg-green-100 text-green-700'
                        : 'bg-yellow-100 text-yellow-700'
                    }`}
                  >
                    {n.status === 'PUBLISHED' ? '已发布' : '草稿'}
                  </span>
                </div>
              </div>
              <p className="mb-2 text-sm text-gray-500">{n.content}</p>
              <div className="flex flex-wrap items-center gap-3 text-xs text-gray-400">
                <span>
                  目标：{NOTIFICATION_TARGET_LABELS[n.targetType] || n.targetType}
                  {n.targetValue ? ` (${n.targetValue})` : ''}
                </span>
                <span>发布：{formatDateTime(n.publishedAt)}</span>
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  )
}

/* ============================================================
   Tab 5 - 数据导出
   ============================================================ */

function DataExportTab() {
  const [province, setProvince] = useState('')
  const [city, setCity] = useState('')
  const [isp, setIsp] = useState('')
  const [configLevel, setConfigLevel] = useState('')
  const [exportFormat, setExportFormat] = useState<'csv' | 'excel'>('csv')
  const [exporting, setExporting] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [exportLogs, setExportLogs] = useState<OperationLog[]>([])
  const [logLoading, setLogLoading] = useState(true)

  const fetchLogs = useCallback(async () => {
    setLogLoading(true)
    try {
      const data = await getOperationLogs()
      setExportLogs(data)
    } catch {
      // 静默
    } finally {
      setLogLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchLogs()
  }, [fetchLogs])

  const handleExport = async () => {
    setExporting(true)
    setError('')
    setSuccess('')
    try {
      const filters: OperationsFilters = {}
      if (province) filters.province = province
      if (city) filters.city = city
      if (isp) filters.isp = isp
      if (configLevel) filters.configLevel = configLevel

      const blob = await exportCsv(filters)
      const url = window.URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `运营数据_${new Date().toISOString().slice(0, 10)}.${exportFormat === 'csv' ? 'csv' : 'xlsx'}`
      document.body.appendChild(a)
      a.click()
      window.URL.revokeObjectURL(url)
      document.body.removeChild(a)
      setSuccess('数据导出成功！')
      fetchLogs()
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : '导出失败')
    } finally {
      setExporting(false)
    }
  }

  return (
    <div className="space-y-6">
      {/* 导出表单 */}
      <div className="rounded-xl bg-white p-5 shadow-sm border border-gray-200">
        <h3 className="mb-4 text-sm font-medium text-gray-700">导出条件</h3>

        <div className="mb-4 flex flex-wrap items-end gap-3">
          <div>
            <label className="mb-1 block text-xs text-gray-500">省份</label>
            <input
              className="input-field w-auto min-w-[120px]"
              placeholder="全部"
              value={province}
              onChange={(e) => setProvince(e.target.value)}
            />
          </div>
          <div>
            <label className="mb-1 block text-xs text-gray-500">城市</label>
            <input
              className="input-field w-auto min-w-[120px]"
              placeholder="全部"
              value={city}
              onChange={(e) => setCity(e.target.value)}
            />
          </div>
          <div>
            <label className="mb-1 block text-xs text-gray-500">运营商</label>
            <input
              className="input-field w-auto min-w-[120px]"
              placeholder="全部"
              value={isp}
              onChange={(e) => setIsp(e.target.value)}
            />
          </div>
          <div>
            <label className="mb-1 block text-xs text-gray-500">配置等级</label>
            <input
              className="input-field w-auto min-w-[120px]"
              placeholder="全部"
              value={configLevel}
              onChange={(e) => setConfigLevel(e.target.value)}
            />
          </div>
        </div>

        <div className="mb-4">
          <label className="mb-2 block text-xs text-gray-500">导出格式</label>
          <div className="flex items-center gap-4">
            <label className="flex items-center gap-2 text-sm text-gray-700 cursor-pointer">
              <input
                type="radio"
                className="text-primary-600"
                checked={exportFormat === 'csv'}
                onChange={() => setExportFormat('csv')}
              />
              CSV
            </label>
            <label className="flex items-center gap-2 text-sm text-gray-700 cursor-pointer">
              <input
                type="radio"
                className="text-primary-600"
                checked={exportFormat === 'excel'}
                onChange={() => setExportFormat('excel')}
              />
              Excel
            </label>
          </div>
        </div>

        {error && (
          <div className="mb-3 rounded-lg bg-red-50 p-2 text-xs text-red-600">{error}</div>
        )}
        {success && (
          <div className="mb-3 rounded-lg bg-green-50 p-2 text-xs text-green-600">{success}</div>
        )}

        <button
          className="btn-primary text-sm"
          disabled={exporting}
          onClick={handleExport}
        >
          {exporting ? '导出中...' : '导出'}
        </button>
      </div>

      {/* 最近导出记录 */}
      <div className="rounded-xl bg-white shadow-sm border border-gray-200 overflow-hidden">
        <div className="px-5 py-3 border-b border-gray-100">
          <h3 className="text-sm font-medium text-gray-700">最近操作记录</h3>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-gray-100 bg-gray-50">
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">操作</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">详情</th>
                <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">数量</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">操作人</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">时间</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-50">
              {logLoading ? (
                <tr>
                  <td colSpan={5} className="px-4 py-8 text-center text-gray-400">
                    加载中...
                  </td>
                </tr>
              ) : exportLogs.length === 0 ? (
                <tr>
                  <td colSpan={5} className="px-4 py-8 text-center text-gray-400">
                    暂无记录
                  </td>
                </tr>
              ) : (
                exportLogs.slice(0, 10).map((log) => (
                  <tr key={log.id} className="hover:bg-gray-50">
                    <td className="px-4 py-3">
                      <span className="inline-flex items-center rounded-full bg-primary-50 px-2 py-0.5 text-xs font-medium text-primary-700">
                        {log.action}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-gray-500">{log.details}</td>
                    <td className="px-4 py-3 text-right text-gray-900">{log.affectedCount}</td>
                    <td className="px-4 py-3 text-gray-500">{log.operator}</td>
                    <td className="px-4 py-3 text-xs text-gray-400">{formatDateTime(log.createdAt)}</td>
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
