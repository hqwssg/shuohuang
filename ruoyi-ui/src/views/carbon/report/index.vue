<template>
  <div class="app-container">
    <el-form :inline="true" size="small">
      <el-form-item>
        <el-button type="primary" icon="el-icon-plus" v-hasPermi="['carbon:report:add']" @click="$router.push('/carbon/report/create')">新建报告</el-button>
        <el-button @click="$router.push('/carbon/report/query')">核算结果查询</el-button>
      </el-form-item>
    </el-form>
    <el-table v-loading="loading" :data="rows">
      <el-table-column prop="id" label="编号" width="80" />
      <el-table-column prop="title" label="标题" />
      <el-table-column prop="reportYear" label="年度" width="80" />
      <el-table-column prop="status" label="状态" width="120">
        <template slot-scope="scope">{{ statusText(scope.row.status) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="280">
        <template slot-scope="scope">
          <el-button type="text" @click="$router.push('/carbon/report/edit/' + scope.row.id)">填报</el-button>
          <el-button v-if="scope.row.latestDocxFileId" type="text" @click="download(scope.row.latestDocxFileId, '报告.docx')">Word</el-button>
          <el-button v-if="scope.row.latestPdfFileId" type="text" @click="download(scope.row.latestPdfFileId, '报告.pdf')">PDF</el-button>
          <el-button type="text" class="danger-text" v-hasPermi="['carbon:report:remove']" @click="remove(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>
<script>
import { listTasks, removeTask, downloadReportArtifact } from '@/api/carbon/report'
import { saveAs } from 'file-saver'
export default {
  name: 'CarbonReportIndex',
  data() {
    return { loading: false, rows: [] }
  },
  created() {
    this.fetch()
  },
  methods: {
    statusText(status) {
      if (status === 'GENERATED') return '已生成'
      if (status === 'DRAFT') return '草稿'
      return status || ''
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
    },
    fetch() {
      this.loading = true
      listTasks().then(res => {
        this.rows = res.data || []
      }).finally(() => { this.loading = false })
    },
    remove(row) {
      this.$confirm('删除后列表不再显示，填报入口也会失效。活动数据和已生成文件仍保留在库中。确定删除「' + row.title + '」？', '逻辑删除', {
        type: 'warning'
      }).then(() => {
        return removeTask(row.id)
      }).then(() => {
        this.$message.success('已删除')
        this.fetch()
      }).catch(() => {})
    }
  }
}
</script>
<style scoped>
.danger-text { color: #f56c6c; }
</style>
