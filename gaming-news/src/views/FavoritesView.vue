<template>
  <main class="favorites">
    <router-link to="/user">← 个人中心</router-link><h1>我的收藏</h1>
    <p v-if="error" role="alert">{{ error }} <button @click="load(page)">重试</button></p>
    <p v-if="loading" role="status">加载中…</p>
    <p v-else-if="!error && !items.length">暂无可查看的收藏</p>
    <ul v-if="!loading"><li v-for="item in items" :key="item.id"><router-link :to="`/article/${item.id}`">{{ item.title }}</router-link><p>{{ item.summary }}</p></li></ul>
    <nav aria-label="收藏分页">
      <button :disabled="loading || page <= 1" @click="load(page-1)">上一页</button>
      <span>第 {{ page }} 页</span>
      <button :disabled="loading || page*20 >= total" @click="load(page+1)">下一页</button>
    </nav>
  </main>
</template>
<script setup>
import { onMounted, onBeforeUnmount, ref } from 'vue'
import request from '../utils/request'
const items = ref([]), page = ref(1), total = ref(0), loading = ref(false), error = ref('')
let generation = 0
onBeforeUnmount(() => { generation += 1 })
async function load(wanted = 1) {
  if (loading.value) return
  const current = ++generation
  loading.value = true; error.value = ''
  try {
    const result = await request({ url: '/v1/me/favorites', params: { pageNum: wanted, pageSize: 20 } })
    if (current !== generation) return
    if (result.code !== 0) throw new Error(result.message || '收藏读取失败')
    items.value = result.data.items; total.value = result.data.total; page.value = wanted
  } catch (cause) {
    if (current === generation) error.value = cause.userMessage || '收藏读取失败，请重试'
  } finally { if (current === generation) loading.value = false }
}
onMounted(() => load())
</script>
<style scoped>
.favorites{max-width:920px;margin:0 auto;padding:72px 28px;color:#151619}.favorites> a{color:#d92d27;font-weight:700;text-decoration:none}.favorites h1{font-size:clamp(2.2rem,5vw,4rem);letter-spacing:-.07em;margin:20px 0 30px}.favorites ul{padding:0;margin:0;border-top:1px solid #dedfe2;list-style:none}.favorites li{padding:22px 0;border-bottom:1px solid #dedfe2}.favorites li>a{font-size:20px;font-weight:800;letter-spacing:-.03em;text-decoration:none}.favorites li>a:hover{color:#d92d27}.favorites p{color:#6f737b;line-height:1.6}.favorites nav{display:flex;align-items:center;gap:12px;margin-top:26px}.favorites button{padding:9px 13px;background:#fff;border:1px solid #dedfe2;font-weight:700;cursor:pointer}@media(max-width:700px){.favorites{padding:44px 20px}.favorites h1{font-size:2.5rem}}
</style>
