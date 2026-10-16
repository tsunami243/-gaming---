import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ArticleList from './ArticleList.vue'
import { getPublicArticlePage } from '../api/public'

vi.mock('../api/public', () => ({ getPublicArticlePage: vi.fn() }))

describe('ArticleList v1 integration', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    getPublicArticlePage.mockReset()
  })

  it('uses one initial v1 request with game/type filters and exposes summary-only cards', async () => {
    getPublicArticlePage.mockResolvedValue({ code: 0, data: { total: 1, items: [{ id: 7, title: '无畏契约 13.05 版本更新说明', summary: '中文摘要', gameCodes: ['valorant'], contentType: 'patch_notes', ingestedAt: '2026-09-20T12:00:00Z' }] } })
    const wrapper = mount(ArticleList, { props: { gameCode: 'valorant', contentType: 'patch_notes' }, global: { plugins: [createPinia()] } })
    await flushPromises()
    expect(getPublicArticlePage).toHaveBeenCalledTimes(1)
    expect(getPublicArticlePage).toHaveBeenCalledWith(expect.objectContaining({ gameCode: 'valorant', contentType: 'patch_notes', pageNum: 1 }))
    expect(wrapper.text()).toContain('中文摘要')
    expect(wrapper.text()).not.toContain('暂无摘要')
  })

  it('shows a user-facing error state instead of stale or fake data', async () => {
    getPublicArticlePage.mockRejectedValue(new Error('服务暂不可用'))
    const wrapper = mount(ArticleList, { global: { plugins: [createPinia()] } })
    await flushPromises()
    expect(wrapper.text()).toContain('服务暂不可用')
    expect(wrapper.text()).not.toContain('NO DATA')
  })
})

function deferred() {
  let resolve, reject
  const promise = new Promise((yes, no) => { resolve = yes; reject = no })
  return { promise, resolve, reject }
}
const page = (id, total = 1) => ({ code: 0, data: { total, items: [{ id, title: `article-${id}`, summary: 'summary', gameCodes: ['cs2'] }] } })

describe('ArticleList request ordering and retry', () => {
  beforeEach(() => getPublicArticlePage.mockReset())
  const render = () => mount(ArticleList, { props: { gameCode: 'cs2' }, global: { plugins: [createPinia()] } })

  it('ignores a late result for the previous game', async () => {
    const old = deferred(), current = deferred()
    getPublicArticlePage.mockReturnValueOnce(old.promise).mockReturnValueOnce(current.promise)
    const wrapper = render()
    await wrapper.setProps({ gameCode: 'valorant' })
    current.resolve(page(2)); await flushPromises()
    old.resolve(page(1)); await flushPromises()
    expect(wrapper.text()).toContain('article-2')
    expect(wrapper.text()).not.toContain('article-1')
    expect(wrapper.emitted('update:total').at(-1)).toEqual([1])
    wrapper.unmount()
  })

  it('does not let a stale failure clear current loading or show an error', async () => {
    const old = deferred(), current = deferred()
    getPublicArticlePage.mockReturnValueOnce(old.promise).mockReturnValueOnce(current.promise)
    const wrapper = render()
    await wrapper.setProps({ contentType: 'guide' })
    old.reject(new Error('stale failure')); await flushPromises()
    expect(wrapper.find('.loading-dots').exists()).toBe(true)
    expect(wrapper.text()).not.toContain('stale failure')
    current.resolve(page(3)); await flushPromises()
    expect(wrapper.text()).toContain('article-3')
    wrapper.unmount()
  })

  it('preserves loaded articles and retries the failed page without skipping', async () => {
    getPublicArticlePage.mockResolvedValueOnce(page(1, 2))
      .mockRejectedValueOnce(new Error('page 2 unavailable')).mockResolvedValueOnce(page(2, 2))
    const wrapper = render(); await flushPromises()
    await wrapper.get('.load-more').trigger('click'); await flushPromises()
    expect(wrapper.text()).toContain('article-1')
    expect(wrapper.get('[role="alert"]').text()).toContain('page 2 unavailable')
    await wrapper.get('.load-more').trigger('click'); await flushPromises()
    expect(getPublicArticlePage.mock.calls.map(([args]) => args.pageNum)).toEqual([1, 2, 2])
    expect(wrapper.findAll('.article-card')).toHaveLength(2)
    expect(wrapper.find('.load-more').exists()).toBe(false)
    wrapper.unmount()
  })

  it('prevents duplicate load-more requests while a page is pending', async () => {
    const more = deferred()
    getPublicArticlePage.mockResolvedValueOnce(page(1, 2)).mockReturnValueOnce(more.promise)
    const wrapper = render(); await flushPromises()
    await wrapper.get('.load-more').trigger('click')
    await wrapper.get('.load-more').trigger('click')
    expect(getPublicArticlePage).toHaveBeenCalledTimes(2)
    expect(wrapper.get('.load-more').attributes('disabled')).toBeDefined()
    more.resolve(page(2, 2)); await flushPromises()
    wrapper.unmount()
  })
})
