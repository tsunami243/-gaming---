<template>
  <header class="top-nav" :class="{ hidden: isHidden }">
    <div class="nav-inner">
      <span class="logo" @click="$router.push('/home')">
        <span class="logo-symbol">◉</span>
        <span class="logo-text">GAMEBOI</span>
      </span>
      <div class="nav-actions">
        <button class="nav-btn" @click="$router.push('/publish')">+ POST</button>
        <button class="nav-btn secondary" @click="$router.push('/user')">☻</button>
      </div>
    </div>
  </header>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
const isHidden = ref(false)
let lastY = 0
const onScroll = () => { const y = window.scrollY; isHidden.value = y > lastY && y > 50; lastY = y }
onMounted(() => window.addEventListener('scroll', onScroll))
onUnmounted(() => window.removeEventListener('scroll', onScroll))
</script>

<style scoped>
.top-nav {
  position: fixed;
  top: 0; left: 0; right: 0;
  z-index: 50;
  background: #111;
  border-bottom: 2px solid #fff;
  transform: translateY(0);
  transition: transform 0.2s;
}
.top-nav.hidden { transform: translateY(-100%); }

.nav-inner {
  display: flex; justify-content: space-between; align-items: center;
  height: 42px; padding: 0 16px; max-width: 1000px; margin: 0 auto;
}

.logo { display: flex; align-items: center; gap: 8px; cursor: pointer; }
.logo-symbol { font-size: 16px; color: #FBBF24; }
.logo-text { font-family: 'Press Start 2P', monospace; font-size: 10px; color: #fff; letter-spacing: 2px; }

.nav-actions { display: flex; gap: 6px; }
.nav-btn {
  font-family: 'Press Start 2P', monospace; font-size: 7px;
  padding: 6px 10px; background: #222; border: 2px solid #fff; color: #fff; cursor: pointer;
  transition: all 0.1s;
}
.nav-btn:hover { background: #333; }
.nav-btn:active { transform: translateY(1px); }
.nav-btn.secondary { border-color: #555; color: #aaa; padding: 6px 8px; font-size: 12px; }
</style>
