import axios from 'axios'
import { BASE_URL } from './constants'
import router from '../router'

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
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      if (!isRedirecting) {
        isRedirecting = true
        router.push('/login').finally(() => {
          isRedirecting = false
        })
      }
    }
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

export default request
