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
            @delete="handleDeleteNode"
            @moveUp="handleMoveUp"
            @moveDown="handleMoveDown"
          />
        </div>
      </div>
      
      <div class="config-panel">
        <div class="panel-header">
          <span class="panel-title">属性显示配置栏</span>
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
      width="400px"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      @close="resetAddForm"
    >
      <el-form :model="addForm" label-width="100px">
        <el-form-item label="节点名称" prop="name" class="name-input-item">
          <el-input v-model="addForm.name" placeholder="请输入节点名称" />
          <el-button 
            size="small" 
            icon="plus" 
            title="点击该按键自动添加名称前缀"
            @click="addNamePrefix"
          />
        </el-form-item>
        
        <el-form-item label="节点类型" prop="typeId">
          <el-select v-model="addForm.typeId" placeholder="请选择节点类型">
            <el-option v-if="addForm.parentNodeTypeId !== 4" :key="2" label="核算子节点" :value="2" />
            <el-option :key="3" label="排放数据采集点" :value="3" />
            <el-option v-if="addForm.parentNodeTypeId !== 4" :key="4" label="运输生产碳排放核算节点" :value="4" />
          </el-select>
        </el-form-item>
        
        <el-form-item v-if="addForm.typeId === 4" label="机车类型">
          <el-select v-model="addForm.locomotiveType" placeholder="请选择机车类型">
            <el-option v-for="type in options.locomotiveTypes" :key="type" :label="type" :value="type" />
          </el-select>
        </el-form-item>
        
        <template v-if="addForm.typeId === 2">
          <el-form-item label="节点编码">
            <el-input 
              v-model="addForm.nodeInfo.nodeCode" 
              placeholder="请输入节点编码"
            />
          </el-form-item>
          
          <el-form-item label="节点名称简称">
            <el-input 
              v-model="addForm.nodeInfo.shortName" 
              placeholder="请输入节点名称简称"
              :title="shortNameTooltip"
            />
            <span class="text-muted">该简称将用于自动生成子节点名称的前缀</span>
          </el-form-item>
          
          <el-form-item label="是否纳入碳排放核算">
            <el-switch 
              v-model="addForm.nodeInfo.includeInCalculation" 
              :active-value="true" 
              :inactive-value="false"
              active-text="是"
              inactive-text="否"
              @change="handleIncludeCalculationChange"
            />
          </el-form-item>
          
          <el-form-item label="节点类型">
            <el-select v-model="addForm.nodeInfo.nodeCategory" placeholder="请选择节点类型">
              <el-option v-for="(value, key) in options.nodeCategories" :key="key" :label="value" :value="key" />
            </el-select>
          </el-form-item>
          
          <el-form-item label="单位说明">
            <el-input 
              v-model="addForm.nodeInfo.unitDescription" 
              type="textarea"
              :rows="3"
              placeholder="阐述单位职责、范围"
            />
          </el-form-item>
          
          <el-form-item label="组织边界说明">
            <el-input 
              v-model="addForm.nodeInfo.orgBoundaryDescription" 
              type="textarea"
              :rows="3"
              placeholder="明确哪些单位、区域纳入核算"
            />
          </el-form-item>
          
          <el-form-item label="运营边界说明">
            <el-input 
              v-model="addForm.nodeInfo.operationBoundaryDescription" 
              type="textarea"
              :rows="3"
              placeholder="明确自有、租赁、外包、代管设施处理口径"
            />
          </el-form-item>
        </template>
        
        <template v-if="addForm.typeId === 3 || addForm.typeId === 4">
          <el-form-item label="统计口径类型">
            <el-select v-model="addForm.config.statisticalCaliber" placeholder="请选择统计口径类型">
              <el-option v-for="item in options.statisticalCalibers" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
          
          <el-form-item label="排放数据大类">
            <el-select v-model="addForm.config.emissionCategory" placeholder="请选择排放数据大类" @change="handleCategoryChange">
              <el-option v-for="item in options.emissionCategories" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
          
          <el-form-item label="采集设备编号" v-if="addForm.typeId === 3">
            <el-input v-model="addForm.config.equipmentCode" placeholder="系统自动生成" readonly />
          </el-form-item>
          
          <el-form-item label="排放数据小类">
            <el-select v-model="addForm.config.emissionSubcategory" placeholder="请选择排放数据小类" @change="handleSubcategoryChange">
              <el-option v-for="item in currentSubcategories" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
          
          <el-form-item label="碳排放因子设置">
            <div class="factor-input-group">
              <el-input 
                v-model="addForm.config.carbonEmissionFactor" 
                placeholder="请输入碳排放因子" 
                @input="handleAddFactorInput"
              />
              <el-button 
                size="small" 
                @click="openAddFactorDialog"
                :disabled="!addForm.config.emissionCategory || !addForm.config.emissionSubcategory"
                title="选择碳排放因子"
              >选择</el-button>
            </div>
          </el-form-item>
          
          <el-form-item label="单位">
            <el-tag type="info">{{ addEmissionFactorUnit }}</el-tag>
          </el-form-item>
          
          <el-form-item label="碳排放因子描述">
            <el-input 
              :value="addForm.config.carbonEmissionFactorDescription" 
              type="textarea"
              :rows="2"
              readonly
              placeholder="碳排放因子描述信息"
            />
          </el-form-item>
          
          <el-form-item label="数据来源">
            <el-select v-model="addForm.config.dataSource" placeholder="请选择数据来源">
              <el-option v-for="item in options.dataSources" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
          
          <template v-if="addForm.typeId === 3">
            <el-form-item label="核算场景">
              <el-select v-model="addForm.config.accountingScenario" placeholder="请选择核算场景" filterable>
                <el-option v-for="item in options.accountingScenarios" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
            
            <el-form-item label="能耗用途">
              <el-select v-model="addForm.config.energyUse" placeholder="请选择能耗用途" filterable>
                <el-option v-for="item in options.energyUses" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
            
            <el-form-item label="是否累计量">
              <el-select v-model="addForm.config.isCumulative" placeholder="请选择">
                <el-option label="是" value="true" />
                <el-option label="否" value="false" />
              </el-select>
            </el-form-item>
            
            <el-form-item label="是否移动源">
              <el-select v-model="addForm.config.isMobileSource" placeholder="请选择">
                <el-option label="是" value="true" />
                <el-option label="否" value="false" />
              </el-select>
            </el-form-item>
            
            <el-form-item label="计量单位">
              <el-select v-model="addForm.config.measurementUnit" placeholder="请选择计量单位">
                <el-option v-for="item in measurementUnits" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>

            <el-form-item label="数据来源系统">
              <div class="input-with-button">
                <el-input
                  v-model="addForm.config.dataSourceSystem"
                  placeholder="请选择数据来源系统"
                  readonly
                />
                <el-button icon="Plus" @click="openDataSourceSystemDialog" />
              </div>
            </el-form-item>

            <el-form-item label="获取方式">
              <el-input
                v-model="addForm.config.acquisitionMethod"
                type="textarea"
                :rows="3"
                placeholder="请输入JSON格式的API接口、请求参数等信息"
              />
            </el-form-item>
          </template>
          
          <el-form-item label="分摊比例(%)">
            <el-input 
              v-model="addForm.config.allocationRatio" 
              placeholder="默认100%" 
              @input="handleAllocationRatioInput"
              @blur="handleAllocationRatioBlur"
            />
          </el-form-item>
          
          <el-form-item label="是否有下级子表">
            <el-switch v-model="addForm.config.hasSubTable" />
          </el-form-item>
          
          <el-form-item label="核算误差约束(%)">
            <el-input 
              v-model="addForm.config.errorConstraint" 
              placeholder="请输入误差范围"
              @input="handleErrorConstraintInput"
              @blur="handleErrorConstraintBlur"
            />
          </el-form-item>
          
          <el-form-item label="采集点描述" v-if="addForm.typeId === 3">
            <el-input 
              v-model="addForm.config.collectionDescription" 
              type="textarea"
              :rows="3"
              placeholder="请输入采集点描述"
            />
          </el-form-item>
          
          <el-collapse v-if="addForm.config.dataSource !== '手工录入'" v-model="addTaskCollapse">
            <el-collapse-item title="数据采集计划任务配置" name="addTaskConfig">
              <el-form :model="addForm.config.taskConfig" label-width="100px">
                <el-form-item label="起始日期">
                  <el-date-picker
                    v-model="addForm.config.taskConfig.startDate"
                    type="datetime"
                    placeholder="选择起始日期"
                    style="width: 100%"
                    format="YYYY-MM-DD HH:mm"
                    value-format="YYYY-MM-DDTHH:mm"
                  />
                </el-form-item>
                
                <el-form-item label="截止日期">
                  <el-date-picker
                    v-model="addForm.config.taskConfig.endDate"
                    type="datetime"
                    placeholder="选择截止日期（可选）"
                    style="width: 100%"
                    format="YYYY-MM-DD HH:mm"
                    value-format="YYYY-MM-DDTHH:mm"
                  />
                  <span v-if="!addForm.config.taskConfig.endDate" class="text-muted">无限制</span>
                </el-form-item>
                
                <el-form-item label="执行周期">
                  <el-select v-model="addForm.config.taskConfig.cycleType" @change="handleAddCycleChange">
                    <el-option label="按日" value="DAILY" />
                    <el-option label="按周" value="WEEKLY" />
                    <el-option label="按月" value="MONTHLY" />
                    <el-option label="按季" value="QUARTERLY" />
                    <el-option label="按年" value="YEARLY" />
                  </el-select>
                </el-form-item>
                
                <el-form-item v-if="addForm.config.taskConfig.cycleType === 'WEEKLY'" label="周执行日期">
                  <el-checkbox-group v-model="addForm.config.taskConfig.weekDays">
                    <el-checkbox label="周一" value="1" />
                    <el-checkbox label="周二" value="2" />
                    <el-checkbox label="周三" value="3" />
                    <el-checkbox label="周四" value="4" />
                    <el-checkbox label="周五" value="5" />
                    <el-checkbox label="周六" value="6" />
                    <el-checkbox label="周日" value="0" />
                  </el-checkbox-group>
                </el-form-item>
                
                <el-form-item v-if="addForm.config.taskConfig.cycleType === 'WEEKLY'" label="重复间隔">
                  <el-input v-model="addForm.config.taskConfig.interval" type="number" placeholder="每N周执行一次" />
                </el-form-item>
                
                <el-form-item v-if="addForm.config.taskConfig.cycleType === 'MONTHLY' || addForm.config.taskConfig.cycleType === 'QUARTERLY'" label="月执行日期">
                  <el-input v-model="addForm.config.taskConfig.monthDays" placeholder="如：1,5,15" />
                  <span class="text-muted">多个日期用逗号分隔</span>
                </el-form-item>
                
                <el-form-item v-if="addForm.config.taskConfig.cycleType === 'YEARLY'" label="年执行日期">
                  <el-input v-model="addForm.config.taskConfig.yearMonths" placeholder="如：1-15,6-15" />
                  <span class="text-muted">格式：月份-日期，多个用逗号分隔</span>
                </el-form-item>
                
                <el-form-item label="执行时间">
                  <el-time-picker
                    v-model="addForm.config.taskConfig.executionTime"
                    format="HH:mm"
                    value-format="HH:mm"
                    :editable="true"
                    style="width: 100%"
                  />
                </el-form-item>
                
                <el-form-item v-if="addForm.config.taskConfig.cycleType === 'MONTHLY'" label="重复间隔">
                  <el-input v-model="addForm.config.taskConfig.interval" type="number" placeholder="每N个月执行一次" />
                </el-form-item>
              </el-form>
            </el-collapse-item>
          </el-collapse>
          
          <el-form-item v-if="addForm.config.dataSource === '手工录入'" label="数据录入">
            <el-tag type="info">手动录入，无需计划任务</el-tag>
          </el-form-item>
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

    <DataSourceSystemDialog
      v-model:visible="showDataSourceSystemDialog"
      @select="handleDataSourceSystemSelect"
      @cancel="handleDataSourceSystemCancel"
    />
    
    <CarbonEmissionFactorDialog
      v-model:visible="showAddFactorDialog"
      :emission-category="addForm.config.emissionCategory"
      :emission-subcategory="addForm.config.emissionSubcategory"
      @select="handleAddFactorSelect"
      @cancel="handleAddFactorCancel"
    />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, defineProps, defineEmits } from 'vue';
import TreeComponent from './components/TreeComponent.vue';
import ConfigPanel from './components/ConfigPanel.vue';
import DataSourceSystemDialog from './components/DataSourceSystemDialog.vue';
import CarbonEmissionFactorDialog from './components/CarbonEmissionFactorDialog.vue';
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
  statisticalCalibers: [],
  emissionCategories: [],
  emissionSubcategories: {},
  dataSources: [],
  accountingScenarios: [],
  energyUses: [],
  /*
  accountingScenarios: [
    '牵引变电所/牵引供电',
    '列车运行',
    '行车调度/通信指挥',
    '车辆维修/机修车间',
    '线路维护保养',
    '场站系统',
    '车库',
    '车间浴室',
    '办公楼',
    '职工食堂',
    '职工宿舍',
    '内部运营车辆',
    '内部运营车辆（单台车）',
    '废弃物处理场所',
    '其他场景'
  ],
  energyUses: [
    '照明',
    '空调',
    '取暖',
    '供热',
    '电梯',
    '水泵',
    '汽车',
    '设备用能',
    '列车运行用能',
    '混合用能计量',
    '其他'
  ]
  */
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
const showDataSourceSystemDialog = ref(false);
const showAddFactorDialog = ref(false);
const addOriginalFactorValue = ref('');
const addModalTitle = ref('');
const deleteTargetNode = ref(null);
const addTaskCollapse = ref(['addTaskConfig']);
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
  config: {
    statisticalCaliber: '',
    emissionCategory: '',
    emissionSubcategory: '',
    carbonEmissionFactor: '',
    carbonEmissionFactorDescription: '',
    dataSource: '',
    accountingScenario: '',
    energyUse: '',
    isCumulative: 'true',
    isMobileSource: 'false',
    measurementUnit: '',
    dataSourceSystem: '',
    acquisitionMethod: '',
    allocationRatio: 100,
    hasSubTable: false,
    errorConstraint: 10,
    taskConfig: {
      startDate: null,
      endDate: null,
      cycleType: 'DAILY',
      weekDays: [],
      monthDays: '',
      yearMonths: '',
      executionTime: '02:00',
      interval: 1
    },
    collectionDescription: '',
    equipmentCode: ''
  },
  nodeInfo: {
    nodeCode: '',
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

/**
 * 根据排放数据大类和小类计算当前可用的计量单位列表
 * 不同的排放数据大类和小类对应不同的计量单位选项
 */
const measurementUnits = computed(() => {
  const category = addForm.config.emissionCategory;
  const subcategory = addForm.config.emissionSubcategory;
  
  if (category === '购入的电力' || category === '输出的电力') {
    return ['MWh', 'KWh', 'GWh'];
  }
  
  if (category === '化石燃料') {
    const solidFuels = ['烟煤', '褐煤', '焦炭', '石油焦'];
    const liquidFuels = ['原油', '燃料油', '汽油', '柴油', '液化天然气', '液化石油气'];
    const gasFuels = ['天然气', '高炉煤气', '转炉煤气', '焦炉煤气'];
    
    if (solidFuels.includes(subcategory)) {
      return ['吨'];
    } else if (liquidFuels.includes(subcategory)) {
      return ['吨', '升', '千克'];
    } else if (gasFuels.includes(subcategory)) {
      return ['10⁴Nm³(万立方米)', '10³Nm³(千立方米)', 'Nm³(标准立方米)'];
    }
  }
  
  if (category === '购入的热力') {
    if (subcategory === '热力数据') {
      return ['吉焦(GJ)', '兆焦(MJ)'];
    } else if (subcategory === '质量单位计量的蒸汽' || subcategory === '质量单位计量的热水') {
      return ['吨'];
    }
  }
  
  if (category === '废弃物处理') {
    if (subcategory === '废水处理排放') {
      return ['立方米'];
    } else if (subcategory === '固体废弃物处理排放') {
      return ['吨'];
    }
  }
  
  return [];
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
    options.statisticalCalibers = res.data.statisticalCalibers;
    options.emissionCategories = res.data.emissionCategories;
    options.emissionSubcategories = res.data.emissionSubcategories;
    options.dataSources = res.data.dataSources;
    options.nodeCategories = res.data.nodeCategories || {};
    if (res.data.accountingScenarios && res.data.accountingScenarios.length > 0) {
      options.accountingScenarios = res.data.accountingScenarios;
    }
    if (res.data.energyUses && res.data.energyUses.length > 0) {
      options.energyUses = res.data.energyUses;
    }
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
  addForm.parentId = parentNode.id;
  addForm.parentNodeTypeId = parentNode.typeId;
  addForm.templateId = parentNode.templateId;
  addForm.typeId = null;
  addForm.name = '';
  addForm.locomotiveType = '';
  addForm.config.statisticalCaliber = '';
  addForm.config.emissionCategory = '';
  addForm.config.emissionSubcategory = '';
  addForm.config.carbonEmissionFactor = '';
  addForm.config.dataSource = '';
  addForm.config.dataSourceSystem = '';
  addForm.config.acquisitionMethod = '';
  addForm.config.allocationRatio = 100;
  addForm.config.hasSubTable = false;
  addForm.config.errorConstraint = 10;
  addForm.config.taskConfig = {
    startDate: null,
    endDate: null,
    cycleType: 'DAILY',
    weekDays: [],
    monthDays: '',
    yearMonths: '',
    executionTime: '02:00',
    interval: 1
  };
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
 * 清空排放数据小类选择，设置默认计量单位，生成设备编号（如果是排放数据采集点）
 */
const handleCategoryChange = () => {
  addForm.config.emissionSubcategory = '';
  addForm.config.measurementUnit = getDefaultMeasurementUnit();
  if (addForm.typeId === 3 && addForm.config.emissionCategory) {
    generateEquipmentCode();
  }
};

/**
 * 排放数据小类变化事件处理函数
 * 根据新选择的小类自动设置对应的计量单位
 */
const handleSubcategoryChange = () => {
  addForm.config.measurementUnit = getDefaultMeasurementUnit();
};

/**
 * 获取当前排放数据大类和小类对应的默认计量单位
 * @returns {string} 默认计量单位
 */
const getDefaultMeasurementUnit = () => {
  const category = addForm.config.emissionCategory;
  const subcategory = addForm.config.emissionSubcategory;
  
  if (category === '购入的电力' || category === '输出的电力') {
    return 'MWh';
  }
  
  if (category === '化石燃料') {
    const liquidFuels = ['原油', '燃料油', '汽油', '柴油', '液化天然气', '液化石油气'];
    const gasFuels = ['天然气', '高炉煤气', '转炉煤气', '焦炉煤气'];
    
    if (liquidFuels.includes(subcategory)) {
      return '吨';
    } else if (gasFuels.includes(subcategory)) {
      return '10⁴Nm³(万立方米)';
    } else if (subcategory) {
      return '吨';
    }
  }
  
  if (category === '购入的热力') {
    if (subcategory === '热力数据') {
      return '吉焦(GJ)';
    } else if (subcategory) {
      return '吨';
    }
  }
  
  if (category === '废弃物处理') {
    if (subcategory === '废水处理排放') {
      return '立方米';
    } else if (subcategory) {
      return '吨';
    }
  }
  
  return '';
};

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

const handleAddCycleChange = () => {
  addForm.config.taskConfig.weekDays = [];
  addForm.config.taskConfig.monthDays = '';
  addForm.config.taskConfig.yearMonths = '';
  addForm.config.taskConfig.interval = 1;
};

const resetAddForm = () => {
  addForm.name = '';
  addForm.typeId = null;
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
  const nodeData = {
    name: addForm.name,
    typeId: addForm.typeId,
    parentId: addForm.parentId,
    templateId: addForm.templateId,
    locomotiveType: addForm.locomotiveType,
    createdBy: currentUser.userId,
    config: (addForm.typeId === 3 || addForm.typeId === 4) ? {
      statisticalCaliber: addForm.config.statisticalCaliber,
      emissionCategory: addForm.config.emissionCategory,
      emissionSubcategory: addForm.config.emissionSubcategory,
      carbonEmissionFactor: addForm.config.carbonEmissionFactor ? parseFloat(addForm.config.carbonEmissionFactor) : null,
      carbonEmissionFactorDescription: addForm.config.carbonEmissionFactorDescription,
      dataSource: addForm.config.dataSource,
      accountingScenario: addForm.config.accountingScenario,
      energyUse: addForm.config.energyUse,
      isCumulative: addForm.config.isCumulative,
      isMobileSource: addForm.config.isMobileSource,
      measurementUnit: addForm.config.measurementUnit,
      dataSourceSystem: addForm.config.dataSourceSystem,
      acquisitionMethod: addForm.config.acquisitionMethod,
      allocationRatio: addForm.config.allocationRatio,
      hasSubTable: addForm.config.hasSubTable,
      errorConstraint: addForm.config.errorConstraint,
      taskConfig: addForm.config.taskConfig,
      collectionDescription: addForm.config.collectionDescription,
      equipmentCode: addForm.config.equipmentCode,
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
  
  nodeApi.createNode(nodeData).then(res => {
    saveTreeSnapshot('add', snapshot);
    loadTree();
    showAddModal.value = false;
  }).catch((error) => {
    console.error('创建节点失败:', error);
    alert('创建节点失败，请重试');
  });
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

const handleSave = () => {
  alert('保存成功');
};

const handleCancel = () => {
  loadTree();
  selectedNode.value = null;
};

const handleConfigSave = () => {
  console.log('Config saved');
};

const handleConfigCancel = () => {
  console.log('Config cancelled');
};

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
 * 打开数据来源系统选择对话框
 */
const openDataSourceSystemDialog = () => {
  showDataSourceSystemDialog.value = true;
};

/**
 * 数据来源系统选择成功回调
 * 将选中的数据来源系统名称回填到表单中
 * @param {Object} result - 选择结果，包含 status 和 systemName
 */
const handleDataSourceSystemSelect = (result) => {
  if (result.status === 'select' && result.systemName) {
    addForm.config.dataSourceSystem = result.systemName;
  }
};

/**
 * 数据来源系统选择取消回调
 */
const handleDataSourceSystemCancel = () => {
  // Just close, no action needed
};

const openAddFactorDialog = () => {
  addOriginalFactorValue.value = addForm.config.carbonEmissionFactor;
  showAddFactorDialog.value = true;
};

const handleAddFactorSelect = (result) => {
  if (result.factorValue) {
    addForm.config.carbonEmissionFactor = result.factorValue;
    addForm.config.carbonEmissionFactorDescription = result.description || '';
  }
  showAddFactorDialog.value = false;
};

const handleAddFactorCancel = () => {
  showAddFactorDialog.value = false;
};

const handleAllocationRatioInput = (value) => {
  if (value === '') return;
  const filtered = value.replace(/[^\d.]/g, '');
  const dotCount = (filtered.match(/\./g) || []).length;
  if (dotCount > 1) {
    const lastDotIndex = filtered.lastIndexOf('.');
    addForm.config.allocationRatio = filtered.slice(0, lastDotIndex) + filtered.slice(lastDotIndex + 1);
  } else {
    addForm.config.allocationRatio = filtered;
  }
};

const handleAllocationRatioBlur = () => {
  const value = addForm.config.allocationRatio;
  if (value === '') {
    addForm.config.allocationRatio = 100;
    return;
  }
  const num = parseFloat(value);
  if (isNaN(num) || num <= 0 || num > 100) {
    alert('分摊比例必须是大于0且小于等于100的数字');
    addForm.config.allocationRatio = 100;
  }
};

const handleErrorConstraintInput = (value) => {
  if (value === '') return;
  const filtered = value.replace(/[^\d.]/g, '');
  const dotCount = (filtered.match(/\./g) || []).length;
  if (dotCount > 1) {
    const lastDotIndex = filtered.lastIndexOf('.');
    addForm.config.errorConstraint = filtered.slice(0, lastDotIndex) + filtered.slice(lastDotIndex + 1);
  } else {
    addForm.config.errorConstraint = filtered;
  }
};

const handleErrorConstraintBlur = () => {
  const value = addForm.config.errorConstraint;
  if (value === '') {
    addForm.config.errorConstraint = 10;
    return;
  }
  const num = parseFloat(value);
  if (isNaN(num) || num <= 0) {
    alert('核实误差约束必须是大于0的数字');
    addForm.config.errorConstraint = 10;
  }
};

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
  padding: 12px 16px;
  border-bottom: 1px solid #e8eaec;
}

.panel-title {
  font-size: 14px;
  font-weight: 600;
  color: #1f2329;
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

.name-input-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.name-input-item .el-input {
  flex: 1;
}

.input-with-button {
  display: flex;
  gap: 8px;
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
</style>
