import { defineStore } from 'pinia'
import { getGames } from '../api/public'

// Keep promises outside serializable Pinia state and isolate them per store instance.
const pending = new WeakMap()

const COLORS = ['#0F380F', '#306230', '#8BAC0F', '#5A8A0F']

export const useCategoryStore = defineStore('category', {
  state: () => ({ items: [], loaded: false, loading: false, error: '' }),
  actions: {
    async fetchCategories(force = false) {
      if (pending.has(this)) return pending.get(this)
      if (this.loaded && !force) return this.items
      this.loading = true
      this.error = ''
      const task = Promise.resolve().then(async () => {
        try {
          const res = await getGames()
          if (res.code !== 0 || !Array.isArray(res.data)) throw new Error(res.message || '获取游戏列表失败')
          this.items = res.data.map((game, index) => ({ ...game, dotColor: COLORS[index % COLORS.length] }))
          this.loaded = true
        } catch (error) {
          this.error = error.userMessage || error.message || '获取游戏列表失败'
        } finally {
          this.loading = false
          pending.delete(this)
        }
        return this.items
      })
      pending.set(this, task)
      return task
    },
    getName(code) { return this.items.find(item => item.code === code)?.nameZh || code || '全部游戏' },
    getColor(code) { return this.items.find(item => item.code === code)?.dotColor || '#0F380F' }
  }
})
