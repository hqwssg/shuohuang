<template>
  <div class="config-panel-wrapper">
    <div class="config-panel">
      <el-form :model="formData" label-width="120px">
        <!-- typeId=3 的"节点名称"在其专属区块中展示，避免重复 -->
        <el-form-item v-if="node.typeId !== 3" label="节点名称">
          <el-input v-model="formData.name" />
        </el-form-item>
        
        <el-form-item v-if="node.typeId === 4" label="机车类型">
          <el-input :value="node.locomotiveType" />
        </el-form-item>
        
        <!-- 排放数据采集点（typeId=3）：显示内容与控件布局与"编辑节点"弹窗保持一致 -->
        <template v-if="node.typeId === 3">
          <el-row :gutter="20">
            <el-col :span="14">
              <el-form-item label="节点名称">
                <el-input v-model="formData.name" />
              </el-form-item>
            </el-col>
            <el-col :span="10">
              <el-form-item label="节点类型">
                <!-- 只读展示：保持可用状态外观，但禁止用户打开/修改 -->
                <el-select :model-value="node.typeId" class="readonly-select" style="width: 100%">
                  <el-option label="排放数据采集点" :value="3" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>

          <!-- 能耗品种大类 + 采集设备编号 一行并排 -->
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="能耗品种大类" label-width="120px">
                <el-select v-model="formData.config.emissionCategory" placeholder="请选择" @change="handleCategoryChange">
                  <el-option v-for="item in options.emissionCategories" :key="item" :label="item" :value="item" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="采集设备编号" label-width="100px">
                <el-input v-model="formData.config.equipmentCode" placeholder="系统自动生成" readonly />
              </el-form-item>
            </el-col>
          </el-row>

          <!-- 碳排放因子设置 + 碳排放因子单位（并排一行） -->
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="碳排放因子设置" label-width="120px">
                <el-input
                  v-model="formData.config.carbonEmissionFactor"
                  placeholder="请输入碳排放因子"
                  @input="handleFactorInput"
                />
              </el-form-item>
            </el-col>
            <el-col :span="10">
              <el-form-item label="碳排放因子单位" label-width="160px">
                <el-input
                  :value="emissionFactorUnit"
                  readonly
                  placeholder="选择碳排放因子后自动带出"
                />
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="碳排放因子描述" label-width="120px">
            <el-input
              :value="formData.config.carbonEmissionFactorDescription"
              type="textarea"
              :rows="2"
              readonly
              placeholder="碳排放因子描述信息"
            />
          </el-form-item>

          <el-form-item label="采集点描述">
            <el-input
              v-model="formData.config.collectionDescription"
              type="textarea"
              :rows="2"
              placeholder="请输入采集点描述"
            />
          </el-form-item>

          <!-- 已选采集点信息（文字展示，自动加载，与"编辑节点"弹窗一致） -->
          <div v-if="pointInfo" class="selected-point-info">
            <div class="selected-point-title">采集点信息</div>
            <el-descriptions :column="2" size="small" border>
              <el-descriptions-item label="名称">{{ pointInfo.name || '-' }}</el-descriptions-item>
              <el-descriptions-item label="能耗品种小类">{{ pointSubcategoryText }}</el-descriptions-item>
              <el-descriptions-item label="能耗场景大类">{{ energyCategoryName(pointInfo.energyCategoryL1) || '-' }}</el-descriptions-item>
              <el-descriptions-item label="能耗场景二级分类">{{ energyCategoryName(pointInfo.energyCategoryL2) || '-' }}</el-descriptions-item>
              <el-descriptions-item label="能耗场景三级分类">{{ energyCategoryName(pointInfo.energyCategoryL3) || '-' }}</el-descriptions-item>
              <el-descriptions-item label="是否累加量">{{ pointInfo.isCumulative === 1 ? '是' : '否' }}</el-descriptions-item>
              <el-descriptions-item label="是否移动源">{{ pointInfo.isMobileSource === 1 ? '是' : '否' }}</el-descriptions-item>
              <el-descriptions-item label="计量单位">{{ pointInfo.measurementUnit || '-' }}</el-descriptions-item>
              <el-descriptions-item label="数据来源系统">{{ pointInfo.dataSourceSystem || '-' }}</el-descriptions-item>
              <el-descriptions-item label="能耗用途分类">{{ pointInfo.energyUseCategory || '-' }}</el-descriptions-item>
              <el-descriptions-item label="能耗数据划拨">{{ pointInfo.energyAllocation || '-' }}</el-descriptions-item>
              <el-descriptions-item label="抄表方式">{{ pointInfo.meterReadingMethod === 0 ? '人工抄表' : '自动抄表' }}</el-descriptions-item>
              <el-descriptions-item label="计费周期" :span="2">{{ formatBillingCycle(pointInfo) }}</el-descriptions-item>
            </el-descriptions>
          </div>
        </template>

        <template v-else-if="node.typeId === 4">
          <el-form-item label="排放数据大类">
            <el-select v-model="formData.config.emissionCategory" @change="handleCategoryChange">
              <el-option v-for="item in options.emissionCategories" :key="item" :label="item" :value="item" />
            </el-select>
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
        </template>
        
        <template v-if="node.typeId === 2">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="节点编码">
                <el-input
                  v-model="formData.nodeInfo.nodeCode"
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="节点名称简称">
                <el-input
                  v-model="formData.nodeInfo.shortName"
                  :title="shortNameTooltip"
                />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="节点类型" label-width="120px">
                <el-select v-model="formData.nodeInfo.nodeCategory" style="width: 100%">
                  <el-option v-for="(value, key) in options.nodeCategories" :key="key" :label="value" :value="key" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="是否纳入碳排放核算" label-width="140px">
                <el-switch
                  v-model="formData.nodeInfo.includeInCalculation"
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

    <CarbonEmissionFactorDialog
      v-model:visible="showFactorDialog"
      :emission-category="formData.config.emissionCategory"
      :emission-subcategory="formData.config.emissionSubcategory"
      @select="handleFactorSelect"
      @cancel="handleFactorCancel"
    />
    <!-- 已移除"修改/保存/取消"操作按钮：右侧面板改为纯显示栏，节点编辑统一通过"添加/编辑节点"弹窗完成 -->
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue';
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
const showFactorDialog = ref(false);
const originalFactorValue = ref('');

const formData = ref({
  name: '',
  config: {
    emissionCategory: '',
    emissionSubcategory: '',
    carbonEmissionFactor: '',
    carbonEmissionFactorDescription: '',
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

/**
 * 当前大类下可选的能耗品种小类列表（从全局字典选项中按大类取值）
 */
const currentSubcategories = computed(() => {
  const category = formData.value.config.emissionCategory;
  return props.options.emissionSubcategories[category] || [];
});

// ===== 采集点信息展示（与"编辑节点"弹窗保持一致） =====

/**
 * 已选采集点完整信息（typeId=3，根据节点配置的采集点类型/ID自动加载）
 */
const pointInfo = ref(null);

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
 * 采集点品种小类文字：电力表为 emissionSubcategory，化石燃料为 fuelType，外购热能为 heatType
 */
const pointSubcategoryText = computed(() => {
  const p = pointInfo.value;
  if (!p) return '-';
  return p.emissionSubcategory || p.fuelType || p.heatType || '-';
});

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
 * 能耗分类编码 → 分类名称映射（数据来自 /api/energy-categories，惰性加载一次并缓存）
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
 * 进入节点时自动加载"采集点信息"（typeId=3 且已关联采集点时）
 */
const loadPointInfo = async () => {
  pointInfo.value = null;
  if (props.node.typeId === 3) {
    const cfg = props.node.config || {};
    if (cfg.collectionPointType && cfg.collectionPointId) {
      pointInfo.value = await fetchPointInfo(cfg.collectionPointType, cfg.collectionPointId);
    }
  }
};

/**
 * 碳排放因子单位（只读展示）：根据能耗品种大类与小类映射出对应的因子单位
 * 电力→kgCO₂/kWh；化石燃料按燃料形态→tCO₂/t 或 tCO₂/10⁴Nm³；
 * 热力→tCO₂/GJ 或 tCO₂/t；废弃物处理按处理类型→tGH₄/m³ 或 tCO₂/t；未命中返回空串
 */
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

/**
 * 初始化表单：用当前节点数据填充表单（配置 + 核算子节点信息），重置编辑模式并快照原始数据
 * 节点切换（watch props.node）时自动调用
 */
const initForm = () => {
  const factorValue = props.node.config?.carbonEmissionFactor || '';
  originalFactorValue.value = factorValue;
  formData.value = {
    name: props.node.name,
    config: {
      emissionCategory: props.node.config?.emissionCategory || '',
      emissionSubcategory: props.node.config?.emissionSubcategory || '',
      carbonEmissionFactor: factorValue,
      carbonEmissionFactorDescription: props.node.config?.carbonEmissionFactorDescription || '',
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
  loadPointInfo();
  ensureEnergyCategoryMap();
}, { immediate: true });

/**
 * 能耗品种大类变化处理：清空小类选择；
 * 若为排放数据采集点（typeId=3）且已选大类，则按大类+节点ID向后端请求生成新的采集设备编号
 */
const handleCategoryChange = () => {
  formData.value.config.emissionSubcategory = '';

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

/**
 * 能耗品种小类变化处理（预留，当前无需联动逻辑）
 */
const handleSubcategoryChange = () => {};

/**
 * "是否纳入碳排放核算"切换处理：编辑模式下取消勾选时弹出二次确认，
 * 用户取消则恢复为纳入核算
 * @param {Boolean} value - 切换后的目标值
 */
const handleIncludeCalculationChange = (value) => {
  if (!value && editMode.value) {
    if (!confirm('该节点及其下属子节点都将不纳入碳排放核算，是否确定？')) {
      formData.value.nodeInfo.includeInCalculation = true;
    }
  }
};

/**
 * 进入编辑模式：解锁表单控件（详情面板默认只读展示）
 */
const handleModify = () => {
  editMode.value = true;
};

/**
 * 保存节点：组装提交数据并 emit "update" 事件交由父组件调用更新接口
 * <p>
 * 数据组装规则：
 *   - typeId=3（排放数据采集点）/ typeId=4（运输生产碳排放核算节点）：提交 config；
 *     其中 typeId=3 额外保留节点上已有的采集点关联字段（collectionPointType/collectionPointId），避免保存时丢失
 *   - typeId=2（核算子节点）：提交 nodeInfo（节点编码、简称、核算标识、边界说明等）
 *   - 其他类型：config 与 nodeInfo 均不提交
 * 保存成功后刷新原始数据快照并退出编辑模式
 * </p>
 */
const handleSave = () => {
  const emitData = {
    id: props.node.id,
    name: formData.value.name,
    config: (props.node.typeId === 3 || props.node.typeId === 4) ? {
      ...formData.value.config,
      // typeId=3：保留采集点关联字段，避免保存时丢失
      ...(props.node.typeId === 3 ? {
        collectionPointType: props.node.config?.collectionPointType ?? null,
        collectionPointId: props.node.config?.collectionPointId ?? null
      } : {})
    } : null,
    nodeInfo: props.node.typeId === 2 ? formData.value.nodeInfo : null
  };
  emit('update', emitData);
  originalData.value = JSON.parse(JSON.stringify(formData.value));
  editMode.value = false;
};

/**
 * 取消编辑：将表单恢复为进入编辑模式前的快照数据，并退出编辑模式
 */
const handleCancel = () => {
  if (originalData.value) {
    formData.value = JSON.parse(JSON.stringify(originalData.value));
  }
  editMode.value = false;
  emit('cancel');
};

/**
 * 打开碳排放因子选择对话框（记录当前因子值作为"是否手动修改"的比对基准）
 */
const openFactorDialog = () => {
  originalFactorValue.value = formData.value.config.carbonEmissionFactor;
  showFactorDialog.value = true;
};

/**
 * 排放因子选择弹窗"选择"回调：将所选因子值与描述回填到表单
 * @param {Object} result - 因子选择结果 { factorValue, description }
 */
const handleFactorSelect = (result) => {
  if (result.factorValue) {
    formData.value.config.carbonEmissionFactor = result.factorValue;
    formData.value.config.carbonEmissionFactorDescription = result.description || '';
  }
  showFactorDialog.value = false;
};

/**
 * 排放因子选择弹窗"取消"回调：关闭弹窗，不改动表单
 */
const handleFactorCancel = () => {
  showFactorDialog.value = false;
};

/**
 * 排放因子手动输入监听：与打开弹窗时的原始值比对，
 * 若用户手动修改了因子值，则在描述中追加"用户手动修改排放因子数据"说明
 */
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

/* 节点类型下拉框只读展示：保持可用状态外观，但拦截鼠标交互禁止修改 */
.readonly-select {
  pointer-events: none;
}

.factor-input-group {
  display: flex;
  gap: 8px;
  align-items: center;
}

.factor-input-group .el-input {
  flex: 1;
}

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
</style>