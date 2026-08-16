import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

/**
 * axios 统一封装：
 * 1. 请求拦截器：自动带 satoken 请求头（Sa-Token 约定的 token 名）
 * 2. 响应拦截器：code!=200 统一弹错；401 跳登录页
 */
const request = axios.create({ baseURL: '/api', timeout: 15000 })

request.interceptors.request.use(config => {
  const token = localStorage.getItem('satoken')
  if (token) config.headers['satoken'] = token
  return config
})

request.interceptors.response.use(
  resp => {
    const r = resp.data
    if (r.code === 200) return r.data
    if (r.code === 401) {
      localStorage.removeItem('satoken')
      router.push('/login')
    }
    ElMessage.error(r.msg || '请求失败')
    return Promise.reject(new Error(r.msg))
  },
  err => {
    ElMessage.error(err.response?.data?.msg || err.message || '网络异常')
    return Promise.reject(err)
  }
)

export default request
