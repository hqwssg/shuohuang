<template>
  <div id="app-root">
    <Login v-if="currentView === 'login'" @login-success="handleLoginSuccess" />
    <HomePage 
      v-else-if="currentView === 'home'" 
      :current-user="currentUser"
      @navigate="handleNavigate"
      @logout="handleLogout"
    />
    <TemplateList
      v-else-if="currentView === 'template-list'"
      :current-user="currentUser"
      @open-template="handleOpenTemplate"
      @edit-factor-template="handleEditFactorTemplate"
      @back="handleBackToHome"
      @logout="handleLogout"
    />
    <CarbonParamsSettings
      v-else-if="currentView === 'params-settings'"
      :current-user="currentUser"
      :initial-factor-template-id="autoOpenFactorTemplateId"
      @back="handleBackToHome"
      @logout="handleLogout"
    />
    <App 
      v-else-if="currentView === 'design'" 
      :current-template="currentTemplate"
      :current-user="currentUser"
      @back="handleBackToTemplateList"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import Login from './components/Login.vue'
import HomePage from './components/HomePage.vue'
import TemplateList from './components/TemplateList.vue'
import CarbonParamsSettings from './components/CarbonParamsSettings.vue'
import App from './App.vue'

const currentView = ref('login')
const currentUser = ref(null)
const currentTemplate = ref(null)
// 从模版管理页跳转"编辑因子"时携带的因子模版ID（CarbonParamsSettings 据此自动打开对应因子模版）
const autoOpenFactorTemplateId = ref(null)
const embedded = ref(false)

onMounted(async () => {
  const params = new URLSearchParams(window.location.search)
  embedded.value = params.get('embedded') === '1'

  try {
    const { data } = await axios.get('/api/security/me')
    currentUser.value = data
    localStorage.setItem('user', JSON.stringify(data))
    currentView.value = params.get('view') === 'params' ? 'params-settings' : 'template-list'
    if (!embedded.value) currentView.value = 'home'
    return
  } catch (error) {
    if (embedded.value) {
      ElMessage.error(error?.response?.status === 403 ? '当前账号没有访问该模块的权限' : '系统登录状态已失效，请重新登录')
      return
    }
  }
  
  const savedUser = localStorage.getItem('user')
  if (savedUser) {
    try {
      currentUser.value = JSON.parse(savedUser)
      currentView.value = 'home'
      console.log('Found saved user, navigating to home')
    } catch (e) {
      console.error('Failed to parse saved user:', e)
      localStorage.removeItem('user')
    }
  }
})

const handleLoginSuccess = (user) => {
  console.log('Login success:', user)
  currentUser.value = user
  localStorage.setItem('user', JSON.stringify(user))
  currentView.value = 'home'
}

const handleNavigate = (view) => {
  console.log('Navigate to:', view)
  if (view === 'template-list') {
    currentView.value = 'template-list'
  } else if (view === 'params-settings') {
    currentView.value = 'params-settings'
  }
}

const handleOpenTemplate = (template) => {
  console.log('Open template:', template)
  currentTemplate.value = template
  currentView.value = 'design'
}

const handleBackToTemplateList = () => {
  console.log('Back to template list')
  currentTemplate.value = null
  currentView.value = 'template-list'
}

const handleBackToHome = () => {
  autoOpenFactorTemplateId.value = null
  if (embedded.value) {
    const parentOrigin = document.referrer ? new URL(document.referrer).origin : '*'
    window.parent.postMessage({ type: 'carbon:navigate-home' }, parentOrigin)
    return
  }
  currentView.value = 'home'
}

// 从核算模版编辑弹窗跳转编辑某个因子模版的因子配置
const handleEditFactorTemplate = (factorTemplateId) => {
  console.log('Edit factor template:', factorTemplateId)
  autoOpenFactorTemplateId.value = factorTemplateId
  currentView.value = 'params-settings'
}

const handleLogout = () => {
  console.log('Logout')
  localStorage.removeItem('user')
  currentUser.value = null
  currentTemplate.value = null
  currentView.value = 'login'
}
</script>
