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
.page { min-height:100vh; display:flex; align-items:flex-start; justify-content:center; background:var(--gb-lightest); position:relative; padding-top:40px; }
.card { position:relative; z-index:1; width:100%; max-width:560px; background:var(--gb-light); border:3px solid var(--gb-darkest); box-shadow:6px 6px 0 var(--gb-dark); padding:32px 28px; }
.title { font-family:var(--font-zh); font-size:18px; color:var(--gb-darkest); margin:0 0 24px; text-align:center; letter-spacing:3px; }
.field { display:block; margin-bottom:16px; }
.field-label { display:block; font-family:var(--font-pixel); font-size:8px; color:var(--gb-dark); margin-bottom:6px; letter-spacing:1px; }
.input { width:100%; padding:12px 14px; border:3px solid var(--gb-dark); background:var(--gb-lightest); color:var(--gb-darkest); font-family:var(--font-body); font-size:14px; box-sizing:border-box; outline:none; transition:border .1s steps(2); }
.input:focus { border-color:var(--gb-darkest); }
.select { cursor:pointer; }
.textarea { resize:vertical; }
.btn { width:100%; padding:14px; margin-top:6px; background:var(--gb-darkest); border:3px solid var(--gb-darkest); color:var(--gb-lightest); font-family:var(--font-pixel); font-size:11px; cursor:pointer; transition:all .08s steps(2); letter-spacing:2px; }
.btn:hover { background:var(--gb-dark); }
.btn:active { transform:translateY(2px); }
.msg { margin:10px 0 0; padding:10px; font-family:var(--font-body); font-size:13px; text-align:center; }
.msg.error { background:var(--gb-lightest); border:2px solid var(--gb-darkest); color:var(--gb-darkest); }
.msg.success { background:var(--gb-darkest); border:2px solid var(--gb-darkest); color:var(--gb-lightest); }
.btn:disabled { opacity:.5; }
</style>
