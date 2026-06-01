<template>
  <div class="article-list">
    <div class="article-cards">
      <div
        v-for="article in articleList"
        :key="article.id"
        class="article-card"
        @click="goToDetail(article.id)"
      >
        <div class="card-image">
          <img v-if="article.coverImg" :src="article.coverImg" :alt="article.title" />
          <div v-else class="card-placeholder">
            <span class="placeholder-icon">▀▄</span>
          </div>
        </div>
        <div class="card-body">
          <h4 class="card-title">{{ article.title }}</h4>
          <p class="card-summary">
            {{ article.summary || stripHtml(article.content)?.slice(0, 80) || '...' }}
          </p>
          <div class="card-meta">
            <span class="meta-tag">{{ categoryStore.getName(article.categoryId) }}</span>
            <span class="meta-time">{{ formatTime(article.updateTime) }}</span>
            <span class="meta-arrow">▶</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 加载中 -->
    <div v-if="loading" class="status-msg">
      <span class="loading-dots">▌▌▌</span>
    </div>

    <!-- 空状态 -->
    <div v-else-if="articleList.length === 0" class="status-msg">
      <span class="empty-icon">□</span>
      <p class="empty-text">NO DATA</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { getArticleList } from '../api/article'
import { useCategoryStore } from '../stores/category'
import { stripHtml } from '../utils/format'

const props = defineProps({
  categoryId: { type: Number, default: null },
})

const emit = defineEmits(['update:total'])

const router = useRouter()
const categoryStore = useCategoryStore()
const articleList = ref([])
const loading = ref(false)

const fetchArticles = async () => {
  loading.value = true
  try {
    const params = { pageNum: 1, pageSize: 20 }
    if (props.categoryId) params.categoryId = props.categoryId
    const res = await getArticleList(params)
    if (res.code === 0) {
      articleList.value = res.data?.items || []
      emit('update:total', res.data?.total || 0)
    }
  } catch (error) {
    console.error('获取文章列表失败', error)
  } finally {
    loading.value = false
  }
}

watch(() => props.categoryId, fetchArticles, { immediate: true })
onMounted(fetchArticles)

const goToDetail = (id) => router.push(`/article/${id}`)

const formatTime = (time) => {
  if (!time) return ''
  const diff = Date.now() - new Date(time).getTime()
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}MIN`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}HR`
  return `${Math.floor(diff / 86400000)}D`
}
</script>

<style scoped>
.article-list {
  padding: 0 12px 16px;
}

.article-cards {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

/* ---- 白底黑框像素卡片 ---- */
.article-card {
  display: flex;
  gap: 12px;
  padding: 10px;
  background: #fff;
  border: 2px solid #ddd;
  cursor: pointer;
  transition: all 0.08s ease;
}

.article-card:hover {
  border-color: #111;
  background: #fafafa;
}

.article-card:active {
  background: #f0f0f0;
  transform: scale(0.995);
}

/* ---- 封面图 ---- */
.card-image {
  flex-shrink: 0;
  width: 96px;
  height: 66px;
  border: 2px solid #ddd;
  overflow: hidden;
  background: #f5f5f5;
}

.card-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.card-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: repeating-linear-gradient(45deg, #f0f0f0, #f0f0f0 4px, #e8e8e8 4px, #e8e8e8 8px);
}
.placeholder-icon { font-family: monospace; font-size: 16px; color: #ccc; }

/* ---- 卡片正文 ---- */
.card-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.card-title {
  font-family: 'Courier New', monospace;
  font-size: 14px;
  font-weight: bold;
  color: #111;
  margin: 0 0 4px;
  line-height: 1.3;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-summary {
  font-family: 'Courier New', monospace;
  font-size: 11px;
  color: #999;
  margin: 0 0 6px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-family: 'Courier New', monospace;
  font-size: 9px;
}

.meta-tag {
  padding: 2px 6px;
  border: 1px solid #ddd;
  color: #888;
  background: #fafafa;
}

.meta-time { color: #bbb; }

.meta-arrow {
  margin-left: auto;
  color: #ccc;
  font-size: 9px;
}
.article-card:hover .meta-arrow { color: #111; }

/* ---- 状态 ---- */
.status-msg {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 0;
  gap: 10px;
}
.loading-dots {
  font-family: monospace; font-size: 18px; color: #ccc;
  animation: blink 0.6s step-end infinite;
}
@keyframes blink { 0%,100%{opacity:1} 50%{opacity:0} }

.empty-icon { font-family: monospace; font-size: 28px; color: #ddd; }
.empty-text { font-family: 'Press Start 2P', monospace; font-size: 10px; color: #ccc; margin: 0; }
</style>
