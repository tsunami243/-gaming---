<template>
  <div class="message-view">
    <TopNav />
    <div class="message-content">
      <div class="message-header">
        <h3 class="list-title">消息中心</h3>
        <span class="list-count">共 {{ messageList.length }} 条</span>
      </div>
      <div class="message-list" v-if="messageList.length > 0">
        <div
          v-for="msg in messageList"
          :key="msg.id"
          class="message-card"
          :class="{ unread: !msg.isRead }"
          @click="handleClick(msg)"
        >
          <div class="message-icon" :style="{ backgroundColor: getTypeColor(msg.type) + '15' }">
            <n-icon :component="getTypeIcon(msg.type)" :style="{ color: getTypeColor(msg.type) }" size="20" />
          </div>
          <div class="message-body">
            <div class="message-title">{{ msg.title }}</div>
            <div class="message-desc">{{ msg.content }}</div>
            <div class="message-time">{{ formatTime(msg.createTime) }}</div>
          </div>
          <div class="unread-dot" v-if="!msg.isRead"></div>
        </div>
      </div>
      <div class="no-message" v-else>
        <n-empty description="暂无消息" />
      </div>
    </div>
    <BottomNav />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { NIcon, NEmpty } from 'naive-ui'
import { Chatbubbles, Heart, Medal, Flash } from '@vicons/ionicons5'
import TopNav from '../components/TopNav.vue'
import BottomNav from '../components/BottomNav.vue'

const messageList = ref([
  {
    id: 1,
    type: 'like',
    title: '有人点赞了你的文章',
    content: '《三角洲行动新版本攻略》收到 15 个赞',
    createTime: Date.now() - 3600000,
    isRead: false
  },
  {
    id: 2,
    type: 'comment',
    title: '收到新评论',
    content: '玩家小明评论了你的文章',
    createTime: Date.now() - 7200000,
    isRead: false
  },
  {
    id: 3,
    type: 'system',
    title: '系统通知',
    content: '欢迎来到游戏资讯平台，开始你的资讯之旅吧',
    createTime: Date.now() - 86400000,
    isRead: true
  }
])

const typeMap = {
  like: { icon: Heart, color: '#0F380F' },
  comment: { icon: Chatbubbles, color: '#306230' },
  system: { icon: Flash, color: '#5A8A0F' },
  activity: { icon: Medal, color: '#8BAC0F' }
}

const getTypeIcon = (type) => typeMap[type]?.icon || Flash
const getTypeColor = (type) => typeMap[type]?.color || '#306230'

const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now - date
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  return `${Math.floor(diff / 86400000)}天前`
}

const handleClick = (msg) => {
  msg.isRead = true
}
</script>

<style scoped>
.message-view {
  min-height: 100vh;
  background-color: var(--gb-lightest);
  padding-bottom: 80px;
}

.message-content {
  max-width: 800px;
  margin: 0 auto;
  padding: 16px;
  padding-top: 70px;
}

.message-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.list-title {
  font-family: var(--font-zh);
  font-size: 20px;
  color: var(--gb-darkest);
  letter-spacing: 2px;
}

.list-count {
  font-family: var(--font-body);
  font-size: 13px;
  color: var(--gb-dark);
}

.message-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.message-card {
  display: flex;
  gap: 14px;
  padding: 16px;
  cursor: pointer;
  position: relative;
  background: var(--gb-light);
  border: 3px solid var(--gb-darkest);
  box-shadow: 4px 4px 0 var(--gb-dark);
  transition: transform 0.08s steps(2), box-shadow 0.08s steps(2);
}

.message-card:hover {
  transform: translate(-1px, -1px);
  box-shadow: 5px 5px 0 var(--gb-darkest);
}

.message-card.unread {
  border-left: 6px solid var(--gb-darkest);
}

.message-icon {
  flex-shrink: 0;
  width: 44px;
  height: 44px;
  border: 2px solid var(--gb-darkest);
  display: flex;
  align-items: center;
  justify-content: center;
}

.message-body {
  flex: 1;
  min-width: 0;
}

.message-title {
  font-family: var(--font-zh);
  font-size: 15px;
  color: var(--gb-darkest);
  margin-bottom: 4px;
}

.message-desc {
  font-family: var(--font-body);
  font-size: 13px;
  color: var(--gb-dark);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.message-time {
  font-family: var(--font-body);
  font-size: 11px;
  color: var(--gb-dark);
  margin-top: 6px;
}

.unread-dot {
  position: absolute;
  top: 16px;
  right: 16px;
  width: 8px;
  height: 8px;
  background: var(--gb-darkest);
}

.no-message {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 60px 0;
}
</style>
