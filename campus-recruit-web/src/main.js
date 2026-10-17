import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/es/locale/lang/zh-cn'

import App from './App.vue'
import router from './router'

// 全局设计 Token + 通用样式（颜色变量/阴影/圆角等在这里）
import './assets/styles/global.css'

// 前端入口文件。
// 人话讲解：
// - createApp(App) 创建 Vue 应用，根组件是 App.vue。
// - use(ElementPlus) 注册 UI 组件库（这样模板里能直接用 <el-button> 等标签）。
//   locale 设置成中文，Element Plus 的日期/分页等文字就是中文。
// - use(createPinia()) 注册状态管理（存登录 token、用户信息，多个页面共享）。
// - use(router) 注册路由（不同 URL 显示不同页面）。
// - mount('#app') 把应用挂到 index.html 里的 <div id="app">。
const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })

app.mount('#app')
