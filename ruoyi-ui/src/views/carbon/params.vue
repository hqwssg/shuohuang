<template>
  <div class="app-container carbon-params-page">
    <div class="carbon-toolbar">
      <div class="toolbar-title">
        <h2>碳排放核算参数设置</h2>
        <span>维护排放因子、采集点、能源分类、单位与换算规则</span>
      </div>
      <div class="toolbar-actions">
        <el-button icon="el-icon-s-home" @click="$router.push('/carbon/index')">返回首页</el-button>
        <el-button icon="el-icon-refresh" @click="reloadFrame">刷新</el-button>
        <el-button type="primary" icon="el-icon-top-right" @click="openParams">新窗口打开</el-button>
      </div>
    </div>

    <div class="params-frame-wrap" v-loading="loading">
      <iframe
        :key="frameKey"
        class="params-frame"
        :src="paramsFrameUrl"
        frameborder="0"
        title="碳排放核算参数设置"
        @load="loading = false"
      ></iframe>
    </div>
  </div>
</template>

<script>
import { resolveServiceUrl } from '@/utils/serviceUrl'

export default {
  name: 'CarbonParams',
  data() {
    return {
      frameKey: 0,
      loading: true,
      modelUrl: process.env.VUE_APP_CARBON_MODEL_URL || '/carbon-model/'
    }
  },
  computed: {
    paramsFrameUrl() {
      const url = new URL(resolveServiceUrl(this.modelUrl, '/carbon-model/'))
      url.searchParams.set('embedded', '1')
      url.searchParams.set('view', 'params')
      return url.toString()
    }
  },
  mounted() {
    window.addEventListener('message', this.handleFrameMessage)
  },
  beforeDestroy() {
    window.removeEventListener('message', this.handleFrameMessage)
  },
  methods: {
    handleFrameMessage(event) {
      if (event.data && event.data.type === 'carbon:navigate-home') {
        this.$router.push('/carbon/index')
      }
    },
    reloadFrame() {
      this.loading = true
      this.frameKey += 1
    },
    openParams() {
      window.open(this.paramsFrameUrl, '_blank')
    }
  }
}
</script>

<style scoped lang="scss">
.carbon-params-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 84px);
  min-height: 680px;
  padding: 16px;
  background: #f5f7fa;
  box-sizing: border-box;
}

.carbon-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
  padding: 14px 16px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  background: #fff;
}

.toolbar-title {
  min-width: 0;

  h2 {
    margin: 0 0 5px;
    color: #1f2d3d;
    font-size: 18px;
    letter-spacing: 0;
  }

  span {
    color: #7a8797;
    font-size: 13px;
    line-height: 1.4;
  }
}

.toolbar-actions {
  display: inline-flex;
  flex: 0 0 auto;
  gap: 8px;
}

.params-frame-wrap {
  flex: 1;
  min-height: 0;
  overflow: hidden;
  border: 1px solid #dfe6ee;
  border-radius: 6px;
  background: #fff;
}

.params-frame {
  display: block;
  width: 100%;
  height: 100%;
  min-height: 100%;
}

@media (max-width: 780px) {
  .carbon-params-page { height: calc(100dvh - 84px); min-height: 620px; padding: 10px; }
  .carbon-toolbar { align-items: stretch; flex-direction: column; }
  .toolbar-actions { flex-wrap: wrap; }
  .toolbar-actions .el-button { margin-left: 0; }
}
</style>
