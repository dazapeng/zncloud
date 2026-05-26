import { useState, useEffect } from 'react'
import { useParams, Link } from 'react-router-dom'
import { getDevice } from '../../api/devices'
import type { Device } from '../../api/devices'

const statusLabels: Record<string, string> = {
  ONLINE: '在线',
  OFFLINE: '离线',
  MAINTENANCE: '维护中',
  DISABLED: '已禁用',
}

const statusColors: Record<string, string> = {
  ONLINE: 'text-green-600 bg-green-50',
  OFFLINE: 'text-gray-500 bg-gray-50',
  MAINTENANCE: 'text-yellow-600 bg-yellow-50',
  DISABLED: 'text-red-600 bg-red-50',
}

export default function DeviceDetailPage() {
  const { id } = useParams<{ id: string }>()
  const [device, setDevice] = useState<Device | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!id) return
    setLoading(true)
    setError('')
    getDevice(Number(id))
      .then(setDevice)
      .catch((err) => setError(err instanceof Error ? err.message : '获取设备详情失败'))
      .finally(() => setLoading(false))
  }, [id])

  if (loading) {
    return (
      <div className="flex items-center justify-center py-12">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary-200 border-t-primary-600" />
        <span className="ml-3 text-sm text-gray-500">加载中...</span>
      </div>
    )
  }

  if (error) {
    return (
      <div className="card">
        <p className="text-red-500">{error}</p>
        <Link to="/user/devices" className="mt-2 inline-block text-sm text-primary-600 hover:underline">
          返回设备列表
        </Link>
      </div>
    )
  }

  if (!device) {
    return (
      <div className="card">
        <p className="text-gray-400">设备不存在</p>
        <Link to="/user/devices" className="mt-2 inline-block text-sm text-primary-600 hover:underline">
          返回设备列表
        </Link>
      </div>
    )
  }

  return (
    <div>
      <div className="mb-4">
        <Link to="/user/devices" className="text-sm text-primary-600 hover:underline">
          &larr; 返回设备列表
        </Link>
      </div>

      <div className="card">
        <div className="flex items-start justify-between">
          <div>
            <h1 className="text-xl font-bold text-gray-900">{device.name}</h1>
            <p className="mt-1 text-sm text-gray-400">设备编号: {device.deviceCode}</p>
          </div>
          <span
            className={`inline-flex items-center rounded-full px-3 py-1 text-sm font-medium ${statusColors[device.status] || 'text-gray-500 bg-gray-50'}`}
          >
            {statusLabels[device.status] || '未知'}
          </span>
        </div>

        {/* 资源使用率 */}
        <div className="mt-6 grid gap-6 sm:grid-cols-3">
          <div className="rounded-lg bg-gray-50 p-4">
            <p className="text-sm text-gray-500">CPU 使用率</p>
            <p className="mt-1 text-2xl font-bold text-gray-900">{device.cpuUsage ?? '-'}%</p>
            <div className="mt-2 h-2 rounded-full bg-gray-200">
              <div
                className="h-2 rounded-full bg-primary-500"
                style={{ width: `${device.cpuUsage ?? 0}%` }}
              />
            </div>
          </div>
          <div className="rounded-lg bg-gray-50 p-4">
            <p className="text-sm text-gray-500">内存使用率</p>
            <p className="mt-1 text-2xl font-bold text-gray-900">{device.memUsage ?? '-'}%</p>
            <div className="mt-2 h-2 rounded-full bg-gray-200">
              <div
                className="h-2 rounded-full bg-primary-500"
                style={{ width: `${device.memUsage ?? 0}%` }}
              />
            </div>
          </div>
          <div className="rounded-lg bg-gray-50 p-4">
            <p className="text-sm text-gray-500">磁盘使用率</p>
            <p className="mt-1 text-2xl font-bold text-gray-900">{device.diskUsage ?? '-'}%</p>
            <div className="mt-2 h-2 rounded-full bg-gray-200">
              <div
                className="h-2 rounded-full bg-primary-500"
                style={{ width: `${device.diskUsage ?? 0}%` }}
              />
            </div>
          </div>
        </div>

        {/* 详细信息 */}
        <div className="mt-6 grid gap-4 sm:grid-cols-2">
          {device.cafeName && (
            <div>
              <span className="text-xs text-gray-400">所属网吧</span>
              <p className="text-sm text-gray-900">{device.cafeName}</p>
            </div>
          )}
          {device.configLevel && (
            <div>
              <span className="text-xs text-gray-400">配置等级</span>
              <p className="text-sm text-gray-900">{device.configLevel}</p>
            </div>
          )}
          {device.ipAddress && (
            <div>
              <span className="text-xs text-gray-400">IP 地址</span>
              <p className="text-sm text-gray-900">{device.ipAddress}</p>
            </div>
          )}
          {device.gpuInfo && (
            <div>
              <span className="text-xs text-gray-400">GPU 信息</span>
              <p className="text-sm text-gray-900">{device.gpuInfo}</p>
            </div>
          )}
          {device.lastHeartbeat && (
            <div>
              <span className="text-xs text-gray-400">最后心跳</span>
              <p className="text-sm text-gray-900">
                {new Date(device.lastHeartbeat).toLocaleString('zh-CN')}
              </p>
            </div>
          )}
          {device.createdAt && (
            <div>
              <span className="text-xs text-gray-400">创建时间</span>
              <p className="text-sm text-gray-900">
                {new Date(device.createdAt).toLocaleString('zh-CN')}
              </p>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
