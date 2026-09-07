<!--
  数据字典设置页面组件
  功能描述：
    1. 顶部显示字典分类下拉框和操作按钮（编辑、新增、删除）
    2. 下方显示选中字典的字典项列表，支持新增、编辑、删除、移动、启用/禁用
    3. 字典项支持层级关系（通过 parent_code 实现）
-->
<template>
  <div class="dict-settings">
    <!-- 上部：字典分类选择 -->
    <div class="dict-selector-panel">
      <div class="selector-label">字典分类选择</div>
      <div class="selector-row">
        <el-select
          v-model="selectedDictId"
          placeholder="请选择字典分类"
          style="flex: 1; margin-right: 10px;"
          @change="handleDictChange"
        >
          <el-option
            v-for="dict in dictList"
            :key="dict.id"
            :label="formatDictLabel(dict)"
            :value="dict.id"
          />
        </el-select>
        <el-button type="primary" @click="openAddDictDialog">新增</el-button>
        <el-button
          :disabled="!selectedDict"
          @click="selectedDict && openEditDictDialog(selectedDict)"
        >
          编辑
        </el-button>
        <el-button
          :disabled="!selectedDict"
          type="danger"
          @click="selectedDict && confirmDeleteDict(selectedDict)"
        >
          删除
        </el-button>
      </div>
    </div>
    
    <!-- 下部：字典项列表 -->
    <div class="dict-item-panel">
      <div class="panel-header">
        <span>字典项列表</span>
        <div class="header-actions">
          <el-button
            :disabled="!selectedDict"
            type="primary"
            size="small"
            @click="openAddItemDialog"
          >
            新增
          </el-button>
        </div>
      </div>
      <div class="panel-body" ref="panelBodyRef">
        <template v-if="selectedDict">
          <el-table
            :data="itemList"
            border
            stripe
            :height="tableHeight"
            style="width: 100%"
          >
            <el-table-column prop="code" label="字典项编码" width="180" />
            <el-table-column prop="value" label="字典项值" min-width="180" />
            <el-table-column prop="parentCode" label="父级编码" width="150">
              <template #default="{ row }">
                {{ row.parentCode || '-' }}
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
                  {{ row.status === 1 ? '启用' : '禁用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="130" fixed="right">
              <template #default="{ row }">
                <div class="action-buttons">
                  <el-tooltip content="编辑" placement="top">
                    <el-button
                      link
                      type="primary"
                      size="small"
                      @click="openEditItemDialog(row)"
                    >
                      <el-icon><Edit /></el-icon>
                    </el-button>
                  </el-tooltip>
                  <el-tooltip content="删除" placement="top">
                    <el-button
                      link
                      type="danger"
                      size="small"
                      @click="confirmDeleteItem(row)"
                    >
                      <el-icon><Delete /></el-icon>
                    </el-button>
                  </el-tooltip>
                  <el-tooltip content="上移" placement="top">
                    <el-button
                      link
                      size="small"
                      :disabled="isFirstItem(row)"
                      @click="moveItemUp(row)"
                    >
                      <el-icon><ArrowUp /></el-icon>
                    </el-button>
                  </el-tooltip>
                  <el-tooltip content="下移" placement="top">
                    <el-button
                      link
                      size="small"
                      :disabled="isLastItem(row)"
                      @click="moveItemDown(row)"
                    >
                      <el-icon><ArrowDown /></el-icon>
                    </el-button>
                  </el-tooltip>
                  <el-tooltip :content="row.status === 1 ? '停用' : '启用'" placement="top">
                    <el-button
                      link
                      :type="row.status === 1 ? 'warning' : 'success'"
                      size="small"
                      @click="toggleItemStatus(row)"
                    >
                      <el-icon v-if="row.status === 1"><CircleClose /></el-icon>
                      <el-icon v-else><CircleCheck /></el-icon>
                    </el-button>
                  </el-tooltip>
                </div>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="itemList.length === 0" description="该字典下暂无字典项" />
        </template>
        <el-empty v-else description="请在上方选择一个字典分类" />
      </div>
    </div>
    
    <!-- 新增/编辑字典弹窗 -->
    <el-dialog
      v-model="dictDialogVisible"
      :title="dictForm.id ? '编辑字典' : '新增字典'"
      width="500px"
      @close="resetDictForm"
    >
      <el-form
        ref="dictFormRef"
        :model="dictForm"
        :rules="dictRules"
        label-width="100px"
      >
        <el-form-item label="字典编码" prop="dictCode">
          <el-input
            v-model="dictForm.dictCode"
            placeholder="请输入字典编码（英文）"
            :disabled="!!dictForm.id"
          />
        </el-form-item>
        <el-form-item label="字典名称" prop="dictName">
          <el-input v-model="dictForm.dictName" placeholder="请输入字典名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model="dictForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入描述信息"
          />
        </el-form-item>
        <el-form-item label="排序号">
          <el-input-number v-model="dictForm.sortOrder" :min="0" :max="999" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dictDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveDict">确定</el-button>
      </template>
    </el-dialog>
    
    <!-- 新增/编辑字典项弹窗 -->
    <el-dialog
      v-model="itemDialogVisible"
      :title="itemForm.id ? '编辑字典项' : '新增字典项'"
      width="500px"
      @close="resetItemForm"
    >
      <el-form
        ref="itemFormRef"
        :model="itemForm"
        :rules="itemRules"
        label-width="100px"
      >
        <el-form-item label="字典项编码" prop="itemCode">
          <el-input
            v-model="itemForm.itemCode"
            placeholder="请输入字典项编码"
            :disabled="!!itemForm.id"
          />
        </el-form-item>
        <el-form-item label="字典项值" prop="itemValue">
          <el-input v-model="itemForm.itemValue" placeholder="请输入字典项值" />
        </el-form-item>
        <el-form-item label="父级编码">
          <el-select
            v-model="itemForm.parentCode"
            placeholder="请选择父级（可选）"
            clearable
            style="width: 100%"
          >
            <el-option
              v-for="item in availableParentItems"
              :key="item.code"
              :label="`${item.code} - ${item.value}`"
              :value="item.code"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="排序号">
          <el-input-number v-model="itemForm.sortOrder" :min="0" :max="999" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="itemDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveItem">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Edit, Delete, ArrowUp, ArrowDown, CircleClose, CircleCheck } from '@element-plus/icons-vue'

const API_BASE = '/api/data-dict'

// ==================== 字典分类数据 ====================
const dictList = ref([])
const selectedDict = ref(null)
const selectedDictId = ref(null)

// ==================== 字典项数据 ====================
const itemList = ref([])

// ==================== 表格高度计算 ====================
/**
 * 字典项列表容器的 ref 引用
 * 用于测量容器实际高度，以设置 el-table 的固定高度
 * 确保 el-table 表头固定、表体可滚动
 */
const panelBodyRef = ref(null)
/**
 * el-table 的高度（像素）
 * 通过测量 panel-body 容器高度动态计算
 */
const tableHeight = ref(400)
/**
 * ResizeObserver 实例
 * 监听 panel-body 容器尺寸变化，实时更新表格高度
 */
let resizeObserver = null

/**
 * 更新 el-table 高度
 * 测量 panel-body 容器的实际像素高度并同步到 tableHeight
 */
const updateTableHeight = () => {
  if (panelBodyRef.value) {
    tableHeight.value = panelBodyRef.value.clientHeight
  }
}

/**
 * 可选的父级字典项列表（计算属性）
 * 排除当前编辑的字典项自身，避免循环引用
 */
const availableParentItems = computed(() => {
  return itemList.value.filter(item => item.id !== itemForm.value.id)
})

/**
 * 格式化字典下拉框显示标签
 * 格式：dict_name  dict_code  description（字段间用两个空格分隔）
 */
const formatDictLabel = (dict) => {
  const parts = []
  if (dict.dictName) parts.push(dict.dictName)
  if (dict.dictCode) parts.push(dict.dictCode)
  if (dict.description) parts.push(dict.description)
  return parts.join('  ')
}

// ==================== 字典表单 ====================
const dictDialogVisible = ref(false)
const dictFormRef = ref(null)
const dictForm = ref({
  id: null,
  dictCode: '',
  dictName: '',
  description: '',
  sortOrder: 0,
  status: 1
})

const dictRules = {
  dictCode: [
    { required: true, message: '请输入字典编码', trigger: 'blur' },
    { pattern: /^[a-zA-Z_][a-zA-Z0-9_]*$/, message: '编码只能包含字母、数字和下划线，且以字母或下划线开头', trigger: 'blur' }
  ],
  dictName: [
    { required: true, message: '请输入字典名称', trigger: 'blur' }
  ]
}

// ==================== 字典项表单 ====================
const itemDialogVisible = ref(false)
const itemFormRef = ref(null)
const itemForm = ref({
  id: null,
  itemCode: '',
  itemValue: '',
  parentCode: '',
  sortOrder: 0,
  status: 1
})

const itemRules = {
  itemCode: [
    { required: true, message: '请输入字典项编码', trigger: 'blur' }
  ],
  itemValue: [
    { required: true, message: '请输入字典项值', trigger: 'blur' }
  ]
}

// ==================== 数据加载 ====================

/**
 * 加载所有字典分类
 */
const loadDictList = async () => {
  try {
    const response = await axios.get(API_BASE)
    dictList.value = response.data || []
    // 如果有选中的字典，重新选中
    if (selectedDictId.value) {
      const dict = dictList.value.find(d => d.id === selectedDictId.value)
      if (dict) {
        selectedDict.value = dict
      } else {
        selectedDictId.value = null
        selectedDict.value = null
      }
    }
  } catch (error) {
    console.error('加载字典列表失败:', error)
    ElMessage.error('加载字典列表失败')
  }
}

/**
 * 加载指定字典的字典项列表
 * @param {number} dictId 字典ID
 */
const loadItems = async (dictId) => {
  try {
    const response = await axios.get(`${API_BASE}/${dictId}/items`)
    itemList.value = response.data || []
  } catch (error) {
    console.error('加载字典项失败:', error)
    ElMessage.error('加载字典项失败')
  }
}

// ==================== 字典操作 ====================

/**
 * 处理字典下拉框选择变化
 * @param {number} id 选中的字典ID
 */
const handleDictChange = (id) => {
  if (id) {
    const dict = dictList.value.find(d => d.id === id)
    selectedDict.value = dict
    loadItems(id)
  } else {
    selectedDict.value = null
    itemList.value = []
  }
}

/**
 * 选中字典并加载其字典项
 * @param {Object} dict 字典对象
 */
const selectDict = (dict) => {
  selectedDictId.value = dict.id
  selectedDict.value = dict
  loadItems(dict.id)
}

/**
 * 打开新增字典弹窗
 */
const openAddDictDialog = () => {
  resetDictForm()
  dictDialogVisible.value = true
}

/**
 * 打开编辑字典弹窗
 * @param {Object} dict 待编辑的字典对象
 */
const openEditDictDialog = (dict) => {
  dictForm.value = { ...dict }
  dictDialogVisible.value = true
}

/**
 * 重置字典表单
 */
const resetDictForm = () => {
  dictForm.value = {
    id: null,
    dictCode: '',
    dictName: '',
    description: '',
    sortOrder: 0,
    status: 1
  }
  dictFormRef.value?.resetFields()
}

/**
 * 保存字典（新增或更新）
 */
const saveDict = async () => {
  try {
    await dictFormRef.value.validate()
    
    if (dictForm.value.id) {
      // 更新
      await axios.put(`${API_BASE}/${dictForm.value.id}`, dictForm.value)
      ElMessage.success('更新成功')
    } else {
      // 新增
      await axios.post(API_BASE, dictForm.value)
      ElMessage.success('新增成功')
    }
    
    dictDialogVisible.value = false
    loadDictList()
  } catch (error) {
    if (error !== false) {
      console.error('保存失败:', error)
      const msg = error.response?.data?.error || '保存失败'
      ElMessage.error(msg)
    }
  }
}

/**
 * 确认删除字典（二次确认）
 * 检查该分类是否包含正在使用的字典项（status=1）
 * @param {Object} dict 待删除的字典对象
 */
const confirmDeleteDict = async (dict) => {
  // 检查是否包含正在使用的字典项
  const itemsResponse = await axios.get(`${API_BASE}/${dict.id}/items`)
  const items = itemsResponse.data || []
  const activeItems = items.filter(item => item.status === 1)
  
  if (activeItems.length > 0) {
    ElMessageBox.alert(
      `该分类包含正在使用的字典项（${activeItems.length}个），请禁用或删除所有启用的字典项后，再删除该字典分类。`,
      '无法删除',
      {
        confirmButtonText: '知道了',
        type: 'warning'
      }
    )
    return
  }
  
  ElMessageBox.confirm(
    `确定要删除字典"${dict.dictName}"吗？删除后其下所有字典项将一并删除！`,
    '删除确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      await axios.delete(`${API_BASE}/${dict.id}`)
      ElMessage.success('删除成功')
      if (selectedDictId.value === dict.id) {
        selectedDictId.value = null
        selectedDict.value = null
        itemList.value = []
      }
      loadDictList()
    } catch (error) {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  }).catch(() => {})
}

// ==================== 字典项操作 ====================

/**
 * 判断指定字典项是否是列表中的第一项
 * @param {Object} item 字典项对象
 * @returns {boolean} 是否为第一项
 */
const isFirstItem = (item) => {
  return itemList.value.length > 0 && itemList.value[0].id === item.id
}

/**
 * 判断指定字典项是否是列表中的最后一项
 * @param {Object} item 字典项对象
 * @returns {boolean} 是否为最后一项
 */
const isLastItem = (item) => {
  return itemList.value.length > 0 && itemList.value[itemList.value.length - 1].id === item.id
}

/**
 * 上移字典项
 * @param {Object} item 待上移的字典项对象
 */
const moveItemUp = async (item) => {
  const index = itemList.value.findIndex(i => i.id === item.id)
  if (index <= 0) return
  
  const prev = itemList.value[index - 1]
  // 交换排序号
  await axios.post(`${API_BASE}/${selectedDict.value.id}/items/batch-sort`, [
    { id: item.id, sortOrder: prev.sortOrder },
    { id: prev.id, sortOrder: item.sortOrder }
  ])
  
  // 本地更新显示
  const temp = itemList.value[index - 1]
  itemList.value[index - 1] = itemList.value[index]
  itemList.value[index] = temp
  ElMessage.success('上移成功')
}

/**
 * 下移字典项
 * @param {Object} item 待下移的字典项对象
 */
const moveItemDown = async (item) => {
  const index = itemList.value.findIndex(i => i.id === item.id)
  if (index < 0 || index >= itemList.value.length - 1) return
  
  const next = itemList.value[index + 1]
  // 交换排序号
  await axios.post(`${API_BASE}/${selectedDict.value.id}/items/batch-sort`, [
    { id: item.id, sortOrder: next.sortOrder },
    { id: next.id, sortOrder: item.sortOrder }
  ])
  
  // 本地更新显示
  const temp = itemList.value[index + 1]
  itemList.value[index + 1] = itemList.value[index]
  itemList.value[index] = temp
  ElMessage.success('下移成功')
}

/**
 * 打开新增字典项弹窗
 */
const openAddItemDialog = () => {
  resetItemForm()
  // 自动计算下一个排序号
  const maxSort = itemList.value.reduce((max, item) => Math.max(max, item.sortOrder || 0), 0)
  itemForm.value.sortOrder = maxSort + 1
  itemDialogVisible.value = true
}

/**
 * 打开编辑字典项弹窗
 * @param {Object} item 待编辑的字典项对象
 */
const openEditItemDialog = (item) => {
  itemForm.value = {
    id: item.id,
    itemCode: item.code,
    itemValue: item.value,
    parentCode: item.parentCode || '',
    sortOrder: item.sortOrder,
    status: item.status
  }
  itemDialogVisible.value = true
}

/**
 * 重置字典项表单
 */
const resetItemForm = () => {
  itemForm.value = {
    id: null,
    itemCode: '',
    itemValue: '',
    parentCode: '',
    sortOrder: 0,
    status: 1
  }
  itemFormRef.value?.resetFields()
}

/**
 * 保存字典项（新增或更新）
 */
const saveItem = async () => {
  try {
    await itemFormRef.value.validate()
    
    if (itemForm.value.id) {
      // 更新
      await axios.put(`${API_BASE}/items/${itemForm.value.id}`, itemForm.value)
      ElMessage.success('更新成功')
    } else {
      // 新增
      await axios.post(`${API_BASE}/${selectedDict.value.id}/items`, itemForm.value)
      ElMessage.success('新增成功')
    }
    
    itemDialogVisible.value = false
    loadItems(selectedDict.value.id)
  } catch (error) {
    if (error !== false) {
      console.error('保存失败:', error)
      const msg = error.response?.data?.error || '保存失败'
      ElMessage.error(msg)
    }
  }
}

/**
 * 切换字典项状态（启用/禁用）
 * @param {Object} item 字典项对象
 */
const toggleItemStatus = async (item) => {
  try {
    const newStatus = item.status === 1 ? 0 : 1
    await axios.put(`${API_BASE}/items/${item.id}/status`, { status: newStatus })
    ElMessage.success(newStatus === 1 ? '已启用' : '已禁用')
    loadItems(selectedDict.value.id)
  } catch (error) {
    console.error('切换状态失败:', error)
    ElMessage.error('切换状态失败')
  }
}

/**
 * 确认删除字典项（二次确认）
 * @param {Object} item 待删除的字典项对象
 */
const confirmDeleteItem = (item) => {
  ElMessageBox.confirm(
    `确定要删除字典项"${item.value}"吗？`,
    '删除确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      await axios.delete(`${API_BASE}/items/${item.id}`)
      ElMessage.success('删除成功')
      loadItems(selectedDict.value.id)
    } catch (error) {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  }).catch(() => {})
}

// ==================== 生命周期 ====================

onMounted(() => {
  loadDictList()
  // 等待 DOM 渲染完成后测量容器高度，并监听容器尺寸变化
  nextTick(() => {
    updateTableHeight()
    if (panelBodyRef.value && typeof ResizeObserver !== 'undefined') {
      resizeObserver = new ResizeObserver(updateTableHeight)
      resizeObserver.observe(panelBodyRef.value)
    }
  })
})

onBeforeUnmount(() => {
  // 组件卸载时断开 ResizeObserver，避免内存泄漏
  if (resizeObserver) {
    resizeObserver.disconnect()
    resizeObserver = null
  }
})
</script>

<style scoped>
.dict-settings {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 15px;
}

/* 上部：字典分类选择器 */
.dict-selector-panel {
  padding: 15px 0;
  border-bottom: 1px solid #ebeef5;
}

.selector-label {
  font-weight: bold;
  color: #303133;
  margin-bottom: 10px;
}

.selector-row {
  display: flex;
  gap: 10px;
  align-items: center;
}

/* 下部：字典项列表 */
.dict-item-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
}

.panel-header {
  padding: 0 0 10px 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: bold;
  color: #303133;
}

.panel-body {
  flex: 1;
  overflow: hidden;
  padding: 0;
}

/* 操作按钮组 */
.action-buttons {
  display: flex;
  gap: 1px;
  align-items: center;
}

.action-buttons .el-button {
  padding: 4px;
  margin: 0;
  min-height: 28px;
}
</style>
