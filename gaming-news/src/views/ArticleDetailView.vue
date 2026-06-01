<template>
  <div class="page">
    <div class="pixel-bg"></div>

    <!-- 顶栏 -->
    <header class="detail-top">
      <button class="back-btn" @click="$router.back()">← BACK</button>
      <span class="top-title">GAMEBOI</span>
      <span class="top-spacer"></span>
    </header>

    <div class="card" v-if="article">
      <!-- 标题 & 元信息 -->
      <h1 class="title">{{ article.title }}</h1>
      <div class="meta">
        <span>{{ categoryStore.getName(article.categoryId) }}</span>
        <span>·</span>
        <span>{{ fmtTime(article.updateTime) }}</span>
        <span>·</span>
        <span>👁 {{ stats.views }}</span>
      </div>

      <!-- 封面 -->
      <div v-if="article.coverImg" class="cover">
        <img :src="article.coverImg" :alt="article.title" />
      </div>

      <!-- 正文 -->
      <div class="body" v-html="article.content"></div>

      <!-- 操作栏 -->
      <div class="actions">
        <button class="act-btn" :class="{ on: liked }" @click="toggleLike" :disabled="!isLogin">
          {{ liked ? '♥' : '♡' }} {{ likeCount }}
        </button>
        <button class="act-btn" :class="{ on: faved }" @click="toggleFav" :disabled="!isLogin">
          {{ faved ? '★' : '☆' }} 收藏
        </button>
        <span v-if="!isLogin" class="login-hint">登录后可互动</span>
      </div>

      <!-- 评论 -->
      <div class="comments">
        <h3 class="section-title">▸ COMMENTS</h3>

        <!-- 评论列表 -->
        <div v-if="comments.length" class="comment-list">
          <div v-for="c in comments" :key="c.id" class="comment-item">
            <div class="comment-head">
              <span class="comment-user">☻ {{ c.userName || '匿名' }}</span>
              <span class="comment-time">{{ fmtTime(c.createTime) }}</span>
            </div>
            <p class="comment-text">{{ c.content }}</p>
          </div>
        </div>
        <p v-else class="no-comment">暂无评论</p>

        <!-- 发评论 -->
        <div class="comment-form" v-if="isLogin">
          <input v-model="newComment" class="cmt-input" placeholder="写下你的评论..." @keyup.enter="submitComment" />
          <button class="cmt-btn" @click="submitComment" :disabled="!newComment.trim()">发送</button>
        </div>
      </div>
    </div>

    <div v-else class="loading">LOADING...</div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { getArticleDetail, getArticleStats, getCommentList, addComment, likeArticle, unlikeArticle, checkArticleLiked, getArticleLikeCount, favArticle, unfavArticle, checkFaved, recordView } from '../api/article'
import { useCategoryStore } from '../stores/category'
import { useUserStore } from '../stores/user'

const route = useRoute()
const categoryStore = useCategoryStore()
const userStore = useUserStore()
const article = ref(null)
const comments = ref([])
const newComment = ref('')
const liked = ref(false)
const faved = ref(false)
const likeCount = ref(0)
const stats = reactive({ views: 0, comments: 0, likes: 0 })
const isLogin = computed(() => userStore.isLoggedIn)

const fmtTime = (t) => {
  if (!t) return ''
  const d = Date.now() - new Date(t).getTime()
  if (d < 60000) return '刚刚'
  if (d < 3600000) return Math.floor(d/60000)+'分钟前'
  if (d < 86400000) return Math.floor(d/3600000)+'小时前'
  return Math.floor(d/86400000)+'天前'
}

onMounted(async () => {
  await categoryStore.fetchCategories()
  const id = route.params.id
  const [r1, r2] = await Promise.all([getArticleDetail(id), getArticleStats(id)])
  if (r1.code === 0) article.value = r1.data
  if (r2.code === 0) Object.assign(stats, r2.data)

  // 评论
  const cr = await getCommentList(id)
  if (cr.code === 0) comments.value = cr.data || []

  // 记录浏览
  recordView(id)

  // 点赞/收藏状态
  if (isLogin.value) {
    checkArticleLiked(id).then(r => { if (r.code===0) liked.value = r.data })
    checkFaved(id).then(r => { if (r.code===0) faved.value = r.data })
  }
  getArticleLikeCount(id).then(r => { if (r.code===0) likeCount.value = r.data })
})

const toggleLike = async () => {
  const id = route.params.id
  if (liked.value) { await unlikeArticle(id); liked.value = false; likeCount.value-- }
  else { await likeArticle(id); liked.value = true; likeCount.value++ }
}

const toggleFav = async () => {
  const id = route.params.id
  if (faved.value) { await unfavArticle(id); faved.value = false }
  else { await favArticle(id); faved.value = true }
}

const submitComment = async () => {
  const text = newComment.value.trim()
  if (!text) return
  const res = await addComment({ articleId: Number(route.params.id), content: text, parentId: 0 })
  if (res.code === 0) {
    comments.value.unshift({ id: Date.now(), content: text, userName: userStore.userInfo?.username || '我', createTime: new Date().toISOString() })
    newComment.value = ''
  }
}
</script>

<style scoped>
.page { min-height:100vh; background:#fff; position:relative; padding-bottom:40px; }
.pixel-bg { position:fixed; inset:0; pointer-events:none; z-index:0; background-image:radial-gradient(circle,#ddd 1px,transparent 1px); background-size:8px 8px; }

/* ---- 顶栏 ---- */
.detail-top { position:sticky; top:0; z-index:20; display:flex; align-items:center; justify-content:space-between; background:#fff; border-bottom:3px solid #111; padding:0 16px; height:46px; }
.back-btn { font-family:'Press Start 2P',monospace; font-size:8px; background:none; border:2px solid #111; padding:6px 10px; cursor:pointer; color:#111; }
.back-btn:hover { background:#f0f0f0; }
.top-title { font-family:'Press Start 2P',monospace; font-size:10px; color:#111; letter-spacing:2px; }
.top-spacer { width:60px; }

/* ---- 卡片 ---- */
.card { position:relative; z-index:1; max-width:760px; margin:20px auto; background:#fff; border:3px solid #111; padding:28px 24px; }
.title { font-family:'ZCOOL KuaiLe','Press Start 2P',monospace; font-size:22px; color:#111; margin:0 0 10px; line-height:1.4; }
.meta { display:flex; gap:8px; font-family:'Courier New',monospace; font-size:12px; color:#888; margin-bottom:18px; flex-wrap:wrap; }

.cover { margin-bottom:18px; border:2px solid #ddd; }
.cover img { width:100%; display:block; }

.body { font-family:'Courier New',monospace; font-size:15px; line-height:1.8; color:#333; margin-bottom:24px; overflow-wrap:break-word; }
.body :deep(img) { max-width:100%; height:auto; border:2px solid #ddd; margin:8px 0; }

/* ---- 操作栏 ---- */
.actions { display:flex; align-items:center; gap:12px; padding:14px 0; border-top:2px solid #eee; border-bottom:2px solid #eee; margin-bottom:20px; }
.act-btn { font-family:'Press Start 2P',monospace; font-size:9px; padding:8px 14px; background:#fff; border:2px solid #ddd; cursor:pointer; color:#888; transition:all .08s; }
.act-btn:hover:not(:disabled) { border-color:#111; color:#111; }
.act-btn.on { border-color:#DC2626; color:#DC2626; background:#FFF5F5; }
.act-btn:disabled { opacity:0.4; cursor:default; }
.login-hint { font-family:'Courier New',monospace; font-size:11px; color:#bbb; }

/* ---- 评论 ---- */
.comments { }
.section-title { font-family:'Press Start 2P',monospace; font-size:10px; color:#111; margin:0 0 14px; letter-spacing:2px; }

.comment-list { display:flex; flex-direction:column; gap:0; }
.comment-item { padding:12px 0; border-bottom:1px solid #eee; }
.comment-head { display:flex; justify-content:space-between; margin-bottom:4px; }
.comment-user { font-family:'Courier New',monospace; font-size:13px; font-weight:bold; color:#111; }
.comment-time { font-family:'Courier New',monospace; font-size:10px; color:#bbb; }
.comment-text { font-family:'Courier New',monospace; font-size:13px; color:#555; margin:0; line-height:1.5; }
.no-comment { font-family:'Courier New',monospace; font-size:12px; color:#ccc; text-align:center; padding:20px 0; }

.comment-form { display:flex; gap:8px; margin-top:14px; }
.cmt-input { flex:1; padding:10px 12px; border:2px solid #ddd; font-family:'Courier New',monospace; font-size:13px; outline:none; }
.cmt-input:focus { border-color:#111; }
.cmt-btn { padding:10px 16px; background:#111; border:2px solid #111; color:#fff; font-family:'Press Start 2P',monospace; font-size:8px; cursor:pointer; }
.cmt-btn:disabled { opacity:0.4; }
.cmt-btn:hover:not(:disabled) { background:#333; }

.loading { font-family:'Press Start 2P',monospace; font-size:12px; text-align:center; padding:80px 0; color:#ccc; }
</style>
