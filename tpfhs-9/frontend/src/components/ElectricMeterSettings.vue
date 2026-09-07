<!--
  电力表设置组件
  功能描述：
    1. 展示站点/区间-集中器-电表的树状结构
    2. 支持节点的增删改查、启停、移动操作
    3. 左侧展示树结构，右侧展示选中节点详情
    
  组件属性：
    - currentUser (Object): 当前登录用户信息
  
  组件事件：
    - back: 返回事件
-->
<template>
  <div class="electric-meter-settings">
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
            <div class="tree-node" :class="{ 'disabled-node': data.status === 0, 'root-node': data.nodeType === 'root', 'virtual-meter-node': data.nodeType === 'meter' && data.isVirtualMeter === 1, 'allocation-child-node': data.nodeType === 'meter' && data.isAllocationChild === 1 }">
              <span class="node-left">
                <span class="node-icon">
                  <el-icon v-if="data.nodeType === 'root'"><FolderOpened /></el-icon>
                  <el-icon v-else-if="data.nodeType === 'station'"><OfficeBuilding /></el-icon>
                  <el-icon v-else-if="data.nodeType === 'concentrator'"><Collection /></el-icon>
                  <el-icon v-else-if="data.nodeType === 'meter'"><Odometer /></el-icon>
                </span>
                <span class="node-label" :title="data.name">
                  <span v-if="data.nodeType === 'meter' && data.isVirtualMeter === 1" class="virtual-meter-tag" title="虚拟电表">[虚]</span><span v-if="data.nodeType === 'meter' && data.isAllocationChild === 1" class="allocation-child-tag" title="分摊子电表">[分摊]</span>{{ data.name }}
                </span>
                <el-tag 
                  v-if="data.status === 0" 
                  size="small" 
                  type="info"
                  class="status-tag"
                >停用</el-tag>
              </span>
              
              <!-- 根节点操作 -->
              <span v-if="data.nodeType === 'root'" class="node-actions">
                <el-button size="small" type="primary" @click="handleAddStation">
                  <el-icon><Plus /></el-icon>
                </el-button>
                <el-button size="small" type="primary" @click="handleExpandAll">展开全部</el-button>
                <el-button size="small" type="danger" @click="handleCollapseAll">折叠全部</el-button>
              </span>
              
              <!-- 站点/区间节点操作 -->
              <span v-else-if="data.nodeType === 'station'" class="node-actions">
                <el-button size="small" link type="primary" title="添加下属集中器" @click.stop="handleAddConcentrator(data)">
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
              
              <!-- 集中器节点操作 -->
              <span v-else-if="data.nodeType === 'concentrator'" class="node-actions">
                <el-button size="small" link type="primary" title="添加下属电表" @click.stop="handleAddMeter(data)">
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
              
              <!-- 电表节点操作 -->
              <span v-else-if="data.nodeType === 'meter'" class="node-actions">
                <el-button v-if="data.isAllocationChild !== 1 && data.isVirtualMeter !== 1" size="small" link type="primary" title="添加下属电表" @click.stop="handleAddMeter(data)">
                  <el-icon><Plus /></el-icon>
                </el-button>
                <el-button v-if="data.isAllocationChild !== 1" size="small" link type="warning" title="创建/修改虚拟分摊子电表" @click.stop="handleOpenVirtualAlloc(data)">
                  <el-icon><Grid /></el-icon>
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
        <el-empty v-if="!treeData?.length || !treeData[0]?.children?.length" description="暂无数据，请添加站点/区间" />
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
          <template v-if="selectedNode.nodeType === 'station'">
            <el-descriptions-item label="名称">{{ selectedNode.name }}</el-descriptions-item>
            <el-descriptions-item label="拼音编码">{{ selectedNode.pinyinCode || '-' }}</el-descriptions-item>
            <el-descriptions-item label="描述">{{ selectedNode.description || '-' }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="selectedNode.status === 1 ? 'success' : 'info'">
                {{ selectedNode.statusText }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ selectedNode.createdAt || '-' }}</el-descriptions-item>
            <el-descriptions-item label="更新时间">{{ selectedNode.updatedAt || '-' }}</el-descriptions-item>
          </template>
          
          <template v-else-if="selectedNode.nodeType === 'concentrator'">
            <el-descriptions-item label="名称">{{ selectedNode.name }}</el-descriptions-item>
            <el-descriptions-item label="拼音编码">{{ selectedNode.pinyinCode || '-' }}</el-descriptions-item>
            <el-descriptions-item label="变压器容量">{{ selectedNode.transformerCapacity || '-' }}</el-descriptions-item>
            <el-descriptions-item label="集中器地址">{{ selectedNode.concentratorAddress || '-' }}</el-descriptions-item>
            <el-descriptions-item label="描述">{{ selectedNode.description || '-' }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="selectedNode.status === 1 ? 'success' : 'info'">
                {{ selectedNode.statusText }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ selectedNode.createdAt || '-' }}</el-descriptions-item>
            <el-descriptions-item label="更新时间">{{ selectedNode.updatedAt || '-' }}</el-descriptions-item>
          </template>
          
          <template v-else-if="selectedNode.nodeType === 'meter'">
            <el-descriptions-item label="名称">{{ selectedNode.name }}</el-descriptions-item>
            <el-descriptions-item label="拼音编码">{{ selectedNode.pinyinCode || '-' }}</el-descriptions-item>
            <el-descriptions-item label="电表类型">{{ selectedNode.meterType || '-' }}</el-descriptions-item>
            <el-descriptions-item label="电表型号">{{ selectedNode.meterModel || '-' }}</el-descriptions-item>
            <el-descriptions-item label="电表地址">{{ selectedNode.meterAddress || '-' }}</el-descriptions-item>
            <el-descriptions-item label="抄表方式">
              {{ selectedNode.meterReadingMethod === 1 ? '自动抄表' : '人工抄表' }}
            </el-descriptions-item>
            <el-descriptions-item label="总表与分表关系">
              {{ selectedNode.parentChildRelationship === 2 ? '总表计数不等于各下属分表计数之和' : '总表计数等于各下属分表计数之和' }}
            </el-descriptions-item>
            <el-descriptions-item label="是否虚拟电表">
              {{ selectedNode.isVirtualMeter === 1 ? '是' : '否' }}
            </el-descriptions-item>
            <el-descriptions-item label="是否分摊虚拟子电表">
              {{ selectedNode.isAllocationChild === 1 ? '是' : '否' }}
            </el-descriptions-item>
            <el-descriptions-item label="分摊比例">
              {{ selectedNode.allocationRatio ?? 1.0 }}
            </el-descriptions-item>
            <el-descriptions-item label="用电分类">{{ selectedNode.powerCategory || '-' }}</el-descriptions-item>
            <el-descriptions-item label="能耗数据划拨">{{ selectedNode.energyAllocation || '-' }}</el-descriptions-item>
            <el-descriptions-item label="是否累加量">
              {{ selectedNode.isCumulative === 1 ? '是' : '否' }}
            </el-descriptions-item>
            <el-descriptions-item label="是否移动源">
              {{ selectedNode.isMobileSource === 1 ? '是' : '否' }}
            </el-descriptions-item>
            <el-descriptions-item label="计量单位">{{ selectedNode.measurementUnit || '-' }}</el-descriptions-item>
            <el-descriptions-item label="数据来源系统">{{ selectedNode.dataSourceSystem || '-' }}</el-descriptions-item>
            <el-descriptions-item label="电源类型">{{ selectedNode.emissionSubcategory || '-' }}</el-descriptions-item>
            <el-descriptions-item label="计费周期单位">{{ { 1: '周', 2: '月', 3: '季度', 4: '年' }[selectedNode.billingCycleUnit] || '-' }}</el-descriptions-item>
            <el-descriptions-item label="计费周期起始偏移">{{ selectedNode.billingCycleStartDate != null ? selectedNode.billingCycleStartDate : '-' }}</el-descriptions-item>
            <el-descriptions-item label="计费周期长度">{{ selectedNode.billingCycleLength || '-' }}</el-descriptions-item>
            <el-descriptions-item label="能耗场景大类">{{ getCategoryName(selectedNode.energyCategoryL1) }}</el-descriptions-item>
            <el-descriptions-item label="能耗二级分类">{{ getCategoryName(selectedNode.energyCategoryL2) }}</el-descriptions-item>
            <el-descriptions-item label="能耗三级分类">{{ getCategoryName(selectedNode.energyCategoryL3) }}</el-descriptions-item>
            <el-descriptions-item label="能耗用途分类">{{ selectedNode.energyUseCategory || '-' }}</el-descriptions-item>
            <el-descriptions-item label="描述" :span="2">{{ selectedNode.description || '-' }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="selectedNode.status === 1 ? 'success' : 'info'">
                {{ selectedNode.statusText }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ selectedNode.createdAt || '-' }}</el-descriptions-item>
            <el-descriptions-item label="更新时间">{{ selectedNode.updatedAt || '-' }}</el-descriptions-item>
          </template>
        </el-descriptions>
        
        <!-- 电表专属操作按钮（虚拟分摊子电表不能更改级联关系和从属集中器） -->
        <div v-if="selectedNode.nodeType === 'meter' && selectedNode.isAllocationChild !== 1 && selectedNode.isVirtualMeter !== 1" class="meter-detail-actions">
          <el-button type="warning" @click="openCascadeDialog">更改电表级联关系</el-button>
          <el-button type="success" @click="openPointDialog">更改电表从属数据集中器</el-button>
        </div>
      </div>
      <el-empty v-else description="请从左侧选择一个节点查看详情" />
    </div>
    
    <!-- 新增/编辑对话框 -->
    <el-dialog 
      v-model="dialogVisible" 
      :title="dialogTitle" 
      width="800px"
      @close="resetForm"
    >
      <el-form :model="formData" :rules="formRules" ref="formRef" label-width="120px">
        <el-form-item label="名称" prop="name" v-if="currentNodeType !== 'meter'">
          <el-input v-model="formData.name" placeholder="请输入名称" />
        </el-form-item>
        <el-form-item label="拼音编码" v-if="currentNodeType !== 'meter' || !editing">
          <el-input v-model="formData.pinyinCode" placeholder="由系统自动生成" disabled />
        </el-form-item>
        
        <!-- 集中器字段 -->
        <template v-if="currentNodeType === 'concentrator'">
          <el-form-item label="变压器容量">
            <el-input v-model="formData.transformerCapacity" placeholder="请输入变压器容量" />
          </el-form-item>
          <el-form-item label="集中器地址" prop="concentratorAddress">
            <el-tooltip content="输入集中器远程抄表的地址编码" placement="top" effect="dark">
              <el-input 
                v-model="formData.concentratorAddress" 
                placeholder="请输入集中器地址（仅支持数字0-9）"
                @input="handleConcentratorAddressInput"
              />
            </el-tooltip>
            <div class="form-item-tip">输入集中器远程抄表的地址编码</div>
          </el-form-item>
        </template>
        
        <!-- 电表字段 -->
        <template v-if="currentNodeType === 'meter'">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="名称" prop="name">
                <el-input v-model="formData.name" placeholder="请输入名称" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="电表类型">
                <el-select v-model="formData.meterType" placeholder="请选择电表类型">
                  <el-option 
                    v-for="item in meterTypeOptions" 
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
              <el-form-item label="能耗场景大类">
                <el-select
                  v-model="formData.energyCategoryL1"
                  placeholder="请选择一级分类"
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
            <el-col :span="12">
              <el-form-item label="电表型号">
                <el-input 
                  v-model="formData.meterModel" 
                  placeholder="请点击选择电表型号"
                  readonly
                  class="meter-model-input"
                  @click="openMeterModelSelector"
                >
                  <template #suffix>
                    <el-icon class="select-icon" @click="openMeterModelSelector">
                      <Plus />
                    </el-icon>
                  </template>
                </el-input>
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="20">
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
            <el-col :span="12">
              <el-form-item label="电表地址" prop="meterAddress">
                <el-input 
                  v-model="formData.meterAddress" 
                  placeholder="仅支持数字0-9"
                  @input="handleMeterAddressInput"
                />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="20">
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
            <el-col :span="12">
              <el-form-item label="能耗数据划拨">
                <el-select v-model="formData.energyAllocation" placeholder="请选择划拨方式">
                  <el-option
                    v-for="item in energyAllocationOptions"
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
              <el-form-item label="抄表方式" label-width="100px">
                <el-radio-group v-model="formData.meterReadingMethod">
                  <el-radio :value="1">自动</el-radio>
                  <el-radio :value="0">人工</el-radio>
                </el-radio-group>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="总表与分表关系" label-width="130px">
                <el-radio-group v-model="formData.parentChildRelationship">
                  <el-radio :value="1">计数相等</el-radio>
                  <el-radio :value="2">计数不相等</el-radio>
                </el-radio-group>
              </el-form-item>
            </el-col>
            <!-- 是否虚拟电表（isVirtualMeter）隐藏不显示，由系统在添加下级电表时自动设置 -->
          </el-row>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="是否分摊虚拟子电表" label-width="150px">
                <el-radio-group v-model="formData.isAllocationChild" disabled>
                  <el-radio :value="1">是</el-radio>
                  <el-radio :value="0">否</el-radio>
                </el-radio-group>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="分摊比例" label-width="90px">
                <div style="display: flex; align-items: center; gap: 8px; width: 100%;">
                  <el-input-number v-model="formData.allocationRatio" :min="0" :step="0.1" :precision="6" :controls="true" disabled style="flex: 1;" />
                  <el-button type="primary" size="small" :disabled="formData.isAllocationChild !== 1" @click="handleEditAllocation">修改分摊比例</el-button>
                </div>
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
                  <el-option v-for="u in electricUnits" :key="u.unitCode" :label="u.unitName" :value="u.unitCode" />
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
                  <el-button
                    type="primary"
                    @click="openDataSourceSystemDialog"
                  >
                    <el-icon><Plus /></el-icon>
                  </el-button>
                </div>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="电源类型">
                <el-select v-model="formData.emissionSubcategory" placeholder="请选择电源类型">
                  <el-option label="外购电力" value="外购电力" />
                  <el-option label="新能源发电（自发自用）" value="新能源发电（自发自用）" />
                </el-select>
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
        </template>
        
        <el-form-item label="描述">
          <el-tooltip content="输入安装地址、用途等描述信息" placement="top" effect="dark">
            <el-input 
              v-model="formData.description" 
              type="textarea" 
              :rows="3" 
              placeholder="输入安装地址、用途等描述信息" 
            />
          </el-tooltip>
          <div class="form-item-tip">输入安装地址、用途等描述信息</div>
        </el-form-item>
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
          <el-input v-model="autoReadingConfig.deviceAddress" placeholder="如电表通讯地址/MQTT主题" />
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

    <!-- 电表型号选择器 -->
    <MeterModelSelector 
      ref="meterModelSelectorRef"
      @select="handleMeterModelSelect"
      @cancel="handleMeterModelCancel"
    />

    <!-- 数据来源系统设置弹窗 -->
    <DataSourceSystemDialog
      v-model:visible="showDataSourceSystemDialog"
      @select="handleDataSourceSystemSelect"
      @cancel="handleDataSourceSystemCancel"
    />
    
    <!-- 更改电表级联关系弹窗 -->
    <el-dialog 
      v-model="cascadeDialogVisible" 
      title="更改电表级联关系" 
      width="500px"
    >
      <div class="dialog-tip">
        当前电表：<strong>{{ selectedNode?.name }}</strong>
      </div>
      <div class="dialog-section-title">选择上级电表（当前电表的父级）：</div>
      <div class="radio-list">
        <el-radio 
          :model-value="cascadeForm.parentMeterId" 
          @update:model-value="val => cascadeForm.parentMeterId = val"
          :label="0"
        >
          无上级电表
        </el-radio>
        <div v-for="meter in cascadeMeterList" :key="meter.id" class="radio-item">
          <el-radio 
            :model-value="cascadeForm.parentMeterId" 
            @update:model-value="val => cascadeForm.parentMeterId = val"
            :label="meter.id"
          >
            {{ meter.name }}
            <span v-if="meter.meterType" class="item-tag">{{ meter.meterType }}</span>
          </el-radio>
        </div>
        <el-empty v-if="cascadeMeterList.length === 0" description="该集中器下暂无其他电表" />
      </div>
      <template #footer>
        <el-button @click="cascadeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveCascade">确认</el-button>
      </template>
    </el-dialog>
    
    <!-- 更改电表从属数据集中器弹窗 -->
    <el-dialog 
      v-model="pointDialogVisible" 
      title="更改电表从属数据集中器" 
      width="600px"
    >
      <div class="dialog-tip">
        当前电表：<strong>{{ selectedNode?.name }}</strong>
      </div>
      <div class="dialog-section-title">选择从属数据集中器：</div>
      <div class="radio-list">
        <el-radio 
          v-for="conc in concentratorList" 
          :key="conc.id"
          :model-value="pointForm.pointId" 
          @update:model-value="val => handleConcentratorChange(val)"
          :label="conc.id"
        >
          {{ conc.name }}
          <span class="item-tag">{{ conc.concentratorAddress || '无地址' }}</span>
        </el-radio>
        <el-empty v-if="concentratorList.length === 0" description="该站点下暂无集中器" />
      </div>
      
      <div v-if="pointForm.pointId" class="meter-list-section">
        <div class="dialog-section-title">选择上级电表（在新的集中器下）：</div>
        <div class="radio-list">
          <el-radio 
            :model-value="pointForm.parentMeterId" 
            @update:model-value="val => pointForm.parentMeterId = val"
            :label="0"
          >
            无上级电表
          </el-radio>
          <div v-for="meter in pointMeterList" :key="meter.id" class="radio-item">
            <el-radio 
              :model-value="pointForm.parentMeterId" 
              @update:model-value="val => pointForm.parentMeterId = val"
              :label="meter.id"
            >
              {{ meter.name }}
              <span v-if="meter.meterType" class="item-tag">{{ meter.meterType }}</span>
            </el-radio>
          </div>
          <el-empty v-if="pointMeterList.length === 0" description="该集中器下暂无其他电表" />
        </div>
      </div>
      
      <template #footer>
        <el-button @click="pointDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="savePoint">确认</el-button>
      </template>
    </el-dialog>

    <!-- 创建/修改虚拟分摊子电表对话框 -->
    <el-dialog
      v-model="virtualAllocDialogVisible"
      title="创建/修改虚拟分摊子电表"
      width="700px"
      :close-on-click-modal="false"
    >
      <div style="margin-bottom: 12px; display: flex; align-items: center; justify-content: space-between;">
        <span style="color: #909399; font-size: 13px;">
          父电表：{{ virtualAllocParentNode?.name }}
          <span style="margin-left: 16px;">分摊比例之和需等于1</span>
        </span>
        <el-button type="primary" size="small" @click="handleAddVirtualAlloc">
          <el-icon><Plus /></el-icon>&nbsp;新增
        </el-button>
      </div>

      <el-table :data="virtualAllocList" border style="width: 100%" :empty-text="'暂无数据，请点击新增按钮添加'">
        <el-table-column label="序号" type="index" width="60" align="center" />
        <el-table-column label="电表名称" min-width="200">
          <template #default="{ row, $index }">
            <el-input
              v-if="row.editable"
              v-model="row.name"
              placeholder="请输入电表名称"
              size="small"
            />
            <span v-else>{{ row.name }}</span>
          </template>
        </el-table-column>
        <el-table-column label="分摊比例" width="180">
          <template #default="{ row, $index }">
            <el-input-number
              v-if="row.editable"
              v-model="row.allocationRatio"
              :min="0"
              :step="0.1"
              :precision="6"
              :controls="true"
              size="small"
              style="width: 100%"
              placeholder="请输入"
            />
            <span v-else>{{ row.allocationRatio }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" align="center">
          <template #default="{ $index }">
            <el-button
              size="small"
              link
              type="primary"
              @click="handleEditVirtualAlloc($index)"
            >{{ virtualAllocList[$index].editable ? '完成' : '编辑' }}</el-button>
            <el-button
              size="small"
              link
              type="danger"
              @click="handleRemoveVirtualAlloc($index)"
            >删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div style="margin-top: 12px; text-align: right; color: #909399; font-size: 13px;">
        当前分摊比例之和：
        <span :style="{ color: virtualAllocList.length && Math.abs(virtualAllocList.reduce((s, i) => s + (Number(i.allocationRatio) || 0), 0) - 1) < 0.000001 ? '#67c23a' : '#f56c6c' }">
          {{ virtualAllocList.length ? virtualAllocList.reduce((s, i) => s + (Number(i.allocationRatio) || 0), 0).toFixed(6) : '0.000000' }}
        </span>
      </div>

      <template #footer>
        <el-button @click="virtualAllocDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="virtualAllocSaving" @click="handleConfirmVirtualAlloc">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick, watch, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { FolderOpened, OfficeBuilding, Collection, Odometer, Plus, ArrowUp, ArrowDown, Delete, Grid } from '@element-plus/icons-vue'
import axios from 'axios'
import { generatePinyinCode } from '../utils/pinyinUtils'
import { unitApi } from '../api/auth'
import MeterModelSelector from './MeterModelSelector.vue'
import DataSourceSystemDialog from './DataSourceSystemDialog.vue'

const props = defineProps({
  currentUser: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['back'])

const API_BASE = '/api/meter-settings'

const treeData = ref([])
const selectedNode = ref(null)
const dialogVisible = ref(false)
const dialogTitle = ref('')
const electricUnits = ref([])
const currentNodeType = ref('')
const editing = ref(false)
/** 分摊比例是否可编辑（已弃用：分摊比例现统一通过"创建/修改虚拟分摊子电表"对话框修改，此变量保留仅为兼容旧调用点） */
const allocationEditable = ref(false)

/** 虚拟分摊子电表对话框 */
const virtualAllocDialogVisible = ref(false)
const virtualAllocParentNode = ref(null)
const virtualAllocList = ref([])
const virtualAllocSaving = ref(false)
// 已删除的已有分摊子电表列表（仅记录带 id 的已有项，确定时执行 delete）
const virtualAllocDeletedList = ref([])
const formRef = ref(null)
const treeRef = ref(null)
const expandedKeys = ref([])
/** 保存当前展开状态，用于操作后恢复 */
const savedExpandedKeys = ref([])
/** 防止 watcher 干扰的标志 */
const isApplyingKeys = ref(false)
const formData = reactive({
  name: '',
  pinyinCode: '',
  description: '',
  transformerCapacity: '',
  concentratorAddress: '',
  meterType: '',
  meterModel: '',
  meterAddress: '',
  meterReadingMethod: 1,
  parentChildRelationship: 1,
  isVirtualMeter: 0,
  isAllocationChild: 0,
  allocationRatio: 1.0,
  powerCategory: '',
  energyAllocation: '',
  pointId: null,
  stationIntervalId: null,
  parentMeterId: null,
  isCumulative: 0,
  isMobileSource: 0,
  measurementUnit: '',
  dataSourceSystem: '',
  emissionSubcategory: '外购电力',
  billingCycleUnit: '月',
  billingCycleStartDate: '每月1日',
  billingCycleLength: 1,
  energyCategoryL1: '',
  energyCategoryL2: '',
  energyCategoryL3: '',
  energyUseCategory: '',
  autoMeterReadingConfig: ''
})

const formRules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  concentratorAddress: [
    { 
      required: true, 
      message: '请输入集中器地址', 
      trigger: 'blur' 
    },
    {
      pattern: /^[0-9]+$/,
      message: '集中器地址只能包含数字0-9',
      trigger: 'blur'
    }
  ],
  meterAddress: [
    { 
      required: true, 
      message: '请输入电表地址', 
      trigger: 'blur' 
    },
    {
      pattern: /^[0-9]+$/,
      message: '电表地址只能包含数字0-9',
      trigger: 'blur'
    }
  ]
}

/**
 * 处理集中器地址输入，过滤非数字字符
 * 通过正则替换移除所有非 0-9 字符，保证集中器地址仅含数字
 * @param {string} value 输入框当前值
 */
const handleConcentratorAddressInput = (value) => {
  const filtered = value.replace(/[^0-9]/g, '')
  formData.concentratorAddress = filtered
}

/**
 * 处理电表地址输入，过滤非数字字符
 * 通过正则替换移除所有非 0-9 字符，保证电表地址仅含数字
 * @param {string} value 输入框当前值
 */
const handleMeterAddressInput = (value) => {
  const filtered = value.replace(/[^0-9]/g, '')
  formData.meterAddress = filtered
}

/**
 * 电表类型数据字典
 */
const meterTypeOptions = ref([])

/**
 * 加载电表类型数据字典
 * 调用 GET /api/data-dict/items/meter_type 获取电表类型下拉框选项；
 * 请求失败时降级使用本地默认选项（单相、三相三线、三相四线、综合电力监测仪表）
 * @returns {Promise<void>}
 */
const loadMeterTypeDict = async () => {
  try {
    const response = await axios.get('/api/data-dict/items/meter_type')
    meterTypeOptions.value = response.data || []
  } catch (error) {
    console.error('加载电表类型字典失败:', error)
    // 降级：使用默认选项
    meterTypeOptions.value = [
      { code: 'single_phase', value: '单相' },
      { code: 'three_phase_three_wire', value: '三相三线' },
      { code: 'three_phase_four_wire', value: '三相四线' },
      { code: 'comprehensive_power_monitor', value: '综合电力监测仪表' }
    ]
  }
}

/**
 * 能耗数据划拨数据字典
 */
const energyAllocationOptions = ref([])

/**
 * 加载能耗数据划拨数据字典
 * 调用 GET /api/data-dict/items/energy_allocation 获取划拨方式下拉框选项；
 * 请求失败时降级使用本地默认选项
 * @returns {Promise<void>}
 */
const loadEnergyAllocationDict = async () => {
  try {
    const response = await axios.get('/api/data-dict/items/energy_allocation')
    energyAllocationOptions.value = response.data || []
  } catch (error) {
    console.error('加载能耗数据划拨字典失败:', error)
    // 降级：使用默认选项
    energyAllocationOptions.value = [
      { code: 'suring', value: '肃宁分公司' },
      { code: 'yuanping', value: '原平分公司' },
      { code: 'jilong', value: '机辆分公司' }
    ]
  }
}

/**
 * 能耗分类数据字典（三级级联）
 */
// 能耗分类字典全量缓存（组件挂载时一次性加载，后续二级/三级选项由 computed 过滤）
const energyCategories = ref([])
// 一级分类（能耗场景大类）下拉框选项：过滤 level===1 的分类
const energyCategoryL1Options = computed(() => energyCategories.value.filter(c => c.level === 1))
// 二级分类下拉框选项：依赖已选一级分类，按 parentId 过滤 level===2 的子分类；未选一级时返回空数组
const energyCategoryL2Options = computed(() => {
  if (!formData.energyCategoryL1) return []
  const parent = energyCategories.value.find(c => c.categoryCode === formData.energyCategoryL1)
  if (!parent) return []
  return energyCategories.value.filter(c => c.level === 2 && c.parentId === parent.id)
})
// 三级分类下拉框选项：依赖已选二级分类，按 parentId 过滤 level===3 的子分类；未选二级时返回空数组
const energyCategoryL3Options = computed(() => {
  if (!formData.energyCategoryL2) return []
  const parent = energyCategories.value.find(c => c.categoryCode === formData.energyCategoryL2)
  if (!parent) return []
  return energyCategories.value.filter(c => c.level === 3 && c.parentId === parent.id)
})
// 一级分类变更回调：清空已选的二级与三级分类，保证级联数据一致性
const handleCategoryL1Change = () => {
  formData.energyCategoryL2 = ''
  formData.energyCategoryL3 = ''
}
// 二级分类变更回调：清空已选的三级分类，保证级联数据一致性
const handleCategoryL2Change = () => {
  formData.energyCategoryL3 = ''
}
// 加载能耗分类字典：调用 GET /api/energy-categories 获取全量三级分类并缓存到 energyCategories
const loadEnergyCategories = async () => {
  try {
    const response = await axios.get('/api/energy-categories')
    energyCategories.value = response.data || []
  } catch (error) {
    console.error('加载能耗分类字典失败:', error)
    energyCategories.value = []
  }
}
// 根据分类编码查分类名称，用于详情面板展示能耗场景分类的中文名称；未匹配时回退显示编码本身
const getCategoryName = (code) => {
  if (!code) return '-'
  const item = energyCategories.value.find(c => c.categoryCode === code)
  return item ? item.categoryName : code
}

/**
 * 能耗用途分类数据字典（含拼音首字母编码，用于下拉框筛选）
 */
const energyUseOptions = ref([])
/**
 * 能耗用途分类下拉框当前查询词
 */
const energyUseQuery = ref('')

/**
 * 能耗用途分类下拉框可见选项（根据查询词过滤）
 * 支持按名称和拼音首字母筛选
 */
const visibleEnergyUses = computed(() => {
  const q = energyUseQuery.value.trim().toLowerCase()
  if (!q) return energyUseOptions.value
  return energyUseOptions.value.filter(item => {
    const label = (item.value || '').toLowerCase()
    const pinyin = (item.pinyinCode || '').toLowerCase()
    return label.includes(q) || pinyin.includes(q)
  })
})

/**
 * 能耗用途分类下拉框筛选方法
 * @param {string} query 用户输入的查询词
 */
const filterEnergyUse = (query) => {
  energyUseQuery.value = query
}

/**
 * 下拉框收起时重置能耗用途分类查询词
 */
const handleEnergyUseVisibleChange = (visible) => {
  if (!visible) {
    energyUseQuery.value = ''
  }
}

/**
 * 加载能耗用途分类数据字典
 * 调用 GET /api/data-dict/items/energy_use 获取能耗用途列表；
 * 为每项生成拼音首字母编码 pinyinCode，用于下拉框拼音筛选；
 * 请求失败时降级使用本地默认选项
 * @returns {Promise<void>}
 */
const loadEnergyUseDict = async () => {
  try {
    const response = await axios.get('/api/data-dict/items/energy_use')
    const list = response.data || []
    energyUseOptions.value = list.map(item => ({
      ...item,
      pinyinCode: generatePinyinCode(item.value || '')
    }))
  } catch (error) {
    console.error('加载能耗用途分类字典失败:', error)
    energyUseOptions.value = []
  }
}

/**
 * 电表型号选择器相关
 */
const meterModelSelectorRef = ref(null)

/**
 * 打开电表型号选择器
 */
const openMeterModelSelector = () => {
  meterModelSelectorRef.value?.open()
}

/**
 * 处理电表型号选择
 */
const handleMeterModelSelect = (result) => {
  if (result.action === 'select') {
    formData.meterModel = result.meterModel
  }
}

/**
 * 处理电表型号选择取消
 */
const handleMeterModelCancel = (_result) => {
  // 取消操作，不做任何处理
}

/**
 * 数据来源系统设置弹窗显示状态
 */
const showDataSourceSystemDialog = ref(false)

/**
 * 打开"数据来源系统设置"弹窗
 */
const openDataSourceSystemDialog = () => {
  showDataSourceSystemDialog.value = true
}

/**
 * 数据来源系统选择成功回调
 * 将选中的系统名称回填到表单 dataSourceSystem 字段
 * @param {Object} result 选择结果，包含 status 和 systemName
 */
const handleDataSourceSystemSelect = (result) => {
  if (result.status === 'select' && result.systemName) {
    formData.dataSourceSystem = result.systemName
  }
}

/**
 * 数据来源系统选择取消回调
 */
const handleDataSourceSystemCancel = () => {
  // 仅关闭，无需处理
}

/**
 * 自动抄表接口配置弹窗显示状态
 */
const autoReadingConfigVisible = ref(false)

/**
 * 自动抄表接口配置表单（最终以 JSON 字符串保存到 formData.autoMeterReadingConfig）
 */
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

/**
 * 打开"自动抄表接口设置"弹窗
 * 若 formData.autoMeterReadingConfig 已有值则解析回填，否则使用默认配置
 */
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

/**
 * 保存自动抄表接口配置
 * 将配置对象序列化为 JSON 字符串后写入 formData.autoMeterReadingConfig
 */
const saveAutoReadingConfig = () => {
  formData.autoMeterReadingConfig = JSON.stringify(autoReadingConfig)
  autoReadingConfigVisible.value = false
  ElMessage.success('自动抄表接口配置已保存（点击"确定"提交后生效）')
}

/**
 * 测试自动抄表接口
 * 校验配置完整性后调用后端测试接口，根据返回结果显示成功/失败
 */
const testingAutoReading = ref(false)
const testAutoReadingInterface = async () => {
  // 1. 校验配置是否已保存
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
    // 编辑模式带上电表ID，新建模式仅传配置
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

/**
 * 监听名称变化，自动生成拼音编码
 */
watch(() => formData.name, (newName) => {
  if (newName) {
    formData.pinyinCode = generatePinyinCode(newName)
  } else {
    formData.pinyinCode = ''
  }
})

/**
 * 获取树结构
 * @param {boolean} preserveExpanded - 是否保留之前的展开状态
 */
const loadTree = async (preserveExpanded = false) => {
  try {
    // 在刷新前保存当前的展开状态
    let keysToRestore = []
    if (preserveExpanded) {
      keysToRestore = getExpandedKeys()
    }
    
    const response = await axios.get(`${API_BASE}/tree`)
    treeData.value = [response.data]
    
    // 等待 el-tree 完全渲染并重建内部 store
    await nextTick()
    await nextTick()
    
    // 设置展开状态
    if (preserveExpanded && keysToRestore.length > 0) {
      // 恢复之前保存的展开状态
      isApplyingKeys.value = true
      expandedKeys.value = [...keysToRestore]
      applyExpandedKeysDirect(keysToRestore)
      isApplyingKeys.value = false
    } else if (preserveExpanded) {
      // 没有展开的节点，保持折叠状态
      isApplyingKeys.value = true
      expandedKeys.value = []
      applyExpandedKeysDirect([])
      isApplyingKeys.value = false
    } else {
      // 初始加载：展开根节点和一级节点
      const keys = collectRootAndStationKeys(treeData.value)
      isApplyingKeys.value = true
      expandedKeys.value = [...keys]
      applyExpandedKeysDirect(keys)
      isApplyingKeys.value = false
    }
  } catch (error) {
    console.error('加载树数据失败:', error)
    treeData.value = []
  }
}

/**
 * 收集所有节点ID（去重）
 */
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

/**
 * 只收集根节点ID（用于"折叠全部"）
 */
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

/**
 * 收集根节点和一级节点ID（用于初始加载）
 * 这样初始状态下会展开根节点和所有一级节点
 */
const collectRootAndStationKeys = (nodes) => {
  const keys = []
  const walk = (arr, level) => {
    for (const n of arr) {
      keys.push(n.id)
      if (level === 0 && n.children && n.children.length) {
        // 收集一级节点
        for (const child of n.children) {
          keys.push(child.id)
        }
      }
    }
  }
  walk(nodes, 0)
  return keys
}

/**
 * 按层级收集节点ID
 * 返回一个二维数组，其中 arr[level] 是该层级所有节点ID的数组
 */
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

/**
 * 展开/折叠事件处理
 * 同步 el-tree 的展开状态到 expandedKeys 响应式变量；
 * 当 isApplyingKeys 为 true（节点移动等操作期间）时跳过，防止干扰
 * @param {Object} data 节点数据对象
 * @param {boolean} expanded 是否展开
 */
const handleExpandChange = (data, expanded) => {
  // 如果正在应用 keys（如节点移动操作），跳过以防止干扰
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

/**
 * 节点点击
 */
const handleNodeClick = (data) => {
  // 使用 data 直接赋值，el-tree 传递的应该是原始数据对象
  console.log('点击节点类型:', data.nodeType)
  console.log('点击节点id:', data.id)
  console.log('点击节点pointId:', data.pointId)
  selectedNode.value = data
}

/**
 * 全部展开
 * 收集树中所有节点 ID 并赋值给 expandedKeys，触发 watcher 应用展开状态
 * @returns {Promise<void>}
 */
const handleExpandAll = async () => {
  const allKeys = collectAllKeys(treeData.value)
  expandedKeys.value = [...allKeys]
  await nextTick()
}

/**
 * 全部折叠（所有节点都折叠，只显示根节点）
 * 清空 expandedKeys 触发 watcher 折叠所有节点
 * @returns {Promise<void>}
 */
const handleCollapseAll = async () => {
  expandedKeys.value = []
  await nextTick()
}

/**
 * 重置表单
 */
const resetForm = () => {
  formData.name = ''
  formData.pinyinCode = ''
  formData.description = ''
  formData.transformerCapacity = ''
  formData.concentratorAddress = ''
  formData.meterType = ''
  formData.meterModel = ''
  formData.meterAddress = ''
  formData.meterReadingMethod = 1
  formData.parentChildRelationship = 1
  formData.isVirtualMeter = 0
  formData.isAllocationChild = 0
  formData.allocationRatio = 1.0
  formData.powerCategory = ''
  formData.energyAllocation = ''
  formData.pointId = null
  formData.stationIntervalId = null
  formData.parentMeterId = null
  formData.isCumulative = 0
  formData.isMobileSource = 0
  formData.measurementUnit = ''
  formData.dataSourceSystem = ''
  formData.emissionSubcategory = '外购电力'
  formData.billingCycleUnit = 2
  formData.billingCycleStartDate = 0
  formData.billingCycleLength = 1
  formData.energyCategoryL1 = ''
  formData.energyCategoryL2 = ''
  formData.energyCategoryL3 = ''
  formData.energyUseCategory = ''
  formData.autoMeterReadingConfig = ''
  formRef.value?.resetFields()
}

/**
 * 添加站点
 */
const handleAddStation = () => {
  currentNodeType.value = 'station'
  dialogTitle.value = '添加站点/区间'
  editing.value = false
  resetForm()
  dialogVisible.value = true
}

/**
 * 添加集中器
 */
const handleAddConcentrator = (node) => {
  currentNodeType.value = 'concentrator'
  dialogTitle.value = '添加集中器'
  editing.value = false
  resetForm()
  formData.stationIntervalId = node.id
  dialogVisible.value = true
}

/**
 * 点击"修改分摊比例"按钮：针对虚拟分摊子电表，打开"创建/修改虚拟分摊子电表"对话框，
 * 统一修改其父节点下属所有分摊子电表的比例（确保比例之和为1）。
 * 通过 selectedNode.parentId 在 treeData 中查找父电表节点，再调用 handleOpenVirtualAlloc。
 * @returns {void} 无返回值；非分摊子电表时按钮已 disabled 不触发；父节点未找到时 ElMessage 提示
 */
const handleEditAllocation = () => {
  const parentId = selectedNode.value.parentId
  const parentNode = findNodeInTree(treeData.value, parentId)
  if (!parentNode) {
    ElMessage.warning('未找到父电表节点，无法修改分摊比例')
    return
  }
  handleOpenVirtualAlloc(parentNode)
}

/**
 * 打开"创建/修改虚拟分摊子电表"对话框
 * 打开前先检查父电表节点是否已包含下属虚拟分摊子电表（isAllocationChild===1）：
 *  - 若无：列表初始化为空
 *  - 若有：列表加载所有已有的分摊子电表（带 id 标记，确定时执行 update）
 * @param {Object} node - 当前点击的电表树节点对象（含 id/name/pointId/children 等字段），作为分摊子电表的父节点
 * @returns {void} 无返回值，通过设置 virtualAllocParentNode/virtualAllocList/virtualAllocDialogVisible 打开对话框
 */
const handleOpenVirtualAlloc = (node) => {
  virtualAllocParentNode.value = node
  virtualAllocList.value = []
  virtualAllocDeletedList.value = []
  // 检查该电表节点是否已包含下属虚拟分摊子电表
  const existingChildren = (node.children || []).filter(c => c.isAllocationChild === 1)
  if (existingChildren.length > 0) {
    // 已有分摊子电表，加载到列表中（带 id 与 _origin 标记，确定时执行 update）
    virtualAllocList.value = existingChildren.map(c => ({
      id: c.id,
      name: c.name,
      allocationRatio: c.allocationRatio,
      editable: false,
      _origin: c
    }))
  }
  virtualAllocDialogVisible.value = true
}

/**
 * 新增一行虚拟分摊子电表
 * 电表名称缺省继承父节点名称 + 序号后缀
 * @returns {void} 无返回值，向 virtualAllocList 追加一个待创建分摊子电表对象 {name, allocationRatio, editable}
 */
const handleAddVirtualAlloc = () => {
  const parentNode = virtualAllocParentNode.value
  const parentName = parentNode?.name || '电表'
  const seq = virtualAllocList.value.length + 1
  virtualAllocList.value.push({
    name: `${parentName}-${seq}`,
    allocationRatio: null,
    editable: true
  })
}

/**
 * 切换某行的编辑/查看状态
 * @param {number} index - 待切换状态的行索引（基于 virtualAllocList）
 * @returns {void} 无返回值，翻转 virtualAllocList[index].editable 控制该行名称与比例是否可编辑
 */
const handleEditVirtualAlloc = (index) => {
  const row = virtualAllocList.value[index]
  row.editable = !row.editable
}

/**
 * 删除某行虚拟分摊子电表
 * 若该行是已有记录（带 id），移入 virtualAllocDeletedList，确定时执行 delete；
 * 若是新增行（无 id），直接从列表移除即可。
 * @param {number} index - 待删除的行索引（基于 virtualAllocList）
 * @returns {void} 无返回值，更新 virtualAllocList 与 virtualAllocDeletedList
 */
const handleRemoveVirtualAlloc = (index) => {
  const item = virtualAllocList.value[index]
  if (item && item.id) {
    // 已有分摊子电表被移除 → 记录到删除列表，确定时执行 delete
    virtualAllocDeletedList.value.push(item)
  }
  virtualAllocList.value.splice(index, 1)
}

/**
 * 确认创建/修改虚拟分摊子电表
 * 1. 校验分摊比例之和是否为1（基于列表中剩余项，已删除项不参与求和）
 * 2. 区分已有（带 id）、新增（无 id）、删除（virtualAllocDeletedList）三类分别处理：
 *    - 已有分摊子电表 → 执行 PUT /api/meter-settings/node/meter/{id} 更新（以原节点数据为基础覆盖名称/比例）
 *    - 新增分摊子电表 → 执行 POST /api/meter-settings/meter 插入（复制父节点信息 + 分摊比例）
 *    - 删除的分摊子电表 → 若原状态为启用先 toggle-status 停用，再执行 DELETE /api/meter-settings/node/meter/{id} 删除
 * 3. 完成后关闭对话框并调用 loadTree(true) 刷新树
 * @returns {Promise<void>} 异步无返回值；校验失败时 ElMessage 提示并中断
 * @关键变量 virtualAllocList - 剩余分摊子电表列表（含 id 标记已有项、_origin 原始节点数据）；
 *   virtualAllocDeletedList - 已删除的已有分摊子电表列表（确定时执行 delete）；
 *   virtualAllocParentNode - 父电表节点；payload - 单个分摊子电表的请求体
 */
const handleConfirmVirtualAlloc = async () => {
  const list = virtualAllocList.value
  if (!list.length && !virtualAllocDeletedList.value.length) {
    ElMessage.warning('请至少添加一个虚拟分摊子电表')
    return
  }
  // 校验名称和比例（仅对列表中剩余项校验，已删除项不参与）
  for (let i = 0; i < list.length; i++) {
    if (!list[i].name || !list[i].name.trim()) {
      ElMessage.warning(`第${i + 1}行电表名称不能为空`)
      return
    }
    if (list[i].allocationRatio === null || list[i].allocationRatio === undefined || list[i].allocationRatio < 0) {
      ElMessage.warning(`第${i + 1}行分摊比例不能为空且不能小于0`)
      return
    }
  }
  // 校验分摊比例之和为1（仅列表中有剩余项时校验）
  if (list.length) {
    const sum = list.reduce((acc, item) => acc + Number(item.allocationRatio), 0)
    if (Math.abs(sum - 1) > 0.000001) {
      ElMessage.error(`分摊比例之和为${sum.toFixed(6)}，不等于1，请调整后重试`)
      return
    }
  }

  const parentNode = virtualAllocParentNode.value
  const userId = props.currentUser?.userId || 1
  virtualAllocSaving.value = true
  try {
    let updateCount = 0
    let insertCount = 0
    for (const item of list) {
      const trimmedName = item.name.trim()
      if (item.id) {
        // 已有分摊子电表 → 执行 update，以原节点数据为基础，覆盖名称与分摊比例
        const origin = item._origin || {}
        const payload = {
          name: trimmedName,
          pinyinCode: generatePinyinCode(trimmedName),
          description: origin.description || parentNode.description || '',
          meterType: origin.meterType || parentNode.meterType || '',
          meterModel: origin.meterModel || parentNode.meterModel || '',
          meterAddress: origin.meterAddress || parentNode.meterAddress || '',
          meterReadingMethod: origin.meterReadingMethod ?? parentNode.meterReadingMethod ?? 1,
          parentChildRelationship: origin.parentChildRelationship ?? parentNode.parentChildRelationship ?? 1,
          isVirtualMeter: origin.isVirtualMeter ?? parentNode.isVirtualMeter ?? 0,
          isAllocationChild: 1,
          allocationRatio: item.allocationRatio,
          powerCategory: origin.powerCategory || parentNode.powerCategory || '',
          energyAllocation: origin.energyAllocation || parentNode.energyAllocation || '',
          pointId: origin.pointId ?? parentNode.pointId,
          stationIntervalId: origin.stationIntervalId ?? parentNode.stationIntervalId,
          parentMeterId: origin.parentId ?? parentNode.id,
          isCumulative: origin.isCumulative ?? parentNode.isCumulative ?? 0,
          isMobileSource: origin.isMobileSource ?? parentNode.isMobileSource ?? 0,
          measurementUnit: origin.measurementUnit || parentNode.measurementUnit || '',
          dataSourceSystem: origin.dataSourceSystem || parentNode.dataSourceSystem || '',
          emissionSubcategory: origin.emissionSubcategory || parentNode.emissionSubcategory || '外购电力',
          billingCycleUnit: origin.billingCycleUnit ?? parentNode.billingCycleUnit ?? 2,
          billingCycleStartDate: origin.billingCycleStartDate ?? parentNode.billingCycleStartDate ?? 0,
          billingCycleLength: origin.billingCycleLength ?? parentNode.billingCycleLength ?? 1,
          energyCategoryL1: origin.energyCategoryL1 || parentNode.energyCategoryL1 || '',
          energyCategoryL2: origin.energyCategoryL2 || parentNode.energyCategoryL2 || '',
          energyCategoryL3: origin.energyCategoryL3 || parentNode.energyCategoryL3 || '',
          energyUseCategory: origin.energyUseCategory || parentNode.energyUseCategory || '',
          createdBy: userId
        }
        await axios.put(`${API_BASE}/node/meter/${item.id}`, payload)
        updateCount++
      } else {
        // 新增分摊子电表 → 执行 insert，复制父节点信息，设置虚拟分摊子电表特有字段
        const payload = {
          name: trimmedName,
          pinyinCode: generatePinyinCode(trimmedName),
          description: parentNode.description || '',
          meterType: parentNode.meterType || '',
          meterModel: parentNode.meterModel || '',
          meterAddress: parentNode.meterAddress || '',
          meterReadingMethod: parentNode.meterReadingMethod || 1,
          parentChildRelationship: parentNode.parentChildRelationship || 1,
          isVirtualMeter: parentNode.isVirtualMeter ?? 0,
          isAllocationChild: 1,
          allocationRatio: item.allocationRatio,
          powerCategory: parentNode.powerCategory || '',
          energyAllocation: parentNode.energyAllocation || '',
          pointId: parentNode.pointId,
          stationIntervalId: parentNode.stationIntervalId,
          parentMeterId: parentNode.id,
          isCumulative: parentNode.isCumulative || 0,
          isMobileSource: parentNode.isMobileSource || 0,
          measurementUnit: parentNode.measurementUnit || '',
          dataSourceSystem: parentNode.dataSourceSystem || '',
          emissionSubcategory: parentNode.emissionSubcategory || '外购电力',
          billingCycleUnit: parentNode.billingCycleUnit ?? 2,
          billingCycleStartDate: parentNode.billingCycleStartDate ?? 0,
          billingCycleLength: parentNode.billingCycleLength || 1,
          energyCategoryL1: parentNode.energyCategoryL1 || '',
          energyCategoryL2: parentNode.energyCategoryL2 || '',
          energyCategoryL3: parentNode.energyCategoryL3 || '',
          energyUseCategory: parentNode.energyUseCategory || '',
          createdBy: userId
        }
        await axios.post(`${API_BASE}/meter`, payload)
        insertCount++
      }
    }
    // 处理已删除的已有分摊子电表 → 执行 delete
    let deleteCount = 0
    for (const item of virtualAllocDeletedList.value) {
      if (!item.id) continue
      // 后端要求删除前节点必须为停用状态：若原节点为启用，先调用 toggle-status 停用
      const origin = item._origin || {}
      if (origin.status === 1) {
        const toggleRes = await axios.post(`${API_BASE}/node/meter/${item.id}/toggle-status`, {}, {
          headers: { 'X-User-Id': userId }
        })
        if (toggleRes.data && toggleRes.data.success === false) {
          throw new Error(toggleRes.data.message || `停用分摊子电表"${item.name}"失败`)
        }
      }
      await axios.delete(`${API_BASE}/node/meter/${item.id}`, {
        headers: { 'X-User-Id': userId }
      })
      deleteCount++
    }
    ElMessage.success(`成功更新${updateCount}个、新增${insertCount}个、删除${deleteCount}个虚拟分摊子电表`)
    virtualAllocDialogVisible.value = false
    loadTree(true)
  } catch (error) {
    if (error.message && !error.response) {
      // toggle-status 返回的业务失败（非 HTTP 错误）
      ElMessage.error(error.message)
    } else if (error.response?.data?.error) {
      ElMessage.error(error.response.data.error)
    } else {
      ElMessage.error('保存虚拟分摊子电表失败')
    }
  } finally {
    virtualAllocSaving.value = false
  }
}

/**
 * 添加电表
 * 当父电表的 parent_child_relationship=2（总表计数≠分表计数之和）时，
 * 检查其下一级子电表中是否已存在虚拟电表：
 *  - 若已存在虚拟电表：直接添加实体电表（确保每个电表下级只能有一个虚拟电表）
 *  - 若不存在虚拟电表：弹窗让用户选择"添加下级实体电表"或"添加下级虚拟电表"
 */
const handleAddMeter = async (node) => {
  currentNodeType.value = 'meter'
  dialogTitle.value = '添加电表'
  editing.value = false
  allocationEditable.value = false
  resetForm()
  if (node.nodeType === 'concentrator') {
    formData.pointId = node.id
    formData.parentMeterId = 0
  } else if (node.nodeType === 'meter') {
    // 子表继承父表的集中器ID
    formData.pointId = node.pointId
    formData.parentMeterId = node.id
    // 父表计数与分表不相等时，需判断是否添加虚拟电表
    if (node.parentChildRelationship === 2) {
      const children = node.children || []
      const hasVirtualChild = children.some(c => c.isVirtualMeter === 1)
      if (!hasVirtualChild) {
        // 尚无虚拟电表，询问用户添加实体还是虚拟电表
        try {
          await ElMessageBox.confirm(
            `当前电表"${node.name}"的总表计数不等于各下属分表计数之和，且其下级暂无虚拟电表。\n请选择要添加的下级电表类型：`,
            '添加下级电表',
            {
              confirmButtonText: '添加下级实体电表',
              cancelButtonText: '添加下级虚拟电表',
              distinguishCancelAndClose: true,
              type: 'info'
            }
          )
          // 用户选择"添加下级实体电表"
          formData.isVirtualMeter = 0
        } catch (action) {
          if (action === 'cancel') {
            // 用户选择"添加下级虚拟电表"
            formData.isVirtualMeter = 1
          } else {
            // 用户点击关闭按钮（X），取消整个添加操作
            return
          }
        }
      } else {
        // 已存在虚拟电表，默认添加实体电表
        formData.isVirtualMeter = 0
      }
    } else {
      // parent_child_relationship=1，普通添加
      formData.isVirtualMeter = 0
    }
  }
  dialogVisible.value = true
}

/**
 * 编辑节点
 */
const handleEdit = (node) => {
  currentNodeType.value = node.nodeType
  dialogTitle.value = '编辑' + getNodeTypeLabel(node.nodeType)
  editing.value = true
  allocationEditable.value = false
  Object.assign(formData, {
    name: node.name,
    pinyinCode: node.pinyinCode || '',
    description: node.description || '',
    transformerCapacity: node.transformerCapacity || '',
    concentratorAddress: node.concentratorAddress || '',
    pointId: node.pointId || 0,
    parentMeterId: node.nodeType === 'meter' ? (node.parentId || 0) : 0,
    meterType: node.meterType || '',
    meterModel: node.meterModel || '',
    meterAddress: node.meterAddress || '',
    meterReadingMethod: node.meterReadingMethod || 1,
    parentChildRelationship: node.parentChildRelationship || 1,
    isVirtualMeter: node.isVirtualMeter ?? 0,
    isAllocationChild: node.isAllocationChild ?? 0,
    allocationRatio: node.allocationRatio ?? 1.0,
    powerCategory: node.powerCategory || '',
    energyAllocation: node.energyAllocation || '',
    isCumulative: node.isCumulative || 0,
    isMobileSource: node.isMobileSource || 0,
    measurementUnit: node.measurementUnit || '',
    dataSourceSystem: node.dataSourceSystem || '',
    emissionSubcategory: node.emissionSubcategory || '外购电力',
    billingCycleUnit: node.billingCycleUnit ?? 2,
    billingCycleStartDate: node.billingCycleStartDate ?? 0,
    billingCycleLength: node.billingCycleLength || 1,
    accountingScenario: node.accountingScenario || '',
    energyUseCategory: node.energyUseCategory || '',
    autoMeterReadingConfig: node.autoMeterReadingConfig || ''
  })
  dialogVisible.value = true
}

/**
 * 获取节点类型标签
 */
const getNodeTypeLabel = (type) => {
  const labels = { station: '站点/区间', concentrator: '集中器', meter: '电表' }
  return labels[type] || ''
}

/**
 * 提交表单
 */
const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    const userId = props.currentUser?.userId || 1
    
    if (editing.value) {
      await axios.put(`${API_BASE}/node/${currentNodeType.value}/${selectedNode.value.id}`, {
        ...formData,
        createdBy: userId
      })
      ElMessage.success('更新成功')
    } else {
      if (currentNodeType.value === 'station') {
        await axios.post(`${API_BASE}/station`, { ...formData, createdBy: userId })
      } else if (currentNodeType.value === 'concentrator') {
        await axios.post(`${API_BASE}/concentrator`, { ...formData, createdBy: userId })
      } else if (currentNodeType.value === 'meter') {
        await axios.post(`${API_BASE}/meter`, { ...formData, createdBy: userId })
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

/**
 * 删除节点
 */
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
    await axios.delete(`${API_BASE}/node/${node.nodeType}/${node.id}`, {
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

/**
 * 切换状态
 */
const handleToggleStatus = async (node) => {
  try {
    const userId = props.currentUser?.userId || 1
    const response = await axios.post(
      `${API_BASE}/node/${node.nodeType}/${node.id}/toggle-status`,
      {},
      { headers: { 'X-User-Id': userId } }
    )
    const result = response.data
    if (result.success) {
      ElMessage.success(result.message)
    } else {
      ElMessage.warning(result.message)
    }
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

/**
 * 获取当前树结构快照（用于回滚）
 */
const getTreeSnapshot = () => JSON.parse(JSON.stringify(treeData.value))

/**
 * 获取当前树中所有展开节点的 ID 列表
 * 直接遍历 el-tree 内部 store 的节点树，确保获取的是真实展开状态
 * （而非 expandedKeys 响应式变量，后者可能因事件时序未及时同步）
 * @returns {Array<number|string>} 已展开节点的 ID 数组
 */
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

/**
 * 在树数据数组中原地移动指定节点（仅修改数组顺序，不调用后端）
 * 通过递归遍历找到目标节点所在层级，按 direction 上下交换位置
 * @param {number|string} nodeId 待移动的节点ID
 * @param {number} direction 移动方向：-1 上移，1 下移
 * @returns {boolean} 移动成功返回 true；已是边界或未找到节点返回 false
 */
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

/**
 * 刷新树组件
 * 通过重新赋值 treeData 数组引用强制 el-tree 重建内部 store，
 * 等待渲染完成后恢复展开状态
 * @returns {Promise<void>}
 */
const refreshTree = async () => {
  treeData.value = [...treeData.value]
  // 等待 el-tree 完全渲染并重建内部 store
  await nextTick()
  await nextTick()
  // 应用展开状态
  applyExpandedKeys()
}

/**
 * 上移节点
 * 实现关键逻辑（展开状态保持）：
 * 1. 先通过 getExpandedKeys 从 el-tree store 获取真实展开状态快照
 * 2. 在树数据中原地上移节点（moveNodeInTreeData）
 * 3. 重建树组件并设置 isApplyingKeys 标志防止 watcher 干扰
 * 4. 通过 applyExpandedKeysDirect 直接操作 store 恢复展开状态
 * 5. 调用后端 move-up 接口；失败时回滚到快照并恢复展开状态
 * @param {Object} node 待上移的节点对象
 * @returns {Promise<void>}
 */
const handleMoveUp = async (node) => {
  const snapshot = getTreeSnapshot()
  // 从 el-tree store 获取实际的展开状态，确保获取所有展开的节点
  const savedKeys = getExpandedKeys()
  
  const moved = moveNodeInTreeData(node.id, -1)
  if (!moved) {
    ElMessage.warning('无法上移，已经是第一个节点')
    return
  }
  
  try {
    // 刷新组件以显示更改（会重建 el-tree 的 store）
    treeData.value = [...treeData.value]
    
    // 使用 flag 防止 watcher 干扰
    isApplyingKeys.value = true
    
    await nextTick()
    await nextTick()
    
    // 直接通过 store 操作应用展开状态
    applyExpandedKeysDirect(savedKeys)
    
    // 同步 expandedKeys
    expandedKeys.value = [...savedKeys]
    
    isApplyingKeys.value = false
    
    const userId = props.currentUser?.userId || 1
    await axios.post(`${API_BASE}/node/${node.nodeType}/${node.id}/move-up`, {}, {
      headers: { 'X-User-Id': userId }
    })
  } catch (error) {
    treeData.value = snapshot
    isApplyingKeys.value = true
    await nextTick()
    await nextTick()
    applyExpandedKeysDirect(savedKeys)
    expandedKeys.value = [...savedKeys]
    isApplyingKeys.value = false
    ElMessage.error(error.response?.data?.error || '上移失败')
  }
}

/**
 * 下移节点
 * 实现关键逻辑（展开状态保持）：
 * 1. 先通过 getExpandedKeys 从 el-tree store 获取真实展开状态快照
 * 2. 在树数据中原地下移节点（moveNodeInTreeData）
 * 3. 重建树组件并设置 isApplyingKeys 标志防止 watcher 干扰
 * 4. 通过 applyExpandedKeysDirect 直接操作 store 恢复展开状态
 * 5. 调用后端 move-down 接口；失败时回滚到快照并恢复展开状态
 * @param {Object} node 待下移的节点对象
 * @returns {Promise<void>}
 */
const handleMoveDown = async (node) => {
  const snapshot = getTreeSnapshot()
  // 从 el-tree store 获取实际的展开状态，确保获取所有展开的节点
  const savedKeys = getExpandedKeys()
  
  const moved = moveNodeInTreeData(node.id, 1)
  if (!moved) {
    ElMessage.warning('无法下移，已经是最后一个节点')
    return
  }
  
  try {
    // 刷新组件以显示更改（会重建 el-tree 的 store）
    treeData.value = [...treeData.value]
    
    // 使用 flag 防止 watcher 干扰
    isApplyingKeys.value = true
    
    await nextTick()
    await nextTick()
    
    // 直接通过 store 操作应用展开状态
    applyExpandedKeysDirect(savedKeys)
    
    // 同步 expandedKeys
    expandedKeys.value = [...savedKeys]
    
    isApplyingKeys.value = false
    
    const userId = props.currentUser?.userId || 1
    await axios.post(`${API_BASE}/node/${node.nodeType}/${node.id}/move-down`, {}, {
      headers: { 'X-User-Id': userId }
    })
  } catch (error) {
    treeData.value = snapshot
    isApplyingKeys.value = true
    await nextTick()
    await nextTick()
    applyExpandedKeysDirect(savedKeys)
    expandedKeys.value = [...savedKeys]
    isApplyingKeys.value = false
    ElMessage.error(error.response?.data?.error || '下移失败')
  }
}

/**
 * 直接同步应用展开状态到 el-tree 内部 store（同步版本）
 * 用于节点移动等需要立即恢复展开状态的场景，通过操作 store.nodesMap 避免事件时序问题
 * @param {Array<number|string>} keys 需展开的节点 ID 数组；未包含的节点将被折叠
 */
const applyExpandedKeysDirect = (keys) => {
  if (!treeRef.value || !treeRef.value.store) {
    return
  }
  
  const { nodesMap } = treeRef.value.store
  
  // 检查 nodesMap 是否有数据
  if (Object.keys(nodesMap).length === 0) {
    return
  }
  
  // 先折叠所有节点
  Object.values(nodesMap).forEach(node => {
    node.expanded = false
  })
  
  // 然后展开指定的节点
  for (const key of keys) {
    const node = nodesMap[key]
    if (node) {
      node.expanded = true
    }
  }
}

/**
 * 应用展开状态到 el-tree（异步带重试版本）
 * 供 watcher 和 loadTree 使用，最多重试 5 次以等待 el-tree 的 store 与 nodesMap 就绪
 * 展开状态来源于 expandedKeys 响应式变量
 * @returns {Promise<void>}
 */
const applyExpandedKeys = async () => {
  for (let attempt = 0; attempt < 5; attempt++) {
    if (!treeRef.value || !treeRef.value.store) {
      await new Promise(resolve => setTimeout(resolve, 50))
      continue
    }
    
    const { nodesMap } = treeRef.value.store
    
    // 检查 nodesMap 是否有数据
    if (Object.keys(nodesMap).length === 0) {
      await new Promise(resolve => setTimeout(resolve, 50))
      continue
    }
    
    // 先折叠所有节点
    Object.values(nodesMap).forEach(node => {
      node.expanded = false
    })
    
    // 然后展开指定的节点
    for (const key of expandedKeys.value) {
      const node = nodesMap[key]
      if (node) {
        node.expanded = true
      }
    }
    
    break
  }
}

// 监听 expandedKeys 变化
watch(expandedKeys, () => {
  // 如果正在应用 keys（如节点移动操作），跳过 watcher
  if (isApplyingKeys.value) {
    return
  }
  applyExpandedKeys()
}, { deep: true })

onMounted(async () => {
  await nextTick()
  // 并行加载数据字典和树结构
  await Promise.all([
    loadMeterTypeDict(),
    loadEnergyAllocationDict(),
    loadEnergyCategories(),
    loadEnergyUseDict(),
    loadTree(),
    (async () => {
      try {
        const res = await unitApi.listByCategory('ELECTRIC')
        electricUnits.value = res.data || []
      } catch (err) {
        console.error('加载电力标准单位失败', err)
      }
    })()
  ])
})

// ==================== 更改电表级联关系 ====================

const cascadeDialogVisible = ref(false)
const cascadeForm = reactive({
  parentMeterId: 0
})
const cascadeMeterList = ref([])

/**
 * 从树数据中递归查找指定 ID 的节点
 * 用于从原始 treeData 中获取完整的节点信息（含 pointId 等字段），
 * 避免 el-tree @node-click 事件传递的 data 对象字段不完整的问题
 * @param {Array} nodes 树节点数组
 * @param {number|string} id 目标节点 ID
 * @returns {Object|null} 找到的节点对象；未找到返回 null
 */
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

/**
 * 打开"更改电表级联关系"弹窗
 * 实现关键逻辑：
 * 1. 通过 findNodeInTree 从 treeData 获取当前电表的完整信息（含 pointId）
 * 2. 调用 GET /api/meter-settings/meters-by-point 获取同集中器下其余电表列表
 * 3. 以当前电表的 parentId 初始化单选框选中项
 * @returns {Promise<void>}
 */
const openCascadeDialog = async () => {
  if (!selectedNode.value) return
  
  // 从树数据中查找完整的节点信息
  const fullNode = findNodeInTree(treeData.value, selectedNode.value.id)
  if (!fullNode) {
    ElMessage.error('获取节点信息失败')
    return
  }
  
  console.log('从树数据中找到的节点:', fullNode)
  console.log('pointId值:', fullNode.pointId)
  
  cascadeForm.parentMeterId = fullNode.parentId || 0
  
  try {
    const response = await axios.get(`${API_BASE}/meters-by-point`, {
      params: {
        pointId: fullNode.pointId,
        excludeMeterId: fullNode.id
      }
    })
    console.log('返回的电表列表:', response.data)
    cascadeMeterList.value = response.data || []
  } catch (error) {
    console.error('加载电表列表失败:', error)
    cascadeMeterList.value = []
  }
  
  cascadeDialogVisible.value = true
}

/**
 * 保存电表级联关系更改
 * 调用 PUT /api/meter-settings/meter/{id}/cascade 接口提交新的上级电表ID，
 * 成功后关闭弹窗、重新加载树结构并清空选中节点
 * @returns {Promise<void>}
 */
const saveCascade = async () => {
  try {
    const userId = props.currentUser?.userId || 1
    await axios.put(`${API_BASE}/meter/${selectedNode.value.id}/cascade`, {
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

// ==================== 更改电表从属数据集中器 ====================

const pointDialogVisible = ref(false)
const pointForm = reactive({
  pointId: 0,
  parentMeterId: 0
})
const concentratorList = ref([])
const pointMeterList = ref([])

/**
 * 打开"更改电表从属数据集中器"弹窗
 * 实现关键逻辑：
 * 1. 通过 findNodeInTree 获取当前电表完整信息
 * 2. 查询当前电表所属集中器的站点ID，再获取该站点下所有集中器
 * 3. 加载当前集中器下的电表列表（用于下半部分展示可选上级电表）
 * @returns {Promise<void>}
 */
const openPointDialog = async () => {
  if (!selectedNode.value) return
  
  // 从树数据中查找完整的节点信息
  const fullNode = findNodeInTree(treeData.value, selectedNode.value.id)
  if (!fullNode) {
    ElMessage.error('获取节点信息失败')
    return
  }
  
  console.log('从树数据中找到的节点(从属集中器):', fullNode)
  
  pointForm.pointId = fullNode.pointId
  pointForm.parentMeterId = fullNode.parentId || 0
  
  // 获取当前电表所属集中器的站点ID
  const pointResponse = await axios.get(`${API_BASE}/node/concentrator/${fullNode.pointId}`)
  const stationId = pointResponse.data?.parentId
  
  if (stationId) {
    try {
      const response = await axios.get(`${API_BASE}/concentrators-by-station`, {
        params: { stationId: stationId }
      })
      concentratorList.value = response.data || []
    } catch (error) {
      console.error('加载集中器列表失败:', error)
      concentratorList.value = []
    }
  }
  
  // 加载当前集中器下的电表列表
  await loadPointMeterList(fullNode.id)
  
  pointDialogVisible.value = true
}

/**
 * 加载指定集中器下的电表列表（排除当前电表自身）
 * 调用 GET /api/meter-settings/meters-by-point 接口，结果存入 pointMeterList
 * @param {number|string} excludeMeterId 需排除的电表ID（当前电表）；为空时取 selectedNode.id
 * @returns {Promise<void>}
 */
const loadPointMeterList = async (excludeMeterId) => {
  if (!pointForm.pointId) {
    pointMeterList.value = []
    return
  }
  
  try {
    const response = await axios.get(`${API_BASE}/meters-by-point`, {
      params: {
        pointId: pointForm.pointId,
        excludeMeterId: excludeMeterId || selectedNode.value?.id
      }
    })
    pointMeterList.value = response.data || []
  } catch (error) {
    console.error('加载电表列表失败:', error)
    pointMeterList.value = []
  }
}

/**
 * 处理集中器单选变化
 * 切换集中器后更新 pointId，将上级电表重置为"无上级电表"（0），
 * 并重新加载新集中器下的电表列表
 * @param {number|string} newPointId 新选中的集中器ID
 * @returns {Promise<void>}
 */
const handleConcentratorChange = async (newPointId) => {
  pointForm.pointId = newPointId
  // 切换集中器后，默认选择"无上级电表"
  pointForm.parentMeterId = 0
  // 重新加载电表列表，排除当前电表自身
  await loadPointMeterList(selectedNode.value?.id)
}

/**
 * 保存电表从属数据集中器更改
 * 调用 PUT /api/meter-settings/meter/{id}/point 接口提交新的集中器ID和上级电表ID，
 * 成功后关闭弹窗、重新加载树结构并清空选中节点
 * @returns {Promise<void>}
 */
const savePoint = async () => {
  try {
    const userId = props.currentUser?.userId || 1
    await axios.put(`${API_BASE}/meter/${selectedNode.value.id}/point`, {
      pointId: pointForm.pointId,
      parentMeterId: pointForm.parentMeterId,
      userId: userId
    })
    ElMessage.success('更改从属数据集中器成功')
    pointDialogVisible.value = false
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
</script>

<style scoped>
.electric-meter-settings {
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

/* 虚拟电表节点：使用不同颜色与实体电表区分 */
.tree-node.virtual-meter-node {
  background-color: var(--el-color-warning-light-9, #fdf6ec);
  border-radius: 4px;
  padding: 0 4px;
}
.tree-node.virtual-meter-node .node-label {
  color: var(--el-color-warning, #e6a23c);
  font-style: italic;
}
.virtual-meter-tag {
  color: var(--el-color-warning, #e6a23c);
  font-weight: bold;
  font-style: normal;
  margin-right: 2px;
}

/* 分摊子电表节点：使用紫色与实体电表区分 */
.tree-node.allocation-child-node {
  background-color: var(--el-color-primary-light-9, #ecf5ff);
  border-radius: 4px;
  padding: 0 4px;
}
.tree-node.allocation-child-node .node-label {
  color: var(--el-color-primary, #409eff);
}
.allocation-child-tag {
  color: var(--el-color-primary, #409eff);
  font-weight: bold;
  font-style: normal;
  margin-right: 2px;
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

.form-item-tip {
  font-size: 12px;
  color: #909399;
  line-height: 1.4;
  margin-top: 4px;
}

.meter-model-input {
  cursor: pointer;
}

.meter-model-input:hover {
  border-color: #409eff;
}

.select-icon {
  cursor: pointer;
  color: #909399;
}

.select-icon:hover {
  color: #409eff;
}

.tree-node:hover .node-actions {
  display: inline-flex;
}

/* 根节点按钮始终可见 */
.tree-node:has(.node-actions) {
  /* placeholder for specificity */
}
.tree-node:hover .node-actions,
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

.meter-detail-actions {
  margin-top: 20px;
  padding-top: 15px;
  border-top: 1px solid #ebeef5;
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}

.dialog-tip {
  padding: 10px 12px;
  background: #f4f4f5;
  border-radius: 4px;
  margin-bottom: 16px;
  font-size: 13px;
  color: #606266;
}

.dialog-section-title {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 12px;
}

.radio-list {
  padding: 8px 0;
}

.radio-list .radio-item {
  padding: 6px 0 6px 24px;
}

.item-tag {
  display: inline-block;
  margin-left: 8px;
  padding: 2px 6px;
  font-size: 12px;
  color: #909399;
  background: #f4f4f5;
  border-radius: 3px;
}

.meter-list-section {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px dashed #dcdfe6;
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
