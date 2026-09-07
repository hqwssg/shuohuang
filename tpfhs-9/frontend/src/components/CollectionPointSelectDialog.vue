<template>
  <!-- 能耗数据采集点选择对话框 -->
  <el-dialog
    v-model="dialogVisible"
    title="能耗数据采集点选择"
    width="90%"
    class="collection-point-select-dialog"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    @close="handleClose"
  >
    <!-- 顶部：能耗品种大类选择（数据来源于数据字典 emission_category） -->
    <div class="category-bar">
      <span class="category-label">能耗品种大类：</span>
      <el-select
        v-model="selectedCategoryCode"
        placeholder="请选择能耗品种大类"
        class="category-select"
        @change="handleCategoryChange"
      >
        <el-option
          v-for="item in categoryOptions"
          :key="item.code"
          :label="item.value"
          :value="item.code"
        />
      </el-select>
    </div>

    <!-- 主体：三列竖排层级列表（一级范围 / 二级范围 / 具体采集点） -->
    <div class="columns-area">
      <!-- 第1列：一级范围列表（仅显示 name），宽度较原三等分减少40% -->
      <div class="list-column col-scope">
        <div class="column-header">{{ leftColumnTitle }}</div>
        <div class="filter-row">
          <el-input
            v-model="scopeFilter"
            size="small"
            placeholder="拼音码自动筛选/关键词"
            clearable
          />
          <el-button size="small" type="primary" @click="applyScopeFilter">筛选</el-button>
          <el-button size="small" @click="clearScopeFilter">清空</el-button>
        </div>
        <div class="list-body">
          <template v-if="selectedCategoryCode">
            <div
              v-for="item in filteredScopes"
              :key="item.id"
              class="list-item"
              :class="{ active: selectedScope && selectedScope.id === item.id }"
              @click="handleScopeClick(item)"
            >
              <div class="item-name">{{ item.name }}</div>
            </div>
            <div v-if="filteredScopes.length === 0" class="list-empty">暂无数据</div>
          </template>
          <div v-else class="list-empty">请先选择能耗品种大类</div>
        </div>
      </div>

      <!-- 第2列：二级范围列表（显示 name 和 description） -->
      <div class="list-column col-sub">
        <div class="column-header">{{ middleColumnTitle }}</div>
        <div class="filter-row">
          <el-input
            v-model="subFilter"
            size="small"
            placeholder="拼音码自动筛选/关键词"
            clearable
          />
          <el-button size="small" type="primary" @click="applySubFilter">筛选</el-button>
          <el-button size="small" @click="clearSubFilter">清空</el-button>
        </div>
        <div class="list-body">
          <template v-if="selectedScope">
            <div
              v-for="item in filteredSubs"
              :key="item.id"
              class="list-item"
              :class="{ active: selectedSub && selectedSub.id === item.id }"
              @click="handleSubClick(item)"
            >
              <div class="item-name">{{ item.name }}</div>
              <div v-if="item.description" class="item-desc">{{ item.description }}</div>
            </div>
            <div v-if="filteredSubs.length === 0" class="list-empty">暂无数据</div>
          </template>
          <div v-else class="list-empty">请先选择{{ leftColumnTitle }}</div>
        </div>
      </div>

      <!-- 第3列：具体采集点列表（显示 name 和 purpose_description），承接采集范围列让出的宽度 -->
      <div class="list-column col-meter">
        <div class="column-header">采集点</div>
        <!-- 采集点列筛选行：能耗品种小类下拉 + 拼音/关键词输入 + 筛选/清空按键，同行紧凑排列 -->
        <div class="filter-row meter-filter-row">
          <el-select
            v-model="selectedSubcategory"
            placeholder="能耗品种小类"
            clearable
            size="small"
            class="subcategory-select"
          >
            <el-option
              v-for="item in subcategoryOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
          <el-input
            v-model="meterFilter"
            size="small"
            placeholder="拼音码/关键词"
            clearable
            class="meter-filter-input"
          />
          <el-button size="small" type="primary" @click="applyMeterFilter">筛选</el-button>
          <el-button size="small" @click="clearMeterFilter">清空</el-button>
        </div>
        <div class="list-body meter-list-body">
          <template v-if="selectedSub">
            <!-- 单选模式：高亮当前行 + 行首圆形单选钮（表头固定，数据行滚动） -->
            <el-table
              v-if="!multiple"
              :data="filteredMeters"
              height="100%"
              size="small"
              row-key="id"
              highlight-current-row
              :current-row-key="selectedMeterId"
              class="meter-table"
              @current-change="onMeterCurrentChange"
              @row-dblclick="onMeterRowDblclick"
            >
              <el-table-column label="" width="44" align="center" class-name="meter-select-col">
                <template #default="{ row }">
                  <el-radio :model-value="selectedMeterId" :label="row.id" @change="onMeterCurrentChange(row)">
                    <span></span>
                  </el-radio>
                </template>
              </el-table-column>
              <el-table-column prop="name" label="名称" min-width="130" show-overflow-tooltip />
              <el-table-column prop="emissionSubcategory" label="能耗品种小类" min-width="110" show-overflow-tooltip>
                <template #default="{ row }">{{ row.emissionSubcategory || '-' }}</template>
              </el-table-column>
              <el-table-column label="子节点" width="64" align="center">
                <template #default="{ row }">{{ row.parentId && row.parentId !== 0 ? '是' : '否' }}</template>
              </el-table-column>
              <el-table-column label="能耗场景大类" min-width="110" show-overflow-tooltip>
                <template #default="{ row }">{{ energyCategoryName(row.energyCategoryL1) }}</template>
              </el-table-column>
              <el-table-column label="能耗场景二级分类" min-width="120" show-overflow-tooltip>
                <template #default="{ row }">{{ energyCategoryName(row.energyCategoryL2) }}</template>
              </el-table-column>
              <el-table-column prop="purposeDescription" label="采集点描述" min-width="130" show-overflow-tooltip />
            </el-table>
            <!-- 多选模式：复选框列 + 行点击切换（表头固定，数据行滚动） -->
            <el-table
              v-else
              ref="meterTableRef"
              :data="filteredMeters"
              height="100%"
              size="small"
              row-key="id"
              class="meter-table"
              @selection-change="onMeterSelectionChange"
              @row-click="onMeterRowClick"
            >
              <el-table-column type="selection" width="44" align="center" class-name="meter-select-col" />
              <el-table-column prop="name" label="名称" min-width="130" show-overflow-tooltip />
              <el-table-column prop="emissionSubcategory" label="能耗品种小类" min-width="110" show-overflow-tooltip>
                <template #default="{ row }">{{ row.emissionSubcategory || '-' }}</template>
              </el-table-column>
              <el-table-column label="子节点" width="64" align="center">
                <template #default="{ row }">{{ row.parentId && row.parentId !== 0 ? '是' : '否' }}</template>
              </el-table-column>
              <el-table-column label="能耗场景大类" min-width="110" show-overflow-tooltip>
                <template #default="{ row }">{{ energyCategoryName(row.energyCategoryL1) }}</template>
              </el-table-column>
              <el-table-column label="能耗场景二级分类" min-width="120" show-overflow-tooltip>
                <template #default="{ row }">{{ energyCategoryName(row.energyCategoryL2) }}</template>
              </el-table-column>
              <el-table-column prop="purposeDescription" label="采集点描述" min-width="130" show-overflow-tooltip />
            </el-table>
            <div v-if="filteredMeters.length === 0" class="list-empty">暂无数据</div>
          </template>
          <div v-else class="list-empty">请先选择{{ middleColumnTitle }}</div>
        </div>
      </div>
    </div>

    <!-- 对话框底部操作栏：碳排放因子控件（多选）+ 取消 / 确定 -->
    <template #footer>
      <div class="footer-bar">
        <el-form v-if="multiple" :model="factorForm" class="factor-form" label-width="76px">
          <div class="factor-grid">
            <div class="factor-col-left">
              <el-form-item label="碳排放因子" label-width="86px">
                <div class="factor-input-group">
                  <el-input
                    v-model="factorForm.carbonEmissionFactor"
                    class="factor-value-input"
                    placeholder="请输入碳排放因子"
                    @input="handleFactorInput"
                  />
                  <el-button
                    size="small"
                    :disabled="!selectedCategoryCode"
                    @click="openFactorDialog"
                    title="选择碳排放因子"
                  >选择</el-button>
                </div>
              </el-form-item>
              <el-form-item label="因子单位" label-width="86px">
                <el-input
                  :value="factorForm.carbonEmissionFactorUnit"
                  class="factor-unit-input"
                  readonly
                  placeholder="选择因子后自动带出"
                />
              </el-form-item>
            </div>
            <div class="factor-col-right">
              <el-form-item label="描述">
                <el-input
                  :value="factorForm.carbonEmissionFactorDescription"
                  type="textarea"
                  :rows="2"
                  readonly
                  placeholder="碳排放因子描述信息"
                />
              </el-form-item>
            </div>
          </div>
        </el-form>
        <div class="dialog-footer">
          <el-button @click="handleCancel">取消</el-button>
          <el-button type="primary" @click="handleConfirm">确定</el-button>
        </div>
      </div>
    </template>
  </el-dialog>

  <!-- 碳排放因子选择弹窗 -->
  <CarbonEmissionFactorDialog
    v-model:visible="showFactorDialog"
    :emission-category="factorForm.categoryName"
    :emission-subcategory="factorForm.emissionSubcategory"
    @select="handleFactorDialogSelect"
    @cancel="handleFactorDialogCancel"
  />
</template>

<script setup>
import { ref, computed, watch, reactive } from 'vue';
import { ElMessage } from 'element-plus';
import CarbonEmissionFactorDialog from './CarbonEmissionFactorDialog.vue';

/**
 * 组件属性定义
 */
const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  // 默认选中的排放数据大类名称（用于与表单当前值联动）
  defaultCategory: {
    type: String,
    default: ''
  },
  // 选择模式：false-单选（默认），true-多选
  multiple: {
    type: Boolean,
    default: false
  },
  // 当前模版树中已存在的采集点ID列表（多选模式下用于重复添加校验）
  existingPointIds: {
    type: Array,
    default: () => []
  }
});

/**
 * 组件事件定义
 */
const emit = defineEmits(['update:visible', 'select', 'cancel']);

// 排放数据大类编码常量
const CATEGORY_PE = 'PE'; // 购入的电力
const CATEGORY_PH = 'PH'; // 购入的热力
const CATEGORY_FF = 'FF'; // 化石燃料

// 大类编码 → 采集点类型映射（对应 collection_record/emission_node_config 的
// collection_point_type：1-电力表，2-化石燃料，3-外购热能），
// 用于与 existingPointIds（"{类型}:{采集点ID}"复合键）做重复校验
const CATEGORY_CODE_TO_POINT_TYPE = {
  [CATEGORY_PE]: 1,
  [CATEGORY_FF]: 2,
  [CATEGORY_PH]: 3
};

// 对话框显示状态
const dialogVisible = ref(false);
// 排放数据大类下拉选项（来自数据字典）
const categoryOptions = ref([]);
// 当前选中大类编码
const selectedCategoryCode = ref('');

// 各列筛选关键词
const scopeFilter = ref('');
const subFilter = ref('');
const meterFilter = ref('');
// 采集点列的"能耗品种小类"筛选值（空=不筛选，显示全部小类）
const selectedSubcategory = ref('');

// 各列已应用的名称模糊查询关键词（点击"筛选"后生效）
const scopeAppliedName = ref('');
const subAppliedName = ref('');
const meterAppliedName = ref('');

// 各列数据
const scopeList = ref([]);
const subList = ref([]);
const meterList = ref([]);

// 各列选中项
const selectedScope = ref(null);
const selectedSub = ref(null);
// 单选模式：使用单个标量变量存储当前选中的采集点ID
const selectedMeterId = ref(null);
// 多选模式：采集点选中ID集合
const selectedMeterIds = ref(new Set());
// 多选模式表格引用（用于行点击切换勾选）
const meterTableRef = ref(null);

// 碳排放因子弹窗显示状态
const showFactorDialog = ref(false);
// 原始因子值（用于判断用户是否手动修改）
const originalFactorValue = ref('');
// 多选模式下因子表单数据
const factorForm = reactive({
  carbonEmissionFactor: '',
  carbonEmissionFactorUnit: '',
  carbonEmissionFactorDescription: '',
  categoryName: '',
  emissionSubcategory: ''
});

// 能耗分类编码 → 分类名称映射（用于表格中把 energy_category_l1/l2 编码翻译成具体分类内容）
const energyCategoryMap = ref({});

// 当前选中的采集点完整对象（单选：selectedMeterId 对应项；多选：按选中集合取）
const selectedMeterItem = computed(() => {
  if (props.multiple) {
    for (const id of selectedMeterIds.value) {
      return meterList.value.find((m) => m.id === id) || null;
    }
    return null;
  }
  return selectedMeterId.value
    ? meterList.value.find((m) => m.id === selectedMeterId.value) || null
    : null;
});

// 当前选中的采集点完整对象数组（多选模式返回按选中顺序排列的数组，单选模式返回包含选中项的数组）
const selectedMeterItems = computed(() => {
  if (props.multiple) {
    return Array.from(selectedMeterIds.value)
      .map((id) => meterList.value.find((m) => m.id === id))
      .filter(Boolean);
  }
  return selectedMeterId.value
    ? [meterList.value.find((m) => m.id === selectedMeterId.value)].filter(Boolean)
    : [];
});

/**
 * 三列标题按大类动态显示
 */
const leftColumnTitle = computed(() => {
  if (selectedCategoryCode.value === CATEGORY_PE) return '站点/区间';
  if (selectedCategoryCode.value === CATEGORY_FF || selectedCategoryCode.value === CATEGORY_PH) return '采集范围';
  return '一级范围';
});

const middleColumnTitle = computed(() => {
  if (selectedCategoryCode.value === CATEGORY_PE) return '集中器';
  if (selectedCategoryCode.value === CATEGORY_FF || selectedCategoryCode.value === CATEGORY_PH) return '细分范围';
  return '二级范围';
});

/**
 * 当前大类对应的采集配置接口基础路径（FF/PH 使用，PE 使用电表设置树接口）
 */
const collectionApiBase = computed(() => {
  if (selectedCategoryCode.value === CATEGORY_FF) return '/api/fossil-fuel-collection';
  if (selectedCategoryCode.value === CATEGORY_PH) return '/api/purchased-heat-collection';
  return '';
});

/**
 * 判断输入是否为纯英文字母（拼音首字母筛选模式）
 */
const isPinyinInput = (text) => /^[a-zA-Z]+$/.test((text || '').trim());

/**
 * 列数据筛选，支持两种方式：
 * (1) 输入为纯英文字母时，实时按 pinyin_code 字段自动筛选（包含匹配，不区分大小写）；
 * (2) 输入关键词后点击"筛选"按钮，对 name 字段进行模糊查询（等同 SQL 的 like '%关键词%'）。
 * @param {Array} list - 列数据
 * @param {string} text - 输入框当前内容
 * @param {string} appliedName - 已应用的名称模糊查询关键词
 */
const filterColumn = (list, text, appliedName) => {
  const input = (text || '').trim();
  if (input && isPinyinInput(input)) {
    const kw = input.toLowerCase();
    return list.filter((it) => it.pinyinCode && it.pinyinCode.toLowerCase().includes(kw));
  }
  const nameKw = (appliedName || '').trim().toLowerCase();
  if (nameKw) {
    return list.filter((it) => it.name && it.name.toLowerCase().includes(nameKw));
  }
  return list;
};

const filteredScopes = computed(() =>
  filterColumn(scopeList.value, scopeFilter.value, scopeAppliedName.value)
);

const filteredSubs = computed(() =>
  filterColumn(subList.value, subFilter.value, subAppliedName.value)
);

/**
 * 当前采集点列表中出现的"能耗品种小类"去重选项（用于顶部小类筛选下拉）
 * 仅收集非空 emissionSubcategory，按出现顺序去重
 */
const subcategoryOptions = computed(() => {
  const set = new Set();
  const arr = [];
  meterList.value.forEach((m) => {
    const sub = m.emissionSubcategory;
    if (sub && !set.has(sub)) {
      set.add(sub);
      arr.push({ label: sub, value: sub });
    }
  });
  return arr;
});

const filteredMeters = computed(() => {
  let list = filterColumn(meterList.value, meterFilter.value, meterAppliedName.value);
  // 顶部"能耗品种小类"筛选：命中指定小类
  if (selectedSubcategory.value) {
    list = list.filter((m) => m.emissionSubcategory === selectedSubcategory.value);
  }
  return list;
});

/**
 * 各列"筛选"按钮：对 name 字段应用模糊查询关键词
 * （若输入为纯英文字母则由拼音码自动筛选生效，无需点击）
 */
const applyScopeFilter = () => {
  scopeAppliedName.value = isPinyinInput(scopeFilter.value) ? '' : scopeFilter.value;
};
const applySubFilter = () => {
  subAppliedName.value = isPinyinInput(subFilter.value) ? '' : subFilter.value;
};
const applyMeterFilter = () => {
  meterAppliedName.value = isPinyinInput(meterFilter.value) ? '' : meterFilter.value;
};

/**
 * 各列"清空"按钮：清空输入框并取消已应用的筛选
 */
const clearScopeFilter = () => {
  scopeFilter.value = '';
  scopeAppliedName.value = '';
};
const clearSubFilter = () => {
  subFilter.value = '';
  subAppliedName.value = '';
};
const clearMeterFilter = () => {
  meterFilter.value = '';
  meterAppliedName.value = '';
};

/**
 * 监听组件visible属性变化
 * 打开时加载大类字典选项并重置状态；尝试与表单当前大类联动
 */
watch(() => props.visible, async (val) => {
  dialogVisible.value = val;
  if (val) {
    resetAll();
    // 并行加载：排放数据大类选项 + 能耗分类编码映射
    await Promise.all([loadCategoryOptions(), loadEnergyCategoryMap()]);
    // 多选模式：进入时大类下拉清空（不预选），由用户自行选择大类后再逐级筛选
    if (props.multiple) {
      selectedCategoryCode.value = '';
      return;
    }
    // 单选模式：与表单当前的排放数据大类联动预选
    const matched = categoryOptions.value.find((c) => c.value === props.defaultCategory);
    if (matched) {
      selectedCategoryCode.value = matched.code;
      handleCategoryChange(matched.code);
    }
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
 * 重置所有选择状态和列数据
 */
const resetAll = () => {
  scopeFilter.value = '';
  subFilter.value = '';
  meterFilter.value = '';
  selectedSubcategory.value = '';
  selectedCategoryCode.value = '';
  scopeAppliedName.value = '';
  subAppliedName.value = '';
  meterAppliedName.value = '';
  scopeList.value = [];
  subList.value = [];
  meterList.value = [];
  selectedScope.value = null;
  selectedSub.value = null;
  selectedMeterId.value = null;
  selectedMeterIds.value.clear();
  // 重置碳排放因子
  factorForm.carbonEmissionFactor = '';
  factorForm.carbonEmissionFactorUnit = '';
  factorForm.carbonEmissionFactorDescription = '';
  factorForm.categoryName = '';
  factorForm.emissionSubcategory = '';
  originalFactorValue.value = '';
};

/**
 * 加载排放数据大类专业字典选项
 * 数据来源：/api/data-dict/items/emission_category
 */
const loadCategoryOptions = async () => {
  try {
    const response = await fetch('/api/data-dict/items/emission_category');
    if (response.ok) {
      const items = await response.json();
      categoryOptions.value = (Array.isArray(items) ? items : []).map((it) => ({
        code: it.code,
        value: it.value
      }));
    }
  } catch (error) {
    console.error('Failed to load emission categories:', error);
    ElMessage.error('加载能耗品种大类失败');
  }
};

/**
 * 排放数据大类变化事件处理
 * 根据所选大类加载第1列的一级范围列表：
 *   PE-购入的电力 → 电表设置树中的站点/区间（emission_station_interval）
 *   FF-化石燃料   → 化石燃料采集范围（emission_fossil_fuel_collection_scope）
 *   PH-购入的热力 → 外购热能采集范围（emission_purchased_heat_collection_scope）
 * @param {string} code - 大类编码
 */
const handleCategoryChange = async (code) => {
  // 清空下级各列（含筛选条件）
  subFilter.value = '';
  subAppliedName.value = '';
  meterFilter.value = '';
  meterAppliedName.value = '';
  selectedSubcategory.value = '';
  subList.value = [];
  meterList.value = [];
  selectedScope.value = null;
  selectedSub.value = null;
  selectedMeterId.value = null;
  selectedMeterIds.value.clear();

  if (!code) {
    scopeList.value = [];
    return;
  }
  // 同步大类名称到因子表单，供因子弹窗使用
  const matched = categoryOptions.value.find((c) => c.code === code);
  factorForm.categoryName = matched ? matched.value : '';
  // 大类切换后清空因子，单位按新大类重新计算
  factorForm.carbonEmissionFactor = '';
  factorForm.carbonEmissionFactorDescription = '';
  factorForm.emissionSubcategory = '';
  factorForm.carbonEmissionFactorUnit = getEmissionFactorUnit(factorForm.categoryName, '');

  try {
    if (code === CATEGORY_PE) {
      // 从电表设置树中提取站点/区间
      const response = await fetch('/api/meter-settings/tree');
      if (!response.ok) {
        ElMessage.error('加载站点/区间失败');
        return;
      }
      const tree = await response.json();
      // 返回的即为根节点（"电表设置"），站点/区间为其下级 station 节点
      scopeList.value = collectStations(tree);
    } else {
      const response = await fetch(`${collectionApiBase.value}/scopes`);
      if (!response.ok) {
        ElMessage.error('加载采集范围失败');
        return;
      }
      const data = await response.json();
      scopeList.value = (Array.isArray(data) ? data : []).map((s) => ({
        id: s.id,
        name: s.name,
        pinyinCode: s.pinyinCode || '',
        description: s.description || ''
      }));
    }
  } catch (error) {
    console.error('Failed to load scopes:', error);
    ElMessage.error('加载一级范围失败');
  }
};

/**
 * 从电表设置树中收集所有站点/区间节点
 * @param {Object} node - 树节点
 * @returns {Array} 站点/区间节点数组
 */
const collectStations = (node) => {
  const result = [];
  if (!node) return result;
  if (node.nodeType === 'station') {
    result.push({
      id: node.id,
      name: node.name,
      pinyinCode: node.pinyinCode || '',
      children: node.children || []
    });
  }
  (node.children || []).forEach((c) => result.push(...collectStations(c)));
  return result;
};

/**
 * 第1列点击事件：选中一级范围并加载第2列
 * PE → 集中器（emission_concentrator，取树中站点的 children）
 * FF/PH → 细分范围（emission_fossil_fuel_collection_sub_scope / emission_purchased_heat_collection_sub_scope）
 * @param {Object} item - 选中的一级范围
 */
const handleScopeClick = async (item) => {
  selectedScope.value = item;
  subFilter.value = '';
  subAppliedName.value = '';
  subList.value = [];
  meterFilter.value = '';
  meterAppliedName.value = '';
  selectedSubcategory.value = '';
  meterList.value = [];
  selectedSub.value = null;
  selectedMeterId.value = null;
  selectedMeterIds.value.clear();

  try {
    if (selectedCategoryCode.value === CATEGORY_PE) {
      subList.value = (item.children || [])
        .filter((c) => c.nodeType === 'concentrator')
        .map((c) => ({
          id: c.id,
          name: c.name,
          pinyinCode: c.pinyinCode || '',
          description: c.description || '',
          children: c.children || []
        }));
    } else {
      const response = await fetch(`${collectionApiBase.value}/scopes/${item.id}/sub-scopes`);
      if (!response.ok) {
        ElMessage.error('加载细分范围失败');
        return;
      }
      const data = await response.json();
      subList.value = (Array.isArray(data) ? data : []).map((s) => ({
        id: s.id,
        name: s.name,
        pinyinCode: s.pinyinCode || '',
        description: s.description || ''
      }));
    }
  } catch (error) {
    console.error('Failed to load sub scopes:', error);
    ElMessage.error('加载二级范围失败');
  }
};

/**
 * 第2列点击事件：选中二级范围并加载第3列具体采集点
 * PE → 集中器下的电表（emission_meter_info，含多级子电表递归拍平）
 * FF → 化石燃料计量表采集点（emission_fossil_fuel_meter_info）
 * PH → 外购热能计量表采集点（emission_purchased_heat_meter_info）
 * @param {Object} item - 选中的二级范围
 */
const handleSubClick = async (item) => {
  selectedSub.value = item;
  meterFilter.value = '';
  meterAppliedName.value = '';
  selectedSubcategory.value = '';
  meterList.value = [];
  selectedMeterId.value = null;
  selectedMeterIds.value.clear();

  try {
    if (selectedCategoryCode.value === CATEGORY_PE) {
      // 从集中器 children 中递归拍平所有电表（含多级子采集点）
      meterList.value = flattenMetersFromTree(item.children || []);
    } else {
      const response = await fetch(`${collectionApiBase.value}/sub-scopes/${item.id}/meters`);
      if (!response.ok) {
        ElMessage.error('加载采集点失败');
        return;
      }
      const data = await response.json();
      // 接口返回该细分范围下的全部采集点（含子采集点），统一映射为标准结构
      meterList.value = (Array.isArray(data) ? data : []).map((m) => ({
        id: m.id,
        parentId: m.parentMeterId ?? 0,
        name: m.name,
        pinyinCode: m.pinyinCode || '',
        purposeDescription: m.purposeDescription || '',
        measurementUnit: m.measurementUnit || '',
        dataSourceSystem: m.dataSourceSystem || '',
        isCumulative: m.isCumulative,
        isMobileSource: m.isMobileSource,
        energyUseCategory: m.energyUseCategory || '',
        energyCategoryL1: m.energyCategoryL1 || '',
        energyCategoryL2: m.energyCategoryL2 || '',
        energyCategoryL3: m.energyCategoryL3 || '',
        emissionSubcategory: m.emissionSubcategory || m.fuelType || m.heatType || '',
        energyAllocation: m.energyAllocation || '',
        meterReadingMethod: m.meterReadingMethod,
        billingCycleUnit: m.billingCycleUnit,
        billingCycleStartDate: m.billingCycleStartDate,
        billingCycleLength: m.billingCycleLength
      }));
    }
  } catch (error) {
    console.error('Failed to load meters:', error);
    ElMessage.error('加载采集点失败');
  }
};

/**
 * 从电表设置树的节点列表中递归拍平所有电表节点
 * @param {Array} nodes - 树节点数组
 * @returns {Array} 电表节点数组（标准结构）
 */
const flattenMetersFromTree = (nodes) => {
  const result = [];
  nodes.forEach((n) => {
    if (n.nodeType === 'meter') {
      result.push({
        id: n.id,
        parentId: n.parentId ?? 0,
        name: n.name,
        pinyinCode: n.pinyinCode || '',
        purposeDescription: n.description || '',
        measurementUnit: n.measurementUnit || '',
        dataSourceSystem: n.dataSourceSystem || '',
        isCumulative: n.isCumulative,
        isMobileSource: n.isMobileSource,
        energyUseCategory: n.energyUseCategory || '',
        energyCategoryL1: n.energyCategoryL1 || '',
        energyCategoryL2: n.energyCategoryL2 || '',
        energyCategoryL3: n.energyCategoryL3 || '',
        emissionSubcategory: n.emissionSubcategory || '',
        energyAllocation: n.energyAllocation || '',
        meterReadingMethod: n.meterReadingMethod,
        billingCycleUnit: n.billingCycleUnit,
        billingCycleStartDate: n.billingCycleStartDate,
        billingCycleLength: n.billingCycleLength,
        children: n.children || []
      });
      result.push(...flattenMetersFromTree(n.children || []));
    }
  });
  return result.map(({ children, ...rest }) => rest);
};

/**
 * 加载能耗分类编码 → 名称映射（数据来自 /api/energy-categories 全量扁平列表）
 * 用于表格中将 energy_category_l1/l2 编码翻译为具体分类内容
 */
const loadEnergyCategoryMap = async () => {
  try {
    const response = await fetch('/api/energy-categories');
    if (response.ok) {
      const items = await response.json();
      const map = {};
      (Array.isArray(items) ? items : []).forEach((it) => {
        if (it && it.categoryCode) {
          map[it.categoryCode] = it.categoryName || it.categoryCode;
        }
      });
      energyCategoryMap.value = map;
    }
  } catch (error) {
    console.error('Failed to load energy categories:', error);
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
 * 根据能耗品种大类和小类计算默认碳排放因子单位（与编辑节点弹窗一致）
 */
const getEmissionFactorUnit = (category, subcategory) => {
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
    if (subcategory === '质量单位计量的热水' || subcategory === '质量单位计量的蒸汽') {
      return 'kgCO₂/kg';
    }
    return 'kgCO₂/GJ';
  }
  if (category === '新能源发电（自发自用）') {
    return 'kgCO₂/kWh';
  }
  return '';
};

/**
 * 打开碳排放因子选择弹窗：选中的第一个采集点的小类作为筛选条件
 */
const openFactorDialog = () => {
  if (!selectedCategoryCode.value) {
    ElMessage.warning('请先选择能耗品种大类');
    return;
  }
  // 优先使用第一个选中采集点的子分类，没有则留空
  let subcategory = '';
  const firstSelected = props.multiple ? selectedMeterItems.value[0] : selectedMeterItem.value;
  if (firstSelected && firstSelected.emissionSubcategory) {
    subcategory = firstSelected.emissionSubcategory;
  }
  factorForm.emissionSubcategory = subcategory;
  // 同步单位（以小类为准）
  factorForm.carbonEmissionFactorUnit = getEmissionFactorUnit(factorForm.categoryName, subcategory);
  originalFactorValue.value = factorForm.carbonEmissionFactor;
  showFactorDialog.value = true;
};

/**
 * 碳排放因子选择弹窗确认回调
 */
const handleFactorDialogSelect = (result) => {
  if (result && result.factorValue !== undefined && result.factorValue !== null) {
    factorForm.carbonEmissionFactor = String(result.factorValue);
    factorForm.carbonEmissionFactorDescription = result.description || '';
    originalFactorValue.value = factorForm.carbonEmissionFactor;
  }
  showFactorDialog.value = false;
};

/**
 * 碳排放因子选择弹窗取消回调
 */
const handleFactorDialogCancel = () => {
  showFactorDialog.value = false;
};

/**
 * 碳排放因子设置输入框变化回调：手动修改时自动追加描述标记
 */
const handleFactorInput = () => {
  const currentValue = factorForm.carbonEmissionFactor;
  if (String(currentValue) !== String(originalFactorValue.value)) {
    factorForm.carbonEmissionFactorDescription = `${currentValue}用户手动修改排放因子数据`;
  }
};

/**
 * 第3列表格事件：单选模式当前行变化
 * @param {Object|null} row - 当前选中行（清空时为 null，不处理）
 */
const onMeterCurrentChange = (row) => {
  if (row) {
    selectedMeterId.value = row.id;
  }
};

/**
 * 第3列表格事件：单选模式双击行 = 选中并确认返回
 */
const onMeterRowDblclick = (row) => {
  if (!row) return;
  selectedMeterId.value = row.id;
  handleConfirm();
};

/**
 * 第3列表格事件：多选模式勾选变化（同步选中ID集合）
 */
const onMeterSelectionChange = (rows) => {
  selectedMeterIds.value = new Set((rows || []).map((r) => r.id));
};

/**
 * 第3列表格事件：多选模式点击行切换勾选
 * 注意：点击复选框列本身时跳过，避免复选框与行点击双重切换
 */
const onMeterRowClick = (row, column) => {
  if (!row) return;
  if (column && column.type === 'selection') return;
  meterTableRef.value?.toggleRowSelection(row);
};

/**
 * 点击确定按钮
 * 单选模式：emit 单个采集点对象；多选模式：emit 采集点对象数组
 * 返回的数据包含所属大类（categoryCode/categoryName）
 * <p>
 * 校验流程：
 *   1. 必选校验：单选需已选中一行；多选需至少勾选一个采集点
 *   2. 多选模式查重校验：用 Set 与 props.existingPointIds（当前模版树中已存在采集点的"{类型}:{ID}"复合键）比对，
 *      命中的采集点从提交列表中剔除并提示"XXX、YYY采集点已经存在，不能添加！每个采集点在同一碳排放模版中只能存在一个。"；
 *      若全部重复则保留弹窗等待调整，部分重复则关闭弹窗只添加未重复点。
 *      查重范围仅限当前模版（existingPointIds 只收集当前模版树的采集点），其他模版不影响本校验
 *   注意：碳排放因子可为空，未设置时使用模版缺省因子。
 * </p>
 */
const handleConfirm = () => {
  // 单选：靠 selectedMeterId 判断；多选：靠 selectedMeterIds 判断
  const hasSelection = props.multiple ? selectedMeterIds.value.size > 0 : !!selectedMeterId.value;
  if (!hasSelection) {
    ElMessage.warning(props.multiple ? '请至少选择一个采集点' : '请先选择一个采集点');
    return;
  }
  if (!props.multiple && !selectedMeterItem.value) {
    // 选中行可能已被筛选/切换清除
    ElMessage.warning('请先选择一个采集点');
    return;
  }
  // 多选模式：碳排放因子可以为空（未设置时使用模版缺省因子）
  const category = categoryOptions.value.find((c) => c.code === selectedCategoryCode.value);
  const categoryName = category ? category.value : '';

  const withCategory = (m) => ({ ...m, categoryCode: selectedCategoryCode.value, categoryName });

  let itemsToEmit = selectedMeterItems.value;

  // 多选模式：校验所选采集点是否已存在于当前模版树中（每个采集点在同一碳排放模版中只能存在一个）
  // 比对键为"{类型}:{采集点ID}"复合键：三张采集点表主键独立编号，仅按 ID 比对会误判不同类型的同号采集点。
  // 所选采集点必属当前选中的大类（切换大类时会清空已选集合），故类型取自当前大类编码映射。
  if (props.multiple && props.existingPointIds.length > 0) {
    const currentType = CATEGORY_CODE_TO_POINT_TYPE[selectedCategoryCode.value];
    const existSet = new Set(props.existingPointIds);
    const keyOf = (m) => `${currentType}:${m.id}`;
    const duplicates = itemsToEmit.filter((m) => m && m.id != null && existSet.has(keyOf(m)));
    if (duplicates.length > 0) {
      ElMessage.error(
        `${duplicates.map((m) => m.name).join('、')}采集点已经存在，不能添加！每个采集点在同一碳排放模版中只能存在一个。`
      );
      itemsToEmit = itemsToEmit.filter((m) => !(m && m.id != null && existSet.has(keyOf(m))));
      if (itemsToEmit.length === 0) {
        // 所选采集点全部已存在：不关闭弹窗，等待用户调整选择
        return;
      }
    }
  }

  if (props.multiple) {
    emit('select', {
      categoryCode: selectedCategoryCode.value,
      categoryName,
      carbonEmissionFactor: factorForm.carbonEmissionFactor,
      carbonEmissionFactorUnit: factorForm.carbonEmissionFactorUnit,
      carbonEmissionFactorDescription: factorForm.carbonEmissionFactorDescription,
      points: itemsToEmit.map(withCategory)
    });
  } else {
    emit('select', withCategory(selectedMeterItem.value));
  }
  handleClose();
};

/**
 * 点击取消按钮
 */
const handleCancel = () => {
  emit('cancel', { status: 'cancel' });
  handleClose();
};

/**
 * 关闭对话框
 */
const handleClose = () => {
  dialogVisible.value = false;
};
</script>

<style scoped>
/* 顶部大类选择栏 */
.category-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.category-label {
  white-space: nowrap;
}

.category-select {
  width: 260px;
}

/* 三列竖排区域 */
.columns-area {
  display: flex;
  gap: 16px;
  height: 100%;
  min-height: 0;
}

/* 单列布局：采集范围列较原三等分减少40%宽度（6份），该部分宽度分配给采集点列（14份），中间列不变（10份） */
.list-column {
  display: flex;
  flex-direction: column;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 8px;
  min-width: 0;
}

.col-scope {
  flex: 6;
}

.col-sub {
  flex: 10;
}

.col-meter {
  flex: 14;
}

.column-header {
  font-weight: bold;
  margin-bottom: 8px;
}

/* 筛选输入框 + 筛选/清空按钮 */
.filter-row {
  display: flex;
  align-items: center;
  gap: 2px;
  margin-bottom: 8px;
}

.filter-row .el-input {
  flex: 1;
  min-width: 0;
}

.filter-row .el-button {
  flex-shrink: 0;
}

/* 采集点列筛选行：小类下拉 + 拼音/关键词输入 + 筛选/清空按键同行紧凑排列 */
.meter-filter-row {
  gap: 4px;
}

/* 能耗品种小类下拉框：固定窄宽，不抢占输入框空间 */
.meter-filter-row .subcategory-select {
  width: 30%;
  flex: 0 0 30%;
}

/* 拼音/关键词输入框：窄宽，可自适应收缩 */
.meter-filter-row .meter-filter-input {
  width: 200px;
  flex: 0 1 60%;
  min-width: 90px;
}

.list-body {
  flex: 1;
  overflow-y: auto;
  min-height: 200px;
}

/* 列表项 */
.list-item {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  padding: 6px 10px;
  cursor: pointer;
  border-radius: 4px;
}

.list-item:hover {
  background-color: #f5f7fa;
}

.list-item.active {
  background-color: #ecf5ff;
}

.list-item .item-content {
  flex: 1;
  min-width: 0;
}

.item-name {
  word-break: break-all;
}

/* 采集点列表：二维表格展示（表头固定不滚动，数据行在表体内部滚动） */
.meter-list-body {
  overflow: hidden; /* 滚动交给 el-table 内部处理，避免出现双重滚动条 */
}

.meter-table {
  width: 100%;
}

/* 表格行首选择列：单选钮/复选框与单元格对齐，去掉默认右边距 */
.meter-table .el-radio {
  margin-right: 0;
}
.meter-table .el-radio .el-radio__label {
  display: none; /* 单选钮不显示文字标签 */
}
.meter-table .el-checkbox {
  margin-right: 0;
}

/* "子节点"列文字居中已由列配置处理；空值显示弱化 */
.meter-table .cell {
  line-height: 1.5;
}

.list-empty {
  text-align: center;
  color: #909399;
  padding: 24px 0;
  font-size: 13px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-left: auto;
  flex-shrink: 0;
}

/* 底部碳排放因子控件（仅多选模式显示）：与取消/确定按键同行，紧凑布局
   左列（碳排放因子+因子单位）占碳排放整体的1/3宽度，右列（描述）占2/3 */
.footer-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  width: 100%;
}

.factor-form {
  flex: 1;
  min-width: 0;
}

.factor-form .el-form-item {
  margin-bottom: 0;
}

.factor-grid {
  display: flex;
  align-items: center;
  gap: 12px;
}

.factor-col-left {
  flex: 1 1 0;
  min-width: 0;
}

.factor-col-right {
  flex: 2 1 0;
  min-width: 0;
}

.factor-input-group {
  display: flex;
  align-items: center;
  gap: 6px;
}

.factor-value-input,
.factor-unit-input {
  width: 120px;
}

.factor-col-right .el-textarea {
  width: 100%;
}
</style>

<style>
/* 弹窗尺寸：宽度与高度均为主窗体的90%，垂直居中 */
.collection-point-select-dialog {
  height: 90vh !important;
  margin-top: 5vh !important;
  margin-bottom: 5vh !important;
  display: flex !important;
  flex-direction: column;
}

.collection-point-select-dialog .el-dialog__body {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.collection-point-select-dialog .columns-area {
  flex: 1;
}
</style>