import { create } from 'zustand'
import type { UserInfo } from '../api/auth'

interface AuthState {
  token: string | null
  user: UserInfo | null
  isAuthenticated: boolean

  /** 设置 token 并持久化 */
  setToken: (token: string) => void
  /** 设置用户信息 */
  setUser: (user: UserInfo) => void
  /** 登录成功：保存 token + 用户 */
  login: (token: string, refreshToken: string, user: UserInfo) => void
  /** 登出：清除所有状态 */
  logout: () => void
  /** 从 localStorage 恢复登录态 */
  hydrate: () => void
}

export const useAuthStore = create<AuthState>((set) => ({
  token: null,
  user: null,
  isAuthenticated: false,

  setToken: (token: string) => {
    localStorage.setItem('access_token', token)
    set({ token, isAuthenticated: true })
  },

  setUser: (user: UserInfo) => {
    localStorage.setItem('user_info', JSON.stringify(user))
    set({ user })
  },

  login: (token: string, refreshToken: string, user: UserInfo) => {
    localStorage.setItem('access_token', token)
    localStorage.setItem('refresh_token', refreshToken)
    localStorage.setItem('user_info', JSON.stringify(user))
    set({ token, user, isAuthenticated: true })
  },

  logout: () => {
    localStorage.removeItem('access_token')
    localStorage.removeItem('refresh_token')
    localStorage.removeItem('user_info')
    set({ token: null, user: null, isAuthenticated: false })
  },

  hydrate: () => {
    const token = localStorage.getItem('access_token')
    const userStr = localStorage.getItem('user_info')
    if (token) {
      let user: UserInfo | null = null
      if (userStr) {
        try {
          user = JSON.parse(userStr)
        } catch {
          // ignore
        }
      }
      set({ token, user, isAuthenticated: true })
    }
  },
}))
