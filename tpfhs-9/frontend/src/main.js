import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import axios from 'axios'
import 'element-plus/dist/index.css'
import './style.css'
import RootApp from './RootApp.vue'

const params = new URLSearchParams(window.location.search)
const userId = params.get('userId')
const userName = params.get('userName')

if (userId) axios.defaults.headers.common['X-User-Id'] = userId
if (userName) axios.defaults.headers.common['X-User-Name'] = encodeURIComponent(userName)

const nativeFetch = window.fetch.bind(window)
window.fetch = (input, init = {}) => {
  const inheritedHeaders = input instanceof Request ? input.headers : undefined
  const headers = new Headers(init.headers || inheritedHeaders)
  if (userId) headers.set('X-User-Id', userId)
  if (userName) headers.set('X-User-Name', encodeURIComponent(userName))
  return nativeFetch(input, { ...init, headers })
}

const app = createApp(RootApp)
app.use(ElementPlus)
app.mount('#app')
