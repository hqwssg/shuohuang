<template>
  <!-- 数据来源系统设置对话框 -->
  <el-dialog
    v-model="dialogVisible"
    title="数据来源系统设置"
    width="600px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-input
        v-model="searchKeyword"
        placeholder="请输入查询条件"
        class="search-input"
        @input="handleSearchInput"
        clearable
      />
      <el-button type="primary" @click="handleSearch">查询</el-button>
      <el-button type="success" @click="handleAdd">新增</el-button>
    </div>
    
    <!-- 数据来源系统列表 -->
    <el-table
      ref="tableRef"
      :data="tableData"
      border
      max-height="300"
      @row-dblclick="handleRowDblClick"
      highlight-current-row
      class="data-table"
    >
      <!-- 单选按钮列 -->
      <el-table-column width="50" align="center">
        <template #default="{ row }">
          <el-radio
            v-model="selectedId"
            :label="row.id"
            @change="handleRadioChange(row)"
          >&nbsp;</el-radio>
        </template>
      </el-table-column>
      <!-- 系统名称列 -->
      <el-table-column prop="systemName" label="数据来源系统名称" min-width="150" />
      <!-- 说明列 -->
      <el-table-column prop="description" label="说明" min-width="200" show-overflow-tooltip />
      <!-- 创建时间列 -->
      <el-table-column prop="createdAt" label="创建时间" width="150">
        <template #default="{ row }">
          {{ formatDate(row.createdAt) }}
        </template>
      </el-table-column>
      <!-- 操作列 -->
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="handleEdit(row)">修改</el-button>
          <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <!-- 对话框底部操作栏 -->
    <div slot="footer" class="dialog-footer">
      <el-button @click="handleCancel">取消</el-button>
      <el-button type="primary" @click="handleSelect">选择</el-button>
    </div>
    
    <!-- 新增/修改数据来源系统的子对话框 -->
    <el-dialog
      v-model="innerDialogVisible"
      :title="isEditMode ? '修改数据来源系统' : '新增数据来源系统'"
      width="400px"
      append-to-body
      :close-on-click-modal="false"
    >
      <el-form :model="formData" label-width="120px" :rules="formRules" ref="formRef">
        <el-form-item label="系统名称" prop="systemName">
          <el-input v-model="formData.systemName" placeholder="请输入系统名称" />
        </el-form-item>
        <el-form-item label="说明" prop="description">
          <el-input
            v-model="formData.description"
            type="textarea"
            :rows="3"
            placeholder="请输入说明"
          />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="innerDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </div>
    </el-dialog>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, watch, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';

/**
 * 组件属性定义
 */
const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  }
});

/**
 * 组件事件定义
 */
const emit = defineEmits(['update:visible', 'select', 'cancel']);

// 主对话框显示状态
const dialogVisible = ref(false);
// 内部编辑/新增对话框显示状态
const innerDialogVisible = ref(false);
// 是否编辑模式标志
const isEditMode = ref(false);
// 搜索关键词
const searchKeyword = ref('');
// 表格数据
const tableData = ref([]);
// 当前选中的ID
const selectedId = ref(null);
// 当前选中的行数据
const selectedRow = ref(null);
// 表格组件引用
const tableRef = ref(null);
// 表单组件引用
const formRef = ref(null);

/**
 * 表单数据对象
 */
const formData = reactive({
  id: null,
  systemName: '',
  description: ''
});

/**
 * 表单验证规则
 */
const formRules = {
  systemName: [
    { required: true, message: '请输入系统名称', trigger: 'blur' }
  ]
};

// 搜索防抖计时器
let searchTimer = null;
// 是否拼音搜索标志
let isPinyinSearch = false;

/**
 * 监听组件visible属性变化
 * 控制对话框的显示和隐藏
 */
watch(() => props.visible, (val) => {
  dialogVisible.value = val;
  if (val) {
    loadData();
    selectedId.value = null;
    selectedRow.value = null;
  }
});

/**
 * 监听对话框显示状态变化
 * 更新组件的visible属性
 */
watch(dialogVisible, (val) => {
  emit('update:visible', val);
});

/**
 * 加载数据来源系统列表
 */
const loadData = async () => {
  try {
    const response = await fetch('/api/data-source-systems');
    if (response.ok) {
      const data = await response.json();
      tableData.value = Array.isArray(data) ? data : (data.data || []);
    }
  } catch (error) {
    console.error('Failed to load data source systems:', error);
    ElMessage.error('加载数据失败');
  }
};

/**
 * 搜索框输入事件处理
 * 防抖处理，判断是否使用拼音搜索
 * @param {string} value - 输入的搜索词
 */
const handleSearchInput = (value) => {
  if (searchTimer) {
    clearTimeout(searchTimer);
  }
  
  const trimmed = value.trim();
  if (trimmed.length > 0) {
    const isAllLetters = /^[a-zA-Z]+$/.test(trimmed);
    isPinyinSearch = isAllLetters;
    
    if (isAllLetters) {
      searchTimer = setTimeout(() => {
        handleSearch();
      }, 300);
    }
  } else {
    isPinyinSearch = false;
  }
};

/**
 * 执行搜索操作
 */
const handleSearch = async () => {
  if (!searchKeyword.value.trim()) {
    loadData();
    return;
  }
  
  try {
    let url = '/api/data-source-systems/search?keyword=' + encodeURIComponent(searchKeyword.value.trim());
    
    const response = await fetch(url);
    if (response.ok) {
      const data = await response.json();
      tableData.value = Array.isArray(data) ? data : (data.data || []);
    }
  } catch (error) {
    console.error('Failed to search:', error);
    ElMessage.error('查询失败');
  }
};

/**
 * 点击新增按钮
 * 打开新增对话框，清空表单
 */
const handleAdd = () => {
  isEditMode.value = false;
  formData.id = null;
  formData.systemName = '';
  formData.description = '';
  innerDialogVisible.value = true;
};

/**
 * 点击编辑按钮
 * 打开编辑对话框，填充表单数据
 * @param {Object} row - 要编辑的行数据
 */
const handleEdit = (row) => {
  isEditMode.value = true;
  formData.id = row.id;
  formData.systemName = row.systemName;
  formData.description = row.description || '';
  innerDialogVisible.value = true;
};

/**
 * 删除数据来源系统
 * @param {Object} row - 要删除的行数据
 */
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除数据来源系统"${row.systemName}"吗？`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    );
    
    const response = await fetch(`/api/data-source-systems/${row.id}`, {
      method: 'DELETE'
    });
    
    if (response.ok) {
      ElMessage.success('删除成功');
      loadData();
      if (selectedId.value === row.id) {
        selectedId.value = null;
        selectedRow.value = null;
      }
    } else {
      const result = await response.json();
      ElMessage.error(result.message || '删除失败');
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('Failed to delete:', error);
      ElMessage.error('删除失败');
    }
  }
};

/**
 * 提交新增或修改
 */
const handleSubmit = async () => {
  if (!formRef.value) return;
  
  try {
    await formRef.value.validate();
    
    const currentUser = JSON.parse(localStorage.getItem('user') || '{}');
    const userId = currentUser.userId;
    
    let response;
    if (isEditMode.value) {
      response = await fetch(`/api/data-source-systems/${formData.id}?userId=${userId || ''}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          systemName: formData.systemName,
          description: formData.description
        })
      });
    } else {
      response = await fetch(`/api/data-source-systems?userId=${userId || ''}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          systemName: formData.systemName,
          description: formData.description
        })
      });
    }
    
    if (response.ok) {
      const result = await response.json();
      if (result.success) {
        ElMessage.success(isEditMode.value ? '修改成功' : '新增成功');
        innerDialogVisible.value = false;
        loadData();
      } else {
        ElMessage.error(result.message || '操作失败');
      }
    } else {
      const result = await response.json();
      ElMessage.error(result.message || '操作失败');
    }
  } catch (error) {
    console.error('Failed to submit:', error);
    ElMessage.error('操作失败');
  }
};

/**
 * 单选按钮变化事件
 * 记录选中的行数据
 * @param {Object} row - 选中的行数据
 */
const handleRadioChange = (row) => {
  selectedRow.value = row;
};

/**
 * 双击行事件
 * 选择该行并关闭对话框
 * @param {Object} row - 双击的行数据
 */
const handleRowDblClick = (row) => {
  selectedId.value = row.id;
  selectedRow.value = row;
  handleSelect();
};

/**
 * 点击选择按钮
 * 返回选中的数据并关闭对话框
 */
const handleSelect = () => {
  if (selectedRow.value) {
    emit('select', {
      status: 'select',
      systemName: selectedRow.value.systemName
    });
    handleClose();
  } else {
    ElMessage.warning('请选择一条数据');
  }
};

/**
 * 点击取消按钮
 * 关闭对话框
 */
const handleCancel = () => {
  emit('cancel', { status: 'cancel' });
  handleClose();
};

/**
 * 关闭对话框
 * 重置搜索和选中状态
 */
const handleClose = () => {
  dialogVisible.value = false;
  searchKeyword.value = '';
  selectedId.value = null;
  selectedRow.value = null;
};

/**
 * 格式化日期显示
 * @param {string} dateString - 日期字符串
 * @returns {string} 格式化后的日期字符串
 */
const formatDate = (dateString) => {
  if (!dateString) return '';
  const date = new Date(dateString);
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  });
};
</script>

<style scoped>
.search-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.search-input {
  flex: 1;
}

.data-table {
  margin-bottom: 16px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
