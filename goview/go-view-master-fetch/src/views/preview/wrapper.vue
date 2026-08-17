<template>
  <preview :key="key"></preview>
</template>

<!-- <preview>：对应脚本里 import Preview from './index.vue'
  （Vue 会把 Preview 自动映射为 <preview> 标签可用）。 -->

    <!-- :key="key"：这是关键点。
在 Vue 里，当组件的 key 改变时，框架会把旧组件当成“不是同一个实例”，从而卸载旧实例、重新创建新实例。
这里就是用它来实现“数据一变就重载整个预览组件”。 -->

<script setup lang="ts">
import { getSessionStorageInfo } from './utils'
import type { ChartEditStorageType } from './index.d'
import { SavePageEnum } from '@/enums/editPageEnum'
import { setSessionStorage } from '@/utils'
import { StorageEnum } from '@/enums/storageEnum'
import { ref } from 'vue'
import Preview from './index.vue'
// 真正的预览主体组件。

let key = ref(Date.now())
// 初始化 key 为当前时间戳。
// 之后每次数据更新时再 key.value = Date.now()，就会触发 <preview> 组件重建。

// 数据变更 -> 组件销毁重建
try {
  const listenerArr = [SavePageEnum.JSON, SavePageEnum.CHART_TO_PREVIEW]
  listenerArr.forEach((saveEvent: string) => {
    if (!window.opener || !window.opener.addEventListener) return
    window.opener.addEventListener(saveEvent, async (e: any) => {
      const localStorageInfo: ChartEditStorageType = (await getSessionStorageInfo()) as unknown as ChartEditStorageType
      setSessionStorage(StorageEnum.GO_CHART_STORAGE_LIST, [{ ...e.detail, id: localStorageInfo.id }])
      key.value = Date.now()
    })
  })
} catch (error) {
  console.log(error)
}
</script>
