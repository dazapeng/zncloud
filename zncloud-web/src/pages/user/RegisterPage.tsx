import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useAuthStore } from '../../stores/authStore'
import { register, sendCode } from '../../api/auth'

export default function RegisterPage() {
  const navigate = useNavigate()
  const { login: storeLogin } = useAuthStore()

  const [phone, setPhone] = useState('')
  const [code, setCode] = useState('')
  const [nickname, setNickname] = useState('')
  const [codeSending, setCodeSending] = useState(false)
  const [countdown, setCountdown] = useState(0)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const handleSendCode = async () => {
    if (!/^1[3-9]\d{9}$/.test(phone)) {
      setError('请输入正确的手机号')
      return
    }
    setError('')
    setCodeSending(true)
    try {
      await sendCode(phone)
      setCountdown(60)
      const timer = setInterval(() => {
        setCountdown((prev) => {
          if (prev <= 1) {
            clearInterval(timer)
            return 0
          }
          return prev - 1
        })
      }, 1000)
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : '发送验证码失败')
    } finally {
      setCodeSending(false)
    }
  }

  const handleRegister = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!phone || !code) {
      setError('请填写手机号和验证码')
      return
    }
    setError('')
    setLoading(true)
    try {
      const res = await register({ phone, code, nickname: nickname || undefined })
      storeLogin(res.access_token, res.refresh_token, res.user)
      if (res.user.role === 'USER') {
        navigate('/user/devices', { replace: true })
      } else {
        navigate('/admin/dashboard', { replace: true })
      }
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : '注册失败')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="flex min-h-[60vh] items-center justify-center">
      <div className="w-full max-w-sm">
        <div className="card">
          <h2 className="mb-6 text-center text-xl font-semibold text-gray-900">注册</h2>

          <form onSubmit={handleRegister} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">手机号</label>
              <input
                type="tel"
                className="input-field"
                placeholder="请输入手机号"
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
                maxLength={11}
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">昵称（选填）</label>
              <input
                type="text"
                className="input-field"
                placeholder="请输入昵称"
                value={nickname}
                onChange={(e) => setNickname(e.target.value)}
                maxLength={20}
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">验证码</label>
              <div className="flex gap-2">
                <input
                  type="text"
                  className="input-field flex-1"
                  placeholder="请输入验证码"
                  value={code}
                  onChange={(e) => setCode(e.target.value)}
                  maxLength={6}
                />
                <button
                  type="button"
                  className="btn-secondary whitespace-nowrap text-xs !px-3"
                  disabled={countdown > 0 || codeSending}
                  onClick={handleSendCode}
                >
                  {countdown > 0 ? `${countdown}s` : codeSending ? '发送中...' : '发送验证码'}
                </button>
              </div>
            </div>

            {error && (
              <p className="text-sm text-red-500">{error}</p>
            )}

            <button
              type="submit"
              className="btn-primary w-full"
              disabled={loading}
            >
              {loading ? '注册中...' : '注册'}
            </button>
          </form>

          <p className="mt-4 text-center text-sm text-gray-500">
            已有账号？{' '}
            <Link to="/user/login" className="text-primary-600 hover:underline">
              立即登录
            </Link>
          </p>
        </div>
      </div>
    </div>
  )
}
