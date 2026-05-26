import { useState, useEffect } from 'react'
import { useAuthStore } from '../../stores/authStore'
import { getMe, updateMe } from '../../api/auth'

export default function UserProfilePage() {
  const { user, setUser } = useAuthStore()
  const [nickname, setNickname] = useState(user?.nickname || '')
  const [loading, setLoading] = useState(false)
  const [fetchLoading, setFetchLoading] = useState(true)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  useEffect(() => {
    getMe()
      .then((data) => {
        setUser(data)
        setNickname(data.nickname || '')
      })
      .catch(() => {
        // 使用 store 中的缓存
      })
      .finally(() => setFetchLoading(false))
  }, [setUser])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    setSuccess('')
    setLoading(true)
    try {
      const updated = await updateMe({ nickname })
      setUser(updated)
      setSuccess('个人信息更新成功')
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : '更新失败')
    } finally {
      setLoading(false)
    }
  }

  if (fetchLoading) {
    return (
      <div className="flex items-center justify-center py-12">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary-200 border-t-primary-600" />
      </div>
    )
  }

  const roleLabel: Record<string, string> = {
    USER: '普通用户',
    CAFE_ADMIN: '网吧管理员',
    OPERATOR: '平台运营',
    SUPER_ADMIN: '超级管理员',
  }

  return (
    <div className="mx-auto max-w-md">
      <h1 className="mb-6 text-xl font-bold text-gray-900">个人信息</h1>

      <div className="card space-y-4">
        <div>
          <span className="text-xs text-gray-400">手机号</span>
          <p className="text-sm text-gray-900">{user?.phone}</p>
        </div>

        <div>
          <span className="text-xs text-gray-400">角色</span>
          <p className="text-sm text-gray-900">{roleLabel[user?.role || 'USER']}</p>
        </div>

        {user?.cafeId && (
          <div>
            <span className="text-xs text-gray-400">所属网吧 ID</span>
            <p className="text-sm text-gray-900">{user.cafeId}</p>
          </div>
        )}

        <hr className="border-gray-100" />

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">昵称</label>
            <input
              type="text"
              className="input-field"
              value={nickname}
              onChange={(e) => setNickname(e.target.value)}
              maxLength={20}
              placeholder="设置您的昵称"
            />
          </div>

          {error && <p className="text-sm text-red-500">{error}</p>}
          {success && <p className="text-sm text-green-500">{success}</p>}

          <button type="submit" className="btn-primary w-full" disabled={loading}>
            {loading ? '保存中...' : '保存修改'}
          </button>
        </form>
      </div>
    </div>
  )
}
