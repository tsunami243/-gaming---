import { mount, flushPromises } from '@vue/test-utils'
import { beforeEach, expect, it, vi } from 'vitest'
import ArticleComments from './ArticleComments.vue'
import request from '../utils/request'
vi.mock('../utils/request',()=>({default:vi.fn()}))
beforeEach(()=>{ request.mockReset() })
it('renders stored comments as text and never offers submission',async()=>{
  request.mockResolvedValue({code:0,data:{items:[{id:1,displayName:'user',content:'<img src=x onerror=alert(1)>'}],total:1}})
  const wrapper=mount(ArticleComments,{props:{articleId:7}});await flushPromises()
  expect(wrapper.text()).toContain('<img src=x onerror=alert(1)>')
  expect(wrapper.find('img').exists()).toBe(false)
  expect(wrapper.find('textarea').exists()).toBe(false)
  expect(wrapper.text()).toContain('评论提交暂未开放')
  expect(request).toHaveBeenCalledWith({url:'/v1/articles/7/comments',params:{pageNum:1,pageSize:20}})
  wrapper.unmount()
})
it('shows an error rather than a fake empty-success state',async()=>{
  request.mockRejectedValue(new Error('offline'))
  const wrapper=mount(ArticleComments,{props:{articleId:7}});await flushPromises()
  expect(wrapper.find('[role="alert"]').exists()).toBe(true)
  expect(wrapper.text()).not.toContain('暂无评论')
  wrapper.unmount()
})
