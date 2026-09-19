import axios from 'axios'
import { ElMessage } from 'element-plus'

/**
 * axios 实例
 * <p>
 * 请求拦截器：统一带上 JWT
 * 响应拦截器：统一解包 Result、统一错误提示、401 自动跳登录
 */
const request = axios.create({
  baseURL: '/api',
  timeout: 15000
})

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('c6_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

/** 登录态失效的统一处理：清本地凭证 + 回登录页（hash 跳转，避免与 router 循环依赖） */
function toLogin(message) {
  localStorage.removeItem('c6_token')
  localStorage.removeItem('c6_user')
  if (!location.hash.startsWith('#/login')) {
    location.hash = '#/login'
  }
  ElMessage.error(message || '登录状态已失效，请重新登录')
}

request.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body.code === 200) {
      return body
    }
    // 兼容仍以 HTTP 200 返回、但业务码为 401 的情况
    if (body.code === 401) {
      toLogin(body.message)
      return Promise.reject(new Error(body.message || '登录状态已失效'))
    }
    ElMessage.error(body.message || '请求失败')
    return Promise.reject(new Error(body.message || '请求失败'))
  },
  (error) => {
    const status = error?.response?.status
    const body = error?.response?.data
    // 后端鉴权失败按 REST 语义返回 HTTP 401，axios 会走 reject 分支，统一在此处理
    if (status === 401 || body?.code === 401) {
      toLogin(body?.message)
      return Promise.reject(error)
    }
    const msg = status === 404 ? '接口不存在（后端未启动？）' : (error.message || '网络异常')
    ElMessage.error(msg)
    return Promise.reject(error)
  }
)

export default request
