import { useState } from 'react'
import { useNavigate, Link, useLocation } from 'react-router-dom'
import { useAuthStore } from '../../stores/authStore'
import { login, sendCode } from '../../api/auth'

export default function LoginPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const { login: storeLogin } = useAuthStore()

  const [phone, setPhone] = useState('')
  const [code, setCode] = useState('')
  const [codeSending, setCodeSending] = useState(false)
  const [countdown, setCountdown] = useState(0)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const from = (location.state as { from?: { pathname: string } })?.from?.pathname || '/user/devices'

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

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!phone || !code) {
      setError('请填写手机号和验证码')
      return
    }
    setError('')
    setLoading(true)
    try {
      const res = await login({ phone, code })
      storeLogin(res.access_token, res.refresh_token, res.user)
      // 根据角色跳转
      if (res.user.role === 'USER') {
        navigate(from, { replace: true })
      } else {
        navigate('/admin/dashboard', { replace: true })
      }
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : '登录失败')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="flex min-h-[60vh] items-center justify-center">
      <div className="w-full max-w-sm">
        <div className="card">
          <h2 className="mb-6 text-center text-xl font-semibold text-gray-900">登录</h2>

          <form onSubmit={handleLogin} className="space-y-4">
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
              {loading ? '登录中...' : '登录'}
            </button>
          </form>

          <p className="mt-4 text-center text-sm text-gray-500">
            还没有账号？{' '}
            <Link to="/user/register" className="text-primary-600 hover:underline">
              立即注册
            </Link>
          </p>
        </div>
      </div>
    </div>
  )
}
