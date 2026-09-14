<template>
  <div class="app-container" v-if="draft">
    <el-steps :active="active" finish-status="success" align-center>
      <el-step title="基础" />
      <el-step title="边界" />
      <el-step title="活动数据" />
      <el-step title="设备" />
      <el-step title="说明" />
      <el-step title="校验生成" />
    </el-steps>
    <basic-step v-if="active===0" :draft="draft" @saved="reload" />
    <boundary-step v-else-if="active===1" :draft="draft" @saved="reload" />
    <activity-step v-else-if="active===2" :draft="draft" @saved="reload" />
    <assets-step v-else-if="active===3" :draft="draft" @saved="reload" />
    <narrative-step v-else-if="active===4" :draft="draft" @saved="reload" />
    <review-step v-else :draft="draft" @saved="reload" />
    <div class="footer-nav">
      <el-button :disabled="active===0" @click="active--">上一步</el-button>
      <el-button :disabled="active===5" type="primary" @click="active++">下一步</el-button>
    </div>
  </div>
</template>
<script>
import { getDraft } from '@/api/carbon/report'
import BasicStep from './steps/BasicStep'
import BoundaryStep from './steps/BoundaryStep'
import ActivityStep from './steps/ActivityStep'
import AssetsStep from './steps/AssetsStep'
import NarrativeStep from './steps/NarrativeStep'
import ReviewStep from './steps/ReviewStep'
export default {
  name: 'CarbonReportEdit',
  components: { BasicStep, BoundaryStep, ActivityStep, AssetsStep, NarrativeStep, ReviewStep },
  data() {
    return { active: 0, draft: null }
  },
  created() {
    this.reload(true)
  },
  activated() {
    this.reload(this.active === 0)
  },
  methods: {
    hasGenerated() {
      const task = (this.draft && this.draft.task) || {}
      const job = this.draft && this.draft.latestJob
      return task.status === 'GENERATED' || (job && (job.docxFileId || job.pdfFileId))
    },
    reload(openReviewIfGenerated) {
      getDraft(this.$route.params.id).then(res => {
        this.draft = res.data
        if (openReviewIfGenerated && this.hasGenerated()) {
          this.active = 5
        }
      })
    }
  }
}
</script>
<style scoped>
.footer-nav { margin-top: 24px; display: flex; justify-content: space-between; }
</style>
