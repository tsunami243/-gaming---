<template>
  <div class="register-view">
    <div class="register-header">
      <h1>游戏资讯</h1>
      <p>注册新账号</p>
    </div>
    <div class="register-form">
      <n-form ref="formRef" :model="formData" :rules="rules">
        <n-form-item path="username">
          <n-input v-model:value="formData.username" placeholder="用户名（5-16位）" />
        </n-form-item>
        <n-form-item path="password">
          <n-input
            v-model:value="formData.password"
            type="password"
            placeholder="密码（5-16位）"
            show-password-on="click"
          />
        </n-form-item>
        <n-form-item>
          <n-button type="primary" block @click="handleRegister" :loading="loading">
            注册
          </n-button>
        </n-form-item>
      </n-form>
      <div class="register-footer">
        <span>已有账号？</span>
        <router-link to="/login">立即登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { NForm, NFormItem, NInput, NButton, useMessage } from 'naive-ui'
import { useUserStore } from '../stores/user'

const router = useRouter()
const message = useMessage()
const userStore = useUserStore()
const formRef = ref(null)
const loading = ref(false)

const formData = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { pattern: /^.{5,16}$/, message: '用户名长度需为5-16位', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { pattern: /^.{5,16}$/, message: '密码长度需为5-16位', trigger: 'blur' }
  ]
}

const handleRegister = async () => {
  try {
    await formRef.value?.validate()
    loading.value = true
    const res = await userStore.registerAction(formData.username, formData.password)
    if (res.code === 0) {
      message.success('注册成功，请登录')
      router.push('/login')
    } else {
      message.error(res.msg || '注册失败')
    }
  } catch (error) {
    console.error('注册失败', error)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-view {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: var(--apple-bg);
  padding: 20px;
}

.register-header {
  text-align: center;
  margin-bottom: 40px;
}

.register-header h1 {
  font-size: 34px;
  font-weight: 700;
  margin-bottom: 8px;
  color: var(--apple-text-primary);
  letter-spacing: -0.5px;
}

.register-header p {
  font-size: 16px;
  color: var(--apple-text-secondary);
}

.register-form {
  width: 100%;
  max-width: 360px;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  padding: 30px;
  border-radius: var(--apple-radius-xl);
  border: 1px solid rgba(255, 255, 255, 0.5);
  box-shadow: var(--apple-shadow-lg);
}

.register-footer {
  text-align: center;
  margin-top: 16px;
  font-size: 14px;
  color: var(--apple-text-secondary);
}

.register-footer a {
  color: var(--apple-blue);
  font-weight: 600;
}
</style>
