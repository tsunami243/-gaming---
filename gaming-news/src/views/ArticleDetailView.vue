<template>
  <div class="page"><div class="pixel-bg"></div>
    <header class="detail-top"><button class="back-btn" @click="$router.back()">← 返回资讯</button><span class="top-title">GAMEBOI</span><span class="top-spacer"></span></header>
    <main v-if="loading" class="status">加载中…</main>
    <main v-else-if="error" class="status"><p>{{ error }}</p><button class="back-btn" @click="load">RETRY</button></main>
    <article v-else-if="article" class="card">
      <h1 class="title">{{ article.title }}</h1>
      <div class="meta"><span>来源：{{ article.sourceName || '未知' }}</span><span>{{ gameNames }}</span><span>·</span><span>原文时间：{{ article.originalPublishedDate ? article.originalPublishedDate + '（仅日期）' : formatDate(article.originalPublishedAt) }}</span><span>·</span><span>{{ article.contentType }}</span><span>原始语言：{{ article.sourceLanguage || '未知' }}</span><span>站内发布：{{ formatDate(article.publishedAt) }}</span></div>
      <div v-if="article.coverImg" class="cover"><img :src="article.coverImg" :alt="article.title" /></div>
      <p v-if="!article.content" class="summary">{{ article.summary }}</p>
      <div v-else class="content">{{ article.content }}</div>
      <p class="notice">内容整理自官方页面，如有出入请以原文为准。</p>
      <a v-if="originalLink" class="original-link" :href="originalLink" target="_blank" rel="noopener noreferrer">阅读原文 ↗</a>
      <p v-else role="status">暂无有效原文链接</p>
      <ArticleInteractions :key="article.id" :article-id="article.id" />
      <ArticleComments :key="`comments-${article.id}`" :article-id="article.id" />
    </article>
    <main v-else class="status">未找到文章</main>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { getPublicArticleDetail } from '../api/public'
import { useCategoryStore } from '../stores/category'
import ArticleInteractions from '../components/ArticleInteractions.vue'
import ArticleComments from '../components/ArticleComments.vue'

const route = useRoute()
const games = useCategoryStore()
const article = ref(null)
const loading = ref(false)
const error = ref('')
const gameNames = computed(() => article.value?.gameCodes?.map(code => games.getName(code)).join(' / ') || 'FPS')
const originalLink = computed(() => {
  try {
    const url = new URL(article.value?.originalUrl)
    return ['https:', 'http:'].includes(url.protocol) && !url.username && !url.password ? url.href : null
  } catch { return null }
})
const formatDate = value => {
  if (!value) return '未知'
  const date = new Date(value)
  return Number.isFinite(date.getTime())
    ? new Intl.DateTimeFormat('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' }).format(date)
    : '未知'
}
let generation = 0
onBeforeUnmount(() => { generation += 1 })
async function load() {
  const requestGeneration = ++generation
  const id = route.params.id
  loading.value = true
  error.value = ''
  article.value = null
  try {
    const res = await getPublicArticleDetail(id)
    if (requestGeneration !== generation) return
    if (res.code !== 0) throw new Error(res.message || '文章不可用')
    article.value = res.data
  } catch (cause) {
    if (requestGeneration !== generation) return
    error.value = cause.response?.status === 404 ? '文章不存在或尚未发布' : (cause.userMessage || cause.message || '加载失败，请稍后重试')
  } finally {
    if (requestGeneration === generation) loading.value = false
  }
}
// Game-name lookup must not delay or duplicate the article request.
onMounted(() => games.fetchCategories())
watch(() => route.params.id, load, { immediate: true })
</script>

<style scoped>
.page{min-height:100vh;background:#f5f5f6;color:#151619}.detail-top{height:72px;display:flex;align-items:center;justify-content:space-between;padding:0 max(20px,calc((100vw - 1080px)/2));background:#fff;border-bottom:1px solid #dedfe2}.back-btn,.original-link{padding:10px 14px;border:1px solid #dedfe2;background:#fff;color:#151619;cursor:pointer;text-decoration:none;font-weight:700}.top-title{font-size:20px;font-weight:900;letter-spacing:-.06em}.top-spacer{width:100px}.card{max-width:900px;margin:52px auto;padding:0;background:#fff;border:1px solid #dedfe2;box-shadow:none}.title{font-size:clamp(2.2rem,5vw,4.1rem);line-height:1.08;letter-spacing:-.06em;padding:44px 48px 16px;margin:0}.meta{display:flex;gap:8px;flex-wrap:wrap;color:#6f737b;font-size:13px;line-height:1.6;padding:0 48px 30px}.cover img{width:100%;display:block}.summary{font-size:18px;line-height:1.8;padding:32px 48px 0;margin:0}.content{font-size:17px;line-height:1.9;padding:32px 48px 0;margin:0;white-space:pre-wrap;overflow-wrap:anywhere;color:#2b2d33}.notice{font-size:14px;color:#6f737b;border-top:1px solid #dedfe2;margin:32px 48px 0;padding-top:20px}.original-link{display:inline-block;background:#d92d27;color:#fff;border-color:#d92d27;margin:16px 48px 30px}.status{padding:110px 20px;text-align:center;color:#6f737b}@media(max-width:700px){.detail-top{height:64px;padding:0 16px}.card{margin:0;border:0}.title{padding:34px 20px 14px}.meta{padding:0 20px 24px}.summary{padding:24px 20px 0}.content{padding:24px 20px 0;font-size:16px}.notice{margin:24px 20px 0}.original-link{margin:16px 20px 24px}}
</style>
