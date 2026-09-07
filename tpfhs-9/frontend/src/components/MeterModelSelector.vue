<template>
  <el-dialog
    v-model="dialogVisible"
    title="电表型号选择和设置"
    width="700px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <!-- 顶部：查询筛选和新增按钮 -->
    <div class="selector-header">
      <el-input
        v-model="searchKeyword"
        placeholder="输入拼音首字母或型号名称"
        clearable
        class="search-input"
        @keyup.enter="handleSearch"
        @clear="handleSearch"
      />
      <el-button type="primary" @click="handleSearch">查询</el-button>
      <el-button type="success" @click="handleAdd">新增</el-button>
    </div>

    <!-- 列表 -->
    <div class="selector-list">
      <div 
        v-for="item in filteredList" 
        :key="item.id" 
        class="list-item"
        :class="{ selected: selectedId === item.id }"
        @dblclick="handleDoubleClick(item)"
      >
        <el-radio
          v-model="selectedId"
          :label="item.id"
          @change="handleSelect(item)"
        >
          <span class="item-name">{{ item.modelName }}</span>
          <el-tag v-if="item.modelType" size="small" class="item-type">
            {{ item.modelType }}
          </el-tag>
          <span v-if="item.description" class="item-desc" :title="item.description">
            {{ item.description }}
          </span>
        </el-radio>
        <div class="item-actions">
          <el-button type="primary" size="small" @click.stop="handleEdit(item)">
            修改
          </el-button>
          <el-button type="danger" size="small" @click.stop="handleDelete(item)">
            删除
          </el-button>
        </div>
      </div>
      <el-empty v-if="filteredList.length === 0" description="暂无数据" />
    </div>

    <!-- 底部按钮 -->
    <template #footer>
      <div class="dialog-footer">
        <el-button 
          type="primary" 
          :disabled="!selectedId" 
          @click="handleConfirm"
        >
          选择
        </el-button>
        <el-button @click="handleCancel">取消</el-button>
      </div>
    </template>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="editDialogVisible"
      :title="editingItem.id ? '编辑电表型号' : '新增电表型号'"
      width="450px"
      :close-on-click-modal="false"
    >
      <el-form 
        ref="editFormRef" 
        :model="editingItem" 
        :rules="editFormRules"
        label-width="100px"
      >
        <el-form-item label="电表型号" prop="modelName">
          <el-input v-model="editingItem.modelName" placeholder="请输入电表型号" />
        </el-form-item>
        <el-form-item label="所属类型" prop="modelType">
          <el-select v-model="editingItem.modelType" placeholder="请选择所属类型" style="width: 100%">
            <el-option label="单相" value="单相" />
            <el-option label="三相三线" value="三相三线" />
            <el-option label="三相四线" value="三相四线" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input 
            v-model="editingItem.description" 
            type="textarea" 
            :rows="3" 
            placeholder="请输入描述信息" 
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 删除确认弹窗 -->
    <el-dialog
      v-model="deleteDialogVisible"
      title="确认删除"
      width="400px"
    >
      <p>确定要删除电表型号 "{{ deletingItem.modelName }}" 吗？</p>
      <template #footer>
        <el-button @click="deleteDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="confirmDelete">确认删除</el-button>
      </template>
    </el-dialog>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { generatePinyinCode } from '../utils/pinyinUtils'

const API_BASE = '/api/meter-model'

const emit = defineEmits(['select', 'cancel'])

// 主弹窗状态
const dialogVisible = ref(false)

// 搜索关键词
const searchKeyword = ref('')
// 是否处于拼音首字母筛选模式（输入英文字母时为 true）
const pinyinFilterMode = ref(false)

// 列表数据
const meterModels = ref([])
/**
 * 过滤后的电表型号列表（计算属性）
 * 根据搜索关键词动态过滤：
 * - 关键词为英文字母时，按电表型号名称的拼音首字母筛选
 * - 关键词为非英文字符时，按型号名称模糊匹配
 * @returns {Array} 过滤后的电表型号数组
 */
const filteredList = computed(() => {
  if (!searchKeyword.value) {
    return meterModels.value
  }
  
  const keyword = searchKeyword.value.trim()
  
  // 判断是否为英文字母
  const isEnglish = /^[a-zA-Z]+$/.test(keyword)
  
  if (isEnglish) {
    // 拼音首字母筛选模式
    pinyinFilterMode.value = true
    const upperKeyword = keyword.toUpperCase()
    return meterModels.value.filter(item => {
      const pinyinCode = generatePinyinCode(item.modelName || '')
      return pinyinCode.startsWith(upperKeyword) || 
             pinyinCode.toLowerCase().startsWith(keyword.toLowerCase())
    })
  } else {
    // 非拼音筛选模式
    pinyinFilterMode.value = false
    return meterModels.value.filter(item => 
      item.modelName && item.modelName.includes(keyword)
    )
  }
})

// 选中
const selectedId = ref(null)
const selectedItem = ref(null)

// 新增/编辑
const editDialogVisible = ref(false)
const editingItem = ref({
  modelName: '',
  modelType: '',
  description: ''
})
const editFormRef = ref(null)
const editFormRules = {
  modelName: [{ required: true, message: '请输入电表型号', trigger: 'blur' }],
  modelType: [{ required: true, message: '请选择所属类型', trigger: 'change' }]
}

// 删除
const deleteDialogVisible = ref(false)
const deletingItem = ref(null)

/**
 * 暴露给父组件的方法
 * open: 打开弹窗并加载电表型号数据
 */
defineExpose({
  open: () => {
    dialogVisible.value = true
    loadData()
  }
})

/**
 * 从后端加载所有电表型号数据
 * 请求 GET /api/meter-model 接口，将结果存入 meterModels
 * @returns {Promise<void>}
 */
const loadData = async () => {
  try {
    const response = await axios.get(API_BASE)
    meterModels.value = response.data || []
  } catch (error) {
    console.error('加载电表型号数据失败:', error)
    ElMessage.error('加载数据失败')
  }
}

/**
 * 查询按钮点击处理
 * filteredList 为计算属性会自动响应 searchKeyword 变化，此函数保留以备扩展
 */
const handleSearch = () => {
  // filteredList 已经是 computed 属性，自动更新
}

/**
 * 单选框选中处理
 * @param {Object} item 被选中的电表型号对象
 */
const handleSelect = (item) => {
  selectedItem.value = item
}

/**
 * 列表条目双击处理
 * 效果等同于选中该条目并点击"选择"按钮
 * @param {Object} item 被双击的电表型号对象
 */
const handleDoubleClick = (item) => {
  selectedId.value = item.id
  selectedItem.value = item
  handleConfirm()
}

/**
 * 确认选择按钮处理
 * 向父组件 emit select 事件，携带选中条目的型号名称，并关闭弹窗
 */
const handleConfirm = () => {
  if (selectedItem.value) {
    emit('select', {
      action: 'select',
      meterModel: selectedItem.value.modelName,
      item: selectedItem.value
    })
    dialogVisible.value = false
  }
}

/**
 * 取消按钮处理
 * 向父组件 emit cancel 事件，仅关闭弹窗不返回名称信息
 */
const handleCancel = () => {
  emit('cancel', { action: 'cancel' })
  dialogVisible.value = false
}

/**
 * 弹窗关闭时的清理处理
 * 重置选中状态和搜索关键词
 */
const handleClose = () => {
  selectedId.value = null
  selectedItem.value = null
  searchKeyword.value = ''
}

/**
 * 新增按钮处理
 * 重置编辑表单为空对象并打开新增/编辑弹窗
 */
const handleAdd = () => {
  editingItem.value = {
    modelName: '',
    modelType: '',
    description: ''
  }
  editDialogVisible.value = true
}

/**
 * 修改按钮处理
 * 将被点击条目的数据拷贝到编辑表单并打开新增/编辑弹窗
 * @param {Object} item 待编辑的电表型号对象
 */
const handleEdit = (item) => {
  editingItem.value = { ...item }
  editDialogVisible.value = true
}

/**
 * 保存按钮处理（新增或更新）
 * 先校验表单，再根据 editingItem 是否含 id 判断调用 POST（新增）或 PUT（更新）接口
 * @returns {Promise<void>}
 */
const handleSave = async () => {
  try {
    await editFormRef.value.validate()
    
    if (editingItem.value.id) {
      // 更新
      await axios.put(`${API_BASE}/${editingItem.value.id}`, editingItem.value)
      ElMessage.success('更新成功')
    } else {
      // 新增
      await axios.post(API_BASE, editingItem.value)
      ElMessage.success('新增成功')
    }
    
    editDialogVisible.value = false
    loadData()
  } catch (error) {
    if (error !== false) { // 验证失败时 error 为 false
      console.error('保存失败:', error)
      ElMessage.error('保存失败')
    }
  }
}

/**
 * 删除按钮处理
 * 记录待删除条目并打开删除确认弹窗
 * @param {Object} item 待删除的电表型号对象
 */
const handleDelete = (item) => {
  deletingItem.value = item
  deleteDialogVisible.value = true
}

/**
 * 确认删除处理
 * 调用 DELETE /api/meter-model/{id} 接口删除电表型号，成功后刷新列表
 * @returns {Promise<void>}
 */
const confirmDelete = async () => {
  try {
    await axios.delete(`${API_BASE}/${deletingItem.value.id}`)
    ElMessage.success('删除成功')
    deleteDialogVisible.value = false
    loadData()
  } catch (error) {
    console.error('删除失败:', error)
    ElMessage.error('删除失败')
  }
}

onMounted(() => {
  // 初始化时不需要加载数据，等待 open() 调用
})
</script>

<style scoped>
.selector-header {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.search-input {
  flex: 1;
}

.selector-list {
  max-height: 400px;
  overflow-y: auto;
  border: 1px solid #ebeef5;
  border-radius: 4px;
}

.list-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  border-bottom: 1px solid #ebeef5;
  cursor: pointer;
  transition: background-color 0.2s;
}

.list-item:last-child {
  border-bottom: none;
}

.list-item:hover {
  background-color: #f5f7fa;
}

.list-item.selected {
  background-color: #ecf5ff;
}

.item-name {
  font-weight: 500;
  margin-right: 8px;
}

.item-type {
  margin-right: 8px;
}

.item-desc {
  color: #909399;
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 200px;
  display: inline-block;
}

.item-actions {
  display: flex;
  gap: 4px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>
