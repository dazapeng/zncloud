import { useState, useEffect } from 'react'
import { getDevices, batchUpdateDevices, wakeDevice, rebootDevice, poweroffDevice } from '../../api/devices'
import type { Device, DeviceListParams } from '../../api/devices'

const statusOptions = [
  { value: '', label: '全部状态' },
  { value: 'ONLINE', label: '在线' },
  { value: 'OFFLINE', label: '离线' },
  { value: 'PENDING_ONLINE', label: '唤醒中' },
  { value: 'REGISTERED', label: '已注册' },
  { value: 'IN_USE', label: '使用中' },
  { value: 'MAINTENANCE', label: '维护中' },
  { value: 'DISABLED', label: '已禁用' },
]

const statusLabels: Record<string, string> = {
  ONLINE: '在线',
  OFFLINE: '离线',
  PENDING_ONLINE: '唤醒中',
  REGISTERED: '已注册',
  IN_USE: '使用中',
  MAINTENANCE: '维护中',
  DISABLED: '已禁用',
}

const statusBadgeColors: Record<string, string> = {
  ONLINE: 'bg-green-100 text-green-700',
  OFFLINE: 'bg-gray-100 text-gray-500',
  PENDING_ONLINE: 'bg-blue-100 text-blue-700',
  REGISTERED: 'bg-purple-100 text-purple-700',
  IN_USE: 'bg-yellow-100 text-yellow-700',
  MAINTENANCE: 'bg-yellow-100 text-yellow-700',
  DISABLED: 'bg-red-100 text-red-700',
}

interface ConfirmDialog {
  show: boolean
  title: string
  message: string
  action: 'wake' | 'reboot' | 'poweroff'
  deviceId: string
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
  const [confirmDialog, setConfirmDialog] = useState<ConfirmDialog>({
    show: false,
    title: '',
    message: '',
    action: 'wake',
    deviceId: '',
  })
  const [powerLoading, setPowerLoading] = useState<Record<string, boolean>>({})

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

  // ========== 电源管理 ==========

  const showConfirmDialog = (deviceId: string, action: 'wake' | 'reboot' | 'poweroff') => {
    const titles: Record<string, string> = {
      wake: '确认唤醒',
      reboot: '确认重启',
      poweroff: '确认关机',
    }
    const messages: Record<string, string> = {
      wake: '确定要向此设备发送唤醒信号（WoL 魔术包）吗？设备必须处于离线状态。',
      reboot: '确定要远程重启此设备吗？设备当前正在运行的任务将被中断。',
      poweroff: '确定要远程关闭此设备吗？设备将立即关机。',
    }
    setConfirmDialog({
      show: true,
      title: titles[action],
      message: messages[action],
      action,
      deviceId,
    })
  }

  const handlePowerAction = async () => {
    const { action, deviceId } = confirmDialog
    setConfirmDialog((prev) => ({ ...prev, show: false }))
    setPowerLoading((prev) => ({ ...prev, [deviceId]: true }))

    try {
      switch (action) {
        case 'wake':
          await wakeDevice(deviceId)
          break
        case 'reboot':
          await rebootDevice(deviceId)
          break
        case 'poweroff':
          await poweroffDevice(deviceId)
          break
      }
      fetchDevices()
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : `${action === 'wake' ? '唤醒' : action === 'reboot' ? '重启' : '关机'}操作失败`)
    } finally {
      setPowerLoading((prev) => ({ ...prev, [deviceId]: false }))
    }
  }

  const canWake = (status: string) => status === 'OFFLINE' || status === 'REGISTERED'
  const canReboot = (status: string) => status === 'ONLINE' || status === 'IN_USE'
  const canPoweroff = (status: string) => status === 'ONLINE' || status === 'IN_USE'

  return (
    <div>
      <h1 className="mb-4 text-xl font-bold text-gray-900">设备管理</h1>

      {/* 确认对话框 */}
      {confirmDialog.show && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
          <div className="w-full max-w-sm rounded-lg bg-white p-6 shadow-xl">
            <h3 className="mb-2 text-lg font-semibold text-gray-900">{confirmDialog.title}</h3>
            <p className="mb-6 text-sm text-gray-600">{confirmDialog.message}</p>
            <div className="flex justify-end gap-3">
              <button
                className="btn-secondary text-sm !px-4 !py-2"
                onClick={() => setConfirmDialog((prev) => ({ ...prev, show: false }))}
              >
                取消
              </button>
              <button
                className="rounded bg-primary-600 px-4 py-2 text-sm text-white hover:bg-primary-700"
                onClick={handlePowerAction}
              >
                确认
              </button>
            </div>
          </div>
        </div>
      )}

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
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                  电源操作
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
              ) : devices.length === 0 ? (
                <tr>
                  <td colSpan={9} className="px-4 py-12 text-center text-gray-400">
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
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-1">
                        {/* 唤醒按钮 — 仅设备离线或已注册时显示 */}
                        {canWake(device.status) && (
                          <button
                            className="rounded bg-blue-50 px-2 py-1 text-xs text-blue-600 hover:bg-blue-100 disabled:opacity-50 disabled:cursor-not-allowed"
                            disabled={powerLoading[device.id]}
                            onClick={() => showConfirmDialog(String(device.id), 'wake')}
                          >
                            {powerLoading[device.id] ? '...' : '唤醒'}
                          </button>
                        )}
                        {/* 重启按钮 — 仅设备在线或使用中时显示 */}
                        {canReboot(device.status) && (
                          <button
                            className="rounded bg-yellow-50 px-2 py-1 text-xs text-yellow-600 hover:bg-yellow-100 disabled:opacity-50 disabled:cursor-not-allowed"
                            disabled={powerLoading[device.id]}
                            onClick={() => showConfirmDialog(String(device.id), 'reboot')}
                          >
                            {powerLoading[device.id] ? '...' : '重启'}
                          </button>
                        )}
                        {/* 关机按钮 — 仅设备在线或使用中时显示 */}
                        {canPoweroff(device.status) && (
                          <button
                            className="rounded bg-red-50 px-2 py-1 text-xs text-red-600 hover:bg-red-100 disabled:opacity-50 disabled:cursor-not-allowed"
                            disabled={powerLoading[device.id]}
                            onClick={() => showConfirmDialog(String(device.id), 'poweroff')}
                          >
                            {powerLoading[device.id] ? '...' : '关机'}
                          </button>
                        )}
                        {/* 无可用操作时显示占位 */}
                        {!canWake(device.status) && !canReboot(device.status) && !canPoweroff(device.status) && (
                          <span className="text-xs text-gray-300">-</span>
                        )}
                      </div>
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
