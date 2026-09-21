<template>
  <div class="app-container carbon-system">
    <div class="carbon-toolbar">
      <div>
        <h2>系统管理</h2>
        <span>按设置关联关系整理后台管理入口</span>
      </div>
      <el-button icon="el-icon-s-home" @click="$router.push('/carbon/index')">返回首页</el-button>
    </div>

    <div class="system-layout">
      <aside class="system-tree-panel">
        <div class="tree-heading">
          <span>设置目录</span>
          <small>{{ accessibleCount }}/{{ systemLinks.length }} 项可用</small>
        </div>
        <el-tree
          ref="systemTree"
          class="system-tree"
          :data="treeData"
          :props="treeProps"
          :current-node-key="selectedNodeKey"
          node-key="key"
          default-expand-all
          highlight-current
          @node-click="handleTreeNodeClick"
        >
          <span slot-scope="{ node, data }" class="tree-node" :class="{ 'is-locked': data.type === 'link' && !data.link.accessible }">
            <i :class="data.icon"></i>
            <span>{{ node.label }}</span>
            <small v-if="data.type === 'group'">{{ data.count }}</small>
            <i v-else-if="!data.link.accessible" class="el-icon-lock tree-lock"></i>
          </span>
        </el-tree>
      </aside>

      <main class="system-content" :class="{ 'is-embedded': selectedLink }">
        <template v-if="selectedLink">
          <div class="active-setting-header">
            <div class="active-setting-title">
              <i :class="selectedLink.icon"></i>
              <div>
                <h3>{{ selectedLink.title }}</h3>
                <p>{{ selectedLink.description }}</p>
              </div>
            </div>
            <el-button size="mini" icon="el-icon-folder-opened" @click="showGroup(selectedGroup)">返回目录</el-button>
          </div>

          <section class="embedded-wrapper">
            <iframe
              v-if="selectedLink.external"
              class="system-frame"
              :src="selectedLink.path"
              frameborder="0"
              title="GoView"
            ></iframe>
            <component
              v-else-if="activeViewComponent"
              :is="activeViewComponent"
              :key="selectedLink.path"
              class="embedded-system-view"
            />
            <el-empty v-else description="暂未配置对应的系统页面" />
          </section>
        </template>

        <template v-else>
          <div class="group-summary">
            <div>
              <h3>{{ currentGroup.title }}</h3>
              <p>{{ currentGroup.description }}</p>
            </div>
            <span>{{ activeLinks.length }} 项设置</span>
          </div>

          <div class="system-grid">
            <button
              v-for="item in activeLinks"
              :key="item.title"
              type="button"
              class="system-link"
              :class="{ 'is-locked': !item.accessible }"
              :aria-disabled="String(!item.accessible)"
              @click="selectLink(item)"
            >
              <i :class="item.icon"></i>
              <span class="link-copy">
                <strong>{{ item.title }}</strong>
                <small>{{ item.description }}</small>
              </span>
              <i :class="item.accessible ? 'el-icon-arrow-right' : 'el-icon-lock'" class="action-icon"></i>
            </button>
          </div>
        </template>
      </main>
    </div>
  </div>
</template>

<script>
import { carbonSystemGroups, carbonSystemLinks, canAccessByPermissions } from '@/utils/carbonAccess'
import SystemUser from '@/views/system/user/index'
import SystemDept from '@/views/system/dept/index'
import SystemPost from '@/views/system/post/index'
import SystemRole from '@/views/system/role/index'
import SystemMenu from '@/views/system/menu/index'
import SystemConfig from '@/views/system/config/index'
import CarbonScopes from '@/views/carbon/scopes'

const embeddedSystemViews = {
  '/system/user': SystemUser,
  '/system/dept': SystemDept,
  '/system/post': SystemPost,
  '/system/role': SystemRole,
  '/system/menu': SystemMenu,
  '/system/config': SystemConfig,
  '/carbon/scopes': CarbonScopes
}

function getSystemLinkKey(item) {
  return `link:${item.path}`
}

export default {
  name: 'CarbonSystem',
  data() {
    return {
      selectedGroup: '',
      selectedNodeKey: '',
      treeProps: {
        children: 'children',
        label: 'title'
      }
    }
  },
  computed: {
    systemLinks() {
      const permissions = this.$store.getters.permissions
      const roles = this.$store.getters.roles
      return carbonSystemLinks.map(item => ({
        ...item,
        accessible: canAccessByPermissions(item, permissions, roles)
      }))
    },
    accessibleCount() {
      return this.systemLinks.filter(item => item.accessible).length
    },
    visibleGroups() {
      return carbonSystemGroups.map(group => {
        const links = this.systemLinks.filter(item => item.group === group.key)
        return {
          ...group,
          links
        }
      }).filter(group => group.links.length)
    },
    treeData() {
      return this.visibleGroups.map(group => ({
        key: group.key,
        title: group.title,
        icon: group.icon,
        type: 'group',
        count: group.links.length,
        children: group.links.map(item => ({
          key: getSystemLinkKey(item),
          title: item.title,
          icon: item.icon,
          type: 'link',
          group: group.key,
          link: item
        }))
      }))
    },
    selectedLink() {
      return this.systemLinks.find(item => item.accessible && getSystemLinkKey(item) === this.selectedNodeKey) || null
    },
    activeViewComponent() {
      if (!this.selectedLink || this.selectedLink.external) {
        return null
      }
      return embeddedSystemViews[this.selectedLink.path] || null
    },
    currentGroup() {
      const groupKey = this.selectedLink ? this.selectedLink.group : this.selectedGroup
      return this.visibleGroups.find(group => group.key === groupKey) || this.visibleGroups[0] || {}
    },
    activeLinks() {
      return this.currentGroup.links || []
    }
  },
  watch: {
    visibleGroups: {
      immediate: true,
      handler(groups) {
        const validKeys = groups.reduce((keys, group) => {
          keys.push(group.key)
          group.links.forEach(item => keys.push(getSystemLinkKey(item)))
          return keys
        }, [])

        if (!groups.some(group => group.key === this.selectedGroup)) {
          this.selectedGroup = groups[0] ? groups[0].key : ''
        }
        if (!validKeys.includes(this.selectedNodeKey)) {
          this.selectedNodeKey = this.selectedGroup
        }
        this.setCurrentTreeKey(this.selectedNodeKey)
      }
    }
  },
  methods: {
    handleTreeNodeClick(data) {
      if (data.type === 'link') {
        this.selectLink(data.link)
        return
      }
      this.showGroup(data.key)
    },
    selectLink(item) {
      if (!item.accessible) {
        this.$message.warning(`当前账号没有“${item.title}”权限，请联系上级管理员分配权限`)
        return
      }
      this.selectedGroup = item.group
      this.selectedNodeKey = getSystemLinkKey(item)
      this.setCurrentTreeKey(this.selectedNodeKey)
    },
    showGroup(groupKey) {
      this.selectedGroup = groupKey
      this.selectedNodeKey = groupKey
      this.setCurrentTreeKey(groupKey)
    },
    setCurrentTreeKey(key) {
      this.$nextTick(() => {
        if (this.$refs.systemTree && key) {
          this.$refs.systemTree.setCurrentKey(key)
        }
      })
    }
  }
}
</script>

<style scoped lang="scss">
.carbon-system {
  background: #f6f8fb;
  min-height: calc(100vh - 84px);
  height: calc(100vh - 84px);
  overflow: auto;
}

.carbon-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;

  h2 {
    margin: 0 0 6px;
    color: #1f2d3d;
    font-size: 22px;
  }

  span {
    color: #7a8797;
  }
}

.system-layout {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: 16px;
  align-items: start;
  min-height: calc(100% - 76px);
}

.system-tree-panel,
.system-content {
  border: 1px solid #e5eaf1;
  border-radius: 6px;
  background: #fff;
}

.system-tree-panel {
  overflow: hidden;
}

.tree-heading {
  display: flex;
  justify-content: space-between;
  align-items: center;
  min-height: 56px;
  padding: 0 20px;
  border-bottom: 1px solid #edf0f5;
  color: #1f2d3d;
  font-size: 16px;
  font-weight: 700;

  small {
    color: #8b97a8;
    font-size: 13px;
    font-weight: 400;
  }
}

.system-tree {
  padding: 12px 14px 18px;
  font-size: 15px;

  ::v-deep .el-tree-node__content {
    height: 38px;
    border-radius: 5px;
  }

  ::v-deep .el-tree-node__children .el-tree-node__content {
    height: 36px;
  }

  ::v-deep .el-tree-node.is-current > .el-tree-node__content {
    background: #edf5ff;
  }
}

.tree-node {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  min-width: 0;
  color: #344054;
  font-size: 15px;

  i {
    flex: 0 0 auto;
    width: 18px;
    color: #2265b4;
    font-size: 17px;
    text-align: center;
  }

  span {
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  small {
    flex: 0 0 auto;
    min-width: 24px;
    height: 20px;
    line-height: 20px;
    border-radius: 10px;
    background: #edf5ff;
    color: #2265b4;
    text-align: center;
    font-size: 12px;
  }

  &.is-locked {
    color: #98a2b3;
  }

  .tree-lock {
    margin-left: auto;
    color: #98a2b3;
  }
}

.system-content {
  min-width: 0;
  padding: 18px;

  &.is-embedded {
    padding: 0;
    overflow: hidden;
  }
}

.group-summary {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid #edf0f5;

  h3 {
    margin: 0 0 8px;
    color: #1f2d3d;
    font-size: 20px;
  }

  p {
    max-width: 720px;
    margin: 0;
    color: #6b7788;
    line-height: 1.7;
  }

  > span {
    flex: 0 0 auto;
    min-width: 84px;
    height: 28px;
    line-height: 28px;
    border-radius: 14px;
    background: #f1f7ed;
    color: #3f7d3f;
    text-align: center;
    font-size: 13px;
  }
}

.system-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.system-link {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr) 20px;
  column-gap: 12px;
  align-items: center;
  min-height: 92px;
  padding: 18px;
  border: 1px solid #e5eaf1;
  border-radius: 6px;
  background: #fbfcfe;
  text-align: left;
  cursor: pointer;
  transition: border-color .2s, box-shadow .2s, transform .2s;

  > i:first-child {
    width: 42px;
    height: 42px;
    line-height: 42px;
    border-radius: 6px;
    text-align: center;
    color: #2265b4;
    background: #edf5ff;
    font-size: 22px;
  }

  .link-copy {
    min-width: 0;
  }

  strong {
    display: block;
    margin-bottom: 6px;
    color: #1f2d3d;
    font-size: 16px;
    font-weight: 700;
    overflow-wrap: anywhere;
  }

  small {
    display: block;
    color: #6b7788;
    line-height: 1.55;
    overflow-wrap: anywhere;
  }

  .action-icon {
    color: #a5afbd;
    font-size: 18px;
  }

  &:hover {
    border-color: #95b8d8;
    box-shadow: 0 10px 22px rgba(31, 45, 61, .08);
    transform: translateY(-1px);
  }

  &.is-locked {
    border-color: #e6e9ef;
    background: #f7f8fa;
    cursor: not-allowed;
    box-shadow: none;
    transform: none;

    > i:first-child,
    strong,
    small,
    .action-icon {
      color: #98a2b3;
    }
  }
}

.active-setting-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  min-height: 74px;
  padding: 14px 18px;
  border-bottom: 1px solid #edf0f5;
}

.active-setting-title {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;

  > i {
    flex: 0 0 auto;
    width: 42px;
    height: 42px;
    line-height: 42px;
    border-radius: 6px;
    background: #edf5ff;
    color: #2265b4;
    text-align: center;
    font-size: 22px;
  }

  h3 {
    margin: 0 0 5px;
    color: #1f2d3d;
    font-size: 19px;
  }

  p {
    margin: 0;
    color: #6b7788;
    line-height: 1.5;
  }
}

.embedded-wrapper {
  min-height: calc(100vh - 250px);
  background: #fff;
}

.embedded-system-view {
  min-height: calc(100vh - 250px);
  padding: 16px;
  background: #fff;
}

.system-frame {
  display: block;
  width: 100%;
  min-height: calc(100vh - 250px);
  border: 0;
  background: #fff;
}

.carbon-system ::v-deep .el-button--mini {
  min-height: 34px;
  padding: 8px 12px;
  font-size: 13px;
}

@media (max-width: 1100px) {
  .system-layout {
    grid-template-columns: 290px minmax(0, 1fr);
  }
}

@media (max-width: 900px) {
  .system-layout {
    grid-template-columns: 1fr;
  }

  .active-setting-header,
  .group-summary {
    flex-direction: column;
    align-items: stretch;
  }

  .system-grid {
    grid-template-columns: 1fr;
  }
}
</style>
