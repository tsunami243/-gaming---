<template>
  <section class="interactions" aria-label="文章互动">
    <p v-if="!user.isLoggedIn">登录后可点赞和收藏。</p>
    <template v-else>
      <p v-if="error" role="alert">{{ error }}</p>
      <p v-if="loading" role="status">正在读取互动状态…</p>
      <button v-if="!ready && !loading" @click="refresh">重新读取状态</button>
      <button :disabled="!ready || busy || loading" :aria-pressed="liked" @click="toggle('like')">{{ liked ? '取消点赞' : '点赞' }}</button>
      <button :disabled="!ready || busy || loading" :aria-pressed="saved" @click="toggle('favorite')">{{ saved ? '取消收藏' : '收藏' }}</button>
    </template>
  </section>
</template>
<script setup>
import { onBeforeUnmount, ref, watch } from 'vue'
import { useUserStore } from '../stores/user'
import { checkArticleLiked, checkFaved, likeArticle, unlikeArticle, favArticle, unfavArticle } from '../api/article'
const props = defineProps({ articleId: { type: Number, required: true } })
const user = useUserStore()
const liked = ref(false), saved = ref(false), ready = ref(false), loading = ref(false), busy = ref(false), error = ref('')
let generation = 0
onBeforeUnmount(() => { generation += 1 })
function data(response) {
  if (response.code !== 0) throw new Error(response.message || '互动操作失败')
  return response.data
}
async function refresh() {
  const current = ++generation
  ready.value = false; liked.value = false; saved.value = false; error.value = ''; busy.value = false
  loading.value = user.isLoggedIn
  if (!user.isLoggedIn) return
  try {
    const [like, favorite] = await Promise.all([checkArticleLiked(props.articleId), checkFaved(props.articleId)])
    if (current !== generation) return
    const likeValue = data(like), savedValue = data(favorite)
    if (typeof likeValue !== 'boolean' || typeof savedValue !== 'boolean') throw new Error('互动状态不可用')
    liked.value = likeValue; saved.value = savedValue; ready.value = true
  } catch (cause) {
    if (current === generation) error.value = cause.userMessage || cause.message || '互动状态读取失败'
  } finally { if (current === generation) loading.value = false }
}
async function toggle(kind) {
  if (!ready.value || busy.value || !user.isLoggedIn) return
  const current = generation
  busy.value = true; error.value = ''
  const state = kind === 'like' ? liked : saved
  const action = kind === 'like' ? (state.value ? unlikeArticle : likeArticle) : (state.value ? unfavArticle : favArticle)
  try {
    data(await action(props.articleId))
    if (current === generation) state.value = !state.value
  } catch (cause) {
    if (current === generation) {
      // The server may have committed before a connection failure; re-read before another mutation.
      ready.value = false
      error.value = cause.userMessage || cause.message || '操作未确认，请重新读取状态'
    }
  } finally { if (current === generation) busy.value = false }
}
watch(() => [props.articleId, user.token], refresh, { immediate: true })
</script>
<style scoped>
.interactions { margin-top: 20px; padding-top: 12px; border-top: 2px solid var(--gb-dark); }
button { margin: 6px; padding: 8px 12px; cursor: pointer; color: var(--gb-darkest); background: var(--gb-lightest); border: 2px solid var(--gb-dark); }
button:disabled { opacity: .5; cursor: not-allowed; }
</style>
