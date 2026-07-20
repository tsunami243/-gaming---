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
.page { min-height:100vh; display:flex; align-items:center; justify-content:center; background:var(--gb-lightest); position:relative; }
.card { position:relative; z-index:1; width:100%; max-width:400px; background:var(--gb-light); border:3px solid var(--gb-darkest); box-shadow:6px 6px 0 var(--gb-dark); padding:32px 28px; text-align:center; }
.profile { display:flex; align-items:center; gap:14px; text-align:left; margin-bottom:24px; padding-bottom:20px; border-bottom:2px solid var(--gb-darkest); }
.avatar { width:56px; height:56px; border:3px solid var(--gb-darkest); background:var(--gb-lightest); display:flex; align-items:center; justify-content:center; flex-shrink:0; }
.avatar-icon { font-size:28px; color:var(--gb-darkest); }
.username { font-family:var(--font-pixel); font-size:11px; color:var(--gb-darkest); margin:0 0 4px; }
.desc { font-family:var(--font-body); font-size:12px; color:var(--gb-dark); margin:0; }
.actions { display:flex; gap:10px; margin-bottom:16px; }
.btn { flex:1; padding:14px 0; background:var(--gb-darkest); border:3px solid var(--gb-darkest); color:var(--gb-lightest); font-family:var(--font-pixel); font-size:9px; cursor:pointer; transition:all .08s steps(2); letter-spacing:1px; }
.btn:hover { background:var(--gb-dark); }
.btn.secondary { background:var(--gb-lightest); color:var(--gb-darkest); }
.btn.secondary:hover { background:var(--gb-light); }
.menu-item { display:flex; justify-content:space-between; width:100%; padding:14px 0; background:none; border:none; border-bottom:2px solid var(--gb-dark); font-family:var(--font-zh); font-size:14px; cursor:pointer; text-align:left; color:var(--gb-darkest); transition:all .08s steps(2); }
.menu-item:hover { background:var(--gb-lightest); padding-left:8px; }
.menu-item.danger { color:var(--gb-darkest); font-weight:bold; }
.arrow { font-size:10px; color:var(--gb-dark); }
.back { margin-top:20px; font-family:var(--font-body); font-size:12px; }
.back a { color:var(--gb-dark); text-decoration:none; }
.back a:hover { color:var(--gb-darkest); }
</style>
