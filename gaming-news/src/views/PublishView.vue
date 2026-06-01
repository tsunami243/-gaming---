<template>
  <div class="page">
    <div class="pixel-bg"></div>
    <div class="card">
      <h1 class="title">发布文章</h1>
      <div class="form">
        <label class="field">
          <span class="field-label">TITLE</span>
          <input v-model="formData.title" class="input" placeholder="文章标题" />
        </label>
        <label class="field">
          <span class="field-label">CATEGORY</span>
          <select v-model="formData.categoryId" class="input select">
            <option :value="null" disabled>选择分类</option>
            <option v-for="c in categoryStore.items" :key="c.id" :value="c.id">{{ c.categoryName }}</option>
          </select>
        </label>
        <label class="field">
          <span class="field-label">CONTENT</span>
          <textarea v-model="formData.content" class="input textarea" placeholder="文章内容..." rows="8"></textarea>
        </label>
        <p v-if="msg.text" class="msg" :class="msg.type">{{ msg.text }}</p>
        <button class="btn" @click="handleSubmit" :disabled="submitting">
          {{ submitting ? '...' : '▶ PUBLISH' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { addArticle } from '../api/article'
import { useCategoryStore } from '../stores/category'
const router = useRouter()
const categoryStore = useCategoryStore()
const submitting = ref(false)
const msg = reactive({ text: '', type: 'error' })
const formData = reactive({ title: '', categoryId: null, content: '' })

const handleSubmit = async () => {
  msg.text = ''
  if (!formData.title || !formData.categoryId || !formData.content) {
    msg.text = '请填写标题、分类和内容'
    return
  }
  submitting.value = true
  try {
    const res = await addArticle(formData)
    if (res.code === 0) {
      msg.text = '✓ 发布成功'
      msg.type = 'success'
      setTimeout(() => router.push('/home'), 800)
    } else {
      msg.text = '✕ ' + (res.message || '发布失败')
      msg.type = 'error'
    }
  } catch {
    msg.text = '✕ 网络错误'
    msg.type = 'error'
  } finally { submitting.value = false }
}

onMounted(() => categoryStore.fetchCategories())
</script>

<style scoped>
.page { min-height:100vh; display:flex; align-items:flex-start; justify-content:center; background:#fff; position:relative; padding-top:40px; }
.pixel-bg { position:fixed; inset:0; pointer-events:none; z-index:0; background-image:radial-gradient(circle,#ddd 1px,transparent 1px); background-size:8px 8px; }
.card { position:relative; z-index:1; width:100%; max-width:560px; background:#fff; border:3px solid #111; padding:32px 28px; }
.title { font-family:'Press Start 2P',monospace; font-size:14px; color:#111; margin:0 0 24px; text-align:center; letter-spacing:3px; }
.field { display:block; margin-bottom:16px; }
.field-label { display:block; font-family:'Press Start 2P',monospace; font-size:8px; color:#999; margin-bottom:6px; letter-spacing:1px; }
.input { width:100%; padding:12px 14px; border:3px solid #ddd; font-family:'Courier New',monospace; font-size:14px; box-sizing:border-box; outline:none; transition:border .1s; }
.input:focus { border-color:#111; }
.select { background:#fff; cursor:pointer; }
.textarea { resize:vertical; }
.btn { width:100%; padding:14px; margin-top:6px; background:#111; border:3px solid #111; color:#fff; font-family:'Press Start 2P',monospace; font-size:11px; cursor:pointer; transition:all .08s; letter-spacing:2px; }
.btn:hover { background:#333; }
.btn:active { transform:translateY(2px); }
.msg { margin:10px 0 0; padding:10px; font-family:'Courier New',monospace; font-size:13px; text-align:center; }
.msg.error { background:#FFF5F5; border:2px solid #DC2626; color:#DC2626; }
.msg.success { background:#F0FFF4; border:2px solid #16A34A; color:#16A34A; }
.btn:disabled { opacity:.5; }
</style>
