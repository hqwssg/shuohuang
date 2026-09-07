<template>
  <div class="purchased-heat-collection-settings">
    <div class="tree-panel">
      <div class="tree-content">
        <el-tree
          ref="treeRef"
          :data="treeData"
          :props="{ label: 'name', children: 'children' }"
          node-key="id"
          :expand-on-click-node="false"
          @expand-change="handleExpandChange"
          @node-click="handleNodeClick"
        >
          <template #default="{ node, data }">
            <div class="tree-node" :class="{ 'disabled-node': data.status === 0, 'root-node': data.nodeType === 'root' }">
              <span class="node-left">
                <span class="node-icon">
                  <el-icon v-if="data.nodeType === 'root'"><FolderOpened /></el-icon>
                  <el-icon v-else-if="data.nodeType === 'scope'"><OfficeBuilding /></el-icon>
                  <el-icon v-else-if="data.nodeType === 'sub_scope'"><Collection /></el-icon>
                  <el-icon v-else-if="data.nodeType === 'meter'"><Odometer /></el-icon>
                </span>
                <span class="node-label" :title="data.name">{{ data.name }}</span>
                <el-tag
                  v-if="data.status === 0 && data.nodeType !== 'root'"
                  size="small"
                  type="info"
                  class="status-tag"
                >停用</el-tag>
              </span>

              <span v-if="data.nodeType === 'root'" class="node-actions">
                <el-button size="small" type="primary" @click="handleAddScope">
                  <el-icon><Plus /></el-icon>
                </el-button>
                <el-button size="small" type="primary" @click="handleExpandAll">展开全部</el-button>
                <el-button size="small" type="danger" @click="handleCollapseAll">折叠全部</el-button>
              </span>

              <span v-else-if="data.nodeType === 'scope'" class="node-actions">
                <el-button size="small" link type="primary" title="添加下属细分范围" @click.stop="handleAddSubScope(data)">
                  <el-icon><Plus /></el-icon>
                </el-button>
                <el-button size="small" link title="上移" @click.stop="handleMoveUp(data)">
                  <el-icon><ArrowUp /></el-icon>
                </el-button>
                <el-button size="small" link title="下移" @click.stop="handleMoveDown(data)">
                  <el-icon><ArrowDown /></el-icon>
                </el-button>
                <el-button
                  size="small"
                  link
                  type="danger"
                  title="删除"
                  :disabled="data.status === 1"
                  @click.stop="handleDelete(data)"
                >
                  <el-icon><Delete /></el-icon>
                </el-button>
                <el-button
                  size="small"
                  link
                  :type="data.status === 1 ? 'warning' : 'success'"
                  @click.stop="handleToggleStatus(data)"
                >
                  {{ data.status === 1 ? '停用' : '启用' }}
                </el-button>
              </span>

              <span v-else-if="data.nodeType === 'sub_scope'" class="node-actions">
                <el-button size="small" link type="primary" title="添加下属采集点" @click.stop="handleAddMeter(data)">
                  <el-icon><Plus /></el-icon>
                </el-button>
                <el-button size="small" link title="上移" @click.stop="handleMoveUp(data)">
                  <el-icon><ArrowUp /></el-icon>
                </el-button>
                <el-button size="small" link title="下移" @click.stop="handleMoveDown(data)">
                  <el-icon><ArrowDown /></el-icon>
                </el-button>
                <el-button
                  size="small"
                  link
                  type="danger"
                  title="删除"
                  :disabled="data.status === 1"
                  @click.stop="handleDelete(data)"
                >
                  <el-icon><Delete /></el-icon>
                </el-button>
                <el-button
                  size="small"
                  link
                  :type="data.status === 1 ? 'warning' : 'success'"
                  @click.stop="handleToggleStatus(data)"
                >
                  {{ data.status === 1 ? '停用' : '启用' }}
                </el-button>
              </span>

              <span v-else-if="data.nodeType === 'meter'" class="node-actions">
                <el-button size="small" link type="primary" title="添加下属采集点" @click.stop="handleAddMeter(data)">
                  <el-icon><Plus /></el-icon>
                </el-button>
                <el-button size="small" link title="上移" @click.stop="handleMoveUp(data)">
                  <el-icon><ArrowUp /></el-icon>
                </el-button>
                <el-button size="small" link title="下移" @click.stop="handleMoveDown(data)">
                  <el-icon><ArrowDown /></el-icon>
                </el-button>
                <el-button
                  size="small"
                  link
                  type="danger"
                  title="删除"
                  :disabled="data.status === 1"
                  @click.stop="handleDelete(data)"
                >
                  <el-icon><Delete /></el-icon>
                </el-button>
                <el-button
                  size="small"
                  link
                  :type="data.status === 1 ? 'warning' : 'success'"
                  @click.stop="handleToggleStatus(data)"
                >
                  {{ data.status === 1 ? '停用' : '启用' }}
                </el-button>
              </span>
            </div>
          </template>
        </el-tree>
        <el-empty v-if="!treeData?.length || !treeData[0]?.children?.length" description="暂无数据，请添加采集范围" />
      </div>
    </div>

    <div class="detail-panel">
      <div v-if="selectedNode" class="detail-content">
        <div class="detail-header">
          <h3>{{ selectedNode.name }} - 详情</h3>
          <div class="detail-actions">
            <el-button type="primary" @click="handleEdit(selectedNode)">编辑</el-button>
          </div>
        </div>
        <el-descriptions :column="2" border>
          <template v-if="selectedNode.nodeType === 'scope'">
            <el-descriptions-item label="名称">{{ selectedNode.name }}</el-descriptions-item>
            <el-descriptions-item label="拼音编码">{{ selectedNode.pinyinCode || '-' }}</el-descriptions-item>
            <el-descriptions-item label="排序">{{ selectedNode.sortOrder ?? '-' }}</el-descriptions-item>
            <el-descriptions-item label="描述" :span="2">{{ selectedNode.description || '-' }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="selectedNode.status === 1 ? 'success' : 'info'">
                {{ selectedNode.status === 1 ? '启用' : '停用' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ selectedNode.createdAt || '-' }}</el-descriptions-item>
            <el-descriptions-item label="更新时间">{{ selectedNode.updatedAt || '-' }}</el-descriptions-item>
          </template>

          <template v-else-if="selectedNode.nodeType === 'sub_scope'">
            <el-descriptions-item label="名称">{{ selectedNode.name }}</el-descriptions-item>
            <el-descriptions-item label="拼音编码">{{ selectedNode.pinyinCode || '-' }}</el-descriptions-item>
            <el-descriptions-item label="所属采集范围">{{ selectedNode.scopeName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="排序">{{ selectedNode.sortOrder ?? '-' }}</el-descriptions-item>
            <el-descriptions-item label="描述" :span="2">{{ selectedNode.description || '-' }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="selectedNode.status === 1 ? 'success' : 'info'">
                {{ selectedNode.status === 1 ? '启用' : '停用' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ selectedNode.createdAt || '-' }}</el-descriptions-item>
            <el-descriptions-item label="更新时间">{{ selectedNode.updatedAt || '-' }}</el-descriptions-item>
          </template>

          <template v-else-if="selectedNode.nodeType === 'meter'">
            <el-descriptions-item label="名称">{{ selectedNode.name }}</el-descriptions-item>
            <el-descriptions-item label="拼音编码">{{ selectedNode.pinyinCode || '-' }}</el-descriptions-item>
            <el-descriptions-item label="热力类型">{{ selectedNode.heatType || '-' }}</el-descriptions-item>
            <el-descriptions-item label="计量表型号">{{ selectedNode.meterModel || '-' }}</el-descriptions-item>
            <el-descriptions-item label="抄表方式">
              {{ selectedNode.meterReadingMethod === 1 ? '自动抄表' : '人工录入' }}
            </el-descriptions-item>
            <!-- 能耗用途（energy_use字段）暂时隐藏，改用三级能耗分类替代 -->
            <!-- <el-descriptions-item label="能耗用途">{{ selectedNode.energyUse || '-' }}</el-descriptions-item> -->
            <el-descriptions-item label="能耗数据归属">{{ selectedNode.energyAllocation || '-' }}</el-descriptions-item>
            <el-descriptions-item label="用途描述" :span="2">{{ selectedNode.purposeDescription || '-' }}</el-descriptions-item>
            <el-descriptions-item label="上级采集点">
              {{ selectedNode.parentMeterId ? (selectedNode.parentMeterName || 'ID: ' + selectedNode.parentMeterId) : '无' }}
            </el-descriptions-item>
            <el-descriptions-item label="自动抄表配置">
              {{ selectedNode.autoMeterReadingConfig ? '已配置' : '未配置' }}
            </el-descriptions-item>
            <el-descriptions-item label="是否累加量">
              {{ selectedNode.isCumulative === 1 ? '是' : '否' }}
            </el-descriptions-item>
            <el-descriptions-item label="是否移动源">
              {{ selectedNode.isMobileSource === 1 ? '是' : '否' }}
            </el-descriptions-item>
            <el-descriptions-item label="计量单位">{{ selectedNode.measurementUnit || '-' }}</el-descriptions-item>
            <el-descriptions-item label="数据来源系统">{{ selectedNode.dataSourceSystem || '-' }}</el-descriptions-item>
            <el-descriptions-item label="能耗一级分类">{{ getCategoryName(selectedNode.energyCategoryL1) || '-' }}</el-descriptions-item>
            <el-descriptions-item label="能耗二级分类">{{ getCategoryName(selectedNode.energyCategoryL2) || '-' }}</el-descriptions-item>
            <el-descriptions-item label="能耗三级分类">{{ getCategoryName(selectedNode.energyCategoryL3) || '-' }}</el-descriptions-item>
            <el-descriptions-item label="能耗用途分类">{{ selectedNode.energyUseCategory || '-' }}</el-descriptions-item>
            <el-descriptions-item label="计费周期">
              {{ selectedNode.billingCycleLength || '-' }} {{ selectedNode.billingCycleUnit || '' }}
              {{ selectedNode.billingCycleStartDate ? '（起始：' + selectedNode.billingCycleStartDate + '）' : '' }}
            </el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="selectedNode.status === 1 ? 'success' : 'info'">
                {{ selectedNode.status === 1 ? '启用' : '停用' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ selectedNode.createdAt || '-' }}</el-descriptions-item>
            <el-descriptions-item label="更新时间">{{ selectedNode.updatedAt || '-' }}</el-descriptions-item>
          </template>
        </el-descriptions>
        <template v-if="selectedNode.nodeType === 'meter'">
          <div class="detail-actions" style="margin-top: 16px;">
            <el-button type="warning" @click="openCascadeDialog">更改采集点级联关系</el-button>
            <el-button type="success" @click="openSubScopeDialog">更改采集点所属细分范围</el-button>
          </div>
        </template>
      </div>
      <el-empty v-else description="请从左侧选择一个节点查看详情" />
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="896px"
      @close="resetForm"
    >
      <el-form :model="formData" :rules="formRules" ref="formRef" label-width="120px">
        <el-form-item label="名称" prop="name" v-if="currentNodeType !== 'meter'">
          <el-input v-model="formData.name" placeholder="请输入名称" />
        </el-form-item>
        <el-form-item label="拼音编码" v-if="currentNodeType !== 'meter'">
          <el-input v-model="formData.pinyinCode" placeholder="由系统自动生成" disabled />
        </el-form-item>

        <template v-if="currentNodeType === 'scope'">
          <el-form-item label="描述">
            <el-input
              v-model="formData.description"
              type="textarea"
              :rows="3"
              placeholder="请输入描述信息"
            />
          </el-form-item>
        </template>

        <template v-else-if="currentNodeType === 'sub_scope'">
          <el-form-item label="所属采集范围" prop="collectionScopeId">
            <el-select v-model="formData.collectionScopeId" placeholder="请选择所属采集范围" filterable style="width: 100%">
              <el-option
                v-for="scope in scopeOptions"
                :key="scope.id"
                :label="scope.name"
                :value="scope.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="描述">
            <el-input
              v-model="formData.description"
              type="textarea"
              :rows="3"
              placeholder="请输入描述信息"
            />
          </el-form-item>
        </template>

        <template v-else-if="currentNodeType === 'meter'">
          <el-form-item v-if="dialogMode === 'edit'" label="所属细分范围">
            <el-input :model-value="formData.subScopeName" disabled />
          </el-form-item>
          <el-form-item v-if="dialogMode === 'edit'" label="上级采集点">
            <el-input :model-value="formData.parentMeterName || '无'" disabled />
          </el-form-item>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="名称" prop="name">
                <el-input v-model="formData.name" placeholder="请输入名称" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="热力类型">
                <el-select v-model="formData.heatType" placeholder="请选择热力类型" filterable style="width: 100%">
                  <el-option
                    v-for="item in heatTypeOptions"
                    :key="item.code"
                    :label="item.value"
                    :value="item.value"
                  />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="计量表型号">
                <el-input v-model="formData.meterModel" placeholder="请输入计量表型号" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <!-- 能耗用途（energy_use字段）暂时隐藏，改用三级能耗分类替代，后续如需启用可恢复此表单项 -->
              <el-form-item label="能耗场景大类">
                <el-select
                  v-model="formData.energyCategoryL1"
                  placeholder="请选择能耗场景大类"
                  clearable
                  @change="handleCategoryL1Change"
                >
                  <el-option
                    v-for="item in energyCategoryL1Options"
                    :key="item.categoryCode"
                    :label="item.categoryName"
                    :value="item.categoryCode"
                  />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="能耗数据归属">
                <el-select v-model="formData.energyAllocation" placeholder="请选择数据归属" style="width: 100%">
                  <el-option
                    v-for="item in energyAllocationOptions"
                    :key="item.code"
                    :label="item.value"
                    :value="item.value"
                  />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="能耗二级分类">
                <el-select
                  v-model="formData.energyCategoryL2"
                  placeholder="请选择二级分类"
                  clearable
                  :disabled="!formData.energyCategoryL1"
                  @change="handleCategoryL2Change"
                >
                  <el-option
                    v-for="item in energyCategoryL2Options"
                    :key="item.categoryCode"
                    :label="item.categoryName"
                    :value="item.categoryCode"
                  />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="抄表方式">
                <el-radio-group v-model="formData.meterReadingMethod">
                  <el-radio :value="1">自动抄表</el-radio>
                  <el-radio :value="0">人工录入</el-radio>
                </el-radio-group>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="能耗三级分类">
                <el-select
                  v-model="formData.energyCategoryL3"
                  placeholder="请选择三级分类"
                  clearable
                  :disabled="!formData.energyCategoryL2"
                >
                  <el-option
                    v-for="item in energyCategoryL3Options"
                    :key="item.categoryCode"
                    :label="item.categoryName"
                    :value="item.categoryCode"
                  />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="20">
            <el-col :span="8">
              <el-form-item label="是否累加量" label-width="100px">
                <el-radio-group v-model="formData.isCumulative">
                  <el-radio :value="1">是</el-radio>
                  <el-radio :value="0">否</el-radio>
                </el-radio-group>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="是否移动源" label-width="100px">
                <el-radio-group v-model="formData.isMobileSource">
                  <el-radio :value="1">是</el-radio>
                  <el-radio :value="0">否</el-radio>
                </el-radio-group>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="计量单位">
                <el-select v-model="formData.measurementUnit" placeholder="请选择计量单位" filterable>
                  <el-option v-for="u in heatUnits" :key="u.unitCode" :label="u.unitName" :value="u.unitCode" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="数据来源系统">
                <div class="input-with-button">
                  <el-input
                    v-model="formData.dataSourceSystem"
                    placeholder="请点击右侧按钮选择"
                    readonly
                  />
                  <el-button type="primary" @click="openDataSourceSystemDialog">
                    <el-icon><Plus /></el-icon>
                  </el-button>
                </div>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="能耗用途分类">
                <el-select
                  v-model="formData.energyUseCategory"
                  filterable
                  :filter-method="filterEnergyUse"
                  placeholder="请选择能耗用途分类"
                  clearable
                  @visible-change="handleEnergyUseVisibleChange"
                >
                  <el-option
                    v-for="item in visibleEnergyUses"
                    :key="item.code"
                    :label="item.value"
                    :value="item.value"
                  />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="20">
            <el-col :span="8">
              <el-form-item label="计费周期单位" label-width="110px">
                <el-select v-model="formData.billingCycleUnit" placeholder="请选择" clearable>
                  <el-option label="周" :value="1" />
                  <el-option label="月" :value="2" />
                  <el-option label="季度" :value="3" />
                  <el-option label="年" :value="4" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="周期起始日期" label-width="110px">
                <el-input-number v-model="formData.billingCycleStartDate" :min="0" :max="31" controls-position="right" style="width: 100%" placeholder="偏移量，如：1" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="周期长度" label-width="90px">
                <el-input-number v-model="formData.billingCycleLength" :min="1" :max="12" controls-position="right" style="width: 100%" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="用途描述">
            <el-input
              v-model="formData.purposeDescription"
              type="textarea"
              :rows="2"
              placeholder="请输入用途描述信息"
            />
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <div class="footer-left" v-if="currentNodeType === 'meter'">
            <el-button type="warning" @click="openAutoReadingConfigDialog">
              自动抄表接口设置
            </el-button>
            <el-button type="info" :loading="testingAutoReading" @click="testAutoReadingInterface">
              自动抄表接口测试
            </el-button>
          </div>
          <div class="footer-right">
            <el-button @click="dialogVisible = false">取消</el-button>
            <el-button type="primary" @click="handleSubmit">确定</el-button>
          </div>
        </div>
      </template>
    </el-dialog>

    <!-- 数据来源系统设置弹窗 -->
    <DataSourceSystemDialog
      v-model:visible="showDataSourceSystemDialog"
      @select="handleDataSourceSystemSelect"
      @cancel="handleDataSourceSystemCancel"
    />

    <!-- 自动抄表接口设置弹窗 -->
    <el-dialog
      v-model="autoReadingConfigVisible"
      title="自动抄表接口设置"
      width="640px"
      :close-on-click-modal="false"
      append-to-body
    >
      <el-form :model="autoReadingConfig" label-width="120px">
        <el-form-item label="接口类型">
          <el-select v-model="autoReadingConfig.interfaceType" placeholder="请选择接口类型">
            <el-option label="HTTP API" value="HTTP_API" />
            <el-option label="Modbus TCP" value="MODBUS_TCP" />
            <el-option label="MQTT" value="MQTT" />
          </el-select>
        </el-form-item>
        <el-form-item label="接口地址">
          <el-input v-model="autoReadingConfig.apiUrl" placeholder="如 http://192.168.1.100 或 192.168.1.100" />
        </el-form-item>
        <el-form-item label="端口">
          <el-input-number v-model="autoReadingConfig.port" :min="1" :max="65535" controls-position="right" />
        </el-form-item>
        <el-form-item label="设备地址">
          <el-input v-model="autoReadingConfig.deviceAddress" placeholder="如采集点通讯地址/MQTT主题" />
        </el-form-item>
        <el-form-item label="采集频率(分钟)">
          <el-input-number v-model="autoReadingConfig.collectionInterval" :min="1" :max="1440" controls-position="right" />
        </el-form-item>
        <el-form-item label="认证方式">
          <el-select v-model="autoReadingConfig.authType" placeholder="请选择认证方式">
            <el-option label="无认证" value="NONE" />
            <el-option label="Basic 认证" value="BASIC" />
            <el-option label="Token 认证" value="TOKEN" />
          </el-select>
        </el-form-item>
        <el-form-item label="用户名" v-if="autoReadingConfig.authType === 'BASIC'">
          <el-input v-model="autoReadingConfig.username" placeholder="Basic 认证用户名" />
        </el-form-item>
        <el-form-item label="密码" v-if="autoReadingConfig.authType === 'BASIC'">
          <el-input v-model="autoReadingConfig.password" type="password" show-password placeholder="Basic 认证密码" />
        </el-form-item>
        <el-form-item label="Token" v-if="autoReadingConfig.authType === 'TOKEN'">
          <el-input v-model="autoReadingConfig.token" type="password" show-password placeholder="Token 认证令牌" />
        </el-form-item>
        <el-form-item label="数据解析路径">
          <el-input v-model="autoReadingConfig.dataPath" placeholder="如 data.items[0].value（JSON路径）" />
        </el-form-item>
        <el-form-item label="请求超时(秒)">
          <el-input-number v-model="autoReadingConfig.timeout" :min="1" :max="120" controls-position="right" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="autoReadingConfigVisible = false">取消</el-button>
        <el-button type="primary" @click="saveAutoReadingConfig">保存</el-button>
      </template>
    </el-dialog>

    <!-- 更改采集点级联关系弹窗 -->
    <el-dialog
      v-model="cascadeDialogVisible"
      title="更改采集点级联关系"
      width="500px"
    >
      <div class="dialog-tip">
        当前采集点：<strong>{{ selectedNode?.name }}</strong>
      </div>
      <div class="dialog-section-title">选择上级采集点（当前采集点的父级）：</div>
      <div class="radio-list">
        <el-radio
          :model-value="cascadeForm.parentMeterId"
          @update:model-value="val => cascadeForm.parentMeterId = val"
          :label="0"
        >
          无上级采集点
        </el-radio>
        <div v-for="meter in cascadeMeterList" :key="meter.id" class="radio-item">
          <el-radio
            :model-value="cascadeForm.parentMeterId"
            @update:model-value="val => cascadeForm.parentMeterId = val"
            :label="meter.id"
          >
            {{ meter.name }}
          </el-radio>
        </div>
        <el-empty v-if="cascadeMeterList.length === 0" description="该细分范围下暂无其他采集点" />
      </div>
      <template #footer>
        <el-button @click="cascadeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveCascade">确认</el-button>
      </template>
    </el-dialog>

    <!-- 更改采集点所属细分范围弹窗 -->
    <el-dialog
      v-model="subScopeDialogVisible"
      title="更改采集点所属细分范围"
      width="600px"
    >
      <div class="dialog-tip">
        当前采集点：<strong>{{ selectedNode?.name }}</strong>
      </div>
      <div class="dialog-section-title">选择所属细分范围：</div>
      <div class="radio-list">
        <div v-for="subScope in allSubScopesList" :key="subScope.id" class="radio-item">
          <el-radio
            :model-value="subScopeForm.subScopeId"
            @update:model-value="val => handleSubScopeChange(val)"
            :label="subScope.id"
          >
            {{ subScope.name }}
          </el-radio>
        </div>
        <el-empty v-if="allSubScopesList.length === 0" description="暂无细分范围" />
      </div>

      <div v-if="subScopeForm.subScopeId" class="meter-list-section">
        <div class="dialog-section-title">选择上级采集点（在新的细分范围下）：</div>
        <div class="radio-list">
          <el-radio
            :model-value="subScopeForm.parentMeterId"
            @update:model-value="val => subScopeForm.parentMeterId = val"
            :label="0"
          >
            无上级采集点
          </el-radio>
          <div v-for="meter in subScopeMeterList" :key="meter.id" class="radio-item">
            <el-radio
              :model-value="subScopeForm.parentMeterId"
              @update:model-value="val => subScopeForm.parentMeterId = val"
              :label="meter.id"
            >
              {{ meter.name }}
            </el-radio>
          </div>
          <el-empty v-if="subScopeMeterList.length === 0" description="该细分范围下暂无其他采集点" />
        </div>
      </div>

      <template #footer>
        <el-button @click="subScopeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveSubScope">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick, watch, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { FolderOpened, OfficeBuilding, Collection, Odometer, Plus, ArrowUp, ArrowDown, Delete } from '@element-plus/icons-vue'
import axios from 'axios'
import { generatePinyinCode } from '../utils/pinyinUtils'
import { unitApi } from '../api/auth'
import DataSourceSystemDialog from './DataSourceSystemDialog.vue'

const props = defineProps({
  currentUser: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['back'])

const API_BASE = '/api/purchased-heat-collection'

const treeData = ref([])
const selectedNode = ref(null)
const dialogVisible = ref(false)
const dialogTitle = ref('')
const currentNodeType = ref('')
const dialogMode = ref('add')
const editing = ref(false)
const heatUnits = ref([])
const formRef = ref(null)
const treeRef = ref(null)
const expandedKeys = ref([])
const savedExpandedKeys = ref([])
const isApplyingKeys = ref(false)

const formData = reactive({
  name: '',
  pinyinCode: '',
  description: '',
  sortOrder: 0,
  collectionScopeId: null,
  subScopeId: null,
  subScopeName: '',
  heatType: '',
  meterModel: '',
  meterReadingMethod: 1,
  energyUse: '',
  energyAllocation: '',
  parentMeterId: null,
  parentMeterName: '',
  purposeDescription: '',
  autoMeterReadingConfig: '',
  isCumulative: 0,
  isMobileSource: 0,
  measurementUnit: '',
  dataSourceSystem: '',
  billingCycleUnit: 2,
  billingCycleStartDate: 0,
  billingCycleLength: 1,
  energyCategoryL1: '',
  energyCategoryL2: '',
  energyCategoryL3: '',
  energyUseCategory: ''
})

const formRules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  collectionScopeId: [{ required: true, message: '请选择所属采集范围', trigger: 'change' }],
  subScopeId: [{ required: true, message: '请选择所属细分范围', trigger: 'change' }]
}

const heatTypeOptions = ref([])
const energyAllocationOptions = ref([])
const scopeOptions = ref([])
const subScopeOptions = ref([])
const parentMeterOptions = ref([])

const loadHeatTypeDict = async () => {
  try {
    const response = await axios.get('/api/data-dict/items/emission_subcategory')
    const items = response.data || []
    heatTypeOptions.value = items
      .filter(item => item.parentCode === 'PH')
      .map(item => ({ code: item.code, value: item.value }))
  } catch (error) {
    console.error('加载热力类型字典失败:', error)
    heatTypeOptions.value = [
      { code: 'PH_S', value: '蒸汽' },
      { code: 'PH_HW', value: '热水' }
    ]
  }
}

const loadEnergyAllocationDict = async () => {
  try {
    const response = await axios.get('/api/data-dict/items/energy_allocation')
    const items = response.data || []
    energyAllocationOptions.value = items.map(item => ({ code: item.code, value: item.value }))
  } catch (error) {
    console.error('加载能耗数据归属字典失败:', error)
    energyAllocationOptions.value = [
      { code: 'suring', value: '肃宁分公司' },
      { code: 'yuanping', value: '原平分公司' },
      { code: 'jilong', value: '机辆分公司' }
    ]
  }
}

// ==================== 能耗分类数据字典（三级级联） ====================
const energyCategoryList = ref([])
const energyCategoryL1Options = computed(() =>
  energyCategoryList.value.filter(c => c.level === 1)
)
const energyCategoryL2Options = computed(() => {
  const l1 = energyCategoryList.value.find(c => c.categoryCode === formData.energyCategoryL1)
  if (!l1) return []
  return energyCategoryList.value.filter(c => c.parentId === l1.id)
})
const energyCategoryL3Options = computed(() => {
  const l2 = energyCategoryList.value.find(c => c.categoryCode === formData.energyCategoryL2)
  if (!l2) return []
  return energyCategoryList.value.filter(c => c.parentId === l2.id)
})
const handleCategoryL1Change = () => {
  formData.energyCategoryL2 = ''
  formData.energyCategoryL3 = ''
}
const handleCategoryL2Change = () => {
  formData.energyCategoryL3 = ''
}
const loadEnergyCategories = async () => {
  try {
    const response = await axios.get('/api/energy-categories')
    energyCategoryList.value = response.data || []
  } catch (error) {
    console.error('加载能耗分类数据失败:', error)
    energyCategoryList.value = []
  }
}
const getCategoryName = (code) => {
  if (!code) return ''
  const cat = energyCategoryList.value.find(c => c.categoryCode === code)
  return cat ? cat.categoryName : code
}

// ==================== 能耗用途分类数据字典（含拼音筛选） ====================
const energyUseDictOptions = ref([])
const energyUseQuery = ref('')
const visibleEnergyUses = computed(() => {
  const q = energyUseQuery.value.trim().toLowerCase()
  if (!q) return energyUseDictOptions.value
  return energyUseDictOptions.value.filter(item => {
    const label = (item.value || '').toLowerCase()
    const pinyin = (item.pinyinCode || '').toLowerCase()
    return label.includes(q) || pinyin.includes(q)
  })
})
const filterEnergyUse = (query) => { energyUseQuery.value = query }
const handleEnergyUseVisibleChange = (visible) => {
  if (!visible) energyUseQuery.value = ''
}
const loadEnergyUseDict = async () => {
  try {
    const response = await axios.get('/api/data-dict/items/energy_use')
    const list = response.data || []
    energyUseDictOptions.value = list.map(item => ({
      ...item,
      pinyinCode: generatePinyinCode(item.value || '')
    }))
  } catch (error) {
    console.error('加载能耗用途分类字典失败:', error)
    energyUseDictOptions.value = []
  }
}

// ==================== 数据来源系统设置弹窗 ====================
const showDataSourceSystemDialog = ref(false)
const openDataSourceSystemDialog = () => { showDataSourceSystemDialog.value = true }
const handleDataSourceSystemSelect = (result) => {
  if (result.status === 'select' && result.systemName) {
    formData.dataSourceSystem = result.systemName
  }
}
const handleDataSourceSystemCancel = () => { /* 仅关闭 */ }

// ==================== 自动抄表接口设置弹窗 ====================
const autoReadingConfigVisible = ref(false)
const defaultAutoReadingConfig = () => ({
  interfaceType: 'HTTP_API',
  apiUrl: '',
  port: 80,
  deviceAddress: '',
  collectionInterval: 15,
  authType: 'NONE',
  username: '',
  password: '',
  token: '',
  dataPath: '',
  timeout: 30
})
const autoReadingConfig = reactive(defaultAutoReadingConfig())

const openAutoReadingConfigDialog = () => {
  const raw = formData.autoMeterReadingConfig
  if (raw) {
    try {
      const parsed = typeof raw === 'string' ? JSON.parse(raw) : raw
      Object.assign(autoReadingConfig, defaultAutoReadingConfig(), parsed)
    } catch (e) {
      console.warn('自动抄表接口配置解析失败，使用默认配置:', e)
      Object.assign(autoReadingConfig, defaultAutoReadingConfig())
    }
  } else {
    Object.assign(autoReadingConfig, defaultAutoReadingConfig())
  }
  autoReadingConfigVisible.value = true
}

const saveAutoReadingConfig = () => {
  formData.autoMeterReadingConfig = JSON.stringify(autoReadingConfig)
  autoReadingConfigVisible.value = false
  ElMessage.success('自动抄表接口配置已保存（点击"确定"提交后生效）')
}

const testingAutoReading = ref(false)
const testAutoReadingInterface = async () => {
  let config = null
  const raw = formData.autoMeterReadingConfig
  if (raw) {
    try {
      config = typeof raw === 'string' ? JSON.parse(raw) : raw
    } catch (e) {
      ElMessage.error('自动抄表接口配置格式错误，请重新设置')
      return
    }
  }
  if (!config) {
    ElMessage.warning('请先完成"自动抄表接口设置"并保存后再测试')
    return
  }
  if (!config.apiUrl) {
    ElMessage.error('接口地址不能为空')
    return
  }
  testingAutoReading.value = true
  try {
    const meterId = editing.value && selectedNode.value ? selectedNode.value.id : null
    const url = meterId
      ? `${API_BASE}/meter/${meterId}/test-reading`
      : `${API_BASE}/meter/test-reading`
    const resp = await axios.post(url, { config })
    if (resp.data && resp.data.success) {
      ElMessage.success(`测试成功：${resp.data.message || '接口可正常获取数据'}`)
    } else {
      ElMessage.error(`测试失败：${resp.data?.message || '接口返回异常'}`)
    }
  } catch (error) {
    const msg = error?.response?.data?.error || error?.message || '请求失败'
    ElMessage.error(`测试失败：${msg}`)
  } finally {
    testingAutoReading.value = false
  }
}

const loadScopeOptions = async () => {
  try {
    const response = await axios.get(`${API_BASE}/scopes`)
    scopeOptions.value = response.data || []
  } catch (error) {
    console.error('加载采集范围列表失败:', error)
    scopeOptions.value = []
  }
}

const loadSubScopeOptions = async () => {
  try {
    const response = await axios.get(`${API_BASE}/sub-scopes`)
    subScopeOptions.value = response.data || []
  } catch (error) {
    console.error('加载细分范围列表失败:', error)
    subScopeOptions.value = []
  }
}

const loadParentMeterOptions = async () => {
  try {
    const response = await axios.get(`${API_BASE}/meters`)
    parentMeterOptions.value = response.data || []
  } catch (error) {
    console.error('加载采集点列表失败:', error)
    parentMeterOptions.value = []
  }
}

watch(() => formData.name, (newName) => {
  if (newName) {
    formData.pinyinCode = generatePinyinCode(newName)
  } else {
    formData.pinyinCode = ''
  }
})

const loadTree = async (preserveExpanded = false) => {
  try {
    let keysToRestore = []
    if (preserveExpanded) {
      keysToRestore = getExpandedKeys()
    }

    const response = await axios.get(`${API_BASE}/tree`)
    treeData.value = [response.data]

    await nextTick()
    await nextTick()

    if (preserveExpanded && keysToRestore.length > 0) {
      isApplyingKeys.value = true
      expandedKeys.value = [...keysToRestore]
      await applyExpandedKeys()
      isApplyingKeys.value = false
    } else if (preserveExpanded) {
      isApplyingKeys.value = true
      expandedKeys.value = []
      await applyExpandedKeys()
      isApplyingKeys.value = false
    } else {
      const keys = collectRootAndScopeKeys(treeData.value)
      isApplyingKeys.value = true
      expandedKeys.value = [...keys]
      await applyExpandedKeys()
      isApplyingKeys.value = false
    }
  } catch (error) {
    console.error('加载树数据失败:', error)
    treeData.value = []
  }
}

const collectAllKeys = (nodes) => {
  const keys = new Set()
  const walk = (arr) => {
    for (const n of arr) {
      keys.add(n.id)
      if (n.children && n.children.length) walk(n.children)
    }
  }
  walk(nodes)
  return Array.from(keys)
}

const collectRootKeys = (nodes) => {
  const keys = []
  const walk = (arr) => {
    for (const n of arr) {
      keys.push(n.id)
      break
    }
  }
  walk(nodes)
  return keys
}

const collectRootAndScopeKeys = (nodes) => {
  const keys = []
  const walk = (arr, level) => {
    for (const n of arr) {
      keys.push(n.id)
      if (level === 0 && n.children && n.children.length) {
        for (const child of n.children) {
          keys.push(child.id)
        }
      }
    }
  }
  walk(nodes, 0)
  return keys
}

const collectKeysByLevel = (nodes) => {
  const levels = []
  const walk = (arr, level) => {
    if (!levels[level]) levels[level] = []
    for (const n of arr) {
      levels[level].push(n.id)
      if (n.children && n.children.length) {
        walk(n.children, level + 1)
      }
    }
  }
  walk(nodes, 0)
  return levels
}

const handleExpandChange = (data, expanded) => {
  if (isApplyingKeys.value) {
    return
  }

  const id = data.id
  if (expanded) {
    if (!expandedKeys.value.includes(id)) {
      expandedKeys.value = [...expandedKeys.value, id]
    }
  } else {
    expandedKeys.value = expandedKeys.value.filter(key => key !== id)
  }
}

const handleNodeClick = (data) => {
  selectedNode.value = data
}

const handleExpandAll = async () => {
  const allKeys = collectAllKeys(treeData.value)
  expandedKeys.value = [...allKeys]
  await nextTick()
}

const handleCollapseAll = async () => {
  expandedKeys.value = []
  await nextTick()
}

const resetForm = () => {
  formData.name = ''
  formData.pinyinCode = ''
  formData.description = ''
  formData.sortOrder = null
  formData.collectionScopeId = null
  formData.subScopeId = null
  formData.subScopeName = ''
  formData.heatType = ''
  formData.meterModel = ''
  formData.meterReadingMethod = 1
  formData.energyUse = ''
  formData.energyAllocation = ''
  formData.parentMeterId = null
  formData.parentMeterName = ''
  formData.purposeDescription = ''
  formData.autoMeterReadingConfig = ''
  formData.isCumulative = 0
  formData.isMobileSource = 0
  formData.measurementUnit = ''
  formData.dataSourceSystem = ''
  formData.billingCycleUnit = 2
  formData.billingCycleStartDate = 0
  formData.billingCycleLength = 1
  formData.energyCategoryL1 = ''
  formData.energyCategoryL2 = ''
  formData.energyCategoryL3 = ''
  formData.energyUseCategory = ''
  formRef.value?.resetFields()
}

const handleAddScope = () => {
  currentNodeType.value = 'scope'
  dialogTitle.value = '添加采集范围'
  editing.value = false
  resetForm()
  loadScopeOptions()
  dialogVisible.value = true
}

const handleAddSubScope = (node) => {
  currentNodeType.value = 'sub_scope'
  dialogTitle.value = '添加细分范围'
  editing.value = false
  resetForm()
  formData.collectionScopeId = node.id
  loadScopeOptions()
  dialogVisible.value = true
}

const handleAddMeter = (node) => {
  currentNodeType.value = 'meter'
  dialogTitle.value = '添加采集点'
  dialogMode.value = 'add'
  editing.value = false
  resetForm()
  // 新增采集点时缺省设置为：综合用能-公共服务-综合供暖系统
  formData.energyCategoryL1 = 'EC03'
  formData.energyCategoryL2 = 'EC0303'
  formData.energyCategoryL3 = 'EC030303'
  if (node.nodeType === 'sub_scope') {
    formData.subScopeId = node.id
    formData.subScopeName = node.name
    formData.parentMeterId = null
    formData.parentMeterName = ''
  } else if (node.nodeType === 'meter') {
    formData.subScopeId = node.subScopeId
    formData.subScopeName = node.subScopeName || ''
    formData.parentMeterId = node.id
    formData.parentMeterName = node.name
  }
  dialogVisible.value = true
}

const handleEdit = (node) => {
  currentNodeType.value = node.nodeType
  dialogTitle.value = '编辑' + getNodeTypeLabel(node.nodeType)
  dialogMode.value = 'edit'
  editing.value = true
  Object.assign(formData, {
    name: node.name,
    pinyinCode: node.pinyinCode || '',
    description: node.description || '',
    sortOrder: node.sortOrder ?? 0,
    collectionScopeId: node.collectionScopeId || null,
    subScopeId: node.subScopeId || null,
    subScopeName: node.subScopeName || '',
    heatType: node.heatType || '',
    meterModel: node.meterModel || '',
    meterReadingMethod: node.meterReadingMethod ?? 1,
    energyUse: node.energyUse || '',
    energyAllocation: node.energyAllocation || '',
    parentMeterId: node.nodeType === 'meter' ? (node.parentMeterId || null) : null,
    parentMeterName: node.nodeType === 'meter' ? (node.parentMeterName || '') : '',
    purposeDescription: node.purposeDescription || '',
    autoMeterReadingConfig: node.autoMeterReadingConfig || '',
    isCumulative: node.isCumulative ?? 0,
    isMobileSource: node.isMobileSource ?? 0,
    measurementUnit: node.measurementUnit || '',
    dataSourceSystem: node.dataSourceSystem || '',
    billingCycleUnit: node.billingCycleUnit ?? 2,
    billingCycleStartDate: node.billingCycleStartDate ?? 0,
    billingCycleLength: node.billingCycleLength || 1,
    energyCategoryL1: node.energyCategoryL1 || '',
    energyCategoryL2: node.energyCategoryL2 || '',
    energyCategoryL3: node.energyCategoryL3 || '',
    energyUseCategory: node.energyUseCategory || ''
  })
  dialogVisible.value = true
}

const getNodeTypeLabel = (type) => {
  const labels = { scope: '采集范围', sub_scope: '细分范围', meter: '采集点' }
  return labels[type] || ''
}

const getNodeApiPath = (nodeType) => {
  const map = { scope: 'scopes', sub_scope: 'sub-scopes', meter: 'meters' }
  return map[nodeType] || nodeType
}

const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    const userId = props.currentUser?.userId || 1
    const { sortOrder: _, ...payload } = formData

    if (editing.value) {
      await axios.put(`${API_BASE}/${getNodeApiPath(currentNodeType.value)}/${selectedNode.value.id}`, {
        ...payload,
        createdBy: userId
      })
      ElMessage.success('更新成功')
    } else {
      if (currentNodeType.value === 'scope') {
        await axios.post(`${API_BASE}/scopes`, { ...payload, createdBy: userId })
      } else if (currentNodeType.value === 'sub_scope') {
        await axios.post(`${API_BASE}/sub-scopes`, { ...payload, createdBy: userId })
      } else if (currentNodeType.value === 'meter') {
        await axios.post(`${API_BASE}/meters`, { ...payload, createdBy: userId })
      }
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadTree(true)
  } catch (error) {
    if (error.response?.data?.error) {
      ElMessage.error(error.response.data.error)
    } else {
      ElMessage.error('操作失败')
    }
  }
}

const handleDelete = async (node) => {
  if (node.status === 1) {
    ElMessage.warning('该节点处于启用状态，请先停用后再删除')
    return
  }
  try {
    await ElMessageBox.confirm(`确定删除"${node.name}"及其所有子节点？`, '删除确认', {
      type: 'warning'
    })
    const userId = props.currentUser?.userId || 1
    await axios.delete(`${API_BASE}/${getNodeApiPath(node.nodeType)}/${node.id}`, {
      headers: { 'X-User-Id': userId }
    })
    ElMessage.success('删除成功')
    selectedNode.value = null
    loadTree(true)
  } catch (error) {
    if (error.response?.data?.error) {
      ElMessage.error(error.response.data.error)
    } else if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const handleToggleStatus = async (node) => {
  try {
    const userId = props.currentUser?.userId || 1
    await axios.put(
      `${API_BASE}/${node.id}/status`,
      { nodeType: node.nodeType, status: node.status === 1 ? 0 : 1 },
      {
        headers: { 'X-User-Id': userId }
      }
    )
    ElMessage.success('状态更新成功')
    loadTree(true)
    if (selectedNode.value?.id === node.id) {
      selectedNode.value = null
    }
  } catch (error) {
    if (error.response?.data?.error) {
      ElMessage.error(error.response.data.error)
    } else {
      ElMessage.error('操作失败')
    }
  }
}

const getTreeSnapshot = () => JSON.parse(JSON.stringify(treeData.value))

const getExpandedKeys = () => {
  const tree = treeRef.value
  if (!tree) return [...expandedKeys.value]

  const store = tree.store
  const keys = []
  const walk = (node) => {
    if (node.expanded) {
      keys.push(node.data.id)
    }
    if (node.childNodes) {
      node.childNodes.forEach(walk)
    }
  }
  walk(store.root)
  return keys
}

const moveNodeInTreeData = (nodeId, direction) => {
  const moveInNodes = (nodes) => {
    for (let i = 0; i < nodes.length; i++) {
      if (nodes[i].id === nodeId) {
        const newIndex = i + direction
        if (newIndex >= 0 && newIndex < nodes.length) {
          const temp = nodes[i]
          nodes.splice(i, 1)
          nodes.splice(newIndex, 0, temp)
          return true
        }
        return false
      }
      if (nodes[i].children && moveInNodes(nodes[i].children)) {
        return true
      }
    }
    return false
  }
  return moveInNodes(treeData.value)
}

const refreshTree = async () => {
  treeData.value = [...treeData.value]
  await nextTick()
  await nextTick()
  await applyExpandedKeys()
}

const handleMoveUp = async (node) => {
  const snapshot = getTreeSnapshot()
  const savedKeys = getExpandedKeys()

  const moved = moveNodeInTreeData(node.id, -1)
  if (!moved) {
    ElMessage.warning('无法上移，已经是第一个节点')
    return
  }

  try {
    treeData.value = [...treeData.value]

    isApplyingKeys.value = true
    expandedKeys.value = [...savedKeys]

    await nextTick()
    await nextTick()

    await applyExpandedKeys()
    isApplyingKeys.value = false

    const userId = props.currentUser?.userId || 1
    await axios.post(`${API_BASE}/${node.nodeType}/${node.id}/move-up`, {}, {
      headers: { 'X-User-Id': userId }
    })
  } catch (error) {
    treeData.value = snapshot
    isApplyingKeys.value = true
    expandedKeys.value = [...savedKeys]
    await nextTick()
    await nextTick()
    await applyExpandedKeys()
    isApplyingKeys.value = false
    ElMessage.error(error.response?.data?.error || '上移失败')
  }
}

const handleMoveDown = async (node) => {
  const snapshot = getTreeSnapshot()
  const savedKeys = getExpandedKeys()

  const moved = moveNodeInTreeData(node.id, 1)
  if (!moved) {
    ElMessage.warning('无法下移，已经是最后一个节点')
    return
  }

  try {
    treeData.value = [...treeData.value]

    isApplyingKeys.value = true
    expandedKeys.value = [...savedKeys]

    await nextTick()
    await nextTick()

    await applyExpandedKeys()
    isApplyingKeys.value = false

    const userId = props.currentUser?.userId || 1
    await axios.post(`${API_BASE}/${node.nodeType}/${node.id}/move-down`, {}, {
      headers: { 'X-User-Id': userId }
    })
  } catch (error) {
    treeData.value = snapshot
    isApplyingKeys.value = true
    expandedKeys.value = [...savedKeys]
    await nextTick()
    await nextTick()
    await applyExpandedKeys()
    isApplyingKeys.value = false
    ElMessage.error(error.response?.data?.error || '下移失败')
  }
}

const applyExpandedKeysDirect = (keys) => {
  if (!treeRef.value || !treeRef.value.store) {
    return false
  }

  const { nodesMap } = treeRef.value.store

  if (Object.keys(nodesMap).length === 0) {
    return false
  }

  Object.values(nodesMap).forEach(node => {
    node.expanded = false
  })

  let allFound = true
  for (const key of keys) {
    const node = nodesMap[key]
    if (node) {
      node.expanded = true
    } else {
      allFound = false
    }
  }
  return allFound
}

const applyExpandedKeys = async () => {
  for (let attempt = 0; attempt < 10; attempt++) {
    if (!treeRef.value || !treeRef.value.store) {
      await new Promise(resolve => setTimeout(resolve, 30))
      continue
    }

    const { nodesMap } = treeRef.value.store

    if (Object.keys(nodesMap).length === 0) {
      await new Promise(resolve => setTimeout(resolve, 30))
      continue
    }

    const missingKeys = expandedKeys.value.filter(key => !nodesMap[key])
    if (missingKeys.length > 0) {
      await new Promise(resolve => setTimeout(resolve, 30))
      continue
    }

    Object.values(nodesMap).forEach(node => {
      node.expanded = false
    })

    for (const key of expandedKeys.value) {
      const node = nodesMap[key]
      if (node) {
        node.expanded = true
      }
    }

    break
  }
}

watch(expandedKeys, () => {
  if (isApplyingKeys.value) {
    return
  }
  applyExpandedKeys()
}, { deep: true })

// ==================== 更改采集点级联关系 ====================

const cascadeDialogVisible = ref(false)
const cascadeForm = reactive({
  parentMeterId: 0
})
const cascadeMeterList = ref([])

const openCascadeDialog = async () => {
  if (!selectedNode.value) return
  const fullNode = findNodeInTree(treeData.value, selectedNode.value.id)
  if (!fullNode) {
    ElMessage.error('获取节点信息失败')
    return
  }
  cascadeForm.parentMeterId = fullNode.parentMeterId || 0
  try {
    const response = await axios.get(`${API_BASE}/meters-by-sub-scope`, {
      params: {
        subScopeId: fullNode.subScopeId,
        excludeMeterId: fullNode.id
      }
    })
    cascadeMeterList.value = response.data || []
  } catch (error) {
    console.error('加载采集点列表失败:', error)
    cascadeMeterList.value = []
  }
  cascadeDialogVisible.value = true
}

const saveCascade = async () => {
  try {
    const userId = props.currentUser?.userId || 1
    await axios.put(`${API_BASE}/meters/${selectedNode.value.id}/cascade`, {
      parentMeterId: cascadeForm.parentMeterId,
      userId: userId
    })
    ElMessage.success('更改级联关系成功')
    cascadeDialogVisible.value = false
    loadTree(true)
    selectedNode.value = null
  } catch (error) {
    if (error.response?.data?.error) {
      ElMessage.error(error.response.data.error)
    } else {
      ElMessage.error('操作失败')
    }
  }
}

// ==================== 更改采集点所属细分范围 ====================

const subScopeDialogVisible = ref(false)
const subScopeForm = reactive({
  subScopeId: 0,
  parentMeterId: 0
})
const allSubScopesList = ref([])
const subScopeMeterList = ref([])

const openSubScopeDialog = async () => {
  if (!selectedNode.value) return
  const fullNode = findNodeInTree(treeData.value, selectedNode.value.id)
  if (!fullNode) {
    ElMessage.error('获取节点信息失败')
    return
  }
  subScopeForm.subScopeId = fullNode.subScopeId
  subScopeForm.parentMeterId = fullNode.parentMeterId || 0

  const collectionScopeId = fullNode.collectionScopeId
  if (collectionScopeId) {
    try {
      const response = await axios.get(`${API_BASE}/scopes/${collectionScopeId}/sub-scopes`)
      allSubScopesList.value = response.data.map(subScope => ({
        id: subScope.id,
        name: subScope.name
      })) || []
    } catch (error) {
      console.error('加载细分范围列表失败:', error)
      allSubScopesList.value = []
    }
  } else {
    allSubScopesList.value = []
  }

  await loadSubScopeMeterList(fullNode.id)
  subScopeDialogVisible.value = true
}

const loadSubScopeMeterList = async (excludeMeterId) => {
  if (!subScopeForm.subScopeId) {
    subScopeMeterList.value = []
    return
  }
  try {
    const response = await axios.get(`${API_BASE}/meters-by-sub-scope`, {
      params: {
        subScopeId: subScopeForm.subScopeId,
        excludeMeterId: excludeMeterId || selectedNode.value?.id
      }
    })
    subScopeMeterList.value = response.data || []
  } catch (error) {
    console.error('加载采集点列表失败:', error)
    subScopeMeterList.value = []
  }
}

const handleSubScopeChange = async (newSubScopeId) => {
  subScopeForm.subScopeId = newSubScopeId
  subScopeForm.parentMeterId = 0
  await loadSubScopeMeterList(selectedNode.value?.id)
}

const saveSubScope = async () => {
  try {
    const userId = props.currentUser?.userId || 1
    await axios.put(`${API_BASE}/meters/${selectedNode.value.id}/sub-scope`, {
      subScopeId: subScopeForm.subScopeId,
      parentMeterId: subScopeForm.parentMeterId,
      userId: userId
    })
    ElMessage.success('更改所属细分范围成功')
    subScopeDialogVisible.value = false
    loadTree(true)
    selectedNode.value = null
  } catch (error) {
    if (error.response?.data?.error) {
      ElMessage.error(error.response.data.error)
    } else {
      ElMessage.error('操作失败')
    }
  }
}

// ==================== 辅助函数 ====================

const findNodeInTree = (nodes, id) => {
  for (const node of nodes) {
    if (node.id === id) {
      return node
    }
    if (node.children && node.children.length > 0) {
      const found = findNodeInTree(node.children, id)
      if (found) {
        return found
      }
    }
  }
  return null
}

onMounted(async () => {
  await nextTick()
  await Promise.all([
    loadHeatTypeDict(),
    loadEnergyAllocationDict(),
    loadEnergyCategories(),
    loadEnergyUseDict(),
    loadTree(),
    (async () => {
      try {
        // 外购热能涵盖：能量(GJ/MJ/kJ)、质量(t/kg)、面积(m²)
        const [heat, mass, area] = await Promise.all([
          unitApi.listByCategory('HEAT'),
          unitApi.listByCategory('SOLID_FUEL'),
          unitApi.listByCategory('AREA')
        ])
        const map = new Map()
        for (const u of [...(heat.data || []), ...(mass.data || []), ...(area.data || [])]) {
          if (!map.has(u.unitCode)) map.set(u.unitCode, u)
        }
        heatUnits.value = Array.from(map.values())
      } catch (err) {
        console.error('加载外购热能标准单位失败', err)
      }
    })()
  ])
})
</script>

<style scoped>
.purchased-heat-collection-settings {
  display: flex;
  width: 100%;
  height: 100%;
  background-color: #f5f7fa;
  overflow: hidden;
}

.tree-panel {
  width: 420px;
  flex-shrink: 0;
  background: white;
  border-right: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  height: 100%;
}

.tree-content {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
  min-height: 0;
}

.tree-node {
  display: flex;
  align-items: center;
  width: 100%;
  font-size: 13px;
  padding: 2px 0;
}

.tree-node.disabled-node {
  color: #c0c4cc;
}

.tree-node.disabled-node .node-label,
.tree-node.disabled-node .node-icon {
  color: #c0c4cc;
}

.node-left {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  flex: 1;
  min-width: 0;
  overflow: hidden;
}

.node-icon {
  font-size: 14px;
  flex-shrink: 0;
}

.node-label {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex-shrink: 1;
  min-width: 0;
}

.status-tag {
  flex-shrink: 0;
}

.node-actions {
  display: none;
  gap: 0;
  flex-shrink: 0;
  margin-left: auto;
  padding-right: 4px;
  --el-button-margin-right: 2px;
}

.node-actions .el-button {
  margin-left: 0;
  margin-right: 2px;
}

.node-actions .el-button:last-child {
  margin-right: 0;
}

.tree-node:hover .node-actions {
  display: inline-flex;
}

.tree-node.root-node .node-actions {
  display: inline-flex;
}

.detail-panel {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  min-width: 0;
  height: 100%;
}

.detail-content {
  background: white;
  border-radius: 8px;
  padding: 20px;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #ebeef5;
}

.detail-header h3 {
  margin: 0;
  font-size: 18px;
}

.dialog-tip {
  padding: 10px 12px;
  background: #ecf5ff;
  border-radius: 4px;
  margin-bottom: 12px;
  color: #409eff;
}

.dialog-section-title {
  font-weight: 600;
  margin: 12px 0 8px;
  color: #303133;
}

.radio-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  max-height: 300px;
  overflow-y: auto;
  padding: 8px;
  background: #f5f7fa;
  border-radius: 4px;
}

.radio-item {
  padding: 4px 0;
}

.item-tag {
  margin-left: 8px;
  font-size: 12px;
  color: #909399;
}

.meter-list-section {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #ebeef5;
}

/* 数据来源系统输入框与按钮组合布局 */
.input-with-button {
  display: flex;
  gap: 8px;
  width: 100%;
}

.input-with-button .el-input {
  flex: 1;
}

.input-with-button .el-button {
  flex-shrink: 0;
}

/* 编辑弹窗底部按钮区域：左侧操作按钮 + 右侧确定/取消 */
.dialog-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.dialog-footer .footer-left {
  display: flex;
  gap: 8px;
}

.dialog-footer .footer-right {
  display: flex;
  gap: 8px;
}
</style>
