import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import './style.css'
import RootApp from './RootApp.vue'

console.log('main.js loaded')

const app = createApp(RootApp)
app.use(ElementPlus)
app.mount('#app')