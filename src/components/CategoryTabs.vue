<template>
  <div class="category-tabs" ref="tabsRef">
    <div class="tabs-wrapper">
      <div
        v-for="tab in tabs"
        :key="tab.key"
        class="tab-item"
        :class="{ active: currentTab === tab.key }"
        :style="{ '--category-color': tab.color }"
        @mouseenter="handleHover(tab, $event)"
        @mouseleave="handleLeave"
        @click="handleClick(tab.key)"
      >
        <span class="tab-text">{{ tab.name }}</span>
        <div
          v-if="hoveredTab?.key === tab.key && tab.key !== 'all'"
          class="logo-bubble"
          :style="{ backgroundColor: tab.color }"
        >
          <div class="bubble-body">
            <img :src="getLogo(tab.logo)" :alt="tab.name" class="bubble-logo" />
          </div>
          <div class="bubble-arrow" :style="{ borderTopColor: tab.color }"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onUnmounted } from 'vue'
import { categories } from '../utils/category'
import deltaForceLogo from '../assets/logos/三角洲行动.png'
import valorantLogo from '../assets/logos/无畏契约.jpg'
import cs2Logo from '../assets/logos/cs2.png'
import apexLogo from '../assets/logos/apex.jpg'

const emit = defineEmits(['category-change'])

const tabs = [
  { key: 'all', name: '全部', color: '#1A1A1A', logo: null },
  ...categories
]

const currentTab = ref('all')
const hoveredTab = ref(null)

const logoMap = {
  'delta-force': deltaForceLogo,
  'valorant': valorantLogo,
  'cs2': cs2Logo,
  'apex': apexLogo
}

const getLogo = (logo) => {
  if (!logo) return ''
  return logoMap[logo] || ''
}

let hoverTimer = null

const handleHover = (tab) => {
  if (tab.key === 'all') return
  clearTimeout(hoverTimer)
  hoverTimer = setTimeout(() => {
    hoveredTab.value = tab
  }, 150)
}

const handleLeave = () => {
  clearTimeout(hoverTimer)
  hoverTimer = setTimeout(() => {
    hoveredTab.value = null
  }, 80)
}

const handleClick = (key) => {
  currentTab.value = key
  emit('category-change', key)
}

onUnmounted(() => {
  clearTimeout(hoverTimer)
})
</script>

<style scoped>
.category-tabs {
  position: sticky;
  top: 56px;
  z-index: 90;
  background: var(--apple-glass-bg);
  backdrop-filter: var(--apple-glass-blur);
  -webkit-backdrop-filter: var(--apple-glass-blur);
  border-bottom: 0.5px solid rgba(0, 0, 0, 0.08);
  padding: 0 16px;
}

.tabs-wrapper {
  display: flex;
  gap: 8px;
  max-width: 1200px;
  margin: 0 auto;
  overflow-x: auto;
  scrollbar-width: none;
}

.tabs-wrapper::-webkit-scrollbar {
  display: none;
}

.tab-item {
  position: relative;
  padding: 14px 16px;
  cursor: pointer;
  white-space: nowrap;
  font-size: 14px;
  font-weight: 500;
  color: var(--apple-text-secondary);
  transition: all 0.2s ease;
}

.tab-item::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 0;
  height: 2.5px;
  background: var(--category-color);
  border-radius: 1.5px;
  transition: width 0.25s ease;
}

.tab-item:hover {
  color: var(--category-color);
}

.tab-item.active {
  color: var(--category-color);
  font-weight: 600;
}

.tab-item.active::after {
  width: 100%;
}

/* Apple 玻璃气泡 */
.logo-bubble {
  position: absolute;
  top: 100%;
  left: 50%;
  transform: translateX(-50%);
  z-index: 100;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.bubble-body {
  padding: 14px 20px;
  border-radius: 0 0 14px 14px;
  background: var(--apple-glass-bg);
  backdrop-filter: var(--apple-glass-blur);
  -webkit-backdrop-filter: var(--apple-glass-blur);
  border: 1px solid rgba(255, 255, 255, 0.5);
  box-shadow: var(--apple-shadow-lg);
  display: flex;
  align-items: center;
  justify-content: center;
}

.bubble-logo {
  height: 30px;
  width: auto;
  display: block;
  border-radius: 6px;
}

.bubble-arrow {
  width: 0;
  height: 0;
  border-left: 10px solid transparent;
  border-right: 10px solid transparent;
  border-top: 10px solid transparent;
  position: relative;
  top: -1px;
}
</style>
