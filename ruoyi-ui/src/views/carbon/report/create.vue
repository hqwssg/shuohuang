<template>
  <div class="app-container">
    <h3>新建碳排放报告</h3>
    <el-form label-width="120px" size="small" style="max-width:640px">
      <el-form-item label="期间类型">
        <el-radio-group v-model="form.periodType">
          <el-radio label="YEAR">年报</el-radio>
          <el-radio label="MONTH">月报</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="报告年度">
        <el-input-number v-model="form.reportYear" :min="2000" :max="2100" />
      </el-form-item>
      <el-form-item v-if="form.periodType === 'MONTH'" label="月份">
        <el-input-number v-model="form.reportMonth" :min="1" :max="12" />
      </el-form-item>
      <el-form-item label="核算任务">
        <el-button @click="loadCalcs">查询成功任务</el-button>
        <el-table :data="calcs" highlight-current-row style="margin-top:8px" @current-change="row => form.calculationTemplateId = row && row.id">
          <el-table-column prop="id" label="任务" width="80" />
          <el-table-column prop="templateName" label="模板" />
          <el-table-column prop="totalEmission" label="总量" />
          <el-table-column prop="warning" label="提示" />
        </el-table>
        <p class="tip">一个期间有多条成功任务时必须手选，系统不会猜测。</p>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="saving" @click="submit">创建并带入</el-button>
        <el-button @click="$router.back()">返回</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>
<script>
import { createTask, listCalculations } from '@/api/carbon/report'
export default {
  name: 'CarbonReportCreate',
  data() {
    return {
      saving: false,
      calcs: [],
      form: { periodType: 'YEAR', reportYear: new Date().getFullYear() - 1, reportMonth: 1, calculationTemplateId: null }
    }
  },
  methods: {
    periodRange() {
      const y = this.form.reportYear
      if (this.form.periodType === 'MONTH') {
        const m = String(this.form.reportMonth).padStart(2, '0')
        return { periodStart: y + '-' + m + '-01', periodEnd: y + '-' + m + '-28' }
      }
      return { periodStart: y + '-01-01', periodEnd: y + '-12-31' }
    },
    loadCalcs() {
      listCalculations(this.periodRange()).then(res => { this.calcs = res.data || [] })
    },
    submit() {
      this.saving = true
      createTask(this.form).then(res => {
        this.$router.push('/carbon/report/edit/' + res.data.id)
      }).finally(() => { this.saving = false })
    }
  }
}
</script>
<style scoped>
.tip { color: #888; font-size: 12px; }
</style>
