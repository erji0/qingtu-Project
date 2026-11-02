import axios from 'axios'
import { ElMessage } from 'element-plus'
import { clearAuth, getToken } from '../utils/auth'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api',
  timeout: 15000
})

request.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    const result = response.data
    if (result && typeof result === 'object' && 'code' in result) {
      if (result.code === 200) {
        return result.data
      }
      if (result.code === 401) {
        redirectToLogin()
      }
      const message = result.message || '请求失败'
      ElMessage.error(message)
      return Promise.reject(new Error(message))
    }
    return result
  },
  (error) => {
    const status = error.response?.status
    const message = error.response?.data?.message || error.message || '网络异常，请稍后重试'
    if (status === 401) {
      redirectToLogin()
    }
    ElMessage.error(message)
    return Promise.reject(error)
  }
)

function redirectToLogin() {
  clearAuth()
  if (!window.location.pathname.endsWith('/login')) {
    window.location.href = '/login'
  }
}

export default request
