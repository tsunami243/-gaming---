<template>
  <div class="landing-view" ref="rootRef">
    <!-- CRT 屏幕整体（退场时整体压缩） -->
    <div class="crt-screen" ref="crtRef">
      <!-- Matrix 像素文字雨 -->
      <div class="matrix-rain">
        <div
          v-for="col in rainColumns"
          :key="col.id"
          class="rain-column"
          :style="col.style"
        >
          <span
            v-for="(ch, j) in col.chars"
            :key="j"
            class="rain-char"
            :style="{ animationDelay: (j * col.charDelay) + 's', opacity: 0.15 + (j / col.chars.length) * 0.25 }"
          >{{ ch }}</span>
        </div>
      </div>
      <!-- LCD 扫描线叠加层 -->
      <div class="scanlines"></div>
      <!-- 像素网格纹理 -->
      <div class="pixel-grid"></div>
      <!-- 开机扫描亮线 -->
      <div class="power-sweep" ref="sweepRef"></div>
      <!-- LCD 屏幕边框 -->
      <div class="screen-bezel"></div>

      <div class="content" ref="contentRef">
        <!-- GameBoi Logo — 模拟 GameBoy 开机动画 -->
        <div class="logo-block">
          <h1 class="brand-name">
            <span
              v-for="(char, i) in brandChars"
              :key="i"
              class="brand-char"
            >{{ char }}</span>
          </h1>
          <div class="brand-line" ref="lineRef"></div>
          <p class="brand-sub">
            <span
              v-for="(char, i) in subChars"
              :key="i"
              class="brand-sub-char"
            >{{ char }}</span>
          </p>
        </div>

        <!-- 开机副标题 -->
        <div class="boot-info" ref="bootRef">
          <p class="boot-text">
            <span class="blink-cursor">■</span> 专注游戏资讯 · 第一时间送达
          </p>
          <p class="boot-version">v1.0 · 2026</p>
        </div>

        <!-- START 按钮 -->
        <button class="start-btn" ref="btnRef" @click="handleEnter">
          <span class="btn-frame">
            <span class="btn-text">▶ &nbsp;START</span>
          </span>
        </button>

        <!-- 底部装饰：GameBoy 按键图标 -->
        <div class="dpad-hint" ref="dpadRef">
          <div class="dpad">
            <span class="dpad-v"></span>
            <span class="dpad-h"></span>
          </div>
          <span class="dpad-label">PRESS ENTER</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import gsap from 'gsap'

const router = useRouter()
const brandChars = 'GAMEBOI'.split('')
const subChars = '游戏资讯平台'.split('')
const rainColumns = ref([])

const rootRef = ref(null)
const crtRef = ref(null)
const contentRef = ref(null)
const sweepRef = ref(null)
const lineRef = ref(null)
const bootRef = ref(null)
const btnRef = ref(null)
const dpadRef = ref(null)

let bootTl = null
let pulseTween = null
let exiting = false

// Matrix 像素文字雨 — 游戏符号 + 片假名 + 像素块
const RAIN_CHARS = [
  '☆','♥','♦','♣','●','○','□','■','▲','▼','▶','◀',
  '游','戏','玩','战','斗','攻','防','血','魔','法',
  'ア','イ','ウ','エ','オ','カ','キ','ク','ケ','コ',
  '0','1','▌','█','░','▒','▓','▀','▄',
]

const seededRandom = (seed) => {
  const x = Math.sin(seed * 9999) * 10000
  return x - Math.floor(x)
}

const initRain = () => {
  const cols = []
  const colCount = 45
  for (let i = 0; i < colCount; i++) {
    const r = seededRandom(i + 42)
    const charCount = 12 + Math.floor(r * 10)
    const chars = []
    for (let j = 0; j < charCount; j++) {
      chars.push(RAIN_CHARS[Math.floor(seededRandom(i * 100 + j) * RAIN_CHARS.length)])
    }
    const baseLeft = (i / colCount) * 100
    const jitter = (r - 0.5) * 6
    const left = baseLeft + jitter
    const duration = 6 + r * 10
    const delay = r * duration
    cols.push({
      id: i,
      chars,
      charDelay: 0.18 + r * 0.3,
      style: {
        left: `${left}%`,
        animationDuration: `${duration}s`,
        animationDelay: `-${delay}s`,
        fontSize: `${10 + r * 10}px`,
      },
    })
  }
  rainColumns.value = cols
}

const playBootSequence = () => {
  const chars = contentRef.value.querySelectorAll('.brand-char')
  const subCharsEls = contentRef.value.querySelectorAll('.brand-sub-char')

  gsap.set([bootRef.value, btnRef.value, dpadRef.value], { opacity: 0 })
  gsap.set(subCharsEls, { opacity: 0 })
  gsap.set(lineRef.value, { scaleX: 0 })

  bootTl = gsap.timeline({ defaults: { ease: 'power2.out' } })

  // t=0.0 电源闪烁：LCD 点亮
  bootTl.fromTo(crtRef.value,
    { opacity: 0, filter: 'brightness(4)' },
    { opacity: 1, filter: 'brightness(1)', duration: 0.45, ease: 'steps(5)' }
  )

  // t=0.15 开机扫描亮线从上往下扫过
  bootTl.fromTo(sweepRef.value,
    { top: '-4%', opacity: 1 },
    { top: '104%', duration: 0.55, ease: 'power1.in' },
    0.15
  )
  bootTl.set(sweepRef.value, { opacity: 0 })

  // t=0.5 LOGO 字符弹性落下
  bootTl.fromTo(chars,
    { y: -90, opacity: 0, scaleY: 2.2 },
    { y: 0, opacity: 1, scaleY: 1, duration: 0.7, stagger: 0.09, ease: 'elastic.out(1, 0.45)' },
    0.5
  )

  // 字符落地时屏幕微震
  bootTl.to(crtRef.value, {
    x: 2, y: 1, duration: 0.05, repeat: 3, yoyo: true, ease: 'steps(1)'
  }, 1.05)

  // t=1.35 分割线展开
  bootTl.to(lineRef.value, { scaleX: 1, duration: 0.4, ease: 'steps(8)' }, 1.35)

  // t=1.6 副标题打字机
  bootTl.to(subCharsEls, {
    opacity: 1, duration: 0.01, stagger: 0.12, ease: 'steps(1)'
  }, 1.6)

  // t=1.9 boot-info 淡入
  bootTl.to(bootRef.value, { opacity: 1, y: 0, duration: 0.4 }, 2.0)

  // t=2.2 START 按钮弹入
  bootTl.fromTo(btnRef.value,
    { opacity: 0, scale: 0.6 },
    { opacity: 1, scale: 1, duration: 0.45, ease: 'back.out(2.5)' },
    2.2
  )

  // START 呼吸脉冲（循环）
  bootTl.call(() => {
    pulseTween = gsap.to(btnRef.value, {
      scale: 1.06, duration: 0.5, yoyo: true, repeat: -1, ease: 'steps(2)'
    })
  })

  // t=2.5 十字键滑入
  bootTl.fromTo(dpadRef.value,
    { opacity: 0, y: 16 },
    { opacity: 1, y: 0, duration: 0.4, ease: 'steps(4)' },
    2.5
  )
}

const handleEnter = () => {
  if (exiting) return
  exiting = true
  window.removeEventListener('keydown', onKeydown)
  if (pulseTween) pulseTween.kill()
  if (bootTl) bootTl.kill()

  // CRT 关机：内容压成水平亮线 → 亮线横向收缩 → 黑屏
  rootRef.value.style.backgroundColor = '#000'
  const exitTl = gsap.timeline({
    onComplete: () => router.push('/home')
  })
  exitTl
    .to(crtRef.value, { scaleY: 0.008, duration: 0.3, ease: 'power3.in' })
    .to(crtRef.value, { scaleX: 0, duration: 0.22, ease: 'power3.in' })
    .to(crtRef.value, { opacity: 0, duration: 0.1 })
}

const onKeydown = (e) => {
  if (e.key === 'Enter') handleEnter()
}

onMounted(() => {
  initRain()
  playBootSequence()
  window.addEventListener('keydown', onKeydown)
})

onUnmounted(() => {
  window.removeEventListener('keydown', onKeydown)
  if (pulseTween) pulseTween.kill()
  if (bootTl) bootTl.kill()
  gsap.killTweensOf('*')
})
</script>

<style scoped>
/* ============================================================
   GameBoi Landing — GameBoy LCD Screen Aesthetic (GSAP)
   ============================================================ */

.landing-view {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: var(--gb-lightest);
  position: relative;
  overflow: hidden;
  cursor: default;
  user-select: none;
}

.crt-screen {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: var(--gb-lightest);
  will-change: transform;
}

/* ---- LCD 扫描线 ---- */
.scanlines {
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: 5;
  background: repeating-linear-gradient(
    0deg,
    transparent,
    transparent 2px,
    rgba(0, 0, 0, 0.03) 2px,
    rgba(0, 0, 0, 0.03) 4px
  );
}

/* ---- 像素网格纹理 ---- */
.pixel-grid {
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: 4;
  background-image:
    linear-gradient(rgba(48, 98, 48, 0.06) 1px, transparent 1px),
    linear-gradient(90deg, rgba(48, 98, 48, 0.06) 1px, transparent 1px);
  background-size: 6px 6px;
}

/* ---- 开机扫描亮线 ---- */
.power-sweep {
  position: absolute;
  left: 0;
  right: 0;
  top: -4%;
  height: 3%;
  z-index: 6;
  pointer-events: none;
  opacity: 0;
  background: linear-gradient(
    to bottom,
    transparent,
    rgba(15, 56, 15, 0.25),
    rgba(155, 188, 15, 0.9),
    rgba(15, 56, 15, 0.25),
    transparent
  );
}

/* ---- 屏幕边框（模拟掌机屏幕内框） ---- */
.screen-bezel {
  position: absolute;
  inset: 12px;
  border: 3px solid var(--gb-darkest);
  border-radius: 1px;
  pointer-events: none;
  z-index: 3;
  box-shadow: inset 0 0 30px rgba(15, 56, 15, 0.08);
}

/* ---- 内容区 ---- */
.content {
  position: relative;
  z-index: 10;
  text-align: center;
  padding: 20px;
  max-width: 520px;
}

/* ============================================================
   LOGO
   ============================================================ */

.logo-block {
  margin-bottom: 32px;
}

.brand-name {
  display: flex;
  justify-content: center;
  gap: 4px;
  margin: 0 0 10px;
}

.brand-char {
  display: inline-block;
  font-family: var(--font-pixel);
  font-size: clamp(28px, 6vw, 52px);
  font-weight: 400;
  color: var(--gb-darkest);
  letter-spacing: 2px;
  text-shadow:
    2px 2px 0 var(--gb-dark),
    -1px -1px 0 var(--gb-lightest);
  opacity: 0;
  will-change: transform;
}

/* 品牌分割线 */
.brand-line {
  width: 140px;
  height: 4px;
  background: var(--gb-darkest);
  margin: 6px auto 14px;
  transform-origin: center;
  box-shadow: 0 1px 0 var(--gb-dark);
}

/* 品牌副标题 */
.brand-sub {
  font-family: var(--font-body);
  font-size: 14px;
  font-weight: bold;
  color: var(--gb-dark);
  letter-spacing: 6px;
}

.brand-sub-char {
  display: inline-block;
}

/* ============================================================
   开机信息
   ============================================================ */

.boot-info {
  margin-bottom: 40px;
}

.boot-text {
  font-family: var(--font-body);
  font-size: 13px;
  color: var(--gb-darkest);
  letter-spacing: 1px;
  margin: 0 0 6px;
}

.blink-cursor {
  display: inline-block;
  animation: blink 1s step-end infinite;
  color: var(--gb-darkest);
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50%      { opacity: 0; }
}

.boot-version {
  font-family: var(--font-body);
  font-size: 10px;
  color: var(--gb-dark);
  letter-spacing: 2px;
}

/* ============================================================
   START 按钮 — 像素块风格
   ============================================================ */

.start-btn {
  position: relative;
  display: inline-block;
  background: none;
  border: none;
  cursor: pointer;
  padding: 0;
  will-change: transform;
}

.btn-frame {
  display: inline-block;
  padding: 14px 36px;
  background: var(--gb-light);
  border: 3px solid var(--gb-darkest);
  position: relative;
  box-shadow:
    4px 4px 0 var(--gb-dark),
    inset -2px -2px 0 rgba(15, 56, 15, 0.15),
    inset 2px 2px 0 rgba(255, 255, 255, 0.08);
  transition: transform 0.08s steps(2), box-shadow 0.08s steps(2);
}

.start-btn:hover .btn-frame {
  transform: translate(1px, 1px);
  box-shadow:
    3px 3px 0 var(--gb-dark),
    inset -2px -2px 0 rgba(15, 56, 15, 0.15),
    inset 2px 2px 0 rgba(255, 255, 255, 0.08);
}

.start-btn:active .btn-frame {
  transform: translate(4px, 4px);
  box-shadow:
    0 0 0 var(--gb-dark),
    inset -2px -2px 0 rgba(15, 56, 15, 0.15),
    inset 2px 2px 0 rgba(255, 255, 255, 0.08);
}

.btn-text {
  font-family: var(--font-pixel);
  font-size: 14px;
  color: var(--gb-darkest);
  letter-spacing: 2px;
}

/* ---- D-Pad 装饰 ---- */
.dpad-hint {
  margin-top: 48px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.dpad {
  position: relative;
  width: 40px;
  height: 40px;
}

.dpad-v {
  position: absolute;
  left: 12px;
  top: 0;
  width: 16px;
  height: 40px;
  background: var(--gb-darkest);
  opacity: 0.2;
}

.dpad-h {
  position: absolute;
  left: 0;
  top: 12px;
  width: 40px;
  height: 16px;
  background: var(--gb-darkest);
  opacity: 0.2;
}

.dpad-label {
  font-family: var(--font-body);
  font-size: 9px;
  color: var(--gb-dark);
  letter-spacing: 3px;
}

/* ============================================================
   Matrix 像素文字雨
   ============================================================ */

.matrix-rain {
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: 2;
  overflow: hidden;
}

.rain-column {
  position: absolute;
  top: -20%;
  display: flex;
  flex-direction: column;
  gap: 0;
  animation: rainFall linear infinite;
}

.rain-char {
  font-family: var(--font-body);
  color: var(--gb-dark);
  line-height: 1.2;
  text-shadow: 0 0 3px rgba(48, 98, 48, 0.3);
  animation: charFlicker 3s ease-in-out infinite;
}

@keyframes rainFall {
  0%   { transform: translateY(-100%); }
  100% { transform: translateY(120vh); }
}

@keyframes charFlicker {
  0%, 100% { opacity: inherit; }
  30%      { opacity: 0.08; }
  32%      { opacity: 0.35; }
  34%      { opacity: 0.1; }
  60%      { opacity: inherit; }
}
</style>
