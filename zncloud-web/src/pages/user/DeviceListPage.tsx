import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { getDevices } from '../../api/devices'
import type { Device, DeviceListParams } from '../../api/devices'
import DeviceCard from '../../components/common/DeviceCard'

const statusOptions = [
  { value: '', label: '全部状态' },
  { value: 'ONLINE', label: '在线' },
  { value: 'OFFLINE', label: '离线' },
  { value: 'MAINTENANCE', label: '维护中' },
  { value: 'DISABLED', label: '已禁用' },
]

const configLevelOptions = [
  { value: '', label: '全部配置' },
  { value: 'LOW', label: '低配' },
  { value: 'MEDIUM', label: '中配' },
  { value: 'HIGH', label: '高配' },
  { value: 'ULTRA', label: '顶配' },
]

export default function DeviceListPage() {
  const navigate = useNavigate()
  const [devices, setDevices] = useState<Device[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [params, setParams] = useState<DeviceListParams>({
    page: 0,
    size: 12,
  })
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)

  const fetchDevices = async () => {
    setLoading(true)
    setError('')
    try {
      const res = await getDevices(params)
      setDevices(res.content)
      setTotalPages(res.totalPages)
      setTotalElements(res.totalElements)
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : '获取设备列表失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchDevices()
  }, [params.page, params.size])

  const handleSearch = () => {
    setParams((prev) => ({ ...prev, page: 0 }))
    fetchDevices()
  }

  return (
    <div>
      <h1 className="mb-4 text-xl font-bold text-gray-900">设备列表</h1>

      {/* 筛选栏 */}
      <div className="mb-6 flex flex-wrap items-center gap-3">
        <select
          className="input-field w-auto"
          value={params.status || ''}
          onChange={(e) => setParams((prev) => ({ ...prev, status: e.target.value || undefined }))}
        >
          {statusOptions.map((opt) => (
            <option key={opt.value} value={opt.value}>
              {opt.label}
            </option>
          ))}
        </select>

        <select
          className="input-field w-auto"
          value={params.configLevel || ''}
          onChange={(e) => setParams((prev) => ({ ...prev, configLevel: e.target.value || undefined }))}
        >
          {configLevelOptions.map((opt) => (
            <option key={opt.value} value={opt.value}>
              {opt.label}
            </option>
          ))}
        </select>

        <button className="btn-primary text-sm" onClick={handleSearch}>
          搜索
        </button>

        <span className="text-sm text-gray-400 ml-auto">
          共 {totalElements} 台设备
        </span>
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

      {/* 加载中 */}
      {loading && (
        <div className="flex items-center justify-center py-12">
          <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary-200 border-t-primary-600" />
          <span className="ml-3 text-sm text-gray-500">加载中...</span>
        </div>
      )}

      {/* 设备列表 */}
      {!loading && !error && (
        <>
          {devices.length === 0 ? (
            <div className="card py-12 text-center text-gray-400">
              暂无设备数据
            </div>
          ) : (
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
              {devices.map((device) => (
                <DeviceCard
                  key={device.id}
                  device={device}
                  onClick={() => navigate(`/user/devices/${device.id}`)}
                />
              ))}
            </div>
          )}

          {/* 分页 */}
          {totalPages > 1 && (
            <div className="mt-6 flex items-center justify-center gap-2">
              <button
                className="btn-secondary text-xs"
                disabled={params.page === 0}
                onClick={() => setParams((prev) => ({ ...prev, page: prev.page! - 1 }))}
              >
                上一页
              </button>
              <span className="text-sm text-gray-500">
                第 {params.page! + 1} / {totalPages} 页
              </span>
              <button
                className="btn-secondary text-xs"
                disabled={params.page! >= totalPages - 1}
                onClick={() => setParams((prev) => ({ ...prev, page: prev.page! + 1 }))}
              >
                下一页
              </button>
            </div>
          )}
        </>
      )}
    </div>
  )
}
