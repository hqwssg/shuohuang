<template>
  <div class="config-panel-wrapper">
    <div class="config-panel">
      <el-form :model="formData" label-width="120px">
        <el-form-item label="节点名称" class="name-input-item">
          <el-input v-model="formData.name" />
          <el-button 
            size="small" 
            icon="plus" 
            title="点击该按键自动添加名称前缀"
            @click="addNamePrefix"
          />
        </el-form-item>
        
        <el-form-item v-if="node.typeId === 4" label="机车类型">
          <el-input :value="node.locomotiveType" />
        </el-form-item>
        
        <template v-if="node.typeId === 3 || node.typeId === 4">
          <el-form-item label="统计口径类型">
            <el-select v-model="formData.config.statisticalCaliber" @change="handleCategoryChange">
              <el-option v-for="item in options.statisticalCalibers" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
          
          <el-form-item label="排放数据大类">
            <el-select v-model="formData.config.emissionCategory" @change="handleCategoryChange">
              <el-option v-for="item in options.emissionCategories" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
          
          <el-form-item v-if="node.typeId === 3" label="采集设备编号">
            <el-input v-model="formData.config.equipmentCode" readonly />
          </el-form-item>
          
          <el-form-item label="排放数据小类">
            <el-select v-model="formData.config.emissionSubcategory" @change="handleSubcategoryChange">
              <el-option v-for="item in currentSubcategories" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
          
          <el-form-item label="碳排放因子设置">
            <div class="factor-input-group">
              <el-input 
                v-model="formData.config.carbonEmissionFactor" 
                placeholder="请输入碳排放因子" 
                @input="handleFactorInput"
              />
              <el-button 
                size="small" 
                @click="openFactorDialog"
                :disabled="!formData.config.emissionCategory || !formData.config.emissionSubcategory"
                title="选择碳排放因子"
              >选择</el-button>
            </div>
          </el-form-item>
          
          <el-form-item label="单位">
            <el-tag type="info">{{ emissionFactorUnit }}</el-tag>
          </el-form-item>
          
          <el-form-item label="碳排放因子描述">
            <el-input 
              :value="formData.config.carbonEmissionFactorDescription" 
              type="textarea"
              :rows="2"
              readonly
              placeholder="碳排放因子描述信息"
            />
          </el-form-item>
          
          <el-form-item label="数据来源">
            <el-select v-model="formData.config.dataSource">
              <el-option v-for="item in options.dataSources" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
          
          <template v-if="node.typeId === 3">
            <el-form-item label="核算场景">
              <el-select v-model="formData.config.accountingScenario" filterable>
                <el-option v-for="item in options.accountingScenarios" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
            
            <el-form-item label="能耗用途">
              <el-select v-model="formData.config.energyUse" filterable>
                <el-option v-for="item in options.energyUses" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
            
            <el-form-item label="是否累计量">
              <el-select v-model="formData.config.isCumulative">
                <el-option label="是" value="true" />
                <el-option label="否" value="false" />
              </el-select>
            </el-form-item>
            
            <el-form-item label="是否移动源">
              <el-select v-model="formData.config.isMobileSource">
                <el-option label="是" value="true" />
                <el-option label="否" value="false" />
              </el-select>
            </el-form-item>
            
            <el-form-item label="计量单位">
              <el-select v-model="formData.config.measurementUnit">
                <el-option v-for="item in measurementUnits" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>

            <el-form-item label="数据来源系统">
              <div class="input-with-button">
                <el-input
                  v-model="formData.config.dataSourceSystem"
                  placeholder="请选择数据来源系统"
                  readonly
                />
                <el-button icon="Plus" @click="openDataSourceSystemDialog" :disabled="!editMode" />
              </div>
            </el-form-item>

            <el-form-item label="获取方式">
              <el-input
                v-model="formData.config.acquisitionMethod"
                type="textarea"
                :rows="3"
                placeholder="请输入JSON格式的API接口、请求参数等信息"
              />
            </el-form-item>
          </template>
          
          <el-form-item label="分摊比例(%)">
            <el-input 
              v-model="formData.config.allocationRatio" 
              placeholder="默认100%" 
              @input="handleAllocationRatioInput"
              @blur="handleAllocationRatioBlur"
            />
          </el-form-item>
          
          <el-form-item label="是否有下级子表">
            <el-switch v-model="formData.config.hasSubTable" />
          </el-form-item>
          
          <el-form-item label="核算误差约束(%)">
            <el-input 
              v-model="formData.config.errorConstraint" 
              placeholder="请输入误差范围"
              @input="handleErrorConstraintInput"
              @blur="handleErrorConstraintBlur"
            />
          </el-form-item>
          
          <el-form-item v-if="node.typeId === 3" label="采集点描述">
            <el-input 
              v-model="formData.config.collectionDescription" 
              type="textarea"
              :rows="3"
              placeholder="请输入采集点描述"
            />
          </el-form-item>
          
          <el-collapse v-if="formData.config.dataSource !== '手工录入'" v-model="taskCollapse">
            <el-collapse-item title="数据采集计划任务配置" name="taskConfig">
              <el-form :model="formData.config.taskConfig" label-width="100px">
                <el-form-item label="起始日期">
                  <el-date-picker
                    v-model="formData.config.taskConfig.startDate"
                    type="datetime"
                    placeholder="选择起始日期"
                    style="width: 100%"
                    format="YYYY-MM-DD HH:mm"
                    value-format="YYYY-MM-DDTHH:mm"
                  />
                </el-form-item>
                
                <el-form-item label="截止日期">
                  <el-date-picker
                    v-model="formData.config.taskConfig.endDate"
                    type="datetime"
                    placeholder="选择截止日期（可选）"
                    style="width: 100%"
                    format="YYYY-MM-DD HH:mm"
                    value-format="YYYY-MM-DDTHH:mm"
                  />
                  <span v-if="!formData.config.taskConfig.endDate" class="text-muted">无限制</span>
                </el-form-item>
                
                <el-form-item label="执行周期">
                  <el-select v-model="formData.config.taskConfig.cycleType" @change="handleCycleChange">
                    <el-option label="按日" value="DAILY" />
                    <el-option label="按周" value="WEEKLY" />
                    <el-option label="按月" value="MONTHLY" />
                    <el-option label="按季" value="QUARTERLY" />
                    <el-option label="按年" value="YEARLY" />
                  </el-select>
                </el-form-item>
                
                <el-form-item v-if="formData.config.taskConfig.cycleType === 'WEEKLY'" label="周执行日期">
                  <el-checkbox-group v-model="formData.config.taskConfig.weekDays">
                    <el-checkbox label="周一" value="1" />
                    <el-checkbox label="周二" value="2" />
                    <el-checkbox label="周三" value="3" />
                    <el-checkbox label="周四" value="4" />
                    <el-checkbox label="周五" value="5" />
                    <el-checkbox label="周六" value="6" />
                    <el-checkbox label="周日" value="0" />
                  </el-checkbox-group>
                </el-form-item>
                
                <el-form-item v-if="formData.config.taskConfig.cycleType === 'WEEKLY'" label="重复间隔">
                  <el-input v-model="formData.config.taskConfig.interval" type="number" placeholder="每N周执行一次" />
                </el-form-item>
                
                <el-form-item v-if="formData.config.taskConfig.cycleType === 'MONTHLY' || formData.config.taskConfig.cycleType === 'QUARTERLY'" label="月执行日期">
                  <el-input v-model="formData.config.taskConfig.monthDays" placeholder="如：1,5,15" />
                  <span class="text-muted">多个日期用逗号分隔</span>
                </el-form-item>
                
                <el-form-item v-if="formData.config.taskConfig.cycleType === 'YEARLY'" label="年执行日期">
                  <el-input v-model="formData.config.taskConfig.yearMonths" placeholder="如：1-15,6-15" />
                  <span class="text-muted">格式：月份-日期，多个用逗号分隔</span>
                </el-form-item>
                
                <el-form-item label="执行时间">
                  <el-time-picker
                    v-model="formData.config.taskConfig.executionTime"
                    format="HH:mm"
                    value-format="HH:mm"
                    :editable="true"
                    style="width: 100%"
                  />
                </el-form-item>
                
                <el-form-item v-if="formData.config.taskConfig.cycleType === 'MONTHLY'" label="重复间隔">
                  <el-input v-model="formData.config.taskConfig.interval" type="number" placeholder="每N个月执行一次" />
                </el-form-item>
              </el-form>
            </el-collapse-item>
          </el-collapse>
          
          <el-form-item v-if="formData.config.dataSource === '手工录入'" label="数据录入">
            <el-tag type="info">手动录入，无需计划任务</el-tag>
          </el-form-item>
        </template>
        
        <template v-if="node.typeId === 2">
          <el-form-item label="节点编码">
            <el-input 
              v-model="formData.nodeInfo.nodeCode" 
            />
          </el-form-item>
          
          <el-form-item label="节点名称简称">
            <el-input 
              v-model="formData.nodeInfo.shortName" 
              :title="shortNameTooltip"
            />
            <span class="text-muted">该简称将用于自动生成子节点名称的前缀</span>
          </el-form-item>
          
          <el-form-item label="是否纳入碳排放核算">
            <el-switch 
              v-model="formData.nodeInfo.includeInCalculation" 
              :active-value="true" 
              :inactive-value="false"
              active-text="是"
              inactive-text="否"
              @change="handleIncludeCalculationChange"
            />
          </el-form-item>
          
          <el-form-item label="节点类型">
            <el-select v-model="formData.nodeInfo.nodeCategory">
              <el-option v-for="(value, key) in options.nodeCategories" :key="key" :label="value" :value="key" />
            </el-select>
          </el-form-item>
          
          <el-form-item label="单位说明">
            <el-input 
              v-model="formData.nodeInfo.unitDescription" 
              type="textarea"
              :rows="3"
              placeholder="阐述单位职责、范围"
            />
          </el-form-item>
          
          <el-form-item label="组织边界说明">
            <el-input 
              v-model="formData.nodeInfo.orgBoundaryDescription" 
              type="textarea"
              :rows="3"
              placeholder="明确哪些单位、区域纳入核算"
            />
          </el-form-item>
          
          <el-form-item label="运营边界说明">
            <el-input 
              v-model="formData.nodeInfo.operationBoundaryDescription" 
              type="textarea"
              :rows="3"
              placeholder="明确自有、租赁、外包、代管设施处理口径"
            />
          </el-form-item>
        </template>
        
        <template v-if="node.typeId === 1">
          <el-form-item label="节点类型">
            <el-tag type="success">根节点</el-tag>
          </el-form-item>
          <el-form-item label="说明">
            <span class="text-muted">系统总节点，可添加各类子节点</span>
          </el-form-item>
        </template>
      </el-form>
      
      <!-- 非编辑模式下的透明覆盖层（不覆盖操作按钮） -->
      <div v-if="!editMode" class="config-overlay"></div>
    </div>

    <DataSourceSystemDialog
      v-model:visible="showDataSourceSystemDialog"
      @select="handleDataSourceSystemSelect"
      @cancel="handleDataSourceSystemCancel"
    />
    
    <CarbonEmissionFactorDialog
      v-model:visible="showFactorDialog"
      :emission-category="formData.config.emissionCategory"
      :emission-subcategory="formData.config.emissionSubcategory"
      @select="handleFactorSelect"
      @cancel="handleFactorCancel"
    />
    
    <div class="config-actions">
      <el-button 
        @click="handleModify" 
        :disabled="editMode"
        type="primary"
      >修改</el-button>
      <el-button 
        type="success" 
        @click="handleSave"
        :disabled="!editMode"
      >保存</el-button>
      <el-button 
        @click="handleCancel"
        :disabled="!editMode"
      >取消</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue';
import DataSourceSystemDialog from './DataSourceSystemDialog.vue';
import CarbonEmissionFactorDialog from './CarbonEmissionFactorDialog.vue';

const props = defineProps({
  node: {
    type: Object,
    required: true
  },
  options: {
    type: Object,
    default: () => ({})
  },
  treeData: {
    type: Array,
    default: () => []
  }
});

const emit = defineEmits(['save', 'cancel', 'update']);

const editMode = ref(false);
const originalData = ref(null);
const taskCollapse = ref(['taskConfig']);
const showDataSourceSystemDialog = ref(false);
const showFactorDialog = ref(false);
const originalFactorValue = ref('');

const formData = ref({
  name: '',
  config: {
    statisticalCaliber: '',
    emissionCategory: '',
    emissionSubcategory: '',
    carbonEmissionFactor: '',
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
    shortName: '',
    includeInCalculation: true,
    nodeCategory: '',
    unitDescription: '',
    orgBoundaryDescription: '',
    operationBoundaryDescription: ''
  }
});

const shortNameTooltip = '该简称将用于自动生成子节点名称的前缀';

const currentSubcategories = computed(() => {
  const category = formData.value.config.emissionCategory;
  return props.options.emissionSubcategories[category] || [];
});

const measurementUnits = computed(() => {
  const category = formData.value.config.emissionCategory;
  const subcategory = formData.value.config.emissionSubcategory;
  
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

const emissionFactorUnit = computed(() => {
  const category = formData.value.config.emissionCategory;
  const subcategory = formData.value.config.emissionSubcategory;
  
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

const initForm = () => {
  const savedTaskConfig = props.node.config?.taskConfig;
  const factorValue = props.node.config?.carbonEmissionFactor || '';
  originalFactorValue.value = factorValue;
  formData.value = {
    name: props.node.name,
    config: {
      statisticalCaliber: props.node.config?.statisticalCaliber || '',
      emissionCategory: props.node.config?.emissionCategory || '',
      emissionSubcategory: props.node.config?.emissionSubcategory || '',
      carbonEmissionFactor: factorValue,
      carbonEmissionFactorDescription: props.node.config?.carbonEmissionFactorDescription || '',
      dataSource: props.node.config?.dataSource || '',
      accountingScenario: props.node.config?.accountingScenario || '',
      energyUse: props.node.config?.energyUse || '',
      isCumulative: props.node.config?.isCumulative !== undefined ? props.node.config.isCumulative : 'true',
      isMobileSource: props.node.config?.isMobileSource !== undefined ? props.node.config.isMobileSource : 'false',
      measurementUnit: props.node.config?.measurementUnit || '',
      dataSourceSystem: props.node.config?.dataSourceSystem || '',
      acquisitionMethod: props.node.config?.acquisitionMethod || '',
      allocationRatio: props.node.config?.allocationRatio || 100,
      hasSubTable: props.node.config?.hasSubTable || false,
      errorConstraint: props.node.config?.errorConstraint || 10,
      taskConfig: savedTaskConfig ? {
        startDate: savedTaskConfig.startDate || null,
        endDate: savedTaskConfig.endDate || null,
        cycleType: savedTaskConfig.cycleType || 'DAILY',
        weekDays: savedTaskConfig.weekDays || [],
        monthDays: savedTaskConfig.monthDays || '',
        yearMonths: savedTaskConfig.yearMonths || '',
        executionTime: savedTaskConfig.executionTime || '02:00',
        interval: savedTaskConfig.interval || 1
      } : {
        startDate: null,
        endDate: null,
        cycleType: 'DAILY',
        weekDays: [],
        monthDays: '',
        yearMonths: '',
        executionTime: '02:00',
        interval: 1
      },
      collectionDescription: props.node.config?.collectionDescription || '',
      equipmentCode: props.node.config?.equipmentCode || ''
    },
    nodeInfo: {
      nodeCode: props.node.nodeInfo?.nodeCode || '',
      shortName: props.node.nodeInfo?.shortName || '',
      includeInCalculation: props.node.nodeInfo?.includeInCalculation !== undefined ? props.node.nodeInfo.includeInCalculation : true,
      nodeCategory: props.node.nodeInfo?.nodeCategory || '',
      unitDescription: props.node.nodeInfo?.unitDescription || '',
      orgBoundaryDescription: props.node.nodeInfo?.orgBoundaryDescription || '',
      operationBoundaryDescription: props.node.nodeInfo?.operationBoundaryDescription || ''
    }
  };
  
  originalData.value = JSON.parse(JSON.stringify(formData.value));
  editMode.value = false;
};

watch(() => props.node, () => {
  initForm();
}, { immediate: true });

const handleCategoryChange = () => {
  formData.value.config.emissionSubcategory = '';
  formData.value.config.measurementUnit = '';
  
  if (props.node.typeId === 3 && props.node.id && formData.value.config.emissionCategory) {
    fetch(`/api/nodes/${props.node.id}/equipment-code?emissionCategory=${encodeURIComponent(formData.value.config.emissionCategory)}`)
      .then(res => res.text())
      .then(code => {
        formData.value.config.equipmentCode = code;
      }).catch(() => {
        formData.value.config.equipmentCode = '';
      });
  }
};

const handleSubcategoryChange = () => {
  const category = formData.value.config.emissionCategory;
  const subcategory = formData.value.config.emissionSubcategory;
  
  if (category === '购入的电力' || category === '输出的电力') {
    formData.value.config.measurementUnit = 'MWh';
  } else if (category === '化石燃料') {
    const liquidFuels = ['原油', '燃料油', '汽油', '柴油', '液化天然气', '液化石油气'];
    const gasFuels = ['天然气', '高炉煤气', '转炉煤气', '焦炉煤气'];
    
    if (liquidFuels.includes(subcategory)) {
      formData.value.config.measurementUnit = '吨';
    } else if (gasFuels.includes(subcategory)) {
      formData.value.config.measurementUnit = '10⁴Nm³(万立方米)';
    } else if (subcategory) {
      formData.value.config.measurementUnit = '吨';
    }
  } else if (category === '购入的热力') {
    if (subcategory === '热力数据') {
      formData.value.config.measurementUnit = '吉焦(GJ)';
    } else if (subcategory) {
      formData.value.config.measurementUnit = '吨';
    }
  } else if (category === '废弃物处理') {
    if (subcategory === '废水处理排放') {
      formData.value.config.measurementUnit = '立方米';
    } else if (subcategory) {
      formData.value.config.measurementUnit = '吨';
    }
  }
};

const handleCycleChange = () => {
  formData.value.config.taskConfig.weekDays = [];
  formData.value.config.taskConfig.monthDays = '';
  formData.value.config.taskConfig.yearMonths = '';
  formData.value.config.taskConfig.interval = 1;
};

const handleAllocationRatioInput = (value) => {
  if (value === '') return;
  const filtered = value.replace(/[^\d.]/g, '');
  const dotCount = (filtered.match(/\./g) || []).length;
  if (dotCount > 1) {
    const lastDotIndex = filtered.lastIndexOf('.');
    formData.value.config.allocationRatio = filtered.slice(0, lastDotIndex) + filtered.slice(lastDotIndex + 1);
  } else {
    formData.value.config.allocationRatio = filtered;
  }
};

const handleAllocationRatioBlur = () => {
  const value = formData.value.config.allocationRatio;
  if (value === '') {
    formData.value.config.allocationRatio = 100;
    return;
  }
  const num = parseFloat(value);
  if (isNaN(num) || num <= 0 || num > 100) {
    alert('分摊比例必须是大于0且小于等于100的数字');
    formData.value.config.allocationRatio = 100;
  }
};

const handleErrorConstraintInput = (value) => {
  if (value === '') return;
  const filtered = value.replace(/[^\d.]/g, '');
  const dotCount = (filtered.match(/\./g) || []).length;
  if (dotCount > 1) {
    const lastDotIndex = filtered.lastIndexOf('.');
    formData.value.config.errorConstraint = filtered.slice(0, lastDotIndex) + filtered.slice(lastDotIndex + 1);
  } else {
    formData.value.config.errorConstraint = filtered;
  }
};

const handleErrorConstraintBlur = () => {
  const value = formData.value.config.errorConstraint;
  if (value === '') {
    formData.value.config.errorConstraint = 10;
    return;
  }
  const num = parseFloat(value);
  if (isNaN(num) || num <= 0) {
    alert('核实误差约束必须是大于0的数字');
    formData.value.config.errorConstraint = 10;
  }
};

const handleIncludeCalculationChange = (value) => {
  if (!value && editMode.value) {
    if (!confirm('该节点及其下属子节点都将不纳入碳排放核算，是否确定？')) {
      formData.value.nodeInfo.includeInCalculation = true;
    }
  }
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
  // 检查节点和父节点ID是否存在
  if (!props.node || !props.node.parentId) {
    return;
  }
  
  // 检查树数据是否有效
  if (!props.treeData || !Array.isArray(props.treeData)) {
    return;
  }
  
  // 查找父节点
  const parentNode = findNodeById(props.treeData, props.node.parentId);
  if (!parentNode) return;
  
  // 收集上级节点的编码（根节点除外）
  const prefixParts = [];
  let currentNode = parentNode;
  
  while (currentNode && currentNode.parentId !== null) {
    if (currentNode.nodeInfo && currentNode.nodeInfo.shortName) {
      prefixParts.unshift(currentNode.nodeInfo.shortName);
    }
    currentNode = findNodeById(props.treeData, currentNode.parentId);
  }
  
  // 如果有前缀，添加到名称前面
  if (prefixParts.length > 0) {
    const originalName = formData.value.name;
    formData.value.name = prefixParts.join('-') + '-' + (originalName || '');
  }
};

const handleModify = () => {
  editMode.value = true;
};

const handleSave = () => {
  const emitData = {
    id: props.node.id,
    name: formData.value.name,
    config: (props.node.typeId === 3 || props.node.typeId === 4) ? formData.value.config : null,
    nodeInfo: props.node.typeId === 2 ? formData.value.nodeInfo : null
  };
  emit('update', emitData);
  originalData.value = JSON.parse(JSON.stringify(formData.value));
  editMode.value = false;
};

const handleCancel = () => {
  if (originalData.value) {
    formData.value = JSON.parse(JSON.stringify(originalData.value));
  }
  editMode.value = false;
  emit('cancel');
};

const openDataSourceSystemDialog = () => {
  showDataSourceSystemDialog.value = true;
};

const handleDataSourceSystemSelect = (result) => {
  if (result.status === 'select' && result.systemName) {
    formData.value.config.dataSourceSystem = result.systemName;
  }
};

const handleDataSourceSystemCancel = () => {
  // Just close, no action needed
};

const openFactorDialog = () => {
  originalFactorValue.value = formData.value.config.carbonEmissionFactor;
  showFactorDialog.value = true;
};

const handleFactorSelect = (result) => {
  if (result.factorValue) {
    formData.value.config.carbonEmissionFactor = result.factorValue;
    formData.value.config.carbonEmissionFactorDescription = result.description || '';
  }
  showFactorDialog.value = false;
};

const handleFactorCancel = () => {
  showFactorDialog.value = false;
};

const handleFactorInput = () => {
  const currentValue = formData.value.config.carbonEmissionFactor;
  if (currentValue !== originalFactorValue.value) {
    formData.value.config.carbonEmissionFactorDescription = `${currentValue}用户手动修改排放因子数据`;
  }
};
</script>

<style scoped>
.config-panel-wrapper {
  position: relative;
}

.config-panel {
  position: relative;
  z-index: 1;
  padding: 16px;
}

.config-overlay {
  position: absolute;
  top: 16px;
  left: 16px;
  right: 16px;
  bottom: 0;
  z-index: 10;
  cursor: not-allowed;
  background-color: transparent;
  pointer-events: auto;
}

.text-muted {
  color: #8c8c8c;
  font-size: 14px;
}

.name-input-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.name-input-item .el-input {
  flex: 1;
}

.config-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 0;
  padding: 16px;
  border-top: 1px solid #e8eaec;
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