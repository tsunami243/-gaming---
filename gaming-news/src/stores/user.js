import { defineStore } from 'pinia'
import { getUserInfo, login, logout as logoutApi, register } from '../api/user'

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
    /**
     * 只清本地状态，不请求后端。
     *
     * 供 401 拦截器复用：令牌已被服务端判定失效时再发一次请求只会再吃一个 401，
     * 因此这里保持同步且无网络调用。主动退出请用 logoutAction()。
     */
    logout() {
      this.token = ''
      this.userInfo = null
      this.profileLoading = false
      this.profileError = ''
      localStorage.removeItem('token')
    },
    /**
     * 主动退出登录：先让服务端作废令牌，再清本地状态。
     *
     * 后端删除 Redis 中的 token{userId}（与改密码同一套失效机制），旧 JWT 随即无法通过双验证。
     * 服务端调用失败（网络异常、令牌本就过期）仍然完成本地退出，否则界面会卡在"看着像已登录"
     * 而所有请求都 401 的状态。期间若已登录了新会话，则保留新会话不动。
     *
     * @returns {Promise<void>}
     */
    async logoutAction() {
      const requestedToken = this.token
      try {
        if (requestedToken) await logoutApi()
      } catch {
        // 令牌可能已失效或网络不可达；本地退出的结果不受影响，不向界面抛出。
      } finally {
        if (this.token === requestedToken) this.logout()
      }
    }
  }
})
