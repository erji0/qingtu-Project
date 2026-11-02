const BASE_URL = import.meta.env.VITE_API_BASE || '/api'

export const TOKEN_KEY = 'qingtu_student_token'
export const USER_KEY = 'qingtu_student_user'

let redirecting = false

export function getToken() {
  return uni.getStorageSync(TOKEN_KEY) || ''
}

export function getUser() {
  const user = uni.getStorageSync(USER_KEY)
  if (!user) return null
  if (typeof user === 'object') return user
  try {
    return JSON.parse(user)
  } catch (e) {
    return null
  }
}

export function setAuth(loginResult) {
  if (!loginResult) return
  uni.setStorageSync(TOKEN_KEY, loginResult.token)
  uni.setStorageSync(USER_KEY, {
    userId: loginResult.userId,
    username: loginResult.username,
    name: loginResult.name,
    role: loginResult.role
  })
}

export function clearAuth() {
  uni.removeStorageSync(TOKEN_KEY)
  uni.removeStorageSync(USER_KEY)
}

function redirectToLogin() {
  if (redirecting) return
  redirecting = true
  clearAuth()
  uni.showToast({ title: '登录状态已失效，请重新登录', icon: 'none' })
  setTimeout(() => {
    uni.reLaunch({
      url: '/pages/login/login',
      complete: () => {
        redirecting = false
      }
    })
  }, 300)
}

function cleanParams(data) {
  if (!data || typeof data !== 'object' || Array.isArray(data)) return data
  const result = {}
  Object.keys(data).forEach((key) => {
    const value = data[key]
    if (value !== undefined && value !== null && value !== '') {
      result[key] = value
    }
  })
  return result
}

export function request(options = {}) {
  const {
    url,
    method = 'GET',
    data,
    header = {},
    silent = false
  } = options
  const token = getToken()

  return new Promise((resolve, reject) => {
    uni.request({
      url: BASE_URL + url,
      method,
      data: method === 'GET' ? cleanParams(data) : data,
      timeout: 15000,
      header: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...header
      },
      success: (res) => {
        const body = res.data
        if (res.statusCode === 401 || (body && body.code === 401)) {
          redirectToLogin()
          reject(new Error('登录状态已失效'))
          return
        }
        if (body && body.code === 200) {
          resolve(body.data)
          return
        }
        const message = (body && body.message) || '请求失败，请稍后重试'
        if (!silent) {
          uni.showToast({ title: message, icon: 'none' })
        }
        reject(new Error(message))
      },
      fail: (err) => {
        if (!silent) {
          uni.showToast({ title: '网络异常，请检查网络连接', icon: 'none' })
        }
        reject(err)
      }
    })
  })
}

export default request
