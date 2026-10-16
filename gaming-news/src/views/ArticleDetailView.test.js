import { reactive } from 'vue'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import ArticleDetailView from './ArticleDetailView.vue'
import { getGames, getPublicArticleDetail } from '../api/public'

let route
vi.mock('vue-router', async importOriginal => ({ ...(await importOriginal()), useRoute: () => route }))
vi.mock('../api/public', () => ({ getGames: vi.fn(), getPublicArticleDetail: vi.fn() }))
const article = (id, originalUrl = 'https://example.com/news') => ({ code: 0, data: {
  id, title: `title-${id}`, summary: '中文摘要', content: '第一段正文\n\n第二段正文',
  readingMode: 'FULL_TEXT', sourceName: '已登记官方来源', originalUrl, gameCodes: ['cs2'], sourceLanguage: 'en',
  originalPublishedAt: null, publishedAt: '2026-09-20T12:00:00Z'
} })
function deferred() {
  let resolve, reject
  const promise = new Promise((yes, no) => { resolve = yes; reject = no })
  return { promise, resolve, reject }
}
let wrapper
afterEach(() => wrapper?.unmount())
beforeEach(() => {
  route = reactive({ params: { id: '1' } })
  getGames.mockReset().mockResolvedValue({ code: 0, data: [] })
  getPublicArticleDetail.mockReset()
})
const render = () => { wrapper = mount(ArticleDetailView, { global: { plugins: [createPinia()], stubs: { ArticleComments: true } } }); return wrapper }

describe('full text detail', () => {
  it('loads once without waiting for game metadata and renders the served body as text', async () => {
    const games = deferred()
    getGames.mockReturnValue(games.promise)
    getPublicArticleDetail.mockResolvedValue(article(1))
    render(); await flushPromises()
    expect(getPublicArticleDetail).toHaveBeenCalledTimes(1)
    expect(wrapper.get('.content').text()).toBe('第一段正文\n\n第二段正文')
    expect(wrapper.text()).toContain('来源：已登记官方来源')
    expect(wrapper.text()).toContain('原文时间：未知')
    expect(wrapper.find('.content').element.innerHTML).not.toContain('<script')
    expect(wrapper.get('a.original-link').attributes('rel')).toContain('noopener')
    games.resolve({ code: 0, data: [] }); await flushPromises()
    expect(getPublicArticleDetail).toHaveBeenCalledTimes(1)
  })

  it('escapes markup coming from the API instead of rendering it', async () => {
    const response = article(1)
    response.data.content = '<img src=x onerror=alert(1)>标记文本'
    getPublicArticleDetail.mockResolvedValue(response)
    render(); await flushPromises()
    expect(wrapper.find('.content').element.innerHTML).not.toContain('<img')
    expect(wrapper.get('.content').text()).toContain('标记文本')
  })

  it('falls back to the summary when no body was served', async () => {
    const response = article(1)
    response.data.content = null
    getPublicArticleDetail.mockResolvedValue(response)
    render(); await flushPromises()
    expect(wrapper.find('.content').exists()).toBe(false)
    expect(wrapper.get('.summary').text()).toBe('中文摘要')
  })

  it.each([null, '', 'javascript:alert(1)', 'data:text/html,hi', '//example.com', 'https://user:pass@example.com'])('does not offer invalid original URL %s', async url => {
    getPublicArticleDetail.mockResolvedValue(article(1, url))
    render(); await flushPromises()
    expect(wrapper.find('.original-link').exists()).toBe(false)
    expect(wrapper.text()).toContain('暂无有效原文链接')
  })

  it('ignores previous article responses after a route change', async () => {
    const old = deferred(), current = deferred()
    getPublicArticleDetail.mockReturnValueOnce(old.promise).mockReturnValueOnce(current.promise)
    render(); route.params.id = '2'; await flushPromises()
    current.resolve(article(2)); await flushPromises()
    old.resolve(article(1)); await flushPromises()
    expect(wrapper.text()).toContain('title-2')
    expect(wrapper.text()).not.toContain('title-1')
  })

  it('shows a clear 404 and retries the same route', async () => {
    getPublicArticleDetail.mockRejectedValueOnce({ response: { status: 404 } }).mockResolvedValueOnce(article(1))
    render(); await flushPromises()
    expect(wrapper.text()).toContain('文章不存在或尚未发布')
    await wrapper.get('main button').trigger('click'); await flushPromises()
    expect(getPublicArticleDetail.mock.calls).toEqual([['1'], ['1']])
    expect(wrapper.text()).toContain('title-1')
  })
})

it('shows date-only evidence without inventing a time or timezone', async () => {
  const response = article(1)
  response.data.originalPublishedDate = '2026-09-20'
  getPublicArticleDetail.mockResolvedValue(response)
  render(); await flushPromises()
  expect(wrapper.text()).toContain('2026-09-20（仅日期）')
  expect(wrapper.text()).not.toContain('原文时间：未知')
})
