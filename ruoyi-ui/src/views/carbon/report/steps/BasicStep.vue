<template>
  <div class="basic-step">
    <el-alert
      title="编制单位和签核人默认来自企业档案，本页只改这一份报告，不会回写档案。报告编号每年手填。"
      type="info"
      :closable="false"
      show-icon
      style="margin-bottom:16px"
    />
    <el-form label-width="140px" size="small">
      <el-form-item label="报告编号">
        <el-input v-model="form.report_number" placeholder="本年报告编号，不从档案带入" />
      </el-form-item>
      <el-form-item label="编制单位">
        <el-input v-model="form.compiler" placeholder="由企业档案带入" />
      </el-form-item>
      <el-form-item label="批准人">
        <el-input v-model="form.approver" placeholder="由企业档案带入" />
      </el-form-item>
      <el-form-item label="审核人">
        <el-input v-model="form.reviewer" placeholder="由企业档案带入" />
      </el-form-item>
      <el-form-item label="校核人">
        <el-input v-model="form.checker" placeholder="由企业档案带入" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="save">保存基础信息</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>
<script>
import { saveSection, getDeptProfile } from '@/api/carbon/report'
export default {
  name: 'BasicStep',
  props: { draft: { type: Object, required: true } },
  data() {
    return { form: { report_number: '', compiler: '', approver: '', reviewer: '', checker: '' } }
  },
  created() {
    this.hydrate()
  },
  methods: {
    parse(json) {
      if (!json) return {}
      if (typeof json === 'object') return json
      try { return JSON.parse(json) } catch (e) { return {} }
    },
    hydrate() {
      const task = this.draft.task || {}
      const meta = this.parse(task.metadataJson)
      this.form.report_number = meta.report_number || ''
      this.form.compiler = meta.compiler || ''
      this.form.approver = meta.approver || ''
      this.form.reviewer = meta.reviewer || ''
      this.form.checker = meta.checker || ''
      const needProfile = !this.form.compiler || !this.form.approver || !this.form.reviewer || !this.form.checker
      if (!needProfile || !task.deptId) {
        return
      }
      getDeptProfile(task.deptId).then(res => {
        const profile = res.data || {}
        if (!this.form.compiler) {
          this.form.compiler = profile.defaultCompiler || ''
        }
        if (!this.form.approver) {
          this.form.approver = profile.defaultApprover || ''
        }
        if (!this.form.reviewer) {
          this.form.reviewer = profile.defaultReviewer || ''
        }
        if (!this.form.checker) {
          this.form.checker = profile.defaultChecker || ''
        }
      }).catch(() => {})
    },
    save() {
      saveSection(this.draft.task.id, 'basic', { version: this.draft.task.version, data: this.form }).then(() => {
        this.$message.success('已保存')
        this.$emit('saved')
      })
    }
  }
}
</script>
<style scoped>
.basic-step { margin-top: 24px; max-width: 720px; }
</style>
