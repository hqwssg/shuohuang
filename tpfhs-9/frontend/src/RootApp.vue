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
import Login from './components/Login.vue'
import HomePage from './components/HomePage.vue'
import TemplateList from './components/TemplateList.vue'
import App from './App.vue'

const currentView = ref('login')
const currentUser = ref(null)
const currentTemplate = ref(null)

onMounted(() => {
  console.log('Initial currentView:', currentView.value)
  
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

const handleLogout = () => {
  console.log('Logout')
  localStorage.removeItem('user')
  currentUser.value = null
  currentTemplate.value = null
  currentView.value = 'login'
}
</script>