<template>
  <div class="landing-view">
    <!-- 樱花粒子背景 -->
    <div class="sakura-container">
      <div
        v-for="(particle, i) in particles"
        :key="i"
        class="sakura-particle"
        :style="particle.style"
      ></div>
    </div>

    <div class="content">
      <h1 class="title">
        <span class="title-main">玩游戏，看咨询，玩转热门，就来</span>
        <span class="title-tsunami">
          <span
            v-for="(char, index) in tsunamiChars"
            :key="index"
            class="tsunami-char"
            :style="char.style"
          >{{ char.char }}</span>
        </span>
        <span class="title-suffix">游戏资讯平台</span>
      </h1>

      <p class="subtitle">专注游戏资讯 | 第一时间送达</p>

      <button class="enter-btn" @click="handleEnter">
        <span class="btn-text">当机立断往里进</span>
        <span class="btn-icon">→</span>
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const tsunamiChars = ref([])
const particles = ref([])

const seededRandom = (seed) => {
  const x = Math.sin(seed * 9999) * 10000
  return x - Math.floor(x)
}

const initParticles = () => {
  const colors = ['#ffb6c1', '#ff91a4', '#ff69b4', '#ffc0cb', '#ff7eb3', '#e5779a']
  particles.value = Array.from({ length: 150 }, (_, i) => {
    const r = seededRandom(i + 1)
    const size = 2 + r * 4
    const color = colors[Math.floor(r * colors.length)]
    return {
      style: {
        left: `${60 + r * 40}%`,
        top: `-5%`,
        width: `${size}px`,
        height: `${size}px`,
        background: `radial-gradient(circle, ${color} 0%, transparent 70%)`,
        animationDelay: `${r * 8}s`,
        animationDuration: `${12 + r * 10}s`,
        opacity: 0.2 + r * 0.4,
        '--drift': `${-60 + r * 120}px`
      }
    }
  })
}

const initTsunamiChars = () => {
  tsunamiChars.value = 'Tsunami'.split('').map((char, index) => {
    const angle = (index / 7) * 360
    const distance = 400 + seededRandom(index + 100) * 200
    const rad = (angle * Math.PI) / 180
    const startX = Math.cos(rad) * distance
    const startY = Math.sin(rad) * distance
    return {
      char,
      style: {
        '--start-x': `${startX}px`,
        '--start-y': `${startY}px`,
        animationDelay: `${1.2 + index * 0.1}s`
      }
    }
  })
}

onMounted(() => {
  initParticles()
  initTsunamiChars()
})

const handleEnter = () => {
  router.push('/home')
}
</script>

<style scoped>
.landing-view {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #fef9f9 0%, #fdf2f2 50%, #fff5f5 100%);
  position: relative;
  overflow: hidden;
}

.sakura-container {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}

.sakura-particle {
  position: absolute;
  border-radius: 50%;
  animation: sakuraDance linear infinite;
}

@keyframes sakuraDance {
  0% {
    transform: translateY(0) translateX(0) rotate(0deg);
    opacity: 0;
  }
  5% {
    opacity: 1;
  }
  50% {
    transform: translateY(55vh) translateX(calc(var(--drift) * 0.5)) rotate(180deg);
    opacity: 0.7;
  }
  100% {
    transform: translateY(110vh) translateX(var(--drift)) rotate(360deg);
    opacity: 0;
  }
}

.content {
  text-align: center;
  z-index: 10;
  padding: 0 20px;
  max-width: 900px;
}

.title {
  font-size: clamp(20px, 4vw, 42px);
  font-weight: bold;
  color: #333;
  margin-bottom: 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
  line-height: 1.6;
}

.title-main {
  color: #e85d75;
  font-size: clamp(18px, 3vw, 26px);
  animation: fadeSlideDown 0.8s ease-out forwards;
  opacity: 0;
}

.title-tsunami {
  display: flex;
  justify-content: center;
  margin: 16px 0;
}

.title-suffix {
  color: #ff6b6b;
  font-size: clamp(22px, 4vw, 32px);
  animation: fadeSlideDown 0.8s 0.8s ease-out forwards;
  opacity: 0;
}

@keyframes fadeSlideDown {
  0% { opacity: 0; transform: translateY(-30px); }
  100% { opacity: 1; transform: translateY(0); }
}

.tsunami-char {
  display: inline-block;
  font-size: clamp(48px, 8vw, 80px);
  font-weight: 900;
  color: #ff6b6b;
  text-shadow: 0 0 10px rgba(255, 107, 107, 0.8), 0 0 20px rgba(255, 107, 107, 0.5);
  opacity: 0;
  animation: tsunamiAssemble 0.8s ease-out forwards;
}

@keyframes tsunamiAssemble {
  0% { opacity: 0; transform: translate(var(--start-x), var(--start-y)) scale(0.5); }
  60% { opacity: 1; transform: translate(0, 0) scale(1.2); }
  80% { transform: translate(0, 0) scale(0.95); }
  100% { opacity: 1; transform: translate(0, 0) scale(1); }
}

.subtitle {
  font-size: 18px;
  color: #e85d75;
  margin-bottom: 50px;
  letter-spacing: 4px;
  opacity: 0;
  animation: fadeIn 0.8s 2s ease-out forwards;
}

@keyframes fadeIn {
  0% { opacity: 0; transform: translateY(20px); }
  100% { opacity: 1; transform: translateY(0); }
}

.enter-btn {
  display: inline-flex;
  align-items: center;
  gap: 14px;
  padding: 18px 50px;
  font-size: 20px;
  font-weight: bold;
  color: #fff;
  background: linear-gradient(135deg, #ff6b6b, #ff8e8e, #ffb6c1);
  border: none;
  border-radius: 50px;
  cursor: pointer;
  opacity: 0;
  transform: translateY(30px);
  animation: btnAppear 0.8s 2.5s ease-out forwards;
  transition: all 0.3s ease;
  box-shadow: 0 8px 30px rgba(255, 107, 107, 0.4);
}

@keyframes btnAppear {
  0% { opacity: 0; transform: translateY(30px) scale(0.9); }
  100% { opacity: 1; transform: translateY(0) scale(1); }
}

.enter-btn:hover {
  transform: translateY(-4px) scale(1.05);
  box-shadow: 0 12px 40px rgba(255, 107, 107, 0.5);
}

.btn-icon {
  transition: transform 0.3s;
}

.enter-btn:hover .btn-icon {
  transform: translateX(6px);
}
</style>
