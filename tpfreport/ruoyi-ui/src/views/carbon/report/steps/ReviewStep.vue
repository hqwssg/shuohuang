<template>
  <div class="review-step">
    <h4>上年对比</h4>
    <el-alert
      title="有上年核算或上年报告快照时会自动带入。本地没有上年数据时可手填；不填也可以生成，第6章同比会空。"
      type="info"
      :closable="false"
      show-icon
      style="margin-bottom:8px"
    />
    <el-form label-width="160px" size="small" class="prior-form">
      <el-form-item label="上年总量 tCO2">
        <el-input v-model="prior.total_emissions_tco2" />
      </el-form-item>
      <el-form-item label="上年燃料 tCO2">
        <el-input v-model="prior.fuel_combustion_tco2" />
      </el-form-item>
      <el-form-item label="上年电力 tCO2">
        <el-input v-model="prior.electricity_emissions_tco2" />
      </el-form-item>
      <el-form-item label="上年热力 tCO2">
        <el-input v-model="prior.heat_emissions_tco2" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" size="small" @click="savePrior">保存上年对比</el-button>
      </el-form-item>
    </el-form>
    <el-button type="primary" :disabled="busy" @click="validate">开始校验</el-button>
    <el-switch v-model="aiEnabled" :disabled="busy" active-text="启用 AI" style="margin-left:16px" />
    <div style="margin-top:16px">
      <el-button :disabled="busy" @click="generate('docx')">生成 Word</el-button>
      <el-button :disabled="busy" @click="generate('pdf')">生成 PDF</el-button>
    </div>
    <el-alert
      v-if="job && job.status === 'succeeded' && !busy"
      title="已有生成产物，可直接下载。修改填报内容后需重新生成才会更新文件。"
      type="success"
      :closable="false"
      show-icon
      style="margin-top:12px"
    />
    <el-table v-if="issues.length" :data="issues" size="small" style="margin-top:16px">
      <el-table-column prop="severity" label="级别" width="90" />
      <el-table-column prop="message" label="问题" />
    </el-table>
    <div v-if="job && busy" class="gen-progress">
      <el-progress :percentage="progress" :status="progressStatus" :stroke-width="16" />
      <p>{{ stageText }}</p>
    </div>
    <div v-if="job && job.status === 'failed'" class="gen-progress">
      <p>生成失败</p>
    </div>
    <el-button v-if="job && job.docxFileId" type="text" @click="download(job.docxFileId, '报告.docx')">下载 Word</el-button>
    <el-button v-if="job && job.pdfFileId" type="text" @click="download(job.pdfFileId, '报告.pdf')">下载 PDF</el-button>
  </div>
</template>
<script>
import { saveAs } from 'file-saver'
import { validateTask, getFactorPlan, createGenerationJob, getLatestGenerationJob, getGenerationJob, saveSection, downloadReportArtifact } from '@/api/carbon/report'
export default {
  name: 'ReviewStep',
  props: { draft: { type: Object, required: true } },
  data() {
    return {
      aiEnabled: false,
      job: null,
      issues: [],
      timer: null,
      prior: {
        report_year: null,
        total_emissions_tco2: '',
        fuel_combustion_tco2: '',
        electricity_emissions_tco2: '',
        heat_emissions_tco2: ''
      }
    }
  },
  computed: {
    busy() {
      return this.job && ['queued', 'processing'].includes(this.job.status)
    },
    progress() {
      return Math.min(100, Math.max(0, Number(this.job && this.job.progress) || 0))
    },
    progressStatus() {
      if (!this.job) return undefined
      if (this.job.status === 'succeeded') return 'success'
      if (this.job.status === 'failed') return 'exception'
      return undefined
    },
    stageText() {
      if (!this.job) return ''
      if (this.job.status === 'failed') return '生成失败'
      if (this.job.status === 'succeeded') return '生成完成'
      const p = this.progress
      if (this.job.status === 'queued' || p < 10) return '排队中'
      if (p < 40) return '正在准备数据'
      if (p < 70) return this.aiEnabled ? '正在调用 AI 撰写章节' : '正在生成 Word'
      if (p < 90) return '正在排版 Word'
      if (p < 100) return '正在转换 PDF'
      return '生成完成'
    }
  },
  created() {
    this.hydratePrior()
    this.restoreLatest()
  },
  beforeDestroy() {
    if (this.timer) clearInterval(this.timer)
  },
  methods: {
    parse(json) {
      if (!json) return {}
      if (typeof json === 'object') return json
      try { return JSON.parse(json) } catch (e) { return {} }
    },
    restoreLatest() {
      const local = this.draft && this.draft.latestJob
      if (local && (local.docxFileId || local.pdfFileId)) {
        this.job = local
      }
      getLatestGenerationJob(this.draft.task.id).then(res => {
        if (res.data && (res.data.docxFileId || res.data.pdfFileId)) {
          this.job = res.data
        }
      })
    },
    hydratePrior() {
      const task = this.draft.task || {}
      const data = this.parse(task.priorYearJson)
      this.prior.report_year = data.report_year || (task.reportYear ? task.reportYear - 1 : '')
      this.prior.total_emissions_tco2 = data.total_emissions_tco2 == null ? '' : data.total_emissions_tco2
      this.prior.fuel_combustion_tco2 = data.fuel_combustion_tco2 == null ? '' : data.fuel_combustion_tco2
      this.prior.electricity_emissions_tco2 = data.electricity_emissions_tco2 == null ? '' : data.electricity_emissions_tco2
      this.prior.heat_emissions_tco2 = data.heat_emissions_tco2 == null ? '' : data.heat_emissions_tco2
    },
    savePrior() {
      saveSection(this.draft.task.id, 'prior_year', {
        version: this.draft.task.version,
        data: this.prior
      }).then(() => {
        this.$message.success('已保存上年对比')
        this.$emit('saved')
      })
    },
    validate() {
      validateTask(this.draft.task.id).then(res => {
        const data = res.data || {}
        const must = data.mustFix || []
        this.issues = must.map(message => ({ severity: '必须修复', message }))
        if (data.ok) this.$message.success('校验通过')
      })
    },
    generate(kind) {
      getFactorPlan(this.draft.task.id).then(res => {
        const plan = res.data || {}
        const missing = plan.missing || []
        if (!missing.length) {
          this.startGenerate(kind, false)
          return
        }
        const reason = plan.reason || '现用核算模板缺少因子'
        this.$confirm(
          reason + '：' + missing.join('、') + '。是否使用系统默认因子继续生成？',
          '缺少模板因子',
          { type: 'warning', confirmButtonText: '使用默认因子', cancelButtonText: '取消' }
        ).then(() => {
          this.startGenerate(kind, true)
        }).catch(() => {})
      })
    },
    startGenerate(kind, useDefaultFactors) {
      createGenerationJob(this.draft.task.id, {
        outputKind: kind,
        aiEnabled: this.aiEnabled,
        useDefaultFactors
      }).then(res => {
        this.job = res.data
        this.poll()
      })
    },
    poll() {
      if (this.timer) clearInterval(this.timer)
      this.timer = setInterval(() => {
        if (!this.job) return
        getGenerationJob(this.draft.task.id, this.job.id).then(res => {
          this.job = res.data
          if (this.job && ['succeeded', 'failed'].includes(this.job.status)) {
            clearInterval(this.timer)
          }
        })
      }, 1500)
    },
    download(id, filename) {
      downloadReportArtifact(id).then(blob => {
        if (!blob || (blob.type && blob.type.indexOf('json') !== -1)) {
          this.$message.error('下载失败')
          return
        }
        saveAs(blob, filename || '报告.docx')
      }).catch(() => {
        this.$message.error('下载失败')
      })
    }
  }
}
</script>
<style scoped>
.review-step { margin-top: 24px; }
.prior-form { max-width: 480px; margin-bottom: 16px; }
h4 { margin: 0 0 8px; }
.gen-progress { margin-top: 20px; max-width: 480px; }
</style>
