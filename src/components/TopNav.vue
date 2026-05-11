<template>
  <div class="top-nav glass" :class="{ hidden: isHidden }">
    <div class="nav-content">
      <div class="nav-left">
        <img src="../assets/logos/main.jpg" alt="Logo" class="logo" @click="$router.push('/')" />
      </div>
      <div class="nav-right">
        <div class="nav-icon-btn" @click="handleSearch">
          <n-icon size="20" :component="Search" />
        </div>
        <div class="nav-icon-btn" @click="handleMessage">
          <n-icon size="20" :component="ChatboxEllipses" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { NIcon } from 'naive-ui'
import { Search, ChatboxEllipses } from '@vicons/ionicons5'

const isHidden = ref(false)
let lastScrollY = 0

const handleScroll = () => {
  const currentScrollY = window.scrollY
  if (currentScrollY > lastScrollY && currentScrollY > 50) {
    isHidden.value = true
  } else {
    isHidden.value = false
  }
  lastScrollY = currentScrollY
}

const handleSearch = () => {
  console.log('search')
}

const handleMessage = () => {
  console.log('message')
}

onMounted(() => {
  window.addEventListener('scroll', handleScroll)
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
})
</script>

<style scoped>
.top-nav {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  transform: translateY(0);
  transition: transform 0.35s cubic-bezier(0.4, 0, 0.2, 1);
}

.top-nav.hidden {
  transform: translateY(-100%);
}

.nav-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 52px;
  padding: 0 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.nav-left {
  display: flex;
  align-items: center;
}

.logo {
  height: 28px;
  width: auto;
  cursor: pointer;
  border-radius: 6px;
}

.nav-right {
  display: flex;
  gap: 8px;
  align-items: center;
}

.nav-icon-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  cursor: pointer;
  color: var(--apple-text-secondary);
  transition: all 0.2s ease;
}

.nav-icon-btn:hover {
  background: rgba(0, 0, 0, 0.05);
  color: var(--apple-text-primary);
}

.nav-icon-btn:active {
  transform: scale(0.95);
}
</style>
