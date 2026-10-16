<template>
  <div class="landing-view">
    <div class="landing-shell">
      <header class="landing-nav"><span class="brand">GAMEBOI</span><span class="edition">FPS 游戏资讯</span></header>
      <main class="landing-main">
        <p class="signal">FPS INTELLIGENCE / 中国</p>
        <h1>把每一次<br><em>关键情报</em>放在准星前。</h1>
        <p class="landing-copy">聚合三角洲行动、无畏契约、CS2 与 Apex Legends 的官方公告、版本更新、赛事与攻略。</p>
        <div class="landing-actions"><button class="enter-btn" @click="router.push('/home')">进入资讯中心 <span>→</span></button><p>内容优先 · 更新可追溯</p></div>
      </main>
      <footer><span>GAMEBOI / 2026</span><span>DELTA FORCE · VALORANT · CS2 · APEX</span></footer>
    </div>
  </div>
</template>
<script setup>
import { useRouter } from 'vue-router'
const router = useRouter()
</script>
<style scoped>
.landing-view{min-height:100vh;background:#f5f5f6;color:#151619;padding:20px}.landing-shell{min-height:calc(100vh - 40px);display:flex;flex-direction:column;background:#fff;border:1px solid #dedfe2;padding:0 clamp(24px,6vw,96px)}.landing-nav{height:78px;border-bottom:1px solid #dedfe2;display:flex;align-items:center;justify-content:space-between}.brand{font-weight:900;font-size:22px;letter-spacing:-.06em}.edition,.signal,footer{font-size:12px;letter-spacing:.09em;color:#6f737b}.landing-main{flex:1;display:flex;flex-direction:column;justify-content:center;max-width:860px;padding:70px 0}.signal{color:#d92d27;font-weight:800;margin:0 0 20px}.landing-main h1{font-size:clamp(3.3rem,9vw,8.5rem);line-height:.89;letter-spacing:-.085em;margin:0;font-weight:900}.landing-main em{font-style:normal;color:#d92d27}.landing-copy{max-width:540px;color:#52555b;font-size:clamp(1rem,1.8vw,1.25rem);line-height:1.7;margin:34px 0}.landing-actions{display:flex;align-items:center;gap:24px;flex-wrap:wrap}.enter-btn{border:0;background:#d92d27;color:white;padding:15px 20px;font-weight:800;font-size:15px;cursor:pointer}.enter-btn:hover{background:#aa201d}.enter-btn span{margin-left:20px;font-size:20px}.landing-actions p{color:#6f737b;font-size:13px}footer{padding:23px 0;display:flex;justify-content:space-between;gap:16px;border-top:1px solid #dedfe2}@media(max-width:600px){.landing-view{padding:0}.landing-shell{min-height:100vh;border:0;padding:0 24px}.landing-nav{height:65px}.landing-main{padding:50px 0}.landing-main h1{font-size:3.4rem}footer{font-size:10px;flex-direction:column}}
</style>
