<template>
  <div class="article-detail">
    <TopNav />
    <div class="article-content" v-if="article">
      <h1 class="article-title">{{ article.title }}</h1>
      <div class="article-meta">
        <span class="article-time">{{ article.updateTime }}</span>
        <span class="article-category" :style="{ color: getCategoryColor(article.categoryId) }">
          {{ getCategoryName(article.categoryId) }}
        </span>
      </div>
      <div class="article-cover" v-if="article.coverImage">
        <img :src="article.coverImage" :alt="article.title" />
      </div>
      <div class="article-body" v-html="article.content"></div>
    </div>
    <div class="loading" v-else>
      <n-spin size="large" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import TopNav from '../components/TopNav.vue'
import { getArticleDetail } from '../api/article'
import { getCategoryName, getCategoryColor } from '../utils/category'

const route = useRoute()
const article = ref(null)

onMounted(async () => {
  const res = await getArticleDetail(route.params.id)
  if (res.code === 0) {
    article.value = res.data
  }
})
</script>

<style scoped>
.article-detail {
  min-height: 100vh;
  background-color: var(--apple-bg);
  padding-bottom: 60px;
}

.article-content {
  max-width: 800px;
  margin: 0 auto;
  padding: 16px;
  padding-top: 70px;
}

.article-title {
  font-size: 26px;
  font-weight: 700;
  margin-bottom: 12px;
  color: var(--apple-text-primary);
  line-height: 1.3;
  letter-spacing: -0.5px;
}

.article-meta {
  display: flex;
  gap: 16px;
  color: var(--apple-text-secondary);
  font-size: 14px;
  margin-bottom: 16px;
}

.article-cover img {
  width: 100%;
  border-radius: var(--apple-radius-lg);
  margin-bottom: 16px;
  box-shadow: var(--apple-shadow-md);
}

.article-body {
  font-size: 16px;
  line-height: 1.8;
  color: var(--apple-text-primary);
}

.loading {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 50vh;
}
</style>
