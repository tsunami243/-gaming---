<template>
  <div class="article-list">
    <div v-if="error && articles.length === 0" class="status-msg"><p class="empty-text">{{ error }}</p><button class="retry-btn" @click="reload">重新加载</button></div>
    <div v-else-if="loading && articles.length === 0" class="status-msg"><span class="loading-dots">▌▌▌</span></div>
    <div v-else-if="articles.length === 0" class="status-msg"><span class="empty-icon">□</span><p class="empty-text">暂无资讯</p></div>
    <template v-else>
      <div class="article-cards">
        <div v-for="article in articles" :key="article.id" class="article-card" @click="router.push(`/article/${article.id}`)">
          <div class="card-image"><img v-if="article.coverImg" :src="article.coverImg" :alt="article.title" class="px-img" /><div v-else class="card-placeholder"><span class="placeholder-icon">▀▄</span></div></div>
          <div class="card-body"><h4 class="card-title">{{ article.title }}</h4><p class="card-summary">{{ article.summary || '暂无摘要' }}</p><div class="card-meta"><span class="meta-tag">{{ gameLabel(article) }}</span><span class="meta-time">{{ formatTime(article.publishedAt || article.originalPublishedAt || article.ingestedAt) }}</span><span class="meta-arrow">▶</span></div></div>
        </div>
      </div>
      <p v-if="error" role="alert">{{ error }}</p>
      <button v-if="hasMore" class="load-more" :disabled="loading" @click="loadMore">{{ loading ? '加载中…' : '加载更多' }}</button>
    </template>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { getPublicArticlePage } from '../api/public'
import { useCategoryStore } from '../stores/category'

const props = defineProps({ gameCode: { type: String, default: null }, contentType: { type: String, default: null } })
const emit = defineEmits(['update:total'])
const router = useRouter(); const categoryStore = useCategoryStore()
const articles = ref([]); const pageNum = ref(1); const total = ref(0); const loading = ref(false); const error = ref('')
const hasMore = computed(() => articles.value.length < total.value)
const gameLabel = article => article.gameCodes?.map(categoryStore.getName).join(' / ') || 'FPS'
const formatTime = value => value ? new Intl.DateTimeFormat('zh-CN', { month: 'numeric', day: 'numeric' }).format(new Date(value)) : ''
// Only the latest filter generation may change the visible list or its loading state.
let generation = 0
onBeforeUnmount(() => { generation += 1 })
async function fetchPage(requestedPage, append, requestGeneration) {
  loading.value = true
  error.value = ''
  try {
    const res = await getPublicArticlePage({ pageNum: requestedPage, pageSize: 20, gameCode: props.gameCode || undefined, contentType: props.contentType || undefined })
    if (requestGeneration !== generation) return
    if (res.code !== 0) throw new Error(res.message || '获取文章失败')
    const page = res.data || { items: [], total: 0 }
    articles.value = append ? [...articles.value, ...(page.items || [])] : (page.items || [])
    // Advance only after success: retrying page 2 must not accidentally request page 3.
    pageNum.value = requestedPage
    total.value = page.total || 0
    emit('update:total', total.value)
  } catch (cause) {
    if (requestGeneration === generation) error.value = cause.userMessage || cause.message || '网络错误，请稍后重试'
  } finally {
    if (requestGeneration === generation) loading.value = false
  }
}
function reload() {
  generation += 1
  articles.value = []
  total.value = 0
  pageNum.value = 0
  emit('update:total', 0)
  return fetchPage(1, false, generation)
}
function loadMore() {
  if (loading.value || !hasMore.value) return
  return fetchPage(pageNum.value + 1, true, generation)
}
watch(() => [props.gameCode, props.contentType], reload, { immediate: true })
</script>

<style scoped>
.article-list{padding:24px 16px}.article-cards{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:22px}.article-card{display:flex;flex-direction:column;background:#fff;border:1px solid #dedfe2;box-shadow:none;cursor:pointer;transition:transform .18s ease,border-color .18s ease}.article-card:hover{transform:translateY(-3px);border-color:#d92d27}.card-image{width:100%;aspect-ratio:16/10;border-bottom:1px solid #dedfe2;overflow:hidden;background:#ececef}.card-image img{width:100%;height:100%;object-fit:cover;transition:transform .35s ease}.article-card:hover .card-image img{transform:scale(1.025)}.card-placeholder{height:100%;display:grid;place-items:center;color:#a4a6ab}.card-body{display:flex;flex-direction:column;flex:1;padding:17px 18px}.card-title{font-size:18px;line-height:1.3;letter-spacing:-.03em;margin:0 0 9px}.card-summary{font-size:14px;line-height:1.6;color:#6f737b;flex:1;margin:0 0 16px;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden}.card-meta{display:flex;align-items:center;gap:10px;font-size:12px;color:#6f737b}.meta-tag{color:#d92d27;font-weight:800}.meta-arrow{margin-left:auto;color:#d92d27}.status-msg{padding:72px 20px;text-align:center;color:#6f737b}.retry-btn,.load-more{border:1px solid #151619;background:#151619;color:#fff;padding:10px 18px;cursor:pointer;font-weight:700}.load-more{display:block;margin:30px auto 0}.empty-icon{font-size:28px;color:#d92d27}@media(min-width:1180px){.article-cards{grid-template-columns:repeat(3,minmax(0,1fr))}}@media(max-width:700px){.article-list{padding:20px 0}.article-cards{grid-template-columns:1fr;gap:14px}.card-body{padding:15px}.card-title{font-size:17px}}
</style>
