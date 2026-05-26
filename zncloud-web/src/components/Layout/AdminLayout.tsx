import { useState } from 'react'
import { Outlet, Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuthStore } from '../../stores/authStore'

const adminMenuItems = [
  { path: '/admin/dashboard', label: '仪表盘', icon: '📊' },
  { path: '/admin/devices', label: '设备管理', icon: '🖥️' },
  { path: '/admin/users', label: '用户管理', icon: '👥' },
  { path: '/admin/finance', label: '财务管理', icon: '💰' },
  { path: '/admin/operations', label: '运营管理', icon: '📈' },
]

export function AdminLayout() {
  const { user, logout } = useAuthStore()
  const location = useLocation()
  const navigate = useNavigate()
  const [sidebarOpen, setSidebarOpen] = useState(false)

  const handleLogout = () => {
    logout()
    navigate('/user/login')
  }

  const isActive = (path: string) => location.pathname === path

  return (
    <div className="flex h-screen overflow-hidden bg-gray-100">
      {/* 移动端遮罩层 */}
      {sidebarOpen && (
        <div
          className="fixed inset-0 z-30 bg-black/50 lg:hidden"
          onClick={() => setSidebarOpen(false)}
        />
      )}

      {/* 侧边栏 */}
      <aside
        className={`fixed inset-y-0 left-0 z-40 flex w-60 flex-col bg-white border-r border-gray-200 transition-transform lg:static lg:translate-x-0 ${
          sidebarOpen ? 'translate-x-0' : '-translate-x-full'
        }`}
      >
        {/* Logo */}
        <div className="flex h-14 items-center justify-center border-b border-gray-200">
          <Link to="/admin/dashboard" className="text-lg font-bold text-primary-600">
            智云管理后台
          </Link>
        </div>

        {/* 导航菜单 */}
        <nav className="flex-1 overflow-y-auto p-3 space-y-1">
          {adminMenuItems.map((item) => (
            <Link
              key={item.path}
              to={item.path}
              className={`flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors ${
                isActive(item.path)
                  ? 'bg-primary-50 text-primary-700'
                  : 'text-gray-600 hover:bg-gray-100 hover:text-gray-900'
              }`}
            >
              <span className="text-lg">{item.icon}</span>
              <span>{item.label}</span>
            </Link>
          ))}
        </nav>

        {/* 底部用户信息 */}
        <div className="border-t border-gray-200 p-3">
          <div className="flex items-center gap-2 text-sm text-gray-600">
            <div className="h-8 w-8 rounded-full bg-primary-100 flex items-center justify-center text-primary-600 font-bold">
              {user?.nickname?.charAt(0) || user?.phone?.charAt(0) || 'U'}
            </div>
            <div className="flex-1 min-w-0">
              <p className="truncate font-medium">{user?.nickname || user?.phone}</p>
              <p className="truncate text-xs text-gray-400">
                {user?.role === 'CAFE_ADMIN' ? '网吧管理员' :
                 user?.role === 'OPERATOR' ? '平台运营' :
                 user?.role === 'SUPER_ADMIN' ? '超级管理员' : '普通用户'}
              </p>
            </div>
          </div>
        </div>
      </aside>

      {/* 右侧主区域 */}
      <div className="flex flex-1 flex-col overflow-hidden">
        {/* 顶栏 */}
        <header className="flex h-14 items-center justify-between border-b border-gray-200 bg-white px-4 shadow-sm">
          <button
            className="rounded-lg p-2 text-gray-500 hover:bg-gray-100 lg:hidden"
            onClick={() => setSidebarOpen(true)}
          >
            <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
            </svg>
          </button>

          <div className="flex items-center gap-3 ml-auto">
            <Link
              to="/user/devices"
              className="text-sm text-gray-500 hover:text-primary-600 transition-colors"
            >
              切换到用户端
            </Link>
            <button onClick={handleLogout} className="btn-secondary text-xs !px-3 !py-1">
              退出登录
            </button>
          </div>
        </header>

        {/* 内容区 */}
        <main className="flex-1 overflow-y-auto p-6">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
