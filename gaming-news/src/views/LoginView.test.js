import { mount, flushPromises } from '@vue/test-utils'
import { beforeEach, expect, it, vi } from 'vitest'
import LoginView from './LoginView.vue'
const mocks = vi.hoisted(() => ({ loginAction: vi.fn(), push: vi.fn() }))
vi.mock('../stores/user', () => ({ useUserStore: () => mocks }))
vi.mock('vue-router', () => ({ useRouter: () => mocks, useRoute: () => ({ query: {} }) }))
beforeEach(() => { mocks.loginAction.mockReset(); mocks.push.mockReset() })
it('reenables submit and displays feedback after transport failure, then permits retry', async () => {
  mocks.loginAction.mockRejectedValueOnce(new Error('network')).mockResolvedValueOnce({ ok: true })
  const wrapper = mount(LoginView, { global: { stubs: { RouterLink: true } } })
  await wrapper.get('input:not([type])').setValue('fixture')
  await wrapper.get('input[type="password"]').setValue('fixture-password')
  await wrapper.get('button').trigger('click'); await flushPromises()
  expect(wrapper.text()).toContain('登录失败，请稍后重试')
  expect(wrapper.get('button').attributes('disabled')).toBeUndefined()
  await wrapper.get('button').trigger('click'); await flushPromises()
  expect(mocks.push).toHaveBeenCalledWith('/home')
  wrapper.unmount()
})
