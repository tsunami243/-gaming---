<template>
  <div class="page">
    <div class="pixel-bg"></div>
    <div class="card">
      <h1 class="title">GAMEBOI</h1>
      <p class="subtitle">玩家登录</p>
      <div class="form">
        <label class="field">
          <span class="field-label">USERNAME</span>
          <input v-model="formData.username" class="input" placeholder="用户名" />
        </label>
        <label class="field">
          <span class="field-label">PASSWORD</span>
          <input v-model="formData.password" class="input" type="password" placeholder="密码" />
        </label>
        <p v-if="errorMsg" class="error">{{ errorMsg }}</p>
        <button class="btn" @click="handleLogin" :disabled="loading">
          {{ loading ? '...' : '▶ LOGIN' }}
        </button>
      </div>
      <p class="footer">
        <router-link to="/register">注册新账号 →</router-link>
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '../stores/user'
const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const loading = ref(false)
const errorMsg = ref('')
const formData = reactive({ username: '', password: '' })

const handleLogin = async () => {
  errorMsg.value = ''
  if (!formData.username || !formData.password) {
    errorMsg.value = '请输入用户名和密码'
    return
  }
  loading.value = true
  const result = await userStore.loginAction(formData.username, formData.password)
  loading.value = false
  if (result.ok) {
    const redirect = route.query.redirect || '/home'
    router.push(redirect)
  } else {
    errorMsg.value = '✕ ' + result.msg
  }
}
</script>

<style scoped>
.page { min-height:100vh; display:flex; align-items:center; justify-content:center; background:#fff; position:relative; }
.pixel-bg { position:fixed; inset:0; pointer-events:none; z-index:0; background-image:radial-gradient(circle,#ddd 1px,transparent 1px); background-size:8px 8px; }
.card { position:relative; z-index:1; width:100%; max-width:360px; background:#fff; border:3px solid #111; padding:40px 30px; text-align:center; }
.title { font-family:'Press Start 2P',monospace; font-size:18px; color:#111; margin:0 0 6px; letter-spacing:3px; }
.subtitle { font-family:'Courier New',monospace; font-size:14px; color:#888; margin:0 0 28px; }
.field { display:block; text-align:left; margin-bottom:16px; }
.field-label { display:block; font-family:'Press Start 2P',monospace; font-size:8px; color:#999; margin-bottom:6px; letter-spacing:1px; }
.input { width:100%; padding:12px 14px; border:3px solid #ddd; font-family:'Courier New',monospace; font-size:15px; box-sizing:border-box; outline:none; transition:border .1s; }
.input:focus { border-color:#111; }
.btn { width:100%; padding:14px; margin-top:10px; background:#111; border:3px solid #111; color:#fff; font-family:'Press Start 2P',monospace; font-size:11px; cursor:pointer; transition:all .08s; letter-spacing:2px; }
.btn:hover { background:#333; }
.btn:active { transform:translateY(2px); }
.btn:disabled { opacity:0.5; }
.error { margin:10px 0 0; padding:10px; background:#FFF5F5; border:2px solid #DC2626; font-family:'Courier New',monospace; font-size:13px; color:#DC2626; text-align:center; }
.footer { margin-top:20px; font-family:'Courier New',monospace; font-size:13px; }
.footer a { color:#111; }
</style>
