<template>
  <div class="app-container">
    <header class="app-header">
      <div class="header-left">
        <div class="logo"></div>
        <span class="title">碳排放模型设计系统</span>
      </div>
      <div class="menu-bar">
        <el-button @click="handleBack" link>返回列表</el-button>
        <el-button @click="handleNewTemplate" link>新建模版</el-button>
        <el-button @click="handleSaveAs" link>另存为</el-button>
      </div>
      <div class="undo-redo-bar" v-if="false">
        <el-button 
          @click="handleUndo" 
          link 
          :disabled="undoStack.length === 0"
          icon="el-icon-undo"
        >撤销</el-button>
        <el-button 
          @click="handleRedo" 
          link 
          :disabled="redoStack.length === 0"
          icon="el-icon-redo"
        >重做</el-button>
      </div>
      <div class="header-right">
        <span class="template-name">{{ currentTemplate?.name || '未命名' }}</span>
        <el-button @click="handleSave" type="primary" v-if="false">保存</el-button>
        <el-button @click="handleCancel" v-if="false">取消</el-button>
      </div>
    </header>
    
    <div class="main-content">
      <div class="tree-panel">
        <div class="panel-header">
          <span class="panel-title">树状结构展栏</span>
        </div>
        <div class="tree-content">
          <tree-component
            :tree-data="treeData"
            :selected-node="selectedNode"
            :options="options"
            @select="handleNodeSelect"
            @add="handleAddNode"
            @addMultiple="handleAddMultipleCollectionNodes"
            @mountTemplate="handleMountTemplate"
            @delete="handleDeleteNode"
            @moveUp="handleMoveUp"
            @moveDown="handleMoveDown"
          />
        </div>
      </div>
      
      <div class="config-panel">
        <div class="panel-header">
          <span class="panel-title">属性显示栏</span>
          <div class="panel-actions">
            <el-button
              v-if="selectedNode && (selectedNode.typeId === 1 || selectedNode.typeId === 2)"
              size="small"
              @click="handleAddMultipleCollectionNodes(selectedNode)"
            >添加多个采集节点</el-button>
            <el-button
              v-if="selectedNode && (selectedNode.typeId === 1 || selectedNode.typeId === 2)"
              size="small"
              @click="handleMountTemplate(selectedNode)"
            >挂载节点模版</el-button>
            <el-button
              type="primary"
              size="small"
              :disabled="!selectedNode"
              @click="handleEditNode(selectedNode)"
            >编辑</el-button>
          </div>
        </div>
        <div class="config-content">
          <config-panel 
            v-if="selectedNode"
            :node="selectedNode"
            :options="options"
            :tree-data="treeData"
            @save="handleConfigSave"
            @cancel="handleConfigCancel"
            @update="handleNodeUpdate"
          />
          <div v-else class="empty-tip">
            <el-empty description="请选择一个节点查看配置" />
          </div>
        </div>
      </div>
    </div>
    
    <el-dialog
      v-model="showAddModal"
      :title="addModalTitle"
      width="800px"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      @close="resetAddForm"
    >
      <el-form :model="addForm" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="14">
            <el-form-item label="节点名称" prop="name" class="name-input-item">
              <el-input v-model="addForm.name" placeholder="请输入节点名称" />
              <el-button
                icon="plus"
                title="点击该按键自动添加名称前缀"
                @click="addNamePrefix"
              >添加前缀</el-button>
            </el-form-item>
          </el-col>
          <el-col :span="10">
            <el-form-item label="节点类型" prop="typeId">
              <el-select v-model="addForm.typeId" placeholder="请选择节点类型" :disabled="isEdit" style="width: 100%">
                <el-option v-if="isEdit && addForm.typeId === 1" label="根节点" :value="1" />
                <el-option v-if="addForm.parentNodeTypeId !== 4" :key="2" label="核算子节点" :value="2" />
                <el-option :key="3" label="排放数据采集点" :value="3" />
                <el-option v-if="addForm.parentNodeTypeId !== 4" :key="4" label="运输生产碳排放核算节点" :value="4" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-form-item v-if="addForm.typeId === 4" label="机车类型">
          <el-select v-model="addForm.locomotiveType" placeholder="请选择机车类型">
            <el-option v-for="type in options.locomotiveTypes" :key="type" :label="type" :value="type" />
          </el-select>
        </el-form-item>
        
        <template v-if="addForm.typeId === 2">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="节点编码">
                <el-input
                  v-model="addForm.nodeInfo.nodeCode"
                  placeholder="请输入节点编码"
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="节点名称简称">
                <el-input
                  v-model="addForm.nodeInfo.shortName"
                  placeholder="请输入节点名称简称"
                  :title="shortNameTooltip"
                />
                <div class="text-muted" style="width: 100%; margin-top: 4px;">该简称将用于自动生成子节点名称的前缀</div>
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="节点类型" label-width="100px">
                <el-select v-model="addForm.nodeInfo.nodeCategory" placeholder="请选择节点类型" style="width: 100%">
                  <el-option v-for="(value, key) in options.nodeCategories" :key="key" :label="value" :value="key" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="是否纳入碳排放核算" label-width="140px">
                <el-switch
                  v-model="addForm.nodeInfo.includeInCalculation"
                  :active-value="true"
                  :inactive-value="false"
                  active-text="是"
                  inactive-text="否"
                  @change="handleIncludeCalculationChange"
                />
              </el-form-item>
            </el-col>
          </el-row>
          
          <el-form-item label="单位说明">
            <el-input 
              v-model="addForm.nodeInfo.unitDescription" 
              type="textarea"
              :rows="2"
              placeholder="阐述单位职责、范围"
            />
          </el-form-item>
          
          <el-form-item label="组织边界说明">
            <el-input 
              v-model="addForm.nodeInfo.orgBoundaryDescription" 
              type="textarea"
              :rows="2"
              placeholder="明确哪些单位、区域纳入核算"
            />
          </el-form-item>
          
          <el-form-item label="运营边界说明">
            <el-input 
              v-model="addForm.nodeInfo.operationBoundaryDescription" 
              type="textarea"
              :rows="2"
              placeholder="明确自有、租赁、外包、代管设施处理口径"
            />
          </el-form-item>
        </template>
        
        <template v-if="addForm.typeId === 3 || addForm.typeId === 4">
          <!-- 1. 排放数据大类（下拉框）+ 选择采集点（按钮）+ 采集设备编号（输入框）一行并排（typeId=3）；
               排放数据大类整行显示（typeId=4） -->
        <el-row :gutter="20">
          <el-col :span="24" v-if="addForm.typeId === 4">
            <el-form-item label="排放数据大类">
              <el-select v-model="addForm.config.emissionCategory" placeholder="请选择排放数据大类" @change="handleCategoryChange">
                <el-option v-for="item in options.emissionCategories" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
          <template v-if="addForm.typeId === 3">
            <el-col :span="9">
              <el-form-item label="能耗品种大类" label-width="100px">
                <el-select v-model="addForm.config.emissionCategory" placeholder="请选择" @change="handleCategoryChange">
                  <el-option v-for="item in options.emissionCategories" :key="item" :label="item" :value="item" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="5">
              <el-form-item label-width="0">
                <el-button @click="openSelectCollectionPointDialog()">选择采集点</el-button>
              </el-form-item>
            </el-col>
            <el-col :span="10">
              <el-form-item label="采集设备编号" label-width="100px">
                <el-input v-model="addForm.config.equipmentCode" placeholder="系统自动生成" readonly />
              </el-form-item>
            </el-col>
          </template>
        </el-row>

          <!-- 3. 碳排放因子设置 + 碳排放因子单位（并排一行） -->
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="碳排放因子设置" label-width="120px">
                <div class="factor-input-group">
                  <el-input
                    v-model="addForm.config.carbonEmissionFactor"
                    placeholder="请输入碳排放因子"
                    @input="handleAddFactorInput"
                  />
                  <el-button
                    size="small"
                    @click="openAddFactorDialog"
                    title="选择碳排放因子"
                  >选择</el-button>
                </div>
              </el-form-item>
            </el-col>
            <el-col :span="10">
              <el-form-item label="碳排放因子单位" label-width="160px">
                <el-input
                  :value="addEmissionFactorUnit"
                  readonly
                  placeholder="选择碳排放因子后自动带出"
                />
              </el-form-item>
            </el-col>
          </el-row>

          <!-- 4. 碳排放因子描述 -->
          <el-form-item label="碳排放因子描述" label-width="120px">
            <el-input
              :value="addForm.config.carbonEmissionFactorDescription"
              type="textarea"
              :rows="2"
              readonly
              placeholder="碳排放因子描述信息"
            />
          </el-form-item>

          <!-- 5. 排放数据小类（仅typeId=4显示） -->
          <el-row :gutter="20" v-if="addForm.typeId === 4">
            <el-col :span="8">
              <el-form-item label="排放数据小类" label-width="100px">
                <el-select
                  v-model="addForm.config.emissionSubcategory"
                  placeholder="请选择"
                  @change="handleSubcategoryChange"
                  style="width: 100%"
                >
                  <el-option v-for="item in currentSubcategories" :key="item" :label="item" :value="item" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="采集点描述" v-if="addForm.typeId === 3">
            <el-input
              v-model="addForm.config.collectionDescription"
              type="textarea"
              :rows="2"
              placeholder="请输入采集点描述"
            />
          </el-form-item>

          <!-- 已选采集点信息（typeId=3，文字展示，来源于"选择采集点"返回的记录） -->
          <div v-if="addForm.typeId === 3 && selectedPointInfo" class="selected-point-info">
            <div class="selected-point-title">采集点信息</div>
            <el-descriptions :column="2" size="small" border>
              <el-descriptions-item label="名称">{{ selectedPointInfo.name || '-' }}</el-descriptions-item>
              <el-descriptions-item label="能耗品种小类">{{ pointSubcategoryText }}</el-descriptions-item>
              <el-descriptions-item label="能耗场景大类">{{ energyCategoryName(selectedPointInfo.energyCategoryL1) || '-' }}</el-descriptions-item>
              <el-descriptions-item label="能耗场景二级分类">{{ energyCategoryName(selectedPointInfo.energyCategoryL2) || '-' }}</el-descriptions-item>
              <el-descriptions-item label="能耗场景三级分类">{{ energyCategoryName(selectedPointInfo.energyCategoryL3) || '-' }}</el-descriptions-item>
              <el-descriptions-item label="是否累加量">{{ selectedPointInfo.isCumulative === 1 ? '是' : '否' }}</el-descriptions-item>
              <el-descriptions-item label="是否移动源">{{ selectedPointInfo.isMobileSource === 1 ? '是' : '否' }}</el-descriptions-item>
              <el-descriptions-item label="计量单位">{{ selectedPointInfo.measurementUnit || '-' }}</el-descriptions-item>
              <el-descriptions-item label="数据来源系统">{{ selectedPointInfo.dataSourceSystem || '-' }}</el-descriptions-item>
              <el-descriptions-item label="能耗用途分类">{{ selectedPointInfo.energyUseCategory || '-' }}</el-descriptions-item>
              <el-descriptions-item label="能耗数据划拨">{{ selectedPointInfo.energyAllocation || '-' }}</el-descriptions-item>
              <el-descriptions-item label="抄表方式">{{ selectedPointInfo.meterReadingMethod === 0 ? '人工抄表' : '自动抄表' }}</el-descriptions-item>
              <el-descriptions-item label="计费周期" :span="2">{{ formatBillingCycle(selectedPointInfo) }}</el-descriptions-item>
            </el-descriptions>
          </div>
        </template>
      </el-form>
      
      <div slot="footer" class="dialog-footer">
        <el-button @click="showAddModal = false">取消</el-button>
        <el-button type="primary" @click="confirmAddNode">确定</el-button>
      </div>
    </el-dialog>
    
    <el-dialog
      v-model="showDeleteModal"
      title="确认删除"
      width="400px"
    >
      <p>确定要删除该节点及其所有子节点吗？此操作不可恢复。</p>
      <div slot="footer" class="dialog-footer">
        <el-button @click="showDeleteModal = false">取消</el-button>
        <el-button type="danger" @click="confirmDeleteNode">确定删除</el-button>
      </div>
    </el-dialog>
    
    <el-dialog
      v-model="showNewTemplateModal"
      title="新建模版"
      width="400px"
    >
      <el-form :model="newTemplateForm">
        <el-form-item label="模版名称">
          <el-input v-model="newTemplateForm.name" placeholder="请输入模版名称" />
        </el-form-item>
        <el-form-item label="描述信息">
          <el-input v-model="newTemplateForm.description" type="textarea" placeholder="请输入描述信息" :rows="3" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="showNewTemplateModal = false">取消</el-button>
        <el-button type="primary" @click="confirmNewTemplate">确定</el-button>
      </div>
    </el-dialog>
    
    <el-dialog
      v-model="showSaveAsModal"
      title="另存为"
      width="400px"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
    >
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

    <CarbonEmissionFactorDialog
      v-model:visible="showAddFactorDialog"
      :emission-category="addForm.config.emissionCategory"
      :emission-subcategory="addForm.config.emissionSubcategory"
      @select="handleAddFactorSelect"
      @cancel="handleAddFactorCancel"
    />

    <!-- 能耗数据采集点选择弹窗（排放数据采集点 typeId=3） -->
    <CollectionPointSelectDialog
      v-model:visible="showCollectionPointDialog"
      :default-category="addForm.config.emissionCategory"
      :multiple="collectionPointMultiple"
      :existing-point-ids="existingCollectionPointIds"
      @select="handleCollectionPointSelect"
      @cancel="handleCollectionPointCancel"
    />

    <!-- 节点模版选择弹窗（挂载节点模版，typeId=1/2） -->
    <el-dialog
      v-model="showMountTemplateDialog"
      title="节点模版选择"
      width="600px"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
    >
      <div class="mount-template-tip" v-if="mountTargetNode">
        挂载到：<strong>{{ mountTargetNode.name }}</strong>
      </div>
      <el-table
        :data="nodeTemplateList"
        v-loading="mountTemplateLoading"
        highlight-current-row
        @current-change="handleMountTemplateCurrentChange"
        height="320"
      >
        <el-table-column prop="name" label="节点模版名称" min-width="180" />
        <el-table-column prop="description" label="描述" min-width="220" show-overflow-tooltip />
        <el-table-column label="创建时间" width="160">
          <template #default="{ row }">
            {{ formatTemplateTime(row.createdAt) }}
          </template>
        </el-table-column>
      </el-table>
      <div v-if="!mountTemplateLoading && nodeTemplateList.length === 0" class="mount-template-empty">
        暂无可用节点模版，请先在模版列表中创建节点模版。
      </div>
      <div slot="footer" class="dialog-footer">
        <el-button @click="showMountTemplateDialog = false">取消</el-button>
        <el-button
          type="primary"
          :disabled="selectedNodeTemplateId === null"
          @click="confirmMountTemplate"
        >确定挂载</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, defineProps, defineEmits } from 'vue';
import TreeComponent from './components/TreeComponent.vue';
import ConfigPanel from './components/ConfigPanel.vue';
import CarbonEmissionFactorDialog from './components/CarbonEmissionFactorDialog.vue';
import CollectionPointSelectDialog from './components/CollectionPointSelectDialog.vue';
import { ElMessage } from 'element-plus';
import { nodeApi, templateApi } from './api/auth';

const props = defineProps({
  currentTemplate: {
    type: Object,
    default: null
  }
});

const emit = defineEmits(['back']);

const treeData = ref([]);
const selectedNode = ref(null);
const options = reactive({
  locomotiveTypes: [],
  emissionCategories: [],
  emissionSubcategories: {},
  nodeCategories: {}
});

const MAX_HISTORY_SIZE = 10;
const undoStack = ref([]);
const redoStack = ref([]);

const saveTreeSnapshot = (action, nodeData, extra = {}) => {
  const snapshot = {
    action,
    nodeData: JSON.parse(JSON.stringify(nodeData)),
    extra: extra,
    timestamp: Date.now()
  };
  
  undoStack.value.push(snapshot);
  
  if (undoStack.value.length > MAX_HISTORY_SIZE) {
    undoStack.value.shift();
  }
  
  redoStack.value = [];
};

const handleUndo = () => {
  if (undoStack.value.length === 0) return;
  
  const snapshot = undoStack.value.pop();
  const reverseAction = snapshot.action === 'add' ? 'delete' : 
                        snapshot.action === 'delete' ? 'add' :
                        snapshot.action;
  redoStack.value.push({
    action: reverseAction,
    nodeData: snapshot.nodeData,
    extra: snapshot.extra,
    timestamp: Date.now()
  });
  
  treeData.value = snapshot.nodeData;
};

const handleRedo = () => {
  if (redoStack.value.length === 0) return;
  
  const snapshot = redoStack.value.pop();
  const reverseAction = snapshot.action === 'add' ? 'delete' : 
                        snapshot.action === 'delete' ? 'add' :
                        snapshot.action;
  undoStack.value.push({
    action: reverseAction,
    nodeData: snapshot.nodeData,
    extra: snapshot.extra,
    timestamp: Date.now()
  });
  
  treeData.value = snapshot.nodeData;
};

const showAddModal = ref(false);
const showDeleteModal = ref(false);
const showSaveAsModal = ref(false);
const showNewTemplateModal = ref(false);
const showAddFactorDialog = ref(false);
const showCollectionPointDialog = ref(false);
const showMountTemplateDialog = ref(false);
const mountTargetNode = ref(null);
const nodeTemplateList = ref([]);
const selectedNodeTemplateId = ref(null);
const mountTemplateLoading = ref(false);
// 采集点选择弹窗模式：false-单选，true-多选
const collectionPointMultiple = ref(false);
// 批量添加采集节点的父节点（点击树节点"添加多个采集节点"按钮时记录）
const batchParentNode = ref(null);
const addOriginalFactorValue = ref('');
const addModalTitle = ref('');
const deleteTargetNode = ref(null);
const saveAsForm = reactive({
  name: ''
});
const newTemplateForm = reactive({
  name: '',
  description: ''
});

const addForm = reactive({
  name: '',
  typeId: null,
  parentId: null,
  templateId: null,
  parentNodeTypeId: null,
  locomotiveType: '',
  // 编辑模式时记录被编辑节点 ID（null 表示新增模式）
  editNodeId: null,
  config: {
    emissionCategory: '',
    emissionSubcategory: '',
    carbonEmissionFactor: '',
    carbonEmissionFactorDescription: '',
    collectionDescription: '',
    equipmentCode: '',
    collectionPointType: null,
    collectionPointId: null
  },
  nodeInfo: {
    nodeCode: '',
    shortName: '',
    includeInCalculation: true,
    nodeCategory: '',
    unitDescription: '',
    orgBoundaryDescription: '',
    operationBoundaryDescription: ''
  }
});

const shortNameTooltip = '该简称将用于自动生成子节点名称的前缀';

/**
 * 根据排放数据大类计算当前可用的排放数据小类列表
 */
const currentSubcategories = computed(() => {
  const category = addForm.config.emissionCategory;
  return options.emissionSubcategories[category] || [];
});

const addEmissionFactorUnit = computed(() => {
  const category = addForm.config.emissionCategory;
  const subcategory = addForm.config.emissionSubcategory;
  
  if (category === '购入的电力' || category === '输出的电力') {
    return 'kgCO₂/kWh';
  }
  
  if (category === '化石燃料') {
    const tUnitFuels = ['烟煤', '褐煤', '焦炭', '石油焦', '原油', '燃料油', '汽油', '柴油', '液化天然气', '液化石油气'];
    const nm3UnitFuels = ['天然气', '高炉煤气', '转炉煤气', '焦炉煤气'];
    
    if (tUnitFuels.includes(subcategory)) {
      return 'tCO₂/t';
    } else if (nm3UnitFuels.includes(subcategory)) {
      return 'tCO₂/10⁴Nm³';
    }
  }
  
  if (category === '购入的热力') {
    if (subcategory === '热力数据') {
      return 'tCO₂/GJ';
    } else if (subcategory === '质量单位计量的蒸汽' || subcategory === '质量单位计量的热水') {
      return 'tCO₂/t';
    }
  }
  
  if (category === '废弃物处理') {
    if (subcategory === '废水处理排放') {
      return 'tGH₄/m³';
    } else if (subcategory === '固体废弃物处理排放') {
      return 'tCO₂/t';
    }
  }
  
  return '';
});

const loadTree = () => {
  if (props.currentTemplate) {
    nodeApi.getTree(props.currentTemplate.id).then(res => {
      treeData.value = res.data ? [res.data] : [];
    });
  }
};

const loadOptions = () => {
  nodeApi.getOptions().then(res => {
    options.locomotiveTypes = res.data.locomotiveTypes;
    options.emissionCategories = res.data.emissionCategories;
    options.emissionSubcategories = res.data.emissionSubcategories;
    options.nodeCategories = res.data.nodeCategories || {};
  }).catch(err => {
    console.error('Failed to load options from API, using default values');
  });
};

const handleIncludeCalculationChange = (value) => {
  if (!value) {
    if (!confirm('该节点及其下属子节点都将不纳入碳排放核算，是否确定？')) {
      addForm.nodeInfo.includeInCalculation = true;
    }
  }
};

onMounted(() => {
  loadTree();
  loadOptions();
});

const handleNodeSelect = (node) => {
  selectedNode.value = node;
};

const handleAddNode = (parentNode) => {
  addModalTitle.value = '添加子节点';
  addForm.editNodeId = null;
  addForm.parentId = parentNode.id;
  addForm.parentNodeTypeId = parentNode.typeId;
  addForm.templateId = parentNode.templateId;
  addForm.typeId = null;
  addForm.name = '';
  addForm.locomotiveType = '';
  addForm.config.emissionCategory = '';
  addForm.config.emissionSubcategory = '';
  addForm.config.carbonEmissionFactor = '';
  addForm.config.carbonEmissionFactorDescription = '';
  addForm.config.collectionPointType = null;
  addForm.config.collectionPointId = null;
  selectedPointInfo.value = null;
  addForm.nodeInfo = {
    nodeCode: '',
    shortName: '',
    includeInCalculation: true,
    nodeCategory: '',
    unitDescription: '',
    orgBoundaryDescription: '',
    operationBoundaryDescription: ''
  };
  showAddModal.value = true;
};

/**
 * 是否为编辑模式（true 表示当前打开的是"编辑节点"弹窗，false 表示"添加子节点"弹窗）
 * 依据 addForm.editNodeId 判定
 */
const isEdit = computed(() => addForm.editNodeId !== null);

/**
 * 点击右侧"属性显示栏"顶部的"编辑"按钮时触发
 * 使用当前选中节点的数据回填 addForm，并打开"编辑节点"弹窗
 * 弹窗与"添加子节点"复用同一套模板，通过 isEdit 控制差异点（如 typeId 禁用、确认时分发到 updateNode）
 * @param {Object} node 当前选中的节点
 */
const handleEditNode = (node) => {
  if (!node) return;
  addModalTitle.value = '编辑节点';
  addForm.editNodeId = node.id;
  addForm.parentId = node.parentId;
  addForm.parentNodeTypeId = node.parentId
    ? (findNodeById(treeData.value, node.parentId)?.typeId ?? null)
    : null;
  addForm.templateId = node.templateId;
  addForm.typeId = node.typeId;
  addForm.name = node.name || '';
  addForm.locomotiveType = node.locomotiveType || '';
  // 回填 config（typeId=3 或 4 才有 config）
  const cfg = node.config || {};
  addForm.config.emissionCategory = cfg.emissionCategory || '';
  addForm.config.emissionSubcategory = cfg.emissionSubcategory || '';
  addForm.config.carbonEmissionFactor = cfg.carbonEmissionFactor ?? '';
  addForm.config.carbonEmissionFactorDescription = cfg.carbonEmissionFactorDescription || '';
  addForm.config.collectionDescription = cfg.collectionDescription || '';
  addForm.config.equipmentCode = cfg.equipmentCode || '';
  addForm.config.collectionPointType = cfg.collectionPointType ?? null;
  addForm.config.collectionPointId = cfg.collectionPointId ?? null;
  // 回填已关联采集点的详细信息（typeId=3，用于弹窗文字展示）
  selectedPointInfo.value = null;
  if (addForm.typeId === 3 && addForm.config.collectionPointType && addForm.config.collectionPointId) {
    ensureEnergyCategoryMap();
    fetchPointInfo(addForm.config.collectionPointType, addForm.config.collectionPointId)
      .then((info) => { selectedPointInfo.value = info; });
  }
  // 回填 nodeInfo（typeId=2 才有 nodeInfo）
  const info = node.nodeInfo || {};
  addForm.nodeInfo = {
    nodeCode: info.nodeCode || '',
    shortName: info.shortName || '',
    includeInCalculation: info.includeInCalculation !== undefined ? info.includeInCalculation : true,
    nodeCategory: info.nodeCategory || '',
    unitDescription: info.unitDescription || '',
    orgBoundaryDescription: info.orgBoundaryDescription || '',
    operationBoundaryDescription: info.operationBoundaryDescription || ''
  };
  showAddModal.value = true;
};

const handleDeleteNode = (node) => {
  deleteTargetNode.value = node;
  showDeleteModal.value = true;
};

const handleMoveUp = (node) => {
  const snapshot = getCurrentTreeSnapshot();
  
  moveNode(node, -1);
  nodeApi.moveNode(node.id, 'up').catch(error => {
    console.error('节点上移失败:', error);
    alert('节点上移失败，请重试');
    // 回滚
    treeData.value = snapshot;
  });
  
  saveTreeSnapshot('moveUp', snapshot, { nodeId: node.id });
};

const handleMoveDown = (node) => {
  const snapshot = getCurrentTreeSnapshot();
  
  moveNode(node, 1);
  nodeApi.moveNode(node.id, 'down').catch(error => {
    console.error('节点下移失败:', error);
    alert('节点下移失败，请重试');
    // 回滚
    treeData.value = snapshot;
  });
  
  saveTreeSnapshot('moveDown', snapshot, { nodeId: node.id });
};

const moveNode = (node, direction) => {
  const moveInNodes = (nodes) => {
    for (let i = 0; i < nodes.length; i++) {
      if (nodes[i].id === node.id) {
        const newIndex = i + direction;
        if (newIndex >= 0 && newIndex < nodes.length) {
          const temp = nodes[i];
          nodes.splice(i, 1);
          nodes.splice(newIndex, 0, temp);
        }
        return true;
      }
      if (nodes[i].children && moveInNodes(nodes[i].children)) {
        return true;
      }
    }
    return false;
  };
  
  moveInNodes(treeData.value);
};

/**
 * 排放数据大类变化事件处理函数
 * 清空排放数据小类选择，生成设备编号（如果是排放数据采集点）
 */
const handleCategoryChange = () => {
  addForm.config.emissionSubcategory = '';
  // 大类变化后原关联采集点不再匹配，清除采集点关联与展示信息
  addForm.config.collectionPointType = null;
  addForm.config.collectionPointId = null;
  selectedPointInfo.value = null;
  // 仅在"新增"模式下生成新设备编号；编辑模式下保留节点已有的 equipmentCode
  // 如需在编辑模式下按新大类重新生成，应使用现有节点 ID 调用 /api/nodes/{id}/equipment-code
  if (!isEdit.value && addForm.typeId === 3 && addForm.config.emissionCategory) {
    generateEquipmentCode();
  }
};

/**
 * 排放数据小类变化事件处理函数
 */
const handleSubcategoryChange = () => {};

const generateEquipmentCode = () => {
  if (!addForm.config.emissionCategory) return;
  
  const parentId = addForm.parentId;
  if (!parentId) return;
  
  nodeApi.getNodeById(parentId).then(parentNode => {
    let nodeId = null;
    
    if (addForm.typeId === 3 && addForm.parentId) {
      fetch('/api/nodes/next-id')
        .then(res => res.json())
        .then(nextId => {
          nodeId = nextId;
          return fetch(`/api/nodes/${addForm.parentId}/equipment-code?emissionCategory=${encodeURIComponent(addForm.config.emissionCategory)}&newNodeId=${nodeId}`);
        })
        .then(res => res.text())
        .then(code => {
          addForm.config.equipmentCode = code;
        }).catch(() => {
          addForm.config.equipmentCode = '';
        });
    }
  }).catch(() => {
    addForm.config.equipmentCode = '';
  });
};

const resetAddForm = () => {
  addForm.name = '';
  addForm.typeId = null;
  addForm.editNodeId = null;
};

/**
 * 递归查找嵌套树结构中的节点
 * @param {Array} nodes - 节点数组（可能包含 children 子节点数组）
 * @param {number|string} id - 要查找的节点 ID
 * @returns {Object|null} - 找到的节点对象，未找到返回 null
 */
const findNodeById = (nodes, id) => {
  for (const node of nodes) {
    if (node.id === id) {
      return node;
    }
    if (node.children && node.children.length > 0) {
      const found = findNodeById(node.children, id);
      if (found) {
        return found;
      }
    }
  }
  return null;
};

/**
 * 自动添加节点名称前缀
 * 将父节点及其所有上级节点（根节点除外）的编码用 "-" 连接，添加到当前节点名称前面
 * 格式：一级子节点编码-二级子节点编码-...-本节点名称
 */
const addNamePrefix = () => {
  // 检查父节点ID是否存在
  if (!addForm.parentId) {
    return;
  }
  
  // 检查树数据是否有效
  if (!treeData.value || !Array.isArray(treeData.value)) {
    return;
  }
  
  // 查找父节点
  const parentNode = findNodeById(treeData.value, addForm.parentId);
  if (!parentNode) return;
  
  // 收集上级节点的编码（根节点除外）
  const prefixParts = [];
  let currentNode = parentNode;
  
  while (currentNode && currentNode.parentId !== null) {
    if (currentNode.nodeInfo && currentNode.nodeInfo.shortName) {
      prefixParts.unshift(currentNode.nodeInfo.shortName);
    }
    currentNode = findNodeById(treeData.value, currentNode.parentId);
  }
  
  // 如果有前缀，添加到名称前面
  if (prefixParts.length > 0) {
    const originalName = addForm.name;
    addForm.name = prefixParts.join('-') + '-' + (originalName || '');
  }
};

const confirmAddNode = () => {
  if (!addForm.name.trim()) {
    alert('请输入节点名称');
    return;
  }

  if (!addForm.typeId) {
    alert('请选择节点类型');
    return;
  }

  const snapshot = getCurrentTreeSnapshot();

  const currentUser = JSON.parse(localStorage.getItem('user') || '{}');
  // 构造与节点类型匹配的载荷：typeId=2 仅带 nodeInfo；typeId=3/4 仅带 config
  const nodeData = {
    name: addForm.name,
    typeId: addForm.typeId,
    parentId: addForm.parentId,
    templateId: addForm.templateId,
    locomotiveType: addForm.locomotiveType,
    config: (addForm.typeId === 3 || addForm.typeId === 4) ? {
      emissionCategory: addForm.config.emissionCategory,
      emissionSubcategory: addForm.config.emissionSubcategory,
      carbonEmissionFactor: addForm.config.carbonEmissionFactor ? parseFloat(addForm.config.carbonEmissionFactor) : null,
      carbonEmissionFactorDescription: addForm.config.carbonEmissionFactorDescription,
      collectionDescription: addForm.config.collectionDescription,
      equipmentCode: addForm.config.equipmentCode,
      collectionPointType: addForm.config.collectionPointType,
      collectionPointId: addForm.config.collectionPointId,
      updatedBy: currentUser.userId
    } : null,
    nodeInfo: addForm.typeId === 2 ? {
      nodeCode: addForm.nodeInfo.nodeCode,
      shortName: addForm.nodeInfo.shortName,
      includeInCalculation: addForm.nodeInfo.includeInCalculation,
      nodeCategory: addForm.nodeInfo.nodeCategory,
      unitDescription: addForm.nodeInfo.unitDescription,
      orgBoundaryDescription: addForm.nodeInfo.orgBoundaryDescription,
      operationBoundaryDescription: addForm.nodeInfo.operationBoundaryDescription
    } : null
  };

  // 编辑模式：调用 updateNode；新增模式：调用 createNode
  if (isEdit.value) {
    nodeApi.updateNode(addForm.editNodeId, {
      ...nodeData,
      updatedBy: currentUser.userId
    }).then(() => {
      saveTreeSnapshot('update', snapshot, { nodeId: addForm.editNodeId });
      loadTree();
      showAddModal.value = false;
      // 编辑完成后保持该节点选中，便于用户继续查看
      selectedNode.value = null;
    }).catch((error) => {
      console.error('更新节点失败:', error);
      alert('更新节点失败，请重试');
    });
  } else {
    nodeData.createdBy = currentUser.userId;
    nodeApi.createNode(nodeData).then(res => {
      saveTreeSnapshot('add', snapshot);
      loadTree();
      showAddModal.value = false;
    }).catch((error) => {
      console.error('创建节点失败:', error);
      alert('创建节点失败，请重试');
    });
  }
};

const confirmDeleteNode = () => {
  if (!deleteTargetNode.value) return;
  
  const snapshot = getCurrentTreeSnapshot();
  
  nodeApi.deleteNode(deleteTargetNode.value.id).then(() => {
    saveTreeSnapshot('delete', snapshot);
    loadTree();
    selectedNode.value = null;
    showDeleteModal.value = false;
  });
};

const getCurrentTreeSnapshot = () => {
  return JSON.parse(JSON.stringify(treeData.value));
};

const handleBack = () => {
  emit('back');
};

const handleNewTemplate = () => {
  newTemplateForm.name = '新建模版_' + Date.now();
  newTemplateForm.description = '';
  showNewTemplateModal.value = true;
};

const confirmNewTemplate = () => {
  if (!newTemplateForm.name.trim()) {
    alert('请输入模版名称');
    return;
  }
  const currentUser = JSON.parse(localStorage.getItem('user') || '{}');
  templateApi.createTemplate(newTemplateForm.name, currentUser.userId, newTemplateForm.description).then(res => {
    showNewTemplateModal.value = false;
    emit('back');
  });
};

const handleSaveAs = () => {
  saveAsForm.name = (props.currentTemplate?.name || '未命名') + '_副本';
  showSaveAsModal.value = true;
};

const confirmSaveAs = () => {
  if (!saveAsForm.name.trim()) {
    alert('请输入新模版名称');
    return;
  }
  const currentUser = JSON.parse(localStorage.getItem('user') || '{}');
  templateApi.copyTemplate(props.currentTemplate.id, saveAsForm.name, currentUser.userId).then(() => {
    showSaveAsModal.value = false;
    alert('另存为成功');
  });
};

/**
 * 格式化模版创建时间，用于"节点模版选择"弹窗表格展示
 * @param {string} time 时间字符串
 * @returns {string} 格式化后的时间字符串
 */
const formatTemplateTime = (time) => {
  if (!time) return '';
  return String(time).replace('T', ' ').substring(0, 19);
};

/**
 * 点击树节点"挂载节点模版"按钮（typeId=1/2 的"排放核算点"）时触发
 * 拉取全部模版并过滤出节点模版（templateType===1），打开"节点模版选择"弹窗
 * @param {Object} node 当前点击的目标父节点（挂载位置）
 */
const handleMountTemplate = (node) => {
  mountTargetNode.value = node;
  selectedNodeTemplateId.value = null;
  nodeTemplateList.value = [];
  showMountTemplateDialog.value = true;
  mountTemplateLoading.value = true;
  templateApi.getAllTemplates().then(res => {
    const list = (res.data || []).filter(t => t.templateType === 1);
    // 排除当前模版自身（不可将节点模版挂载到其自身所属的核算模版之外时无意义，且避免循环）
    nodeTemplateList.value = list;
  }).catch(err => {
    console.error('加载节点模版列表失败:', err);
    ElMessage.error('加载节点模版列表失败');
  }).finally(() => {
    mountTemplateLoading.value = false;
  });
};

/**
 * "节点模版选择"弹窗表格行选中变化回调（单选模式）
 * @param {Object} row 当前选中行
 */
const handleMountTemplateCurrentChange = (row) => {
  selectedNodeTemplateId.value = row ? row.id : null;
};

/**
 * 确认挂载节点模版
 * 调用后端 mountNodeTemplate 接口，将所选节点模版的全部子节点复制到目标父节点之下
 */
const confirmMountTemplate = () => {
  if (!mountTargetNode.value || selectedNodeTemplateId.value === null) return;
  const currentUser = JSON.parse(localStorage.getItem('user') || '{}');
  nodeApi.mountNodeTemplate(mountTargetNode.value.id, selectedNodeTemplateId.value, currentUser.userId)
    .then(() => {
      ElMessage.success('节点模版挂载成功');
      showMountTemplateDialog.value = false;
      loadTree();
    })
    .catch(err => {
      console.error('挂载节点模版失败:', err);
      const msg = err?.response?.data?.message || '挂载节点模版失败，请重试';
      ElMessage.error(msg);
    });
};

const handleSave = () => {
  alert('保存成功');
};

const handleCancel = () => {
  loadTree();
  selectedNode.value = null;
};

/**
 * 属性面板"保存"事件回调（当前仅记录日志，实际保存由 handleNodeUpdate 完成）
 */
const handleConfigSave = () => {
  console.log('Config saved');
};

/**
 * 属性面板"取消"事件回调
 */
const handleConfigCancel = () => {
  console.log('Config cancelled');
};

/**
 * 属性面板节点更新事件回调：调用节点更新接口保存节点名称/配置/核算信息后刷新树
 * @param {Object} data - 待更新的节点数据 { id, name, config, nodeInfo }
 */
const handleNodeUpdate = (data) => {
  const currentUser = JSON.parse(localStorage.getItem('user') || '{}');
  nodeApi.updateNode(data.id, {
    name: data.name,
    config: data.config ? { ...data.config, updatedBy: currentUser.userId } : null,
    nodeInfo: data.nodeInfo,
    updatedBy: currentUser.userId
  }).then(() => {
    loadTree();
  }).catch((error) => {
    console.error('更新节点失败:', error);
    alert('保存失败，请重试');
  });
};

/**
 * 打开碳排放因子选择对话框
 */
const openAddFactorDialog = () => {
  addOriginalFactorValue.value = addForm.config.carbonEmissionFactor;
  showAddFactorDialog.value = true;
};

/**
 * 打开"能耗数据采集点选择"对话框
 * <p>
 * 用于在"排放数据采集点"（typeId=3）的新增/编辑流程中，从既有采集点中选择一个作为数据来源，
 * 弹窗顶部可选择排放数据大类（PE-购入的电力 / PH-购入的热力 / FF-化石燃料），
 * 并按大类以"一级范围 → 二级范围 → 采集点"三列方式逐级选择；
 * 多选模式（multiple=true）下可勾选多个采集点并统一设置碳排放因子，用于批量创建子节点。
 * 选择确认后：
 *   1. 将所选大类同步到表单的"排放数据大类"字段（触发小类清空及设备编码刷新）
 *   2. 记录采集点关联信息（collectionPointType/collectionPointId），并在"采集点信息"面板中展示采集点详情
 * </p>
 * @param {Boolean} multiple - 是否多选模式：true=批量添加子节点，false/缺省=单选回填表单
 */
const openSelectCollectionPointDialog = (multiple = false) => {
  collectionPointMultiple.value = !!multiple;
  showCollectionPointDialog.value = true;
};

/**
 * 收集当前模版树中所有"排放数据采集点"（typeId=3）已关联的采集点
 * 供"能耗数据采集点选择"弹窗（多选）确定时做重复添加校验：
 * 每个采集点在同一碳排放模版中只能存在一个
 * <p>
 * 注意：仅遍历当前模版（props.currentTemplate）的树 treeData，
 * 其他模版中的采集点不影响本校验；
 * 元素为"{类型}:{采集点ID}"复合键（如 "1:5"），因为三张采集点表
 * （电力/化石燃料/外购热能）主键各自独立编号，仅按 ID 比对会把
 * 不同类型的同号采集点误判为重复
 */
const existingCollectionPointIds = computed(() => {
  const keys = [];
  const walk = (nodes) => {
    (nodes || []).forEach((n) => {
      if (!n) return;
      if (n.typeId === 3 && n.config && n.config.collectionPointId != null
          && n.config.collectionPointType != null) {
        keys.push(`${n.config.collectionPointType}:${n.config.collectionPointId}`);
      }
      if (Array.isArray(n.children) && n.children.length > 0) {
        walk(n.children);
      }
    });
  };
  walk(treeData.value);
  return keys;
});

/**
 * 采集点类型编码映射：购入的电力(PE)→1电力表、化石燃料(FF)→2化石燃料、购入的热力(PH)→3外购热能
 */
const collectionPointTypeMap = { PE: 1, FF: 2, PH: 3 };

/**
 * 能耗分类编码 → 分类名称映射（数据来自 /api/energy-categories，惰性加载一次并缓存）
 * 用于"采集点信息"面板把 energy_category_l1/l2/l3 编码翻译成具体分类内容
 */
const energyCategoryMap = ref({});
let energyCategoryMapLoaded = false;
const ensureEnergyCategoryMap = async () => {
  if (energyCategoryMapLoaded) return;
  energyCategoryMapLoaded = true;
  try {
    const response = await fetch('/api/energy-categories');
    if (!response.ok) throw new Error(`HTTP ${response.status}`);
    const items = await response.json();
    const map = {};
    (Array.isArray(items) ? items : []).forEach((it) => {
      if (it && it.categoryCode) {
        map[it.categoryCode] = it.categoryName || it.categoryCode;
      }
    });
    energyCategoryMap.value = map;
  } catch (e) {
    energyCategoryMapLoaded = false; // 加载失败允许下次重试
    console.error('Failed to load energy categories:', e);
  }
};

/**
 * 能耗分类编码转显示名称（未命中映射时回退显示原编码，空值返回空串）
 */
const energyCategoryName = (code) => {
  if (!code) return '';
  return energyCategoryMap.value[code] || code;
};

/**
 * 已选采集点完整信息（用于编辑弹窗中以文字方式展示采集点具体信息）
 */
const selectedPointInfo = ref(null);

/**
 * 采集点品种小类文字：电力表为 emissionSubcategory，化石燃料为 fuelType，外购热能为 heatType
 */
const pointSubcategoryText = computed(() => {
  const p = selectedPointInfo.value;
  if (!p) return '-';
  return p.emissionSubcategory || p.fuelType || p.heatType || '-';
});

/**
 * 根据采集点类型拉取采集点详情
 * @param {Number} type - 采集点类型：1-电力表，2-化石燃料，3-外购热能
 * @param {Number} id - 采集点ID
 * @returns {Promise<Object|null>} 采集点详情，获取失败返回 null
 */
const fetchPointInfo = async (type, id) => {
  if (!type || !id) return null;
  const urls = {
    1: `/api/meter-settings/node/meter/${id}`,
    2: `/api/fossil-fuel-collection/meters/${id}`,
    3: `/api/purchased-heat-collection/meters/${id}`
  };
  try {
    const response = await fetch(urls[type]);
    if (!response.ok) return null;
    return await response.json();
  } catch (e) {
    console.error('Failed to fetch collection point info:', e);
    return null;
  }
};

/**
 * 计费周期综合显示
 * 由 billing_cycle_unit（周期单位）、billing_cycle_start_date（相对第0天的偏移量）、
 * billing_cycle_length（周期数量）三个字段综合而成，如：每月第1天、每2年第12天
 */
const formatBillingCycle = (p) => {
  if (!p || p.billingCycleUnit == null) return '-';
  const units = { 1: '周', 2: '月', 3: '季度', 4: '年' };
  const unit = units[p.billingCycleUnit];
  if (!unit) return '-';
  const len = p.billingCycleLength ?? 1;
  const start = p.billingCycleStartDate ?? 0;
  return `每${len > 1 ? len : ''}${unit}第${start}天`;
};

/**
 * 树节点"添加多个采集节点"按钮点击回调（排放核算点 typeId=2）
 * 记录父节点后以多选模式打开"能耗数据采集点选择"弹窗，
 * 确认后为每个选中的采集点在该节点下批量创建"排放数据采集点"子节点
 * @param {Object} parentNode - 排放核算点节点
 */
const handleAddMultipleCollectionNodes = (parentNode) => {
  batchParentNode.value = parentNode;
  openSelectCollectionPointDialog(true);
};

/**
 * 批量创建"排放数据采集点"子节点
 * 按单选回填一致的规则为每个采集点构造节点数据：
 * 节点名称取采集点名称，配置中写入能耗品种大类、采集点类型/ID及采集点属性，
 * 碳排放因子设置/单位/描述也一并写入，设备编号按父节点编码规则逐个生成
 * @param {Array} points - 选中的采集点对象数组（含 categoryCode/categoryName）
 * @param {Object} factorInfo - 碳排放因子信息（carbonEmissionFactor/carbonEmissionFactorUnit/carbonEmissionFactorDescription）
 */
const handleBatchCreateCollectionNodes = async (points, factorInfo = {}) => {
  const parentNode = batchParentNode.value;
  if (!parentNode || !points || points.length === 0) return;
  const currentUser = JSON.parse(localStorage.getItem('user') || '{}');
  const factorValue = factorInfo.carbonEmissionFactor !== undefined && factorInfo.carbonEmissionFactor !== ''
    ? parseFloat(factorInfo.carbonEmissionFactor)
    : null;
  const factorDesc = factorInfo.carbonEmissionFactorDescription || '';
  let successCount = 0;
  for (const point of points) {
    try {
      // 生成设备编号：先取下一个节点ID，再按父节点编码规则生成
      let equipmentCode = '';
      try {
        const nextIdRes = await fetch('/api/nodes/next-id');
        if (nextIdRes.ok) {
          const nextId = await nextIdRes.json();
          const codeRes = await fetch(`/api/nodes/${parentNode.id}/equipment-code?emissionCategory=${encodeURIComponent(point.categoryName || '')}&newNodeId=${nextId}`);
          if (codeRes.ok) equipmentCode = await codeRes.text();
        }
      } catch (e) {
        console.error('生成设备编号失败:', e);
      }
      const nodeData = {
        name: point.name || point.purposeDescription || '采集点',
        typeId: 3,
        parentId: parentNode.id,
        templateId: parentNode.templateId,
        locomotiveType: '',
        config: {
          emissionCategory: point.categoryName || '',
          emissionSubcategory: point.emissionSubcategory || '',
          carbonEmissionFactor: factorValue,
          carbonEmissionFactorDescription: factorDesc,
          collectionDescription: '',
          equipmentCode,
          collectionPointType: collectionPointTypeMap[point.categoryCode] ?? null,
          collectionPointId: point.id,
          createdBy: currentUser.userId
        },
        nodeInfo: null,
        createdBy: currentUser.userId
      };
      await nodeApi.createNode(nodeData);
      successCount++;
    } catch (error) {
      console.error('创建采集节点失败:', error);
    }
  }
  loadTree();
  batchParentNode.value = null;
  if (successCount > 0) {
    ElMessage.success(`成功添加 ${successCount} 个采集节点`);
  }
  if (successCount < points.length) {
    ElMessage.warning(`${points.length - successCount} 个采集节点创建失败`);
  }
};

/**
 * 采集点选择确认回调
 * 单选模式下 point 为单个采集点对象；多选模式（批量）下 point 为对象，结构为：
 *   { categoryCode, categoryName, carbonEmissionFactor, carbonEmissionFactorUnit, carbonEmissionFactorDescription, points: [...] }
 * 多选模式（非批量，由表单"选择采集点"触发）下 point 为对象数组（兼容旧结构）
 * @param {Object|Array} point - 选中的采集点数据
 */
const handleCollectionPointSelect = (point) => {
  // 批量模式：为树节点批量创建"排放数据采集点"子节点
  if (collectionPointMultiple.value && batchParentNode.value) {
    showCollectionPointDialog.value = false;
    // 新结构：{ points, carbonEmissionFactor, ... }
    if (point && typeof point === 'object' && !Array.isArray(point) && Array.isArray(point.points)) {
      const factorInfo = {
        carbonEmissionFactor: point.carbonEmissionFactor,
        carbonEmissionFactorUnit: point.carbonEmissionFactorUnit,
        carbonEmissionFactorDescription: point.carbonEmissionFactorDescription
      };
      handleBatchCreateCollectionNodes(point.points, factorInfo);
    } else {
      // 兼容旧结构
      const points = Array.isArray(point) ? point : [point];
      handleBatchCreateCollectionNodes(points);
    }
    return;
  }
  // 单选模式或表单内多选回填：取首个采集点
  const firstPoint = Array.isArray(point) ? point[0] : point;
  if (!firstPoint) {
    showCollectionPointDialog.value = false;
    return;
  }
  // 1. 同步排放数据大类（复用大类变化处理：清空小类、必要时生成设备编号）
  if (firstPoint.categoryName && addForm.config.emissionCategory !== firstPoint.categoryName) {
    addForm.config.emissionCategory = firstPoint.categoryName;
    handleCategoryChange();
  }
  // 2. 记录采集点关联（类型 + ID）及完整信息（用于弹窗文字展示）
  //    注意：需在大类同步（handleCategoryChange 会清除关联）之后设置
  addForm.config.collectionPointType = collectionPointTypeMap[firstPoint.categoryCode] ?? null;
  addForm.config.collectionPointId = firstPoint.id ?? null;
  // 惰性加载能耗分类编码映射（面板中 L1/L2/L3 编码翻译为分类名称）
  ensureEnergyCategoryMap();
  selectedPointInfo.value = firstPoint;
  ElMessage.success(`已选择采集点：${firstPoint.name || firstPoint.purposeDescription || '未知采集点'}`);
  showCollectionPointDialog.value = false;
};

/**
 * 采集点选择取消回调
 */
const handleCollectionPointCancel = () => {
  batchParentNode.value = null;
  showCollectionPointDialog.value = false;
};

/**
 * 排放因子选择弹窗"选择"回调：将所选因子值与描述回填到新增表单
 * @param {Object} result - 因子选择结果 { factorValue, description }
 */
const handleAddFactorSelect = (result) => {
  if (result.factorValue) {
    addForm.config.carbonEmissionFactor = result.factorValue;
    addForm.config.carbonEmissionFactorDescription = result.description || '';
  }
  showAddFactorDialog.value = false;
};

/**
 * 排放因子选择弹窗"取消"回调：关闭弹窗，不改动表单
 */
const handleAddFactorCancel = () => {
  showAddFactorDialog.value = false;
};

/**
 * 排放因子手动输入监听：与打开弹窗时的原始值比对，
 * 若用户手动修改了因子值，则在描述中追加"用户手动修改排放因子数据"说明
 */
const handleAddFactorInput = () => {
  const currentValue = addForm.config.carbonEmissionFactor;
  if (currentValue !== addOriginalFactorValue.value) {
    addForm.config.carbonEmissionFactorDescription = `${currentValue}用户手动修改排放因子数据`;
  }
};
</script>

<style scoped>
.app-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f5f5;
}

.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  height: 60px;
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo {
  width: 36px;
  height: 36px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 6px;
}

.title {
  font-size: 18px;
  font-weight: bold;
  color: #1f2329;
}

.menu-bar {
  display: flex;
  gap: 12px;
}

.undo-redo-bar {
  display: flex;
  gap: 12px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.template-name {
  font-size: 14px;
  color: #8c8c8c;
}

.main-content {
  flex: 1;
  display: flex;
  padding: 16px;
  gap: 16px;
  overflow: hidden;
}

.tree-panel {
  width: 525px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  display: flex;
  flex-direction: column;
}

.config-panel {
  flex: 1;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  display: flex;
  flex-direction: column;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid #e8eaec;
}

.panel-title {
  font-size: 14px;
  font-weight: 600;
  color: #1f2329;
}

.panel-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.tree-content {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.config-content {
  flex: 1;
  overflow-y: auto;
}

.empty-tip {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 200px;
}

.text-muted {
  color: #8c8c8c;
  font-size: 12px;
  margin-left: 8px;
}

/* 注意：不在 .name-input-item 根元素本身设置 flex/gap，否则会把 gap 错误地作用在 label 与 content 之间 */
/* 正确做法：用 :deep() 穿透到 Element Plus 的 .el-form-item__content 容器，让 input 与 button 在该容器内并排且间距 10px */
.name-input-item :deep(.el-form-item__content) {
  display: flex;
  align-items: center;
  gap: 10px;
}

.name-input-item :deep(.el-input) {
  flex: 1;
}

.name-input-item :deep(.el-button) {
  width: 80px;
  text-align: center; /* 确保水平居中 */
  padding-left: 1px;
  padding-right: 12px;
}

.input-with-button {
  display: flex;
  width: 100%;
  max-width: 300px;
  gap: 8px;
}

/* 已选采集点信息展示区（编辑节点弹窗，typeId=3） */
.selected-point-info {
  margin: 0 0 18px 0;
  padding: 10px 12px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  background-color: #fafcff;
}

.selected-point-title {
  font-size: 13px;
  font-weight: 600;
  color: #409eff;
  margin-bottom: 8px;
}

.input-with-button .el-input {
  flex: 1;
}

.factor-input-group {
  display: flex;
  gap: 8px;
  align-items: center;
}

.factor-input-group .el-input {
  flex: 1;
}

/* "节点模版选择"弹窗样式 */
.mount-template-tip {
  margin-bottom: 12px;
  font-size: 14px;
  color: #595959;
}

.mount-template-tip strong {
  color: #1890ff;
}

.mount-template-empty {
  text-align: center;
  color: #8c8c8c;
  padding: 24px 0;
  font-size: 13px;
}
</style>
