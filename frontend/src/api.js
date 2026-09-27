import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from './router.js'

const api = axios.create({ baseURL: '/api', timeout: 15000 })

api.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

api.interceptors.response.use(
  resp => {
    const body = resp.data
    // 文件下载等非 JSON 包装直接返回
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code === 0) return body.data
      ElMessage.error(body.message || '操作失败')
      return Promise.reject(new Error(body.message))
    }
    return resp
  },
  error => {
    if (error.response?.status === 401) {
      localStorage.clear()
      ElMessage.error('登录已失效，请重新登录')
      router.push('/login')
    } else {
      ElMessage.error(error.response?.data?.message || error.message || '网络异常')
    }
    return Promise.reject(error)
  }
)

export default api
