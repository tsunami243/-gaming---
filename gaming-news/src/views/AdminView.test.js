import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import { beforeEach, afterEach, expect, it, vi } from 'vitest'
import AdminView from './AdminView.vue'
import * as api from '../api/admin'
vi.mock('../api/admin',()=>({getModerationArticles:vi.fn(),getModerationAudit:vi.fn(),getAdminSources:vi.fn(),getIngestRuns:vi.fn(),approveArticle:vi.fn(),rejectArticle:vi.fn(),unpublishArticle:vi.fn(),pauseSource:vi.fn(),resumeSource:vi.fn()}))
let wrapper
const row=status=>({id:7,title:'fixture',summary:'summary',workflowStatus:status,revision:1024,originalUrl:'https://example.invalid'})
const response=status=>({code:0,data:{items:[row(status)],total:1}})
beforeEach(()=>{
 Object.values(api).forEach(fn=>fn.mockReset())
 api.getModerationArticles.mockResolvedValue(response('PENDING_REVIEW'))
 api.getModerationAudit.mockResolvedValue({code:0,data:{items:[],total:0}});api.getAdminSources.mockResolvedValue({code:0,data:[]});api.getIngestRuns.mockResolvedValue({code:0,data:[]})
})
afterEach(()=>wrapper?.unmount())
const button=text=>wrapper.findAll('button').find(b=>b.text()===text)
// AppHeader needs a router context; mount with a memory router so the header
// (and its slotted actions) render without Vue injection warnings.
const render=()=>mount(AdminView,{global:{plugins:[createRouter({history:createMemoryHistory(),routes:[{path:'/',component:{template:'<div />'}},{path:'/home',component:{template:'<div />'}}]})]}})
it('shows unpublish only for published items and submits the current revision',async()=>{
 wrapper=render();await flushPromises()
 expect(button('UNPUBLISH')).toBeUndefined()
 api.getModerationArticles.mockResolvedValue(response('PUBLISHED'))
 await wrapper.get('select').setValue('PUBLISHED');await flushPromises()
 expect(api.getModerationArticles).toHaveBeenLastCalledWith({workflowStatus:'PUBLISHED',pageNum:1,pageSize:20})
 expect(button('APPROVE')).toBeUndefined()
 api.unpublishArticle.mockResolvedValue({code:0,data:row('UNPUBLISHED')})
 await wrapper.get('textarea').setValue('原文失效')
 await button('UNPUBLISH').trigger('click');await flushPromises()
 expect(api.unpublishArticle).toHaveBeenCalledWith(7,{revision:1024,reasonDetail:'原文失效'})
})
it('prevents duplicate approve while request is pending',async()=>{
 let resolve;api.approveArticle.mockImplementation(()=>new Promise(done=>{resolve=done}))
 wrapper=render();await flushPromises()
 await button('APPROVE').trigger('click');await button('APPROVE').trigger('click')
 expect(api.approveArticle).toHaveBeenCalledTimes(1)
 resolve({code:0});await flushPromises()
})
it('shows revision conflict and requires refresh before any further mutation',async()=>{
 api.approveArticle.mockRejectedValue({response:{status:409}})
 wrapper=render();await flushPromises()
 await button('APPROVE').trigger('click');await flushPromises()
 expect(wrapper.get('[role="alert"]').text()).toContain('刷新列表')
 expect(button('APPROVE').attributes('disabled')).toBeDefined()
 expect(api.approveArticle).toHaveBeenCalledTimes(1)
 await button('REFRESH').trigger('click');await flushPromises()
 expect(button('APPROVE').attributes('disabled')).toBeUndefined()
})

it('loads and renders bounded article audit history without treating it as an article mutation',async()=>{
 api.getModerationAudit.mockResolvedValue({code:0,data:{items:[{id:9,actorUserId:31,action:'APPROVE',beforeStatus:'PENDING_REVIEW',afterStatus:'PUBLISHED',revision:4,reasonDetail:'official notes checked',createdAt:'2026-09-22T10:00:00'}],total:1}})
 wrapper=render();await flushPromises()
 await button('AUDIT').trigger('click');await flushPromises()
 expect(api.getModerationAudit).toHaveBeenCalledWith(7,{pageNum:1,pageSize:20})
 expect(wrapper.text()).toContain('管理员 #31')
 expect(wrapper.text()).toContain('official notes checked')
 expect(api.approveArticle).not.toHaveBeenCalled()
})
