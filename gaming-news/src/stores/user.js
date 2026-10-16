import { defineStore } from 'pinia'
import { getUserInfo, login, register } from '../api/user'

export const useUserStore = defineStore('user', {
  state: () => ({
    userInfo: null,
    profileLoading: false,
    profileError: '',
    token: localStorage.getItem('token') || ''
  }),
  getters: {
    isLoggedIn: (state) => !!state.token
  },
  actions: {
    async fetchUserInfo() {
      const requestedToken = this.token
      if (!requestedToken) return
      this.profileLoading = true
      this.profileError = ''
      try {
        const res = await getUserInfo()
        if (this.token !== requestedToken) return
        if (res.code !== 0 || !res.data) throw new Error(res.message || '资料加载失败')
        this.userInfo = res.data
      } catch (error) {
        if (this.token === requestedToken) this.profileError = error.userMessage || '资料加载失败，请重试'
      } finally {
        if (this.token === requestedToken) this.profileLoading = false
      }
    },
    async loginAction(username, password) {
      const res = await login({ username, password })
      if (res.code === 0) {
        this.token = res.data
        localStorage.setItem('token', res.data)
        await this.fetchUserInfo()
        return this.token === res.data ? { ok: true } : { ok: false, msg: '登录已失效，请重新登录' }
      }
      return { ok: false, msg: res.message || '登录失败' }
    },
    async registerAction(username, password) {
      return await register({ username, password })
    },
    logout() {
      this.token = ''
      this.userInfo = null
      this.profileLoading = false
      this.profileError = ''
      localStorage.removeItem('token')
    }
  }
})
