<template>
  <el-dialog
    :model-value="visible"
    @update:model-value="handleUpdateVisible"
    :title="dialogTitle"
    width="900px"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    @close="handleClose"
  >
    <div class="dialog-content">
      <div v-if="showHotWaterInput" class="additional-inputs">
        <el-form :model="hotWaterForm" label-width="150px">
          <el-form-item label="热水温度">
            <el-input 
              v-model.number="hotWaterForm.temperature" 
              placeholder="请输入热水温度"
            />
            <span class="unit-text">℃</span>
          </el-form-item>
        </el-form>
      </div>
      
      <div v-if="showSteamInput" class="additional-inputs">
        <el-form :model="steamForm" label-width="150px">
          <el-form-item label="蒸汽种类">
            <el-select v-model="steamForm.steamType">
              <el-option label="饱和蒸汽" value="饱和蒸汽" />
              <el-option label="过热蒸汽" value="过热蒸汽" />
            </el-select>
          </el-form-item>
          <el-form-item label="蒸汽温度">
            <el-input 
              v-model.number="steamForm.temperature" 
              placeholder="请输入蒸汽温度"
            />
            <span class="unit-text">℃</span>
          </el-form-item>
          <el-form-item v-if="steamForm.steamType !== '饱和蒸汽'" label="蒸汽气压">
            <el-input 
              v-model.number="steamForm.pressure" 
              placeholder="请输入蒸汽气压"
            />
            <span class="unit-text">MPa</span>
          </el-form-item>
        </el-form>
      </div>
      
      <div class="factor-list">
        <el-table :data="factorList" border>
          <el-table-column width="50">
            <template #default="scope">
              <el-radio 
                :value="scope.row.id" 
                v-model="selectedFactorId" 
                @change="handleSelectChange(scope.row)"
              />
            </template>
          </el-table-column>
          <el-table-column prop="name" label="碳排放因子名称" />
          <el-table-column prop="value" label="因子值" />
          <el-table-column prop="unit" label="单位" />
          <el-table-column prop="description" label="描述" :show-overflow-tooltip="true" />
        </el-table>
      </div>
    </div>
    
    <div slot="footer" class="dialog-footer">
      <el-button @click="handleCancel">取消</el-button>
      <el-button type="primary" @click="handleConfirm" :disabled="!selectedFactor">确定</el-button>
    </div>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch, reactive } from 'vue';

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  emissionCategory: {
    type: String,
    default: ''
  },
  emissionSubcategory: {
    type: String,
    default: ''
  }
});

const emit = defineEmits(['select', 'cancel', 'update:visible']);

const selectedFactorId = ref(null);
const selectedFactor = ref(null);
const factorList = ref([]);
const isLoading = ref(false);

const hotWaterForm = reactive({
  temperature: 60
});

const steamForm = reactive({
  steamType: '饱和蒸汽',
  temperature: null,
  pressure: null
});

const showHotWaterInput = computed(() => {
  return props.emissionCategory === '购入的热力' && props.emissionSubcategory === '质量单位计量的热水';
});

const showSteamInput = computed(() => {
  return props.emissionCategory === '购入的热力' && props.emissionSubcategory === '质量单位计量的蒸汽';
});

const dialogTitle = computed(() => {
  return `选择碳排放因子 - ${props.emissionCategory} - ${props.emissionSubcategory}`;
});

const loadFactors = async () => {
  if (!props.emissionCategory) return;
  
  isLoading.value = true;
  try {
    const response = await fetch(`/api/emission-factor-selector/factors?category=${encodeURIComponent(props.emissionCategory)}&subcategory=${encodeURIComponent(props.emissionSubcategory || '')}`);
    const data = await response.json();
    factorList.value = data;
  } catch (error) {
    console.error('Failed to load emission factors:', error);
    factorList.value = [];
  } finally {
    isLoading.value = false;
  }
};

const handleSelectChange = (factor) => {
  selectedFactorId.value = factor.id;
  selectedFactor.value = factor;
};

const handleConfirm = async () => {
  if (!selectedFactor.value) return;
  
  if (showHotWaterInput.value) {
    if (!hotWaterForm.temperature) {
      alert('请输入热水温度');
      return;
    }
    if (hotWaterForm.temperature < 0 || hotWaterForm.temperature > 100) {
      alert('热水温度范围应在0-100℃之间');
      return;
    }
    
    try {
      const response = await fetch('/api/emission-factor-selector/calculate-hot-water', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          factorId: selectedFactor.value.id,
          temperature: hotWaterForm.temperature
        })
      });
      const result = await response.json();
      if (result.success) {
        emit('select', {
          factorValue: result.factorValue,
          description: result.description,
          factorId: selectedFactor.value?.id ?? null,
          factorName: selectedFactor.value?.name ?? '',
          factorUnit: selectedFactor.value?.unit ?? ''
        });
      } else {
        alert(result.message || '计算失败');
        return;
      }
    } catch (error) {
      console.error('Failed to calculate hot water factor:', error);
      alert('计算失败');
      return;
    }
  } else if (showSteamInput.value) {
    if (!steamForm.steamType) {
      alert('请选择蒸汽种类');
      return;
    }
    if (!steamForm.temperature) {
      alert('请输入蒸汽温度');
      return;
    }

    // 饱和蒸汽温度范围校验：0.01°C - 373.946°C（水的饱和相变区域）
    if (steamForm.steamType === '饱和蒸汽') {
      if (steamForm.temperature < 0.01 || steamForm.temperature > 373.946) {
        alert('超出水在饱和相变区域的温度极限 (0.01°C - 373.946°C)，请修改！');
        return;
      }
    }
    
    if (steamForm.steamType === '过热蒸汽') {
      if (!steamForm.pressure) {
        alert('请输入蒸汽气压');
        return;
      }
      if (steamForm.temperature < 0 || steamForm.temperature > 650) {
        alert('蒸汽温度范围应在0-650℃之间');
        return;
      }
      if (steamForm.pressure < 0.001 || steamForm.pressure > 35) {
        alert('蒸汽气压范围应在0.001-35MPa之间');
        return;
      }
    }
    
    try {
      const response = await fetch('/api/emission-factor-selector/calculate-steam', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          factorId: selectedFactor.value.id,
          steamType: steamForm.steamType,
          temperature: steamForm.temperature,
          pressure: steamForm.pressure
        })
      });
      const result = await response.json();
      if (result.success) {
        emit('select', {
          factorValue: result.factorValue,
          description: result.description,
          factorId: selectedFactor.value?.id ?? null,
          factorName: selectedFactor.value?.name ?? '',
          factorUnit: selectedFactor.value?.unit ?? ''
        });
      } else {
        alert(result.message || '计算失败');
        return;
      }
    } catch (error) {
      console.error('Failed to calculate steam factor:', error);
      alert('计算失败');
      return;
    }
  } else {
    emit('select', {
      factorValue: selectedFactor.value.value,
      description: selectedFactor.value.description,
      factorId: selectedFactor.value?.id ?? null,
      factorName: selectedFactor.value?.name ?? '',
      factorUnit: selectedFactor.value?.unit ?? ''
    });
  }
  
  selectedFactorId.value = null;
    selectedFactor.value = null;
  };
  
  const handleCancel = () => {
    selectedFactorId.value = null;
    selectedFactor.value = null;
    resetAdditionalInputs();
    emit('cancel');
    emit('update:visible', false);
  };

  const handleClose = () => {
    selectedFactorId.value = null;
    selectedFactor.value = null;
    resetAdditionalInputs();
    emit('update:visible', false);
  };

  const handleUpdateVisible = (value) => {
    if (!value) {
      selectedFactorId.value = null;
      selectedFactor.value = null;
      resetAdditionalInputs();
    }
    emit('update:visible', value);
};

const resetAdditionalInputs = () => {
  hotWaterForm.temperature = 60;
  steamForm.steamType = '饱和蒸汽';
  steamForm.temperature = null;
  steamForm.pressure = null;
};

watch(() => props.visible, (val) => {
  if (val) {
    selectedFactorId.value = null;
    selectedFactor.value = null;
    loadFactors();
  }
});

watch([() => props.emissionCategory, () => props.emissionSubcategory], () => {
  if (props.visible) {
    loadFactors();
  }
});
</script>

<style scoped>
.dialog-content {
  padding: 16px;
}

.additional-inputs {
  margin-bottom: 16px;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 4px;
}

.unit-text {
  margin-left: 8px;
  color: #606266;
}

.factor-list {
  max-height: 400px;
  overflow-y: auto;
}

.dialog-footer {
  text-align: right;
}
</style>