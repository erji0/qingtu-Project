const TOKEN_KEY = 'qingtu_driver_token'
const USER_KEY = 'qingtu_driver_user'

const BASE_URL = (import.meta.env.VITE_API_BASE || '/api').replace(/\/$/, '')

let redirectingToLogin = false

export function getToken() {
  return uni.getStorageSync(TOKEN_KEY) || ''
}

export function getDriverUser() {
  try {
    const user = uni.getStorageSync(USER_KEY)
    if (!user) return null
    return typeof user === 'string' ? JSON.parse(user) : user
  } catch (e) {
    return null
  }
}

export function saveAuth(token, user) {
  if (token) uni.setStorageSync(TOKEN_KEY, token)
  if (user) uni.setStorageSync(USER_KEY, user)
}

export function clearAuth() {
  uni.removeStorageSync(TOKEN_KEY)
  uni.removeStorageSync(USER_KEY)
}

function redirectToLogin() {
  if (redirectingToLogin) return
  redirectingToLogin = true
  uni.reLaunch({
    url: '/pages/login/login',
    complete: () => {
      setTimeout(() => {
        redirectingToLogin = false
      }, 800)
    }
  })
}

function handleUnauthorized(message) {
  clearAuth()
  uni.showToast({
    title: message || '登录状态已失效，请重新登录',
    icon: 'none'
  })
  redirectToLogin()
}

function showError(message) {
  uni.showToast({
    title: message || '请求失败',
    icon: 'none',
    duration: 2000
  })
}

/**
 * 统一请求封装
 * 后端返回结构：{ code, message, data }
 */
export function request(options = {}) {
  const {
    url,
    method = 'GET',
    data,
    header = {},
    showErrorToast = true
  } = options

  return new Promise((resolve, reject) => {
    const token = getToken()

    uni.request({
      url: `${BASE_URL}${url}`,
      method,
      data,
      timeout: 15000,
      header: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...header
      },
      success: (res) => {
        const body = res.data || {}
        const code = body.code

        if (res.statusCode === 401 || code === 401) {
          const message = body.message || '登录状态已失效，请重新登录'
          clearAuth()
          if (showErrorToast) {
            uni.showToast({ title: message, icon: 'none' })
          }
          redirectToLogin()
          reject(new Error(message))
          return
        }

        if (code === 200) {
          resolve(body.data)
          return
        }

        const message = body.message || `请求失败（${res.statusCode}）`
        if (showErrorToast) showError(message)
        reject(new Error(message))
      },
      fail: (err) => {
        if (showErrorToast) showError('网络异常，请稍后重试')
        reject(err)
      }
    })
  })
}

export default request
