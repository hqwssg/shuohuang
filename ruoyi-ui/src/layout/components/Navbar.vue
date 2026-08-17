<template>
  <div class="navbar">
    <button type="button" class="home-entry" @click="$router.push('/')">
      <i class="el-icon-s-home"></i>
      <span>碳排放管理系统</span>
    </button>

    <el-dropdown class="avatar-container" trigger="hover">
      <div class="avatar-wrapper">
        <img :src="avatar" class="user-avatar">
        <span class="user-nickname">{{ nickName || name }}</span>
        <i class="el-icon-arrow-down"></i>
      </div>
      <el-dropdown-menu slot="dropdown">
        <router-link to="/user/profile">
          <el-dropdown-item>个人中心</el-dropdown-item>
        </router-link>
        <el-dropdown-item divided @click.native="logout">
          <span>退出登录</span>
        </el-dropdown-item>
      </el-dropdown-menu>
    </el-dropdown>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'

export default {
  computed: {
    ...mapGetters([
      'avatar',
      'name',
      'nickName'
    ])
  },
  methods: {
    logout() {
      this.$confirm('确定注销并退出系统吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.$store.dispatch('LogOut').then(() => {
          location.href = '/login'
        })
      }).catch(() => {})
    }
  }
}
</script>

<style lang="scss" scoped>
.navbar {
  display: flex;
  align-items: center;
  height: 50px;
  padding: 0 18px;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 21, 41, .08);
  box-sizing: border-box;
}

.home-entry {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 34px;
  padding: 0;
  border: 0;
  background: transparent;
  color: #1f2d3d;
  cursor: pointer;
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 0;

  i {
    color: #1f8f6b;
    font-size: 19px;
  }
}

.avatar-container {
  margin-left: auto;
  cursor: pointer;
}

.avatar-wrapper {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #1f2d3d;
}

.user-avatar {
  width: 30px;
  height: 30px;
  border-radius: 50%;
}

.user-nickname {
  max-width: 160px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
  font-weight: 700;
}

@media (max-width: 640px) {
  .navbar {
    padding: 0 12px;
  }

  .home-entry span {
    display: none;
  }

  .user-nickname {
    max-width: 96px;
  }
}
</style>
