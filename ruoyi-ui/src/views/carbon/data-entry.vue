<template>
  <div class="app-container carbon-data-entry">
    <div class="carbon-toolbar">
      <div>
        <h2>人工数据录入</h2>
        <span>按节点口径完成手工填报、校验、审核与追溯</span>
      </div>
      <div class="toolbar-actions">
        <el-button icon="el-icon-refresh" :loading="loadingTree" @click="loadTree">刷新节点</el-button>
        <el-button v-if="canEditData" type="primary" icon="el-icon-download" @click="downloadImportTemplate">下载导入模板</el-button>
      </div>
    </div>

    <el-alert
      v-if="serviceError"
      class="service-alert"
      type="warning"
      :title="serviceError"
      show-icon
      :closable="false"
    />

    <div class="entry-workbench">
      <aside class="node-panel">
        <div class="panel-title">
          <span>录入对象</span>
          <small>{{ entryNodeCount }} 个采集点</small>
        </div>
        <el-select
          v-model="query.templateId"
          class="template-select"
          filterable
          placeholder="选择模板"
          @change="handleTemplateChange"
        >
          <el-option v-for="item in templates" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
        <el-input
          v-model="treeKeyword"
          class="tree-search"
          clearable
          prefix-icon="el-icon-search"
          placeholder="搜索节点"
        />
        <el-checkbox v-model="manualOnly" class="manual-filter" @change="selectFirstNode">只看手工录入采集点</el-checkbox>

        <el-tree
          ref="nodeTree"
          v-loading="loadingTree"
          class="node-tree"
          :data="filteredTree"
          :props="treeProps"
          node-key="id"
          default-expand-all
          highlight-current
          :filter-node-method="filterNode"
          @node-click="handleNodeClick"
        >
          <span slot-scope="{ node, data }" class="tree-node" :class="{ 'is-entry': isEntryNode(data) }">
            <i :class="nodeIcon(data)"></i>
            <span>{{ node.label }}</span>
            <el-tag v-if="isEntryNode(data)" size="mini" :type="isManualNode(data) ? 'success' : 'info'">
              {{ isManualNode(data) ? '手工' : '其他' }}
            </el-tag>
          </span>
        </el-tree>
      </aside>

      <main class="entry-main">
        <el-empty v-if="!selectedNode" description="请选择左侧采集点后录入数据" />

        <template v-else>
          <section class="caliber-card">
            <div class="caliber-head">
              <div>
                <span class="eyebrow">当前采集点</span>
                <h3>{{ selectedNode.name }}</h3>
              </div>
              <el-tag :type="isManualNode(selectedNode) ? 'success' : 'warning'">
                {{ isManualNode(selectedNode) ? '手工录入' : '非手工来源' }}
              </el-tag>
            </div>
            <div class="caliber-grid">
              <div>
                <span>排放大类</span>
                <strong>{{ selectedConfig.emissionCategory || '-' }}</strong>
              </div>
              <div>
                <span>排放小类</span>
                <strong>{{ selectedConfig.emissionSubcategory || '-' }}</strong>
              </div>
              <div>
                <span>计量单位</span>
                <strong>{{ selectedConfig.measurementUnit || '-' }}</strong>
              </div>
              <div>
                <span>排放因子</span>
                <strong>{{ formatNumber(selectedConfig.carbonEmissionFactor) }}</strong>
              </div>
              <div>
                <span>分摊比例</span>
                <strong>{{ formatRatio(selectedConfig.allocationRatio) }}</strong>
              </div>
              <div>
                <span>采集编码</span>
                <strong>{{ selectedConfig.equipmentCode || '-' }}</strong>
              </div>
            </div>
          </section>

          <el-tabs v-model="activeTab" class="entry-tabs" @tab-click="handleTabClick">
            <el-tab-pane label="单条录入" name="single">
              <section class="entry-section">
                <el-form ref="entryForm" :model="form" :rules="rules" label-width="104px">
                  <el-row :gutter="16">
                    <el-col :xs="24" :md="8">
                      <el-form-item label="统计周期" prop="periodType">
                        <el-select v-model="form.periodType" style="width: 100%" @change="syncPeriodEnd">
                          <el-option v-for="item in periodOptions" :key="item.value" :label="item.label" :value="item.value" />
                        </el-select>
                      </el-form-item>
                    </el-col>
                    <el-col :xs="24" :md="8">
                      <el-form-item label="开始日期" prop="periodStart">
                        <el-date-picker
                          v-model="form.periodStart"
                          type="date"
                          value-format="yyyy-MM-dd"
                          style="width: 100%"
                          @change="syncPeriodEnd"
                        />
                      </el-form-item>
                    </el-col>
                    <el-col :xs="24" :md="8">
                      <el-form-item label="结束日期" prop="periodEnd">
                        <el-date-picker
                          v-model="form.periodEnd"
                          type="date"
                          value-format="yyyy-MM-dd"
                          style="width: 100%"
                          :disabled="form.periodType !== 'CUSTOM'"
                        />
                      </el-form-item>
                    </el-col>
                  </el-row>

                  <el-row :gutter="16">
                    <el-col :xs="24" :md="8">
                      <el-form-item label="活动数据" prop="activityValue">
                        <el-input-number v-model="form.activityValue" :min="0" :precision="6" style="width: 100%" />
                      </el-form-item>
                    </el-col>
                    <el-col :xs="24" :md="8">
                      <el-form-item label="计量单位" prop="activityUnit">
                        <el-select v-model="form.activityUnit" allow-create filterable style="width: 100%">
                          <el-option v-for="unit in unitOptions" :key="unit" :label="unit" :value="unit" />
                        </el-select>
                      </el-form-item>
                    </el-col>
                    <el-col :xs="24" :md="8">
                      <el-form-item label="换算系数" prop="unitConvertRate">
                        <el-input-number v-model="form.unitConvertRate" :min="0.000001" :precision="8" style="width: 100%" />
                      </el-form-item>
                    </el-col>
                  </el-row>

                  <el-row :gutter="16">
                    <el-col :xs="24" :md="8">
                      <el-form-item label="排放因子" prop="emissionFactor">
                        <el-input-number v-model="form.emissionFactor" :min="0" :precision="8" style="width: 100%" />
                      </el-form-item>
                    </el-col>
                    <el-col :xs="24" :md="8">
                      <el-form-item label="因子单位">
                        <el-input v-model="form.factorUnit" clearable />
                      </el-form-item>
                    </el-col>
                    <el-col :xs="24" :md="8">
                      <el-form-item label="分摊比例">
                        <el-input-number v-model="form.allocationRatio" :min="0" :max="100" :precision="4" style="width: 100%" />
                      </el-form-item>
                    </el-col>
                  </el-row>

                  <el-row :gutter="16">
                    <el-col :xs="24" :md="8">
                      <el-form-item label="碳排放量">
                        <el-input :value="formatNumber(estimatedEmission)" disabled>
                          <template slot="append">tCO2e</template>
                        </el-input>
                      </el-form-item>
                    </el-col>
                    <el-col :xs="24" :md="8">
                      <el-form-item label="凭证编号">
                        <el-input v-model="form.sourceNo" clearable placeholder="如台账编号、电表编号" />
                      </el-form-item>
                    </el-col>
                    <el-col :xs="24" :md="8">
                      <el-form-item label="凭证地址">
                        <el-input v-model="form.evidenceUrl" clearable placeholder="附件地址或文件名" />
                      </el-form-item>
                    </el-col>
                  </el-row>

                  <el-form-item label="备注说明">
                    <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="补录、估算、异常或修正原因" />
                  </el-form-item>
                </el-form>

                <div class="form-actions">
                  <el-button @click="resetEntryForm">重置</el-button>
                  <el-button v-if="canEditData" type="primary" plain :loading="saving" @click="saveEntry('DRAFT')">保存草稿</el-button>
                  <el-button v-if="canSubmitData" type="success" :loading="saving" @click="saveEntry('SUBMITTED')">提交审核</el-button>
                </div>
              </section>
            </el-tab-pane>

            <el-tab-pane label="批量导入" name="batch">
              <section class="entry-section">
                <div class="batch-toolbar">
                  <input ref="importFile" type="file" accept=".csv,text/csv" class="file-input" @change="handleImportFile">
                  <el-button v-if="canEditData" icon="el-icon-upload2" @click="$refs.importFile.click()">选择 CSV 文件</el-button>
                  <el-button v-if="canEditData" type="primary" :disabled="!validImportRows.length" :loading="importing" @click="importBatch">导入有效数据</el-button>
                  <span>{{ importRows.length }} 行预览，{{ validImportRows.length }} 行有效</span>
                </div>
                <el-alert
                  v-if="importErrors.length"
                  class="import-alert"
                  type="warning"
                  show-icon
                  :closable="false"
                  :title="importErrors.slice(0, 3).join('；')"
                />
                <el-table :data="importRows" border max-height="420">
                  <el-table-column label="状态" width="90" align="center">
                    <template slot-scope="scope">
                      <el-tag size="mini" :type="scope.row._error ? 'danger' : 'success'">
                        {{ scope.row._error ? '异常' : '有效' }}
                      </el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column label="节点" prop="nodeName" min-width="150" show-overflow-tooltip />
                  <el-table-column label="周期" width="120">
                    <template slot-scope="scope">{{ periodLabel(scope.row.periodType) }}</template>
                  </el-table-column>
                  <el-table-column label="开始日期" prop="periodStart" width="120" />
                  <el-table-column label="结束日期" prop="periodEnd" width="120" />
                  <el-table-column label="活动数据" prop="activityValue" width="120" align="right" />
                  <el-table-column label="单位" prop="activityUnit" width="90" />
                  <el-table-column label="问题" prop="_error" min-width="200" show-overflow-tooltip />
                </el-table>
              </section>
            </el-tab-pane>

            <el-tab-pane v-if="canReviewData" :label="`待审核(${pendingRecords.length})`" name="review">
              <section class="entry-section">
                <el-table v-loading="loadingRecords" :data="pendingRecords" border>
                  <el-table-column label="周期" width="180">
                    <template slot-scope="scope">{{ periodText(scope.row) }}</template>
                  </el-table-column>
                  <el-table-column label="活动数据" min-width="130" align="right">
                    <template slot-scope="scope">{{ formatNumber(scope.row.activityValue) }} {{ scope.row.activityUnit }}</template>
                  </el-table-column>
                  <el-table-column label="碳排放量" prop="carbonEmission" width="130" align="right">
                    <template slot-scope="scope">{{ formatNumber(scope.row.carbonEmission) }}</template>
                  </el-table-column>
                  <el-table-column label="凭证" min-width="160" show-overflow-tooltip>
                    <template slot-scope="scope">{{ scope.row.sourceNo || scope.row.evidenceUrl || '-' }}</template>
                  </el-table-column>
                  <el-table-column label="提交时间" prop="updatedAt" width="170" />
                  <el-table-column label="操作" width="210" align="center" fixed="right">
                    <template slot-scope="scope">
                      <el-button v-if="canReviewData" type="text" icon="el-icon-check" @click="approveRecord(scope.row)">通过</el-button>
                      <el-button v-if="canReviewData" type="text" icon="el-icon-close" @click="openReject(scope.row)">驳回</el-button>
                      <el-button type="text" icon="el-icon-document" @click="viewAudits(scope.row)">流水</el-button>
                    </template>
                  </el-table-column>
                </el-table>
              </section>
            </el-tab-pane>

            <el-tab-pane label="历史记录" name="history">
              <section class="entry-section">
                <el-form :model="historyQuery" size="small" :inline="true" class="history-filter">
                  <el-form-item label="日期">
                    <el-date-picker
                      v-model="historyQuery.dateRange"
                      type="daterange"
                      value-format="yyyy-MM-dd"
                      range-separator="-"
                      start-placeholder="开始日期"
                      end-placeholder="结束日期"
                      style="width: 260px"
                    />
                  </el-form-item>
                  <el-form-item label="状态">
                    <el-select v-model="historyQuery.status" clearable style="width: 140px">
                      <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
                    </el-select>
                  </el-form-item>
                  <el-form-item>
                    <el-button type="primary" icon="el-icon-search" @click="handleHistoryQuery">查询</el-button>
                    <el-button icon="el-icon-refresh" @click="resetHistoryQuery">重置</el-button>
                  </el-form-item>
                </el-form>

                <el-table v-loading="loadingRecords" :data="historyRecords" border>
                  <el-table-column label="状态" width="100" align="center">
                    <template slot-scope="scope">
                      <el-tag size="mini" :type="statusType(scope.row.status)">{{ statusLabel(scope.row.status) }}</el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column label="周期" width="180">
                    <template slot-scope="scope">{{ periodText(scope.row) }}</template>
                  </el-table-column>
                  <el-table-column label="活动数据" min-width="140" align="right">
                    <template slot-scope="scope">{{ formatNumber(scope.row.activityValue) }} {{ scope.row.activityUnit }}</template>
                  </el-table-column>
                  <el-table-column label="因子" prop="emissionFactor" width="110" align="right">
                    <template slot-scope="scope">{{ formatNumber(scope.row.emissionFactor) }}</template>
                  </el-table-column>
                  <el-table-column label="碳排放量" prop="carbonEmission" width="130" align="right">
                    <template slot-scope="scope">{{ formatNumber(scope.row.carbonEmission) }}</template>
                  </el-table-column>
                  <el-table-column label="凭证" min-width="150" show-overflow-tooltip>
                    <template slot-scope="scope">{{ scope.row.sourceNo || scope.row.evidenceUrl || '-' }}</template>
                  </el-table-column>
                  <el-table-column label="备注" prop="remark" min-width="160" show-overflow-tooltip />
                  <el-table-column label="操作" width="260" align="center" fixed="right">
                    <template slot-scope="scope">
                      <el-button v-if="canEditData && canEdit(scope.row)" type="text" icon="el-icon-edit" @click="editRecord(scope.row)">编辑</el-button>
                      <el-button v-if="canSubmitData && canSubmit(scope.row)" type="text" icon="el-icon-position" @click="submitRecord(scope.row)">提交</el-button>
                      <el-button v-if="canLockData && scope.row.status === 'APPROVED'" type="text" icon="el-icon-lock" @click="lockRecord(scope.row)">锁定</el-button>
                      <el-button type="text" icon="el-icon-document" @click="viewAudits(scope.row)">流水</el-button>
                      <el-button v-if="canLockData && canVoid(scope.row)" type="text" icon="el-icon-delete" @click="voidRecord(scope.row)">作废</el-button>
                    </template>
                  </el-table-column>
                </el-table>

                <el-pagination
                  class="table-pagination"
                  background
                  layout="total, sizes, prev, pager, next, jumper"
                  :total="historyTotal"
                  :page-size.sync="historyQuery.size"
                  :current-page.sync="historyQuery.page"
                  :page-sizes="[10, 20, 50, 100]"
                  @size-change="loadHistory"
                  @current-change="loadHistory"
                />
              </section>
            </el-tab-pane>
          </el-tabs>
        </template>
      </main>
    </div>

    <el-dialog title="驳回原因" :visible.sync="rejectVisible" width="460px" append-to-body>
      <el-input v-model="rejectComment" type="textarea" :rows="4" placeholder="请输入驳回原因" />
      <div slot="footer" class="dialog-footer">
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="rejectRecord">确定驳回</el-button>
      </div>
    </el-dialog>

    <el-dialog title="操作流水" :visible.sync="auditVisible" width="720px" append-to-body>
      <el-timeline v-if="auditLogs.length">
        <el-timeline-item
          v-for="item in auditLogs"
          :key="item.id"
          :timestamp="item.operatedAt"
          placement="top"
        >
          <div class="audit-item">
            <strong>{{ auditActionLabel(item.action) }}</strong>
            <span>{{ statusLabel(item.fromStatus) }} -> {{ statusLabel(item.toStatus) }}</span>
            <p v-if="item.comment">{{ item.comment }}</p>
          </div>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="暂无操作流水" />
    </el-dialog>
  </div>
</template>

<script>
import { templateApi, nodeApi, emissionApi, flattenCarbonTree } from '@/api/carbon'
import { hasAnyPermission, hasAnyRole } from '@/utils/permissionMatch'

function today() {
  const date = new Date()
  return formatDate(date)
}

function formatDate(date) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

function parseDate(value) {
  if (!value) return null
  const parts = String(value).split('-').map(Number)
  return new Date(parts[0], (parts[1] || 1) - 1, parts[2] || 1)
}

export default {
  name: 'CarbonDataEntry',
  data() {
    return {
      loadingTree: false,
      loadingRecords: false,
      saving: false,
      importing: false,
      serviceError: '',
      templates: [],
      rawTree: null,
      treeKeyword: '',
      manualOnly: false,
      selectedNode: null,
      activeTab: 'single',
      treeProps: {
        children: 'children',
        label: 'name'
      },
      query: {
        templateId: null
      },
      form: this.emptyForm(),
      rules: {
        periodType: [{ required: true, message: '请选择统计周期', trigger: 'change' }],
        periodStart: [{ required: true, message: '请选择开始日期', trigger: 'change' }],
        periodEnd: [{ required: true, message: '请选择结束日期', trigger: 'change' }],
        activityValue: [{ required: true, message: '请输入活动数据', trigger: 'blur' }],
        activityUnit: [{ required: true, message: '请选择计量单位', trigger: 'change' }],
        unitConvertRate: [{ required: true, message: '请输入换算系数', trigger: 'blur' }],
        emissionFactor: [{ required: true, message: '请输入排放因子', trigger: 'blur' }]
      },
      periodOptions: [
        { label: '日', value: 'DAY' },
        { label: '月', value: 'MONTH' },
        { label: '季', value: 'QUARTER' },
        { label: '年', value: 'YEAR' },
        { label: '自定义', value: 'CUSTOM' }
      ],
      statusOptions: [
        { label: '草稿', value: 'DRAFT', type: 'info' },
        { label: '待审核', value: 'SUBMITTED', type: 'warning' },
        { label: '已通过', value: 'APPROVED', type: 'success' },
        { label: '已驳回', value: 'REJECTED', type: 'danger' },
        { label: '已锁定', value: 'LOCKED', type: '' },
        { label: '已作废', value: 'VOIDED', type: 'info' }
      ],
      historyQuery: {
        status: '',
        dateRange: [],
        page: 1,
        size: 10
      },
      historyRecords: [],
      pendingRecords: [],
      historyTotal: 0,
      importRows: [],
      importErrors: [],
      rejectVisible: false,
      rejectComment: '',
      rejectTarget: null,
      auditVisible: false,
      auditLogs: []
    }
  },
  computed: {
    isAdmin() {
      return hasAnyRole(this.$store.getters.roles, ['admin'])
    },
    canEditData() {
      return this.isAdmin || hasAnyPermission(this.$store.getters.permissions, ['carbon:data:edit'])
    },
    canSubmitData() {
      return this.isAdmin || hasAnyPermission(this.$store.getters.permissions, ['carbon:data:submit'])
    },
    canReviewData() {
      return this.isAdmin || hasAnyPermission(this.$store.getters.permissions, ['carbon:data:review'])
    },
    canLockData() {
      return this.isAdmin || hasAnyPermission(this.$store.getters.permissions, ['carbon:data:lock'])
    },
    selectedConfig() {
      return (this.selectedNode && this.selectedNode.config) || {}
    },
    filteredTree() {
      if (!this.rawTree) return []
      const node = this.filterManualTree(this.rawTree)
      return node ? [node] : []
    },
    allEntryNodes() {
      if (!this.rawTree) return []
      return flattenCarbonTree(this.rawTree).filter(item => this.isEntryNode(item))
    },
    entryNodeCount() {
      return this.allEntryNodes.length
    },
    unitOptions() {
      const units = new Set()
      if (this.selectedConfig.measurementUnit) units.add(this.selectedConfig.measurementUnit)
      ;(this.categoryUnits(this.selectedConfig.emissionCategory) || []).forEach(unit => units.add(unit))
      return Array.from(units)
    },
    estimatedEmission() {
      const value = Number(this.form.activityValue || 0)
      const rate = Number(this.form.unitConvertRate || 0)
      const factor = Number(this.form.emissionFactor || 0)
      const ratio = Number(this.form.allocationRatio || 0) / 100
      return value * rate * factor * ratio
    },
    validImportRows() {
      return this.importRows.filter(item => !item._error)
    }
  },
  watch: {
    treeKeyword(value) {
      if (this.$refs.nodeTree) {
        this.$refs.nodeTree.filter(value)
      }
    }
  },
  created() {
    this.loadTemplates()
  },
  methods: {
    operatorId() {
      return this.$store.getters.id || 1
    },
    emptyForm() {
      return {
        id: null,
        nodeId: null,
        periodType: 'MONTH',
        periodStart: today().slice(0, 8) + '01',
        periodEnd: today(),
        activityValue: null,
        activityUnit: '',
        unitConvertRate: 1,
        emissionFactor: null,
        factorUnit: '',
        allocationRatio: 100,
        evidenceUrl: '',
        sourceNo: '',
        remark: '',
        status: 'DRAFT'
      }
    },
    async loadTemplates() {
      this.serviceError = ''
      try {
        this.templates = await templateApi.list({ silent: true })
        if (this.templates.length && !this.query.templateId) {
          this.query.templateId = this.templates[0].id
        }
        await this.loadTree()
      } catch (error) {
        this.serviceError = this.resolveServiceError(error)
      }
    },
    async handleTemplateChange() {
      this.selectedNode = null
      await this.loadTree()
    },
    async loadTree() {
      if (!this.query.templateId) return
      this.loadingTree = true
      this.serviceError = ''
      try {
        this.rawTree = await nodeApi.tree(this.query.templateId, { silent: true })
        this.selectFirstNode()
      } catch (error) {
        this.rawTree = null
        this.selectedNode = null
        this.serviceError = this.resolveServiceError(error)
      } finally {
        this.loadingTree = false
      }
    },
    selectFirstNode() {
      this.$nextTick(() => {
        const candidates = flattenCarbonTree(this.filteredTree[0] || {}).filter(item => this.isEntryNode(item))
        const first = candidates[0]
        if (first) {
          this.selectNode(first)
        }
      })
    },
    handleNodeClick(data) {
      if (!this.isEntryNode(data)) return
      this.selectNode(data)
    },
    selectNode(node) {
      this.selectedNode = node
      this.resetEntryForm()
      this.$nextTick(() => {
        if (this.$refs.nodeTree) {
          this.$refs.nodeTree.setCurrentKey(node.id)
        }
      })
      this.loadHistory()
      this.loadPending()
    },
    resetEntryForm() {
      const form = this.emptyForm()
      form.nodeId = this.selectedNode ? this.selectedNode.id : null
      form.activityUnit = this.selectedConfig.measurementUnit || ''
      form.emissionFactor = this.selectedConfig.carbonEmissionFactor || 0
      form.factorUnit = this.selectedConfig.carbonEmissionFactorDescription || 'tCO2e/unit'
      form.allocationRatio = this.selectedConfig.allocationRatio || 100
      form.periodEnd = this.periodEndByType(form.periodType, form.periodStart)
      this.form = form
      this.$nextTick(() => this.$refs.entryForm && this.$refs.entryForm.clearValidate())
    },
    syncPeriodEnd() {
      if (this.form.periodType !== 'CUSTOM') {
        this.form.periodEnd = this.periodEndByType(this.form.periodType, this.form.periodStart)
      }
    },
    periodEndByType(type, startText) {
      const start = parseDate(startText)
      if (!start) return ''
      const end = new Date(start.getTime())
      if (type === 'MONTH') {
        end.setMonth(start.getMonth() + 1, 0)
      } else if (type === 'QUARTER') {
        const quarterStart = Math.floor(start.getMonth() / 3) * 3
        end.setMonth(quarterStart + 3, 0)
      } else if (type === 'YEAR') {
        end.setMonth(11, 31)
      }
      return formatDate(end)
    },
    buildPayload(status) {
      return Object.assign({}, this.form, {
        nodeId: this.selectedNode.id,
        dataDate: this.form.periodStart,
        actualValue: Number(this.form.activityValue),
        carbonEmission: Number(this.estimatedEmission.toFixed(6)),
        calculatedValue: Number(this.estimatedEmission.toFixed(6)),
        dataSource: 'MANUAL',
        qualityScore: status === 'DRAFT' ? 0.8 : 1,
        status,
        createdBy: this.operatorId(),
        updatedBy: this.operatorId()
      })
    },
    saveEntry(status) {
      this.$refs.entryForm.validate(async valid => {
        if (!valid) return
        if (status === 'SUBMITTED' && !this.form.sourceNo && !this.form.evidenceUrl) {
          this.$message.warning('提交审核前请填写凭证编号或凭证地址')
          return
        }
        this.saving = true
        try {
          const payload = this.buildPayload(status === 'SUBMITTED' ? 'DRAFT' : status)
          let saved
          if (this.form.id) {
            saved = await emissionApi.update(this.form.id, payload)
          } else {
            saved = await emissionApi.create(payload)
          }
          if (status === 'SUBMITTED') {
            await emissionApi.submit(saved.id, { operatorId: this.operatorId(), comment: '提交审核' })
          }
          this.$message.success(status === 'DRAFT' ? '草稿已保存' : '已提交审核')
          this.resetEntryForm()
          this.loadHistory()
          this.loadPending()
        } finally {
          this.saving = false
        }
      })
    },
    async loadHistory() {
      if (!this.selectedNode) return
      this.loadingRecords = true
      try {
        const range = this.historyQuery.dateRange || []
        const page = await emissionApi.query({
          nodeId: this.selectedNode.id,
          startDate: range[0],
          endDate: range[1],
          status: this.historyQuery.status,
          page: this.historyQuery.page - 1,
          size: this.historyQuery.size,
          sortBy: 'dataDate',
          sortDirection: 'DESC'
        }, { silent: true })
        this.historyRecords = page.content || []
        this.historyTotal = page.totalElements || 0
      } catch (error) {
        this.historyRecords = []
        this.historyTotal = 0
        this.serviceError = this.resolveServiceError(error)
      } finally {
        this.loadingRecords = false
      }
    },
    async loadPending() {
      if (!this.selectedNode) return
      const page = await emissionApi.query({
        nodeId: this.selectedNode.id,
        status: 'SUBMITTED',
        page: 0,
        size: 100,
        sortBy: 'createdAt',
        sortDirection: 'ASC'
      }, { silent: true })
      this.pendingRecords = page.content || []
    },
    handleHistoryQuery() {
      this.historyQuery.page = 1
      this.loadHistory()
    },
    resetHistoryQuery() {
      this.historyQuery.status = ''
      this.historyQuery.dateRange = []
      this.historyQuery.page = 1
      this.loadHistory()
    },
    handleTabClick(tab) {
      if (tab.name === 'review') this.loadPending()
      if (tab.name === 'history') this.loadHistory()
    },
    editRecord(row) {
      this.activeTab = 'single'
      this.form = Object.assign(this.emptyForm(), row, {
        activityValue: row.activityValue || row.actualValue,
        remark: row.remark || row.remarks || '',
        periodStart: row.periodStart || row.dataDate,
        periodEnd: row.periodEnd || row.dataDate
      })
      this.$nextTick(() => this.$refs.entryForm && this.$refs.entryForm.clearValidate())
    },
    async submitRecord(row) {
      await this.$confirm('确认提交该记录进入审核？', '系统提示', { type: 'warning' })
      await emissionApi.submit(row.id, { operatorId: this.operatorId(), comment: '提交审核' })
      this.$message.success('已提交审核')
      this.loadHistory()
      this.loadPending()
    },
    async approveRecord(row) {
      await this.$confirm('确认审核通过该记录？', '系统提示', { type: 'warning' })
      await emissionApi.approve(row.id, { operatorId: this.operatorId(), comment: '审核通过' })
      this.$message.success('审核已通过')
      this.loadHistory()
      this.loadPending()
    },
    openReject(row) {
      this.rejectTarget = row
      this.rejectComment = ''
      this.rejectVisible = true
    },
    async rejectRecord() {
      if (!this.rejectComment) {
        this.$message.warning('请输入驳回原因')
        return
      }
      this.saving = true
      try {
        await emissionApi.reject(this.rejectTarget.id, { operatorId: this.operatorId(), comment: this.rejectComment })
        this.$message.success('已驳回')
        this.rejectVisible = false
        this.loadHistory()
        this.loadPending()
      } finally {
        this.saving = false
      }
    },
    async lockRecord(row) {
      await this.$confirm('锁定后该记录不可继续编辑，是否继续？', '系统提示', { type: 'warning' })
      await emissionApi.lock(row.id, { operatorId: this.operatorId(), comment: '锁定归档' })
      this.$message.success('记录已锁定')
      this.loadHistory()
    },
    async voidRecord(row) {
      await this.$confirm('确认作废该记录？作废后不再参与统计。', '系统提示', { type: 'warning' })
      await emissionApi.voidEntry(row.id, { operatorId: this.operatorId(), comment: '人工作废' })
      this.$message.success('记录已作废')
      this.loadHistory()
      this.loadPending()
    },
    async viewAudits(row) {
      this.auditLogs = await emissionApi.audits(row.id)
      this.auditVisible = true
    },
    downloadImportTemplate() {
      const header = ['节点编码/采集点编码', '统计周期类型', '开始日期', '结束日期', '活动数据', '计量单位', '排放因子', '凭证编号/附件名', '备注']
      const sampleCode = this.selectedConfig.equipmentCode || (this.selectedNode && this.selectedNode.id) || ''
      const sample = [sampleCode, 'MONTH', today().slice(0, 8) + '01', today(), '100', this.selectedConfig.measurementUnit || 'kWh', this.selectedConfig.carbonEmissionFactor || '', '台账-001', '']
      this.downloadText([header, sample].map(row => row.join(',')).join('\n'), '人工数据录入导入模板.csv', 'text/csv;charset=utf-8')
    },
    handleImportFile(event) {
      const file = event.target.files[0]
      if (!file) return
      const reader = new FileReader()
      reader.onload = e => {
        this.parseImportText(e.target.result)
        event.target.value = ''
      }
      reader.readAsText(file, 'utf-8')
    },
    parseImportText(text) {
      this.importRows = []
      this.importErrors = []
      const lines = String(text || '').replace(/^\uFEFF/, '').split(/\r?\n/).filter(line => line.trim())
      if (lines.length < 2) {
        this.importErrors = ['导入文件没有数据行']
        return
      }
      const headers = this.parseCsvLine(lines[0])
      this.importRows = lines.slice(1).map((line, index) => {
        const values = this.parseCsvLine(line)
        const row = {}
        headers.forEach((header, i) => {
          row[header] = values[i]
        })
        return this.mapImportRow(row, index + 2)
      })
      this.importErrors = this.importRows.filter(row => row._error).map(row => `第${row._row}行：${row._error}`)
    },
    parseCsvLine(line) {
      const values = []
      let current = ''
      let quoted = false
      for (let i = 0; i < line.length; i++) {
        const char = line[i]
        if (char === '"' && line[i + 1] === '"') {
          current += '"'
          i++
        } else if (char === '"') {
          quoted = !quoted
        } else if (char === ',' && !quoted) {
          values.push(current.trim())
          current = ''
        } else {
          current += char
        }
      }
      values.push(current.trim())
      return values
    },
    mapImportRow(row, rowNumber) {
      const node = this.resolveImportNode(row['节点编码/采集点编码'])
      const periodType = (row['统计周期类型'] || 'MONTH').toUpperCase()
      const activityValue = Number(row['活动数据'])
      const mapped = {
        _row: rowNumber,
        nodeId: node && node.id,
        nodeName: node && node.name,
        periodType,
        periodStart: row['开始日期'],
        periodEnd: row['结束日期'] || this.periodEndByType(periodType, row['开始日期']),
        activityValue,
        activityUnit: row['计量单位'] || (node && node.config && node.config.measurementUnit),
        unitConvertRate: 1,
        emissionFactor: row['排放因子'] ? Number(row['排放因子']) : (node && node.config && node.config.carbonEmissionFactor),
        factorUnit: node && node.config && node.config.carbonEmissionFactorDescription,
        allocationRatio: (node && node.config && node.config.allocationRatio) || 100,
        sourceNo: row['凭证编号/附件名'],
        remark: row['备注'],
        dataSource: 'MANUAL',
        status: 'DRAFT',
        createdBy: this.operatorId(),
        updatedBy: this.operatorId()
      }
      mapped.carbonEmission = Number(((mapped.activityValue || 0) * (mapped.emissionFactor || 0) * ((mapped.allocationRatio || 100) / 100)).toFixed(6))
      mapped.calculatedValue = mapped.carbonEmission
      mapped.actualValue = mapped.activityValue
      mapped.dataDate = mapped.periodStart
      mapped._error = this.importRowError(mapped)
      return mapped
    },
    resolveImportNode(code) {
      if (!code && this.selectedNode) return this.selectedNode
      return this.allEntryNodes.find(node => {
        const config = node.config || {}
        const info = node.nodeInfo || {}
        return String(node.id) === String(code) || config.equipmentCode === code || info.nodeCode === code
      })
    },
    importRowError(row) {
      if (!row.nodeId) return '未匹配到节点'
      if (!row.periodStart || !row.periodEnd) return '统计周期不完整'
      if (Number.isNaN(row.activityValue) || row.activityValue < 0) return '活动数据不合法'
      if (!row.activityUnit) return '计量单位不能为空'
      if (row.emissionFactor === undefined || row.emissionFactor === null || Number.isNaN(Number(row.emissionFactor))) return '排放因子不合法'
      return ''
    },
    async importBatch() {
      this.importing = true
      try {
        const result = await emissionApi.batchCreate(this.validImportRows)
        this.$message.success(`导入完成：成功 ${result.success || 0} 条，失败 ${result.failed || 0} 条`)
        this.importErrors = result.errors || []
        this.importRows = []
        this.loadHistory()
      } finally {
        this.importing = false
      }
    },
    filterManualTree(node) {
      if (!node) return null
      const children = (node.children || []).map(child => this.filterManualTree(child)).filter(Boolean)
      const entry = this.isEntryNode(node)
      if (entry && this.manualOnly && !this.isManualNode(node)) {
        return null
      }
      if (!entry && this.manualOnly && !children.length) {
        return null
      }
      return Object.assign({}, node, { children })
    },
    filterNode(value, data) {
      if (!value) return true
      return String(data.name || '').indexOf(value) !== -1
    },
    isEntryNode(node) {
      return node && (node.typeId === 3 || node.typeId === 4)
    },
    isManualNode(node) {
      const source = node && node.config && node.config.dataSource
      return source === 'MANUAL' || source === '手工录入'
    },
    nodeIcon(node) {
      if (!this.isEntryNode(node)) return 'el-icon-folder'
      return this.isManualNode(node) ? 'el-icon-edit-outline' : 'el-icon-connection'
    },
    categoryUnits(category) {
      const map = {
        '购入的电力': ['kWh', 'MWh', 'GWh'],
        '购入的热力': ['GJ', 'MJ', '吨'],
        '化石燃料': ['t', 'kg', 'L', 'Nm3', '104 Nm3'],
        '废弃物处理': ['t', 'm3']
      }
      return map[category] || ['kWh', 'MWh', 'GJ', 't', 'kg', 'm3']
    },
    canEdit(row) {
      return row.status === 'DRAFT' || row.status === 'REJECTED'
    },
    canSubmit(row) {
      return row.status === 'DRAFT' || row.status === 'REJECTED'
    },
    canVoid(row) {
      return row.status !== 'VOIDED' && row.status !== 'LOCKED'
    },
    statusLabel(value) {
      if (!value) return '-'
      const item = this.statusOptions.find(option => option.value === value)
      return item ? item.label : value
    },
    statusType(value) {
      const item = this.statusOptions.find(option => option.value === value)
      return item ? item.type : 'info'
    },
    periodLabel(value) {
      const item = this.periodOptions.find(option => option.value === value)
      return item ? item.label : value
    },
    periodText(row) {
      return `${this.periodLabel(row.periodType)} ${row.periodStart || row.dataDate || '-'} 至 ${row.periodEnd || row.dataDate || '-'}`
    },
    auditActionLabel(action) {
      const map = {
        CREATE: '创建',
        UPDATE: '修改',
        SUBMIT: '提交',
        APPROVE: '审核通过',
        REJECT: '驳回',
        LOCK: '锁定',
        VOID: '作废'
      }
      return map[action] || action
    },
    formatNumber(value) {
      if (value === undefined || value === null || value === '') return '-'
      return Number(value).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 6 })
    },
    formatRatio(value) {
      if (value === undefined || value === null || value === '') return '-'
      return `${Number(value).toFixed(2)}%`
    },
    downloadText(content, filename, type) {
      const blob = new Blob(['\uFEFF' + content], { type })
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = filename
      link.click()
      URL.revokeObjectURL(url)
    },
    resolveServiceError(error) {
      const response = error && error.response
      const data = response && response.data
      if (typeof data === 'string' && data.indexOf('Proxy error') !== -1) {
        return '碳排放服务未启动或代理配置异常，请确认后端服务已启动后再刷新页面。'
      }
      return (data && (data.message || data.error)) || '碳排放数据暂时无法加载，请稍后重试。'
    }
  }
}
</script>

<style scoped lang="scss">
.carbon-data-entry {
  min-height: calc(100vh - 84px);
  background: #f6f8fb;
}

.carbon-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;

  h2 {
    margin: 0 0 6px;
    color: #1f2d3d;
    font-size: 22px;
  }

  span {
    color: #7a8797;
  }
}

.toolbar-actions {
  display: flex;
  gap: 8px;
}

.service-alert {
  margin-bottom: 16px;
}

.entry-workbench {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}

.node-panel,
.entry-main,
.caliber-card,
.entry-section {
  border: 1px solid #e5eaf1;
  border-radius: 6px;
  background: #fff;
}

.node-panel {
  min-height: 680px;
  padding: 16px;
}

.panel-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  color: #1f2d3d;
  font-weight: 700;

  small {
    color: #8b97a8;
    font-weight: 400;
  }
}

.template-select,
.tree-search,
.manual-filter {
  width: 100%;
  margin-bottom: 12px;
}

.node-tree {
  margin-top: 6px;

  ::v-deep .el-tree-node__content {
    height: 36px;
    border-radius: 5px;
  }
}

.tree-node {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  width: 100%;

  i {
    flex: 0 0 auto;
    color: #2265b4;
  }

  span {
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.entry-main {
  min-width: 0;
  padding: 16px;
}

.caliber-card {
  margin-bottom: 16px;
  padding: 18px;
}

.caliber-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;

  h3 {
    margin: 5px 0 0;
    color: #1f2d3d;
    font-size: 20px;
  }
}

.eyebrow {
  color: #6b7788;
  font-size: 12px;
  font-weight: 700;
}

.caliber-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;

  > div {
    min-width: 0;
    padding: 12px;
    border-radius: 6px;
    background: #f8fafc;
  }

  span {
    display: block;
    margin-bottom: 6px;
    color: #7a8797;
    font-size: 12px;
  }

  strong {
    display: block;
    color: #1f2d3d;
    overflow-wrap: anywhere;
  }
}

.entry-tabs {
  ::v-deep .el-tabs__header {
    margin-bottom: 12px;
  }
}

.entry-section {
  padding: 16px;
}

.form-actions,
.batch-toolbar {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 8px;
}

.batch-toolbar {
  justify-content: flex-start;
  margin-bottom: 12px;

  span {
    color: #7a8797;
  }
}

.file-input {
  display: none;
}

.import-alert {
  margin-bottom: 12px;
}

.history-filter {
  margin-bottom: 8px;
}

.table-pagination {
  margin-top: 16px;
  text-align: right;
}

.audit-item {
  strong,
  span {
    display: block;
  }

  span {
    margin-top: 4px;
    color: #6b7788;
  }

  p {
    margin: 8px 0 0;
    color: #1f2d3d;
    line-height: 1.6;
  }
}

@media (max-width: 1100px) {
  .entry-workbench {
    grid-template-columns: 280px minmax(0, 1fr);
  }

  .caliber-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .carbon-toolbar,
  .caliber-head,
  .toolbar-actions,
  .form-actions,
  .batch-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .entry-workbench,
  .caliber-grid {
    grid-template-columns: 1fr;
  }
}
</style>
