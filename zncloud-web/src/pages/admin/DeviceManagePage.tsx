import { useState, useEffect } from 'react'
import { getDevices, batchUpdateDevices } from '../../api/devices'
import type { Device, DeviceListParams } from '../../api/devices'

const statusOptions = [
  { value: '', label: '全部状态' },
  { value: 'ONLINE', label: '在线' },
  { value: 'OFFLINE', label: '离线' },
  { value: 'MAINTENANCE', label: '维护中' },
  { value: 'DISABLED', label: '已禁用' },
]

const statusLabels: Record<string, string> = {
  ONLINE: '在线',
  OFFLINE: '离线',
  MAINTENANCE: '维护中',
  DISABLED: '已禁用',
}

const statusBadgeColors: Record<string, string> = {
  ONLINE: 'bg-green-100 text-green-700',
  OFFLINE: 'bg-gray-100 text-gray-500',
  MAINTENANCE: 'bg-yellow-100 text-yellow-700',
  DISABLED: 'bg-red-100 text-red-700',
}

export default function DeviceManagePage() {
  const [devices, setDevices] = useState<Device[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [params, setParams] = useState<DeviceListParams>({ page: 0, size: 15 })
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [selectedIds, setSelectedIds] = useState<number[]>([])
  const [batchLoading, setBatchLoading] = useState(false)

  const fetchDevices = async () => {
    setLoading(true)
    setError('')
    try {
      const res = await getDevices(params)
      setDevices(res.content)
      setTotalPages(res.totalPages)
      setTotalElements(res.totalElements)
      setSelectedIds([])
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

  const toggleSelect = (id: number) => {
    setSelectedIds((prev) =>
      prev.includes(id) ? prev.filter((i) => i !== id) : [...prev, id],
    )
  }

  const toggleSelectAll = () => {
    if (selectedIds.length === devices.length) {
      setSelectedIds([])
    } else {
      setSelectedIds(devices.map((d) => d.id))
    }
  }

  const handleBatchAction = async (action: string) => {
    if (selectedIds.length === 0) return
    setBatchLoading(true)
    try {
      await batchUpdateDevices(selectedIds, action)
      fetchDevices()
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : '批量操作失败')
    } finally {
      setBatchLoading(false)
    }
  }

  return (
    <div>
      <h1 className="mb-4 text-xl font-bold text-gray-900">设备管理</h1>

      {/* 筛选栏 */}
      <div className="mb-4 flex flex-wrap items-center gap-3">
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

        <button className="btn-primary text-sm" onClick={handleSearch}>
          搜索
        </button>

        <span className="text-sm text-gray-400 ml-auto">
          共 {totalElements} 台设备
        </span>
      </div>

      {/* 批量操作栏 */}
      {selectedIds.length > 0 && (
        <div className="mb-4 flex items-center gap-2 rounded-lg bg-primary-50 p-3 text-sm">
          <span className="text-primary-700">已选择 {selectedIds.length} 台设备</span>
          <button
            className="btn-secondary text-xs !px-2 !py-1"
            disabled={batchLoading}
            onClick={() => handleBatchAction('enable')}
          >
            启用
          </button>
          <button
            className="btn-secondary text-xs !px-2 !py-1"
            disabled={batchLoading}
            onClick={() => handleBatchAction('disable')}
          >
            禁用
          </button>
          <button
            className="rounded bg-red-50 px-2 py-1 text-xs text-red-600 hover:bg-red-100"
            disabled={batchLoading}
            onClick={() => handleBatchAction('delete')}
          >
            删除
          </button>
        </div>
      )}

      {/* 错误提示 */}
      {error && (
        <div className="mb-4 rounded-lg bg-red-50 p-3 text-sm text-red-600">
          {error}
          <button className="ml-2 underline" onClick={fetchDevices}>
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
                <th className="w-10 px-4 py-3 text-left">
                  <input
                    type="checkbox"
                    className="rounded border-gray-300"
                    checked={selectedIds.length === devices.length && devices.length > 0}
                    onChange={toggleSelectAll}
                  />
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                  设备名称
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                  编号
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                  状态
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                  网吧
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                  CPU
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                  内存
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                  心跳时间
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-50">
              {loading ? (
                <tr>
                  <td colSpan={8} className="px-4 py-12 text-center text-gray-400">
                    加载中...
                  </td>
                </tr>
              ) : devices.length === 0 ? (
                <tr>
                  <td colSpan={8} className="px-4 py-12 text-center text-gray-400">
                    暂无设备数据
                  </td>
                </tr>
              ) : (
                devices.map((device) => (
                  <tr key={device.id} className="hover:bg-gray-50 transition-colors">
                    <td className="px-4 py-3">
                      <input
                        type="checkbox"
                        className="rounded border-gray-300"
                        checked={selectedIds.includes(device.id)}
                        onChange={() => toggleSelect(device.id)}
                      />
                    </td>
                    <td className="px-4 py-3 font-medium text-gray-900">{device.name}</td>
                    <td className="px-4 py-3 text-gray-500">{device.deviceCode}</td>
                    <td className="px-4 py-3">
                      <span
                        className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${statusBadgeColors[device.status] || 'bg-gray-100 text-gray-500'}`}
                      >
                        {statusLabels[device.status] || '未知'}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-gray-500">{device.cafeName || '-'}</td>
                    <td className="px-4 py-3 text-gray-500">{device.cpuUsage ?? '-'}%</td>
                    <td className="px-4 py-3 text-gray-500">{device.memUsage ?? '-'}%</td>
                    <td className="px-4 py-3 text-xs text-gray-400">
                      {device.lastHeartbeat
                        ? new Date(device.lastHeartbeat).toLocaleString('zh-CN')
                        : '-'}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* 分页 */}
      {totalPages > 1 && (
        <div className="mt-4 flex items-center justify-center gap-2">
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
    </div>
  )
}
