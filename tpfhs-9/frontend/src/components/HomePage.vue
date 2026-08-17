<template>
  <div class="home-page">
    <div class="home-header">
      <div class="logo-area">
        <div class="logo-icon"></div>
        <span class="system-title">碳排放管理系统</span>
      </div>
      <div class="user-info">
        <span class="user-name">{{ currentUser?.nickName || currentUser?.userName }}</span>
        <el-button @click="$emit('logout')" type="text">退出登录</el-button>
      </div>
    </div>
    
    <div class="home-content">
      <div class="menu-card" @click="$emit('navigate', 'template-list')">
        <div class="menu-icon model-icon"></div>
        <h3 class="menu-title">碳排放模型管理</h3>
        <p class="menu-desc">管理碳排放核算模型的设计与配置</p>
      </div>
      
      <div class="menu-card" @click="$emit('navigate', 'params-settings')">
        <div class="menu-icon params-icon"></div>
        <h3 class="menu-title">碳排放核算参数设置</h3>
        <p class="menu-desc">设置碳排放核算相关参数</p>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  currentUser: {
    type: Object,
    required: true
  }
})

defineEmits(['navigate', 'logout'])
</script>

<style scoped>
.home-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.home-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 40px;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
}

.logo-area {
  display: flex;
  align-items: center;
  gap: 15px;
}

.logo-icon {
  width: 50px;
  height: 50px;
  background: white;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.system-title {
  font-size: 24px;
  font-weight: bold;
  color: white;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 20px;
  color: white;
}

.user-name {
  font-size: 16px;
}

.home-content {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 80px;
  padding: 40px;
}

.menu-card {
  width: 320px;
  height: 280px;
  background: white;
  border-radius: 16px;
  padding: 30px;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
}

.menu-card:hover {
  transform: translateY(-10px);
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.3);
}

.menu-icon {
  width: 80px;
  height: 80px;
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20px;
}

.model-icon {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.params-icon {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.menu-title {
  font-size: 22px;
  font-weight: bold;
  color: #333;
  margin-bottom: 10px;
}

.menu-desc {
  font-size: 14px;
  color: #999;
  line-height: 1.6;
}
</style>