<template>
  <el-container class="layout">
    <el-aside width="220px" class="sidebar">
      <div class="logo">
        <div class="logo-badge">
          <el-icon><Van /></el-icon>
        </div>
        <div class="logo-text">
          <span class="logo-name">青途校车</span>
          <span class="logo-sub">智能调度管理端</span>
        </div>
      </div>

      <el-menu
        :default-active="activeMenu"
        class="side-menu"
        router
        background-color="transparent"
        text-color="rgba(255, 255, 255, 0.72)"
        active-text-color="#ffffff"
      >
        <el-menu-item v-for="item in menus" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.title }}</span>
        </el-menu-item>
      </el-menu>

      <div class="sidebar-footer">校园校车智能调度系统</div>
    </el-aside>

    <el-container class="main-container">
      <el-header class="topbar">
        <div class="topbar-left">
          <span class="topbar-title">{{ currentTitle }}</span>
          <el-tag size="small" effect="plain" class="env-tag">管理端</el-tag>
        </div>
        <div class="topbar-right">
          <span class="today">{{ todayText }}</span>
          <el-dropdown trigger="click" @command="handleCommand">
            <span class="user-trigger">
              <span class="avatar">{{ initial }}</span>
              <span class="user-name">{{ user.name || user.username || '管理员' }}</span>
              <el-icon class="arrow"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item disabled>
                  <el-icon><User /></el-icon>{{ user.username || '' }}
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="content">
        <router-view v-slot="{ Component }">
          <component :is="Component" />
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { clearAuth, getUser } from '../utils/auth'

const route = useRoute()
const router = useRouter()

const user = getUser() || {}

const menus = [
  { path: '/dashboard', title: '仪表盘', icon: 'Odometer' },
  { path: '/routes', title: '线路管理', icon: 'Guide' },
  { path: '/stops', title: '站点管理', icon: 'Location' },
  { path: '/vehicles', title: '车辆管理', icon: 'Van' },
  { path: '/shifts', title: '班次管理', icon: 'Tickets' },
  { path: '/reservations', title: '预约记录', icon: 'Document' },
  { path: '/statistics', title: '预约统计', icon: 'TrendCharts' },
  { path: '/suggestions', title: '调度建议', icon: 'MagicStick' },
  { path: '/drivers', title: '司机管理', icon: 'User' },
  { path: '/monitor', title: '车辆监控', icon: 'Monitor' }
]

const activeMenu = computed(() => route.path)
const currentTitle = computed(() => route.meta?.title || '仪表盘')
const initial = computed(() => (user.name || user.username || 'A').slice(0, 1))

const todayText = computed(() => {
  const date = new Date()
  const week = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][date.getDay()]
  return `${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日 ${week}`
})

async function handleCommand(command) {
  if (command !== 'logout') return
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '退出登录', {
      confirmButtonText: '退出',
      cancelButtonText: '取消',
      type: 'warning'
    })
    clearAuth()
    router.replace('/login')
  } catch (e) {
    // 用户取消
  }
}
</script>

<style scoped>
.layout {
  height: 100vh;
  overflow: hidden;
}

.sidebar {
  display: flex;
  flex-direction: column;
  background: linear-gradient(180deg, #2E3F57 0%, #5E7496 55%, #8BA3C7 100%);
  color: #fff;
  overflow: hidden;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 22px 18px 18px;
}

.logo-badge {
  width: 40px;
  height: 40px;
  border-radius: 11px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.16);
  border: 1px solid rgba(255, 255, 255, 0.22);
  font-size: 20px;
}

.logo-text {
  display: flex;
  flex-direction: column;
  line-height: 1.25;
}

.logo-name {
  font-size: 17px;
  font-weight: 700;
  letter-spacing: 1px;
}

.logo-sub {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.62);
  letter-spacing: 1px;
}

.side-menu {
  flex: 1;
  border-right: none;
  padding: 6px 12px;
  overflow-y: auto;
}

.side-menu :deep(.el-menu-item) {
  height: 44px;
  line-height: 44px;
  margin-bottom: 4px;
  border-radius: 9px;
}

.side-menu :deep(.el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.1);
  color: #fff;
}

.side-menu :deep(.el-menu-item.is-active) {
  background: rgba(255, 255, 255, 0.18);
  color: #fff;
  font-weight: 600;
}

.sidebar-footer {
  padding: 14px 18px 18px;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.42);
  letter-spacing: 0.5px;
}

.main-container {
  min-width: 0;
  background: var(--qt-bg);
}

.topbar {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid var(--qt-border);
  padding: 0 24px;
}

.topbar-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.topbar-title {
  font-size: 17px;
  font-weight: 600;
  color: #111827;
}

.env-tag {
  color: var(--qt-primary);
  border-color: #c7d7fb;
  background: #f3f7ff;
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 22px;
}

.today {
  font-size: 13px;
  color: var(--qt-text-secondary);
}

.user-trigger {
  display: flex;
  align-items: center;
  gap: 9px;
  cursor: pointer;
  padding: 5px 8px;
  border-radius: 8px;
  outline: none;
}

.user-trigger:hover {
  background: #f4f7fd;
}

.avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  color: #fff;
  background: linear-gradient(135deg, #8BA3C7, #A9BCD6);
}

.user-name {
  font-size: 14px;
  color: #374151;
}

.arrow {
  color: #9ca3af;
  font-size: 12px;
}

.content {
  padding: 22px 24px 28px;
  overflow-y: auto;
  background: var(--qt-bg);
}
</style>
