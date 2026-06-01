<template>
  <div class="category-tabs" ref="tabsRef">
    <div class="tabs-wrapper">
      <div
        v-for="tab in tabs"
        :key="tab.id ?? 'all'"
        class="tab-item"
        :class="{ active: currentTab === tab.id }"
        :style="{ '--category-color': tab.color }"
        @mouseenter="handleHover(tab, $event)"
        @mouseleave="handleLeave"
        @click="handleClick(tab)"
      >
        <span class="tab-text">{{ tab.name }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useCategoryStore } from '../stores/category'

const emit = defineEmits(['category-change'])
const categoryStore = useCategoryStore()

const tabs = computed(() => [
  { id: null, name: '全部', color: '#1A1A1A' },
  ...categoryStore.items.map(c => ({
    id: c.id,
    name: c.categoryName,
    color: '#007AFF'
  }))
])

const currentTab = ref(null)
const hoveredTab = ref(null)

const getLogo = () => ''

let hoverTimer = null

const handleHover = (tab) => {
  if (!tab.id) return
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

const handleClick = (tab) => {
  currentTab.value = tab.id
  emit('category-change', tab.id)
}

onMounted(async () => {
  await categoryStore.fetchCategories()
})

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
