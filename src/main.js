import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { createPinia } from 'pinia'
import naive from 'naive-ui'
import './assets/styles/global.css'
import './assets/styles/apple.css'

const app = createApp(App)
app.use(router)
app.use(createPinia())
app.use(naive)
app.mount('#app')
