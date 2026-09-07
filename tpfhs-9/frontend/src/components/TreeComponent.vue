<template>
  <div class="tree-container">
    <div v-for="node in treeData" :key="node.id" class="tree-node">
      <div 
        class="node-item" 
        :class="{ active: selectedNode && selectedNode.id === node.id }"
        @click="handleNodeClick(node)"
      >
        <span 
          v-if="node.canHaveChildren && (node.children && node.children.length > 0)" 
          class="expand-icon"
          @click.stop="toggleExpand(node)"
        >
          <el-icon :size="16" :class="{ expanded: expandedNodes.includes(node.id) }">
            <ArrowRight />
          </el-icon>
        </span>
        <span v-else-if="node.canHaveChildren" class="expand-icon placeholder">•</span>
        <span v-else class="expand-icon placeholder">·</span>
        
        <span class="node-icon">
          <el-icon :size="16">
            <component :is="getNodeIcon(node.typeName)" />
          </el-icon>
        </span>
        
        <span class="node-name">{{ node.name }}</span>
        
        <span 
          v-if="canAddChild(node)" 
          class="add-btn"
          @click.stop="handleAddChild(node)"
        >
          <el-icon :size="14"><Plus /></el-icon>
        </span>
        
        <span
          v-if="node.typeId === 2"
          class="add-multiple-btn"
          title="添加多个采集节点"
          @click.stop="handleAddMultipleCollectionNodes(node)"
        >
          <el-icon :size="14"><Collection /></el-icon>
        </span>

        <span
          v-if="node.typeId === 1 || node.typeId === 2"
          class="mount-template-btn"
          title="挂载节点模版"
          @click.stop="handleMountTemplate(node)"
        >
          <el-icon :size="14"><Connection /></el-icon>
        </span>
        
        <span 
          v-if="node.id !== 1"
          class="delete-btn"
          @click.stop="handleDeleteNode(node)"
        >
          <el-icon :size="14"><Minus /></el-icon>
        </span>
        
        <span 
          v-if="canMoveUp(node)"
          class="move-up-btn"
          @click.stop="handleMoveUp(node)"
        >
          <el-icon :size="14"><ArrowUp /></el-icon>
        </span>
        
        <span 
          v-if="canMoveDown(node)"
          class="move-down-btn"
          @click.stop="handleMoveDown(node)"
        >
          <el-icon :size="14"><ArrowDown /></el-icon>
        </span>
      </div>
      
      <div 
        v-if="node.canHaveChildren && node.children && node.children.length > 0 && expandedNodes.includes(node.id)"
        class="children-container"
      >
        <tree-component
          :tree-data="node.children"
          :selected-node="selectedNode"
          :options="options"
          @select="$emit('select', $event)"
          @add="$emit('add', $event)"
          @addMultiple="$emit('addMultiple', $event)"
          @mountTemplate="$emit('mountTemplate', $event)"
          @delete="$emit('delete', $event)"
          @moveUp="$emit('moveUp', $event)"
          @moveDown="$emit('moveDown', $event)"
        />
      </div>
    </div>
  </div>
</template>

<script setup>import { ref, provide } from 'vue';
import { ArrowRight, Plus, Minus, ArrowUp, ArrowDown, Folder, FolderOpened, Document, ShoppingCart, Collection, Connection } from '@element-plus/icons-vue';
const props = defineProps({
 treeData: {
 type: Array,
 default: () => []
 },
 selectedNode: {
 type: Object,
 default: null
 },
 options: {
 type: Object,
 default: () => ({})
 }
});
const emit = defineEmits(['select', 'add', 'addMultiple', 'mountTemplate', 'delete', 'moveUp', 'moveDown']);
const expandedNodes = ref([]);
provide('expandedNodes', expandedNodes);
const toggleExpand = (node) => {
 const index = expandedNodes.value.indexOf(node.id);
 if (index > -1) {
 expandedNodes.value.splice(index, 1);
 }
 else {
 expandedNodes.value.push(node.id);
 }
};
const handleNodeClick = (node) => {
 emit('select', node);
};
const canAddChild = (node) => {
 return node.canHaveChildren;
};
const handleAddChild = (node) => {
 emit('add', node);
};
const handleAddMultipleCollectionNodes = (node) => {
  emit('addMultiple', node);
};
/**
 * 点击树节点"挂载节点模版"按钮时触发，向父组件抛出 mountTemplate 事件
 * 由父组件（App.vue）打开"节点模版选择"弹窗并执行挂载逻辑
 * @param {Object} node 当前点击的目标父节点（typeId=1/2 的"排放核算点"，挂载位置）
 */
const handleMountTemplate = (node) => {
  emit('mountTemplate', node);
};
const handleDeleteNode = (node) => {
 emit('delete', node);
};
const canMoveUp = (node) => {
 const siblings = getSiblings(node);
 const currentIndex = siblings.findIndex(n => n.id === node.id);
 return currentIndex > 0;
};
const canMoveDown = (node) => {
 const siblings = getSiblings(node);
 const currentIndex = siblings.findIndex(n => n.id === node.id);
 return currentIndex < siblings.length - 1;
};
const getSiblings = (node) => {
 let siblings = null;
 const findSiblings = (nodes) => {
 for (let i = 0; i < nodes.length; i++) {
 const n = nodes[i];
 if (n.id === node.id) {
 siblings = nodes;
 return true;
 }
 if (n.children && findSiblings(n.children)) {
 return true;
 }
 }
 return false;
 };
 findSiblings(props.treeData);
 return siblings;
};
const handleMoveUp = (node) => {
 emit('moveUp', node);
};
const handleMoveDown = (node) => {
 emit('moveDown', node);
};
const getNodeIcon = (typeName) => {
 switch (typeName) {
 case 'root':
 return FolderOpened;
 case 'calculation':
 return Folder;
 case 'data_collection':
 return Document;
 case 'transport':
 return ShoppingCart;
 default:
 return Folder;
 }
};
</script>

<style scoped>
.tree-container {
  padding-left: 0;
}

.tree-node {
  position: relative;
}

.node-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  cursor: pointer;
  border-radius: 4px;
  transition: background-color 0.2s;
}

.node-item:hover {
  background-color: #f5f7fa;
}

.node-item.active {
  background-color: #e6f7ff;
  color: #1890ff;
}

.expand-icon {
  width: 16px;
  height: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #8c8c8c;
}

.expand-icon.placeholder {
  cursor: default;
}

.expand-icon .el-icon--expanded {
  transform: rotate(90deg);
  transition: transform 0.2s;
}

.node-icon {
  color: #595959;
}

.node-item.active .node-icon {
  color: #1890ff;
}

.node-name {
  flex: 1;
  font-size: 14px;
  color: #333;
}

.node-item.active .node-name {
  color: #1890ff;
}

.add-btn {
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background-color: #fff;
  border: 1px solid #d9d9d9;
  color: #595959;
  opacity: 0;
  transition: all 0.2s;
}

.node-item:hover .add-btn {
  opacity: 1;
}

.add-btn:hover {
  background-color: #1890ff;
  border-color: #1890ff;
  color: #fff;
}

.add-multiple-btn {
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background-color: #fff;
  border: 1px solid #d9d9d9;
  color: #595959;
  opacity: 0;
  transition: all 0.2s;
}

.node-item:hover .add-multiple-btn {
  opacity: 1;
}

.add-multiple-btn:hover {
  background-color: #722ed1;
  border-color: #722ed1;
  color: #fff;
}

.mount-template-btn {
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background-color: #fff;
  border: 1px solid #d9d9d9;
  color: #595959;
  opacity: 0;
  transition: all 0.2s;
}

.node-item:hover .mount-template-btn {
  opacity: 1;
}

.mount-template-btn:hover {
  background-color: #13c2c2;
  border-color: #13c2c2;
  color: #fff;
}

.delete-btn {
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background-color: #fff;
  border: 1px solid #d9d9d9;
  color: #595959;
  opacity: 0;
  transition: all 0.2s;
}

.node-item:hover .delete-btn {
  opacity: 1;
}

.delete-btn:hover {
  background-color: #ff4d4f;
  border-color: #ff4d4f;
  color: #fff;
}

.move-up-btn, .move-down-btn {
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background-color: #fff;
  border: 1px solid #d9d9d9;
  color: #595959;
  opacity: 0;
  transition: all 0.2s;
}

.node-item:hover .move-up-btn,
.node-item:hover .move-down-btn {
  opacity: 1;
}

.move-up-btn:hover, .move-down-btn:hover {
  background-color: #1890ff;
  border-color: #1890ff;
  color: #fff;
}

.add-menu {
  position: absolute;
  top: 0;
  right: 0;
  background-color: #fff;
  border: 1px solid #e8eaec;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  z-index: 100;
  min-width: 150px;
}

.menu-item {
  padding: 8px 16px;
  font-size: 14px;
  color: #333;
  cursor: pointer;
  white-space: nowrap;
}

.menu-item:hover {
  background-color: #f5f7fa;
}

.children-container {
  padding-left: 20px;
  border-left: 1px solid #e8eaec;
}
</style>