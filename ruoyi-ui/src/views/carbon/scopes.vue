<template>
  <div class="app-container scope-management">
    <div class="scope-toolbar">
      <el-radio-group v-model="type" size="medium" @change="load">
        <el-radio-button label="electricity">电力站点 / 区间</el-radio-button>
        <el-radio-button label="fossil">化石能源</el-radio-button>
        <el-radio-button label="heat">外购热能</el-radio-button>
      </el-radio-group>
      <el-input v-model="search" placeholder="搜索范围、公司、部门" clearable prefix-icon="el-icon-search" />
      <el-tooltip content="刷新" placement="top"><el-button icon="el-icon-refresh" circle @click="load" /></el-tooltip>
    </div>

    <el-table v-loading="loading" :data="visibleRows" class="scope-table" height="100%" border>
      <el-table-column prop="name" label="站点 / 区间 / 采集范围" min-width="190" />
      <el-table-column label="所属分公司" min-width="160">
        <template slot-scope="{ row }"><span v-if="row.companyName">{{ row.companyName }}</span><el-tag v-else type="warning">未分配</el-tag></template>
      </el-table-column>
      <el-table-column label="节点权限" min-width="360">
        <template slot-scope="{ row }">
          <el-tag v-for="grant in row.grants" :key="grant.grantId || `${grant.deptId}-${grant.nodeType}-${grant.nodeId}`" :type="grant.canWrite ? 'success' : 'info'" class="scope-grant">
            {{ grant.nodeName || row.name }} · {{ departmentLabel(grant.deptId) }} · {{ grant.canWrite ? '可维护' : '只读' }}<span v-if="grant.canDelegate"> · 可委派</span>
          </el-tag>
          <span v-if="!row.grants.length">暂无节点授权</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="145" fixed="right">
        <template slot-scope="{ row }">
          <el-tooltip :disabled="canEdit" content="没有采集范围授权权限">
            <span><el-button type="primary" icon="el-icon-s-check" :disabled="!canEdit" @click="edit(row)">分配</el-button></span>
          </el-tooltip>
        </template>
      </el-table-column>
      <template slot="empty"><el-empty description="没有可管理的范围" /></template>
    </el-table>

    <el-dialog :title="active ? active.name + ' · 一级范围分配' : '范围分配'" :visible.sync="open" width="min(960px, 94vw)" append-to-body :close-on-click-modal="false">
      <el-form label-width="110px">
        <el-form-item label="所属分公司">
          <el-select v-model="form.companyDeptId" :disabled="!options.companyAdmin" placeholder="请选择分公司" @change="changeCompany">
            <el-option v-for="company in options.companies" :key="company.deptId" :label="company.deptName" :value="company.deptId" />
          </el-select>
        </el-form-item>
        <el-form-item label="一级授权部门">
          <el-select v-model="selectedDeptIds" multiple filterable placeholder="选择部门或供电工队" class="department-select" @change="changeDepartments">
            <el-option v-for="department in candidateDepartments" :key="department.deptId" :label="departmentLabel(department.deptId)" :value="department.deptId" />
          </el-select>
        </el-form-item>
      </el-form>
      <el-table :data="form.grants" border max-height="230">
        <el-table-column label="部门 / 工队" min-width="240"><template slot-scope="{ row }">{{ departmentLabel(row.deptId) }}</template></el-table-column>
        <el-table-column label="权限" width="145"><template slot-scope="{ row }"><el-switch v-model="row.canWrite" active-text="维护" inactive-text="只读" /></template></el-table-column>
      </el-table>

      <div class="node-heading"><strong>二级节点授权</strong><span>部门可在此把所属范围继续委派给下属部门或供电工队</span></div>
      <el-table :data="nodes" border max-height="280">
        <el-table-column label="节点" min-width="260"><template slot-scope="{ row }"><span :style="{ paddingLeft: `${row.level * 18}px` }">{{ row.nodeName }}</span></template></el-table-column>
        <el-table-column label="已授权部门" min-width="300"><template slot-scope="{ row }"><el-tag v-for="grant in row.grants" :key="grant.grantId" class="scope-grant" :type="grant.canWrite ? 'success' : 'info'">{{ departmentLabel(grant.deptId) }} · {{ grant.canWrite ? '维护' : '只读' }}</el-tag><span v-if="!row.grants.length">未分配</span></template></el-table-column>
        <el-table-column label="操作" width="120"><template slot-scope="{ row }"><el-button size="small" type="primary" plain icon="el-icon-share" @click="openNodeGrant(row)">委派</el-button></template></el-table-column>
      </el-table>
      <span slot="footer"><el-button @click="open = false">取消</el-button><el-button type="primary" icon="el-icon-check" :loading="saving" :disabled="!form.companyDeptId" @click="save">保存一级分配</el-button></span>
    </el-dialog>

    <el-dialog :title="nodeActive ? '委派节点：' + nodeActive.nodeName : '节点委派'" :visible.sync="nodeOpen" width="min(620px, 92vw)" append-to-body :close-on-click-modal="false">
      <el-form label-width="100px">
        <el-form-item label="目标部门"><el-select v-model="nodeForm.deptId" filterable placeholder="选择下属部门或工队" class="department-select"><el-option v-for="department in candidateDepartments" :key="department.deptId" :label="departmentLabel(department.deptId)" :value="department.deptId" /></el-select></el-form-item>
        <el-form-item label="读取权限"><el-switch v-model="nodeForm.canRead" active-text="可见" inactive-text="不可见" /></el-form-item>
        <el-form-item label="维护权限"><el-switch v-model="nodeForm.canWrite" active-text="可维护" inactive-text="只读" :disabled="!nodeForm.canRead" /></el-form-item>
        <el-form-item label="继续委派"><el-switch v-model="nodeForm.canDelegate" active-text="允许" inactive-text="禁止" :disabled="!nodeForm.canRead" /></el-form-item>
      </el-form>
      <span slot="footer"><el-button @click="nodeOpen = false">取消</el-button><el-button type="primary" :loading="nodeSaving" :disabled="!nodeForm.deptId" @click="saveNodeGrant">保存节点权限</el-button></span>
    </el-dialog>
  </div>
</template>

<script>
import { scopeOptions, listScopes, listScopeNodes, saveScope, saveNodeGrant } from '@/api/carbon/scopes'
import { hasAnyPermission } from '@/utils/permissionMatch'

export default {
  name: 'CarbonScopes',
  data() {
    return { type: 'electricity', search: '', rows: [], nodes: [], loading: false, saving: false, nodeSaving: false, open: false, nodeOpen: false, active: null, nodeActive: null, selectedDeptIds: [], options: { companyAdmin: false, companies: [], departments: [] }, form: { companyDeptId: null, grants: [] }, nodeForm: { deptId: null, canRead: true, canWrite: false, canDelegate: false } }
  },
  computed: {
    canEdit() { return hasAnyPermission(this.$store.getters.permissions, ['carbon:scope:edit']) },
    visibleRows() { const text = this.search.trim(); return this.rows.filter(row => !text || [row.name, row.companyName, ...row.grants.map(g => `${g.nodeName || ''} ${this.departmentLabel(g.deptId)}`)].join(' ').includes(text)) },
    candidateDepartments() { return this.options.departments.filter(d => d.deptId !== this.form.companyDeptId && (d.ancestors || '').split(',').includes(String(this.form.companyDeptId))) }
  },
  async created() { await this.load() },
  methods: {
    departmentLabel(id) { const d = this.options.departments.find(item => item.deptId === id); if (!d) return String(id); const p = this.options.departments.find(item => item.deptId === d.parentId); return p ? `${p.deptName} / ${d.deptName}` : d.deptName },
    rootType() { return this.type === 'electricity' ? 'station' : 'scope' },
    isRootGrant(grant) { return !grant.nodeType || grant.nodeType === this.rootType() || (this.type !== 'electricity' && grant.nodeType === 'scope') },
    async load() { this.loading = true; try { const options = await scopeOptions(); this.options = options.data; const result = await listScopes(this.type); this.rows = result.data || [] } finally { this.loading = false } },
    async edit(row) { this.active = row; const rootGrants = row.grants.filter(g => this.isRootGrant(g)); this.form = { companyDeptId: row.companyDeptId || null, grants: rootGrants.map(g => ({ deptId: g.deptId, canWrite: !!g.canWrite })) }; this.selectedDeptIds = this.form.grants.map(g => g.deptId); const result = await listScopeNodes(this.type, row.id); this.nodes = (result.data || []).map(node => ({ ...node, level: node.nodeType === 'station' || node.nodeType === 'scope' ? 0 : node.nodeType === 'meter' ? 2 : 1 })); this.open = true },
    changeCompany() { this.selectedDeptIds = []; this.form.grants = [] },
    changeDepartments(ids) { this.form.grants = ids.map(id => this.form.grants.find(g => g.deptId === id) || { deptId: id, canWrite: false }) },
    async save() { this.saving = true; try { await saveScope(this.type, this.active.id, this.form); this.$message.success('一级范围分配已保存'); this.open = false; await this.load() } finally { this.saving = false } },
    openNodeGrant(node) { const existing = node.grants[0]; this.nodeActive = node; this.nodeForm = { deptId: existing ? existing.deptId : null, canRead: existing ? !!existing.canRead : true, canWrite: existing ? !!existing.canWrite : false, canDelegate: existing ? !!existing.canDelegate : false }; this.nodeOpen = true },
    async saveNodeGrant() { this.nodeSaving = true; try { await saveNodeGrant(this.type, this.active.id, this.nodeActive.nodeType, this.nodeActive.nodeId, this.nodeForm); this.$message.success('节点权限已保存'); this.nodeOpen = false; const result = await listScopeNodes(this.type, this.active.id); this.nodes = (result.data || []).map(node => ({ ...node, level: node.nodeType === 'station' || node.nodeType === 'scope' ? 0 : node.nodeType === 'meter' ? 2 : 1 })) } finally { this.nodeSaving = false } }
  }
}
</script>

<style scoped>
.scope-management { display: flex; flex-direction: column; min-height: 560px; height: calc(100vh - 185px); }
.scope-toolbar { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; margin-bottom: 16px; }
.scope-toolbar .el-input { width: 260px; }
.scope-table { flex: 1; min-height: 0; }
.scope-grant { margin: 3px; white-space: normal; height: auto; line-height: 22px; }
.department-select { width: 100%; }
.node-heading { display: flex; align-items: baseline; gap: 14px; margin: 22px 0 10px; border-top: 1px solid #ebeef5; padding-top: 16px; }
.node-heading span { color: #909399; font-size: 13px; }
@media (max-width: 700px) { .scope-toolbar .el-input { width: 100%; } .node-heading { display: block; } .node-heading span { display: block; margin-top: 6px; } }
</style>
