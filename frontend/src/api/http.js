import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

const http = axios.create({
  baseURL: '/api',
  timeout: 20000
})

http.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) config.headers['X-Token'] = token
  return config
})

http.interceptors.response.use(
  resp => {
    // 文件流直接返回
    if (resp.config.responseType === 'blob') return resp
    if (resp.data && resp.data.code === 0) return resp.data.data
    ElMessage.error(resp.data?.message || '请求失败')
    return Promise.reject(new Error(resp.data?.message || '请求失败'))
  },
  error => {
    const status = error.response?.status
    const msg = error.response?.data?.message || error.message || '网络异常'
    if (status === 400 && (msg.includes('未登录') || msg.includes('登录凭证'))) {
      localStorage.clear()
      router.push('/login')
    }
    ElMessage.error(msg)
    return Promise.reject(error)
  }
)

export default http
