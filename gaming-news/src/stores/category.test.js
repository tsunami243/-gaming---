import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, expect, it, vi } from 'vitest'
import { useCategoryStore } from './category'
import { getGames } from '../api/public'
vi.mock('../api/public', () => ({ getGames: vi.fn() }))
beforeEach(() => { setActivePinia(createPinia()); getGames.mockReset() })
const response = { code: 0, data: [{ code: 'cs2', nameZh: 'CS2' }] }
it('shares the same network operation across concurrent callers, including force refresh', async () => {
  let resolve
  getGames.mockImplementation(() => new Promise(done => { resolve = done }))
  const store = useCategoryStore()
  const calls = [store.fetchCategories(), store.fetchCategories(), store.fetchCategories(true)]
  await Promise.resolve()
  expect(getGames).toHaveBeenCalledTimes(1)
  resolve(response); await Promise.all(calls)
  expect(store.loaded).toBe(true)
  expect(store.loading).toBe(false)
  expect(store.getName('cs2')).toBe('CS2')
  await store.fetchCategories()
  expect(getGames).toHaveBeenCalledTimes(1)
})
it('allows retry after failure rather than caching a rejected operation', async () => {
  getGames.mockRejectedValueOnce(new Error('offline')).mockResolvedValueOnce(response)
  const store = useCategoryStore()
  await store.fetchCategories()
  expect(store.loaded).toBe(false)
  expect(store.error).toBe('offline')
  await store.fetchCategories()
  expect(store.error).toBe('')
  expect(store.loaded).toBe(true)
  expect(getGames).toHaveBeenCalledTimes(2)
})
it('keeps existing game labels when a forced refresh fails', async () => {
  getGames.mockResolvedValueOnce(response).mockRejectedValueOnce(new Error('unavailable'))
  const store = useCategoryStore()
  await store.fetchCategories(); await store.fetchCategories(true)
  expect(store.getName('cs2')).toBe('CS2')
  expect(store.error).toBe('unavailable')
  expect(store.loading).toBe(false)
})
