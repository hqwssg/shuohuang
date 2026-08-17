<template>
  <div class="login-container">
    <div class="login-box">
      <div class="logo-section">
        <div class="logo"></div>
        <h2>碳排放核算系统</h2>
      </div>
      
      <form class="login-form" @submit.prevent="handleLogin">
        <div class="form-item">
          <label for="username">用户名</label>
          <input 
            id="username"
            v-model="username" 
            type="text" 
            placeholder="请输入用户名"
            class="login-input"
          />
        </div>
        
        <div class="form-item">
          <label for="password">密码</label>
          <input 
            id="password"
            v-model="password" 
            type="password" 
            placeholder="请输入密码"
            class="login-input"
          />
        </div>
        
        <div class="form-item">
          <button type="submit" class="login-btn">登录</button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { authApi } from '../api/auth'

const emit = defineEmits(['login-success'])

const username = ref('')
const password = ref('')

console.log('Login component loaded')

const handleLogin = () => {
  console.log('Login clicked:', username.value, password.value)
  
  if (!username.value || !password.value) {
    alert('请输入用户名和密码')
    return
  }
  
  authApi.login(username.value, password.value).then(res => {
    console.log('Login success:', res.data)
    localStorage.setItem('user', JSON.stringify(res.data))
    emit('login-success', res.data)
  }).catch(err => {
    console.error('Login error:', err)
    alert(err.response?.data?.message || '登录失败')
  })
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.login-box {
  background: #fff;
  padding: 40px;
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  width: 100%;
  max-width: 400px;
}

.logo-section {
  text-align: center;
  margin-bottom: 30px;
}

.logo {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  margin: 0 auto 20px;
}

h2 {
  font-size: 24px;
  color: #1f2329;
  margin: 0;
}

.login-form {
  margin-top: 20px;
}

.form-item {
  margin-bottom: 20px;
}

.form-item label {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  color: #595959;
}

.login-input {
  width: 100%;
  height: 40px;
  padding: 0 12px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  font-size: 14px;
  transition: border-color 0.2s;
  box-sizing: border-box;
}

.login-input:focus {
  outline: none;
  border-color: #1890ff;
}

.login-input::placeholder {
  color: #bfbfbf;
}

.login-btn {
  width: 100%;
  height: 40px;
  background-color: #1890ff;
  color: #fff;
  border: none;
  border-radius: 4px;
  font-size: 16px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.login-btn:hover {
  background-color: #40a9ff;
}

.login-btn:active {
  background-color: #096dd9;
}
</style>