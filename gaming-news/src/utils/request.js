import axios from 'axios'
import { BASE_URL } from './constants'
import router from '../router'
import { getActivePinia } from 'pinia'
import { useUserStore } from '../stores/user'

const request = axios.create({
  baseURL: BASE_URL,
  timeout: 10000
})

request.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = 'Bearer ' + token
  }
  return config
})

let isRedirecting = false

request.interceptors.response.use(
  response => response.data,
  error => {
    if (error.response?.status === 403) {
      error.userMessage = '当前账号没有执行此操作的权限'
    }
    if (error.response?.status === 401) {
      error.userMessage = '登录已失效，请重新登录'
      const authorization = error.config?.headers?.Authorization
      const currentToken = localStorage.getItem('token')
      // A response for an older session must not sign out a freshly logged-in user.
      if (currentToken && authorization === `Bearer ${currentToken}`) {
        const pinia = getActivePinia()
        if (pinia) useUserStore(pinia).logout()
        else localStorage.removeItem('token')
        if (!isRedirecting && router.currentRoute.value.path !== '/login') {
          isRedirecting = true
          const redirect = router.currentRoute.value.fullPath
          Promise.resolve(router.push({ name: 'Login', query: { redirect } }))
            .catch(() => {})
            .finally(() => { isRedirecting = false })
        }
      }
    }
    // Do not log Axios error objects: they can contain passwords or bearer tokens.
    return Promise.reject(error)
  }
)

export default request
