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
  margin: 16px;
  border-radius: var(--apple-radius-lg);
  overflow: hidden;
  box-shadow: var(--apple-shadow-md);
}

.banner-item {
  width: 100%;
  height: 200px;
  background-size: cover;
  background-position: center;
  cursor: pointer;
}

.banner-overlay {
  width: 100%;
  height: 100%;
  background: linear-gradient(to right, rgba(0,0,0,0.65) 0%, rgba(0,0,0,0.15) 100%);
  display: flex;
  align-items: center;
  padding: 24px;
}

.banner-content {
  color: var(--color-white);
  max-width: 60%;
}

.banner-tag {
  display: inline-block;
  padding: 4px 12px;
  border-radius: var(--apple-radius-sm);
  font-size: 12px;
  font-weight: 600;
  margin-bottom: 8px;
}

.banner-title {
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 8px;
  line-height: 1.4;
}

.banner-desc {
  font-size: 14px;
  opacity: 0.9;
  line-height: 1.5;
}
</style>
