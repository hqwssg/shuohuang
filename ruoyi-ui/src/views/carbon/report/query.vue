<template>
  <div class="app-container carbon-report-page">
    <div class="carbon-toolbar no-print">
      <div>
        <h2>核算结果查询</h2>
        <span>按模板、节点和日期查询核算区间结果</span>
      </div>
      <div class="toolbar-actions">
        <el-button type="primary" icon="el-icon-document" :loading="loading" @click="generateReport">生成报告</el-button>
        <el-button icon="el-icon-printer" :disabled="!report" @click="printReport">打印</el-button>
        <el-button icon="el-icon-download" :disabled="!report" @click="exportReport">导出</el-button>
      </div>
    </div>

    <section class="carbon-panel no-print">
      <el-form :model="query" size="small" :inline="true" label-width="82px">
        <el-form-item label="模板">
          <el-select v-model="query.templateId" filterable style="width: 220px" @change="handleTemplateChange">
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
      </el-form>
    </section>

    <section v-if="report" ref="report" class="report-sheet">
      <header class="report-header">
        <div>
          <span>Carbon Emission Report</span>
          <h1>碳排放核算报告</h1>
        </div>
        <div class="report-meta">
          <p>生成时间：{{ report.generatedAt }}</p>
          <p>统计区间：{{ report.startDate }} 至 {{ report.endDate }}</p>
        </div>
      </header>

      <div class="report-info">
        <div>
          <span>模板</span>
          <strong>{{ report.templateName }}</strong>
        </div>
        <div>
          <span>节点</span>
          <strong>{{ report.nodeName }}</strong>
        </div>
        <div>
          <span>记录数</span>
          <strong>{{ report.total }}</strong>
        </div>
      </div>

      <div class="report-summary">
        <div>
          <span>实际排放总量</span>
          <strong>{{ formatNumber(report.actualTotal) }}</strong>
          <small>tCO2e</small>
        </div>
        <div>
          <span>计算排放总量</span>
          <strong>{{ formatNumber(report.calculatedTotal) }}</strong>
          <small>tCO2e</small>
        </div>
        <div>
          <span>平均质量评分</span>
          <strong>{{ formatPercent(report.avgQuality) }}</strong>
          <small>quality</small>
        </div>
      </div>

      <h3>月度汇总</h3>
      <el-table :data="report.monthly" border>
        <el-table-column label="月份" prop="month" width="140" />
        <el-table-column label="实际排放量" prop="actual" align="right">
          <template slot-scope="scope">{{ formatNumber(scope.row.actual) }}</template>
        </el-table-column>
        <el-table-column label="计算排放量" prop="calculated" align="right">
          <template slot-scope="scope">{{ formatNumber(scope.row.calculated) }}</template>
        </el-table-column>
        <el-table-column label="平均质量" prop="quality" align="right">
          <template slot-scope="scope">{{ formatPercent(scope.row.quality) }}</template>
        </el-table-column>
      </el-table>

      <h3>明细数据</h3>
      <el-table :data="report.records" border>
        <el-table-column label="节点" prop="nodeName" min-width="160" show-overflow-tooltip />
        <el-table-column label="日期" prop="dataDate" width="120" />
        <el-table-column label="实际排放量" prop="actualValue" align="right">
          <template slot-scope="scope">{{ formatNumber(scope.row.actualValue) }}</template>
        </el-table-column>
        <el-table-column label="计算排放量" prop="calculatedValue" align="right">
          <template slot-scope="scope">{{ formatNumber(scope.row.calculatedValue) }}</template>
        </el-table-column>
        <el-table-column label="质量评分" prop="qualityScore" align="right">
          <template slot-scope="scope">{{ formatPercent(scope.row.qualityScore) }}</template>
        </el-table-column>
      </el-table>

      <h3>结论</h3>
      <p class="report-conclusion">{{ report.conclusion }}</p>
    </section>

    <el-empty v-else class="empty-report" description="请选择条件后生成报告" />
  </div>
</template>

<script>
import { templateApi, nodeApi, emissionApi, flattenCarbonTree } from '@/api/carbon'

function defaultRange() {
  const end = new Date()
  const start = new Date()
  start.setMonth(start.getMonth() - 1)
  const format = date => `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
  return [format(start), format(end)]
}

export default {
  name: 'CarbonReport',
  data() {
    return {
      loading: false,
      templates: [],
      nodeOptions: [],
      query: {
        templateId: null,
        nodeId: null,
        dateRange: defaultRange()
      },
      report: null
    }
  },
  created() {
    this.loadTemplates()
  },
  methods: {
    async loadTemplates() {
      this.templates = await templateApi.list()
      if (this.templates.length) {
        this.query.templateId = this.templates[0].id
        await this.loadNodes()
      }
    },
    async handleTemplateChange() {
      this.query.nodeId = null
      this.report = null
      await this.loadNodes()
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
    async generateReport() {
      const range = this.query.dateRange || []
      if (!this.query.nodeId || !range[0] || !range[1]) {
        this.$message.warning('请选择节点和统计日期')
        return
      }
      this.loading = true
      try {
        const page = await emissionApi.query({
          nodeId: this.query.nodeId,
          startDate: range[0],
          endDate: range[1],
          page: 0,
          size: 2000,
          sortBy: 'dataDate',
          sortDirection: 'ASC'
        })
        const records = page.content || []
        const monthly = this.monthlySummary(records)
        const qualityValues = records
          .map(item => Number(item.qualityScore))
          .filter(value => !Number.isNaN(value))
        const actualTotal = records.reduce((sum, item) => sum + Number(item.actualValue || 0), 0)
        const calculatedTotal = records.reduce((sum, item) => sum + Number(item.calculatedValue || 0), 0)
        const avgQuality = qualityValues.length ? qualityValues.reduce((sum, value) => sum + value, 0) / qualityValues.length : null
        const template = this.templates.find(item => item.id === this.query.templateId)
        const node = this.nodeOptions.find(item => item.id === this.query.nodeId)
        this.report = {
          generatedAt: this.formatDateTime(new Date()),
          startDate: range[0],
          endDate: range[1],
          templateName: template ? template.name : '-',
          nodeName: node ? node.name : '-',
          total: page.totalElements || records.length,
          actualTotal,
          calculatedTotal,
          avgQuality,
          monthly,
          records,
          conclusion: this.buildConclusion(records.length, actualTotal, avgQuality)
        }
      } finally {
        this.loading = false
      }
    },
    monthlySummary(records) {
      const map = {}
      records.forEach(item => {
        const month = String(item.dataDate || '').slice(0, 7) || '未填写'
        if (!map[month]) map[month] = { actual: 0, calculated: 0, qualities: [] }
        map[month].actual += Number(item.actualValue || 0)
        map[month].calculated += Number(item.calculatedValue || 0)
        if (item.qualityScore !== null && item.qualityScore !== undefined) {
          map[month].qualities.push(Number(item.qualityScore))
        }
      })
      return Object.keys(map).sort().map(month => {
        const item = map[month]
        return {
          month,
          actual: item.actual,
          calculated: item.calculated,
          quality: item.qualities.length ? item.qualities.reduce((sum, value) => sum + value, 0) / item.qualities.length : null
        }
      })
    },
    buildConclusion(count, actualTotal, avgQuality) {
      if (!count) {
        return '当前统计区间暂无排放数据，建议补充人工录入或检查采集任务。'
      }
      const qualityText = avgQuality === null ? '暂无质量评分' : `平均质量评分为${this.formatPercent(avgQuality)}`
      return `当前区间共纳入${count}条排放数据，实际排放总量为${this.formatNumber(actualTotal)} tCO2e，${qualityText}。`
    },
    printReport() {
      window.print()
    },
    exportReport() {
      if (!this.report) return
      const lines = [
        '碳排放核算报告',
        `生成时间：${this.report.generatedAt}`,
        `模板：${this.report.templateName}`,
        `节点：${this.report.nodeName}`,
        `统计区间：${this.report.startDate} 至 ${this.report.endDate}`,
        `记录数：${this.report.total}`,
        `实际排放总量：${this.formatNumber(this.report.actualTotal)} tCO2e`,
        `计算排放总量：${this.formatNumber(this.report.calculatedTotal)} tCO2e`,
        `平均质量评分：${this.formatPercent(this.report.avgQuality)}`,
        '',
        '月度汇总：',
        ...this.report.monthly.map(item => `${item.month}，实际=${this.formatNumber(item.actual)}，计算=${this.formatNumber(item.calculated)}，质量=${this.formatPercent(item.quality)}`),
        '',
        '结论：',
        this.report.conclusion
      ]
      this.downloadText(lines.join('\n'), '碳排放核算报告.txt')
    },
    downloadText(content, filename) {
      const blob = new Blob([content], { type: 'text/plain;charset=utf-8' })
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = filename
      link.click()
      URL.revokeObjectURL(url)
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
    },
    formatDateTime(date) {
      const pad = value => String(value).padStart(2, '0')
      return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
    }
  }
}
</script>

<style scoped lang="scss">
.carbon-report-page {
  min-height: calc(100vh - 84px);
  background: #f6f8fb;
}

.carbon-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
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

.carbon-panel,
.report-sheet {
  border: 1px solid #e5eaf1;
  border-radius: 6px;
  background: #fff;
}

.carbon-panel {
  margin-bottom: 16px;
  padding: 16px;
}

.report-sheet {
  max-width: 1120px;
  margin: 0 auto 24px;
  padding: 34px;
  color: #1f2d3d;
}

.report-header {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  padding-bottom: 20px;
  border-bottom: 2px solid #1f8f6b;

  span {
    color: #1f8f6b;
    font-size: 12px;
    font-weight: 700;
    text-transform: uppercase;
  }

  h1 {
    margin: 8px 0 0;
    font-size: 30px;
    letter-spacing: 0;
  }
}

.report-meta {
  color: #5f6f82;
  text-align: right;

  p {
    margin: 5px 0;
  }
}

.report-info,
.report-summary {
  display: grid;
  gap: 12px;
  margin-top: 18px;
}

.report-info {
  grid-template-columns: 2fr 2fr 1fr;
}

.report-summary {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.report-info > div,
.report-summary > div {
  padding: 16px;
  border: 1px solid #e5eaf1;
  border-radius: 6px;
  background: #fbfcfe;

  span,
  small {
    display: block;
    color: #7a8797;
  }

  strong {
    display: block;
    margin-top: 8px;
    font-size: 20px;
  }
}

.report-summary strong {
  font-size: 24px;
}

h3 {
  margin: 28px 0 12px;
  font-size: 18px;
}

.report-conclusion {
  margin: 0;
  padding: 16px;
  border-left: 4px solid #1f8f6b;
  background: #f2f8f5;
  line-height: 1.8;
}

.empty-report {
  margin-top: 40px;
}

@media (max-width: 900px) {
  .carbon-toolbar,
  .report-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .report-info,
  .report-summary {
    grid-template-columns: 1fr;
  }

  .report-meta {
    text-align: left;
  }
}

@media print {
  .no-print,
  .sidebar-container,
  .navbar,
  .tags-view-container {
    display: none !important;
  }

  .carbon-report-page {
    background: #fff;
  }

  .report-sheet {
    max-width: none;
    margin: 0;
    padding: 0;
    border: 0;
  }
}
</style>
