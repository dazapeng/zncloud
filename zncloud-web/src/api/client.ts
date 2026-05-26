import axios from 'axios'
import type { AxiosError, InternalAxiosRequestConfig } from 'axios'

const client = axios.create({
  baseURL: '/api/v1',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
})

/* ---- 请求拦截器：自动携带 JWT ---- */
client.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('access_token')
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error),
)

/* ---- 响应拦截器：401 自动跳登录，自动刷新 token ---- */
let isRefreshing = false
let pendingQueue: Array<{
  resolve: (token: string) => void
  reject: (err: unknown) => void
}> = []

function processQueue(error: unknown, token: string | null = null) {
  pendingQueue.forEach((p) => {
    if (error) {
      p.reject(error)
    } else if (token) {
      p.resolve(token)
    }
  })
  pendingQueue = []
}

client.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & {
      _retry?: boolean
    }

    // 不是 401 或者已经是重试过的请求，直接拒绝
    if (error.response?.status !== 401 || originalRequest._retry) {
      return Promise.reject(error)
    }

    // 已经在刷新 token，将请求排队
    if (isRefreshing) {
      return new Promise<string>((resolve, reject) => {
        pendingQueue.push({ resolve, reject })
      }).then((token) => {
        if (originalRequest.headers) {
          originalRequest.headers.Authorization = `Bearer ${token}`
        }
        return client(originalRequest)
      })
    }

    originalRequest._retry = true
    isRefreshing = true

    const refreshToken = localStorage.getItem('refresh_token')
    if (!refreshToken) {
      isRefreshing = false
      redirectToLogin()
      return Promise.reject(error)
    }

    try {
      const { data } = await axios.post('/api/v1/auth/refresh', {
        refreshToken,
      })
      const newToken: string = data.accessToken || data.token || data.access_token
      localStorage.setItem('access_token', newToken)
      if (data.refreshToken || data.refresh_token) {
        localStorage.setItem('refresh_token', data.refreshToken || data.refresh_token)
      }
      processQueue(null, newToken)
      if (originalRequest.headers) {
        originalRequest.headers.Authorization = `Bearer ${newToken}`
      }
      return client(originalRequest)
    } catch (refreshError) {
      processQueue(refreshError, null)
      redirectToLogin()
      return Promise.reject(refreshError)
    } finally {
      isRefreshing = false
    }
  },
)

function redirectToLogin() {
  localStorage.removeItem('access_token')
  localStorage.removeItem('refresh_token')
  localStorage.removeItem('user_info')
  // 只在浏览器环境下跳转
  if (typeof window !== 'undefined') {
    const currentPath = window.location.pathname
    if (!currentPath.startsWith('/user/login')) {
      window.location.href = '/user/login'
    }
  }
}

export default client
