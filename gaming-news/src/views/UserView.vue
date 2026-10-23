<template>
  <div class="user-page">
    <AppHeader title="GAMEBOI / 账户" />
    <main class="user-frame">
      <div class="card">
      <!-- 头像 & 用户名 -->
      <div class="profile">
        <div class="avatar">
          <span class="avatar-icon">☻</span>
        </div>
        <div>
          <h2 class="username">{{ userInfo?.username || (isLoggedIn ? '资料待加载' : '未登录') }}</h2>
          <p class="desc">游戏资讯爱好者</p>
        </div>
      </div>

      <!-- 未登录 → 引导 -->
      <div v-if="!isLoggedIn" class="actions">
        <button class="btn" @click="$router.push('/login')">登录</button>
        <button class="btn secondary" @click="$router.push('/register')">注册</button>
      </div>

      <p v-if="userStore.profileLoading" role="status">正在加载资料…</p>
      <div v-if="userStore.profileError" role="alert">
        {{ userStore.profileError }}
        <button :disabled="userStore.profileLoading" @click="userStore.fetchUserInfo()">重试加载资料</button>
      </div>

      <!-- 已登录 → 菜单 -->
      <div v-if="isLoggedIn" class="menu">
        <button class="menu-item" @click="$router.push('/favorites')">
          <span>我的收藏</span><span class="arrow">▸</span>
        </button>
        <button v-if="isContentAdmin" class="menu-item" @click="$router.push('/admin')">
          <span>内容审核后台</span><span class="arrow">▸</span>
        </button>
        <button class="menu-item danger" @click="handleLogout">
          <span>退出登录</span><span class="arrow">▸</span>
        </button>
      </div>

      </div>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import AppHeader from '../components/AppHeader.vue'
const router = useRouter()
const userStore = useUserStore()
const userInfo = computed(() => userStore.userInfo)
const isLoggedIn = computed(() => userStore.isLoggedIn)
const isContentAdmin = computed(() => ['ROLE_CONTENT_ADMIN', 'ROLE_SUPER_ADMIN'].includes(userInfo.value?.role))
onMounted(() => { if (isLoggedIn.value) userStore.fetchUserInfo() })
const handleLogout = async () => { await userStore.logoutAction(); router.push('/login') }
</script>

<style scoped>
.user-page { min-height:100vh; display:flex; flex-direction:column; background:var(--gb-canvas); color:var(--gb-ink); }
.user-frame { flex:1; display:flex; align-items:flex-start; justify-content:center; padding:56px 24px 64px; }
.card { width:100%; max-width:760px; padding:36px; background:var(--gb-paper); border:1px solid var(--gb-line); border-radius:4px; text-align:center; }
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
</style>
