import { mount, flushPromises } from '@vue/test-utils'
import { beforeEach, expect, it, vi } from 'vitest'
import FavoritesView from './FavoritesView.vue'
import request from '../utils/request'
vi.mock('../utils/request', () => ({ default: vi.fn() }))
beforeEach(() => request.mockReset())
const render = () => mount(FavoritesView, { global: { stubs: { RouterLink: { props: ['to'], template: '<a :href="to"><slot /></a>' } } } })
it('shows stored favorites and requests the next page without a user ID parameter', async () => {
  request.mockResolvedValueOnce({code:0,data:{items:[{id:7,title:'Saved',summary:'text'}],total:21}})
    .mockResolvedValueOnce({code:0,data:{items:[{id:8,title:'Next'}],total:21}})
  const wrapper=render(); await flushPromises()
  expect(wrapper.get('li a').attributes('href')).toBe('/article/7')
  await wrapper.findAll('nav button')[1].trigger('click'); await flushPromises()
  expect(request).toHaveBeenLastCalledWith({url:'/v1/me/favorites',params:{pageNum:2,pageSize:20}})
  expect(wrapper.text()).toContain('Next')
  wrapper.unmount()
})
it('does not advance the page or discard favorites when loading fails', async () => {
  request.mockResolvedValueOnce({code:0,data:{items:[{id:7,title:'Saved'}],total:21}}).mockRejectedValueOnce(new Error('offline'))
  const wrapper=render(); await flushPromises()
  await wrapper.findAll('nav button')[1].trigger('click'); await flushPromises()
  expect(wrapper.text()).toContain('第 1 页')
  expect(wrapper.text()).toContain('Saved')
  expect(wrapper.find('[role="alert"]').exists()).toBe(true)
  wrapper.unmount()
})
