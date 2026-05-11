<template>
  <div class="login-view">
    <div class="login-header">
      <h1>游戏资讯</h1>
      <p>登录您的账号</p>
    </div>
    <div class="login-form">
      <n-form ref="formRef" :model="formData" :rules="rules">
        <n-form-item path="username">
          <n-input v-model:value="formData.username" placeholder="用户名" />
        </n-form-item>
        <n-form-item path="password">
          <n-input
            v-model:value="formData.password"
            type="password"
            placeholder="密码"
            show-password-on="click"
          />
        </n-form-item>
        <n-form-item>
          <n-button type="primary" block @click="handleLogin" :loading="loading">
            登录
          </n-button>
        </n-form-item>
      </n-form>
      <div class="login-footer">
        <span>还没有账号？</span>
        <router-link to="/register">立即注册</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { NForm, NFormItem, NInput, NButton, useMessage, useDialog } from 'naive-ui'
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
  username: { required: true, message: '请输入用户名', trigger: 'blur' },
  password: { required: true, message: '请输入密码', trigger: 'blur' }
}

const handleLogin = async () => {
  try {
    await formRef.value?.validate()
    loading.value = true
    const success = await userStore.loginAction(formData.username, formData.password)
    if (success) {
      message.success('登录成功')
      router.push('/')
    } else {
      message.error('用户名或密码错误')
    }
  } catch (error) {
    console.error('登录失败', error)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-view {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: var(--apple-bg);
  padding: 20px;
}

.login-header {
  text-align: center;
  margin-bottom: 40px;
}

.login-header h1 {
  font-size: 34px;
  font-weight: 700;
  margin-bottom: 8px;
  color: var(--apple-text-primary);
  letter-spacing: -0.5px;
}

.login-header p {
  font-size: 16px;
  color: var(--apple-text-secondary);
}

.login-form {
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

.login-footer {
  text-align: center;
  margin-top: 16px;
  font-size: 14px;
  color: var(--apple-text-secondary);
}

.login-footer a {
  color: var(--apple-blue);
  font-weight: 600;
}
</style>
