import { ref } from 'vue'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import UserView from './UserView.vue'
import { logout } from '../api/user'

// Regression: the profile page used to have no navigation at all beyond a tiny
// inline text link, so a signed-in user could not get back to the news home page.
const push = vi.fn()
vi.mock('vue-router', () => ({ useRouter: () => ({ push }) }))
vi.mock('../api/user', () => ({
  getUserInfo: vi.fn().mockResolvedValue({ code: 0, data: { id: 2, username: 'admin', role: 'ROLE_CONTENT_ADMIN' } }),
  login: vi.fn(),
  register: vi.fn(),
  logout: vi.fn().mockResolvedValue({ code: 0 })
}))

let wrapper

beforeEach(() => {
  push.mockReset()
  logout.mockClear()
  localStorage.clear()
})

const render = () => {
  wrapper = mount(UserView, { global: { plugins: [createPinia()] } })
  return wrapper
}

describe('profile page navigation', () => {
  it('offers a home entry that routes to /home', async () => {
    render()
    const back = wrapper.get('.back-btn')
    expect(back.text()).toContain('返回资讯')

    await back.trigger('click')

    expect(push).toHaveBeenCalledWith('/home')
  })

  it('does not depend on browser history to leave the profile page', async () => {
    render()
    // A single explicit control, so the page is never a dead end.
    expect(wrapper.findAll('.back-btn')).toHaveLength(1)
    expect(wrapper.findAll('a')).toHaveLength(0)
  })
})

describe('signing out', () => {
  it('tells the server to revoke the token before leaving the page', async () => {
    localStorage.setItem('token', 'session-token')
    render()
    await flushPromises()

    await wrapper.get('.menu-item.danger').trigger('click')
    await flushPromises()

    expect(logout).toHaveBeenCalledTimes(1)
    expect(localStorage.getItem('token')).toBeNull()
    expect(push).toHaveBeenCalledWith('/login')
  })
})
