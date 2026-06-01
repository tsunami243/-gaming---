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
  margin: 12px;
  border: 3px solid #111;
  overflow: hidden;
}

.banner-item {
  width: 100%;
  height: 180px;
  background-size: cover;
  background-position: center;
  cursor: pointer;
}

.banner-overlay {
  width: 100%;
  height: 100%;
  background: linear-gradient(to top, rgba(0,0,0,0.7) 0%, rgba(0,0,0,0.1) 60%);
  display: flex;
  align-items: flex-end;
  padding: 16px;
}

.banner-content {
  color: #fff;
}

.banner-title {
  font-family: 'Press Start 2P', monospace;
  font-size: 10px;
  margin-bottom: 4px;
  line-height: 1.5;
  text-shadow: 0 2px 4px rgba(0,0,0,0.5);
}

.banner-desc {
  font-family: 'Courier New', monospace;
  font-size: 11px;
  opacity: 0.8;
}
</style>
