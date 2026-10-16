import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import ArticleInteractions from './ArticleInteractions.vue'
import { useUserStore } from '../stores/user'
import * as api from '../api/article'
vi.mock('../api/article', () => ({ checkArticleLiked: vi.fn(), checkFaved: vi.fn(), likeArticle: vi.fn(), unlikeArticle: vi.fn(), favArticle: vi.fn(), unfavArticle: vi.fn() }))
let pinia, wrapper
beforeEach(() => {
  localStorage.clear(); pinia = createPinia(); setActivePinia(pinia)
  Object.values(api).forEach(fn => fn.mockReset())
  api.checkArticleLiked.mockResolvedValue({ code: 0, data: false })
  api.checkFaved.mockResolvedValue({ code: 0, data: false })
})
afterEach(() => wrapper?.unmount())
const render = (login = true) => { if (login) useUserStore().token = 'fixture'; wrapper = mount(ArticleInteractions, { props: { articleId: 7 }, global: { plugins: [pinia] } }); return wrapper }
it('does not issue authenticated checks for anonymous readers', async () => {
  render(false); await flushPromises()
  expect(wrapper.text()).toContain('登录后可点赞和收藏')
  expect(api.checkFaved).not.toHaveBeenCalled()
  expect(api.checkArticleLiked).not.toHaveBeenCalled()
})
it('updates favorite state only after confirmed mutation and prevents double submit', async () => {
  let resolve
  api.favArticle.mockImplementation(() => new Promise(done => { resolve = done }))
  render(); await flushPromises()
  const button = wrapper.findAll('button').find(b => b.text() === '收藏')
  await button.trigger('click'); await button.trigger('click')
  expect(api.favArticle).toHaveBeenCalledTimes(1)
  expect(button.text()).toBe('收藏')
  resolve({ code: 0 }); await flushPromises()
  expect(button.text()).toBe('取消收藏')
  expect(button.attributes('aria-pressed')).toBe('true')
})
it('does not pretend a failed like succeeded, and requires a state re-read', async () => {
  api.likeArticle.mockRejectedValue(new Error('timeout'))
  render(); await flushPromises()
  await wrapper.findAll('button').find(b => b.text() === '点赞').trigger('click'); await flushPromises()
  expect(wrapper.get('[role="alert"]').text()).toContain('timeout')
  expect(wrapper.text()).not.toContain('取消点赞')
  expect(wrapper.text()).toContain('重新读取状态')
  api.checkArticleLiked.mockResolvedValue({ code: 0, data: true })
  await wrapper.findAll('button').find(b => b.text() === '重新读取状态').trigger('click'); await flushPromises()
  expect(wrapper.text()).toContain('取消点赞')
})
it('clears interaction state when the session expires', async () => {
  api.checkFaved.mockResolvedValue({ code: 0, data: true })
  render(); await flushPromises()
  expect(wrapper.text()).toContain('取消收藏')
  useUserStore().logout(); await flushPromises()
  expect(wrapper.findAll('button')).toHaveLength(0)
  expect(wrapper.text()).toContain('登录后可点赞和收藏')
})
