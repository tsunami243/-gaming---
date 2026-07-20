<template>
  <nav class="bottom-nav">
    <button
      v-for="tab in tabs"
      :key="tab.key"
      class="nav-item"
      :class="{ active: currentRoute === tab.key, 'is-primary': tab.key === 'publish' }"
      @click="handleClick(tab)"
    >
      <span class="nav-icon">{{ tab.icon }}</span>
      <span class="nav-label">{{ tab.name }}</span>
    </button>
  </nav>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()

const tabs = [
  { key: 'home', name: 'HOME', icon: '⌂', path: '/home' },
  { key: 'hot',  name: 'HOT',  icon: '★', path: '/home' },
  { key: 'publish', name: 'POST', icon: '✚', path: '/publish' },
  { key: 'message', name: 'MSG', icon: '✉', path: '/message' },
  { key: 'user', name: 'ME', icon: '☻', path: '/user' },
]

const currentRoute = computed(() => {
  const p = route.path
  if (p === '/home' || p === '/' || p.startsWith('/article')) return 'home'
  if (p === '/publish') return 'publish'
  if (p === '/message') return 'message'
  if (p.startsWith('/user')) return 'user'
  return 'home'
})

const handleClick = (tab) => {
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
  align-items: center;
  height: 60px;
  padding-bottom: env(safe-area-inset-bottom);
  background: var(--gb-lightest);
  border-top: 3px solid var(--gb-darkest);
}

.nav-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: 6px 12px;
  background: none;
  border: none;
  cursor: pointer;
  color: var(--gb-dark);
  transition: all 0.06s steps(2);
  font-family: var(--font-body);
}

.nav-item:hover {
  color: var(--gb-darkest);
}

.nav-item.active {
  color: var(--gb-darkest);
}

.nav-item.active .nav-icon {
  text-shadow: 1px 1px 0 rgba(15, 56, 15, 0.3);
}

.nav-item:active {
  transform: translateY(2px);
}

.nav-icon {
  font-size: 18px;
  line-height: 1;
}

.nav-label {
  font-size: 9px;
  font-weight: bold;
  letter-spacing: 1px;
}

/* ---- 发布按钮突出 ---- */
.nav-item.is-primary {
  position: relative;
  top: -10px;
}

.nav-item.is-primary .nav-icon {
  font-size: 24px;
  background: var(--gb-light);
  border: 2px solid var(--gb-darkest);
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 3px 3px 0 var(--gb-dark);
}
</style>
