import { useState, useEffect, useCallback } from 'react'
import { useAuthStore } from '../../stores/authStore'
import {
  getRates,
  updateRate,
  getRateHistory,
  getActiveRates,
  batchCreateRates,
  configLevelLabels,
  configLevelColors,
  rateStatusLabels,
  rateStatusColors,
} from '../../api/billingRates'
import type {
  BillingRateResponse,
  BillingRateRequest,
  ConfigLevel,
  BatchRateRequest,
} from '../../api/billingRates'

/* ──────────────────────────────────────────────
   视图切换标签
   ────────────────────────────────────────────── */
type ViewTab = 'cafe' | 'batch' | 'history'

const viewTabs: { key: ViewTab; label: string; icon: string }[] = [
  { key: 'cafe', label: '网吧费率', icon: '🏪' },
  { key: 'batch', label: '批量调价', icon: '📦' },
  { key: 'history', label: '历史记录', icon: '📋' },
]

/* ──────────────────────────────────────────────
   模拟数据（后端未就绪时使用）
   ────────────────────────────────────────────── */
const MOCK_CAFES = [
  { id: 'cafe-001', name: '极速网咖（南京路店）', province: '上海', city: '上海市' },
  { id: 'cafe-002', name: '电竞风暴（天河店）', province: '广东', city: '广州市' },
  { id: 'cafe-003', name: '云悦网咖（春熙路店）', province: '四川', city: '成都市' },
  { id: 'cafe-004', name: '蓝海电竞（解放路店）', province: '浙江', city: '杭州市' },
  { id: 'cafe-005', name: '星耀网咖（徐汇店）', province: '上海', city: '上海市' },
]

type ConfigHardware = {
  level: ConfigLevel
  gpu: string
  cpu: string
  memory: string
  pricePerHour: number
  discountStart: number | null
  discountEnd: number | null
  discountRate: number | null
}

const MOCK_HARDWARE: Record<string, ConfigHardware[]> = {
  'cafe-001': [
    { level: 'ENTRY', gpu: 'GTX 1650', cpu: 'i5-12400', memory: '16GB', pricePerHour: 4, discountStart: null, discountEnd: null, discountRate: null },
    { level: 'MAINSTREAM', gpu: 'RTX 3060', cpu: 'i7-12700', memory: '32GB', pricePerHour: 6, discountStart: 23, discountEnd: 8, discountRate: 0.7 },
    { level: 'HIGH_PERFORMANCE', gpu: 'RTX 4070', cpu: 'i9-13900', memory: '32GB', pricePerHour: 10, discountStart: null, discountEnd: null, discountRate: null },
  ],
  'cafe-002': [
    { level: 'ENTRY', gpu: 'GTX 1660', cpu: 'i5-13400', memory: '16GB', pricePerHour: 5, discountStart: null, discountEnd: null, discountRate: null },
    { level: 'MAINSTREAM', gpu: 'RTX 4060', cpu: 'i7-13700', memory: '32GB', pricePerHour: 7, discountStart: 0, discountEnd: 6, discountRate: 0.6 },
    { level: 'HIGH_PERFORMANCE', gpu: 'RTX 4080', cpu: 'i9-14900', memory: '64GB', pricePerHour: 12, discountStart: null, discountEnd: null, discountRate: null },
  ],
  'cafe-003': [
    { level: 'ENTRY', gpu: 'GTX 1650', cpu: 'i5-12400', memory: '16GB', pricePerHour: 3.5, discountStart: null, discountEnd: null, discountRate: null },
    { level: 'MAINSTREAM', gpu: 'RTX 3060', cpu: 'i5-13500', memory: '32GB', pricePerHour: 5.5, discountStart: null, discountEnd: null, discountRate: null },
  ],
  'cafe-004': [
    { level: 'ENTRY', gpu: 'GTX 1660', cpu: 'i5-13400', memory: '16GB', pricePerHour: 4.5, discountStart: null, discountEnd: null, discountRate: null },
    { level: 'MAINSTREAM', gpu: 'RTX 4060', cpu: 'i7-13700', memory: '32GB', pricePerHour: 6.5, discountStart: 1, discountEnd: 7, discountRate: 0.65 },
  ],
  'cafe-005': [
    { level: 'ENTRY', gpu: 'GTX 1650', cpu: 'i5-12400', memory: '16GB', pricePerHour: 3, discountStart: null, discountEnd: null, discountRate: null },
    { level: 'MAINSTREAM', gpu: 'RTX 3050', cpu: 'i5-13500', memory: '16GB', pricePerHour: 5, discountStart: null, discountEnd: null, discountRate: null },
    { level: 'HIGH_PERFORMANCE', gpu: 'RTX 4070', cpu: 'i9-13900', memory: '32GB', pricePerHour: 9, discountStart: null, discountEnd: null, discountRate: null },
  ],
}

const MOCK_HISTORY: {
  id: number
  cafeId: string
  cafeName: string
  configLevel: ConfigLevel
  oldPrice: number | null
  newPrice: number
  operator: string
  createdAt: string
}[] = [
  { id: 1, cafeId: 'cafe-001', cafeName: '极速网咖（南京路店）', configLevel: 'MAINSTREAM', oldPrice: 5, newPrice: 6, operator: '管理员', createdAt: '2026-05-25 14:30:00' },
  { id: 2, cafeId: 'cafe-002', cafeName: '电竞风暴（天河店）', configLevel: 'HIGH_PERFORMANCE', oldPrice: 15, newPrice: 12, operator: '运营小李', createdAt: '2026-05-24 10:15:00' },
  { id: 3, cafeId: 'cafe-003', cafeName: '云悦网咖（春熙路店）', configLevel: 'MAINSTREAM', oldPrice: null, newPrice: 5.5, operator: '管理员', createdAt: '2026-05-23 09:00:00' },
  { id: 4, cafeId: 'cafe-001', cafeName: '极速网咖（南京路店）', configLevel: 'ENTRY', oldPrice: 3, newPrice: 4, operator: '管理员', createdAt: '2026-05-22 16:45:00' },
  { id: 5, cafeId: 'cafe-004', cafeName: '蓝海电竞（解放路店）', configLevel: 'MAINSTREAM', oldPrice: 8, newPrice: 6.5, operator: '运营小李', createdAt: '2026-05-21 11:20:00' },
]

const ALL_CONFIG_LEVELS: ConfigLevel[] = ['ENTRY', 'MAINSTREAM', 'HIGH_PERFORMANCE']

/* ──────────────────────────────────────────────
   主页面组件
   ────────────────────────────────────────────── */
export default function RateConfigPage() {
  const { user } = useAuthStore()
  const [activeTab, setActiveTab] = useState<ViewTab>('cafe')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [successMsg, setSuccessMsg] = useState('')
  const [useMock, setUseMock] = useState(true) // 后端未就绪时使用模拟数据

  const showSuccess = (msg: string) => {
    setSuccessMsg(msg)
    setTimeout(() => setSuccessMsg(''), 3000)
  }

  const showError = (msg: string) => {
    setError(msg)
    setTimeout(() => setError(''), 5000)
  }

  /* ── 尝试从后端获取数据，失败则降级到 mock ── */
  const tryFetch = useCallback(async <T,>(fetcher: () => Promise<T>, mockFallback: T): Promise<T> => {
    try {
      setLoading(true)
      setError('')
      const result = await fetcher()
      setUseMock(false)
      return result
    } catch {
      setUseMock(true)
      return mockFallback
    } finally {
      setLoading(false)
    }
  }, [])

  return (
    <div>
      {/* 页面标题 */}
      <h1 className="mb-6 text-xl font-bold text-gray-900">费率设置</h1>

      {/* 全局消息提示 */}
      {successMsg && (
        <div className="mb-4 rounded-lg bg-green-50 p-3 text-sm text-green-700">
          {successMsg}
          <button className="ml-2 underline" onClick={() => setSuccessMsg('')}>关闭</button>
        </div>
      )}
      {error && (
        <div className="mb-4 rounded-lg bg-red-50 p-3 text-sm text-red-600">
          {error}
          <button className="ml-2 underline" onClick={() => setError('')}>关闭</button>
        </div>
      )}

      {useMock && (
        <div className="mb-4 rounded-lg bg-yellow-50 p-3 text-xs text-yellow-700">
          ⚠️ 当前使用模拟数据展示，后端 API 未响应时自动降级。
        </div>
      )}

      {/* 视图切换标签 */}
      <div className="mb-6 flex gap-2 border-b border-gray-200">
        {viewTabs.map((tab) => (
          <button
            key={tab.key}
            onClick={() => setActiveTab(tab.key)}
            className={`flex items-center gap-1.5 px-4 py-2.5 text-sm font-medium border-b-2 transition-colors ${
              activeTab === tab.key
                ? 'border-primary-500 text-primary-600'
                : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
            }`}
          >
            <span>{tab.icon}</span>
            <span>{tab.label}</span>
          </button>
        ))}
      </div>

      {/* 视图内容 */}
      {activeTab === 'cafe' && <CafeRateView tryFetch={tryFetch} showSuccess={showSuccess} showError={showError} useMock={useMock} />}
      {activeTab === 'batch' && <BatchRateView tryFetch={tryFetch} showSuccess={showSuccess} showError={showError} />}
      {activeTab === 'history' && <HistoryView tryFetch={tryFetch} useMock={useMock} />}
    </div>
  )
}

/* ════════════════════════════════════════════════
   视图 1: 网吧费率管理
   ════════════════════════════════════════════════ */
function CafeRateView({
  tryFetch,
  showSuccess,
  showError,
  useMock,
}: {
  tryFetch: <T>(fetcher: () => Promise<T>, fallback: T) => Promise<T>
  showSuccess: (msg: string) => void
  showError: (msg: string) => void
  useMock: boolean
}) {
  const { user } = useAuthStore()
  const [selectedCafeId, setSelectedCafeId] = useState<string>(
    user?.role === 'CAFE_ADMIN' && user.cafeId ? String(user.cafeId) : ''
  )
  const [rates, setRates] = useState<BillingRateResponse[]>([])
  const [loading, setLoading] = useState(false)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [editForm, setEditForm] = useState<{
    pricePerHour: string
    discountStart: string
    discountEnd: string
    discountRate: string
  }>({ pricePerHour: '', discountStart: '', discountEnd: '', discountRate: '' })
  const [saving, setSaving] = useState(false)

  const cafes = useMock ? MOCK_CAFES : MOCK_CAFES // 后端未完成时统一用 mock

  const loadRates = useCallback(async (cafeId: string) => {
    if (!cafeId) return
    setLoading(true)
    try {
      const data = await tryFetch(() => getRates(cafeId), [])
      if (data.length > 0) {
        setRates(data)
      } else if (useMock && MOCK_HARDWARE[cafeId]) {
        // 无数据时从 mock 硬件配置构建
        const built: BillingRateResponse[] = MOCK_HARDWARE[cafeId].map((h) => ({
          id: 0,
          cafeId,
          configLevel: h.level,
          pricePerHour: h.pricePerHour,
          discountStart: h.discountStart,
          discountEnd: h.discountEnd,
          discountRate: h.discountRate,
          effectiveAt: new Date().toISOString(),
          status: 'ACTIVE' as const,
          createdBy: '',
          remark: '',
          createdAt: new Date().toISOString(),
        }))
        setRates(built)
      } else {
        setRates([])
      }
    } finally {
      setLoading(false)
    }
  }, [tryFetch, useMock])

  useEffect(() => {
    if (selectedCafeId) loadRates(selectedCafeId)
  }, [selectedCafeId, loadRates])

  const handleEdit = (rate: BillingRateResponse) => {
    setEditingId(rate.id)
    setEditForm({
      pricePerHour: String(rate.pricePerHour),
      discountStart: rate.discountStart != null ? String(rate.discountStart) : '',
      discountEnd: rate.discountEnd != null ? String(rate.discountEnd) : '',
      discountRate: rate.discountRate != null ? String(rate.discountRate) : '',
    })
  }

  const handleSave = async (rate: BillingRateResponse) => {
    const price = parseFloat(editForm.pricePerHour)
    if (isNaN(price) || price <= 0) {
      showError('请输入有效的小时单价')
      return
    }

    const ds = editForm.discountStart ? parseInt(editForm.discountStart) : null
    const de = editForm.discountEnd ? parseInt(editForm.discountEnd) : null
    const dr = editForm.discountRate ? parseFloat(editForm.discountRate) : null

    if ((ds != null || de != null || dr != null) && !(ds != null && de != null && dr != null)) {
      showError('折扣时段需同时填写开始、结束时间和折扣率')
      return
    }

    setSaving(true)
    try {
      const request: BillingRateRequest = {
        cafeId: selectedCafeId,
        configLevel: rate.configLevel,
        pricePerHour: price,
        discountStart: ds,
        discountEnd: de,
        discountRate: dr,
      }

      if (!useMock && rate.id > 0) {
        await updateRate(rate.id, request)
      }
      // mock 模式下直接更新本地状态
      setRates((prev) =>
        prev.map((r) =>
          r.configLevel === rate.configLevel
            ? { ...r, pricePerHour: price, discountStart: ds, discountEnd: de, discountRate: dr }
            : r
        )
      )
      setEditingId(null)
      showSuccess(`${configLevelLabels[rate.configLevel]} 费率已更新`)
    } catch {
      showError('保存失败，请重试')
    } finally {
      setSaving(false)
    }
  }

  const handleCancelEdit = () => {
    setEditingId(null)
  }

  return (
    <div>
      {/* 网吧选择器 */}
      <div className="mb-6">
        <label className="mb-1.5 block text-sm font-medium text-gray-700">选择网吧</label>
        <select
          className="input-field max-w-md"
          value={selectedCafeId}
          onChange={(e) => setSelectedCafeId(e.target.value)}
        >
          <option value="">-- 请选择网吧 --</option>
          {cafes.map((cafe) => (
            <option key={cafe.id} value={cafe.id}>
              {cafe.name}（{cafe.province}·{cafe.city}）
            </option>
          ))}
        </select>
      </div>

      {!selectedCafeId ? (
        <div className="card text-center text-gray-400 py-12">请先选择一个网吧以查看费率配置</div>
      ) : loading ? (
        <div className="space-y-4">
          {[1, 2, 3].map((i) => (
            <div key={i} className="card animate-pulse">
              <div className="h-5 w-32 rounded bg-gray-200 mb-3" />
              <div className="h-8 w-full rounded bg-gray-100" />
            </div>
          ))}
        </div>
      ) : rates.length === 0 ? (
        <div className="card text-center text-gray-400 py-12">该网吧暂无费率配置</div>
      ) : (
        <div className="space-y-4">
          {rates.map((rate) => (
            <div key={rate.configLevel} className="card">
              {/* 配置标题 */}
              <div className="flex items-center justify-between mb-4">
                <div className="flex items-center gap-3">
                  <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${configLevelColors[rate.configLevel]}`}>
                    {configLevelLabels[rate.configLevel]}
                  </span>
                  {rate.status && (
                    <span className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs ${rateStatusColors[rate.status]}`}>
                      {rateStatusLabels[rate.status]}
                    </span>
                  )}
                </div>
                {editingId !== rate.id && (
                  <button className="btn-secondary text-xs !px-3 !py-1" onClick={() => handleEdit(rate)}>
                    编辑
                  </button>
                )}
              </div>

              {/* 编辑/查看模式 */}
              {editingId === rate.id ? (
                <div className="space-y-3">
                  <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
                    <div>
                      <label className="mb-1 block text-xs text-gray-500">每小时单价（元）</label>
                      <input
                        type="number"
                        className="input-field"
                        value={editForm.pricePerHour}
                        onChange={(e) => setEditForm((f) => ({ ...f, pricePerHour: e.target.value }))}
                        min="0"
                        step="0.5"
                      />
                    </div>
                    <div>
                      <label className="mb-1 block text-xs text-gray-500">折扣起始（时）</label>
                      <input
                        type="number"
                        className="input-field"
                        placeholder="留空无折扣"
                        value={editForm.discountStart}
                        onChange={(e) => setEditForm((f) => ({ ...f, discountStart: e.target.value }))}
                        min="0"
                        max="23"
                      />
                    </div>
                    <div>
                      <label className="mb-1 block text-xs text-gray-500">折扣结束（时）</label>
                      <input
                        type="number"
                        className="input-field"
                        placeholder="留空无折扣"
                        value={editForm.discountEnd}
                        onChange={(e) => setEditForm((f) => ({ ...f, discountEnd: e.target.value }))}
                        min="0"
                        max="23"
                      />
                    </div>
                    <div>
                      <label className="mb-1 block text-xs text-gray-500">折扣率（如 0.7）</label>
                      <input
                        type="number"
                        className="input-field"
                        placeholder="留空无折扣"
                        value={editForm.discountRate}
                        onChange={(e) => setEditForm((f) => ({ ...f, discountRate: e.target.value }))}
                        min="0"
                        max="1"
                        step="0.05"
                      />
                    </div>
                  </div>
                  {editForm.discountStart && editForm.discountEnd && editForm.discountRate && (
                    <p className="text-xs text-blue-600">
                      ⏰ {editForm.discountStart}:00〜{editForm.discountEnd}:00 时段折扣 {parseFloat(editForm.discountRate || '0') * 100}%（
                      实付 ¥{parseFloat(editForm.pricePerHour || '0') * parseFloat(editForm.discountRate || '1')}/小时）
                    </p>
                  )}
                  <div className="flex gap-2 pt-1">
                    <button
                      className="btn-primary text-sm"
                      onClick={() => handleSave(rate)}
                      disabled={saving}
                    >
                      {saving ? '保存中...' : '保存'}
                    </button>
                    <button className="btn-secondary text-sm" onClick={handleCancelEdit} disabled={saving}>
                      取消
                    </button>
                  </div>
                </div>
              ) : (
                <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4 text-sm">
                  <div>
                    <p className="text-xs text-gray-400">每小时单价</p>
                    <p className="mt-0.5 text-lg font-semibold text-gray-900">
                      ¥{rate.pricePerHour.toFixed(1)}
                    </p>
                  </div>
                  <div>
                    <p className="text-xs text-gray-400">折扣时段</p>
                    <p className="mt-0.5 text-gray-700">
                      {rate.discountStart != null && rate.discountEnd != null
                        ? `${String(rate.discountStart).padStart(2, '0')}:00〜${String(rate.discountEnd).padStart(2, '0')}:00`
                        : '无折扣'}
                    </p>
                  </div>
                  <div>
                    <p className="text-xs text-gray-400">折扣率</p>
                    <p className="mt-0.5 text-gray-700">
                      {rate.discountRate != null
                        ? `${(rate.discountRate * 100).toFixed(0)}%`
                        : '-'}
                    </p>
                  </div>
                  <div>
                    <p className="text-xs text-gray-400">生效时间</p>
                    <p className="mt-0.5 text-gray-500 text-xs">
                      {rate.effectiveAt ? new Date(rate.effectiveAt).toLocaleString('zh-CN') : '-'}
                    </p>
                  </div>
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

/* ════════════════════════════════════════════════
   视图 2: 批量调价
   ════════════════════════════════════════════════ */
function BatchRateView({
  tryFetch,
  showSuccess,
  showError,
}: {
  tryFetch: <T>(fetcher: () => Promise<T>, fallback: T) => Promise<T>
  showSuccess: (msg: string) => void
  showError: (msg: string) => void
}) {
  const [selectedLevel, setSelectedLevel] = useState<ConfigLevel | ''>('')
  const [selectedCafeIds, setSelectedCafeIds] = useState<string[]>([])
  const [pricePerHour, setPricePerHour] = useState('')
  const [discountStart, setDiscountStart] = useState('')
  const [discountEnd, setDiscountEnd] = useState('')
  const [discountRate, setDiscountRate] = useState('')
  const [saving, setSaving] = useState(false)
  const [history, setHistory] = useState(MOCK_HISTORY.slice(0, 5))

  const cafes = MOCK_CAFES

  const toggleCafe = (id: string) => {
    setSelectedCafeIds((prev) =>
      prev.includes(id) ? prev.filter((c) => c !== id) : [...prev, id]
    )
  }

  const toggleAll = () => {
    if (selectedCafeIds.length === cafes.length) {
      setSelectedCafeIds([])
    } else {
      setSelectedCafeIds(cafes.map((c) => c.id))
    }
  }

  const handleBatchSubmit = async () => {
    if (!selectedLevel) {
      showError('请选择配置等级')
      return
    }
    if (selectedCafeIds.length === 0) {
      showError('请至少选择一个网吧')
      return
    }
    const price = parseFloat(pricePerHour)
    if (isNaN(price) || price <= 0) {
      showError('请输入有效的小时单价')
      return
    }

    const ds = discountStart ? parseInt(discountStart) : null
    const de = discountEnd ? parseInt(discountEnd) : null
    const dr = discountRate ? parseFloat(discountRate) : null

    if ((ds != null || de != null || dr != null) && !(ds != null && de != null && dr != null)) {
      showError('折扣时段需同时填写开始、结束时间和折扣率')
      return
    }

    setSaving(true)
    try {
      const request: BatchRateRequest = {
        cafeIds: selectedCafeIds,
        configLevel: selectedLevel,
        pricePerHour: price,
        discountStart: ds,
        discountEnd: de,
        discountRate: dr,
        remark: `批量调价 - ${configLevelLabels[selectedLevel]}`,
      }

      await tryFetch(() => batchCreateRates(request), [])

      // 更新本地历史
      const newEntry = {
        id: Date.now(),
        cafeId: '',
        cafeName: `${selectedCafeIds.length} 个网吧`,
        configLevel: selectedLevel,
        oldPrice: null,
        newPrice: price,
        operator: '管理员',
        createdAt: new Date().toLocaleString('zh-CN'),
      }
      setHistory((prev) => [newEntry, ...prev].slice(0, 20))

      showSuccess(`已对 ${selectedCafeIds.length} 个网吧的 ${configLevelLabels[selectedLevel]} 批量调价为 ¥${price}/小时`)
      setPricePerHour('')
      setDiscountStart('')
      setDiscountEnd('')
      setDiscountRate('')
    } catch {
      showError('批量调价失败，请重试')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="grid gap-6 lg:grid-cols-3">
      {/* 左侧：调价表单 */}
      <div className="lg:col-span-2 space-y-6">
        {/* 配置等级选择 */}
        <div className="card">
          <h3 className="mb-3 text-sm font-medium text-gray-700">1. 选择配置等级</h3>
          <div className="flex flex-wrap gap-2">
            {ALL_CONFIG_LEVELS.map((level) => (
              <button
                key={level}
                onClick={() => setSelectedLevel(level)}
                className={`rounded-lg border px-4 py-2 text-sm font-medium transition-colors ${
                  selectedLevel === level
                    ? 'border-primary-500 bg-primary-50 text-primary-700'
                    : 'border-gray-200 text-gray-600 hover:border-gray-300 hover:bg-gray-50'
                }`}
              >
                {configLevelLabels[level]}
              </button>
            ))}
          </div>
        </div>

        {/* 网吧选择 */}
        <div className="card">
          <div className="flex items-center justify-between mb-3">
            <h3 className="text-sm font-medium text-gray-700">2. 选择网吧</h3>
            <button className="text-xs text-primary-600 hover:underline" onClick={toggleAll}>
              {selectedCafeIds.length === cafes.length ? '取消全选' : '全选'}
            </button>
          </div>
          <div className="max-h-60 overflow-y-auto space-y-1">
            {cafes.map((cafe) => (
              <label
                key={cafe.id}
                className={`flex items-center gap-3 rounded-lg px-3 py-2 cursor-pointer transition-colors ${
                  selectedCafeIds.includes(cafe.id) ? 'bg-primary-50' : 'hover:bg-gray-50'
                }`}
              >
                <input
                  type="checkbox"
                  className="h-4 w-4 rounded border-gray-300 text-primary-600 focus:ring-primary-300"
                  checked={selectedCafeIds.includes(cafe.id)}
                  onChange={() => toggleCafe(cafe.id)}
                />
                <span className="text-sm text-gray-700">{cafe.name}</span>
                <span className="ml-auto text-xs text-gray-400">{cafe.province}·{cafe.city}</span>
              </label>
            ))}
          </div>
          <p className="mt-2 text-xs text-gray-400">已选择 {selectedCafeIds.length} 个网吧</p>
        </div>

        {/* 价格设置 */}
        <div className="card">
          <h3 className="mb-3 text-sm font-medium text-gray-700">3. 设置统一价格</h3>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <div>
              <label className="mb-1 block text-xs text-gray-500">每小时单价（元）</label>
              <input
                type="number"
                className="input-field"
                placeholder="输入价格"
                value={pricePerHour}
                onChange={(e) => setPricePerHour(e.target.value)}
                min="0"
                step="0.5"
              />
            </div>
            <div>
              <label className="mb-1 block text-xs text-gray-500">折扣起始（时）</label>
              <input
                type="number"
                className="input-field"
                placeholder="留空无折扣"
                value={discountStart}
                onChange={(e) => setDiscountStart(e.target.value)}
                min="0"
                max="23"
              />
            </div>
            <div>
              <label className="mb-1 block text-xs text-gray-500">折扣结束（时）</label>
              <input
                type="number"
                className="input-field"
                placeholder="留空无折扣"
                value={discountEnd}
                onChange={(e) => setDiscountEnd(e.target.value)}
                min="0"
                max="23"
              />
            </div>
            <div>
              <label className="mb-1 block text-xs text-gray-500">折扣率（如 0.7）</label>
              <input
                type="number"
                className="input-field"
                placeholder="留空无折扣"
                value={discountRate}
                onChange={(e) => setDiscountRate(e.target.value)}
                min="0"
                max="1"
                step="0.05"
              />
            </div>
          </div>

          {/* 预览 */}
          {selectedLevel && selectedCafeIds.length > 0 && pricePerHour && (
            <div className="mt-3 rounded-lg bg-blue-50 p-3 text-xs text-blue-700">
              📋 将对 <strong>{selectedCafeIds.length}</strong> 个网吧的{' '}
              <strong>{configLevelLabels[selectedLevel]}</strong>
              {discountStart && discountEnd && discountRate
                ? ` 设置 ¥${pricePerHour}/小时（${discountStart}:00〜${discountEnd}:00 折扣 ${parseFloat(discountRate) * 100}%）`
                : ` 设置 ¥${pricePerHour}/小时`}
            </div>
          )}

          <button
            className="btn-primary mt-4 text-sm"
            onClick={handleBatchSubmit}
            disabled={saving || !selectedLevel || selectedCafeIds.length === 0}
          >
            {saving ? '提交中...' : `确认批量调价`}
          </button>
        </div>
      </div>

      {/* 右侧：最近调价记录 */}
      <div className="card self-start !p-0 overflow-hidden">
        <div className="px-4 py-3 border-b border-gray-100">
          <h3 className="text-sm font-medium text-gray-700">最近调价记录</h3>
        </div>
        <div className="divide-y divide-gray-50 max-h-96 overflow-y-auto">
          {history.length === 0 ? (
            <div className="px-4 py-8 text-center text-xs text-gray-400">暂无调价记录</div>
          ) : (
            history.map((h) => (
              <div key={h.id} className="px-4 py-3">
                <div className="flex items-center justify-between">
                  <span className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${configLevelColors[h.configLevel]}`}>
                    {configLevelLabels[h.configLevel]}
                  </span>
                  <span className="text-xs text-gray-400">{h.createdAt}</span>
                </div>
                <p className="mt-1 text-sm text-gray-700">
                  {h.cafeName}
                </p>
                <p className="text-xs text-gray-500">
                  {h.oldPrice != null ? `¥${h.oldPrice.toFixed(1)} → ` : ''}
                  <span className="font-medium text-green-600">¥{h.newPrice.toFixed(1)}</span>
                  <span className="ml-2 text-gray-400">/时 · {h.operator}</span>
                </p>
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  )
}

/* ── 历史记录条目类型 ── */
interface HistoryEntry {
  id: number
  cafeId: string
  cafeName: string
  configLevel: ConfigLevel
  oldPrice: number | null
  newPrice: number
  operator: string
  createdAt: string
}

/* ════════════════════════════════════════════════
   视图 3: 历史费率变更记录
   ════════════════════════════════════════════════ */
function HistoryView({
  tryFetch,
  useMock,
}: {
  tryFetch: <T>(fetcher: () => Promise<T>, fallback: T) => Promise<T>
  useMock: boolean
}) {
  const [history, setHistory] = useState<HistoryEntry[]>([])
  const [loading, setLoading] = useState(true)
  const [filterCafeId, setFilterCafeId] = useState('')
  const [filterLevel, setFilterLevel] = useState<ConfigLevel | ''>('')

  useEffect(() => {
    const load = async () => {
      setLoading(true)
      try {
        if (useMock) {
          setHistory(MOCK_HISTORY)
        } else {
          const data = await tryFetch(
            () => getRateHistory(filterCafeId || undefined, filterLevel || undefined),
            [] as unknown as BillingRateResponse[]
          )
          // 将后端返回的 BillingRateResponse 映射为 HistoryEntry
          const mapped: HistoryEntry[] = (data as BillingRateResponse[]).map((r) => ({
            id: r.id,
            cafeId: r.cafeId,
            cafeName: '网吧 #' + r.cafeId,
            configLevel: r.configLevel,
            oldPrice: r.pricePerHour,
            newPrice: r.pricePerHour,
            operator: r.createdBy || '系统',
            createdAt: r.createdAt || r.effectiveAt,
          }))
          setHistory(mapped.length > 0 ? mapped : MOCK_HISTORY)
        }
      } finally {
        setLoading(false)
      }
    }
    load()
  }, [useMock, filterCafeId, filterLevel, tryFetch])

  const filtered = history.filter((h) => {
    if (filterCafeId && h.cafeId !== filterCafeId) return false
    if (filterLevel && h.configLevel !== filterLevel) return false
    return true
  })

  return (
    <div>
      {/* 筛选条件 */}
      <div className="mb-6 flex flex-wrap gap-4">
        <div>
          <label className="mb-1 block text-xs text-gray-500">网吧筛选</label>
          <select
            className="input-field max-w-xs"
            value={filterCafeId}
            onChange={(e) => setFilterCafeId(e.target.value)}
          >
            <option value="">全部网吧</option>
            {MOCK_CAFES.map((cafe) => (
              <option key={cafe.id} value={cafe.id}>{cafe.name}</option>
            ))}
          </select>
        </div>
        <div>
          <label className="mb-1 block text-xs text-gray-500">配置等级</label>
          <select
            className="input-field max-w-[140px]"
            value={filterLevel}
            onChange={(e) => setFilterLevel(e.target.value as ConfigLevel | '')}
          >
            <option value="">全部等级</option>
            {ALL_CONFIG_LEVELS.map((level) => (
              <option key={level} value={level}>{configLevelLabels[level]}</option>
            ))}
          </select>
        </div>
      </div>

      {/* 时间线 */}
      {loading ? (
        <div className="space-y-3">
          {[1, 2, 3, 4, 5].map((i) => (
            <div key={i} className="card animate-pulse">
              <div className="h-4 w-48 rounded bg-gray-200 mb-2" />
              <div className="h-4 w-32 rounded bg-gray-100" />
            </div>
          ))}
        </div>
      ) : filtered.length === 0 ? (
        <div className="card text-center text-gray-400 py-12">暂无费率变更记录</div>
      ) : (
        <div className="relative">
          {/* 纵向时间线 */}
          <div className="absolute left-4 top-2 bottom-2 w-0.5 bg-gray-200" />
          <div className="space-y-4">
            {filtered.map((h, idx) => (
              <div key={h.id || idx} className="relative pl-10">
                {/* 时间线圆点 */}
                <div className="absolute left-2.5 top-1.5 h-3 w-3 rounded-full border-2 border-primary-400 bg-white" />
                {/* 内容卡片 */}
                <div className="card !py-3 !px-4">
                  <div className="flex items-center justify-between flex-wrap gap-2">
                    <div className="flex items-center gap-2">
                      <span className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${configLevelColors[h.configLevel]}`}>
                        {configLevelLabels[h.configLevel]}
                      </span>
                      <span className="text-sm font-medium text-gray-700">{h.cafeName}</span>
                    </div>
                    <span className="text-xs text-gray-400">{h.createdAt}</span>
                  </div>
                  <div className="mt-1.5 flex items-center gap-2 text-sm">
                    <span className="text-gray-500">价格变动：</span>
                    {h.oldPrice != null ? (
                      <>
                        <span className="text-gray-400 line-through">¥{h.oldPrice.toFixed(1)}</span>
                        <span className="text-gray-300">→</span>
                      </>
                    ) : null}
                    <span className="font-semibold text-green-600">¥{h.newPrice.toFixed(1)}</span>
                    <span className="text-xs text-gray-400">/小时</span>
                  </div>
                  <div className="mt-1 text-xs text-gray-400">
                    操作人：{h.operator}
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}
