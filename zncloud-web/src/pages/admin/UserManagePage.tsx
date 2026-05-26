import { useState, useEffect } from 'react'
import client from '../../api/client'

interface User {
  id: number
  phone: string
  nickname: string
  role: string
  cafeId?: number
  createdAt?: string
}

const roleLabels: Record<string, string> = {
  USER: '普通用户',
  CAFE_ADMIN: '网吧管理员',
  OPERATOR: '平台运营',
  SUPER_ADMIN: '超级管理员',
}

export default function UserManagePage() {
  const [users, setUsers] = useState<User[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)

  const fetchUsers = async () => {
    setLoading(true)
    setError('')
    try {
      // 使用通用的查询接口（如果有专门的用户管理 API 则替换）
      const res = await client.get('/users', { params: { page, size: 15 } })
      setUsers(res.data.content || res.data)
      setTotalPages(res.data.totalPages || 1)
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : '获取用户列表失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchUsers()
  }, [page])

  return (
    <div>
      <h1 className="mb-4 text-xl font-bold text-gray-900">用户管理</h1>

      {/* 错误提示 */}
      {error && (
        <div className="mb-4 rounded-lg bg-red-50 p-3 text-sm text-red-600">
          {error}
          <button className="ml-2 underline" onClick={fetchUsers}>
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
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">ID</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">手机号</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">昵称</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">角色</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">网吧 ID</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">注册时间</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-50">
              {loading ? (
                <tr>
                  <td colSpan={6} className="px-4 py-12 text-center text-gray-400">
                    加载中...
                  </td>
                </tr>
              ) : users.length === 0 ? (
                <tr>
                  <td colSpan={6} className="px-4 py-12 text-center text-gray-400">
                    暂无用户数据
                  </td>
                </tr>
              ) : (
                users.map((user) => (
                  <tr key={user.id} className="hover:bg-gray-50 transition-colors">
                    <td className="px-4 py-3 text-gray-500">{user.id}</td>
                    <td className="px-4 py-3 font-medium text-gray-900">{user.phone}</td>
                    <td className="px-4 py-3 text-gray-500">
                      {user.nickname || <span className="text-gray-300">未设置</span>}
                    </td>
                    <td className="px-4 py-3">
                      <span className="inline-flex items-center rounded-full bg-primary-50 px-2 py-0.5 text-xs font-medium text-primary-700">
                        {roleLabels[user.role] || user.role}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-gray-500">{user.cafeId ?? '-'}</td>
                    <td className="px-4 py-3 text-xs text-gray-400">
                      {user.createdAt
                        ? new Date(user.createdAt).toLocaleString('zh-CN')
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
            disabled={page === 0}
            onClick={() => setPage((p) => p - 1)}
          >
            上一页
          </button>
          <span className="text-sm text-gray-500">
            第 {page + 1} / {totalPages} 页
          </span>
          <button
            className="btn-secondary text-xs"
            disabled={page >= totalPages - 1}
            onClick={() => setPage((p) => p + 1)}
          >
            下一页
          </button>
        </div>
      )}
    </div>
  )
}
