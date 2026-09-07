<!--
  能耗三级分类设置组件
  功能描述：
    1. 对按能耗使用场景划分的三级分类（生产用能/生产辅助用能/综合用能）进行管理
    2. 一级分类：可添加、删除、编辑
    3. 选中一级分类后，可对其下属的二级分类进行添加、删除、编辑
    4. 选中二级分类后，可对其下属的三级分类进行添加、删除、编辑
    5. 分类编码：支持手动输入或自动生成（按"父级编码+两位序号"规则）
    6. 删除父级分类会级联删除其下属所有子分类（数据库 ON DELETE CASCADE）

  组件属性：
    - currentUser (Object): 当前登录用户信息，用于记录 createdBy/updatedBy

  依赖接口：
    - GET    /api/energy-categories/level/{level}    获取指定层级的分类
    - GET    /api/energy-categories/children/{parentId}  获取指定父级下的子分类
    - POST   /api/energy-categories                 新增分类
    - PUT    /api/energy-categories/{id}            修改分类
    - DELETE /api/energy-categories/{id}            删除分类
-->
<template>
  <div class="energy-category-settings">
    <div class="page-header">
      <h3>能耗三级分类设置</h3>
      <div class="header-tip">
        <el-icon><InfoFilled /></el-icon>
        <span>层级关系：一级（生产用能/生产辅助用能/综合用能等） → 二级 → 三级。选中上级后可管理其下属分类。</span>
      </div>
    </div>

    <div class="category-layout">
      <!-- ============ 一级分类 ============ -->
      <div class="category-column">
        <div class="column-header">
          <span class="column-title">一级分类</span>
          <el-button type="primary" size="small" @click="openAddDialog(1)">新增</el-button>
        </div>
        <div class="column-body">
          <el-table
            :data="level1List"
            v-loading="loading1"
            border
            highlight-current-row
            :max-height="tableMaxHeight"
            @current-change="handleLevel1Select"
            :row-class-name="rowClass1"
            @row-click="handleLevel1RowClick"
            size="small"
          >
            <el-table-column prop="categoryCode" label="编码" width="46" />
            <el-table-column prop="categoryName" label="分类名称" min-width="100" show-overflow-tooltip />
            <el-table-column label="状态" width="52">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
                  {{ row.status === 1 ? '启用' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="66" fixed="right">
              <template #default="{ row }">
                <el-tooltip content="编辑" placement="top">
                  <el-button link type="primary" size="small" @click.stop="openEditDialog(row)">
                    <el-icon><Edit /></el-icon>
                  </el-button>
                </el-tooltip>
                <el-tooltip content="删除" placement="top">
                  <el-button link type="danger" size="small" @click.stop="confirmDelete(row)">
                    <el-icon><Delete /></el-icon>
                  </el-button>
                </el-tooltip>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>

      <!-- ============ 二级分类 ============ -->
      <div class="category-column">
        <div class="column-header">
          <span class="column-title">
            二级分类
            <span v-if="selectedLevel1" class="parent-hint">— {{ selectedLevel1.categoryName }}</span>
          </span>
          <el-button type="primary" size="small" :disabled="!selectedLevel1" @click="openAddDialog(2)">新增</el-button>
        </div>
        <div class="column-body">
          <el-table
            :data="level2List"
            v-loading="loading2"
            border
            highlight-current-row
            :max-height="tableMaxHeight"
            @current-change="handleLevel2Select"
            :row-class-name="rowClass2"
            @row-click="handleLevel2RowClick"
            size="small"
          >
            <el-table-column prop="categoryCode" label="编码" width="60" />
            <el-table-column prop="categoryName" label="分类名称" min-width="200" show-overflow-tooltip />
            <el-table-column label="状态" width="52">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
                  {{ row.status === 1 ? '启用' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="66" fixed="right">
              <template #default="{ row }">
                <el-tooltip content="编辑" placement="top">
                  <el-button link type="primary" size="small" @click.stop="openEditDialog(row)">
                    <el-icon><Edit /></el-icon>
                  </el-button>
                </el-tooltip>
                <el-tooltip content="删除" placement="top">
                  <el-button link type="danger" size="small" @click.stop="confirmDelete(row)">
                    <el-icon><Delete /></el-icon>
                  </el-button>
                </el-tooltip>
              </template>
            </el-table-column>
          </el-table>
          <el-empty
            v-if="!selectedLevel1"
            description="请先在左侧选择一级分类"
            :image-size="80"
            class="column-empty"
          />
        </div>
      </div>

      <!-- ============ 三级分类 ============ -->
      <div class="category-column">
        <div class="column-header">
          <span class="column-title">
            三级分类
            <span v-if="selectedLevel2" class="parent-hint">— {{ selectedLevel2.categoryName }}</span>
          </span>
          <el-button type="primary" size="small" :disabled="!selectedLevel2" @click="openAddDialog(3)">新增</el-button>
        </div>
        <div class="column-body">
          <el-table
            :data="level3List"
            v-loading="loading3"
            border
            :max-height="tableMaxHeight"
            size="small"
          >
            <el-table-column prop="categoryCode" label="编码" width="76" />
            <el-table-column prop="categoryName" label="分类名称" min-width="160" show-overflow-tooltip />
            <el-table-column label="状态" width="70">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
                  {{ row.status === 1 ? '启用' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="66" fixed="right">
              <template #default="{ row }">
                <el-tooltip content="编辑" placement="top">
                  <el-button link type="primary" size="small" @click.stop="openEditDialog(row)">
                    <el-icon><Edit /></el-icon>
                  </el-button>
                </el-tooltip>
                <el-tooltip content="删除" placement="top">
                  <el-button link type="danger" size="small" @click.stop="confirmDelete(row)">
                    <el-icon><Delete /></el-icon>
                  </el-button>
                </el-tooltip>
              </template>
            </el-table-column>
          </el-table>
          <el-empty
            v-if="!selectedLevel2"
            description="请先在左侧选择二级分类"
            :image-size="80"
            class="column-empty"
          />
        </div>
      </div>
    </div>

    <!-- ============ 新增/编辑对话框 ============ -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="560px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="100px"
      >
        <el-form-item label="所属层级">
          <el-tag type="info">{{ levelLabel(formData.level) }}</el-tag>
          <span v-if="formData.parentId" class="parent-info">
            （父级：{{ parentDisplay }}）
          </span>
        </el-form-item>
        <el-form-item label="分类编码" prop="categoryCode">
          <el-input v-model="formData.categoryCode" placeholder="如：EC01 或 EC0101" style="width: 70%" />
          <el-button @click="generateCode" style="margin-left: 8px">自动生成</el-button>
        </el-form-item>
        <el-form-item label="分类名称" prop="categoryName">
          <el-input v-model="formData.categoryName" placeholder="如：生产用能" />
        </el-form-item>
        <el-form-item label="备注说明">
          <el-input
            v-model="formData.remark"
            type="textarea"
            :rows="3"
            placeholder="该分类的适用范围、数据来源等说明"
          />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="formData.sortOrder" :min="0" :max="9999" />
          <span class="form-tip">数值越小越靠前，留空将自动取同级最大值+1</span>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="formData.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { InfoFilled, Edit, Delete } from '@element-plus/icons-vue'
import axios from 'axios'
import { generatePinyinCode } from '../utils/pinyinUtils'

const props = defineProps({
  /**
   * 当前登录用户信息
   */
  currentUser: {
    type: Object,
    default: () => ({})
  }
})

// ============ 列表数据 ============
const level1List = ref([])
const level2List = ref([])
const level3List = ref([])
const loading1 = ref(false)
const loading2 = ref(false)
const loading3 = ref(false)

// 当前选中的各级分类
const selectedLevel1 = ref(null)
const selectedLevel2 = ref(null)

// ============ 表格高度 ============
const tableMaxHeight = computed(() => Math.max(window.innerHeight - 280, 300))

// ============ 对话框状态 ============
const dialogVisible = ref(false)
const dialogTitle = ref('新增分类')
const formRef = ref(null)
const editingId = ref(null)
const formData = reactive({
  id: null,
  categoryCode: '',
  categoryName: '',
  parentId: null,
  level: 1,
  remark: '',
  sortOrder: null,
  status: 1
})

const formRules = {
  categoryCode: [
    { required: true, message: '请输入分类编码', trigger: 'blur' },
    { max: 50, message: '编码长度不能超过50个字符', trigger: 'blur' }
  ],
  categoryName: [
    { required: true, message: '请输入分类名称', trigger: 'blur' },
    { max: 100, message: '名称长度不能超过100个字符', trigger: 'blur' }
  ]
}

/**
 * 计算对话框中"父级"显示文本
 */
const parentDisplay = computed(() => {
  if (!formData.parentId) return ''
  if (formData.level === 2 && selectedLevel1.value) {
    return selectedLevel1.value.categoryName
  }
  if (formData.level === 3 && selectedLevel2.value) {
    return selectedLevel2.value.categoryName
  }
  return ''
})

/**
 * 层级中文标签
 */
const levelLabel = (level) => {
  if (level === 1) return '一级分类'
  if (level === 2) return '二级分类'
  if (level === 3) return '三级分类'
  return ''
}

// ============ 行样式（高亮选中行） ============
const rowClass1 = ({ row }) => {
  return selectedLevel1.value && row.id === selectedLevel1.value.id ? 'selected-row' : ''
}
const rowClass2 = ({ row }) => {
  return selectedLevel2.value && row.id === selectedLevel2.value.id ? 'selected-row' : ''
}

// ============ 数据加载方法 ============

/**
 * 加载一级分类
 */
const loadLevel1 = async () => {
  loading1.value = true
  try {
    const response = await axios.get('/api/energy-categories/level/1')
    level1List.value = response.data || []
    // 校验当前选中的一级分类是否仍在列表中
    if (selectedLevel1.value) {
      const stillExists = level1List.value.some(it => it.id === selectedLevel1.value.id)
      if (!stillExists) {
        selectedLevel1.value = null
        level2List.value = []
        selectedLevel2.value = null
        level3List.value = []
      }
    }
  } catch (error) {
    console.error('加载一级分类失败:', error)
    ElMessage.error('加载一级分类失败')
  } finally {
    loading1.value = false
  }
}

/**
 * 加载指定一级分类下的二级分类
 */
const loadLevel2 = async (parentId) => {
  if (!parentId) {
    level2List.value = []
    return
  }
  loading2.value = true
  try {
    const response = await axios.get(`/api/energy-categories/children/${parentId}`)
    level2List.value = response.data || []
    // 校验当前选中的二级分类是否仍在列表中
    if (selectedLevel2.value) {
      const stillExists = level2List.value.some(it => it.id === selectedLevel2.value.id)
      if (!stillExists) {
        selectedLevel2.value = null
        level3List.value = []
      }
    }
  } catch (error) {
    console.error('加载二级分类失败:', error)
    ElMessage.error('加载二级分类失败')
  } finally {
    loading2.value = false
  }
}

/**
 * 加载指定二级分类下的三级分类
 */
const loadLevel3 = async (parentId) => {
  if (!parentId) {
    level3List.value = []
    return
  }
  loading3.value = true
  try {
    const response = await axios.get(`/api/energy-categories/children/${parentId}`)
    level3List.value = response.data || []
  } catch (error) {
    console.error('加载三级分类失败:', error)
    ElMessage.error('加载三级分类失败')
  } finally {
    loading3.value = false
  }
}

// ============ 选中事件处理 ============

/**
 * 选中一级分类行
 */
const handleLevel1Select = (row) => {
  if (!row) return
  selectedLevel1.value = row
  // 重置下级选中状态
  selectedLevel2.value = null
  level3List.value = []
  loadLevel2(row.id)
}

/**
 * 兼容 element-plus current-change 行点击
 */
const handleLevel1RowClick = (row) => {
  handleLevel1Select(row)
}

/**
 * 选中二级分类行
 */
const handleLevel2Select = (row) => {
  if (!row) return
  selectedLevel2.value = row
  loadLevel3(row.id)
}

const handleLevel2RowClick = (row) => {
  handleLevel2Select(row)
}

// ============ 新增/编辑对话框 ============

/**
 * 重置表单
 */
const resetForm = () => {
  formData.id = null
  formData.categoryCode = ''
  formData.categoryName = ''
  formData.parentId = null
  formData.level = 1
  formData.remark = ''
  formData.sortOrder = null
  formData.status = 1
  editingId.value = null
}

/**
 * 打开新增对话框
 * @param {number} level 1/2/3
 */
const openAddDialog = (level) => {
  resetForm()
  formData.level = level
  if (level === 2) {
    if (!selectedLevel1.value) {
      ElMessage.warning('请先选择一级分类')
      return
    }
    formData.parentId = selectedLevel1.value.id
  } else if (level === 3) {
    if (!selectedLevel2.value) {
      ElMessage.warning('请先选择二级分类')
      return
    }
    formData.parentId = selectedLevel2.value.id
  }
  // 预生成编码（用户可修改）
  formData.categoryCode = suggestCode(level)
  dialogTitle.value = `新增${levelLabel(level)}`
  dialogVisible.value = true
}

/**
 * 打开编辑对话框
 * @param {Object} row 行数据
 */
const openEditDialog = (row) => {
  resetForm()
  editingId.value = row.id
  formData.id = row.id
  formData.categoryCode = row.categoryCode
  formData.categoryName = row.categoryName
  formData.parentId = row.parentId
  formData.level = row.level
  formData.remark = row.remark || ''
  formData.sortOrder = row.sortOrder
  formData.status = row.status
  dialogTitle.value = `编辑${levelLabel(row.level)}`
  dialogVisible.value = true
}

/**
 * 自动生成分类编码建议值
 * 规则：
 *  - 一级：EC + 两位序号（如 EC01、EC05）
 *  - 二级：父级编码 + 两位序号（如 EC0101）
 *  - 三级：父级编码 + 两位序号（如 EC020101）
 * 同时叠加拼音首字母作为备选以提升可读性
 */
const suggestCode = (level) => {
  let parentCode = ''
  let siblings = []
  if (level === 1) {
    siblings = level1List.value
  } else if (level === 2 && selectedLevel1.value) {
    parentCode = selectedLevel1.value.categoryCode
    siblings = level2List.value
  } else if (level === 3 && selectedLevel2.value) {
    parentCode = selectedLevel2.value.categoryCode
    siblings = level3List.value
  }

  // 计算下一个两位序号
  let nextSeq = 1
  if (siblings.length > 0) {
    const seqs = siblings.map(it => {
      const code = it.categoryCode || ''
      const tail = code.slice(-2)
      const n = parseInt(tail, 10)
      return isNaN(n) ? 0 : n
    })
    nextSeq = Math.max(...seqs) + 1
  }
  const seqStr = String(nextSeq).padStart(2, '0')
  return `${parentCode}${seqStr}`
}

/**
 * 点击"自动生成"按钮，将建议编码填入输入框
 * 当用户已输入名称时，附加拼音首字母作为后缀以提升可读性
 */
const generateCode = () => {
  const base = suggestCode(formData.level)
  // 如果用户已填名称，则使用拼音首字母作为可读后缀
  const nameCode = formData.categoryName ? generatePinyinCode(formData.categoryName) : ''
  if (nameCode) {
    formData.categoryCode = `${base}_${nameCode}`.slice(0, 50)
  } else {
    formData.categoryCode = base
  }
}

/**
 * 保存分类（新增或编辑）
 */
const handleSave = async () => {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  try {
    const payload = {
      categoryCode: formData.categoryCode.trim(),
      categoryName: formData.categoryName.trim(),
      parentId: formData.parentId,
      level: formData.level,
      remark: formData.remark,
      sortOrder: formData.sortOrder,
      status: formData.status,
      updatedBy: props.currentUser?.userId || null
    }
    if (editingId.value) {
      // 编辑
      await axios.put(`/api/energy-categories/${editingId.value}`, payload)
      ElMessage.success('更新成功')
    } else {
      // 新增
      payload.createdBy = props.currentUser?.userId || null
      await axios.post('/api/energy-categories', payload)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    // 刷新对应层级列表
    await refreshAfterSave(formData.level)
  } catch (error) {
    if (error.response?.data?.message) {
      ElMessage.error(error.response.data.message)
    } else if (error.response?.data?.error) {
      ElMessage.error(error.response.data.error)
    } else if (error.message) {
      ElMessage.error(error.message)
    } else {
      ElMessage.error('保存失败')
    }
  }
}

/**
 * 保存后刷新对应层级列表，并保持选中状态
 */
const refreshAfterSave = async (level) => {
  if (level === 1) {
    await loadLevel1()
    if (selectedLevel1.value) {
      const updated = level1List.value.find(it => it.id === selectedLevel1.value.id)
      if (updated) {
        selectedLevel1.value = updated
      }
    }
  } else if (level === 2) {
    if (selectedLevel1.value) {
      await loadLevel2(selectedLevel1.value.id)
      if (selectedLevel2.value) {
        const updated = level2List.value.find(it => it.id === selectedLevel2.value.id)
        if (updated) {
          selectedLevel2.value = updated
        }
      }
    }
  } else if (level === 3) {
    if (selectedLevel2.value) {
      await loadLevel3(selectedLevel2.value.id)
    }
  }
}

// ============ 删除 ============

/**
 * 删除分类确认
 * 由于数据库配置了 ON DELETE CASCADE，删除父级分类会级联删除所有子分类，
 * 因此在删除前会向用户明确提示。
 */
const confirmDelete = async (row) => {
  try {
    const childCount = await getChildCount(row.id)
    let message = `确定要删除"${row.categoryName}"（${row.categoryCode}）吗？`
    if (childCount > 0) {
      message = `该分类下存在 ${childCount} 个子分类，删除后将级联删除所有下属子分类！\n确定要删除"${row.categoryName}"吗？`
    }
    await ElMessageBox.confirm(message, '确认删除', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
      dangerouslyUseHTMLString: false
    })
    await axios.delete(`/api/energy-categories/${row.id}`)
    ElMessage.success('删除成功')
    await refreshAfterDelete(row)
  } catch (error) {
    if (error === 'cancel') return
    if (error.response?.data?.message) {
      ElMessage.error(error.response.data.message)
    } else if (error.response?.data?.error) {
      ElMessage.error(error.response.data.error)
    } else if (error.message) {
      ElMessage.error(error.message)
    } else {
      ElMessage.error('删除失败')
    }
  }
}

/**
 * 查询子分类数量（用于删除前提示）
 */
const getChildCount = async (parentId) => {
  try {
    const response = await axios.get(`/api/energy-categories/children/${parentId}`)
    return (response.data || []).length
  } catch {
    return 0
  }
}

/**
 * 删除后刷新对应层级列表，并处理选中状态变化
 */
const refreshAfterDelete = async (row) => {
  if (row.level === 1) {
    // 如果删除的是当前选中的一级分类，清除所有下级选中
    if (selectedLevel1.value && selectedLevel1.value.id === row.id) {
      selectedLevel1.value = null
      selectedLevel2.value = null
      level2List.value = []
      level3List.value = []
    }
    await loadLevel1()
  } else if (row.level === 2) {
    // 如果删除的是当前选中的二级分类，清除三级选中
    if (selectedLevel2.value && selectedLevel2.value.id === row.id) {
      selectedLevel2.value = null
      level3List.value = []
    }
    if (selectedLevel1.value) {
      await loadLevel2(selectedLevel1.value.id)
    }
  } else if (row.level === 3) {
    if (selectedLevel2.value) {
      await loadLevel3(selectedLevel2.value.id)
    }
  }
}

// ============ 初始化 ============
onMounted(() => {
  loadLevel1()
})
</script>

<style scoped>
.energy-category-settings {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 10px 0;
}

.page-header {
  margin-bottom: 12px;
  padding: 0 4px;
}

.page-header h3 {
  margin: 0 0 6px 0;
  font-size: 16px;
  color: #303133;
}

.header-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #909399;
  line-height: 1.4;
}

.category-layout {
  display: flex;
  gap: 12px;
  flex: 1;
  overflow: hidden;
  min-height: 0;
}

.category-column {
  flex: 1;
  display: flex;
  flex-direction: column;
  background-color: #ffffff;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  overflow: hidden;
  min-width: 0;
}

.column-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background-color: #f5f7fa;
  border-bottom: 1px solid #ebeef5;
}

.column-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  display: flex;
  align-items: center;
  gap: 6px;
}

.parent-hint {
  font-size: 12px;
  font-weight: normal;
  color: #67c23a;
}

.column-body {
  flex: 1;
  overflow: auto;
  padding: 6px;
  display: flex;
  flex-direction: column;
}

.column-empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.parent-info {
  margin-left: 8px;
  color: #67c23a;
  font-size: 13px;
}

.form-tip {
  margin-left: 10px;
  font-size: 12px;
  color: #909399;
}

/* 选中行高亮样式 */
:deep(.selected-row) > td {
  background-color: #ecf5ff !important;
}

:deep(.el-table__row:hover) > td {
  background-color: #f5f7fa;
}

/* 让 el-table 行可点击 */
:deep(.el-table__body tr) {
  cursor: pointer;
}
</style>
