import { useState, useEffect } from 'react'
import { getRiskEvents, getRiskEventDetail, markAsFalsePositive, type RiskEvent } from '../../api/sessions'

const riskLevelColors: Record<string, string> = {
  CRITICAL: 'bg-red-100 text-red-800 border-red-200',
  HIGH: 'bg-orange-100 text-orange-800 border-orange-200',
  LOW: 'bg-yellow-100 text-yellow-800 border-yellow-200',
  NONE: 'bg-green-100 text-green-800 border-green-200',
}

const riskLevelLabels: Record<string, string> = {
  CRITICAL: '严重',
  HIGH: '高危',
  LOW: '低危',
  NONE: '正常',
}

export default function RiskEventPage() {
  const [events, setEvents] = useState<RiskEvent[]>([])
  const [loading, setLoading] = useState(true)
  const [page, setPage] = useState(1)
  const [totalPages, setTotalPages] = useState(1)
  const [selectedEvent, setSelectedEvent] = useState<RiskEvent | null>(null)
  const [detailLoading, setDetailLoading] = useState(false)
  const [actionMsg, setActionMsg] = useState('')

  const fetchEvents = async (pageNum: number) => {
    setLoading(true)
    try {
      const result = await getRiskEvents({ pageNum, pageSize: 15 })
      setEvents(result.records || [])
      setTotalPages(result.pages || 1)
    } catch (e) {
      console.error('Failed to fetch risk events', e)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchEvents(page)
  }, [page])

  const handleViewDetail = async (checkLogId: number) => {
    setDetailLoading(true)
    setActionMsg('')
    try {
      const detail = await getRiskEventDetail(checkLogId)
      setSelectedEvent(detail)
    } catch (e) {
      console.error('Failed to fetch risk event detail', e)
    } finally {
      setDetailLoading(false)
    }
  }

  const handleMarkFalsePositive = async () => {
    if (!selectedEvent) return
    try {
      await markAsFalsePositive(selectedEvent.checkLogId, 'Admin marked as false positive')
      setActionMsg('已标记为误报')
      setSelectedEvent((prev) => prev ? { ...prev, handled: true } : null)
      fetchEvents(page)
    } catch (e) {
      setActionMsg('操作失败')
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">风险事件管理</h1>
          <p className="mt-1 text-sm text-gray-500">
            监控和管理远程桌面内容安全违规事件
          </p>
        </div>
        <div className="flex items-center gap-2">
          <span className="inline-flex items-center gap-1 rounded-full bg-red-50 px-3 py-1 text-xs font-medium text-red-700">
            <span className="h-2 w-2 rounded-full bg-red-500 animate-pulse" />
            实时监控
          </span>
        </div>
      </div>

      {/* Risk Event List */}
      <div className="rounded-xl border border-gray-200 bg-white shadow-sm">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-gray-100 bg-gray-50">
                <th className="px-4 py-3 text-left font-medium text-gray-500">时间</th>
                <th className="px-4 py-3 text-left font-medium text-gray-500">风险等级</th>
                <th className="px-4 py-3 text-left font-medium text-gray-500">会话ID</th>
                <th className="px-4 py-3 text-left font-medium text-gray-500">违规分类</th>
                <th className="px-4 py-3 text-left font-medium text-gray-500">置信度</th>
                <th className="px-4 py-3 text-left font-medium text-gray-500">截图预览</th>
                <th className="px-4 py-3 text-left font-medium text-gray-500">状态</th>
                <th className="px-4 py-3 text-left font-medium text-gray-500">操作</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-50">
              {loading ? (
                <tr>
                  <td colSpan={8} className="px-4 py-12 text-center text-gray-400">
                    加载中...
                  </td>
                </tr>
              ) : events.length === 0 ? (
                <tr>
                  <td colSpan={8} className="px-4 py-12 text-center text-gray-400">
                    暂无风险事件
                  </td>
                </tr>
              ) : (
                events.map((event) => (
                  <tr key={event.checkLogId} className="hover:bg-gray-50 transition-colors">
                    <td className="px-4 py-3 text-gray-600 whitespace-nowrap">
                      {new Date(event.checkedAt).toLocaleString('zh-CN')}
                    </td>
                    <td className="px-4 py-3">
                      <span className={`inline-flex rounded-full border px-2.5 py-0.5 text-xs font-medium ${riskLevelColors[event.riskLevel] || riskLevelColors.NONE}`}>
                        {riskLevelLabels[event.riskLevel] || event.riskLevel}
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      <code className="rounded bg-gray-100 px-1.5 py-0.5 text-xs text-gray-600">
                        {event.sessionId.substring(0, 8)}...
                      </code>
                    </td>
                    <td className="px-4 py-3 text-gray-600 max-w-[200px] truncate">
                      {event.categories || '-'}
                    </td>
                    <td className="px-4 py-3 text-gray-600">
                      {(event.confidence * 100).toFixed(1)}%
                    </td>
                    <td className="px-4 py-3">
                      {event.screenshotThumbnailUrl ? (
                        <img
                          src={event.screenshotThumbnailUrl}
                          alt="screenshot"
                          className="h-12 w-20 rounded border border-gray-200 object-cover cursor-pointer hover:opacity-80 transition-opacity"
                          onClick={() => handleViewDetail(event.checkLogId)}
                        />
                      ) : (
                        <span className="text-xs text-gray-400">无预览</span>
                      )}
                    </td>
                    <td className="px-4 py-3">
                      {event.handled ? (
                        <span className="inline-flex items-center rounded-full bg-green-50 px-2 py-0.5 text-xs font-medium text-green-700">
                          已处理
                        </span>
                      ) : (
                        <span className="inline-flex items-center rounded-full bg-red-50 px-2 py-0.5 text-xs font-medium text-red-700">
                          待处理
                        </span>
                      )}
                    </td>
                    <td className="px-4 py-3">
                      <button
                        onClick={() => handleViewDetail(event.checkLogId)}
                        className="text-primary-600 hover:text-primary-700 text-xs font-medium"
                      >
                        查看详情
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        {totalPages > 1 && (
          <div className="flex items-center justify-between border-t border-gray-100 px-4 py-3">
            <span className="text-sm text-gray-500">
              第 {page} / {totalPages} 页
            </span>
            <div className="flex gap-2">
              <button
                onClick={() => setPage((p) => Math.max(1, p - 1))}
                disabled={page <= 1}
                className="rounded-lg border border-gray-200 px-3 py-1.5 text-sm text-gray-600 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                上一页
              </button>
              <button
                onClick={() => setPage((p) => Math.min(totalPages, p + 1))}
                disabled={page >= totalPages}
                className="rounded-lg border border-gray-200 px-3 py-1.5 text-sm text-gray-600 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                下一页
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Detail Modal */}
      {selectedEvent && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50" onClick={() => setSelectedEvent(null)}>
          <div className="w-full max-w-2xl rounded-xl bg-white p-6 shadow-xl" onClick={(e) => e.stopPropagation()}>
            <div className="flex items-center justify-between mb-6">
              <h2 className="text-lg font-bold text-gray-900">风险事件详情</h2>
              <button
                onClick={() => setSelectedEvent(null)}
                className="rounded-lg p-1.5 text-gray-400 hover:bg-gray-100 hover:text-gray-600"
              >
                <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>

            {detailLoading ? (
              <div className="py-12 text-center text-gray-400">加载中...</div>
            ) : (
              <div className="space-y-4">
                {/* Risk Level Badge */}
                <div className="flex items-center gap-3">
                  <span className="text-sm font-medium text-gray-500">风险等级：</span>
                  <span className={`inline-flex rounded-full border px-3 py-1 text-sm font-medium ${riskLevelColors[selectedEvent.riskLevel] || riskLevelColors.NONE}`}>
                    {riskLevelLabels[selectedEvent.riskLevel] || selectedEvent.riskLevel}
                  </span>
                  {selectedEvent.handled && (
                    <span className="inline-flex items-center rounded-full bg-green-50 px-2.5 py-0.5 text-xs font-medium text-green-700">
                      已处理
                    </span>
                  )}
                </div>

                {/* Event Info */}
                <div className="grid grid-cols-2 gap-4 rounded-lg bg-gray-50 p-4">
                  <div>
                    <span className="text-xs text-gray-400">检测时间</span>
                    <p className="text-sm text-gray-700">{new Date(selectedEvent.checkedAt).toLocaleString('zh-CN')}</p>
                  </div>
                  <div>
                    <span className="text-xs text-gray-400">会话ID</span>
                    <p className="text-sm text-gray-700 font-mono">{selectedEvent.sessionId}</p>
                  </div>
                  <div>
                    <span className="text-xs text-gray-400">违规分类</span>
                    <p className="text-sm text-gray-700">{selectedEvent.categories || '-'}</p>
                  </div>
                  <div>
                    <span className="text-xs text-gray-400">置信度</span>
                    <p className="text-sm text-gray-700">{(selectedEvent.confidence * 100).toFixed(1)}%</p>
                  </div>
                  <div>
                    <span className="text-xs text-gray-400">会话状态</span>
                    <p className="text-sm text-gray-700">{selectedEvent.sessionStatus || '-'}</p>
                  </div>
                  <div>
                    <span className="text-xs text-gray-400">持续时长</span>
                    <p className="text-sm text-gray-700">{selectedEvent.durationMinutes || 0} 分钟</p>
                  </div>
                </div>

                {/* Screenshot */}
                {selectedEvent.screenshotThumbnailUrl && (
                  <div>
                    <span className="text-sm font-medium text-gray-500 mb-2 block">违规截图：</span>
                    <img
                      src={selectedEvent.screenshotThumbnailUrl}
                      alt="Flagged screenshot"
                      className="w-full rounded-lg border border-gray-200 object-contain max-h-80"
                    />
                  </div>
                )}

                {/* Action */}
                {actionMsg && (
                  <div className={`rounded-lg px-4 py-2 text-sm ${actionMsg.includes('失败') ? 'bg-red-50 text-red-600' : 'bg-green-50 text-green-600'}`}>
                    {actionMsg}
                  </div>
                )}

                {!selectedEvent.handled && (
                  <div className="flex justify-end gap-3 pt-2">
                    <button
                      onClick={() => setSelectedEvent(null)}
                      className="rounded-lg border border-gray-200 px-4 py-2 text-sm text-gray-600 hover:bg-gray-50"
                    >
                      关闭
                    </button>
                    <button
                      onClick={handleMarkFalsePositive}
                      className="rounded-lg bg-primary-600 px-4 py-2 text-sm text-white hover:bg-primary-700"
                    >
                      标记为误报
                    </button>
                  </div>
                )}
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  )
}
