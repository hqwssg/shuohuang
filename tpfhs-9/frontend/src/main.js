import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import axios from 'axios'
import 'element-plus/dist/index.css'
import './style.css'
import RootApp from './RootApp.vue'

const token = document.cookie
  .split('; ')
  .find(item => item.startsWith('Admin-Token='))
  ?.split('=')
  .slice(1)
  .join('=')

if (token) axios.defaults.headers.common.Authorization = `Bearer ${decodeURIComponent(token)}`

const nativeFetch = window.fetch.bind(window)
window.fetch = (input, init = {}) => {
  const inheritedHeaders = input instanceof Request ? input.headers : undefined
  const headers = new Headers(init.headers || inheritedHeaders)
  if (token && !headers.has('Authorization')) {
    headers.set('Authorization', `Bearer ${decodeURIComponent(token)}`)
  }
  return nativeFetch(input, { ...init, headers })
}

const app = createApp(RootApp)
app.use(ElementPlus)
app.mount('#app')
