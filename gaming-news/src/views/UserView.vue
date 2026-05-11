<template>
  <div class="user-view">
    <TopNav />
    <div class="user-content">
      <div class="user-header">
        <div class="avatar">
          <img :src="userInfo?.avatar || defaultAvatar" :alt="userInfo?.username" />
        </div>
        <div class="user-info">
          <h3 class="username">{{ userInfo?.username || '未登录' }}</h3>
          <p class="user-desc">游戏资讯爱好者</p>
        </div>
      </div>
      <div class="user-actions" v-if="!isLoggedIn">
        <n-button type="primary" @click="$router.push('/login')">登录</n-button>
        <n-button @click="$router.push('/register')">注册</n-button>
      </div>
      <div class="user-menu">
        <div class="menu-item" @click="$router.push('/user')">
          <span>我的收藏</span>
          <n-icon><ChevronForward /></n-icon>
        </div>
        <div class="menu-item" @click="handleLogout" v-if="isLoggedIn">
          <span>退出登录</span>
          <n-icon><ChevronForward /></n-icon>
        </div>
      </div>
    </div>
    <BottomNav />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { NButton, NIcon } from 'naive-ui'
import { ChevronForward } from '@vicons/ionicons5'
import TopNav from '../components/TopNav.vue'
import BottomNav from '../components/BottomNav.vue'
import { useUserStore } from '../stores/user'

const router = useRouter()
const userStore = useUserStore()

const userInfo = computed(() => userStore.userInfo)
const isLoggedIn = computed(() => userStore.isLoggedIn)
const defaultAvatar = 'https://via.placeholder.com/60'

const handleLogout = () => {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.user-view {
  min-height: 100vh;
  background-color: var(--apple-bg);
  padding-bottom: 60px;
}

.user-content {
  max-width: 800px;
  margin: 0 auto;
  padding: 16px;
  padding-top: 70px;
}

.user-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: var(--apple-radius-lg);
  border: 1px solid rgba(255, 255, 255, 0.5);
  box-shadow: var(--apple-shadow-md);
  margin-bottom: 16px;
}

.avatar img {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  box-shadow: var(--apple-shadow-sm);
}

.username {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 4px;
  color: var(--apple-text-primary);
}

.user-desc {
  font-size: 14px;
  color: var(--apple-text-secondary);
}

.user-actions {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.user-menu {
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: var(--apple-radius-lg);
  border: 1px solid rgba(255, 255, 255, 0.5);
  box-shadow: var(--apple-shadow-md);
  overflow: hidden;
}

.menu-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-bottom: 0.5px solid rgba(0, 0, 0, 0.06);
  cursor: pointer;
  color: var(--apple-text-primary);
  transition: background 0.2s ease;
}

.menu-item:hover {
  background: rgba(0, 0, 0, 0.02);
}

.menu-item:last-child {
  border-bottom: none;
}
</style>
