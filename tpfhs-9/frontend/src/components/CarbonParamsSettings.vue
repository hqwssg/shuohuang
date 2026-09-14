<!--
  碳排放核算参数设置页面
  功能描述：
    1. 展示碳排放核算相关参数的设置入口
    2. 左侧菜单栏包含：碳排放因子设置、能耗数据自动采集点设置、碳排放模型数据字典设置、参数设置（含能耗三级分类设置）
    3. 点击菜单项可在右侧内容区切换对应的设置表单
  
  组件属性：
    - currentUser (Object): 当前登录用户信息，包含 userId、userName、nickName、deptId
  
  组件事件：
    - back: 返回首页事件
    - logout: 退出登录事件
-->
<template>
  <div class="params-settings-container" :class="{ 'readonly-mode': !canEdit }">
    <div class="header">
      <div class="header-left">
        <el-button @click="$emit('back')" :icon="ArrowLeft" circle />
        <h1>碳排放核算参数设置</h1>
      </div>
      <div class="user-info">
        <span>{{ currentUser?.nickName || currentUser?.userName }}</span>
        <el-button @click="$emit('logout')" link type="danger">退出登录</el-button>
      </div>
    </div>
    <el-alert
      v-if="!canEdit"
      title="当前账号只有查看权限，参数新增、编辑、删除和启停操作不可用"
      type="warning"
      show-icon
      :closable="false"
      class="permission-alert"
    />
    
    <div class="main-content">
      <div class="sidebar">
        <el-menu
          :default-active="activeMenu"
          :default-openeds="['factor', 'collection', 'params-config']"
          @select="handleMenuSelect"
          class="sidebar-menu"
        >
          <el-sub-menu index="factor">
            <template #title>
              <el-icon><DataAnalysis /></el-icon>
              <span>碳排放因子设置</span>
            </template>
            <el-menu-item index="factor-template">
              碳排放因子模版管理
            </el-menu-item>
            <el-menu-item index="electric-factor">
              外购电力排放因子设置
            </el-menu-item>
            <el-menu-item index="fossil-factor">
              化石燃料排放因子设置
            </el-menu-item>
            <el-menu-item index="thermal-factor">
              外购热力排放因子设置
            </el-menu-item>
            <el-sub-menu index="waste-factor">
              <template #title>
                <span>废弃物排放因子设置</span>
              </template>
              <el-menu-item index="solid-waste-factor">
                固体废弃物焚烧排放因子设置
              </el-menu-item>
              <el-menu-item index="wastewater-factor">
                废水处理排放因子设置
              </el-menu-item>
            </el-sub-menu>
          </el-sub-menu>
          
          <el-sub-menu index="collection">
            <template #title>
              <el-icon><Collection /></el-icon>
              <span>能耗数据自动采集点设置</span>
            </template>
            <el-menu-item index="electric-meter">
              电力表设置
            </el-menu-item>
            <el-menu-item index="fossil-fuel-collection">
              化石燃料采集设置
            </el-menu-item>
            <el-menu-item index="purchased-heat-collection">
              外购热能采集设置
            </el-menu-item>
          </el-sub-menu>
          
          <el-menu-item index="data-dict">
            <el-icon><Notebook /></el-icon>
            <span>碳排放模型数据字典设置</span>
          </el-menu-item>

          <el-sub-menu index="params-config">
            <template #title>
              <el-icon><Setting /></el-icon>
              <span>参数设置</span>
            </template>
            <el-menu-item index="energy-category">
              能耗三级分类设置
            </el-menu-item>
            <el-menu-item index="calc-unit-default">
              碳排放核算缺省单位设置
            </el-menu-item>
            <el-menu-item index="unit-conversion">
              碳排放核算单位转换系数设置
            </el-menu-item>
          </el-sub-menu>
        </el-menu>
      </div>
      
      <div class="content">
        <div class="content-header" v-if="activeMenu !== 'electric-meter' && activeMenu !== 'data-dict' && activeMenu !== 'fossil-fuel-collection' && activeMenu !== 'purchased-heat-collection' && activeMenu !== 'energy-category' && activeMenu !== 'calc-unit-default' && activeMenu !== 'unit-conversion' && activeMenu !== 'factor-template' && activeMenu !== 'template-edit'">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/' }">参数设置</el-breadcrumb-item>
            <el-breadcrumb-item>{{ currentMenuTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
          <h2>{{ currentMenuTitle }}</h2>
        </div>
        
        <div class="content-body" :class="{ 'meter-mode': activeMenu === 'electric-meter' || activeMenu === 'fossil-fuel-collection' || activeMenu === 'purchased-heat-collection', 'dict-mode': activeMenu === 'data-dict', 'category-mode': activeMenu === 'energy-category', 'calc-unit-mode': activeMenu === 'calc-unit-default' || activeMenu === 'unit-conversion' || activeMenu === 'factor-template' || activeMenu === 'template-edit' }">
          <!-- 外购电力排放因子设置 -->
          <div v-if="activeMenu === 'electric-factor'" class="factor-content">
            <div class="factor-toolbar">
              <el-input
                v-model="electricSearchKeyword"
                placeholder="按因子名称搜索"
                clearable
                style="width: 250px"
                @keyup.enter="handleElectricSearch"
                @clear="loadElectricFactors"
              />
              <el-button type="primary" @click="handleElectricSearch">搜索</el-button>
              <el-button @click="handleElectricSearchReset">重置</el-button>
              <el-button type="success" @click="handleAddElectricFactor">新增电力排放因子</el-button>
            </div>
            <el-table :data="electricFactorList" style="width: 100%" v-loading="electricLoading" border :max-height="tableMaxHeight">
              <el-table-column prop="factorName" label="碳排放因子名称" min-width="220" show-overflow-tooltip />
              <el-table-column prop="factorValue" label="碳排放因子" width="100" />
              <el-table-column prop="unit" label="单位" width="100" />
              <el-table-column prop="description" label="说明" min-width="320" show-overflow-tooltip />
              <el-table-column prop="updatedAt" label="更新时间" width="180">
                <template #default="{ row }">
                  {{ formatDateTime(row.updatedAt) }}
                </template>
              </el-table-column>
              <el-table-column label="操作" width="105" fixed="right">
                <template #default="{ row }">
                  <el-button link type="primary" @click="handleEditElectricFactor(row)">编辑</el-button>
                  <el-button link type="danger" @click="handleDeleteElectricFactor(row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>

            <!-- 新增/编辑电力排放因子对话框 -->
            <el-dialog
              v-model="electricDialogVisible"
              :title="electricDialogTitle"
              width="550px"
            >
              <el-form :model="electricForm" label-width="120px" :rules="electricFormRules" ref="electricFormRef">
                <el-form-item label="因子名称" prop="factorName">
                  <el-input v-model="electricForm.factorName" placeholder="如：2023年全国电力碳排放因子" />
                </el-form-item>
                <el-form-item label="碳排放因子" prop="factorValue">
                  <el-input-number v-model="electricForm.factorValue" :precision="6" :step="0.0001" :min="0" style="width: 100%" />
                </el-form-item>
                <el-form-item label="单位" prop="unit">
                  <el-select v-model="electricForm.unit" placeholder="请选择单位" filterable style="width: 100%">
                    <el-option v-for="u in electricFactorUnits" :key="u.factorUnitCode" :label="u.factorUnitName" :value="u.factorUnitName" />
                  </el-select>
                </el-form-item>
                <el-form-item label="说明">
                  <el-input v-model="electricForm.description" type="textarea" :rows="3" placeholder="数据来源和适用范围说明" />
                </el-form-item>
              </el-form>
              <template #footer>
                <el-button @click="electricDialogVisible = false">取消</el-button>
                <el-button type="primary" @click="handleSaveElectricFactor">确定</el-button>
              </template>
            </el-dialog>
          </div>
          
          <!-- 化石燃料排放因子设置 -->
          <div v-else-if="activeMenu === 'fossil-factor'" class="factor-content">
            <div class="factor-toolbar">
              <el-input
                v-model="fossilSearchKeyword"
                placeholder="按燃料品种搜索"
                clearable
                style="width: 250px"
                @keyup.enter="handleFossilSearch"
                @clear="loadFossilFactors"
              />
              <el-button type="primary" @click="handleFossilSearch">搜索</el-button>
              <el-button @click="handleFossilSearchReset">重置</el-button>
              <el-button type="success" @click="handleAddFossilFactor">新增化石燃料排放因子</el-button>
            </div>
            <el-table :data="fossilFactorList" style="width: 100%" v-loading="fossilLoading" border :max-height="tableMaxHeight">
              <el-table-column prop="emissionFactorName" label="因子名称" min-width="160" show-overflow-tooltip />
              <el-table-column prop="fuelType" label="燃料品种" width="110" />
              <el-table-column prop="source" label="来源" width="90" />
              <el-table-column prop="unit" label="计量单位" width="100">
                <template #default="{ row }">
                  {{ resolveUnitName(row.unit) }}
                </template>
              </el-table-column>
              <el-table-column prop="lowerHeatingValue" label="低位发热量" width="99" />
              <el-table-column prop="carbonContentPerUnitHeat" label="单位热值含碳量" width="123" />
              <el-table-column prop="fuelOxidationRate" label="氧化率" width="68" />
              <el-table-column prop="emissionFactor" label="碳排放因子" width="95" />
              <el-table-column prop="factorUnit" label="因子单位" width="104" />
              <el-table-column prop="description" label="说明" min-width="200" show-overflow-tooltip />
              <el-table-column prop="updatedAt" label="更新时间" width="138">
                <template #default="{ row }">
                  {{ formatDateTime(row.updatedAt) }}
                </template>
              </el-table-column>
              <el-table-column label="操作" width="105" fixed="right">
                <template #default="{ row }">
                  <el-button link type="primary" @click="handleEditFossilFactor(row)">编辑</el-button>
                  <el-button link type="danger" @click="handleDeleteFossilFactor(row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>

            <!-- 新增/编辑化石燃料排放因子对话框 -->
            <el-dialog
              v-model="fossilDialogVisible"
              :title="fossilDialogTitle"
              width="820px"
            >
              <el-form :model="fossilForm" label-width="130px" :rules="fossilFormRules" ref="fossilFormRef">
                <el-form-item label="因子名称" prop="emissionFactorName">
                  <el-input v-model="fossilForm.emissionFactorName" placeholder="如：烟煤排放因子" />
                </el-form-item>
                <div class="form-row">
                  <el-form-item label="燃料品种" prop="fuelType" class="form-col">
                    <el-input v-model="fossilForm.fuelType" placeholder="如：烟煤" />
                  </el-form-item>
                  <el-form-item label="计量单位" prop="unit" class="form-col">
                    <el-select v-model="fossilForm.unit" @change="handleUnitChange" placeholder="请选择" filterable style="width: 100%">
                      <el-option v-for="u in fossilFuelFactorUnits" :key="u.unitCode" :label="u.unitName" :value="u.unitCode" />
                    </el-select>
                  </el-form-item>
                </div>
                <el-form-item label="来源" prop="source">
                  <el-input v-model="fossilForm.source" placeholder="如：缺省值" />
                </el-form-item>
                <el-form-item label="低位发热量" prop="lowerHeatingValue">
                  <div class="input-with-unit">
                    <el-input-number v-model="fossilForm.lowerHeatingValue" :precision="6" :step="0.001" :min="0" style="flex: 1" />
                    <span class="unit-label">{{ lowerHeatingValueUnit }}</span>
                  </div>
                </el-form-item>
                <div class="form-row">
                  <el-form-item label="单位热值含碳量" prop="carbonContentPerUnitHeat" class="form-col">
                    <div class="input-with-unit">
                      <el-input-number v-model="fossilForm.carbonContentPerUnitHeat" :precision="6" :step="0.001" :min="0" style="flex: 1" />
                      <span class="unit-label">10⁻³ tC/GJ</span>
                    </div>
                  </el-form-item>
                  <el-form-item label="燃料氧化率" prop="fuelOxidationRate" class="form-col">
                    <el-input-number v-model="fossilForm.fuelOxidationRate" :precision="4" :step="0.01" :min="0" :max="1" style="width: 100%" />
                  </el-form-item>
                </div>
                <div class="form-row">
                  <el-form-item label="碳排放因子" prop="emissionFactor" class="form-col">
                    <el-input v-model="fossilForm.emissionFactor" readonly />
                  </el-form-item>
                  <el-form-item label="因子单位" prop="factorUnit" class="form-col">
                    <el-input v-model="fossilForm.factorUnit" readonly />
                  </el-form-item>
                </div>
                <el-form-item label="说明">
                  <el-input v-model="fossilForm.description" type="textarea" :rows="3" placeholder="数据来源和适用范围说明" />
                </el-form-item>
              </el-form>
              <template #footer>
                <div class="dialog-footer">
                  <span class="formula-tip">这里的碳排放因子按以下公式自动计算：碳排放因子=低位发热量*（单位热值含碳量*10⁻³）*燃料氧化率*44/12</span>
                  <div class="footer-buttons">
                    <el-button @click="fossilDialogVisible = false">取消</el-button>
                    <el-button type="primary" @click="handleSaveFossilFactor">确定</el-button>
                  </div>
                </div>
              </template>
            </el-dialog>
          </div>
          
          <!-- 外购热力排放因子设置 -->
          <div v-else-if="activeMenu === 'thermal-factor'" class="factor-content">
            <div class="factor-toolbar">
              <el-input
                v-model="thermalSearchKeyword"
                placeholder="按因子名称搜索"
                clearable
                style="width: 250px"
                @keyup.enter="handleThermalSearch"
                @clear="loadThermalFactors"
              />
              <el-button type="primary" @click="handleThermalSearch">搜索</el-button>
              <el-button @click="handleThermalSearchReset">重置</el-button>
              <el-button type="success" @click="handleAddThermalFactor">新增热力排放因子</el-button>
            </div>
            <el-table :data="thermalFactorList" style="width: 100%" v-loading="thermalLoading" border :max-height="tableMaxHeight">
              <el-table-column prop="emissionFactorName" label="碳排放因子名称" min-width="220" show-overflow-tooltip />
              <el-table-column prop="emissionFactor" label="碳排放因子" width="95" />
              <el-table-column prop="unit" label="单位" width="80" />
              <el-table-column prop="source" label="来源" width="120" />
              <el-table-column prop="description" label="说明" min-width="320" show-overflow-tooltip />
              <el-table-column prop="updatedAt" label="更新时间" width="180">
                <template #default="{ row }">
                  {{ formatDateTime(row.updatedAt) }}
                </template>
              </el-table-column>
              <el-table-column label="操作" width="105" fixed="right">
                <template #default="{ row }">
                  <el-button link type="primary" @click="handleEditThermalFactor(row)">编辑</el-button>
                  <el-button link type="danger" @click="handleDeleteThermalFactor(row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>

            <!-- 新增/编辑热力排放因子弹窗 -->
            <el-dialog
              v-model="thermalDialogVisible"
              :title="thermalDialogTitle"
              width="600px"
            >
              <el-form :model="thermalForm" label-width="140px" :rules="thermalFormRules" ref="thermalFormRef">
                <el-form-item label="因子名称" prop="emissionFactorName">
                  <el-input v-model="thermalForm.emissionFactorName" placeholder="如：全国统一热力排放因子缺省值" />
                </el-form-item>
                <el-form-item label="碳排放因子" prop="emissionFactor">
                  <el-input-number v-model="thermalForm.emissionFactor" :precision="6" :step="0.0001" :min="0" style="width: 100%" />
                </el-form-item>
                <el-form-item label="单位" prop="unit">
                  <el-select v-model="thermalForm.unit" placeholder="请选择单位" style="width: 100%">
                    <el-option label="tCO₂/GJ" value="tCO₂/GJ" />
                    <el-option label="kgCO₂/GJ" value="kgCO₂/GJ" />
                  </el-select>
                </el-form-item>
                <el-form-item label="来源" prop="source">
                  <el-input v-model="thermalForm.source" placeholder="如：缺省值、IPCC指南" />
                </el-form-item>
                <el-form-item label="说明">
                  <el-input v-model="thermalForm.description" type="textarea" :rows="3" placeholder="数据来源和适用范围说明" />
                </el-form-item>
              </el-form>
              <template #footer>
                <el-button @click="thermalDialogVisible = false">取消</el-button>
                <el-button type="primary" @click="handleSaveThermalFactor">确定</el-button>
              </template>
            </el-dialog>
          </div>
          
          <!-- 固体废弃物焚烧排放因子设置 -->
          <div v-else-if="activeMenu === 'solid-waste-factor'" class="factor-content">
            <div class="factor-toolbar">
              <el-input
                v-model="solidWasteSearchKeyword"
                placeholder="按因子名称搜索"
                clearable
                style="width: 250px"
                @keyup.enter="handleSolidWasteSearch"
                @clear="loadSolidWasteFactors"
              />
              <el-button type="primary" @click="handleSolidWasteSearch">搜索</el-button>
              <el-button @click="handleSolidWasteSearchReset">重置</el-button>
              <el-button type="success" @click="handleAddSolidWasteFactor">新增固体废弃物焚烧排放因子</el-button>
            </div>
            <el-table :data="solidWasteFactorList" style="width: 100%" v-loading="solidWasteLoading" border :max-height="tableMaxHeight">
              <el-table-column prop="emissionFactorName" label="因子名称" min-width="180" show-overflow-tooltip />
              <el-table-column prop="wasteType" label="固体废物种类" width="130" />
              <el-table-column prop="ccw" label="碳含量比例" width="95" />
              <el-table-column prop="fcf" label="化石碳比例" width="95" />
              <el-table-column prop="ce" label="燃烧效率" width="81" />
              <el-table-column prop="emissionFactor" label="碳排放因子" width="95" />
              <el-table-column prop="unit" label="单位" width="78" show-overflow-tooltip />
              <el-table-column prop="source" label="来源" width="100" />
              <el-table-column prop="description" label="说明" min-width="200" show-overflow-tooltip />
              <el-table-column prop="updatedAt" label="更新时间" width="140">
                <template #default="{ row }">
                  {{ formatDateTime(row.updatedAt) }}
                </template>
              </el-table-column>
              <el-table-column label="操作" width="105" fixed="right">
                <template #default="{ row }">
                  <el-button link type="primary" @click="handleEditSolidWasteFactor(row)">编辑</el-button>
                  <el-button link type="danger" @click="handleDeleteSolidWasteFactor(row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>

            <!-- 新增/编辑固体废弃物焚烧排放因子弹窗 -->
            <el-dialog
              v-model="solidWasteDialogVisible"
              :title="solidWasteDialogTitle"
              width="680px"
            >
              <el-form :model="solidWasteForm" label-width="150px" :rules="solidWasteFormRules" ref="solidWasteFormRef">
                <el-form-item label="因子名称" prop="emissionFactorName">
                  <el-input v-model="solidWasteForm.emissionFactorName" placeholder="如：生活垃圾焚烧" />
                </el-form-item>
                <el-form-item label="固体废物种类" prop="wasteType">
                  <el-input v-model="solidWasteForm.wasteType" placeholder="如：生活垃圾、危险废物" />
                </el-form-item>
                <div class="form-row">
                  <el-form-item label="碳含量比例 (CCW)" prop="ccw" class="form-col">
                    <el-input-number v-model="solidWasteForm.ccw" :precision="4" :step="0.01" :min="0" :max="1" style="width: 100%" />
                  </el-form-item>
                  <el-form-item label="化石碳比例 (FCF)" prop="fcf" class="form-col">
                    <el-input-number v-model="solidWasteForm.fcf" :precision="4" :step="0.01" :min="0" :max="1" style="width: 100%" />
                  </el-form-item>
                </div>
                <el-form-item label="燃烧效率 (CE)" prop="ce">
                  <el-input-number v-model="solidWasteForm.ce" :precision="4" :step="0.01" :min="0" :max="1" style="width: 100%" />
                </el-form-item>
                <div class="form-row">
                  <el-form-item label="碳排放因子" prop="emissionFactor" class="form-col">
                    <el-input v-model="solidWasteForm.emissionFactor" readonly />
                  </el-form-item>
                  <el-form-item label="单位" prop="unit" class="form-col">
                    <el-select v-model="solidWasteForm.unit" placeholder="请选择单位" style="width: 100%">
                      <el-option label="tCO₂/t" value="tCO₂/t" />
                      <el-option label="kgCO₂/t" value="kgCO₂/t" />
                    </el-select>
                  </el-form-item>
                </div>
                <el-form-item label="来源" prop="source">
                  <el-input v-model="solidWasteForm.source" placeholder="如：缺省值、IPCC指南" />
                </el-form-item>
                <el-form-item label="说明">
                  <el-input v-model="solidWasteForm.description" type="textarea" :rows="3" placeholder="数据来源和适用范围说明" />
                </el-form-item>
              </el-form>
              <template #footer>
                <div class="dialog-footer">
                  <span class="formula-tip">这里的碳排放因子按以下公式自动计算：<br>碳排放因子=碳含量比例(CCW)*化石碳比例(FCF)*燃烧效率(CE)*44/12</span>
                  <div class="footer-buttons">
                    <el-button @click="solidWasteDialogVisible = false">取消</el-button>
                    <el-button type="primary" @click="handleSaveSolidWasteFactor">确定</el-button>
                  </div>
                </div>
              </template>
            </el-dialog>
          </div>
          
          <!-- 废水处理排放因子设置 -->
          <div v-else-if="activeMenu === 'wastewater-factor'" class="factor-content">
            <div class="factor-toolbar">
              <el-input
                v-model="wastewaterSearchKeyword"
                placeholder="按因子名称搜索"
                clearable
                style="width: 250px"
                @keyup.enter="handleWastewaterSearch"
                @clear="loadWastewaterFactors"
              />
              <el-button type="primary" @click="handleWastewaterSearch">搜索</el-button>
              <el-button @click="handleWastewaterSearchReset">重置</el-button>
              <el-button type="success" @click="handleAddWastewaterFactor">新增废水处理排放因子</el-button>
            </div>
            <el-table :data="wastewaterFactorList" style="width: 100%" v-loading="wastewaterLoading" border :max-height="tableMaxHeight" :header-cell-style="{ 'text-align': 'center' }">
              <el-table-column prop="emissionFactorName" label="因子名称" min-width="200" show-overflow-tooltip />
              <el-table-column prop="wastewaterType" label="废水种类" width="150" show-overflow-tooltip />
              <el-table-column prop="od" label="需氧浓度系数(mg/L)" width="110" />
              <el-table-column prop="bo" label="甲烷产生能力(tCH₄/t)" width="110" />
              <el-table-column prop="mcf" label="甲烷修正因子" width="70" />
              <el-table-column prop="gwp" label="GWP" width="60" />
              <el-table-column prop="emissionFactor" label="碳排放因子" width="100" />
              <el-table-column prop="unit" label="单位" width="92" show-overflow-tooltip />
              <el-table-column prop="source" label="来源" width="90" />
              <el-table-column prop="description" label="说明" min-width="200" show-overflow-tooltip />
              <el-table-column prop="updatedAt" label="更新时间" width="138">
                <template #default="{ row }">
                  {{ formatDateTime(row.updatedAt) }}
                </template>
              </el-table-column>
              <el-table-column label="操作" width="105" fixed="right">
                <template #default="{ row }">
                  <el-button link type="primary" @click="handleEditWastewaterFactor(row)">编辑</el-button>
                  <el-button link type="danger" @click="handleDeleteWastewaterFactor(row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>

            <!-- 新增/编辑废水处理排放因子弹窗 -->
            <el-dialog
              v-model="wastewaterDialogVisible"
              :title="wastewaterDialogTitle"
              width="770px"
            >
              <el-form :model="wastewaterForm" label-width="110px" :rules="wastewaterFormRules" ref="wastewaterFormRef">
                <el-form-item label="因子名称" prop="emissionFactorName">
                  <el-input v-model="wastewaterForm.emissionFactorName" placeholder="如：生活污水-化粪池" />
                </el-form-item>
                <el-form-item label="废水种类" prop="wastewaterType">
                  <el-input v-model="wastewaterForm.wastewaterType" placeholder="如：生活污水、机车检修废水" />
                </el-form-item>
                <div class="form-row">
                  <el-form-item label="需氧浓度系数(OD)" prop="od" class="form-col" label-width="150px">
                    <div class="input-with-unit">
                      <el-input-number v-model="wastewaterForm.od" :precision="4" :step="1" :min="0" style="width: 100%" />
                      <span class="unit-label">mg/L</span>
                    </div>
                  </el-form-item>
                  <el-form-item label="甲烷修正因子(MCF)" prop="mcf" class="form-col" label-width="210px">
                    <el-input-number v-model="wastewaterForm.mcf" :precision="4" :step="0.01" :min="0" :max="1" style="width: 100%" />
                  </el-form-item>
                </div>
                <div class="form-row">
                  <el-form-item label="甲烷产生能力(Bo)" prop="bo" class="form-col" label-width="150px">
                    <div class="input-with-unit">
                      <el-input-number v-model="wastewaterForm.bo" :precision="4" :step="0.01" :min="0" style="width: 100%" />
                      <span class="unit-label">tCH₄/tOD</span>
                    </div>
                  </el-form-item>
                  <el-form-item label="全球变暖潜能值(GWP)" prop="gwp" class="form-col" label-width="210px">
                    <el-input-number v-model="wastewaterForm.gwp" :precision="2" :step="1" :min="0" style="width: 100%" />
                  </el-form-item>
                </div>
                <div class="form-row">
                  <el-form-item label="碳排放因子" prop="emissionFactor" class="form-col">
                    <el-input v-model="wastewaterForm.emissionFactor" readonly />
                  </el-form-item>
                  <el-form-item label="单位" prop="unit" class="form-col">
                    <el-select v-model="wastewaterForm.unit" placeholder="请选择单位" filterable style="width: 100%">
                      <el-option v-for="u in wastewaterFactorUnits" :key="u.factorUnitCode" :label="u.factorUnitName" :value="u.factorUnitName" />
                    </el-select>
                  </el-form-item>
                </div>
                <el-form-item label="来源" prop="source">
                  <el-input v-model="wastewaterForm.source" placeholder="如：缺省值、IPCC指南" />
                </el-form-item>
                <el-form-item label="说明">
                  <el-input v-model="wastewaterForm.description" type="textarea" :rows="3" placeholder="数据来源和适用范围说明" />
                </el-form-item>
              </el-form>
              <template #footer>
                <div class="dialog-footer">
                  <span class="formula-tip">这里的碳排放因子按以下公式自动计算：<br>碳排放因子=需氧浓度系数(OD)*甲烷产生能力(Bo)*甲烷修正因子(MCF)*全球变暖潜能值(GWP)/1000000</span>
                  <div class="footer-buttons">
                    <el-button @click="wastewaterDialogVisible = false">取消</el-button>
                    <el-button type="primary" @click="handleSaveWastewaterFactor">确定</el-button>
                  </div>
                </div>
              </template>
            </el-dialog>
          </div>
          
          <!-- 电力表设置 -->
          <ElectricMeterSettings
            v-else-if="activeMenu === 'electric-meter'"
            :current-user="currentUser"
          />
          
          <!-- 化石燃料采集设置 -->
          <FossilFuelCollectionSettings
            v-else-if="activeMenu === 'fossil-fuel-collection'"
            :current-user="currentUser"
          />
          
          <!-- 外购热能采集设置 -->
          <PurchasedHeatCollectionSettings
            v-else-if="activeMenu === 'purchased-heat-collection'"
            :current-user="currentUser"
          />
          
          <!-- 碳排放模型数据字典设置 -->
          <div v-else-if="activeMenu === 'data-dict'" class="dict-content">
            <DataDictSettings />
          </div>

          <!-- 能耗三级分类设置 -->
          <EnergyCategorySettings
            v-else-if="activeMenu === 'energy-category'"
            :current-user="currentUser"
          />

          <!-- 因子模版管理 -->
          <div v-else-if="activeMenu === 'factor-template'" class="calc-unit-content">
            <div class="calc-unit-header">
              <div class="calc-unit-tip">
                <el-icon><InfoFilled /></el-icon>
                <span>管理多个碳排放因子设置库（模版）。每个模版可独立配置各能耗小类的碳排放因子。模版可设为<strong>共享</strong>（所有人可用）或<strong>私有</strong>（仅创建人可用），缺省共享。</span>
              </div>
              <div>
                <el-button type="success" :icon="Plus" @click="openAddTemplate">新增模版</el-button>
                <el-button type="primary" :icon="Refresh" :loading="factorTemplateLoading" @click="loadFactorTemplates">重新加载</el-button>
              </div>
            </div>
            <el-table :data="factorTemplateList" style="width: 100%" v-loading="factorTemplateLoading" border :max-height="tableMaxHeight" stripe>
              <el-table-column type="index" label="序号" width="60" align="center" />
              <el-table-column prop="templateName" label="模版名称" min-width="180" show-overflow-tooltip />
              <el-table-column prop="templateDescription" label="模版说明" min-width="250" show-overflow-tooltip />
              <el-table-column label="共享/私有" width="110" align="center">
                <template #default="{ row }">
                  <el-tag :type="row.isShared === 1 ? 'success' : 'info'" size="small">
                    {{ row.isShared === 1 ? '共享' : '私有' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="启用/停用" width="110" align="center">
                <template #default="{ row }">
                  <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
                    {{ row.status === 1 ? '启用' : '停用' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="createdAt" label="创建时间" width="170" align="center" />
              <el-table-column label="操作" width="260" align="center" fixed="right">
                <template #default="{ row }">
                  <el-button type="primary" size="small" @click="openEditTemplateFactors(row)">编辑因子</el-button>
                  <el-button type="warning" size="small" @click="openEditTemplateInfo(row)">编辑信息</el-button>
                  <el-button type="danger" size="small" @click="handleDeleteTemplate(row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <!-- 编辑模版因子设置（复用缺省因子设置操作方式） -->
          <div v-else-if="activeMenu === 'template-edit'" class="calc-unit-content">
            <div class="calc-unit-header">
              <div class="calc-unit-tip">
                <el-icon><InfoFilled /></el-icon>
                <span>正在编辑因子模版「<strong>{{ currentEditingTemplate?.templateName }}</strong>」的碳排放因子设置：点击"添加能耗子类"选择小类后，再从对应的因子库中选择一条因子作为该模版的缺省值。</span>
              </div>
              <div>
                <el-button :icon="ArrowLeft" @click="backToTemplateList">返回模版列表</el-button>
                <el-button type="success" :icon="Plus" @click="openAddDefaultFactor">添加能耗子类</el-button>
                <el-button type="primary" :icon="Refresh" :loading="defaultFactorLoading" @click="loadTemplateFactors(currentEditingTemplate.id)">重新加载</el-button>
              </div>
            </div>
            <el-table :data="defaultFactorList" style="width: 100%" v-loading="defaultFactorLoading" border :max-height="tableMaxHeight" stripe>
              <el-table-column type="index" label="序号" width="60" align="center" />
              <el-table-column prop="subcategoryCode" label="小类编码" width="110" align="center" />
              <el-table-column prop="subcategoryName" label="能耗小类" min-width="150" show-overflow-tooltip />
              <el-table-column prop="factorName" label="缺省因子名称" min-width="200" show-overflow-tooltip>
                <template #default="{ row }">
                  <span v-if="row.factorName">{{ row.factorName }}</span>
                  <el-tag v-else type="warning" size="small">未选择因子</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="factorValue" label="因子值" width="130" align="right" />
              <el-table-column prop="factorUnit" label="单位" width="140" show-overflow-tooltip />
              <el-table-column prop="factorDescription" label="因子说明" min-width="220" show-overflow-tooltip />
              <el-table-column label="操作" width="180" align="center" fixed="right">
                <template #default="{ row }">
                  <el-button type="primary" size="small" @click="openChangeFactor(row)">更换因子</el-button>
                  <el-button type="danger" size="small" @click="handleDeleteDefaultFactor(row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>

            <!-- 添加能耗子类对话框 -->
            <el-dialog v-model="addSubcategoryDialogVisible" title="添加能耗子类" width="480px">
              <el-form :model="addSubcategoryForm" label-width="110px">
                <el-form-item label="能耗大类" required>
                  <el-select
                    v-model="addSubcategoryForm.parentCode"
                    placeholder="请先选择能耗大类"
                    filterable
                    style="width: 100%"
                    @change="handleAddCategoryChange"
                  >
                    <el-option
                      v-for="c in availableCategories"
                      :key="c.code"
                      :label="c.value"
                      :value="c.code"
                    />
                  </el-select>
                </el-form-item>
                <el-form-item label="能耗小类" required>
                  <el-select
                    v-model="addSubcategoryForm.subcategoryCode"
                    :placeholder="addSubcategoryForm.parentCode ? '请选择能耗小类' : '请先选择能耗大类'"
                    :disabled="!addSubcategoryForm.parentCode"
                    filterable
                    style="width: 100%"
                  >
                    <el-option
                      v-for="d in availableSubcategories"
                      :key="d.code"
                      :label="d.value + '（' + d.code + '）'"
                      :value="d.code"
                    />
                  </el-select>
                </el-form-item>
              </el-form>
              <template #footer>
                <el-button @click="addSubcategoryDialogVisible = false">取消</el-button>
                <el-button type="primary" @click="confirmAddSubcategory">下一步：选择因子</el-button>
              </template>
            </el-dialog>

            <!-- 因子选择对话框（复用公共组件） -->
            <CarbonEmissionFactorDialog
              v-model:visible="factorDialogVisible"
              :emission-category="factorDialogCategory"
              :emission-subcategory="factorDialogSubcategory"
              @select="handleFactorSelected"
            />
          </div>

          <div v-else-if="activeMenu === 'calc-unit-default'" class="calc-unit-content">
            <div class="calc-unit-header">
              <div class="calc-unit-tip">
                <el-icon><InfoFilled /></el-icon>
                <span>为每种能耗小类设置“核算汇总单位”和“报告单位”。两者之间的换算系数不再在此处填写，由<strong>计量单位转换系数表（emission_unit_conversion）</strong>统一提供，避免双源数据不一致。单位编码必须为标准单位（emission_unit_standard.unit_code）。行数据固定，不允许新增/删除。</span>
              </div>
              <el-button type="primary" :icon="Refresh" :loading="calcUnitLoading" @click="loadCalcUnitDefaults">重新加载</el-button>
            </div>
            <el-table :data="calcUnitList" style="width: 100%" v-loading="calcUnitLoading" border :max-height="tableMaxHeight" stripe>
              <el-table-column type="index" label="序号" width="60" align="center" />
              <el-table-column prop="subcategoryCode" label="小类编码" width="110" align="center" />
              <el-table-column prop="subcategoryName" label="能耗小类" min-width="170" show-overflow-tooltip />
              <el-table-column label="核算单位（数据汇总时统一）" width="240">
                <template #default="{ row }">
                  <el-select v-model="row.calculationUnit" placeholder="请选择" filterable style="width: 100%">
                    <el-option v-for="u in allStandardUnits" :key="'c_' + u.unitCode" :label="u.unitName" :value="u.unitCode" />
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="报告单位（核算报告展示）" width="240">
                <template #default="{ row }">
                  <el-select v-model="row.reportUnit" placeholder="请选择" filterable style="width: 100%">
                    <el-option v-for="u in allStandardUnits" :key="'r_' + u.unitCode" :label="u.unitName" :value="u.unitCode" />
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column prop="remark" label="备注说明" min-width="240" show-overflow-tooltip>
                <template #default="{ row }">
                  <el-input v-model="row.remark" maxlength="500" show-word-limit placeholder="可选" />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="130" align="center" fixed="right">
                <template #default="{ row }">
                  <el-button type="primary" size="small" @click="saveCalcUnitRow(row)">保存</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <!-- 碳排放核算单位转换系数设置 -->
          <div v-else-if="activeMenu === 'unit-conversion'" class="calc-unit-content">
            <div class="calc-unit-header">
              <div class="calc-unit-tip">
                <el-icon><InfoFilled /></el-icon>
                <span>为每种能耗小类维护成对的单位转换系数（如汽油 L↔t，双向显式存储）。<strong>源单位、目标单位必须从下拉框选择标准单位，禁止人工录入</strong>，避免标准不统一导致后续计算失败。转换公式：目标值 = 源值 × 转换系数。</span>
              </div>
              <div style="display:flex;align-items:center;gap:10px;">
                <el-select v-model="conversionFilterSubcategory" placeholder="按能耗小类筛选" clearable filterable style="width:220px" @change="filterConversions">
                  <el-option v-for="item in subcategoryDictItems" :key="item.code" :label="item.value" :value="item.code" />
                </el-select>
                <el-button type="primary" :icon="Plus" @click="openConversionDialog(null)">新增转换系数</el-button>
                <el-button :icon="Refresh" :loading="unitConversionLoading" @click="loadUnitConversions">重新加载</el-button>
              </div>
            </div>
            <el-table :data="filteredConversionList" style="width: 100%" v-loading="unitConversionLoading" border :max-height="tableMaxHeight" stripe>
              <el-table-column type="index" label="序号" width="60" align="center" />
              <el-table-column label="能耗小类" min-width="150">
                <template #default="{ row }">
                  <span>{{ resolveSubcategoryName(row.subcategoryCode) }}（{{ row.subcategoryCode }}）</span>
                </template>
              </el-table-column>
              <el-table-column label="源单位" width="160">
                <template #default="{ row }">
                  <span>{{ resolveUnitName(row.fromUnitCode) }}（{{ row.fromUnitCode }}）</span>
                </template>
              </el-table-column>
              <el-table-column label="目标单位" width="160">
                <template #default="{ row }">
                  <span>{{ resolveUnitName(row.toUnitCode) }}（{{ row.toUnitCode }}）</span>
                </template>
              </el-table-column>
              <el-table-column prop="conversionFactor" label="转换系数" width="140" align="right" />
              <el-table-column prop="remark" label="备注说明" min-width="220" show-overflow-tooltip />
              <el-table-column label="操作" width="160" align="center" fixed="right">
                <template #default="{ row }">
                  <el-button type="primary" size="small" @click="openConversionDialog(row)">编辑</el-button>
                  <el-button type="danger" size="small" @click="deleteConversionRow(row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <!-- 默认显示 -->
          <div v-else class="default-content">
            <el-empty description="请从左侧菜单选择要设置的参数" />
          </div>

          <!-- 模版信息对话框（共享：factor-template 和 template-edit 视图共用） -->
          <el-dialog v-model="templateDialogVisible" :title="editingTemplateId ? '编辑模版信息' : '新增模版'" width="520px" append-to-body>
            <el-form :model="templateForm" label-width="100px">
              <el-form-item label="模版名称" required>
                <el-input v-model="templateForm.templateName" placeholder="请输入模版名称" maxlength="200" show-word-limit />
              </el-form-item>
              <el-form-item label="模版说明">
                <el-input v-model="templateForm.templateDescription" type="textarea" :rows="3" placeholder="模版用途说明（可选）" maxlength="1000" show-word-limit />
              </el-form-item>
              <el-form-item label="是否共享">
                <el-switch v-model="templateForm.isShared" :active-value="1" :inactive-value="0" active-text="共享" inactive-text="私有" />
              </el-form-item>
              <el-form-item v-if="editingTemplateId" label="启用状态">
                <el-switch v-model="templateForm.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="停用" />
              </el-form-item>
            </el-form>
            <template #footer>
              <el-button @click="templateDialogVisible = false">取消</el-button>
              <el-button type="primary" @click="confirmSaveTemplate">保存</el-button>
            </template>
          </el-dialog>

          <!-- 单位转换系数新增/编辑对话框 -->
          <el-dialog v-model="conversionDialogVisible" :title="editingConversionId ? '编辑单位转换系数' : '新增单位转换系数'" width="540px" append-to-body>
            <el-form :model="conversionForm" label-width="120px">
              <el-form-item label="能耗小类" required>
                <el-select v-model="conversionForm.subcategoryCode" placeholder="请选择能耗小类" filterable :disabled="!!editingConversionId" style="width: 100%">
                  <el-option v-for="item in subcategoryDictItems" :key="item.code" :label="item.value" :value="item.code" />
                </el-select>
              </el-form-item>
              <el-form-item label="源单位" required>
                <el-select v-model="conversionForm.fromUnitCode" placeholder="请选择源单位" filterable style="width: 100%">
                  <el-option v-for="u in allStandardUnits" :key="'fc_' + u.unitCode" :label="u.unitName" :value="u.unitCode" />
                </el-select>
              </el-form-item>
              <el-form-item label="目标单位" required>
                <el-select v-model="conversionForm.toUnitCode" placeholder="请选择目标单位" filterable style="width: 100%">
                  <el-option v-for="u in allStandardUnits" :key="'tc_' + u.unitCode" :label="u.unitName" :value="u.unitCode" />
                </el-select>
              </el-form-item>
              <el-form-item label="转换系数" required>
                <el-input-number v-model="conversionForm.conversionFactor" :precision="8" :step="0.0001" :controls="false" style="width: 100%" placeholder="目标值 = 源值 × 转换系数" />
              </el-form-item>
              <el-form-item label="备注说明">
                <el-input v-model="conversionForm.remark" type="textarea" :rows="2" maxlength="500" show-word-limit placeholder="可选" />
              </el-form-item>
            </el-form>
            <template #footer>
              <el-button @click="conversionDialogVisible = false">取消</el-button>
              <el-button type="primary" :loading="conversionSaving" @click="confirmSaveConversion">保存</el-button>
            </template>
          </el-dialog>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { ArrowLeft, DataAnalysis, Collection, Notebook, Setting, InfoFilled, Refresh, Plus } from '@element-plus/icons-vue'
import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import ElectricMeterSettings from './ElectricMeterSettings.vue'
import DataDictSettings from './DataDictSettings.vue'
import FossilFuelCollectionSettings from './FossilFuelCollectionSettings.vue'
import PurchasedHeatCollectionSettings from './PurchasedHeatCollectionSettings.vue'
import EnergyCategorySettings from './EnergyCategorySettings.vue'
import CarbonEmissionFactorDialog from './CarbonEmissionFactorDialog.vue'
import { calcUnitDefaultApi, unitApi, factorUnitApi, defaultFactorApi, factorTemplateApi, unitConversionApi } from '../api/auth'
import { hasPermission } from '../utils/permissions'

const props = defineProps({
  /**
   * 当前登录用户信息
   * @property {number} userId - 用户ID
   * @property {string} userName - 用户名
   * @property {string} nickName - 用户昵称
   * @property {number} deptId - 部门ID
   */
  currentUser: {
    type: Object,
    required: true
  },
  /**
   * 从核算模版管理页跳转"编辑因子"时携带的因子模版ID
   * 传入后自动进入"碳排放因子模版管理"并打开该因子模版的因子编辑界面
   */
  initialFactorTemplateId: {
    type: [Number, String],
    default: null
  }
})

const emit = defineEmits([
  /**
   * 返回首页事件
   */
  'back',
  /**
   * 退出登录事件
   */
  'logout'
])

const canEdit = computed(() => hasPermission(props.currentUser, 'carbon:params:edit') || hasPermission(props.currentUser, 'carbon:params:collection:edit'))

/**
 * 当前激活的菜单项
 */
const activeMenu = ref('electric-factor')

/**
 * 菜单标题映射
 */
const menuTitleMap = {
  'factor-template': '碳排放因子模版管理',
  'template-edit': '编辑因子模版',
  'electric-factor': '外购电力排放因子设置',
  'fossil-factor': '化石燃料排放因子设置',
  'thermal-factor': '外购热力排放因子设置',
  'solid-waste-factor': '固体废弃物焚烧排放因子设置',
  'wastewater-factor': '废水处理排放因子设置',
  'electric-meter': '电力表设置',
  'fossil-fuel-collection': '化石燃料采集设置',
  'purchased-heat-collection': '外购热能采集设置',
  'data-dict': '碳排放模型数据字典设置',
  'energy-category': '能耗三级分类设置',
  'calc-unit-default': '碳排放核算缺省单位设置',
  'unit-conversion': '碳排放核算单位转换系数设置'
}

/**
 * 当前菜单标题
 */
const currentMenuTitle = computed(() => menuTitleMap[activeMenu.value] || '参数设置')

/**
 * 表格最大高度（用于纵向滚动，表头固定）
 * 窗口高度减去顶部导航(60)、内容区padding(40)、面包屑标题(50)、工具栏(50)、底部边距(60)等
 */
const tableMaxHeight = computed(() => Math.max(window.innerHeight - 280, 300))

/**
 * 处理菜单选择
 * @param {string} index - 选中菜单项的索引
 */
const handleMenuSelect = (index) => {
  activeMenu.value = index
  if (index === 'calc-unit-default') {
    loadCalcUnitDefaults()
  }
  if (index === 'unit-conversion') {
    loadUnitConversions()
  }
  if (index === 'factor-template') {
    loadFactorTemplates()
  }
}

// ============ 因子模版管理 ============

const factorTemplateLoading = ref(false)
const factorTemplateList = ref([])
const templateDialogVisible = ref(false)
const templateForm = ref({ templateName: '', templateDescription: '', isShared: 1, status: 1 })
const editingTemplateId = ref(null)
const currentEditingTemplate = ref(null)    // 当前正在编辑因子的模版对象
const currentTemplateId = ref(null)         // null=系统缺省模式，非null=模版编辑模式

/**
 * 加载全部因子模版
 */
async function loadFactorTemplates() {
  factorTemplateLoading.value = true
  try {
    const res = await factorTemplateApi.listAll()
    factorTemplateList.value = res.data || []
  } catch (err) {
    console.error('加载因子模版失败', err)
    ElMessage.error('加载因子模版失败：' + (err.response?.data?.message || err.message))
  } finally {
    factorTemplateLoading.value = false
  }
}

/**
 * 打开新增模版对话框
 */
function openAddTemplate() {
  editingTemplateId.value = null
  templateForm.value = { templateName: '', templateDescription: '', isShared: 1, status: 1 }
  templateDialogVisible.value = true
}

/**
 * 打开编辑模版信息对话框
 */
function openEditTemplateInfo(row) {
  editingTemplateId.value = row.id
  templateForm.value = {
    templateName: row.templateName,
    templateDescription: row.templateDescription || '',
    isShared: row.isShared,
    status: row.status
  }
  templateDialogVisible.value = true
}

/**
 * 保存模版（新增或编辑信息）
 */
async function confirmSaveTemplate() {
  if (!templateForm.value.templateName?.trim()) {
    ElMessage.warning('请输入模版名称')
    return
  }
  try {
    if (editingTemplateId.value) {
      const res = await factorTemplateApi.update(editingTemplateId.value, {
        templateName: templateForm.value.templateName.trim(),
        templateDescription: templateForm.value.templateDescription,
        isShared: templateForm.value.isShared,
        status: templateForm.value.status,
        updatedBy: props.currentUser?.userId
      })
      if (res.data?.success === false) {
        ElMessage.error(res.data.message || '更新失败')
        return
      }
      ElMessage.success('模版信息已更新')
    } else {
      const res = await factorTemplateApi.create({
        templateName: templateForm.value.templateName.trim(),
        templateDescription: templateForm.value.templateDescription,
        isShared: templateForm.value.isShared,
        createdBy: props.currentUser?.userId
      })
      if (res.data?.success === false) {
        ElMessage.error(res.data.message || '新增失败')
        return
      }
      ElMessage.success('模版已创建，请点击"编辑因子"配置碳排放因子')
    }
    templateDialogVisible.value = false
    await loadFactorTemplates()
  } catch (err) {
    console.error('保存模版失败', err)
    ElMessage.error('保存失败：' + (err.response?.data?.message || err.message))
  }
}

/**
 * 切换模版启用/停用状态
 */
async function handleToggleTemplateStatus(row) {
  try {
    const res = await factorTemplateApi.toggleStatus(row.id, props.currentUser?.userId)
    if (res.data?.success === false) {
      ElMessage.error(res.data.message || '操作失败')
      return
    }
    ElMessage.success(row.status === 1 ? '已停用' : '已启用')
    await loadFactorTemplates()
  } catch (err) {
    console.error('切换模版状态失败', err)
    ElMessage.error('操作失败：' + (err.response?.data?.message || err.message))
  }
}

/**
 * 切换模版共享/私有状态
 */
async function handleToggleTemplateShared(row) {
  try {
    const res = await factorTemplateApi.toggleShared(row.id, props.currentUser?.userId)
    if (res.data?.success === false) {
      ElMessage.error(res.data.message || '操作失败')
      return
    }
    ElMessage.success(row.isShared === 1 ? '已设为私有' : '已设为共享')
    await loadFactorTemplates()
  } catch (err) {
    console.error('切换模版共享状态失败', err)
    ElMessage.error('操作失败：' + (err.response?.data?.message || err.message))
  }
}

/**
 * 删除模版（先级联清理该模版下的缺省因子，再删模版）
 */
async function handleDeleteTemplate(row) {
  try {
    await ElMessageBox.confirm(
      `确定删除模版"${row.templateName}"吗？该模版下已配置的碳排放因子将一并删除，此操作不可撤销。`,
      '删除确认',
      { type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  try {
    // 先清理该模版下的缺省因子
    await defaultFactorApi.removeByTemplate(row.id)
    // 再删除模版
    const res = await factorTemplateApi.remove(row.id)
    if (res.data?.success === false) {
      ElMessage.error(res.data.message || '删除失败')
      return
    }
    ElMessage.success('模版已删除')
    await loadFactorTemplates()
  } catch (err) {
    console.error('删除模版失败', err)
    ElMessage.error('删除失败：' + (err.response?.data?.message || err.message))
  }
}

/**
 * 打开模版因子编辑界面
 */
function openEditTemplateFactors(row) {
  currentEditingTemplate.value = row
  currentTemplateId.value = row.id
  activeMenu.value = 'template-edit'
  loadTemplateFactors(row.id)
}

/**
 * 返回模版列表
 */
function backToTemplateList() {
  currentEditingTemplate.value = null
  currentTemplateId.value = null
  activeMenu.value = 'factor-template'
}

/**
 * 加载模版下的缺省因子
 */
async function loadTemplateFactors(templateId) {
  defaultFactorLoading.value = true
  try {
    const [factorRes, dictRes] = await Promise.all([
      defaultFactorApi.listByTemplate(templateId),
      axios.get('/api/data-dict/items/emission_subcategory')
    ])
    defaultFactorList.value = factorRes.data || []
    subcategoryDictItems.value = dictRes.data || []
  } catch (err) {
    console.error('加载模版因子失败', err)
    ElMessage.error('加载模版因子失败：' + (err.response?.data?.message || err.message))
  } finally {
    defaultFactorLoading.value = false
  }
}

// ============ 系统缺省碳排放因子设置 ============

const defaultFactorLoading = ref(false)
const defaultFactorList = ref([])          // 已配置的缺省因子列表
const subcategoryDictItems = ref([])       // emission_subcategory 字典项（VO 字段：code/value/parentCode）
const addSubcategoryDialogVisible = ref(false)
const addSubcategoryForm = ref({ parentCode: '', subcategoryCode: '' })

/**
 * 能耗大类编码 -> 中文名称（与字典 emission_category 一致）
 */
const CATEGORY_NAME_MAP = {
  PE: '购入的电力',
  PH: '购入的热力',
  FF: '化石燃料',
  WT: '废弃物处理',
  EP: '输出的电力'
}
const factorDialogVisible = ref(false)
const factorDialogCategory = ref('')       // 因子选择对话框入参：中文大类名
const factorDialogSubcategory = ref('')    // 因子选择对话框入参：中文小类/燃料名
const editingDefaultFactorId = ref(null)   // null=新增流程，否则为更换因子的记录 id
const pendingSubcategory = ref(null)       // 新增流程中待绑定因子的小类字典项

/**
 * 小类编码 -> 因子库映射
 * 返回 { factorSource, categoryName, subcategoryName }
 * categoryName/subcategoryName 对应因子选择器 API 所需的中文参数
 */
function resolveFactorLibrary(code, itemValue) {
  const name = itemValue || ''
  if (code.startsWith('PE_')) {
    return { factorSource: 'ELECTRICITY', categoryName: '购入的电力', subcategoryName: '' }
  }
  if (code.startsWith('FF_')) {
    // 化石燃料：选择器按燃料品种中文名称过滤（如 柴油）
    return { factorSource: 'FOSSIL', categoryName: '化石燃料', subcategoryName: name }
  }
  if (code.startsWith('PH_')) {
    return { factorSource: 'THERMAL', categoryName: '购入的热力', subcategoryName: name }
  }
  if (code === 'WT_SW') {
    return { factorSource: 'WASTE_INCINERATION', categoryName: '废弃物处理', subcategoryName: '固体废弃物处理排放' }
  }
  if (code === 'WT_WW') {
    return { factorSource: 'WASTEWATER', categoryName: '废弃物处理', subcategoryName: '废水处理排放' }
  }
  return null
}

/**
 * 可选大类：存在"尚未配置缺省因子且有对应因子库"的小类的大类
 * （大类名称取 emission_category 字典含义，与 CATEGORY_NAME_MAP 一致）
 */
const availableCategories = computed(() => {
  const used = new Set(defaultFactorList.value.map(r => r.subcategoryCode))
  const codes = new Set()
  subcategoryDictItems.value.forEach(d => {
    if (d.code && d.parentCode && !used.has(d.code) && resolveFactorLibrary(d.code, d.value)) {
      codes.add(d.parentCode)
    }
  })
  return Array.from(codes).map(code => ({
    code,
    value: CATEGORY_NAME_MAP[code] || code
  }))
})

/**
 * 可选小类：当前所选大类下、字典中尚未配置缺省因子的小类
 */
const availableSubcategories = computed(() => {
  const parent = addSubcategoryForm.value.parentCode
  if (!parent) return []
  const used = new Set(defaultFactorList.value.map(r => r.subcategoryCode))
  return subcategoryDictItems.value
    .filter(d => d.code && d.parentCode === parent && !used.has(d.code))
    .filter(d => resolveFactorLibrary(d.code, d.value))
})

/**
 * 切换大类时清空已选小类
 */
function handleAddCategoryChange() {
  addSubcategoryForm.value.subcategoryCode = ''
}

/**
 * 加载已配置的系统缺省因子
 */
async function loadDefaultFactors() {
  currentTemplateId.value = null
  defaultFactorLoading.value = true
  try {
    const [factorRes, dictRes] = await Promise.all([
      defaultFactorApi.listAll(),
      axios.get('/api/data-dict/items/emission_subcategory')
    ])
    defaultFactorList.value = factorRes.data || []
    subcategoryDictItems.value = dictRes.data || []
  } catch (err) {
    console.error('加载系统缺省因子失败', err)
    ElMessage.error('加载系统缺省因子失败：' + (err.response?.data?.message || err.message))
  } finally {
    defaultFactorLoading.value = false
  }
}

/**
 * 打开"添加能耗子类"对话框
 */
function openAddDefaultFactor() {
  const open = () => {
    addSubcategoryForm.value = { parentCode: '', subcategoryCode: '' }
    addSubcategoryDialogVisible.value = true
  }
  if (subcategoryDictItems.value.length === 0) {
    loadDefaultFactors().then(open)
    return
  }
  open()
}

/**
 * 确认添加小类 -> 进入因子选择对话框
 */
function confirmAddSubcategory() {
  const { parentCode, subcategoryCode } = addSubcategoryForm.value
  if (!parentCode) {
    ElMessage.warning('请先选择能耗大类')
    return
  }
  if (!subcategoryCode) {
    ElMessage.warning('请选择能耗小类')
    return
  }
  const dictItem = subcategoryDictItems.value.find(d => d.code === subcategoryCode)
  if (!dictItem) {
    ElMessage.warning('小类信息不存在')
    return
  }
  const lib = resolveFactorLibrary(subcategoryCode, dictItem.value)
  if (!lib) {
    ElMessage.warning('该小类暂无可选因子库')
    return
  }
  pendingSubcategory.value = dictItem
  editingDefaultFactorId.value = null
  addSubcategoryDialogVisible.value = false
  factorDialogCategory.value = lib.categoryName
  factorDialogSubcategory.value = lib.subcategoryName
  factorDialogVisible.value = true
}

/**
 * 为已配置行更换因子：直接打开因子选择对话框
 */
function openChangeFactor(row) {
  const lib = resolveFactorLibrary(row.subcategoryCode, row.subcategoryName)
  if (!lib) {
    ElMessage.warning('该小类暂无可选因子库')
    return
  }
  pendingSubcategory.value = null
  editingDefaultFactorId.value = row.id
  factorDialogCategory.value = lib.categoryName
  factorDialogSubcategory.value = lib.subcategoryName || row.subcategoryName || ''
  factorDialogVisible.value = true
}

/**
 * 因子选择对话框确认回调
 * @param {Object} sel { factorValue, description, factorId, factorName, factorUnit }
 */
async function handleFactorSelected(sel) {
  factorDialogVisible.value = false
  try {
    if (editingDefaultFactorId.value) {
      // 更换因子：update（factorSource 不变，后端保留原值）
      const res = await defaultFactorApi.update(editingDefaultFactorId.value, {
        factorId: sel.factorId,
        factorName: sel.factorName,
        factorValue: sel.factorValue,
        factorUnit: sel.factorUnit,
        factorDescription: sel.description,
        updatedBy: props.currentUser?.userId
      })
      if (res.data?.success === false) {
        ElMessage.error(res.data.message || '更换因子失败')
        return
      }
      ElMessage.success('缺省因子已更新')
    } else {
      // 新增：create（小类 + 因子一起入库）
      const dictItem = pendingSubcategory.value
      const lib = resolveFactorLibrary(dictItem.code, dictItem.value)
      const res = await defaultFactorApi.create({
        templateId: currentTemplateId.value,
        subcategoryCode: dictItem.code,
        subcategoryName: dictItem.value,
        factorSource: lib.factorSource,
        factorId: sel.factorId,
        factorName: sel.factorName,
        factorValue: sel.factorValue,
        factorUnit: sel.factorUnit,
        factorDescription: sel.description,
        createdBy: props.currentUser?.userId
      })
      if (res.data?.success === false) {
        ElMessage.error(res.data.message || '添加失败')
        return
      }
      ElMessage.success(`已为"${dictItem.value}"设置缺省因子`)
    }
    pendingSubcategory.value = null
    editingDefaultFactorId.value = null
    // 根据当前模式重新加载对应的因子列表
    if (currentTemplateId.value) {
      await loadTemplateFactors(currentTemplateId.value)
    } else {
      await loadDefaultFactors()
    }
  } catch (err) {
    console.error('保存缺省因子失败', err)
    ElMessage.error('保存失败：' + (err.response?.data?.message || err.message))
  }
}

/**
 * 删除一条缺省因子配置
 */
async function handleDeleteDefaultFactor(row) {
  try {
    await ElMessageBox.confirm(
      `确定删除"${row.subcategoryName}"的系统缺省碳排放因子配置吗？删除后该小类将没有缺省因子。`,
      '删除确认',
      { type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  try {
    const res = await defaultFactorApi.remove(row.id)
    if (res.data?.success === false) {
      ElMessage.error(res.data.message || '删除失败')
      return
    }
    ElMessage.success('删除成功')
    if (currentTemplateId.value) {
      await loadTemplateFactors(currentTemplateId.value)
    } else {
      await loadDefaultFactors()
    }
  } catch (err) {
    console.error('删除缺省因子失败', err)
    ElMessage.error('删除失败：' + (err.response?.data?.message || err.message))
  }
}

// ============ 碳排放核算缺省单位设置 ============

const calcUnitLoading = ref(false)
const calcUnitList = ref([])
const allStandardUnits = ref([])
const fossilFuelFactorUnits = ref([])
// 因子单位列表（按小类从 emission_factor_unit 表加载）
const electricFactorUnits = ref([])
const thermalFactorUnits = ref([])
const solidWasteFactorUnits = ref([])
const wastewaterFactorUnits = ref([])

/**
 * 加载全部标准单位（emission_unit_standard），供核算单位/报告单位下拉使用
 */
async function loadAllStandardUnits() {
  try {
    const res = await unitApi.listAllStandards()
    allStandardUnits.value = res.data || []
    // 化石燃料因子计量单位：固体燃料 + 气体燃料类合并（t/kg/g + m3/wan_m3/Nm3/wan_Nm3）
    fossilFuelFactorUnits.value = (res.data || []).filter(u =>
      u.unitCategory === 'SOLID_FUEL' || u.unitCategory === 'GAS_FUEL'
    )
  } catch (err) {
    console.error('加载标准单位失败', err)
    ElMessage.error('加载标准单位失败：' + (err.response?.data?.message || err.message))
  }
}

/**
 * 加载各小类因子单位（emission_factor_unit），供因子单位下拉使用
 * 按小类限定：电力 PE_PF、热力 PH_HD、固体废弃物 WT_SW、废水 WT_WW
 * 下拉 label=factorUnitName、value=factorUnitName（与历史数据兼容）
 */
async function loadFactorUnits() {
  try {
    const [elec, therm, sw, ww] = await Promise.all([
      factorUnitApi.getBySubcategory('PE_PF'),
      factorUnitApi.getBySubcategory('PH_HD'),
      factorUnitApi.getBySubcategory('WT_SW'),
      factorUnitApi.getBySubcategory('WT_WW')
    ])
    electricFactorUnits.value = elec.data || []
    thermalFactorUnits.value = therm.data || []
    solidWasteFactorUnits.value = sw.data || []
    wastewaterFactorUnits.value = ww.data || []
  } catch (err) {
    console.error('加载因子单位失败', err)
  }
}

/**
 * 根据 unit_code 查询 unit_name（用于表格显示）
 * 兼容历史自由文本数据：找不到映射时原样返回
 */
function resolveUnitName(code) {
  if (!code) return ''
  const u = allStandardUnits.value.find(x => x.unitCode === code)
  return u ? u.unitName : code
}

/**
 * 加载全部缺省单位设置
 */
async function loadCalcUnitDefaults() {
  calcUnitLoading.value = true
  try {
    if (allStandardUnits.value.length === 0) {
      await loadAllStandardUnits()
    }
    const res = await calcUnitDefaultApi.listAll()
    calcUnitList.value = res.data || []
  } catch (err) {
    console.error('加载缺省单位设置失败', err)
    ElMessage.error('加载缺省单位设置失败：' + (err.response?.data?.message || err.message))
  } finally {
    calcUnitLoading.value = false
  }
}

/**
 * 保存单行缺省单位设置
 */
async function saveCalcUnitRow(row) {
  if (!row.calculationUnit || !row.calculationUnit.trim()) {
    ElMessage.warning('核算单位不能为空')
    return
  }
  if (!row.reportUnit || !row.reportUnit.trim()) {
    ElMessage.warning('报告单位不能为空')
    return
  }
  try {
    const res = await calcUnitDefaultApi.update(row.subcategoryCode, {
      calculationUnit: row.calculationUnit.trim(),
      reportUnit: row.reportUnit.trim(),
      remark: row.remark ?? '',
      updatedBy: props.currentUser?.userId
    })
    const payload = res.data || {}
    if (payload.success === false) {
      ElMessage.error(payload.message || '保存失败')
      return
    }
    ElMessage.success(`"${row.subcategoryName}"的缺省单位保存成功`)
  } catch (err) {
    console.error('保存缺省单位失败', err)
    ElMessage.error('保存失败：' + (err.response?.data?.message || err.message))
  }
}

// ============ 碳排放核算单位转换系数设置 ============

const unitConversionLoading = ref(false)
const unitConversionList = ref([])              // 全部转换系数记录
const conversionFilterSubcategory = ref('')    // 列表筛选小类
const conversionDialogVisible = ref(false)
const conversionSaving = ref(false)
const editingConversionId = ref(null)          // null=新增，否则为编辑记录 id
const conversionForm = ref({
  subcategoryCode: '',
  fromUnitCode: '',
  toUnitCode: '',
  conversionFactor: null,
  remark: ''
})

/**
 * 按筛选条件过滤后的转换系数列表
 */
const filteredConversionList = computed(() => {
  if (!conversionFilterSubcategory.value) return unitConversionList.value
  return unitConversionList.value.filter(
    r => r.subcategoryCode === conversionFilterSubcategory.value
  )
})

/**
 * 小类编码 -> 中文名称（取自 emission_subcategory 字典，找不到原样返回）
 */
function resolveSubcategoryName(code) {
  if (!code) return ''
  const item = subcategoryDictItems.value.find(d => d.code === code)
  return item ? item.value : code
}

/**
 * 加载全部单位转换系数，并确保标准单位、小类字典已加载
 */
async function loadUnitConversions() {
  unitConversionLoading.value = true
  try {
    if (allStandardUnits.value.length === 0) {
      await loadAllStandardUnits()
    }
    if (subcategoryDictItems.value.length === 0) {
      const dictRes = await axios.get('/api/data-dict/items/emission_subcategory')
      subcategoryDictItems.value = dictRes.data || []
    }
    const res = await unitConversionApi.listAll()
    unitConversionList.value = res.data || []
  } catch (err) {
    console.error('加载单位转换系数失败', err)
    ElMessage.error('加载单位转换系数失败：' + (err.response?.data?.message || err.message))
  } finally {
    unitConversionLoading.value = false
  }
}

/**
 * 列表筛选切换（仅前端过滤，无需重新请求）
 */
function filterConversions() {
  // computed 自动响应，此处无需额外处理
}

/**
 * 打开新增/编辑对话框
 * @param {Object|null} row 传 null 为新增，传行对象为编辑
 */
function openConversionDialog(row) {
  if (row) {
    editingConversionId.value = row.id
    conversionForm.value = {
      subcategoryCode: row.subcategoryCode,
      fromUnitCode: row.fromUnitCode,
      toUnitCode: row.toUnitCode,
      conversionFactor: row.conversionFactor,
      remark: row.remark ?? ''
    }
  } else {
    editingConversionId.value = null
    conversionForm.value = {
      subcategoryCode: '',
      fromUnitCode: '',
      toUnitCode: '',
      conversionFactor: null,
      remark: ''
    }
  }
  conversionDialogVisible.value = true
}

/**
 * 保存（新增/编辑）转换系数
 */
async function confirmSaveConversion() {
  const f = conversionForm.value
  if (!f.subcategoryCode) {
    ElMessage.warning('请选择能耗小类')
    return
  }
  if (!f.fromUnitCode) {
    ElMessage.warning('请选择源单位')
    return
  }
  if (!f.toUnitCode) {
    ElMessage.warning('请选择目标单位')
    return
  }
  if (f.fromUnitCode === f.toUnitCode) {
    ElMessage.warning('源单位与目标单位不能相同')
    return
  }
  if (f.conversionFactor == null || f.conversionFactor === '') {
    ElMessage.warning('请填写转换系数')
    return
  }
  conversionSaving.value = true
  try {
    const payload = {
      subcategoryCode: f.subcategoryCode,
      fromUnitCode: f.fromUnitCode,
      toUnitCode: f.toUnitCode,
      conversionFactor: f.conversionFactor,
      remark: f.remark ?? ''
    }
    let res
    if (editingConversionId.value) {
      res = await unitConversionApi.update(editingConversionId.value, payload)
    } else {
      res = await unitConversionApi.create(payload)
    }
    const data = res.data || {}
    if (data.success === false) {
      ElMessage.error(data.message || '保存失败')
      return
    }
    ElMessage.success(editingConversionId.value ? '更新成功' : '新增成功')
    conversionDialogVisible.value = false
    await loadUnitConversions()
  } catch (err) {
    console.error('保存单位转换系数失败', err)
    ElMessage.error('保存失败：' + (err.response?.data?.message || err.message))
  } finally {
    conversionSaving.value = false
  }
}

/**
 * 删除单条转换系数
 */
async function deleteConversionRow(row) {
  try {
    await ElMessageBox.confirm(
      `确认删除「${resolveSubcategoryName(row.subcategoryCode)}」下 ${row.fromUnitCode} → ${row.toUnitCode} 的转换系数？`,
      '删除确认',
      { type: 'warning' }
    )
  } catch {
    return
  }
  try {
    const res = await unitConversionApi.remove(row.id)
    const data = res.data || {}
    if (data.success === false) {
      ElMessage.error(data.message || '删除失败')
      return
    }
    ElMessage.success('删除成功')
    await loadUnitConversions()
  } catch (err) {
    console.error('删除单位转换系数失败', err)
    ElMessage.error('删除失败：' + (err.response?.data?.message || err.message))
  }
}

/**
 * 外购电力排放因子列表
 */
const electricFactorList = ref([])
const electricLoading = ref(false)
const electricSearchKeyword = ref('')
const electricDialogVisible = ref(false)
const electricDialogTitle = ref('新增电力排放因子')
const electricFormRef = ref(null)
const electricEditingId = ref(null)
const electricForm = ref({
  factorName: '',
  factorValue: 0,
  unit: 'kgCO₂/kWh',
  description: ''
})
const electricFormRules = {
  factorName: [{ required: true, message: '请输入因子名称', trigger: 'blur' }],
  factorValue: [{ required: true, message: '请输入碳排放因子值', trigger: 'blur' }],
  unit: [{ required: true, message: '请输入单位', trigger: 'blur' }]
}

/**
 * 格式化日期时间
 */
const formatDateTime = (dt) => {
  if (!dt) return '-'
  const d = new Date(dt)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

/**
 * 加载电力排放因子列表
 */
const loadElectricFactors = async () => {
  electricLoading.value = true
  try {
    const response = await axios.get('/api/electricity-carbon-factors')
    electricFactorList.value = response.data || []
  } catch (error) {
    console.error('加载电力排放因子失败:', error)
    ElMessage.error('加载电力排放因子失败')
  } finally {
    electricLoading.value = false
  }
}

/**
 * 搜索电力排放因子
 */
const handleElectricSearch = async () => {
  if (!electricSearchKeyword.value.trim()) {
    loadElectricFactors()
    return
  }
  electricLoading.value = true
  try {
    const response = await axios.get('/api/electricity-carbon-factors/search', {
      params: { factorName: electricSearchKeyword.value.trim() }
    })
    electricFactorList.value = response.data || []
  } catch (error) {
    console.error('搜索失败:', error)
    ElMessage.error('搜索失败')
  } finally {
    electricLoading.value = false
  }
}

/**
 * 重置搜索
 */
const handleElectricSearchReset = () => {
  electricSearchKeyword.value = ''
  loadElectricFactors()
}

/**
 * 重置表单
 */
const resetElectricForm = () => {
  electricForm.value = {
    factorName: '',
    factorValue: 0,
    unit: 'kgCO₂/kWh',
    description: ''
  }
  electricEditingId.value = null
}

/**
 * 新增电力排放因子
 */
const handleAddElectricFactor = () => {
  resetElectricForm()
  electricDialogTitle.value = '新增电力排放因子'
  electricDialogVisible.value = true
}

/**
 * 编辑电力排放因子
 */
const handleEditElectricFactor = (row) => {
  resetElectricForm()
  electricDialogTitle.value = '编辑电力排放因子'
  electricForm.value = {
    factorName: row.factorName,
    factorValue: row.factorValue,
    unit: row.unit,
    description: row.description || ''
  }
  electricEditingId.value = row.id
  electricDialogVisible.value = true
}

/**
 * 保存电力排放因子（新增或编辑）
 */
const handleSaveElectricFactor = async () => {
  if (!electricFormRef.value) return
  try {
    await electricFormRef.value.validate()
  } catch {
    return
  }
  try {
    const payload = {
      ...electricForm.value,
      updatedBy: props.currentUser?.userId || null
    }
    if (electricEditingId.value) {
      await axios.put(`/api/electricity-carbon-factors/${electricEditingId.value}`, payload)
      ElMessage.success('更新成功')
    } else {
      payload.createdBy = props.currentUser?.userId || null
      await axios.post('/api/electricity-carbon-factors', payload)
      ElMessage.success('新增成功')
    }
    electricDialogVisible.value = false
    loadElectricFactors()
  } catch (error) {
    if (error.response?.data?.message) {
      ElMessage.error(error.response.data.message)
    } else if (error.response?.data?.error) {
      ElMessage.error(error.response.data.error)
    } else {
      ElMessage.error('保存失败')
    }
  }
}

/**
 * 删除电力排放因子
 */
const handleDeleteElectricFactor = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除"${row.factorName}"吗？`, '确认删除', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await axios.delete(`/api/electricity-carbon-factors/${row.id}`)
    ElMessage.success('删除成功')
    loadElectricFactors()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

/**
 * 化石燃料排放因子列表
 */
const fossilFactorList = ref([])
const fossilLoading = ref(false)
const fossilSearchKeyword = ref('')
const fossilDialogVisible = ref(false)
const fossilDialogTitle = ref('新增化石燃料排放因子')
const fossilFormRef = ref(null)
const fossilEditingId = ref(null)
const fossilForm = ref({
  emissionFactorName: '',
  fuelType: '',
  source: '',
  unit: 't',
  lowerHeatingValue: 0,
  carbonContentPerUnitHeat: 0,
  fuelOxidationRate: 0,
  emissionFactor: 0,
  factorUnit: 'tCO₂/t',
  description: ''
})
const fossilFormRules = {
  emissionFactorName: [{ required: true, message: '请输入因子名称', trigger: 'blur' }],
  fuelType: [{ required: true, message: '请输入燃料品种', trigger: 'blur' }],
  unit: [{ required: true, message: '请选择计量单位', trigger: 'change' }]
}

/**
 * 低位发热量的单位描述（根据计量单位动态变化）
 */
const lowerHeatingValueUnit = computed(() => {
  return fossilForm.value.unit === 'wan_Nm3' ? 'GJ/万Nm³' : 'GJ/t'
})

/**
 * 计量单位切换时，自动更新因子单位
 * unit_code: 't' → tCO₂/t（每吨燃料排放多少吨CO₂）
 * unit_code: 'wan_Nm3' → tCO₂/万Nm³（每万标方气体排放多少吨CO₂）
 * 其他单位（kg/g/m3/Nm3/wan_m3）保持原值不变
 */
const handleUnitChange = () => {
  if (fossilForm.value.unit === 'wan_Nm3') {
    fossilForm.value.factorUnit = 'tCO₂/万Nm³'
  } else if (fossilForm.value.unit === 't') {
    fossilForm.value.factorUnit = 'tCO₂/t'
  }
  // 其他单位不自动覆盖（保留原值或用户后续手动指定）
}

/**
 * 监听低位发热量、单位热值含碳量、燃料氧化率的变化
 * 按公式自动计算碳排放因子：碳排放因子 = (低位发热量 × 单位热值含碳量 × 燃料氧化率 × 44/12) / 1000
 */
watch(
  () => [fossilForm.value.lowerHeatingValue, fossilForm.value.carbonContentPerUnitHeat, fossilForm.value.fuelOxidationRate],
  ([lhv, cc, ox]) => {
    if (lhv != null && cc != null && ox != null && lhv > 0 && cc > 0 && ox > 0) {
      fossilForm.value.emissionFactor = Number((lhv * cc * ox * 44 / 12 / 1000).toFixed(6))
    }
  }
)

/**
 * 加载化石燃料排放因子列表
 */
const loadFossilFactors = async () => {
  fossilLoading.value = true
  try {
    const response = await axios.get('/api/fossil-fuel-factors')
    fossilFactorList.value = response.data || []
  } catch (error) {
    console.error('加载化石燃料排放因子失败:', error)
    ElMessage.error('加载化石燃料排放因子失败')
  } finally {
    fossilLoading.value = false
  }
}

/**
 * 按燃料品种搜索
 */
const handleFossilSearch = async () => {
  if (!fossilSearchKeyword.value.trim()) {
    loadFossilFactors()
    return
  }
  fossilLoading.value = true
  try {
    const response = await axios.get('/api/fossil-fuel-factors/search', {
      params: { fuelType: fossilSearchKeyword.value.trim() }
    })
    fossilFactorList.value = response.data || []
  } catch (error) {
    console.error('搜索失败:', error)
    ElMessage.error('搜索失败')
  } finally {
    fossilLoading.value = false
  }
}

/**
 * 重置搜索
 */
const handleFossilSearchReset = () => {
  fossilSearchKeyword.value = ''
  loadFossilFactors()
}

/**
 * 重置化石燃料表单
 */
const resetFossilForm = () => {
  fossilForm.value = {
    emissionFactorName: '',
    fuelType: '',
    source: '',
    unit: 't',
    lowerHeatingValue: 0,
    carbonContentPerUnitHeat: 0,
    fuelOxidationRate: 0,
    emissionFactor: 0,
    factorUnit: 'tCO₂/t',
    description: ''
  }
  fossilEditingId.value = null
}

/**
 * 新增化石燃料排放因子
 */
const handleAddFossilFactor = () => {
  resetFossilForm()
  fossilDialogTitle.value = '新增化石燃料排放因子'
  fossilDialogVisible.value = true
}

/**
 * 编辑化石燃料排放因子
 * 兼容旧数据中 unit 为 "104 Nm3" 的情况，自动映射为 "万Nm³"
 */
const handleEditFossilFactor = (row) => {
  resetFossilForm()
  fossilDialogTitle.value = '编辑化石燃料排放因子'
  const unit = (row.unit === '104 Nm3' || row.unit === '万Nm³') ? '万Nm³' : 't'
  fossilForm.value = {
    emissionFactorName: row.emissionFactorName,
    fuelType: row.fuelType,
    source: row.source || '',
    unit: unit,
    lowerHeatingValue: row.lowerHeatingValue,
    carbonContentPerUnitHeat: row.carbonContentPerUnitHeat,
    fuelOxidationRate: row.fuelOxidationRate,
    emissionFactor: row.emissionFactor,
    factorUnit: unit === '万Nm³' ? 'tCO₂/万Nm³' : 'tCO₂/t',
    description: row.description || ''
  }
  fossilEditingId.value = row.id
  fossilDialogVisible.value = true
}

/**
 * 保存化石燃料排放因子（新增或编辑）
 */
const handleSaveFossilFactor = async () => {
  if (!fossilFormRef.value) return
  try {
    await fossilFormRef.value.validate()
  } catch {
    return
  }
  try {
    const payload = {
      ...fossilForm.value,
      updatedBy: props.currentUser?.userId || null
    }
    if (fossilEditingId.value) {
      await axios.put(`/api/fossil-fuel-factors/${fossilEditingId.value}`, payload)
      ElMessage.success('更新成功')
    } else {
      payload.createdBy = props.currentUser?.userId || null
      await axios.post('/api/fossil-fuel-factors', payload)
      ElMessage.success('新增成功')
    }
    fossilDialogVisible.value = false
    loadFossilFactors()
  } catch (error) {
    if (error.response?.data?.message) {
      ElMessage.error(error.response.data.message)
    } else if (error.response?.data?.error) {
      ElMessage.error(error.response.data.error)
    } else {
      ElMessage.error('保存失败')
    }
  }
}

/**
 * 删除化石燃料排放因子
 */
const handleDeleteFossilFactor = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除"${row.emissionFactorName}"吗？`, '确认删除', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await axios.delete(`/api/fossil-fuel-factors/${row.id}`)
    ElMessage.success('删除成功')
    loadFossilFactors()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

/**
 * 外购热力排放因子列表
 */
const thermalFactorList = ref([])
const thermalLoading = ref(false)
const thermalSearchKeyword = ref('')
const thermalDialogVisible = ref(false)
const thermalDialogTitle = ref('新增热力排放因子')
const thermalFormRef = ref(null)
const thermalEditingId = ref(null)
const thermalForm = ref({
  emissionFactorName: '',
  emissionFactor: 0,
  unit: 'tCO₂/GJ',
  source: '',
  description: ''
})
const thermalFormRules = {
  emissionFactorName: [{ required: true, message: '请输入因子名称', trigger: 'blur' }],
  emissionFactor: [{ required: true, message: '请输入碳排放因子值', trigger: 'blur' }],
  unit: [{ required: true, message: '请选择单位', trigger: 'change' }]
}

/**
 * 加载热力排放因子列表
 */
const loadThermalFactors = async () => {
  thermalLoading.value = true
  try {
    const response = await axios.get('/api/thermal-emission-factors')
    thermalFactorList.value = response.data || []
  } catch (error) {
    console.error('加载热力排放因子失败:', error)
    ElMessage.error('加载热力排放因子失败')
  } finally {
    thermalLoading.value = false
  }
}

/**
 * 搜索热力排放因子（前端本地按因子名称过滤）
 */
const handleThermalSearch = async () => {
  if (!thermalSearchKeyword.value.trim()) {
    loadThermalFactors()
    return
  }
  thermalLoading.value = true
  try {
    const response = await axios.get('/api/thermal-emission-factors')
    const all = response.data || []
    const kw = thermalSearchKeyword.value.trim().toLowerCase()
    thermalFactorList.value = all.filter(it =>
      (it.emissionFactorName || '').toLowerCase().includes(kw)
    )
  } catch (error) {
    console.error('搜索失败:', error)
    ElMessage.error('搜索失败')
  } finally {
    thermalLoading.value = false
  }
}

/**
 * 重置搜索
 */
const handleThermalSearchReset = () => {
  thermalSearchKeyword.value = ''
  loadThermalFactors()
}

/**
 * 重置热力表单
 */
const resetThermalForm = () => {
  thermalForm.value = {
    emissionFactorName: '',
    emissionFactor: 0,
    unit: 'tCO₂/GJ',
    source: '',
    description: ''
  }
  thermalEditingId.value = null
}

/**
 * 新增热力排放因子
 */
const handleAddThermalFactor = () => {
  resetThermalForm()
  thermalDialogTitle.value = '新增热力排放因子'
  thermalDialogVisible.value = true
}

/**
 * 编辑热力排放因子
 */
const handleEditThermalFactor = (row) => {
  resetThermalForm()
  thermalDialogTitle.value = '编辑热力排放因子'
  thermalForm.value = {
    emissionFactorName: row.emissionFactorName,
    emissionFactor: row.emissionFactor,
    unit: row.unit || 'tCO₂/GJ',
    source: row.source || '',
    description: row.description || ''
  }
  thermalEditingId.value = row.id
  thermalDialogVisible.value = true
}

/**
 * 保存热力排放因子（新增或编辑）
 */
const handleSaveThermalFactor = async () => {
  if (!thermalFormRef.value) return
  try {
    await thermalFormRef.value.validate()
  } catch {
    return
  }
  try {
    const payload = {
      ...thermalForm.value,
      updatedBy: props.currentUser?.userId || null
    }
    if (thermalEditingId.value) {
      await axios.put(`/api/thermal-emission-factors/${thermalEditingId.value}`, payload)
      ElMessage.success('更新成功')
    } else {
      payload.createdBy = props.currentUser?.userId || null
      await axios.post('/api/thermal-emission-factors', payload)
      ElMessage.success('新增成功')
    }
    thermalDialogVisible.value = false
    loadThermalFactors()
  } catch (error) {
    if (error.response?.data?.message) {
      ElMessage.error(error.response.data.message)
    } else if (error.response?.data?.error) {
      ElMessage.error(error.response.data.error)
    } else {
      ElMessage.error('保存失败')
    }
  }
}

/**
 * 删除热力排放因子
 */
const handleDeleteThermalFactor = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除"${row.emissionFactorName}"吗？`, '确认删除', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await axios.delete(`/api/thermal-emission-factors/${row.id}`)
    ElMessage.success('删除成功')
    loadThermalFactors()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

/**
 * 固体废弃物焚烧排放因子列表
 */
const solidWasteFactorList = ref([])
const solidWasteLoading = ref(false)
const solidWasteSearchKeyword = ref('')
const solidWasteDialogVisible = ref(false)
const solidWasteDialogTitle = ref('新增固体废弃物焚烧排放因子')
const solidWasteFormRef = ref(null)
const solidWasteEditingId = ref(null)
const solidWasteForm = ref({
  emissionFactorName: '',
  wasteType: '',
  ccw: 0,
  fcf: 0,
  ce: 0,
  emissionFactor: 0,
  unit: 'tCO₂/t固体废物',
  source: '',
  description: ''
})
const solidWasteFormRules = {
  emissionFactorName: [{ required: true, message: '请输入因子名称', trigger: 'blur' }],
  wasteType: [{ required: true, message: '请输入固体废物种类', trigger: 'blur' }],
  ccw: [{ required: true, message: '请输入碳含量比例', trigger: 'blur' }],
  fcf: [{ required: true, message: '请输入化石碳比例', trigger: 'blur' }],
  ce: [{ required: true, message: '请输入燃烧效率', trigger: 'blur' }],
  unit: [{ required: true, message: '请选择单位', trigger: 'change' }]
}

/**
 * 监听 CCW、FCF、CE 的变化
 * 按公式自动计算碳排放因子：碳排放因子 = CCW × FCF × CE × 44/12
 */
watch(
  () => [solidWasteForm.value.ccw, solidWasteForm.value.fcf, solidWasteForm.value.ce],
  ([ccw, fcf, ce]) => {
    if (ccw != null && fcf != null && ce != null && ccw > 0 && fcf > 0 && ce > 0) {
      solidWasteForm.value.emissionFactor = Number((ccw * fcf * ce * 44 / 12).toFixed(6))
    }
  }
)

/**
 * 加载固体废弃物焚烧排放因子列表
 */
const loadSolidWasteFactors = async () => {
  solidWasteLoading.value = true
  try {
    const response = await axios.get('/api/waste-incineration-factors')
    solidWasteFactorList.value = response.data || []
  } catch (error) {
    console.error('加载固体废弃物焚烧排放因子失败:', error)
    ElMessage.error('加载固体废弃物焚烧排放因子失败')
  } finally {
    solidWasteLoading.value = false
  }
}

/**
 * 搜索固体废弃物焚烧排放因子（前端本地按因子名称过滤）
 */
const handleSolidWasteSearch = async () => {
  if (!solidWasteSearchKeyword.value.trim()) {
    loadSolidWasteFactors()
    return
  }
  solidWasteLoading.value = true
  try {
    const response = await axios.get('/api/waste-incineration-factors')
    const all = response.data || []
    const kw = solidWasteSearchKeyword.value.trim().toLowerCase()
    solidWasteFactorList.value = all.filter(it =>
      (it.emissionFactorName || '').toLowerCase().includes(kw)
    )
  } catch (error) {
    console.error('搜索失败:', error)
    ElMessage.error('搜索失败')
  } finally {
    solidWasteLoading.value = false
  }
}

/**
 * 重置搜索
 */
const handleSolidWasteSearchReset = () => {
  solidWasteSearchKeyword.value = ''
  loadSolidWasteFactors()
}

/**
 * 重置固体废弃物表单
 */
const resetSolidWasteForm = () => {
  solidWasteForm.value = {
    emissionFactorName: '',
    wasteType: '',
    ccw: 0,
    fcf: 0,
    ce: 0,
    emissionFactor: 0,
    unit: 'tCO₂/t固体废物',
    source: '',
    description: ''
  }
  solidWasteEditingId.value = null
}

/**
 * 新增固体废弃物焚烧排放因子
 */
const handleAddSolidWasteFactor = () => {
  resetSolidWasteForm()
  solidWasteDialogTitle.value = '新增固体废弃物焚烧排放因子'
  solidWasteDialogVisible.value = true
}

/**
 * 编辑固体废弃物焚烧排放因子
 */
const handleEditSolidWasteFactor = (row) => {
  resetSolidWasteForm()
  solidWasteDialogTitle.value = '编辑固体废弃物焚烧排放因子'
  solidWasteForm.value = {
    emissionFactorName: row.emissionFactorName,
    wasteType: row.wasteType,
    ccw: row.ccw,
    fcf: row.fcf,
    ce: row.ce,
    emissionFactor: row.emissionFactor,
    unit: row.unit || 'tCO₂/t固体废物',
    source: row.source || '',
    description: row.description || ''
  }
  solidWasteEditingId.value = row.id
  solidWasteDialogVisible.value = true
}

/**
 * 保存固体废弃物焚烧排放因子（新增或编辑）
 */
const handleSaveSolidWasteFactor = async () => {
  if (!solidWasteFormRef.value) return
  try {
    await solidWasteFormRef.value.validate()
  } catch {
    return
  }
  try {
    const payload = {
      ...solidWasteForm.value,
      updatedBy: props.currentUser?.userId || null
    }
    if (solidWasteEditingId.value) {
      await axios.put(`/api/waste-incineration-factors/${solidWasteEditingId.value}`, payload)
      ElMessage.success('更新成功')
    } else {
      payload.createdBy = props.currentUser?.userId || null
      await axios.post('/api/waste-incineration-factors', payload)
      ElMessage.success('新增成功')
    }
    solidWasteDialogVisible.value = false
    loadSolidWasteFactors()
  } catch (error) {
    if (error.response?.data?.message) {
      ElMessage.error(error.response.data.message)
    } else if (error.response?.data?.error) {
      ElMessage.error(error.response.data.error)
    } else {
      ElMessage.error('保存失败')
    }
  }
}

/**
 * 删除固体废弃物焚烧排放因子
 */
const handleDeleteSolidWasteFactor = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除"${row.emissionFactorName}"吗？`, '确认删除', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await axios.delete(`/api/waste-incineration-factors/${row.id}`)
    ElMessage.success('删除成功')
    loadSolidWasteFactors()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

/**
 * 废水处理排放因子列表
 */
const wastewaterFactorList = ref([])
const wastewaterLoading = ref(false)
const wastewaterSearchKeyword = ref('')
const wastewaterDialogVisible = ref(false)
const wastewaterDialogTitle = ref('新增废水处理排放因子')
const wastewaterFormRef = ref(null)
const wastewaterEditingId = ref(null)
const wastewaterForm = ref({
  emissionFactorName: '',
  wastewaterType: '',
  od: 0,
  bo: 0,
  mcf: 0,
  gwp: 28,
  emissionFactor: 0,
  unit: 'tCO₂/m³',
  source: '',
  description: ''
})
const wastewaterFormRules = {
  emissionFactorName: [{ required: true, message: '请输入因子名称', trigger: 'blur' }],
  wastewaterType: [{ required: true, message: '请输入废水种类', trigger: 'blur' }],
  od: [{ required: true, message: '请输入需氧浓度系数', trigger: 'blur' }],
  bo: [{ required: true, message: '请输入甲烷产生能力', trigger: 'blur' }],
  mcf: [{ required: true, message: '请输入甲烷修正因子', trigger: 'blur' }],
  gwp: [{ required: true, message: '请输入全球变暖潜能值', trigger: 'blur' }],
  unit: [{ required: true, message: '请选择单位', trigger: 'change' }]
}

/**
 * 监听 OD、Bo、MCF、GWP 的变化
 * 按公式自动计算碳排放因子：碳排放因子 = OD × Bo × MCF × GWP / 1000000
 */
watch(
  () => [wastewaterForm.value.od, wastewaterForm.value.bo, wastewaterForm.value.mcf, wastewaterForm.value.gwp],
  ([od, bo, mcf, gwp]) => {
    if (od != null && bo != null && mcf != null && gwp != null && od > 0 && bo > 0 && mcf > 0 && gwp > 0) {
      wastewaterForm.value.emissionFactor = Number((od * bo * mcf * gwp / 1000000).toFixed(6))
    }
  }
)

/**
 * 加载废水处理排放因子列表
 */
const loadWastewaterFactors = async () => {
  wastewaterLoading.value = true
  try {
    const response = await axios.get('/api/wastewater-treatment-factors')
    wastewaterFactorList.value = response.data || []
  } catch (error) {
    console.error('加载废水处理排放因子失败:', error)
    ElMessage.error('加载废水处理排放因子失败')
  } finally {
    wastewaterLoading.value = false
  }
}

/**
 * 搜索废水处理排放因子（前端本地按因子名称过滤）
 */
const handleWastewaterSearch = async () => {
  if (!wastewaterSearchKeyword.value.trim()) {
    loadWastewaterFactors()
    return
  }
  wastewaterLoading.value = true
  try {
    const response = await axios.get('/api/wastewater-treatment-factors')
    const all = response.data || []
    const kw = wastewaterSearchKeyword.value.trim().toLowerCase()
    wastewaterFactorList.value = all.filter(it =>
      (it.emissionFactorName || '').toLowerCase().includes(kw)
    )
  } catch (error) {
    console.error('搜索失败:', error)
    ElMessage.error('搜索失败')
  } finally {
    wastewaterLoading.value = false
  }
}

/**
 * 重置搜索
 */
const handleWastewaterSearchReset = () => {
  wastewaterSearchKeyword.value = ''
  loadWastewaterFactors()
}

/**
 * 重置废水处理表单
 */
const resetWastewaterForm = () => {
  wastewaterForm.value = {
    emissionFactorName: '',
    wastewaterType: '',
    od: 0,
    bo: 0,
    mcf: 0,
    gwp: 28,
    emissionFactor: 0,
    unit: 'tCO₂/m³',
    source: '',
    description: ''
  }
  wastewaterEditingId.value = null
}

/**
 * 新增废水处理排放因子
 */
const handleAddWastewaterFactor = () => {
  resetWastewaterForm()
  wastewaterDialogTitle.value = '新增废水处理排放因子'
  wastewaterDialogVisible.value = true
}

/**
 * 编辑废水处理排放因子
 */
const handleEditWastewaterFactor = (row) => {
  resetWastewaterForm()
  wastewaterDialogTitle.value = '编辑废水处理排放因子'
  wastewaterForm.value = {
    emissionFactorName: row.emissionFactorName,
    wastewaterType: row.wastewaterType,
    od: row.od,
    bo: row.bo,
    mcf: row.mcf,
    gwp: row.gwp,
    emissionFactor: row.emissionFactor,
    unit: row.unit || 'tCO₂/m³',
    source: row.source || '',
    description: row.description || ''
  }
  wastewaterEditingId.value = row.id
  wastewaterDialogVisible.value = true
}

/**
 * 保存废水处理排放因子（新增或编辑）
 */
const handleSaveWastewaterFactor = async () => {
  if (!wastewaterFormRef.value) return
  try {
    await wastewaterFormRef.value.validate()
  } catch {
    return
  }
  try {
    const payload = {
      ...wastewaterForm.value,
      updatedBy: props.currentUser?.userId || null
    }
    if (wastewaterEditingId.value) {
      await axios.put(`/api/wastewater-treatment-factors/${wastewaterEditingId.value}`, payload)
      ElMessage.success('更新成功')
    } else {
      payload.createdBy = props.currentUser?.userId || null
      await axios.post('/api/wastewater-treatment-factors', payload)
      ElMessage.success('新增成功')
    }
    wastewaterDialogVisible.value = false
    loadWastewaterFactors()
  } catch (error) {
    if (error.response?.data?.message) {
      ElMessage.error(error.response.data.message)
    } else if (error.response?.data?.error) {
      ElMessage.error(error.response.data.error)
    } else {
      ElMessage.error('保存失败')
    }
  }
}

/**
 * 删除废水处理排放因子
 */
const handleDeleteWastewaterFactor = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除"${row.emissionFactorName}"吗？`, '确认删除', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await axios.delete(`/api/wastewater-treatment-factors/${row.id}`)
    ElMessage.success('删除成功')
    loadWastewaterFactors()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

/**
 * 电力表列表（模拟数据）
 */
const electricMeterList = ref([
  { meterName: '1#主电表', meterCode: 'EM-001', meterType: '三相四线', position: '配电室A区', status: '启用' },
  { meterName: '2#副电表', meterCode: 'EM-002', meterType: '三相三线', position: '配电室B区', status: '启用' },
  { meterName: '3#计量表', meterCode: 'EM-003', meterType: '单相', position: '车间入口', status: '停用' }
])

/**
 * 组件挂载时加载排放因子数据
 */
onMounted(async () => {
  loadAllStandardUnits()
  loadFactorUnits()
  loadElectricFactors()
  loadFossilFactors()
  loadThermalFactors()
  loadSolidWasteFactors()
  loadWastewaterFactors()

  // 从核算模版管理页跳转"编辑因子"：自动进入因子模版管理并打开指定因子模版
  if (props.initialFactorTemplateId != null) {
    activeMenu.value = 'factor-template'
    await loadFactorTemplates()
    const target = factorTemplateList.value.find(t => t.id === props.initialFactorTemplateId)
    if (target) {
      openEditTemplateFactors(target)
    }
  }
})
</script>

<style scoped>
.params-settings-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #f5f7fa;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 30px;
  height: 60px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 15px;
}

.header-left h1 {
  font-size: 20px;
  margin: 0;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 15px;
}

.user-info span {
  font-size: 14px;
}

.main-content {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.sidebar {
  width: 240px;
  background-color: #ffffff;
  border-right: 1px solid #e4e7ed;
  overflow-y: auto;
}

.sidebar-menu {
  border-right: none;
}

.sidebar-menu:not(.el-menu--collapse) {
  width: 240px;
}

.content {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 20px 30px;
  overflow: hidden;
  min-height: 0;
}

.content-header {
  margin-bottom: 20px;
}

.content-header h2 {
  font-size: 20px;
  color: #303133;
  margin: 10px 0 0 0;
}

.content-body {
  flex: 1;
  background-color: #ffffff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
  overflow: hidden;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.content-body.meter-mode {
  padding: 0;
  border-radius: 0;
  box-shadow: none;
  background-color: #f5f7fa;
}

.content-body.dict-mode {
  padding: 0 20px;
}

.content-body.category-mode {
  padding: 0 20px;
}

.toolbar {
  margin-top: 15px;
  padding-top: 15px;
  border-top: 1px solid #ebeef5;
}

/* 碳排放核算缺省单位设置 */
.content-body.calc-unit-mode {
  padding: 0 16px;
}
.calc-unit-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.calc-unit-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.calc-unit-tip {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 10px 14px;
  background: #ecf5ff;
  border: 1px solid #d9ecff;
  border-radius: 6px;
  color: #606266;
  font-size: 13px;
  line-height: 1.6;
  flex: 1;
  margin-right: 16px;
}
.calc-unit-tip .el-icon {
  color: #409eff;
  margin-top: 2px;
  flex-shrink: 0;
}

.factor-toolbar {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.default-content {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
}

.form-row {
  display: flex;
  gap: 16px;
}

.form-col {
  flex: 1;
}

.input-with-unit {
  display: flex;
  align-items: center;
  gap: 8px;
}

.unit-label {
  font-size: 13px;
  color: #909399;
  white-space: nowrap;
}

.dialog-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.formula-tip {
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
  text-align: left;
}

.footer-buttons {
  display: flex;
  gap: 10px;
}

.readonly-mode .main-content :deep(.el-button--success),
.readonly-mode .main-content :deep(.el-button--danger),
.readonly-mode .main-content :deep(.el-table__cell .el-button--primary),
.readonly-mode .main-content :deep(.el-switch) {
  opacity: .45;
  pointer-events: none;
  cursor: not-allowed;
}

.permission-alert {
  flex: 0 0 auto;
  border-radius: 0;
}
</style>
