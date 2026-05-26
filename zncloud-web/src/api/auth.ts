import client from './client'

export interface LoginRequest {
  phone: string
  code: string
}

export interface RegisterRequest {
  phone: string
  code: string
  nickname?: string
}

export interface AuthResponse {
  access_token: string
  refresh_token: string
  user: UserInfo
}

export interface UserInfo {
  id: number
  phone: string
  nickname: string
  role: 'USER' | 'CAFE_ADMIN' | 'OPERATOR' | 'SUPER_ADMIN'
  avatar?: string
  cafeId?: number
  createdAt?: string
}

/** 发送验证码 */
export async function sendCode(phone: string): Promise<void> {
  await client.post('/auth/send-code', { phone })
}

/** 登录 */
export async function login(data: LoginRequest): Promise<AuthResponse> {
  const res = await client.post<AuthResponse>('/auth/login', data)
  return res.data
}

/** 注册 */
export async function register(data: RegisterRequest): Promise<AuthResponse> {
  const res = await client.post<AuthResponse>('/auth/register', data)
  return res.data
}

/** 刷新 token */
export async function refreshToken(refreshToken: string): Promise<{ access_token: string; refresh_token?: string }> {
  const res = await client.post('/auth/refresh', { refreshToken })
  return res.data
}

/** 获取当前用户信息 */
export async function getMe(): Promise<UserInfo> {
  const res = await client.get<UserInfo>('/users/me')
  return res.data
}

/** 更新用户信息 */
export async function updateMe(data: Partial<UserInfo>): Promise<UserInfo> {
  const res = await client.patch<UserInfo>('/users/me', data)
  return res.data
}
