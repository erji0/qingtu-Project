<template>
  <view class="page tasks-page">
    <view class="summary">
      <view class="summary-item">
        <text class="summary-value running">{{ counts.running }}</text>
        <text class="summary-label">执行中</text>
      </view>
      <view class="summary-divider"></view>
      <view class="summary-item">
        <text class="summary-value pending">{{ counts.pending }}</text>
        <text class="summary-label">待执行</text>
      </view>
      <view class="summary-divider"></view>
      <view class="summary-item">
        <text class="summary-value finished">{{ counts.finished }}</text>
        <text class="summary-label">已完成</text>
      </view>
    </view>

    <view v-if="loading && !loaded" class="loading-box">
      <text class="muted">任务加载中...</text>
    </view>

    <view v-else-if="!tasks.length" class="empty-box">
      <view class="empty-icon">空</view>
      <text class="empty-title">暂无任务</text>
      <text class="empty-tip">当前没有分配给您的任务</text>
      <button class="refresh-button" @click="fetchTasks({ showLoading: true })">刷新</button>
    </view>

    <view v-else>
      <view v-for="group in groups" :key="group.key" class="group">
        <view v-if="group.items.length" class="group-header">
          <view class="group-title-wrap">
            <view class="group-dot" :class="'dot-' + group.key.toLowerCase()"></view>
            <text class="group-title">{{ group.title }}</text>
            <text class="group-count">({{ group.items.length }})</text>
          </view>
          <text class="group-hint">{{ group.hint }}</text>
        </view>

        <view
          v-for="task in group.items"
          :key="task.id"
          class="task-card"
          :class="'card-' + group.key.toLowerCase()"
          @click="openDetail(task)"
        >
          <view class="task-head">
            <view class="route-wrap">
              <text class="route-name">{{ task.routeName }}</text>
              <text class="task-date">{{ task.shiftDate }}</text>
            </view>
            <text class="status-badge" :class="'badge-' + group.key.toLowerCase()">
              {{ statusText(task.status) }}
            </text>
          </view>

          <view class="time-row">
            <text class="depart-time">{{ formatTime(task.departTime) }}</text>
            <text class="time-label">发车</text>
            <text class="plate">{{ task.plateNo || '未分配车辆' }}</text>
          </view>

          <view class="metrics">
            <view class="metric">
              <text class="metric-value">{{ task.reservedCount || 0 }}/{{ task.capacity || 0 }}</text>
              <text class="metric-label">预约 / 容量</text>
            </view>
            <view class="metric">
              <text class="metric-value">{{ (task.stops && task.stops.length) || 0 }}</text>
              <text class="metric-label">站点数</text>
            </view>
          </view>

          <view class="stops-row">
            <text class="stops-text">{{ firstStop(task) }} → {{ lastStop(task) }}</text>
            <uni-icons type="right" size="16" color="#c0c4cc"></uni-icons>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onHide, onPullDownRefresh, onShow, onUnload } from '@dcloudio/uni-app'
import { getTasks } from '../../api/driver'
import { getToken } from '../../api/request'

const tasks = ref([])
const loading = ref(false)
const loaded = ref(false)

let refreshTimer = null
let fetching = false

const groups = computed(() => [
  {
    key: 'RUNNING',
    title: '执行中',
    hint: '优先处理',
    items: tasks.value.filter((task) => task.status === 'RUNNING')
  },
  {
    key: 'PENDING',
    title: '待执行',
    hint: '等待发车',
    items: tasks.value.filter((task) => task.status === 'PENDING')
  },
  {
    key: 'FINISHED',
    title: '已完成',
    hint: '历史任务',
    items: tasks.value.filter((task) => task.status === 'FINISHED')
  }
])

const counts = computed(() => ({
  running: groups.value[0].items.length,
  pending: groups.value[1].items.length,
  finished: groups.value[2].items.length
}))

function statusText(status) {
  if (status === 'RUNNING') return '执行中'
  if (status === 'PENDING') return '待执行'
  if (status === 'FINISHED') return '已完成'
  return status || '未知'
}

function formatTime(time) {
  return time ? String(time).slice(0, 5) : '--:--'
}

function firstStop(task) {
  return task.stops && task.stops.length ? task.stops[0].name : '暂无站点'
}

function lastStop(task) {
  return task.stops && task.stops.length ? task.stops[task.stops.length - 1].name : '暂无站点'
}

async function fetchTasks(options = {}) {
  if (fetching) {
    if (options.stopPullDown) uni.stopPullDownRefresh()
    return
  }

  fetching = true
  if (options.showLoading) loading.value = true

  try {
    const data = await getTasks(options.silent === true)
    tasks.value = data || []
    loaded.value = true
  } catch (e) {
    // 错误提示已由请求封装统一处理
  } finally {
    fetching = false
    loading.value = false
    if (options.stopPullDown) uni.stopPullDownRefresh()
  }
}

function startAutoRefresh() {
  stopAutoRefresh()
  refreshTimer = setInterval(() => {
    fetchTasks({ silent: true })
  }, 10000)
}

function stopAutoRefresh() {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
}

function openDetail(task) {
  uni.navigateTo({
    url: `/pages/task-detail/task-detail?id=${task.id}`
  })
}

onShow(() => {
  if (!getToken()) {
    uni.reLaunch({ url: '/pages/login/login' })
    return
  }
  fetchTasks({ showLoading: !loaded.value })
  startAutoRefresh()
})

onHide(() => {
  stopAutoRefresh()
})

onUnload(() => {
  stopAutoRefresh()
})

onPullDownRefresh(() => {
  fetchTasks({ stopPullDown: true })
})
</script>

<style scoped>
.tasks-page {
  padding-bottom: 40rpx;
}

.summary {
  display: flex;
  align-items: center;
  background-color: #ffffff;
  border-radius: 20rpx;
  padding: 28rpx 16rpx;
  margin-bottom: 28rpx;
  box-shadow: 0 6rpx 20rpx rgba(31, 45, 61, 0.05);
}

.summary-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.summary-value {
  font-size: 40rpx;
  font-weight: 700;
  line-height: 1.2;
}

.summary-value.running {
  color: #8BA3C7;
}

.summary-value.pending {
  color: #ff9900;
}

.summary-value.finished {
  color: #18b566;
}

.summary-label {
  margin-top: 8rpx;
  font-size: 24rpx;
  color: #909399;
}

.summary-divider {
  width: 2rpx;
  height: 56rpx;
  background-color: #ebeef5;
}

.group {
  margin-bottom: 12rpx;
}

.group-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8rpx 8rpx 18rpx;
}

.group-title-wrap {
  display: flex;
  align-items: center;
}

.group-dot {
  width: 12rpx;
  height: 12rpx;
  border-radius: 6rpx;
  margin-right: 12rpx;
}

.dot-running {
  background-color: #8BA3C7;
}

.dot-pending {
  background-color: #ff9900;
}

.dot-finished {
  background-color: #18b566;
}

.group-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #1f2d3d;
}

.group-count {
  margin-left: 6rpx;
  font-size: 24rpx;
  color: #909399;
}

.group-hint {
  font-size: 22rpx;
  color: #b3b9c4;
}

.task-card {
  background-color: #ffffff;
  border-radius: 20rpx;
  padding: 26rpx 28rpx;
  margin-bottom: 20rpx;
  border-left: 8rpx solid #dcdfe6;
  box-shadow: 0 6rpx 20rpx rgba(31, 45, 61, 0.05);
}

.card-running {
  border-left-color: #8BA3C7;
}

.card-pending {
  border-left-color: #ff9900;
}

.card-finished {
  border-left-color: #18b566;
  opacity: 0.9;
}

.task-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}

.route-wrap {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding-right: 16rpx;
}

.route-name {
  font-size: 32rpx;
  font-weight: 600;
  color: #1f2d3d;
  line-height: 1.35;
}

.task-date {
  margin-top: 8rpx;
  font-size: 24rpx;
  color: #909399;
}

.status-badge {
  flex-shrink: 0;
  padding: 6rpx 18rpx;
  border-radius: 999rpx;
  font-size: 22rpx;
  font-weight: 500;
}

.badge-running {
  color: #ffffff;
  background-color: #8BA3C7;
}

.badge-pending {
  color: #b36b00;
  background-color: #fff3e0;
}

.badge-finished {
  color: #0b8f50;
  background-color: #e8f8ef;
}

.time-row {
  display: flex;
  align-items: baseline;
  margin-top: 22rpx;
}

.depart-time {
  font-size: 44rpx;
  font-weight: 700;
  color: #8BA3C7;
  letter-spacing: 1rpx;
}

.time-label {
  margin-left: 8rpx;
  font-size: 22rpx;
  color: #909399;
}

.plate {
  margin-left: auto;
  font-size: 26rpx;
  color: #606266;
  padding: 4rpx 14rpx;
  background-color: #f5f7fb;
  border-radius: 8rpx;
}

.metrics {
  display: flex;
  margin-top: 22rpx;
  padding: 18rpx 0;
  border-top: 2rpx solid #f0f2f5;
  border-bottom: 2rpx solid #f0f2f5;
}

.metric {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.metric + .metric {
  border-left: 2rpx solid #f0f2f5;
  padding-left: 24rpx;
}

.metric-value {
  font-size: 30rpx;
  font-weight: 600;
  color: #303133;
}

.metric-label {
  margin-top: 6rpx;
  font-size: 22rpx;
  color: #909399;
}

.stops-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 20rpx;
}

.stops-text {
  flex: 1;
  font-size: 24rpx;
  color: #606266;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.loading-box {
  padding: 160rpx 0;
  text-align: center;
}

.empty-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 140rpx 40rpx;
  background-color: #ffffff;
  border-radius: 20rpx;
}

.empty-icon {
  width: 120rpx;
  height: 120rpx;
  border-radius: 60rpx;
  background-color: #f0f5ff;
  color: #8BA3C7;
  font-size: 32rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.empty-title {
  margin-top: 24rpx;
  font-size: 30rpx;
  font-weight: 600;
  color: #303133;
}

.empty-tip {
  margin-top: 10rpx;
  font-size: 24rpx;
  color: #909399;
}

.refresh-button {
  margin-top: 36rpx;
  width: 240rpx;
  height: 72rpx;
  line-height: 72rpx;
  border-radius: 36rpx;
  font-size: 26rpx;
  color: #8BA3C7;
  background-color: #f0f5ff;
}
</style>
