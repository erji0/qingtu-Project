<template>
  <view class="page mine-page">
    <view class="profile-card">
      <view class="avatar">{{ avatarText }}</view>
      <view class="profile-info">
        <text class="profile-name">{{ displayName }}</text>
        <text class="profile-sub">司机账号：{{ user ? user.username : '--' }}</text>
      </view>
      <view class="role-badge">司机</view>
    </view>

    <view class="card stats-card">
      <view class="card-title">
        今日任务
        <text class="title-extra">{{ today }}</text>
      </view>

      <view class="stats-row">
        <view class="stat-item">
          <text class="stat-value running">{{ runningCount }}</text>
          <text class="stat-label">执行中</text>
        </view>
        <view class="stat-item">
          <text class="stat-value pending">{{ pendingCount }}</text>
          <text class="stat-label">待执行</text>
        </view>
        <view class="stat-item">
          <text class="stat-value finished">{{ finishedCount }}</text>
          <text class="stat-label">已完成</text>
        </view>
      </view>

      <view class="stats-tip">{{ statsTip }}</view>
    </view>

    <view class="card">
      <view class="card-title">账号信息</view>
      <view class="info-row">
        <text class="info-label">姓名</text>
        <text class="info-value">{{ displayName }}</text>
      </view>
      <view class="info-row">
        <text class="info-label">账号</text>
        <text class="info-value">{{ user ? user.username : '--' }}</text>
      </view>
      <view class="info-row last">
        <text class="info-label">身份</text>
        <text class="info-value">校园巴士司机</text>
      </view>
    </view>

    <button class="logout-button" @click="handleLogout">退出登录</button>
  </view>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getTasks } from '../../api/driver'
import { clearAuth, getDriverUser, getToken } from '../../api/request'

const user = ref(null)
const loading = ref(false)

const stats = reactive({
  today: 0,
  running: 0,
  pending: 0,
  finished: 0
})

const displayName = computed(() => {
  if (!user.value) return '司机'
  return user.value.name || user.value.username || '司机'
})

const avatarText = computed(() => displayName.value.slice(0, 1))

const today = computed(() => stats.today)
const runningCount = computed(() => stats.running)
const pendingCount = computed(() => stats.pending)
const finishedCount = computed(() => stats.finished)

const statsTip = computed(() => {
  if (!stats.today) return '今日暂无任务安排'
  return `今日共 ${stats.today} 个任务，请注意发车时间`
})

function getTodayString() {
  const now = new Date()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  return `${now.getFullYear()}-${month}-${day}`
}

async function loadStats() {
  if (loading.value) return
  loading.value = true
  try {
    const tasks = (await getTasks(true)) || []
    const todayStr = getTodayString()
    const todayTasks = tasks.filter((task) => task.shiftDate === todayStr)

    stats.today = todayTasks.length
    stats.running = todayTasks.filter((task) => task.status === 'RUNNING').length
    stats.pending = todayTasks.filter((task) => task.status === 'PENDING').length
    stats.finished = todayTasks.filter((task) => task.status === 'FINISHED').length
  } catch (e) {
    // 错误提示已由请求封装统一处理
  } finally {
    loading.value = false
  }
}

function handleLogout() {
  uni.showModal({
    title: '退出登录',
    content: '确认退出当前司机账号吗？',
    confirmText: '退出',
    confirmColor: '#f56c6c',
    success: (res) => {
      if (!res.confirm) return
      clearAuth()
      uni.reLaunch({ url: '/pages/login/login' })
    }
  })
}

onShow(() => {
  if (!getToken()) {
    uni.reLaunch({ url: '/pages/login/login' })
    return
  }
  user.value = getDriverUser()
  loadStats()
})
</script>

<style scoped>
.mine-page {
  padding-bottom: 48rpx;
}

.profile-card {
  display: flex;
  align-items: center;
  padding: 40rpx 32rpx;
  margin-bottom: 24rpx;
  border-radius: 24rpx;
  background: linear-gradient(135deg, #8BA3C7, #5b9dff);
  box-shadow: 0 12rpx 32rpx rgba(139, 163, 199, 0.22);
}

.avatar {
  width: 100rpx;
  height: 100rpx;
  border-radius: 50rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36rpx;
  font-weight: 700;
  color: #8BA3C7;
  background-color: #ffffff;
  flex-shrink: 0;
}

.profile-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  margin-left: 24rpx;
  min-width: 0;
}

.profile-name {
  font-size: 38rpx;
  font-weight: 700;
  color: #ffffff;
}

.profile-sub {
  margin-top: 10rpx;
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.82);
}

.role-badge {
  flex-shrink: 0;
  padding: 6rpx 18rpx;
  border-radius: 999rpx;
  font-size: 22rpx;
  color: #8BA3C7;
  background-color: #ffffff;
}

.card-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.title-extra {
  font-size: 26rpx;
  font-weight: 600;
  color: #8BA3C7;
}

.stats-row {
  display: flex;
  margin-top: 10rpx;
  padding: 20rpx 0 8rpx;
}

.stat-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stat-item + .stat-item {
  border-left: 2rpx solid #f0f2f5;
}

.stat-value {
  font-size: 42rpx;
  font-weight: 700;
  line-height: 1.2;
}

.stat-value.running {
  color: #8BA3C7;
}

.stat-value.pending {
  color: #ff9900;
}

.stat-value.finished {
  color: #18b566;
}

.stat-label {
  margin-top: 8rpx;
  font-size: 24rpx;
  color: #909399;
}

.stats-tip {
  margin-top: 20rpx;
  padding-top: 20rpx;
  border-top: 2rpx solid #f0f2f5;
  font-size: 22rpx;
  color: #909399;
  text-align: center;
}

.info-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx 0;
  border-bottom: 2rpx solid #f0f2f5;
}

.info-row.last {
  border-bottom: none;
  padding-bottom: 0;
}

.info-label {
  font-size: 26rpx;
  color: #909399;
}

.info-value {
  font-size: 26rpx;
  color: #303133;
}

.logout-button {
  margin-top: 12rpx;
  width: 100%;
  height: 88rpx;
  line-height: 88rpx;
  border-radius: 44rpx;
  font-size: 30rpx;
  color: #f56c6c;
  background-color: #ffffff;
  border: 2rpx solid #fbc4c4;
}
</style>
