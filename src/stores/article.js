import { defineStore } from 'pinia'
import { getArticleList } from '../api/article'

export const useArticleStore = defineStore('article', {
  state: () => ({
    articleList: [],
    currentCategory: 'all',
    pageNum: 1,
    pageSize: 10,
    total: 0
  }),
  actions: {
    async fetchArticleList(categoryId = null, state = null) {
      try {
        const params = {
          pageNum: this.pageNum,
          pageSize: this.pageSize
        }
        if (categoryId) params.categoryId = categoryId
        if (state) params.state = state

        const res = await getArticleList(params)
        if (res.code === 0) {
          this.articleList = res.data.records || []
          this.total = res.data.total || 0
        }
      } catch (error) {
        console.error('获取文章列表失败:', error)
      }
    },
    setCategory(category) {
      this.currentCategory = category
      this.pageNum = 1
      this.fetchArticleList()
    }
  }
})
