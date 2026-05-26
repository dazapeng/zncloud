import { useState } from 'react'

// 占位财务数据
interface FinanceRecord {
  id: number
  date: string
  type: string
  description: string
  amount: number
  status: 'COMPLETED' | 'PENDING' | 'FAILED'
}

const mockRecords: FinanceRecord[] = [
  { id: 1, date: '2026-05-25', type: '充值', description: '用户充值 - 张三', amount: 100.0, status: 'COMPLETED' },
  { id: 2, date: '2026-05-25', type: '消费', description: '上机消费 - 李四', amount: -15.0, status: 'COMPLETED' },
  { id: 3, date: '2026-05-24', type: '提现', description: '网吧提现 - 网鱼网咖', amount: -5000.0, status: 'PENDING' },
  { id: 4, date: '2026-05-24', type: '充值', description: '用户充值 - 王五', amount: 50.0, status: 'COMPLETED' },
  { id: 5, date: '2026-05-23', type: '退款', description: '退款 - 设备故障', amount: 20.0, status: 'COMPLETED' },
]

export default function CafeFinancePage() {
  const [records] = useState<FinanceRecord[]>(mockRecords)
  const [dateRange, setDateRange] = useState({ start: '', end: '' })

  const totalIncome = records
    .filter((r) => r.amount > 0 && r.status === 'COMPLETED')
    .reduce((sum, r) => sum + r.amount, 0)

  const totalExpense = records
    .filter((r) => r.amount < 0 && r.status === 'COMPLETED')
    .reduce((sum, r) => sum + Math.abs(r.amount), 0)

  const statusLabels: Record<string, string> = {
    COMPLETED: '已完成',
    PENDING: '处理中',
    FAILED: '失败',
  }

  const statusColors: Record<string, string> = {
    COMPLETED: 'text-green-600 bg-green-50',
    PENDING: 'text-yellow-600 bg-yellow-50',
    FAILED: 'text-red-600 bg-red-50',
  }

  return (
    <div>
      <h1 className="mb-6 text-xl font-bold text-gray-900">财务管理</h1>

      {/* 统计卡片 */}
      <div className="mb-6 grid gap-4 sm:grid-cols-3">
        <div className="card">
          <p className="text-sm text-gray-500">总收入</p>
          <p className="mt-1 text-2xl font-bold text-green-600">¥{totalIncome.toFixed(2)}</p>
        </div>
        <div className="card">
          <p className="text-sm text-gray-500">总支出</p>
          <p className="mt-1 text-2xl font-bold text-red-500">¥{totalExpense.toFixed(2)}</p>
        </div>
        <div className="card">
          <p className="text-sm text-gray-500">净收入</p>
          <p className={`mt-1 text-2xl font-bold ${totalIncome - totalExpense >= 0 ? 'text-green-600' : 'text-red-500'}`}>
            ¥{(totalIncome - totalExpense).toFixed(2)}
          </p>
        </div>
      </div>

      {/* 筛选 */}
      <div className="mb-4 flex flex-wrap items-center gap-3">
        <div>
          <label className="mr-2 text-xs text-gray-500">开始日期</label>
          <input
            type="date"
            className="input-field w-auto"
            value={dateRange.start}
            onChange={(e) => setDateRange((prev) => ({ ...prev, start: e.target.value }))}
          />
        </div>
        <div>
          <label className="mr-2 text-xs text-gray-500">结束日期</label>
          <input
            type="date"
            className="input-field w-auto"
            value={dateRange.end}
            onChange={(e) => setDateRange((prev) => ({ ...prev, end: e.target.value }))}
          />
        </div>
        <button className="btn-primary text-sm">查询</button>
      </div>

      {/* 表格 */}
      <div className="card !p-0 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-gray-100 bg-gray-50">
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">日期</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">类型</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">描述</th>
                <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">金额</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">状态</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-50">
              {records.map((record) => (
                <tr key={record.id} className="hover:bg-gray-50 transition-colors">
                  <td className="px-4 py-3 text-gray-500">{record.date}</td>
                  <td className="px-4 py-3 text-gray-900">{record.type}</td>
                  <td className="px-4 py-3 text-gray-500">{record.description}</td>
                  <td className={`px-4 py-3 text-right font-medium ${record.amount >= 0 ? 'text-green-600' : 'text-red-500'}`}>
                    {record.amount >= 0 ? '+' : ''}¥{record.amount.toFixed(2)}
                  </td>
                  <td className="px-4 py-3">
                    <span className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${statusColors[record.status]}`}>
                      {statusLabels[record.status]}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      <p className="mt-4 text-center text-xs text-gray-400">
        数据为占位示例，实际数据将在对接完成后显示
      </p>
    </div>
  )
}
