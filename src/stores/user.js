import { defineStore } from 'pinia'
import { getUserInfo, login, register } from '../api/user'

export const useUserStore = defineStore('user', {
  state: () => ({
    userInfo: null,
    token: localStorage.getItem('token') || ''
  }),
  getters: {
    isLoggedIn: (state) => !!state.token
  },
  actions: {
    async fetchUserInfo() {
      try {
        const res = await getUserInfo()
        if (res.code === 0) {
          this.userInfo = res.data
        }
      } catch (error) {
        console.error('获取用户信息失败:', error)
      }
    },
    async loginAction(username, password) {
      const res = await login({ username, password })
      if (res.code === 0) {
        this.token = res.data
        localStorage.setItem('token', res.data)
        await this.fetchUserInfo()
        return true
      }
      return false
    },
    async registerAction(username, password) {
      return await register({ username, password })
    },
    logout() {
      this.token = ''
      this.userInfo = null
      localStorage.removeItem('token')
    }
  }
})
