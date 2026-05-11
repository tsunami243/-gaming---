<template>
  <div class="article-list">
    <div class="list-header">
      <h3 class="list-title">{{ currentTitle }}</h3>
      <span class="list-count">共 {{ total }} 篇</span>
    </div>
    <div class="article-cards">
      <div
        v-for="article in articleList"
        :key="article.id"
        class="article-card apple-card"
        @click="goToDetail(article.id)"
      >
        <div class="card-image">
          <img :src="article.coverImage || defaultImage" :alt="article.title" />
        </div>
        <div class="card-content">
          <h4 class="card-title">{{ article.title }}</h4>
          <p class="card-summary">{{ article.summary || stripHtml(article.content)?.slice(0, 60) || '' }}...</p>
          <div class="card-meta">
            <span class="meta-tag" :style="{ background: getCategoryColor(article.categoryId) + '15', color: getCategoryColor(article.categoryId) }">
              {{ getCategoryName(article.categoryId) }}
            </span>
            <span class="meta-time">{{ formatTime(article.updateTime) }}</span>
          </div>
        </div>
      </div>
    </div>
    <div class="loading-more" v-if="loading">
      <n-spin size="medium" />
    </div>
    <div class="no-more" v-else-if="articleList.length === 0">
      <n-empty description="暂无文章" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { NIcon, NEmpty, NSpin } from 'naive-ui'
import { ChatboxEllipses } from '@vicons/ionicons5'
import { getArticleList } from '../api/article'
import { categories, getCategoryName, getCategoryColor } from '../utils/category'
import { DEFAULT_ARTICLE_IMAGE } from '../utils/constants'
import { stripHtml } from '../utils/format'

const props = defineProps({
  category: {
    type: String,
    default: 'all'
  }
})

const router = useRouter()
const articleList = ref([])
const total = ref(0)
const loading = ref(false)
const defaultImage = DEFAULT_ARTICLE_IMAGE

const categoryTitleMap = {
  'all': '最新资讯',
  'delta-force': '三角洲行动',
  'valorant': '无畏契约',
  'cs2': 'Counter-Strike',
  'apex': 'Apex Legends'
}

watch(() => props.category, async (newCategory) => {
  currentTitle.value = categoryTitleMap[newCategory] || '最新资讯'
  await fetchArticles()
}, { immediate: true })

const fetchArticles = async () => {
  loading.value = true
  try {
    const params = { pageNum: 1, pageSize: 20 }
    if (props.category !== 'all') {
      const cat = categories.find(c => c.key === props.category)
      if (cat) params.categoryId = cat.id
    }
    const res = await getArticleList(params)
    if (res.code === 0) {
      articleList.value = res.data?.records || []
      total.value = res.data?.total || 0
    }
  } catch (error) {
    console.error('获取文章列表失败', error)
  } finally {
    loading.value = false
  }
}

const goToDetail = (id) => {
  router.push(`/article/${id}`)
}

const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now - date
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  return `${Math.floor(diff / 86400000)}天前`
}

const currentTitle = ref('最新资讯')

onMounted(fetchArticles)
</script>

<style scoped>
.article-list {
  padding: 16px;
  max-width: 1200px;
  margin: 0 auto;
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.list-title {
  font-size: 20px;
  font-weight: 600;
  color: var(--apple-text-primary);
}

.list-count {
  font-size: 13px;
  color: var(--apple-text-tertiary);
}

.article-cards {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.article-card {
  display: flex;
  gap: 14px;
  padding: 14px;
  cursor: pointer;
}

.card-image {
  flex-shrink: 0;
  width: 100px;
  height: 72px;
  border-radius: var(--apple-radius-md);
  overflow: hidden;
}

.card-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.card-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  min-width: 0;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--apple-text-primary);
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-summary {
  font-size: 13px;
  color: var(--apple-text-tertiary);
  line-height: 1.5;
  margin-top: 4px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-meta {
  display: flex;
  gap: 10px;
  align-items: center;
  font-size: 11px;
  margin-top: 8px;
}

.meta-tag {
  padding: 3px 8px;
  border-radius: 6px;
  font-weight: 500;
}

.meta-time {
  color: var(--apple-text-tertiary);
}

.loading-more,
.no-more {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 40px 0;
}
</style>
