<template>
  <div class="home-view">
    <TopNav />
    <transition name="page-enter">
      <div class="home-content">
        <CategoryTabs @category-change="handleCategoryChange" />
        <SwiperBanner v-if="currentCategory === 'all'" />
        <ArticleList :category="currentCategory" />
      </div>
    </transition>
    <BottomNav />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import TopNav from '../components/TopNav.vue'
import CategoryTabs from '../components/CategoryTabs.vue'
import SwiperBanner from '../components/SwiperBanner.vue'
import ArticleList from '../components/ArticleList.vue'
import BottomNav from '../components/BottomNav.vue'

const currentCategory = ref('all')

const handleCategoryChange = (category) => {
  currentCategory.value = category
}
</script>

<style scoped>
.home-view {
  min-height: 100vh;
  background-color: var(--apple-bg);
  padding-bottom: 60px;
}

.home-content {
  animation: pageSlideIn 0.8s ease-out;
}

@keyframes pageSlideIn {
  0% {
    opacity: 0;
    transform: translateY(30px);
  }
  100% {
    opacity: 1;
    transform: translateY(0);
  }
}

.page-enter-enter-active {
  animation: pageSlideIn 0.8s ease-out;
}
</style>
