<template>
  <div class="swiper-banner">
    <n-carousel v-if="banners.length > 0" :interval="4000" :loop="true" autoplay>
      <div
        v-for="(banner, index) in banners"
        :key="index"
        class="banner-item"
        :style="{ backgroundImage: `url(${banner.coverImg})` }"
        @click="handleClick(banner.id)"
      >
        <div class="banner-overlay">
          <div class="banner-content">
            <h2 class="banner-title">{{ banner.title }}</h2>
            <p class="banner-desc">点击查看详情</p>
          </div>
        </div>
      </div>
    </n-carousel>
    <div v-else class="no-banner">
      <n-empty description="暂无轮播图" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { NCarousel, NEmpty } from 'naive-ui'
import { getCarouselList } from '../api/article'

const router = useRouter()
const banners = ref([])

const handleClick = (articleId) => {
  router.push(`/article/${articleId}`)
}

onMounted(async () => {
  try {
    const res = await getCarouselList(5)
    if (res.code === 0 && res.data) {
      banners.value = res.data
    }
  } catch (error) {
    console.error('获取轮播图失败', error)
  }
})
</script>

<style scoped>
.swiper-banner {
  margin: 16px 16px 4px;
  border: 3px solid var(--gb-darkest);
  box-shadow: 4px 4px 0 var(--gb-dark);
  overflow: hidden;
}

.banner-item {
  width: 100%;
  height: 220px;
  background-size: cover;
  background-position: center;
  cursor: pointer;
}

.banner-overlay {
  width: 100%;
  height: 100%;
  background: linear-gradient(to top, rgba(15, 56, 15, 0.85) 0%, rgba(15, 56, 15, 0.1) 60%);
  display: flex;
  align-items: flex-end;
  padding: 16px;
}

.banner-content {
  color: var(--gb-lightest);
}

.banner-title {
  font-family: var(--font-pixel);
  font-size: 10px;
  margin-bottom: 4px;
  line-height: 1.5;
  text-shadow: 0 2px 4px rgba(15, 56, 15, 0.8);
}

.banner-desc {
  font-family: var(--font-body);
  font-size: 11px;
  opacity: 0.85;
}
</style>
