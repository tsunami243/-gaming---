<template>
  <div class="page">
    <div class="pixel-bg"></div>
    <div class="card">
      <!-- 头像 & 用户名 -->
      <div class="profile">
        <div class="avatar">
          <span class="avatar-icon">☻</span>
        </div>
        <div>
          <h2 class="username">{{ userInfo?.username || '未登录' }}</h2>
          <p class="desc">游戏资讯爱好者</p>
        </div>
      </div>

      <!-- 未登录 → 引导 -->
      <div v-if="!isLoggedIn" class="actions">
        <button class="btn" @click="$router.push('/login')">▶ LOGIN</button>
        <button class="btn secondary" @click="$router.push('/register')">▶ REGISTER</button>
      </div>

      <!-- 已登录 → 菜单 -->
      <div v-else class="menu">
        <button class="menu-item" @click="$router.push('/publish')">
          <span>发布文章</span><span class="arrow">▸</span>
        </button>
        <button class="menu-item" @click="$router.push('/user')">
          <span>我的收藏</span><span class="arrow">▸</span>
        </button>
        <button class="menu-item danger" @click="handleLogout">
          <span>退出登录</span><span class="arrow">▸</span>
        </button>
      </div>

      <p class="back"><router-link to="/home">← 返回首页</router-link></p>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
const router = useRouter()
const userStore = useUserStore()
const userInfo = computed(() => userStore.userInfo)
const isLoggedIn = computed(() => userStore.isLoggedIn)
const handleLogout = () => { userStore.logout(); router.push('/login') }
</script>

<style scoped>
.page { min-height:100vh; display:flex; align-items:center; justify-content:center; background:#fff; position:relative; }
.pixel-bg { position:fixed; inset:0; pointer-events:none; z-index:0; background-image:radial-gradient(circle,#ddd 1px,transparent 1px); background-size:8px 8px; }
.card { position:relative; z-index:1; width:100%; max-width:400px; background:#fff; border:3px solid #111; padding:32px 28px; text-align:center; }
.profile { display:flex; align-items:center; gap:14px; text-align:left; margin-bottom:24px; padding-bottom:20px; border-bottom:2px solid #eee; }
.avatar { width:56px; height:56px; border:3px solid #111; display:flex; align-items:center; justify-content:center; flex-shrink:0; }
.avatar-icon { font-size:28px; }
.username { font-family:'Press Start 2P',monospace; font-size:11px; color:#111; margin:0 0 4px; }
.desc { font-family:'Courier New',monospace; font-size:12px; color:#999; margin:0; }
.actions { display:flex; gap:10px; margin-bottom:16px; }
.btn { flex:1; padding:14px 0; background:#111; border:3px solid #111; color:#fff; font-family:'Press Start 2P',monospace; font-size:9px; cursor:pointer; transition:all .08s; letter-spacing:1px; }
.btn:hover { background:#333; }
.btn.secondary { background:#fff; color:#111; }
.menu-item { display:flex; justify-content:space-between; width:100%; padding:14px 0; background:none; border:none; border-bottom:2px solid #eee; font-family:'Courier New',monospace; font-size:14px; cursor:pointer; text-align:left; color:#111; transition:all .08s; }
.menu-item:hover { background:#f5f5f5; padding-left:8px; }
.menu-item.danger { color:#DC2626; }
.arrow { font-size:10px; color:#ccc; }
.back { margin-top:20px; font-family:'Courier New',monospace; font-size:12px; }
.back a { color:#999; text-decoration:none; }
.back a:hover { color:#111; }
</style>
