import axios from 'axios'
import { router } from '../router/index'
import { useAuthStore } from '../stores/authStore'

const api = axios.create({
  baseURL: '/api/v1'
})

api.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  error => Promise.reject(error)
)

api.interceptors.response.use(
  response => response,
  error => {
    if (error.response?.status === 401) {
      const requestUrl: string = error.config?.url ?? ''
      // localStorage.getItem('token') being non-null means we haven't redirected yet;
      // the synchronous removeItem inside clearAuth() acts as a once-per-session gate for concurrent 401s
      if (!requestUrl.endsWith('/auth/login') && localStorage.getItem('token') !== null) {
        useAuthStore().clearAuth()
        router.push('/login')
      }
    }
    return Promise.reject(error)
  }
)

export default api

