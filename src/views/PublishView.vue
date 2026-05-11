<template>
  <div class="publish-view">
    <TopNav title="发布文章" />
    <div class="publish-content">
      <div class="publish-form">
        <n-form ref="formRef" :model="formData" :rules="rules">
          <n-form-item label="标题" path="title">
            <n-input v-model:value="formData.title" placeholder="请输入文章标题" />
          </n-form-item>
          <n-form-item label="分类" path="categoryId">
            <n-select
              v-model:value="formData.categoryId"
              :options="categoryOptions"
              placeholder="请选择分类"
            />
          </n-form-item>
          <n-form-item label="内容" path="content">
            <n-input
              v-model:value="formData.content"
              type="textarea"
              placeholder="请输入文章内容"
              :rows="10"
            />
          </n-form-item>
          <n-form-item>
            <n-button type="primary" @click="handleSubmit" :loading="submitting">
              发布
            </n-button>
          </n-form-item>
        </n-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { NForm, NFormItem, NInput, NSelect, NButton, useMessage } from 'naive-ui'
import TopNav from '../components/TopNav.vue'
import { addArticle } from '../api/article'
import { categoryOptions } from '../utils/category'

const router = useRouter()
const message = useMessage()
const formRef = ref(null)
const submitting = ref(false)

const formData = reactive({
  title: '',
  categoryId: null,
  content: ''
})

const rules = {
  title: { required: true, message: '请输入标题', trigger: 'blur' },
  categoryId: { required: true, type: 'number', message: '请选择分类', trigger: 'change' },
  content: { required: true, message: '请输入内容', trigger: 'blur' }
}

const handleSubmit = async () => {
  try {
    await formRef.value?.validate()
    submitting.value = true
    const res = await addArticle(formData)
    if (res.code === 0) {
      message.success('发布成功')
      router.push('/')
    }
  } catch (error) {
    console.error('表单校验失败', error)
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.publish-view {
  min-height: 100vh;
  background-color: var(--apple-bg);
  padding-bottom: 60px;
}

.publish-content {
  display: flex;
  justify-content: center;
  padding: 70px 16px 16px;
}

.publish-form {
  width: 100%;
  max-width: 560px;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  padding: 30px;
  border-radius: var(--apple-radius-xl);
  border: 1px solid rgba(255, 255, 255, 0.5);
  box-shadow: var(--apple-shadow-lg);
}
</style>
