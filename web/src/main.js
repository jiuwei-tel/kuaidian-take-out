import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

// 顺序不能反：先加载 Element Plus 样式，再用自己的令牌覆盖
import 'element-plus/dist/index.css'
import '@/styles/tokens.css'
import '@/styles/index.css'

import App from './App.vue'
import router from './router'

const app = createApp(App)

// 图标全量注册，模板里直接 <el-icon><Odometer /></el-icon>
for (const [name, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(name, component)
}

app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })

app.mount('#app')
