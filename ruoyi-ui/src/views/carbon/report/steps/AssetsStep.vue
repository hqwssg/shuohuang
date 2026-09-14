<template>
  <div class="assets-step">
    <el-alert
      title="用能设备默认来自企业档案；报告配图默认用肃宁图，可换成本报告的图。本页只改这一份报告，不会回写档案。"
      type="info"
      :closable="false"
      show-icon
      style="margin-bottom:16px"
    />
    <h4>报告配图</h4>
    <el-alert
      title="封面 logo、组织机构图、机关所在地默认带入。换图只影响这一份报告。"
      type="info"
      :closable="false"
      show-icon
      style="margin-bottom:8px"
    />
    <div class="image-slots">
      <div v-for="slot in imageSlots" :key="slot.role" class="image-card">
        <div class="image-title">{{ slot.label }}</div>
        <div class="image-preview">
          <img v-if="previews[slot.role]" :src="previews[slot.role]" :alt="slot.label" />
          <span v-else>加载中…</span>
        </div>
        <div class="image-meta">{{ customLabel(slot.role) }}</div>
        <div class="image-actions">
          <el-button size="mini" type="primary" @click="pickImage(slot.role)">更换</el-button>
          <el-button size="mini" :disabled="!isCustom(slot.role)" @click="resetImage(slot.role)">恢复默认</el-button>
        </div>
      </div>
    </div>
    <input ref="imageFile" type="file" accept="image/png,image/jpeg" style="display:none" @change="onImagePicked" />
    <h4>计量器具</h4>
    <el-table v-if="meters.length" :data="meters" size="small">
      <el-table-column prop="kind" label="类别" width="120" />
      <el-table-column prop="name" label="名称" />
      <el-table-column prop="energy" label="能源种类" width="140" />
      <el-table-column prop="model" label="型号" width="120" />
      <el-table-column prop="sourceTable" label="来源表" min-width="220" />
    </el-table>
    <el-empty v-else description="当前报告还没有带入的计量表" />
    <h4>第7章监测设备</h4>
    <el-alert
      title="精度、量程请手填，不要编造。检定周期可先用「每年1次」。"
      type="warning"
      :closable="false"
      show-icon
      style="margin-bottom:8px"
    />
    <el-table :data="monitoring" size="mini">
      <el-table-column label="名称" min-width="140">
        <template slot-scope="scope">
          <el-input v-model="scope.row.name" size="mini" />
        </template>
      </el-table-column>
      <el-table-column label="型号" width="120">
        <template slot-scope="scope">
          <el-input v-model="scope.row.model" size="mini" />
        </template>
      </el-table-column>
      <el-table-column label="精度" width="100">
        <template slot-scope="scope">
          <el-input v-model="scope.row.accuracy" size="mini" />
        </template>
      </el-table-column>
      <el-table-column label="量程" width="120">
        <template slot-scope="scope">
          <el-input v-model="scope.row.measurementRange" size="mini" />
        </template>
      </el-table-column>
      <el-table-column label="安装位置" min-width="120">
        <template slot-scope="scope">
          <el-input v-model="scope.row.location" size="mini" />
        </template>
      </el-table-column>
      <el-table-column label="检定周期" width="120">
        <template slot-scope="scope">
          <el-input v-model="scope.row.calibrationFrequency" size="mini" />
        </template>
      </el-table-column>
      <el-table-column label="" width="70">
        <template slot-scope="scope">
          <el-button type="text" size="mini" @click="monitoring.splice(scope.$index, 1)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-button size="mini" style="margin-top:8px" @click="addMonitor">新增监测行</el-button>
    <h4>用能设备</h4>
    <el-table :data="equipment" size="mini">
      <el-table-column label="名称" min-width="140">
        <template slot-scope="scope">
          <el-input v-model="scope.row.name" size="mini" />
        </template>
      </el-table-column>
      <el-table-column label="类别" width="120">
        <template slot-scope="scope">
          <el-input v-model="scope.row.category" size="mini" />
        </template>
      </el-table-column>
      <el-table-column label="数量" width="90">
        <template slot-scope="scope">
          <el-input v-model="scope.row.quantity" size="mini" />
        </template>
      </el-table-column>
      <el-table-column label="单位" width="70">
        <template slot-scope="scope">
          <el-input v-model="scope.row.unit" size="mini" />
        </template>
      </el-table-column>
      <el-table-column label="型号" width="120">
        <template slot-scope="scope">
          <el-input v-model="scope.row.model" size="mini" />
        </template>
      </el-table-column>
      <el-table-column label="能源种类" width="100">
        <template slot-scope="scope">
          <el-input v-model="scope.row.energyType" size="mini" />
        </template>
      </el-table-column>
      <el-table-column label="备注" min-width="160">
        <template slot-scope="scope">
          <el-input v-model="scope.row.remark" size="mini" />
        </template>
      </el-table-column>
      <el-table-column label="" width="70">
        <template slot-scope="scope">
          <el-button type="text" size="mini" @click="equipment.splice(scope.$index, 1)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-button size="mini" style="margin-top:8px" @click="addRow">新增一行</el-button>
    <el-button type="primary" size="mini" style="margin-top:8px;margin-left:8px" @click="save">保存设备与监测</el-button>
  </div>
</template>
<script>
import { saveSection, getDeptProfile, uploadReportImage, resetReportImage, fetchReportImageBlob } from '@/api/carbon/report'

const KNOWN_MODELS = {
  '肃宁天然气计量表': 'LWQ-50',
  '肃宁柴油计量表': 'LC-40',
  '肃宁外购电表': 'DTZ341',
  '肃宁外购热表': 'RLB-80'
}

const IMAGE_SLOTS = [
  { role: 'cover_logo', label: '封面 Logo' },
  { role: 'org_chart', label: '组织机构图' },
  { role: 'office_location', label: '机关所在地' }
]

export default {
  name: 'AssetsStep',
  props: { draft: { type: Object, required: true } },
  data() {
    return { equipment: [], monitoring: [], previews: {}, pendingRole: '', imageSlots: IMAGE_SLOTS }
  },
  computed: {
    meters() {
      const seen = {}
      const rows = []
      const push = (key, row) => {
        if (seen[key]) return
        seen[key] = true
        rows.push(row)
      }
      ;(this.draft.fuels || []).forEach(item => {
        const name = item.facility || item.sourceName || ''
        push('fuel:' + name + ':' + (item.fuelType || ''), {
          kind: '化石燃料',
          name: name || '未命名燃料计量表',
          energy: this.fuelLabel(item.fuelType),
          model: KNOWN_MODELS[name] || '',
          sourceTable: 'emission_fossil_fuel_meter_info'
        })
      })
      ;(this.draft.electricity || []).forEach(item => {
        const name = item.sourceName || item.facility || ''
        push('el:' + name, {
          kind: '外购电力',
          name: name || '未命名电表',
          energy: '电力',
          model: KNOWN_MODELS[name] || '',
          sourceTable: 'emission_meter_info'
        })
      })
      ;(this.draft.heat || []).forEach(item => {
        const name = item.site || item.sourceName || item.facility || ''
        push('heat:' + name, {
          kind: '外购热力',
          name: name || '未命名热表',
          energy: '热力',
          model: KNOWN_MODELS[name] || '',
          sourceTable: 'emission_purchased_heat_meter_info'
        })
      })
      return rows
    }
  },
  created() {
    this.hydrate()
    this.loadPreviews()
  },
  beforeDestroy() {
    Object.keys(this.previews).forEach(role => {
      if (this.previews[role]) URL.revokeObjectURL(this.previews[role])
    })
  },
  watch: {
    'draft.images': {
      handler() {
        this.loadPreviews()
      }
    }
  },
  methods: {
    customFile(role) {
      const images = (this.draft && this.draft.images) || []
      return images.find(item => item && item.role === role && item.id) || null
    },
    isCustom(role) {
      return !!this.customFile(role)
    },
    customLabel(role) {
      return this.isCustom(role) ? '本报告已替换' : '使用默认图'
    },
    revokePreview(role) {
      if (this.previews[role]) {
        URL.revokeObjectURL(this.previews[role])
        this.$set(this.previews, role, '')
      }
    },
    previewUrl(role) {
      const file = this.customFile(role)
      if (file && file.id) {
        return '/carbon/report/artifacts/' + file.id + '/download'
      }
      return '/carbon/report/default-images/' + role
    },
    loadPreviews() {
      IMAGE_SLOTS.forEach(slot => {
        fetchReportImageBlob(this.previewUrl(slot.role)).then(blob => {
          if (!blob || (blob.type && blob.type.indexOf('json') !== -1)) {
            this.revokePreview(slot.role)
            return
          }
          this.revokePreview(slot.role)
          this.$set(this.previews, slot.role, URL.createObjectURL(blob))
        }).catch(() => {
          this.revokePreview(slot.role)
        })
      })
    },
    pickImage(role) {
      this.pendingRole = role
      this.$refs.imageFile.value = ''
      this.$refs.imageFile.click()
    },
    onImagePicked(event) {
      const file = event.target.files && event.target.files[0]
      const role = this.pendingRole
      this.pendingRole = ''
      event.target.value = ''
      if (!file || !role) return
      const name = (file.name || '').toLowerCase()
      if (!name.endsWith('.png') && !name.endsWith('.jpg') && !name.endsWith('.jpeg')) {
        this.$message.error('只支持 PNG 或 JPEG 图片')
        return
      }
      uploadReportImage(this.draft.task.id, role, file).then(() => {
        this.$message.success('已更换')
        this.$emit('saved')
      })
    },
    resetImage(role) {
      resetReportImage(this.draft.task.id, role).then(() => {
        this.$message.success('已恢复默认')
        this.$emit('saved')
      })
    },
    emptyRow() {
      return { name: '', category: '', quantity: '', unit: '', model: '', energyType: '', remark: '' }
    },
    emptyMonitor() {
      return {
        name: '',
        model: '',
        accuracy: '',
        measurementRange: '',
        location: '',
        calibrationFrequency: '每年1次',
        sourceTable: '',
        sourceId: ''
      }
    },
    normalize(row) {
      return {
        name: row.name || '',
        category: row.category || '',
        quantity: row.quantity == null ? '' : row.quantity,
        unit: row.unit || '',
        model: row.model || '',
        energyType: row.energyType || row.energy_type || '',
        remark: row.remark || ''
      }
    },
    normalizeMonitor(row) {
      return {
        name: row.name || '',
        model: row.model || '',
        accuracy: row.accuracy || '',
        measurementRange: row.measurementRange || row.measurement_range || '',
        location: row.location || '',
        calibrationFrequency: row.calibrationFrequency || row.calibration_frequency || '每年1次',
        sourceTable: row.sourceTable || row.source_table || '',
        sourceId: row.sourceId || row.source_id || ''
      }
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
    suggestedMonitoring() {
      return this.meters.map(meter => ({
        name: meter.name,
        model: meter.model || '',
        accuracy: '',
        measurementRange: '',
        location: '',
        calibrationFrequency: '每年1次',
        sourceTable: meter.sourceTable || '',
        sourceId: ''
      }))
    },
    hydrate() {
      const savedEq = this.draft.equipment || []
      if (savedEq.length) {
        this.equipment = savedEq.map(row => this.normalize(row))
      } else {
        const task = this.draft.task || {}
        if (task.deptId) {
          getDeptProfile(task.deptId).then(res => {
            const profile = res.data || {}
            this.equipment = this.parseList(profile.defaultEquipmentJson).map(row => this.normalize(row))
          }).catch(() => {
            this.equipment = []
          })
        } else {
          this.equipment = []
        }
      }
      const savedMon = this.draft.monitoring || []
      if (savedMon.length) {
        this.monitoring = savedMon.map(row => this.normalizeMonitor(row))
      } else {
        this.monitoring = this.suggestedMonitoring()
      }
    },
    addRow() {
      this.equipment.push(this.emptyRow())
    },
    addMonitor() {
      this.monitoring.push(this.emptyMonitor())
    },
    save() {
      saveSection(this.draft.task.id, 'assets', {
        version: this.draft.task.version,
        data: { equipment: this.equipment, monitoring: this.monitoring }
      }).then(() => {
        this.$message.success('已保存')
        this.$emit('saved')
      })
    },
    fuelLabel(type) {
      if (type === 'natural_gas') return '天然气'
      if (type === 'gasoline') return '汽油'
      if (type === 'diesel') return '柴油'
      return type || ''
    }
  }
}
</script>
<style scoped>
.assets-step { margin-top: 24px; max-width: 1100px; }
h4 { margin: 16px 0 8px; }
.image-slots { display: flex; gap: 16px; flex-wrap: wrap; margin-bottom: 16px; }
.image-card { width: 240px; border: 1px solid #ebeef5; border-radius: 4px; padding: 12px; background: #fff; }
.image-title { font-weight: 600; margin-bottom: 8px; }
.image-preview { height: 140px; display: flex; align-items: center; justify-content: center; background: #f5f7fa; overflow: hidden; }
.image-preview img { max-width: 100%; max-height: 140px; object-fit: contain; }
.image-meta { color: #909399; font-size: 12px; margin: 8px 0; }
.image-actions { display: flex; gap: 8px; }
</style>
