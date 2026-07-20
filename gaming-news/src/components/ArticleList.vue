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
          <img v-if="article.coverImg" :src="article.coverImg" :alt="article.title" class="px-img" />
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
  padding: 16px;
}

/* ---- 卡带网格：≥1400px 3列 / ≥900px 2列 / 移动 1列 ---- */
.article-cards {
  display: grid;
  grid-template-columns: 1fr;
  gap: 16px;
}

@media (min-width: 900px) {
  .article-cards { grid-template-columns: repeat(2, 1fr); }
}

@media (min-width: 1400px) {
  .article-cards { grid-template-columns: repeat(3, 1fr); }
}

/* ---- 像素卡带卡片 ---- */
.article-card {
  display: flex;
  flex-direction: column;
  background: var(--gb-light);
  border: 3px solid var(--gb-darkest);
  box-shadow: 4px 4px 0 var(--gb-dark);
  cursor: pointer;
  transition: transform 0.08s steps(2), box-shadow 0.08s steps(2);
}

.article-card:hover {
  transform: translate(-2px, -2px);
  box-shadow: 6px 6px 0 var(--gb-darkest);
}

.article-card:active {
  transform: translate(2px, 2px);
  box-shadow: 2px 2px 0 var(--gb-dark);
}

/* ---- 封面图 16:9 ---- */
.card-image {
  width: 100%;
  aspect-ratio: 16 / 9;
  border-bottom: 3px solid var(--gb-darkest);
  overflow: hidden;
  background: var(--gb-lightest);
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
  background: repeating-linear-gradient(
    45deg,
    var(--gb-lightest), var(--gb-lightest) 4px,
    var(--gb-light) 4px, var(--gb-light) 8px
  );
}
.placeholder-icon { font-family: monospace; font-size: 20px; color: var(--gb-dark); }

/* ---- 卡片正文 ---- */
.card-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  padding: 10px 12px 12px;
}

.card-title {
  font-family: var(--font-body);
  font-size: 14px;
  font-weight: bold;
  color: var(--gb-darkest);
  margin: 0 0 6px;
  line-height: 1.35;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-summary {
  font-family: var(--font-body);
  font-size: 11px;
  color: var(--gb-dark);
  margin: 0 0 10px;
  flex: 1;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-family: var(--font-body);
  font-size: 9px;
}

.meta-tag {
  padding: 2px 6px;
  border: 1px solid var(--gb-darkest);
  color: var(--gb-darkest);
  background: var(--gb-lightest);
}

.meta-time { color: var(--gb-dark); }

.meta-arrow {
  margin-left: auto;
  color: var(--gb-dark);
  font-size: 9px;
}
.article-card:hover .meta-arrow { color: var(--gb-darkest); }

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
  font-family: monospace; font-size: 18px; color: var(--gb-dark);
  animation: blink 0.6s step-end infinite;
}
@keyframes blink { 0%,100%{opacity:1} 50%{opacity:0} }

.empty-icon { font-family: monospace; font-size: 28px; color: var(--gb-dark); }
.empty-text { font-family: var(--font-pixel); font-size: 10px; color: var(--gb-dark); margin: 0; }
</style>
