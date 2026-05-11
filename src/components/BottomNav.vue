<template>
  <div class="bottom-nav glass">
    <div
      v-for="tab in tabs"
      :key="tab.key"
      class="nav-item"
      :class="{ active: currentRoute === tab.key, 'is-publish': tab.key === 'publish' }"
      @click="handleClick(tab)"
    >
      <div v-if="tab.key === 'publish'" class="publish-btn">
        <n-icon size="24" :component="Add" />
      </div>
      <template v-else>
        <div class="nav-icon-wrap">
          <n-icon size="22" :component="tab.icon" />
        </div>
        <span class="nav-text">{{ tab.name }}</span>
      </template>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { NIcon } from 'naive-ui'
import { Home, Flame, Chatbubbles, Add, Person } from '@vicons/ionicons5'

const router = useRouter()
const route = useRoute()

const tabs = [
  { key: 'home', name: '首页', icon: Home, path: '/' },
  { key: 'hot', name: '热点', icon: Flame, path: '/' },
  { key: 'publish', name: '发布', icon: Add, path: '/publish' },
  { key: 'message', name: '消息', icon: Chatbubbles, path: '/message' },
  { key: 'user', name: '我的', icon: Person, path: '/user' }
]

const currentRoute = computed(() => {
  const path = route.path
  if (path === '/' || path.startsWith('/article')) return 'home'
  if (path === '/user' || path.startsWith('/user')) return 'user'
  if (path === '/publish') return 'publish'
  if (path === '/message' || path.startsWith('/message')) return 'message'
  return 'home'
})

const handleClick = (tab) => {
  if (tab.key === 'hot') {
    router.push('/home')
    return
  }
  router.push(tab.path)
}
</script>

<style scoped>
.bottom-nav {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: 100;
  display: flex;
  justify-content: space-around;
  align-items: flex-end;
  height: 68px;
  padding-bottom: env(safe-area-inset-bottom);
  border-top: 0.5px solid rgba(0, 0, 0, 0.1);
}

.nav-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  color: var(--apple-text-tertiary);
  transition: all 0.2s ease;
  padding: 8px 16px;
  border-radius: 12px;
}

.nav-item:active {
  transform: scale(0.95);
}

.nav-item.active {
  color: var(--apple-blue);
}

.nav-item.active .nav-icon-wrap {
  background: rgba(0, 122, 255, 0.1);
}

.nav-item.is-publish {
  position: relative;
  top: -16px;
}

.nav-icon-wrap {
  width: 40px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  transition: background 0.2s ease;
}

.publish-btn {
  width: 52px;
  height: 52px;
  background: linear-gradient(135deg, var(--apple-blue), #5856D6);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  box-shadow: 0 8px 24px rgba(0, 122, 255, 0.35);
  margin-top: -8px;
}

.nav-text {
  font-size: 10px;
  font-weight: 500;
}
</style>
