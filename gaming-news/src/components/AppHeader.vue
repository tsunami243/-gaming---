<template>
  <header class="app-top">
    <button class="back-btn" type="button" @click="goHome">← 返回资讯</button>
    <span class="top-title">{{ title }}</span>
    <span class="top-actions"><slot name="actions" /></span>
  </header>
</template>

<script setup>
import { useRouter } from 'vue-router'

defineProps({ title: { type: String, default: 'GAMEBOI' } })

const router = useRouter()

// Always target the news home explicitly: history.back() can leave the site
// (deep links, logins and redirects all land here with an unrelated history).
const goHome = () => router.push('/home')
</script>

<style scoped>
.app-top{height:72px;display:flex;align-items:center;justify-content:space-between;gap:12px;padding:0 max(20px,calc((100vw - 1080px)/2));background:#fff;border-bottom:1px solid #dedfe2}
.back-btn{min-height:44px;min-width:44px;padding:0 16px;border:1px solid #dedfe2;background:#fff;color:#151619;cursor:pointer;font-weight:700;font-size:14px}
.back-btn:hover{color:#d92d27;border-color:#d92d27}
.top-title{font-size:18px;font-weight:900;letter-spacing:-.06em;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.top-actions{min-width:44px;display:flex;justify-content:flex-end;gap:8px}
/* Keep slotted actions at a real touch target size, matching the back button. */
.top-actions :deep(button){min-height:44px}
@media(max-width:700px){.app-top{height:64px;padding:0 16px;gap:8px}.top-title{font-size:15px}.back-btn{padding:0 12px;font-size:13px}}
</style>
