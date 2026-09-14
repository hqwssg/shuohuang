<template>
  <div class="narrative-step">
    <el-alert
      title="第三章活动数据来源说明默认来自企业档案，本页只改这一份报告，不会回写档案。"
      type="info"
      :closable="false"
      show-icon
      style="margin-bottom:16px"
    />
    <h4>天然气</h4>
    <el-form label-width="140px" size="small">
      <el-form-item label="用途"><el-input type="textarea" :rows="2" v-model="form.natural_gas.usage" /></el-form-item>
      <el-form-item label="计量方式"><el-input v-model="form.natural_gas.metering_method" /></el-form-item>
      <el-form-item label="来源文件"><el-input v-model="form.natural_gas.source_documents_text" placeholder="多个用逗号分隔" /></el-form-item>
      <el-form-item label="单位说明"><el-input v-model="form.natural_gas.unit_note" /></el-form-item>
      <el-form-item label="标况说明"><el-input type="textarea" :rows="2" v-model="form.natural_gas.standard_condition_note" /></el-form-item>
      <el-form-item label="变化说明"><el-input v-model="form.natural_gas.change_note" /></el-form-item>
    </el-form>
    <h4>汽柴油</h4>
    <el-form label-width="140px" size="small">
      <el-form-item label="用途"><el-input type="textarea" :rows="2" v-model="form.liquid_fuel.usage" /></el-form-item>
      <el-form-item label="计量方式"><el-input v-model="form.liquid_fuel.metering_method" /></el-form-item>
      <el-form-item label="来源文件"><el-input v-model="form.liquid_fuel.source_documents_text" placeholder="多个用逗号分隔" /></el-form-item>
    </el-form>
    <h4>电力</h4>
    <el-form label-width="140px" size="small">
      <el-form-item label="非化石电力"><el-input v-model="form.electricity.non_fossil_power_note" /></el-form-item>
      <el-form-item label="电网来源"><el-input v-model="form.electricity.grid_source" /></el-form-item>
      <el-form-item label="来源文件"><el-input v-model="form.electricity.source_documents_text" /></el-form-item>
      <el-form-item label="结算说明"><el-input type="textarea" :rows="2" v-model="form.electricity.settlement_note" /></el-form-item>
    </el-form>
    <h4>热力</h4>
    <el-form label-width="140px" size="small">
      <el-form-item label="协议说明"><el-input v-model="form.heat.contract_note" /></el-form-item>
      <el-form-item label="缴费方式"><el-input v-model="form.heat.payment_method" /></el-form-item>
      <el-form-item label="估算方法"><el-input v-model="form.heat.estimation_method" /></el-form-item>
      <el-form-item label="转供说明"><el-input v-model="form.heat.no_resale_note" /></el-form-item>
      <el-form-item label="来源文件"><el-input v-model="form.heat.source_documents_text" /></el-form-item>
    </el-form>
    <el-button type="primary" size="small" @click="save">保存说明</el-button>
  </div>
</template>
<script>
import { saveSection, getDeptProfile } from '@/api/carbon/report'

function emptySection() {
  return { usage: '', metering_method: '', source_documents_text: '', unit_note: '', standard_condition_note: '', change_note: '', non_fossil_power_note: '', grid_source: '', settlement_note: '', contract_note: '', payment_method: '', estimation_method: '', no_resale_note: '' }
}

export default {
  name: 'NarrativeStep',
  props: { draft: { type: Object, required: true } },
  data() {
    return {
      form: {
        natural_gas: emptySection(),
        liquid_fuel: emptySection(),
        electricity: emptySection(),
        heat: emptySection()
      }
    }
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
    docsText(list) {
      if (Array.isArray(list)) return list.join('，')
      if (typeof list === 'string') return list
      return ''
    },
    docsList(text) {
      return String(text || '').split(/[,，]/).map(s => s.trim()).filter(Boolean)
    },
    applyProse(raw) {
      const data = this.parse(raw)
      const assign = (key, fields) => {
        const src = data[key] || {}
        fields.forEach(field => {
          this.form[key][field] = src[field] || ''
        })
        this.form[key].source_documents_text = this.docsText(src.source_documents)
      }
      assign('natural_gas', ['usage', 'metering_method', 'unit_note', 'standard_condition_note', 'change_note'])
      assign('liquid_fuel', ['usage', 'metering_method'])
      assign('electricity', ['non_fossil_power_note', 'grid_source', 'settlement_note'])
      assign('heat', ['contract_note', 'payment_method', 'estimation_method', 'no_resale_note'])
    },
    isEmpty() {
      return !this.form.natural_gas.usage && !this.form.liquid_fuel.usage && !this.form.electricity.grid_source && !this.form.heat.contract_note
    },
    hydrate() {
      this.applyProse(this.draft.task && this.draft.task.activityProseJson)
      if (!this.isEmpty()) return
      const deptId = this.draft.task && this.draft.task.deptId
      if (!deptId) return
      getDeptProfile(deptId).then(res => {
        const profile = res.data || {}
        this.applyProse(profile.defaultActivityProseJson)
      }).catch(() => {})
    },
    payload() {
      return {
        natural_gas: {
          usage: this.form.natural_gas.usage,
          metering_method: this.form.natural_gas.metering_method,
          unit_note: this.form.natural_gas.unit_note,
          standard_condition_note: this.form.natural_gas.standard_condition_note,
          change_note: this.form.natural_gas.change_note,
          source_documents: this.docsList(this.form.natural_gas.source_documents_text)
        },
        liquid_fuel: {
          usage: this.form.liquid_fuel.usage,
          metering_method: this.form.liquid_fuel.metering_method,
          source_documents: this.docsList(this.form.liquid_fuel.source_documents_text)
        },
        electricity: {
          non_fossil_power_note: this.form.electricity.non_fossil_power_note,
          grid_source: this.form.electricity.grid_source,
          settlement_note: this.form.electricity.settlement_note,
          source_documents: this.docsList(this.form.electricity.source_documents_text)
        },
        heat: {
          contract_note: this.form.heat.contract_note,
          payment_method: this.form.heat.payment_method,
          estimation_method: this.form.heat.estimation_method,
          no_resale_note: this.form.heat.no_resale_note,
          source_documents: this.docsList(this.form.heat.source_documents_text)
        }
      }
    },
    save() {
      saveSection(this.draft.task.id, 'narrative', {
        version: this.draft.task.version,
        data: this.payload()
      }).then(() => {
        this.$message.success('已保存')
        this.$emit('saved')
      })
    }
  }
}
</script>
<style scoped>
.narrative-step { margin-top: 24px; max-width: 860px; }
h4 { margin: 16px 0 8px; }
</style>
