import { defineStore } from 'pinia'
import request from '../utils/request'

const COLOR_MAP = {
  '三角洲行动':    { bg: '#4ADE80', text: '#052E16', glow: '#86EFAC' },
  '无畏契约':      { bg: '#DC2626', text: '#FEE2E2', glow: '#FCA5A5' },
  'Counter-Strike': { bg: '#F97316', text: '#2D1706', glow: '#FDBA74' },
  'Apex Legends':   { bg: '#EF4444', text: '#FEF2F2', glow: '#FCA5A5' },
  '三角洲':         { bg: '#4ADE80', text: '#052E16', glow: '#86EFAC' },
}

const DEFAULT_COLOR = { bg: '#3B82F6', text: '#DBEAFE', glow: '#93C5FD' }

export const useCategoryStore = defineStore('category', {
  state: () => ({
    items: [],
    loaded: false
  }),
  actions: {
    async fetchCategories() {
      if (this.loaded) return this.items
      try {
        const res = await request({ url: '/category', method: 'get' })
        if (res.code === 0) {
          this.items = res.data || []
          this.loaded = true
        }
      } catch (e) {
        console.error('Failed to fetch categories', e)
      }
      return this.items
    },
    getName(id) {
      const cat = this.items.find(c => c.id === id)
      return cat ? cat.categoryName : '???'
    },
    getColor(id) {
      const cat = this.items.find(c => c.id === id)
      const name = cat ? cat.categoryName : ''
      return COLOR_MAP[name] || DEFAULT_COLOR
    }
  }
})
