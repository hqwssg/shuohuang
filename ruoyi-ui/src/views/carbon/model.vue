<template>
  <div class="app-container carbon-model-page">
    <div class="carbon-model-toolbar">
      <div class="toolbar-title">
        <h2>碳排放模型设置</h2>
        <span>{{ resolvedModelUrl }}</span>
      </div>
      <div class="toolbar-actions">
        <el-button icon="el-icon-s-home" @click="$router.push('/carbon/index')">返回首页</el-button>
        <el-button icon="el-icon-refresh" @click="reloadFrame">刷新</el-button>
        <el-button type="primary" icon="el-icon-top-right" @click="openModel">新窗口打开</el-button>
      </div>
    </div>

    <div class="model-frame-wrap" v-loading="loading">
      <iframe
        :key="frameKey"
        class="model-frame"
        :src="modelFrameUrl"
        frameborder="0"
        @load="loading = false"
      ></iframe>
    </div>
  </div>
</template>

<script>
import { resolveServiceUrl } from '@/utils/serviceUrl'

export default {
  name: 'CarbonModel',
  data() {
    return {
      frameKey: 0,
      loading: true,
      modelUrl: process.env.VUE_APP_CARBON_MODEL_URL || '/carbon-model/'
    }
  },
  computed: {
    resolvedModelUrl() {
      return resolveServiceUrl(this.modelUrl, '/carbon-model/').replace(/\/$/, '')
    },
    modelFrameUrl() {
      const url = new URL(this.resolvedModelUrl)
      url.searchParams.set('embedded', '1')
      url.searchParams.set('view', 'model')
      return url.toString()
    }
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
    openModel() {
      window.open(this.modelFrameUrl, '_blank')
    }
  },
  mounted() {
    window.addEventListener('message', this.handleFrameMessage)
  },
  beforeDestroy() {
    window.removeEventListener('message', this.handleFrameMessage)
  }
}
</script>

<style scoped lang="scss">
.carbon-model-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 84px);
  min-height: 680px;
  padding: 16px;
  background: #f5f7fa;
  box-sizing: border-box;
}

.carbon-model-toolbar {
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
    font-weight: 700;
    letter-spacing: 0;
  }

  span {
    display: block;
    max-width: 62vw;
    overflow: hidden;
    color: #7a8797;
    font-size: 12px;
    line-height: 1.3;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.toolbar-actions {
  display: inline-flex;
  flex: 0 0 auto;
  gap: 8px;
}

.model-frame-wrap {
  flex: 1;
  min-height: 0;
  border: 1px solid #dfe6ee;
  border-radius: 6px;
  overflow: hidden;
  background: #fff;
}

.model-frame {
  display: block;
  width: 100%;
  height: 100%;
  min-height: 100%;
}

@media (max-width: 780px) {
  .carbon-model-page {
    height: calc(100dvh - 84px);
    min-height: 620px;
    padding: 10px;
  }

  .carbon-model-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .toolbar-title span {
    max-width: 100%;
  }

  .toolbar-actions {
    flex-wrap: wrap;

    .el-button {
      margin-left: 0;
    }
  }
}
</style>
