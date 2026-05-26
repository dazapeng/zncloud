import { Routes, Route, Navigate } from 'react-router-dom'
import { UserLayout } from './components/Layout/UserLayout'
import { AdminLayout } from './components/Layout/AdminLayout'
import ProtectedRoute from './components/common/ProtectedRoute'
import LoginPage from './pages/user/LoginPage'
import RegisterPage from './pages/user/RegisterPage'
import DeviceListPage from './pages/user/DeviceListPage'
import DeviceDetailPage from './pages/user/DeviceDetailPage'
import UserProfilePage from './pages/user/UserProfilePage'
import DashboardPage from './pages/admin/DashboardPage'
import DeviceManagePage from './pages/admin/DeviceManagePage'
import UserManagePage from './pages/admin/UserManagePage'
import CafeFinancePage from './pages/admin/CafeFinancePage'

export default function App() {
  return (
    <Routes>
      {/* 默认重定向到用户端登录 */}
      <Route path="/" element={<Navigate to="/user/login" replace />} />

      {/* ========== 用户端路由 ========== */}
      <Route path="/user" element={<UserLayout />}>
        <Route index element={<Navigate to="/user/login" replace />} />
        <Route path="login" element={<LoginPage />} />
        <Route path="register" element={<RegisterPage />} />
        <Route
          path="devices"
          element={
            <ProtectedRoute roles={['USER', 'CAFE_ADMIN', 'OPERATOR', 'SUPER_ADMIN']}>
              <DeviceListPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="devices/:id"
          element={
            <ProtectedRoute roles={['USER', 'CAFE_ADMIN', 'OPERATOR', 'SUPER_ADMIN']}>
              <DeviceDetailPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="profile"
          element={
            <ProtectedRoute roles={['USER', 'CAFE_ADMIN', 'OPERATOR', 'SUPER_ADMIN']}>
              <UserProfilePage />
            </ProtectedRoute>
          }
        />
      </Route>

      {/* ========== 管理端路由 ========== */}
      <Route path="/admin" element={<AdminLayout />}>
        <Route
          index
          element={
            <Navigate
              to="/admin/dashboard"
              replace
            />
          }
        />
        <Route
          path="dashboard"
          element={
            <ProtectedRoute roles={['CAFE_ADMIN', 'OPERATOR', 'SUPER_ADMIN']}>
              <DashboardPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="devices"
          element={
            <ProtectedRoute roles={['CAFE_ADMIN', 'OPERATOR', 'SUPER_ADMIN']}>
              <DeviceManagePage />
            </ProtectedRoute>
          }
        />
        <Route
          path="users"
          element={
            <ProtectedRoute roles={['OPERATOR', 'SUPER_ADMIN']}>
              <UserManagePage />
            </ProtectedRoute>
          }
        />
        <Route
          path="finance"
          element={
            <ProtectedRoute roles={['CAFE_ADMIN', 'OPERATOR', 'SUPER_ADMIN']}>
              <CafeFinancePage />
            </ProtectedRoute>
          }
        />
      </Route>

      {/* 404 回退 */}
      <Route path="*" element={<Navigate to="/user/login" replace />} />
    </Routes>
  )
}
