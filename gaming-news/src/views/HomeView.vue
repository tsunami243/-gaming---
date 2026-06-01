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

const CAT_COLORS = ['#4ADE80','#DC2626','#F97316','#EF4444','#3B82F6','#A855F7','#EC4899','#14B8A6']

const tabs = computed(() => {
  const all = [{ id: null, name: 'ALL', dotColor: '#111' }]
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
   GAMEBOI ARCADE — 白底像素 + 左分类 + 汉堡菜单
   ============================================================ */

.arcade-cabinet {
  min-height: 100vh;
  background: #fff;
  position: relative;
  overflow-x: hidden;
}

/* ---- 像素背景网格 ---- */
.pixel-bg {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 0;
  background-image:
    radial-gradient(circle, #ddd 1px, transparent 1px);
  background-size: 8px 8px;
}

/* ============================================================
   顶部栏：汉堡 + Marqee
   ============================================================ */

.top-bar {
  position: sticky;
  top: 0;
  z-index: 30;
  background: #fff;
  border-bottom: 3px solid #111;
}

.hamburger {
  position: absolute;
  left: 14px;
  top: 14px;
  z-index: 40;
  display: flex;
  flex-direction: column;
  gap: 5px;
  padding: 8px 7px;
  background: #111;
  border: 2px solid #111;
  cursor: pointer;
  transition: background 0.1s;
}
.hamburger:hover { background: #333; }
.ham-line { width: 20px; height: 3px; background: #fff; }

/* ---- 滑出菜单 ---- */
.slide-menu {
  position: fixed;
  inset: 0;
  z-index: 200;
  background: rgba(0,0,0,0.3);
}
.menu-panel {
  position: absolute;
  left: 0; top: 0; bottom: 0;
  width: 220px;
  background: #fff;
  border-right: 3px solid #111;
  padding: 20px 16px;
  box-shadow: 4px 0 20px rgba(0,0,0,0.15);
}
.menu-header {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 20px; padding-bottom: 12px; border-bottom: 2px solid #111;
}
.menu-title { font-family: 'Press Start 2P', monospace; font-size: 11px; color: #111; }
.menu-close {
  font-size: 18px; background: none; border: 2px solid #111; cursor: pointer;
  width: 30px; height: 30px; display: flex; align-items: center; justify-content: center;
}
.menu-item {
  display: flex; align-items: center; gap: 10px;
  width: 100%; padding: 12px 10px; margin-bottom: 6px;
  background: none; border: 2px solid #ddd;
  font-family: 'Courier New', monospace; font-size: 14px;
  cursor: pointer; transition: all 0.1s;
}
.menu-item:hover { border-color: #111; background: #f5f5f5; }
.menu-icon { font-size: 10px; }

/* ---- Marqee ---- */
.marquee {
  background: #111;
  padding: 10px 20px 8px;
  text-align: center;
  margin-left: 52px; /* 给汉堡让位 */
}
.marquee-title {
  font-family: 'Press Start 2P', monospace;
  font-size: clamp(9px, 2vw, 14px);
  color: #fff;
  letter-spacing: 3px;
  margin: 6px 0;
  text-shadow: 0 0 8px rgba(255,255,255,0.4);
}
.marquee-bulbs {
  display: flex; justify-content: center; gap: 10px;
}
.bulb {
  width: 6px; height: 6px; border-radius: 50%;
  background: #FDE047; box-shadow: 0 0 5px 2px rgba(253,224,71,0.5);
  animation: bulbChase 1.5s ease-in-out infinite;
}
@keyframes bulbChase {
  0%,100% { opacity: 0.3; background: #FDE047; }
  50%     { opacity: 1;   background: #fff; box-shadow: 0 0 8px 3px rgba(255,255,255,0.7); }
}

/* ============================================================
   主体布局
   ============================================================ */

.main-layout {
  display: flex;
  max-width: 1100px;
  margin: 0 auto;
  position: relative;
  z-index: 1;
}

/* === 左侧分类栏 === */
.sidebar {
  width: 130px;
  flex-shrink: 0;
  border-right: 3px solid #111;
  background: #fafafa;
  padding: 0 0 40px;
  min-height: calc(100vh - 110px);
  position: sticky;
  top: 110px;
  align-self: flex-start;
}

.sidebar-title {
  font-family: 'Press Start 2P', monospace;
  font-size: 8px;
  color: #999;
  padding: 14px 8px 10px;
  letter-spacing: 2px;
  border-bottom: 1px solid #ddd;
}

.cat-item {
  display: flex;
  align-items: center;
  gap: 6px;
  width: 100%;
  padding: 10px 8px;
  background: none;
  border: none;
  border-bottom: 1px solid #eee;
  cursor: pointer;
  font-family: 'ZCOOL KuaiLe', 'Courier New', monospace;
  font-size: 13px;
  font-weight: normal;
  color: #555;
  transition: all 0.08s;
  text-align: left;
}

.cat-item:hover {
  background: #f0f0f0;
  color: #111;
}

.cat-item.active {
  background: #111;
  color: #fff;
}

.cat-dot {
  width: 8px; height: 8px; border-radius: 2px;
  flex-shrink: 0;
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
  flex: 1;
  min-width: 0;
  padding: 0;
}

.section-hd {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 16px 10px;
  border-bottom: 2px solid #eee;
  margin: 0 12px;
}

.section-label {
  font-family: 'ZCOOL KuaiLe', 'Press Start 2P', 'Courier New', monospace;
  font-size: 20px;
  font-weight: normal;
  color: #111;
  letter-spacing: 2px;
}

.section-count {
  font-family: 'Courier New', monospace;
  font-size: 11px;
  color: #999;
}
</style>
