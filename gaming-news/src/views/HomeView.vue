<template>
  <div class="arcade-cabinet">
    <!-- 像素背景网格 -->
    <div class="pixel-bg"></div>

    <!-- ============================================ -->
    <!--  顶部：汉堡菜单 + MARQUEE                     -->
    <!-- ============================================ -->
    <header class="top-bar">
      <!-- 汉堡菜单 -->
      <button class="hamburger" @click="menuOpen = !menuOpen">
        <span class="ham-line"></span>
        <span class="ham-line"></span>
        <span class="ham-line"></span>
      </button>

      <!-- 滑出菜单 -->
      <div v-if="menuOpen" class="slide-menu" @click.self="menuOpen = false">
        <div class="menu-panel">
          <div class="menu-header">
            <span class="menu-title">MENU</span>
            <button class="menu-close" @click="menuOpen = false">✕</button>
          </div>
          <template v-if="!isLogin">
            <button class="menu-item" @click="nav('/login?redirect=' + $route.fullPath)">
              <span class="menu-icon">▸</span> 登录
            </button>
            <button class="menu-item" @click="nav('/register')">
              <span class="menu-icon">▸</span> 注册
            </button>
          </template>
          <template v-else>
            <button class="menu-item" @click="nav('/publish')">
              <span class="menu-icon">▸</span> 发布文章
            </button>
            <button class="menu-item" @click="nav('/user')">
              <span class="menu-icon">▸</span> 个人中心
            </button>
            <button class="menu-item" @click="handleLogout">
              <span class="menu-icon">▸</span> 退出
            </button>
          </template>

          <!-- 移动端分类（桌面端由侧栏承担） -->
          <div class="menu-cats">
            <div class="menu-cats-title">SELECT</div>
            <button
              v-for="tab in tabs"
              :key="'m' + (tab.id ?? 'all')"
              class="menu-item"
              :class="{ 'cat-active': currentCategoryId === tab.id }"
              @click="selectCategory(tab.id); menuOpen = false"
            >
              <span class="cat-dot" :style="{ background: tab.dotColor }"></span>
              {{ tab.name }}
            </button>
          </div>
        </div>
      </div>

      <!-- Marqee 招牌 -->
      <div class="marquee">
        <div class="marquee-bulbs">
          <span v-for="i in 10" :key="'b'+i" class="bulb" :style="{ animationDelay: (i*0.12)+'s' }"></span>
        </div>
        <h1 class="marquee-title">
          ★ GAMEBOI ARCADE ★
        </h1>
        <div class="marquee-bulbs">
          <span v-for="i in 10" :key="'t'+i" class="bulb" :style="{ animationDelay: (i*0.12+1)+'s' }"></span>
        </div>
      </div>
    </header>

    <!-- ============================================ -->
    <!--  主体：左分类栏 + 右内容                       -->
    <!-- ============================================ -->
    <div class="main-layout">
      <!-- 左侧分类栏 -->
      <aside class="sidebar">
        <div class="sidebar-title">SELECT</div>
        <button
          v-for="tab in tabs"
          :key="tab.id ?? 'all'"
          class="cat-item"
          :class="{ active: currentCategoryId === tab.id }"
          @click="selectCategory(tab.id)"
        >
          <span class="cat-dot" :style="{ background: tab.dotColor }"></span>
          <span class="cat-name">{{ tab.name }}</span>
          <span class="cat-marker">{{ currentCategoryId === tab.id ? '◀' : '' }}</span>
        </button>
      </aside>

      <!-- 右侧内容区 -->
      <main class="content-area">
        <!-- 轮播（全部时显示） -->
        <SwiperBanner v-if="currentCategoryId === null" />

        <!-- 区域标题 -->
        <div class="section-hd">
          <span class="section-label">{{ currentTitle }}</span>
          <span class="section-count">{{ total }} 篇</span>
        </div>

        <!-- 文章列表 -->
        <ArticleList
          :categoryId="currentCategoryId"
          @update:total="t => total = t"
        />
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import SwiperBanner from '../components/SwiperBanner.vue'
import ArticleList from '../components/ArticleList.vue'
import { useCategoryStore } from '../stores/category'
import { useUserStore } from '../stores/user'

const router = useRouter()
const categoryStore = useCategoryStore()
const userStore = useUserStore()
const isLogin = computed(() => userStore.isLoggedIn)
const currentCategoryId = ref(null)
const total = ref(0)
const menuOpen = ref(false)

const CAT_COLORS = ['#0F380F', '#306230', '#8BAC0F', '#5A8A0F', '#1F4A1F', '#6B9A0F']

const tabs = computed(() => {
  const all = [{ id: null, name: 'ALL', dotColor: '#0F380F' }]
  const cats = categoryStore.items.map((c, i) => ({
    id: c.id, name: c.categoryName,
    dotColor: CAT_COLORS[i % CAT_COLORS.length]
  }))
  return [...all, ...cats]
})

const currentTitle = computed(() => {
  if (currentCategoryId.value === null) return '最新资讯'
  return categoryStore.getName(currentCategoryId.value)
})

const selectCategory = (id) => { currentCategoryId.value = id }
const nav = (path) => { menuOpen.value = false; router.push(path) }
const handleLogout = () => { userStore.logout(); menuOpen.value = false; router.push('/home') }

onMounted(async () => { await categoryStore.fetchCategories() })
</script>

<style scoped>
/* ============================================================
   GAMEBOI ARCADE — GameBoy 绿 全屏街机布局
   ============================================================ */

.arcade-cabinet {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--gb-lightest);
  position: relative;
  overflow: hidden;
}

/* ============================================================
   顶部栏：汉堡 + Marqee
   ============================================================ */

.top-bar {
  position: relative;
  flex-shrink: 0;
  height: 64px;
  z-index: 30;
  background: var(--gb-darkest);
  border-bottom: 3px solid var(--gb-dark);
}

.hamburger {
  position: absolute;
  left: 12px;
  top: 50%;
  transform: translateY(-50%);
  z-index: 40;
  display: flex;
  flex-direction: column;
  gap: 5px;
  padding: 8px 7px;
  background: var(--gb-light);
  border: 2px solid var(--gb-lightest);
  cursor: pointer;
  transition: background 0.1s steps(2);
}
.hamburger:hover { background: var(--gb-lightest); }
.ham-line { width: 20px; height: 3px; background: var(--gb-darkest); }

/* ---- 滑出菜单 ---- */
.slide-menu {
  position: fixed;
  inset: 0;
  z-index: 200;
  background: rgba(15, 56, 15, 0.4);
}
.menu-panel {
  position: absolute;
  left: 0; top: 0; bottom: 0;
  width: 240px;
  background: var(--gb-lightest);
  border-right: 3px solid var(--gb-darkest);
  padding: 20px 16px;
  box-shadow: 4px 0 0 var(--gb-dark);
  overflow-y: auto;
}
.menu-header {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 20px; padding-bottom: 12px; border-bottom: 2px solid var(--gb-darkest);
}
.menu-title { font-family: var(--font-pixel); font-size: 11px; color: var(--gb-darkest); }
.menu-close {
  font-size: 18px; background: none; border: 2px solid var(--gb-darkest); cursor: pointer;
  width: 30px; height: 30px; display: flex; align-items: center; justify-content: center;
  color: var(--gb-darkest);
}
.menu-close:hover { background: var(--gb-light); }
.menu-item {
  display: flex; align-items: center; gap: 10px;
  width: 100%; padding: 12px 10px; margin-bottom: 6px;
  background: none; border: 2px solid var(--gb-dark);
  font-family: var(--font-zh); font-size: 14px;
  color: var(--gb-darkest);
  cursor: pointer; transition: all 0.1s steps(2);
}
.menu-item:hover { border-color: var(--gb-darkest); background: var(--gb-light); }
.menu-item.cat-active { background: var(--gb-darkest); color: var(--gb-lightest); }
.menu-icon { font-size: 10px; }

/* 移动端分类区：桌面端隐藏 */
.menu-cats { display: none; }
.menu-cats-title {
  font-family: var(--font-pixel); font-size: 8px; color: var(--gb-dark);
  letter-spacing: 2px; margin: 18px 0 8px; padding-top: 14px;
  border-top: 2px solid var(--gb-dark);
}

/* ---- Marqee ---- */
.marquee {
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 4px 20px 4px 64px; /* 左侧给汉堡让位 */
  text-align: center;
}
.marquee-title {
  font-family: var(--font-pixel);
  font-size: clamp(9px, 2vw, 14px);
  color: var(--gb-lightest);
  letter-spacing: 3px;
  margin: 4px 0;
  text-shadow: 0 0 8px rgba(155, 188, 15, 0.5);
}
.marquee-bulbs {
  display: flex; justify-content: center; gap: 10px;
}
.bulb {
  width: 6px; height: 6px;
  background: var(--gb-dark);
  animation: bulbChase 1.5s ease-in-out infinite;
}
@keyframes bulbChase {
  0%,100% { opacity: 0.3; background: var(--gb-dark); }
  50%     { opacity: 1;   background: var(--gb-lightest); box-shadow: 0 0 8px 3px rgba(155, 188, 15, 0.7); }
}

/* ============================================================
   主体布局：全屏 Grid
   ============================================================ */

.main-layout {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: 160px 1fr;
  position: relative;
  z-index: 1;
}

/* === 左侧分类栏 === */
.sidebar {
  border-right: 3px solid var(--gb-darkest);
  background: var(--gb-light);
  padding: 0 0 40px;
  overflow-y: auto;
}

.sidebar-title {
  font-family: var(--font-pixel);
  font-size: 8px;
  color: var(--gb-darkest);
  padding: 14px 10px 10px;
  letter-spacing: 2px;
  border-bottom: 2px solid var(--gb-darkest);
}

.cat-item {
  display: flex;
  align-items: center;
  gap: 6px;
  width: 100%;
  padding: 12px 10px;
  background: none;
  border: none;
  border-bottom: 1px solid rgba(15, 56, 15, 0.25);
  cursor: pointer;
  font-family: var(--font-zh);
  font-size: 13px;
  color: var(--gb-darkest);
  transition: all 0.08s steps(2);
  text-align: left;
}

.cat-item:hover {
  background: var(--gb-lightest);
}

.cat-item.active {
  background: var(--gb-darkest);
  color: var(--gb-lightest);
}

.cat-dot {
  width: 8px; height: 8px;
  flex-shrink: 0;
  border: 1px solid var(--gb-darkest);
}

.cat-item.active .cat-dot {
  border-color: var(--gb-lightest);
}

.cat-name {
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.cat-marker {
  font-size: 10px;
  color: inherit;
}

/* === 右侧内容 === */
.content-area {
  min-width: 0;
  overflow-y: auto;
}

.section-hd {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 16px 10px;
  border-bottom: 2px solid var(--gb-dark);
  margin: 0 16px;
}

.section-label {
  font-family: var(--font-zh);
  font-size: 20px;
  color: var(--gb-darkest);
  letter-spacing: 2px;
}

.section-count {
  font-family: var(--font-body);
  font-size: 11px;
  color: var(--gb-dark);
}

/* ============================================================
   响应式：移动端侧栏收进汉堡抽屉
   ============================================================ */

@media (max-width: 768px) {
  .main-layout {
    grid-template-columns: 1fr;
  }
  .sidebar {
    display: none;
  }
  .menu-cats {
    display: block;
  }
}
</style>
