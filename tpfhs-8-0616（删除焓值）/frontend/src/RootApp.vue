<template>
  <div id="app-root">
    <Login v-if="currentView === 'login'" @login-success="handleLoginSuccess" />
    <TemplateList 
      v-else-if="currentView === 'template-list'" 
      :current-user="currentUser"
      :embedded="isEmbedded"
      @open-template="handleOpenTemplate"
      @logout="handleLogout"
    />
    <App 
      v-else-if="currentView === 'design'" 
      :current-template="currentTemplate"
      @back="handleBackToTemplateList"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import Login from './components/Login.vue'
import TemplateList from './components/TemplateList.vue'
import App from './App.vue'

const currentView = ref('login')
const currentUser = ref(null)
const currentTemplate = ref(null)
const isEmbedded = ref(false)

const getEmbeddedUser = () => {
  const params = new URLSearchParams(window.location.search)
  if (params.get('embedded') !== '1') {
    return null
  }
  const userId = Number(params.get('userId')) || 1
  const userName = params.get('userName') || ''
  const name = params.get('name') || userName || '若依用户'
  return { userId, userName, name }
}

onMounted(() => {
  console.log('Initial currentView:', currentView.value)

  const embeddedUser = getEmbeddedUser()
  if (embeddedUser) {
    isEmbedded.value = true
    currentUser.value = embeddedUser
    localStorage.setItem('user', JSON.stringify(embeddedUser))
    currentView.value = 'template-list'
    console.log('Embedded mode, navigating to template-list')
    return
  }
  
  const savedUser = localStorage.getItem('user')
  if (savedUser) {
    try {
      currentUser.value = JSON.parse(savedUser)
      currentView.value = 'template-list'
      console.log('Found saved user, navigating to template-list')
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
  currentView.value = 'template-list'
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

const handleLogout = () => {
  if (isEmbedded.value) {
    currentTemplate.value = null
    currentView.value = 'template-list'
    return
  }
  console.log('Logout')
  localStorage.removeItem('user')
  currentUser.value = null
  currentTemplate.value = null
  currentView.value = 'login'
}
</script>
