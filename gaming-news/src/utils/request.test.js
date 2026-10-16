import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import request from './request'
import { useUserStore } from '../stores/user'
import router from '../router'

vi.mock('../router', () => ({ default: { push: vi.fn().mockResolvedValue(), currentRoute: { value: { path: '/user', fullPath: '/user?tab=profile' } } } }))
function rejectHttp(config, status) { return Promise.reject({ config, response: { status } }) }
beforeEach(() => {
  localStorage.clear()
  setActivePinia(createPinia())
  router.push.mockClear()
})
function session(token = 'test-session') {
  const user = useUserStore()
  user.token = token
  user.userInfo = { username: 'fixture', role: 'ROLE_CONTENT_ADMIN' }
  localStorage.setItem('token', token)
  return user
}
describe('HTTP session expiry', () => {
  it('clears both persistent and reactive session and preserves return path', async () => {
    const user = session()
    await expect(request({ url: '/user/userInfo', adapter: config => rejectHttp(config, 401) })).rejects.toMatchObject({ userMessage: '登录已失效，请重新登录' })
    expect(user.isLoggedIn).toBe(false)
    expect(user.userInfo).toBeNull()
    expect(localStorage.getItem('token')).toBeNull()
    expect(router.push).toHaveBeenCalledWith({ name: 'Login', query: { redirect: '/user?tab=profile' } })
  })
  it('does not clear a newer session for a late unauthorized response', async () => {
    const user = session('old')
    let rejectOld
    const pending = request({ url: '/user/userInfo', adapter: config => new Promise((resolve, reject) => { rejectOld = () => reject({ config, response: { status: 401 } }) }) })
    await vi.waitFor(() => expect(rejectOld).toBeDefined())
    user.token = 'new'; localStorage.setItem('token', 'new')
    rejectOld()
    await expect(pending).rejects.toBeDefined()
    expect(user.token).toBe('new')
    expect(router.push).not.toHaveBeenCalled()
  })
  it('keeps session on forbidden actions and provides permission feedback', async () => {
    const user = session()
    await expect(request({ url: '/v1/admin/articles', adapter: config => rejectHttp(config, 403) })).rejects.toMatchObject({ userMessage: '当前账号没有执行此操作的权限' })
    expect(user.isLoggedIn).toBe(true)
    expect(router.push).not.toHaveBeenCalled()
  })
  it('does not redirect anonymous login failures', async () => {
    await expect(request({ url: '/user/login', adapter: config => rejectHttp(config, 401) })).rejects.toBeDefined()
    expect(router.push).not.toHaveBeenCalled()
  })
})
