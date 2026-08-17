<template>
  <suspense>
    <!-- 用来处理子树里的“异步组件 / async setup /
      依赖异步资源”的场景：当内部还有东西没准备好时，<Suspense> 会进入“pending”状态；准备好后再切到“resolved”状态。 -->
    <suspense-index></suspense-index>
    <!-- 这是真正承载预览逻辑的组件（文件 ./suspenseIndex.vue）。index.vue 只是把它挂进来。 -->
  </suspense>
</template>

<script setup lang="ts">
import suspenseIndex from './suspenseIndex.vue'
</script>
