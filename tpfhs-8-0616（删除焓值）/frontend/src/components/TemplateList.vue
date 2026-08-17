<template>
  <div class="template-list-container">
    <div class="header">
      <h1>碳排放核算模版管理</h1>
      <div class="user-info">
        <span>当前用户: {{ currentUser?.nickName || currentUser?.name || currentUser?.userName }}</span>
        <el-button v-if="!embedded" @click="handleLogout" link>退出登录</el-button>
      </div>
    </div>

    <div class="toolbar">
      <el-button @click="handleNewTemplate" type="primary">新建模版</el-button>
      <el-button @click="handleOpenTemplate" :disabled="!selectedTemplate">打开模版</el-button>
      <el-button @click="handleSaveAs" :disabled="!selectedTemplate">另存为</el-button>
      <el-button @click="handleDeleteTemplate" :disabled="!selectedTemplate" type="danger">删除当前模版</el-button>
    </div>

    <div class="template-list">
      <table>
        <thead>
          <tr>
            <th>选择</th>
            <th>模版名称</th>
            <th>创建人</th>
            <th>创建时间</th>
            <th>最后修改人</th>
            <th>最后修改时间</th>
            <th>版本</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="template in templates"
            :key="template.id"
            :class="{ selected: selectedTemplate?.id === template.id }"
            @click="selectTemplate(template)"
            @dblclick="handleDoubleClick(template)"
          >
            <td><el-radio :value="template.id" v-model="selectedId" /></td>
            <td>{{ template.name }}</td>
            <td>{{ template.createdByName }}</td>
            <td>{{ formatDate(template.createdAt) }}</td>
            <td>{{ template.updatedByName }}</td>
            <td>{{ formatDate(template.updatedAt) }}</td>
            <td>{{ template.version }}</td>
            <td>
              <el-tag :type="template.enabled ? 'success' : 'danger'">
                {{ template.enabled ? '使能' : '禁止' }}
              </el-tag>
            </td>
            <td>
              <el-button
                @click.stop="handleEditTemplate(template)"
                link
                size="small"
              >编辑</el-button>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-if="templates.length === 0" class="empty-tip">
        <el-empty description="暂无模版" />
      </div>
    </div>

    <el-dialog v-model="showNewModal" title="新建模版" width="400px">
      <el-form :model="newTemplateForm">
        <el-form-item label="模版名称">
          <el-input v-model="newTemplateForm.name" placeholder="请输入模版名称" />
        </el-form-item>
        <el-form-item label="描述信息">
          <el-input v-model="newTemplateForm.description" type="textarea" placeholder="请输入描述信息" :rows="3" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="showNewModal = false">取消</el-button>
        <el-button type="primary" @click="confirmNewTemplate">确定</el-button>
      </div>
    </el-dialog>

    <el-dialog v-model="showSaveAsModal" title="另存为" width="400px">
      <el-form :model="saveAsForm">
        <el-form-item label="新模版名称">
          <el-input v-model="saveAsForm.name" placeholder="请输入新模版名称" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="showSaveAsModal = false">取消</el-button>
        <el-button type="primary" @click="confirmSaveAs">确定</el-button>
      </div>
    </el-dialog>

    <el-dialog v-model="showDeleteConfirm" title="确认删除">
      <p>确定要删除该模版吗？此操作不可恢复。</p>
      <div slot="footer" class="dialog-footer">
        <el-button @click="showDeleteConfirm = false">取消</el-button>
        <el-button type="danger" @click="confirmDelete">确定删除</el-button>
      </div>
    </el-dialog>

    <el-dialog v-model="showEditModal" title="编辑模版属性" width="600px">
      <el-form :model="editForm" label-width="120px">
        <el-form-item label="模版名称">
          <el-input v-model="editForm.name" disabled />
        </el-form-item>

        <el-form-item label="描述信息">
          <el-input v-model="editForm.description" type="textarea" placeholder="请输入描述信息" :rows="3" />
        </el-form-item>

        <el-form-item label="是否使能">
          <el-radio-group v-model="editForm.enabled">
            <el-radio :value="true">使能</el-radio>
            <el-radio :value="false">禁止</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="计划任务设置">
          <el-collapse v-model="taskCollapse">
            <el-collapse-item title="计划任务配置" name="1">
              <el-form :model="editForm.taskConfig" label-width="100px">
                <el-form-item label="起始日期">
                  <el-date-picker
                    v-model="editForm.taskConfig.startDate"
                    type="datetime"
                    placeholder="选择起始日期"
                    format="YYYY-MM-DD HH:mm"
                    value-format="YYYY-MM-DDTHH:mm"
                  />
                </el-form-item>

                <el-form-item label="截止日期">
                  <el-date-picker
                    v-model="editForm.taskConfig.endDate"
                    type="datetime"
                    placeholder="选择截止日期（可选）"
                    format="YYYY-MM-DD HH:mm"
                    value-format="YYYY-MM-DDTHH:mm"
                  />
                  <span v-if="!editForm.taskConfig.endDate" class="text-muted">无限制</span>
                </el-form-item>

                <el-form-item label="执行周期">
                  <el-select v-model="editForm.taskConfig.cycleType" @change="handleCycleChange">
                    <el-option label="按日" value="DAILY" />
                    <el-option label="按周" value="WEEKLY" />
                    <el-option label="按月" value="MONTHLY" />
                    <el-option label="按季" value="QUARTERLY" />
                    <el-option label="按年" value="YEARLY" />
                  </el-select>
                </el-form-item>

                <el-form-item v-if="editForm.taskConfig.cycleType === 'WEEKLY'" label="周执行日期">
                  <el-checkbox-group v-model="editForm.taskConfig.weekDays">
                    <el-checkbox label="周一" value="1" />
                    <el-checkbox label="周二" value="2" />
                    <el-checkbox label="周三" value="3" />
                    <el-checkbox label="周四" value="4" />
                    <el-checkbox label="周五" value="5" />
                    <el-checkbox label="周六" value="6" />
                    <el-checkbox label="周日" value="0" />
                  </el-checkbox-group>
                </el-form-item>

                <el-form-item v-if="editForm.taskConfig.cycleType === 'WEEKLY'" label="重复间隔">
                  <el-input v-model="editForm.taskConfig.interval" type="number" placeholder="每N周执行一次" />
                </el-form-item>

                <el-form-item v-if="editForm.taskConfig.cycleType === 'MONTHLY' || editForm.taskConfig.cycleType === 'QUARTERLY'" label="月执行日期">
                  <el-input v-model="editForm.taskConfig.monthDays" placeholder="如：1,5,15" />
                  <span class="text-muted">多个日期用逗号分隔</span>
                </el-form-item>

                <el-form-item v-if="editForm.taskConfig.cycleType === 'YEARLY'" label="年执行日期">
                  <el-input v-model="editForm.taskConfig.yearMonths" placeholder="如：1-15,6-15" />
                  <span class="text-muted">格式：月份-日期，多个用逗号分隔</span>
                </el-form-item>

                <el-form-item label="执行时间">
                  <el-time-picker
                    v-model="editForm.taskConfig.executionTime"
                    format="HH:mm"
                    value-format="HH:mm"
                    :editable="true"
                    style="width: 100%"
                  />
                </el-form-item>

                <el-form-item v-if="editForm.taskConfig.cycleType === 'MONTHLY'" label="重复间隔">
                  <el-input v-model="editForm.taskConfig.interval" type="number" placeholder="每N个月执行一次" />
                </el-form-item>
              </el-form>
            </el-collapse-item>
          </el-collapse>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="showEditModal = false">取消</el-button>
        <el-button type="primary" @click="confirmEdit">确定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { templateApi } from '../api/auth';

const props = defineProps({
  currentUser: {
    type: Object,
    default: null
  },
  embedded: {
    type: Boolean,
    default: false
  }
});

const emit = defineEmits(['open-template', 'logout']);

const templates = ref([]);
const selectedTemplate = ref(null);
const selectedId = ref(null);

const showNewModal = ref(false);
const showSaveAsModal = ref(false);
const showDeleteConfirm = ref(false);
const showEditModal = ref(false);
const taskCollapse = ref(['1']);

const newTemplateForm = ref({
  name: '',
  description: ''
});

const saveAsForm = ref({
  name: ''
});

const editForm = ref({
  id: null,
  name: '',
  description: '',
  enabled: true,
  taskConfig: {
    startDate: null,
    endDate: null,
    cycleType: 'DAILY',
    weekDays: [],
    monthDays: '',
    yearMonths: '',
    executionTime: '02:00',
    interval: 1
  }
});

const loadTemplates = () => {
  templateApi.getAllTemplates().then(res => {
    templates.value = res.data;
  });
};

onMounted(() => {
  loadTemplates();
});

const selectTemplate = (template) => {
  selectedTemplate.value = template;
  selectedId.value = template.id;
};

const handleDoubleClick = (template) => {
  emit('open-template', template);
};

const handleNewTemplate = () => {
  newTemplateForm.value = {
    name: '',
    description: ''
  };
  showNewModal.value = true;
};

const confirmNewTemplate = () => {
  if (!newTemplateForm.value.name.trim()) {
    alert('请输入模版名称');
    return;
  }
  templateApi.createTemplate(newTemplateForm.value.name, props.currentUser?.userId, newTemplateForm.value.description).then(() => {
    showNewModal.value = false;
    loadTemplates();
    alert('新建成功');
  });
};

const handleOpenTemplate = () => {
  if (selectedTemplate.value) {
    emit('open-template', selectedTemplate.value);
  }
};

const handleSaveAs = () => {
  if (selectedTemplate.value) {
    saveAsForm.value = {
      name: selectedTemplate.value.name + '_副本'
    };
    showSaveAsModal.value = true;
  }
};

const confirmSaveAs = () => {
  if (!saveAsForm.value.name.trim()) {
    alert('请输入新模版名称');
    return;
  }
  templateApi.copyTemplate(selectedTemplate.value.id, saveAsForm.value.name, props.currentUser?.userId).then(() => {
    showSaveAsModal.value = false;
    loadTemplates();
    alert('另存为成功');
  });
};

const handleDeleteTemplate = () => {
  if (selectedTemplate.value) {
    showDeleteConfirm.value = true;
  }
};

const confirmDelete = () => {
  templateApi.deleteTemplate(selectedTemplate.value.id).then(() => {
    showDeleteConfirm.value = false;
    loadTemplates();
    selectedTemplate.value = null;
    selectedId.value = null;
    alert('删除成功');
  });
};

const handleLogout = () => {
  localStorage.removeItem('user');
  emit('logout');
};

const formatDate = (dateStr) => {
  if (!dateStr) return '';
  const date = new Date(dateStr);
  return date.toLocaleString('zh-CN');
};

const initEditForm = () => {
  editForm.value = {
    id: null,
    name: '',
    description: '',
    enabled: true,
    taskConfig: {
      startDate: null,
      endDate: null,
      cycleType: 'DAILY',
      weekDays: [],
      monthDays: '',
      yearMonths: '',
      executionTime: '02:00',
      interval: 1
    }
  };
};

const handleEditTemplate = (template) => {
  initEditForm();

  editForm.value.id = template.id;
  editForm.value.name = template.name;
  editForm.value.description = template.description || '';
  editForm.value.enabled = template.enabled !== undefined ? template.enabled : true;

  if (template.taskConfig && template.taskConfig.trim()) {
    try {
      const taskConfig = JSON.parse(template.taskConfig);
      editForm.value.taskConfig = {
        startDate: taskConfig.startDate || null,
        endDate: taskConfig.endDate || null,
        cycleType: taskConfig.cycleType || 'DAILY',
        weekDays: taskConfig.weekDays || [],
        monthDays: taskConfig.monthDays || '',
        yearMonths: taskConfig.yearMonths || '',
        executionTime: taskConfig.executionTime || '02:00',
        interval: taskConfig.interval || 1
      };
    } catch (e) {
      console.error('Failed to parse taskConfig:', e);
    }
  }

  showEditModal.value = true;
};

const handleCycleChange = () => {
  const cycleType = editForm.value.taskConfig.cycleType;
  if (cycleType !== 'WEEKLY') {
    editForm.value.taskConfig.weekDays = [];
  }
  if (cycleType !== 'MONTHLY' && cycleType !== 'QUARTERLY') {
    editForm.value.taskConfig.monthDays = '';
  }
  if (cycleType !== 'YEARLY') {
    editForm.value.taskConfig.yearMonths = '';
  }
};

const confirmEdit = () => {
  const currentUser = JSON.parse(localStorage.getItem('user') || '{}');
  templateApi.updateTemplateProperties(
    editForm.value.id,
    editForm.value.description,
    editForm.value.enabled,
    JSON.stringify(editForm.value.taskConfig),
    currentUser.userId
  ).then(() => {
    showEditModal.value = false;
    loadTemplates();
    alert('编辑成功');
  });
};
</script>

<style scoped>
.template-list-container {
  padding: 20px;
  min-height: 100vh;
  background-color: #f5f7fa;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.header h1 {
  font-size: 24px;
  color: #1f2329;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 14px;
}

.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.template-list {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  padding: 20px;
}

.template-list table {
  width: 100%;
  border-collapse: collapse;
}

.template-list th,
.template-list td {
  padding: 12px;
  text-align: left;
  border-bottom: 1px solid #e8eaec;
}

.template-list th {
  background-color: #f5f7fa;
  font-weight: 600;
  color: #595959;
}

.template-list tbody tr:hover {
  background-color: #f5f7fa;
}

.template-list tbody tr.selected {
  background-color: #e6f7ff;
}

.empty-tip {
  padding: 40px;
  text-align: center;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.text-muted {
  color: #8c8c8c;
  font-size: 12px;
  margin-left: 8px;
}
</style>
