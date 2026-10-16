import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, expect, it, vi } from 'vitest'
import { useUserStore } from './user'
import { getUserInfo } from '../api/user'
vi.mock('../api/user', () => ({ getUserInfo: vi.fn(), login: vi.fn(), register: vi.fn() }))
beforeEach(() => { localStorage.clear(); setActivePinia(createPinia()); getUserInfo.mockReset() })
it('loads profile from a persisted session and supports failure retry', async () => {
  localStorage.setItem('token', 'fixture')
  const store = useUserStore()
  getUserInfo.mockRejectedValueOnce(new Error('network')).mockResolvedValueOnce({ code: 0, data: { username: 'admin', role: 'ROLE_CONTENT_ADMIN' } })
  await store.fetchUserInfo()
  expect(store.isLoggedIn).toBe(true)
  expect(store.profileError).toBe('资料加载失败，请重试')
  expect(store.profileLoading).toBe(false)
  await store.fetchUserInfo()
  expect(store.userInfo.username).toBe('admin')
  expect(store.profileError).toBe('')
})
it('does not restore profile after logout while a request is pending', async () => {
  const store = useUserStore(); store.token = 'old'
  let resolve
  getUserInfo.mockImplementation(() => new Promise(done => { resolve = done }))
  const pending = store.fetchUserInfo(); store.logout()
  resolve({ code: 0, data: { username: 'old-admin' } }); await pending
  expect(store.userInfo).toBeNull()
  expect(store.profileLoading).toBe(false)
})
it('late old-session failures do not override the new profile', async () => {
  const store = useUserStore(); store.token = 'old'
  let reject
  getUserInfo.mockImplementationOnce(() => new Promise((yes, no) => { reject = no }))
  const old = store.fetchUserInfo()
  store.token = 'new'
  getUserInfo.mockResolvedValueOnce({ code: 0, data: { username: 'new' } })
  await store.fetchUserInfo()
  reject(new Error('old failure')); await old
  expect(store.userInfo.username).toBe('new')
  expect(store.profileError).toBe('')
})
