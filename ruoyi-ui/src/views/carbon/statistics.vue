<template>
  <div class="app-container carbon-statistics">
    <div class="carbon-toolbar">
      <div>
        <h2>统计查询</h2>
        <span>按模板、节点、日期和数据来源汇总排放数据</span>
      </div>
      <el-button v-if="canExport" icon="el-icon-download" @click="exportCsv">导出结果</el-button>
    </div>

    <section class="carbon-panel">
      <el-form :model="query" size="small" :inline="true" label-width="82px">
        <el-form-item label="模板">
          <el-select v-model="query.templateId" filterable clearable style="width: 220px" @change="handleTemplateChange">
            <el-option v-for="item in templates" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="节点">
          <el-select v-model="query.nodeId" filterable clearable style="width: 240px">
            <el-option v-for="item in nodeOptions" :key="item.id" :label="nodeLabel(item)" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期">
          <el-date-picker
            v-model="query.dateRange"
            type="daterange"
            value-format="yyyy-MM-dd"
            range-separator="-"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 260px"
          />
        </el-form-item>
        <el-form-item label="来源">
          <el-select v-model="query.dataSource" clearable style="width: 140px">
            <el-option v-for="item in sourceOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="handleQuery">查询</el-button>
          <el-button icon="el-icon-refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </section>

    <div class="summary-grid">
      <div class="summary-item">
        <span>记录数</span>
        <strong>{{ total }}</strong>
      </div>
      <div class="summary-item">
        <span>实际排放量</span>
        <strong>{{ formatNumber(summary.actualTotal) }}</strong>
      </div>
      <div class="summary-item">
        <span>计算排放量</span>
        <strong>{{ formatNumber(summary.calculatedTotal) }}</strong>
      </div>
      <div class="summary-item">
        <span>平均质量</span>
        <strong>{{ formatPercent(summary.avgQuality) }}</strong>
      </div>
    </div>

    <el-row :gutter="16">
      <el-col :xs="24" :lg="14">
        <section class="carbon-panel">
          <div class="panel-title">月度排放趋势</div>
          <div ref="trendChart" class="chart"></div>
        </section>
      </el-col>
      <el-col :xs="24" :lg="10">
        <section class="carbon-panel">
          <div class="panel-title">来源占比</div>
          <div ref="sourceChart" class="chart"></div>
        </section>
      </el-col>
    </el-row>

    <section class="carbon-panel">
      <el-table v-loading="loading" :data="records" border>
        <el-table-column label="节点" prop="nodeName" min-width="160" show-overflow-tooltip />
        <el-table-column label="日期" prop="dataDate" width="120" align="center" />
        <el-table-column label="实际排放量" prop="actualValue" width="140" align="right">
          <template slot-scope="scope">{{ formatNumber(scope.row.actualValue) }}</template>
        </el-table-column>
        <el-table-column label="计算排放量" prop="calculatedValue" width="140" align="right">
          <template slot-scope="scope">{{ formatNumber(scope.row.calculatedValue) }}</template>
        </el-table-column>
        <el-table-column label="数据来源" prop="dataSource" width="110" align="center">
          <template slot-scope="scope">{{ sourceLabel(scope.row.dataSource) }}</template>
        </el-table-column>
        <el-table-column label="质量评分" prop="qualityScore" width="110" align="center">
          <template slot-scope="scope">{{ formatPercent(scope.row.qualityScore) }}</template>
        </el-table-column>
        <el-table-column label="备注" prop="remarks" min-width="160" show-overflow-tooltip />
      </el-table>

      <el-pagination
        class="table-pagination"
        background
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        :page-size.sync="query.size"
        :current-page.sync="query.page"
        :page-sizes="[10, 20, 50, 100]"
        @size-change="loadRecords"
        @current-change="loadRecords"
      />
    </section>
  </div>
</template>

<script>
import * as echarts from 'echarts'
import { templateApi, nodeApi, emissionApi, flattenCarbonTree } from '@/api/carbon'
import { hasAnyPermission, hasAnyRole } from '@/utils/permissionMatch'

export default {
  name: 'CarbonStatistics',
  computed: {
    canExport() {
      return hasAnyRole(this.$store.getters.roles, ['admin'])
        || hasAnyPermission(this.$store.getters.permissions, ['carbon:statistics:export'])
    }
  },
  data() {
    return {
      loading: false,
      templates: [],
      nodeOptions: [],
      records: [],
      total: 0,
      summary: {
        actualTotal: 0,
        calculatedTotal: 0,
        avgQuality: null
      },
      query: {
        templateId: null,
        nodeId: null,
        dateRange: [],
        dataSource: '',
        page: 1,
        size: 20
      },
      sourceOptions: [
        { label: '手工录入', value: 'MANUAL' },
        { label: '数据库', value: 'DB' },
        { label: 'API输入', value: 'API' }
      ],
      trendChart: null,
      sourceChart: null
    }
  },
  created() {
    this.loadTemplates()
  },
  mounted() {
    window.addEventListener('resize', this.resizeCharts)
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.resizeCharts)
    if (this.trendChart) this.trendChart.dispose()
    if (this.sourceChart) this.sourceChart.dispose()
  },
  methods: {
    async loadTemplates() {
      this.templates = await templateApi.list()
      if (this.templates.length) {
        this.query.templateId = this.templates[0].id
        await this.loadNodes()
      }
      this.loadRecords()
    },
    async handleTemplateChange() {
      this.query.nodeId = null
      await this.loadNodes()
      this.handleQuery()
    },
    async loadNodes() {
      this.nodeOptions = []
      if (!this.query.templateId) return
      const tree = await nodeApi.tree(this.query.templateId)
      this.nodeOptions = flattenCarbonTree(tree).filter(item => item.typeId === 3 || item.typeId === 4)
      if (!this.query.nodeId && this.nodeOptions.length) {
        this.query.nodeId = this.nodeOptions[0].id
      }
    },
    queryParams(size) {
      const range = this.query.dateRange || []
      return {
        nodeId: this.query.nodeId,
        startDate: range[0],
        endDate: range[1],
        dataSource: this.query.dataSource,
        page: this.query.page - 1,
        size: size || this.query.size,
        sortBy: 'dataDate',
        sortDirection: 'DESC'
      }
    },
    async loadRecords() {
      this.loading = true
      try {
        const page = await emissionApi.query(this.queryParams())
        this.records = page.content || []
        this.total = page.totalElements || 0
        await this.loadSummary()
      } finally {
        this.loading = false
      }
    },
    async loadSummary() {
      let allRecords = this.records
      if (this.total > this.records.length) {
        const page = await emissionApi.query(this.queryParams(Math.min(this.total, 2000)))
        allRecords = page.content || this.records
      }
      const qualityValues = allRecords
        .map(item => Number(item.qualityScore))
        .filter(value => !Number.isNaN(value))
      this.summary = {
        actualTotal: allRecords.reduce((sum, item) => sum + Number(item.actualValue || 0), 0),
        calculatedTotal: allRecords.reduce((sum, item) => sum + Number(item.calculatedValue || 0), 0),
        avgQuality: qualityValues.length ? qualityValues.reduce((sum, value) => sum + value, 0) / qualityValues.length : null
      }
      this.renderCharts(allRecords)
    },
    handleQuery() {
      this.query.page = 1
      this.loadRecords()
    },
    resetQuery() {
      this.query.dateRange = []
      this.query.dataSource = ''
      this.query.page = 1
      this.loadRecords()
    },
    renderCharts(records) {
      const trend = this.monthlyTrend(records)
      const source = this.sourceSummary(records)
      this.$nextTick(() => {
        if (!this.trendChart) this.trendChart = echarts.init(this.$refs.trendChart)
        if (!this.sourceChart) this.sourceChart = echarts.init(this.$refs.sourceChart)
        this.trendChart.setOption({
          color: ['#1f8f6b', '#2265b4'],
          tooltip: { trigger: 'axis' },
          legend: { data: ['实际排放量', '计算排放量'] },
          grid: { left: 36, right: 16, top: 48, bottom: 28 },
          xAxis: { type: 'category', data: trend.months },
          yAxis: { type: 'value' },
          series: [
            { name: '实际排放量', type: 'line', smooth: true, data: trend.actual },
            { name: '计算排放量', type: 'bar', data: trend.calculated }
          ]
        })
        this.sourceChart.setOption({
          color: ['#1f8f6b', '#2265b4', '#b4691f', '#b84d4d'],
          tooltip: { trigger: 'item' },
          series: [{
            type: 'pie',
            radius: ['48%', '72%'],
            center: ['50%', '52%'],
            label: { formatter: '{b}\n{d}%' },
            data: source
          }]
        })
      })
    },
    monthlyTrend(records) {
      const map = {}
      records.forEach(item => {
        const month = String(item.dataDate || '').slice(0, 7) || '未填写'
        if (!map[month]) map[month] = { actual: 0, calculated: 0 }
        map[month].actual += Number(item.actualValue || 0)
        map[month].calculated += Number(item.calculatedValue || 0)
      })
      const months = Object.keys(map).sort()
      return {
        months,
        actual: months.map(month => Number(map[month].actual.toFixed(2))),
        calculated: months.map(month => Number(map[month].calculated.toFixed(2)))
      }
    },
    sourceSummary(records) {
      const map = {}
      records.forEach(item => {
        const source = this.sourceLabel(item.dataSource || '未知')
        map[source] = (map[source] || 0) + Number(item.actualValue || 0)
      })
      return Object.keys(map).map(key => ({ name: key, value: Number(map[key].toFixed(2)) }))
    },
    resizeCharts() {
      if (this.trendChart) this.trendChart.resize()
      if (this.sourceChart) this.sourceChart.resize()
    },
    exportCsv() {
      const header = ['节点', '日期', '实际排放量', '计算排放量', '数据来源', '质量评分', '备注']
      const rows = this.records.map(item => [
        item.nodeName || '',
        item.dataDate || '',
        item.actualValue || '',
        item.calculatedValue || '',
        this.sourceLabel(item.dataSource),
        item.qualityScore || '',
        item.remarks || ''
      ])
      const csv = [header].concat(rows).map(row => row.map(value => `"${String(value).replace(/"/g, '""')}"`).join(',')).join('\n')
      this.downloadText(csv, '碳排放统计查询.csv', 'text/csv;charset=utf-8')
    },
    downloadText(content, filename, type) {
      const blob = new Blob([content], { type })
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = filename
      link.click()
      URL.revokeObjectURL(url)
    },
    sourceLabel(value) {
      const item = this.sourceOptions.find(source => source.value === value)
      return item ? item.label : value
    },
    nodeLabel(item) {
      return `${'　'.repeat(item.level || 0)}${item.name}`
    },
    formatNumber(value) {
      if (value === undefined || value === null || value === '') return '-'
      return Number(value).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 6 })
    },
    formatPercent(value) {
      if (value === undefined || value === null || value === '') return '-'
      return `${(Number(value) * 100).toFixed(0)}%`
    }
  }
}
</script>

<style scoped lang="scss">
.carbon-statistics {
  min-height: calc(100vh - 84px);
  background: #f6f8fb;
}

.carbon-toolbar,
.panel-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.carbon-toolbar {
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

.carbon-panel {
  margin-bottom: 16px;
  padding: 16px;
  border: 1px solid #e5eaf1;
  border-radius: 6px;
  background: #fff;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 16px;
}

.summary-item {
  padding: 18px;
  border: 1px solid #e5eaf1;
  border-radius: 6px;
  background: #fff;

  span {
    display: block;
    color: #7a8797;
    font-size: 13px;
  }

  strong {
    display: block;
    margin-top: 10px;
    color: #1f2d3d;
    font-size: 24px;
  }
}

.panel-title {
  margin-bottom: 12px;
  color: #1f2d3d;
  font-weight: 700;
}

.chart {
  width: 100%;
  height: 330px;
}

.table-pagination {
  margin-top: 16px;
  text-align: right;
}

@media (max-width: 1000px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .carbon-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
