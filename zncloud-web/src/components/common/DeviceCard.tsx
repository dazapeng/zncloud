import type { Device } from '../../api/devices'

interface Props {
  device: Device
  onClick?: () => void
}

const statusLabels: Record<string, string> = {
  ONLINE: '在线',
  OFFLINE: '离线',
  MAINTENANCE: '维护中',
  DISABLED: '已禁用',
}

const statusColors: Record<string, string> = {
  ONLINE: 'bg-green-100 text-green-700',
  OFFLINE: 'bg-gray-100 text-gray-500',
  MAINTENANCE: 'bg-yellow-100 text-yellow-700',
  DISABLED: 'bg-red-100 text-red-700',
}

export default function DeviceCard({ device, onClick }: Props) {
  return (
    <div
      className="card cursor-pointer transition-shadow hover:shadow-md"
      onClick={onClick}
    >
      <div className="flex items-start justify-between">
        <div>
          <h3 className="text-base font-semibold text-gray-900">{device.name}</h3>
          <p className="mt-0.5 text-xs text-gray-400">编号: {device.deviceCode}</p>
        </div>
        <span
          className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${statusColors[device.status] || statusColors.OFFLINE}`}
        >
          {statusLabels[device.status] || '未知'}
        </span>
      </div>

      <div className="mt-4 grid grid-cols-2 gap-3 text-sm">
        <div>
          <span className="text-gray-400">CPU</span>
          <div className="mt-1 flex items-center gap-2">
            <div className="h-2 flex-1 rounded-full bg-gray-200">
              <div
                className="h-2 rounded-full bg-primary-500"
                style={{ width: `${device.cpuUsage ?? 0}%` }}
              />
            </div>
            <span className="w-10 text-right text-xs text-gray-500">
              {device.cpuUsage ?? '-'}%
            </span>
          </div>
        </div>
        <div>
          <span className="text-gray-400">内存</span>
          <div className="mt-1 flex items-center gap-2">
            <div className="h-2 flex-1 rounded-full bg-gray-200">
              <div
                className="h-2 rounded-full bg-primary-500"
                style={{ width: `${device.memUsage ?? 0}%` }}
              />
            </div>
            <span className="w-10 text-right text-xs text-gray-500">
              {device.memUsage ?? '-'}%
            </span>
          </div>
        </div>
      </div>

      {device.cafeName && (
        <p className="mt-2 text-xs text-gray-400">
          所属网吧: {device.cafeName}
        </p>
      )}

      {device.lastHeartbeat && (
        <p className="mt-0.5 text-xs text-gray-400">
          最后心跳: {new Date(device.lastHeartbeat).toLocaleString('zh-CN')}
        </p>
      )}
    </div>
  )
}
