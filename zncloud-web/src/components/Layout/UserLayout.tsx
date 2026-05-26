import { Outlet, Link, useNavigate } from 'react-router-dom'
import { useAuthStore } from '../../stores/authStore'

export function UserLayout() {
  const { user, isAuthenticated, logout } = useAuthStore()
  const navigate = useNavigate()

  const handleLogout = () => {
    logout()
    navigate('/user/login')
  }

  return (
    <div className="min-h-screen bg-gray-50">
      {/* 顶部导航 */}
      <header className="bg-white shadow-sm border-b border-gray-200">
        <div className="mx-auto flex h-14 max-w-7xl items-center justify-between px-4">
          <div className="flex items-center gap-6">
            <Link to="/user/devices" className="text-lg font-bold text-primary-600">
              智云网吧
            </Link>
            {isAuthenticated && (
              <nav className="hidden items-center gap-4 sm:flex">
                <Link
                  to="/user/devices"
                  className="text-sm text-gray-600 hover:text-primary-600 transition-colors"
                >
                  设备列表
                </Link>
                <Link
                  to="/user/profile"
                  className="text-sm text-gray-600 hover:text-primary-600 transition-colors"
                >
                  个人中心
                </Link>
              </nav>
            )}
          </div>

          <div className="flex items-center gap-3">
            {isAuthenticated && user ? (
              <>
                <span className="text-sm text-gray-500">
                  {user.nickname || user.phone}
                  <span className="ml-1 text-xs text-gray-400">
                    ({user.role === 'CAFE_ADMIN' ? '网吧管理员' :
                      user.role === 'OPERATOR' ? '平台运营' :
                      user.role === 'SUPER_ADMIN' ? '超级管理员' : '普通用户'})
                  </span>
                </span>
                <button onClick={handleLogout} className="btn-secondary text-xs !px-3 !py-1">
                  退出
                </button>
              </>
            ) : (
              <>
                <Link to="/user/login" className="btn-primary text-xs !px-3 !py-1">
                  登录
                </Link>
                <Link to="/user/register" className="btn-secondary text-xs !px-3 !py-1">
                  注册
                </Link>
              </>
            )}
          </div>
        </div>
      </header>

      {/* 内容区 */}
      <main className="mx-auto max-w-7xl px-4 py-6">
        <Outlet />
      </main>
    </div>
  )
}
