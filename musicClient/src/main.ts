import { createApp } from 'vue'
import App from './App.vue'
import router from './routers/index'
import Store from '@/stores'
import 'element-plus/theme-chalk/dark/css-vars.css'
import './style/index.scss'
const app = createApp(App)

// 路由
app.use(router)
// 状态管理
app.use(Store)
app.mount('#app')
