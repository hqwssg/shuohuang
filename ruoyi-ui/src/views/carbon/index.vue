<template>
  <div class="carbon-home">
    <div v-if="visibleModules.length" class="module-grid">
      <button
        v-for="item in visibleModules"
        :key="item.key"
        class="module-button"
        :class="item.color"
        type="button"
        @click="go(item)"
      >
        <i :class="item.icon"></i>
        <span>{{ item.title }}</span>
      </button>
    </div>
    <div v-else class="empty-home">
      <i class="el-icon-lock"></i>
      <span>暂无可访问功能</span>
    </div>
  </div>
</template>

<script>
import { carbonModules, canAccessByPermissions } from '@/utils/carbonAccess'

export default {
  name: 'CarbonIndex',
  computed: {
    visibleModules() {
      const permissions = this.$store.getters.permissions
      const roles = this.$store.getters.roles
      return carbonModules.filter(item => canAccessByPermissions(item, permissions, roles))
    }
  },
  methods: {
    go(item) {
      if (item.external) {
        window.location.href = item.path
        return
      }

      const target = item.routeName ? { name: item.routeName } : item.path
      this.$router.push(target)
    }
  }
}
</script>

<style scoped lang="scss">
.carbon-home {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  padding: 28px;
  background: #f5f7fa;
  box-sizing: border-box;
}

.module-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(180px, 240px));
  gap: 18px;
  width: min(100%, 1014px);
}

.module-button {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  min-height: 112px;
  padding: 18px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  background: #fff;
  color: #1f2d3d;
  cursor: pointer;
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 0;
  text-align: center;
  transition: border-color .2s, box-shadow .2s, transform .2s;

  i {
    flex: 0 0 auto;
    font-size: 26px;
  }

  span {
    min-width: 0;
    overflow-wrap: anywhere;
    line-height: 1.35;
  }

  &:hover {
    border-color: #95b8d8;
    box-shadow: 0 12px 26px rgba(31, 45, 61, .09);
    transform: translateY(-2px);
  }

  &.green i { color: #1f8f6b; }
  &.blue i { color: #2265b4; }
  &.orange i { color: #b4691f; }
  &.cyan i { color: #187f8d; }
  &.red i { color: #b84d4d; }
  &.gray i { color: #4a5568; }
  &.purple i { color: #7a4ca5; }
  &.violet i { color: #8a3f72; }
}

.empty-home {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  color: #8b97a8;
  font-size: 15px;
}

.empty-home i {
  font-size: 20px;
}

@media (max-width: 1040px) {
  .module-grid {
    grid-template-columns: repeat(2, minmax(150px, 1fr));
  }
}

@media (max-width: 540px) {
  .carbon-home {
    padding: 18px;
  }

  .module-grid {
    grid-template-columns: 1fr;
  }
}
</style>
