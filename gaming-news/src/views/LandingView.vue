<template>
  <div class="landing-view">
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
    <!-- LCD 屏幕边框 -->
    <div class="screen-bezel"></div>

    <div class="content">
      <!-- GameBoi Logo — 模拟 GameBoy 开机动画 -->
      <div class="logo-block">
        <h1 class="brand-name">
          <span
            v-for="(char, i) in brandChars"
            :key="i"
            class="brand-char"
            :style="{ animationDelay: (0.6 + i * 0.08) + 's' }"
          >{{ char }}</span>
        </h1>
        <div class="brand-line"></div>
        <p class="brand-sub">游戏资讯平台</p>
      </div>

      <!-- 开机副标题 -->
      <div class="boot-info">
        <p class="boot-text">
          <span class="blink-cursor">■</span> 专注游戏资讯 · 第一时间送达
        </p>
        <p class="boot-version">v1.0 · 2026</p>
      </div>

      <!-- START 按钮 -->
      <button class="start-btn" @click="handleEnter">
        <span class="btn-frame">
          <span class="btn-text">▶ &nbsp;START</span>
        </span>
        <span class="btn-shadow"></span>
      </button>

      <!-- 底部装饰：GameBoy 按键图标 -->
      <div class="dpad-hint">
        <div class="dpad">
          <span class="dpad-v"></span>
          <span class="dpad-h"></span>
        </div>
        <span class="dpad-label">PRESS ENTER</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const brandChars = 'GAMEBOI'.split('')
const rainColumns = ref([])

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
    // 更长的字符链 (12–22个)，覆盖更多面积
    const charCount = 12 + Math.floor(r * 10)
    const chars = []
    for (let j = 0; j < charCount; j++) {
      chars.push(RAIN_CHARS[Math.floor(seededRandom(i * 100 + j) * RAIN_CHARS.length)])
    }
    // 均匀分布全宽，加一些随机偏移
    const baseLeft = (i / colCount) * 100
    const jitter = (r - 0.5) * 6  // ±3%
    const left = baseLeft + jitter
    const duration = 6 + r * 10  // 6-16s，稍快一些增加密度
    const delay = r * duration   // 用百分比偏移确保各列不同阶段
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

onMounted(() => {
  initRain()
})

const handleEnter = () => {
  router.push('/home')
}
</script>

<style scoped>
/* ============================================================
   GameBoi Landing — GameBoy LCD Screen Aesthetic
   ============================================================ */

/* ---- GameBoy LCD 绿 色板 ---- */
:root {
  --gb-bg:       #9BBC0F;
  --gb-bg-dark:  #8BAC0F;
  --gb-text:     #0F380F;
  --gb-text-dim: #306230;
  --gb-accent:   #306230;
  --gb-border:   #0F380F;
}

.landing-view {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: var(--gb-bg);
  position: relative;
  overflow: hidden;
  cursor: default;
  user-select: none;
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

/* ---- 屏幕边框（模拟掌机屏幕内框） ---- */
.screen-bezel {
  position: absolute;
  inset: 12px;
  border: 3px solid var(--gb-border);
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
  animation: lcdPowerOn 0.35s ease-out;
}

/* LCD 开机闪烁 */
@keyframes lcdPowerOn {
  0%   { opacity: 0; filter: brightness(3); }
  20%  { opacity: 0.6; filter: brightness(2); }
  60%  { opacity: 0.9; filter: brightness(0.8); }
  100% { opacity: 1; filter: brightness(1); }
}

/* ============================================================
   LOGO — Nintendo 风格下落动画
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
  font-family: 'Press Start 2P', 'Courier New', monospace;
  font-size: clamp(28px, 6vw, 52px);
  font-weight: 400;
  color: var(--gb-text);
  letter-spacing: 2px;
  text-shadow:
    2px 2px 0 var(--gb-text-dim),
    -1px -1px 0 var(--gb-bg);
  opacity: 0;
  animation: charDrop 0.35s ease-out forwards;
}

/* 字符从天而降 */
@keyframes charDrop {
  0%   { opacity: 0; transform: translateY(-80px) scaleY(2); }
  60%  { opacity: 1; transform: translateY(4px) scaleY(0.9); }
  80%  { transform: translateY(-2px) scaleY(1.05); }
  100% { opacity: 1; transform: translateY(0) scaleY(1); }
}

/* 品牌分割线 */
.brand-line {
  width: 0;
  height: 4px;
  background: var(--gb-text);
  margin: 6px auto 14px;
  animation: lineExpand 0.5s 1s ease-out forwards;
  box-shadow: 0 1px 0 var(--gb-text-dim);
}

@keyframes lineExpand {
  0%   { width: 0; }
  100% { width: 140px; }
}

/* 品牌副标题 */
.brand-sub {
  font-family: 'Courier New', monospace;
  font-size: 14px;
  font-weight: bold;
  color: var(--gb-text-dim);
  letter-spacing: 6px;
  opacity: 0;
  animation: fadeIn 0.6s 1.3s ease-out forwards;
}

/* ============================================================
   开机信息
   ============================================================ */

.boot-info {
  margin-bottom: 40px;
  opacity: 0;
  animation: fadeIn 0.6s 1.5s ease-out forwards;
}

.boot-text {
  font-family: 'Courier New', monospace;
  font-size: 13px;
  color: var(--gb-text);
  letter-spacing: 1px;
  margin: 0 0 6px;
}

.blink-cursor {
  display: inline-block;
  animation: blink 1s step-end infinite;
  color: var(--gb-text);
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50%      { opacity: 0; }
}

.boot-version {
  font-family: 'Courier New', monospace;
  font-size: 10px;
  color: var(--gb-text-dim);
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
  opacity: 0;
  animation: fadeIn 0.6s 2s ease-out forwards;
  transition: transform 0.1s ease;
}

.start-btn:hover {
  transform: translate(1px, 1px);
}

.start-btn:active {
  transform: translate(3px, 3px);
}

.btn-frame {
  display: inline-block;
  padding: 14px 36px;
  background: var(--gb-bg-dark);
  border: 3px solid var(--gb-text);
  position: relative;
  box-shadow:
    4px 4px 0 var(--gb-text-dim),
    inset -2px -2px 0 rgba(15, 56, 15, 0.15),
    inset 2px 2px 0 rgba(255, 255, 255, 0.08);
}

.btn-text {
  font-family: 'Press Start 2P', 'Courier New', monospace;
  font-size: 14px;
  color: var(--gb-text);
  letter-spacing: 2px;
}

.btn-shadow {
  display: none;
}

/* ---- D-Pad 装饰 ---- */
.dpad-hint {
  margin-top: 48px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  opacity: 0;
  animation: fadeIn 0.6s 2.5s ease-out forwards;
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
  background: var(--gb-text);
  opacity: 0.2;
}

.dpad-h {
  position: absolute;
  left: 0;
  top: 12px;
  width: 40px;
  height: 16px;
  background: var(--gb-text);
  opacity: 0.2;
}

.dpad-label {
  font-family: 'Courier New', monospace;
  font-size: 9px;
  color: var(--gb-text-dim);
  letter-spacing: 3px;
}

/* ============================================================
   通用动画
   ============================================================ */

@keyframes fadeIn {
  0%   { opacity: 0; transform: translateY(8px); }
  100% { opacity: 1; transform: translateY(0); }
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
  font-family: 'Courier New', monospace;
  color: var(--gb-text-dim);
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
