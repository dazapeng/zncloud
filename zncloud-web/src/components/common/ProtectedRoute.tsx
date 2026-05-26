import { useEffect } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { useAuthStore } from '../../stores/authStore'

interface Props {
  children: React.ReactNode
  roles?: string[]
}

export default function ProtectedRoute({ children, roles }: Props) {
  const { isAuthenticated, user, hydrate } = useAuthStore()
  const location = useLocation()

  // 页面加载时从 localStorage 恢复状态
  useEffect(() => {
    hydrate()
  }, [hydrate])

  if (!isAuthenticated) {
    // 未登录，重定向到登录页面，保留来源页
    return <Navigate to="/user/login" state={{ from: location }} replace />
  }

  // 检查角色
  if (roles && user && roles.length > 0 && !roles.includes(user.role)) {
    // 无权限，根据角色跳转
    if (user.role === 'USER') {
      return <Navigate to="/user/devices" replace />
    }
    return <Navigate to="/admin/dashboard" replace />
  }

  return <>{children}</>
}
