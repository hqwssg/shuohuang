<template>
  <div class="activity-step">
    <p>燃料/电/热来自成功核算任务，自动字段只读。需要改数时使用「本报告覆盖」，来源原值不会被改写。</p>
    <el-table :data="draft.fuels" size="small">
      <el-table-column prop="fuelType" label="燃料" width="120" />
      <el-table-column prop="facility" label="设施" />
      <el-table-column prop="sourceValue" label="来源值" width="100" />
      <el-table-column prop="sourceUnit" label="来源单位" width="100" />
      <el-table-column prop="normalizedValue" label="报告值" width="100" />
      <el-table-column prop="normalizedUnit" label="报告单位" width="100" />
      <el-table-column prop="dataOrigin" label="来源" width="110">
        <template slot-scope="scope">
          <el-tag size="mini" :type="scope.row.dataOrigin === 'AUTO' ? 'success' : 'warning'">{{ originLabel(scope.row.dataOrigin) }}</el-tag>
        </template>
      </el-table-column>
    </el-table>
    <h4>外购电力</h4>
    <el-table :data="draft.electricity" size="small">
      <el-table-column prop="sourceName" label="计量点" />
      <el-table-column prop="normalizedValue" label="MWh" width="120" />
      <el-table-column prop="dataOrigin" label="来源" width="110">
        <template slot-scope="scope">
          <el-tag size="mini">{{ originLabel(scope.row.dataOrigin) }}</el-tag>
        </template>
      </el-table-column>
    </el-table>
    <h4>外购热力</h4>
    <el-table :data="draft.heat" size="small">
      <el-table-column prop="site" label="站点" />
      <el-table-column prop="normalizedValue" label="GJ" width="120" />
      <el-table-column prop="dataOrigin" label="来源" width="110">
        <template slot-scope="scope">
          <el-tag size="mini">{{ originLabel(scope.row.dataOrigin) }}</el-tag>
        </template>
      </el-table-column>
    </el-table>
    <h4>工作量</h4>
    <el-alert
      title="工作量指标名来自企业档案，数量每年手填。不填数量不会按 0 生成。"
      type="info"
      :closable="false"
      show-icon
      style="margin-bottom:8px"
    />
    <el-table :data="workload" size="mini">
      <el-table-column label="名称" min-width="180">
        <template slot-scope="scope">
          <el-input v-model="scope.row.name" size="mini" />
        </template>
      </el-table-column>
      <el-table-column label="数量" width="140">
        <template slot-scope="scope">
          <el-input v-model="scope.row.quantity" size="mini" />
        </template>
      </el-table-column>
      <el-table-column label="单位" width="100">
        <template slot-scope="scope">
          <el-input v-model="scope.row.unit" size="mini" />
        </template>
      </el-table-column>
      <el-table-column label="" width="70">
        <template slot-scope="scope">
          <el-button type="text" size="mini" @click="workload.splice(scope.$index, 1)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-button size="mini" style="margin-top:8px" @click="addWorkload">新增一行</el-button>
    <el-button type="primary" size="mini" style="margin-top:8px;margin-left:8px" @click="saveWorkload">保存工作量</el-button>
    <p v-if="draft.missingWorkload" class="gap">工作量未填，校验会标缺失，不会按 0 生成。</p>
  </div>
</template>
<script>
import { saveSection, getDeptProfile } from '@/api/carbon/report'
export default {
  name: 'ActivityStep',
  props: { draft: { type: Object, required: true } },
  data() {
    return { workload: [] }
  },
  created() {
    this.hydrateWorkload()
  },
  methods: {
    originLabel(origin) {
      if (origin === 'AUTO') return '自动带入'
      if (origin === 'OVERRIDE') return '本报告覆盖'
      if (origin === 'IMPORT') return '导入'
      return '手工'
    },
    parseList(raw) {
      if (!raw) return []
      if (Array.isArray(raw)) return raw
      if (typeof raw === 'string') {
        try {
          const parsed = JSON.parse(raw)
          return Array.isArray(parsed) ? parsed : []
        } catch (e) {
          return []
        }
      }
      return []
    },
    hydrateWorkload() {
      const saved = this.draft.workload || []
      if (saved.length) {
        this.workload = saved.map(row => ({
          name: row.name || '',
          quantity: row.quantity == null ? '' : row.quantity,
          unit: row.unit || ''
        }))
        return
      }
      const deptId = this.draft.task && this.draft.task.deptId
      if (!deptId) {
        this.workload = []
        return
      }
      getDeptProfile(deptId).then(res => {
        const profile = res.data || {}
        this.workload = this.parseList(profile.defaultWorkloadJson).map(row => ({
          name: row.name || '',
          quantity: '',
          unit: row.unit || ''
        }))
      }).catch(() => {
        this.workload = []
      })
    },
    addWorkload() {
      this.workload.push({ name: '', quantity: '', unit: '' })
    },
    saveWorkload() {
      saveSection(this.draft.task.id, 'workload', {
        version: this.draft.task.version,
        data: { workload: this.workload }
      }).then(() => {
        this.$message.success('已保存')
        this.$emit('saved')
      })
    }
  }
}
</script>
<style scoped>
.activity-step { margin-top: 24px; }
h4 { margin: 16px 0 8px; }
.gap { color: #e6a23c; margin-top: 12px; }
</style>
