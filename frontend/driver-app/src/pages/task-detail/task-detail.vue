<template>
  <view class="page detail-page">
    <view v-if="loading && !task" class="loading-box">
      <text class="muted">任务加载中...</text>
    </view>

    <view v-else-if="!task" class="empty-box">
      <text class="empty-title">任务不存在或无权查看</text>
      <button class="refresh-button" @click="loadTask">重新加载</button>
    </view>

    <view v-else>
      <view class="status-banner" :class="'banner-' + statusClass">
        <view class="banner-main">
          <text class="banner-status">{{ statusText(task.status) }}</text>
          <text class="banner-route">{{ task.routeName }}</text>
        </view>
        <view class="banner-right">
          <text class="banner-date">{{ task.shiftDate }}</text>
          <text class="banner-time">{{ formatTime(task.departTime) }} 发车</text>
        </view>
      </view>

      <view class="card">
        <view class="card-title">任务信息</view>

        <view class="info-row">
          <text class="info-label">线路</text>
          <text class="info-value">{{ task.routeName }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">班次日期</text>
          <text class="info-value">{{ task.shiftDate }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">发车时间</text>
          <text class="info-value strong">{{ formatTime(task.departTime) }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">车辆车牌</text>
          <text class="info-value">{{ task.plateNo || '未分配车辆' }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">预约 / 容量</text>
          <text class="info-value">{{ task.reservedCount || 0 }} / {{ task.capacity || 0 }} 人</text>
        </view>
        <view class="info-row">
          <text class="info-label">任务状态</text>
          <text class="info-value status-text" :class="'status-' + statusClass">{{ statusText(task.status) }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">开始时间</text>
          <text class="info-value">{{ formatDateTime(task.startTime) }}</text>
        </view>
        <view class="info-row last">
          <text class="info-label">结束时间</text>
          <text class="info-value">{{ formatDateTime(task.endTime) }}</text>
        </view>
      </view>

      <view class="card">
        <view class="card-title">
          线路站点
          <text class="title-extra">共 {{ (task.stops && task.stops.length) || 0 }} 站</text>
        </view>

        <view v-if="!task.stops || !task.stops.length" class="section-empty">暂无站点信息</view>

        <view v-else class="stop-list">
          <view
            v-for="(stop, index) in task.stops"
            :key="stop.id"
            class="stop-item"
          >
            <view class="stop-index">{{ index + 1 }}</view>
            <view class="stop-line">
              <view v-if="index !== 0" class="line-before"></view>
              <view v-if="index !== task.stops.length - 1" class="line-after"></view>
            </view>
            <view class="stop-body">
              <text class="stop-name">{{ stop.name }}</text>
              <text v-if="index === 0" class="stop-tag">始发站</text>
              <text v-else-if="index === task.stops.length - 1" class="stop-tag">终点站</text>
            </view>
          </view>
        </view>
      </view>

      <view v-if="task.status === 'RUNNING' || task.status === 'FINISHED'" class="card">
        <view class="card-title">
          车辆位置
          <text v-if="location" class="title-extra update-time">更新于 {{ location.updateTime || '--' }}</text>
        </view>

        <view v-if="!location" class="section-empty">暂无车辆位置信息</view>

        <view v-else>
          <view class="location-head">
            <view class="location-block">
              <text class="location-label">当前站</text>
              <text class="location-value current">{{ location.currentStopName || '--' }}</text>
            </view>
            <view class="location-arrow">→</view>
            <view class="location-block right">
              <text class="location-label">下一站</text>
              <text class="location-value next">{{ location.nextStopName || '--' }}</text>
            </view>
          </view>

          <view class="progress-wrap">
            <view class="progress-info">
              <text class="progress-label">行程进度</text>
              <text class="progress-percent">{{ location.progressPercent || 0 }}%</text>
            </view>
            <view class="progress-track">
              <view class="progress-fill" :style="{ width: progressWidth }"></view>
            </view>
          </view>

          <view class="eta-row">
            <view class="eta-item">
              <text class="eta-value">{{ location.etaNextStopMinutes || 0 }}</text>
              <text class="eta-label">距下一站(分钟)</text>
            </view>
            <view class="eta-item">
              <text class="eta-value">{{ location.etaArriveMinutes || 0 }}</text>
              <text class="eta-label">预计到达终点(分钟)</text>
            </view>
            <view class="eta-item">
              <text class="eta-value" :class="'run-' + runStatusClass">{{ runStatusText }}</text>
              <text class="eta-label">运行状态</text>
            </view>
          </view>

          <view class="card-title sub-title">站点进度</view>
          <view class="location-stops">
            <view
              v-for="(stop, index) in location.stops"
              :key="stop.id"
              class="location-stop"
              :class="'state-' + (stop.state || 'waiting').toLowerCase()"
            >
              <view class="loc-dot-wrap">
                <view class="loc-dot"></view>
                <view v-if="index !== location.stops.length - 1" class="loc-line"></view>
              </view>
              <view class="loc-body">
                <text class="loc-name">{{ stop.name }}</text>
                <text class="loc-state">{{ stopStateText(stop.state) }}</text>
              </view>
            </view>
          </view>
        </view>
      </view>

      <view class="action-card">
        <view v-if="task.status === 'PENDING'">
          <button class="action-button primary" :loading="actionLoading" :disabled="actionLoading" @click="handleStart">
            开始任务
          </button>
          <view class="action-tip">确认发车前请核对线路、车辆与预约人数</view>
        </view>

        <view v-else-if="task.status === 'RUNNING'">
          <button class="action-button finish" :loading="actionLoading" :disabled="actionLoading" @click="handleFinish">
            结束任务
          </button>
          <button
            class="action-button outline"
            :class="{ 'outline-warning': isDelayed }"
            :loading="runStatusLoading"
            :disabled="runStatusLoading || actionLoading"
            @click="handleToggleRunStatus"
          >
            {{ isDelayed ? '恢复正常运行' : '标记延误' }}
          </button>
          <view class="action-tip">运行中位置每 10 秒自动刷新</view>
        </view>

        <view v-else-if="task.status === 'FINISHED'" class="finished-tip">
          <view class="finished-title">任务已完成</view>
          <view class="finished-desc">本任务已结束，所有操作已关闭</view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onHide, onLoad, onPullDownRefresh, onShow, onUnload } from '@dcloudio/uni-app'
import {
  finishTask,
  getTaskDetail,
  getTaskLocation,
  startTask,
  updateRunStatus
} from '../../api/driver'
import { getToken } from '../../api/request'

const task = ref(null)
const location = ref(null)
const loading = ref(false)
const actionLoading = ref(false)
const runStatusLoading = ref(false)

let taskId = null
let locationTimer = null
let locationFetching = false
let pageVisible = false

const statusClass = computed(() => {
  return task.value ? String(task.value.status || '').toLowerCase() : 'unknown'
})

const isDelayed = computed(() => {
  return !!location.value && location.value.runStatus === 'DELAYED'
})

const runStatusClass = computed(() => {
  return location.value ? String(location.value.runStatus || 'running').toLowerCase() : 'running'
})

const runStatusText = computed(() => {
  if (!location.value) return '--'
  const status = location.value.runStatus
  if (status === 'DELAYED') return '延误'
  if (status === 'ARRIVED') return '已到达'
  if (status === 'RUNNING') return '正常'
  return status || '--'
})

const progressWidth = computed(() => {
  const value = location.value && location.value.progressPercent ? location.value.progressPercent : 0
  return `${Math.max(0, Math.min(100, Number(value)))}%`
})

function statusText(status) {
  if (status === 'RUNNING') return '执行中'
  if (status === 'PENDING') return '待执行'
  if (status === 'FINISHED') return '已完成'
  return status || '未知'
}

function formatTime(time) {
  return time ? String(time).slice(0, 5) : '--:--'
}

function formatDateTime(value) {
  if (!value) return '--'
  return String(value).replace('T', ' ').slice(0, 16)
}

function stopStateText(state) {
  if (state === 'PASSED') return '已经过'
  if (state === 'CURRENT') return '当前站'
  if (state === 'NEXT') return '下一站'
  return '未到达'
}

function toast(message) {
  uni.showToast({ title: message, icon: 'none' })
}

async function loadTask() {
  if (!taskId) return
  loading.value = true
  try {
    const data = await getTaskDetail(taskId)
    task.value = data
    if (data && data.location) {
      location.value = data.location
    }
    syncLocationPolling()
  } catch (e) {
    // 错误提示已由请求封装统一处理
  } finally {
    loading.value = false
  }
}

async function fetchLocation(silent = true) {
  if (!taskId || locationFetching) return
  locationFetching = true
  try {
    const data = await getTaskLocation(taskId, silent)
    location.value = data || null
  } catch (e) {
    // 轮询失败静默处理
  } finally {
    locationFetching = false
  }
}

function startLocationPolling() {
  stopLocationPolling()
  fetchLocation(false)
  locationTimer = setInterval(() => {
    fetchLocation(true)
  }, 10000)
}

function stopLocationPolling() {
  if (locationTimer) {
    clearInterval(locationTimer)
    locationTimer = null
  }
}

function syncLocationPolling() {
  if (!pageVisible || !task.value) return
  if (task.value.status === 'RUNNING') {
    startLocationPolling()
  } else if (task.value.status === 'FINISHED') {
    stopLocationPolling()
    fetchLocation(false)
  } else {
    stopLocationPolling()
  }
}

function handleStart() {
  if (!task.value || actionLoading.value) return
  uni.showModal({
    title: '开始任务',
    content: '确认开始执行该任务吗？请再次核对车辆与线路。',
    confirmText: '开始',
    success: async (res) => {
      if (!res.confirm) return
      actionLoading.value = true
      try {
        const data = await startTask(taskId)
        task.value = data
        if (data && data.location) location.value = data.location
        uni.showToast({ title: '任务已开始', icon: 'success' })
        syncLocationPolling()
      } catch (e) {
        // 错误提示已由请求封装统一处理
      } finally {
        actionLoading.value = false
      }
    }
  })
}

function handleFinish() {
  if (!task.value || actionLoading.value) return
  uni.showModal({
    title: '结束任务',
    content: '确认结束当前任务吗？结束后将不能再次开始。',
    confirmText: '确认结束',
    confirmColor: '#f56c6c',
    success: async (res) => {
      if (!res.confirm) return
      actionLoading.value = true
      try {
        const data = await finishTask(taskId)
        task.value = data
        if (data && data.location) location.value = data.location
        stopLocationPolling()
        uni.showToast({ title: '任务已完成', icon: 'success' })
      } catch (e) {
        // 错误提示已由请求封装统一处理
      } finally {
        actionLoading.value = false
      }
    }
  })
}

function handleToggleRunStatus() {
  if (!task.value || runStatusLoading.value) return
  const nextStatus = isDelayed.value ? 'RUNNING' : 'DELAYED'
  const content = isDelayed.value
    ? '确认恢复正常运行吗？'
    : '确认将该任务标记为延误吗？'

  uni.showModal({
    title: isDelayed.value ? '恢复正常' : '标记延误',
    content,
    confirmText: '确认',
    success: async (res) => {
      if (!res.confirm) return
      runStatusLoading.value = true
      try {
        const data = await updateRunStatus(taskId, nextStatus)
        location.value = data || null
        uni.showToast({ title: nextStatus === 'DELAYED' ? '已标记延误' : '已恢复正常', icon: 'success' })
      } catch (e) {
        // 错误提示已由请求封装统一处理
      } finally {
        runStatusLoading.value = false
      }
    }
  })
}

onLoad((options) => {
  taskId = options && options.id ? options.id : null
  if (!getToken()) {
    uni.reLaunch({ url: '/pages/login/login' })
    return
  }
  if (!taskId) {
    toast('缺少任务参数')
    return
  }
  loadTask()
})

onShow(() => {
  pageVisible = true
  syncLocationPolling()
})

onHide(() => {
  pageVisible = false
  stopLocationPolling()
})

onUnload(() => {
  pageVisible = false
  stopLocationPolling()
})

onPullDownRefresh(async () => {
  await loadTask()
  uni.stopPullDownRefresh()
})
</script>

<style scoped>
.detail-page {
  padding-bottom: 40rpx;
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

.empty-title {
  font-size: 30rpx;
  color: #606266;
}

.refresh-button {
  margin-top: 32rpx;
  width: 240rpx;
  height: 72rpx;
  line-height: 72rpx;
  border-radius: 36rpx;
  font-size: 26rpx;
  color: #2f7df6;
  background-color: #f0f5ff;
}

.status-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 32rpx;
  border-radius: 24rpx;
  margin-bottom: 24rpx;
  color: #ffffff;
}

.banner-running {
  background: linear-gradient(135deg, #2f7df6, #5b9dff);
}

.banner-pending {
  background: linear-gradient(135deg, #ff9900, #ffb84d);
}

.banner-finished {
  background: linear-gradient(135deg, #18b566, #42c986);
}

.banner-unknown {
  background: linear-gradient(135deg, #909399, #b3b9c4);
}

.banner-main {
  display: flex;
  flex-direction: column;
}

.banner-status {
  font-size: 38rpx;
  font-weight: 700;
  letter-spacing: 1rpx;
}

.banner-route {
  margin-top: 10rpx;
  font-size: 26rpx;
  opacity: 0.9;
}

.banner-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.banner-date {
  font-size: 24rpx;
  opacity: 0.9;
}

.banner-time {
  margin-top: 8rpx;
  font-size: 28rpx;
  font-weight: 600;
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
}

.info-label {
  font-size: 26rpx;
  color: #909399;
}

.info-value {
  max-width: 70%;
  font-size: 26rpx;
  color: #303133;
  text-align: right;
  word-break: break-all;
}

.info-value.strong {
  color: #2f7df6;
  font-size: 30rpx;
  font-weight: 600;
}

.status-text {
  font-weight: 600;
}

.status-text.status-running {
  color: #2f7df6;
}

.status-text.status-pending {
  color: #ff9900;
}

.status-text.status-finished {
  color: #18b566;
}

.title-extra {
  float: right;
  font-size: 22rpx;
  font-weight: normal;
  color: #909399;
}

.title-extra.update-time {
  max-width: 55%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.section-empty {
  padding: 40rpx 0;
  text-align: center;
  font-size: 24rpx;
  color: #b3b9c4;
}

.stop-list {
  padding-top: 4rpx;
}

.stop-item {
  display: flex;
  align-items: center;
  min-height: 76rpx;
}

.stop-index {
  width: 44rpx;
  height: 44rpx;
  line-height: 44rpx;
  border-radius: 22rpx;
  text-align: center;
  font-size: 22rpx;
  color: #2f7df6;
  background-color: #eaf2ff;
  flex-shrink: 0;
}

.stop-line {
  position: relative;
  width: 36rpx;
  height: 76rpx;
  flex-shrink: 0;
}

.line-before,
.line-after {
  position: absolute;
  left: 17rpx;
  width: 2rpx;
  background-color: #e4e7ed;
}

.line-before {
  top: 0;
  height: 50%;
}

.line-after {
  top: 50%;
  height: 50%;
}

.stop-body {
  flex: 1;
  display: flex;
  align-items: center;
  padding: 16rpx 0;
}

.stop-name {
  font-size: 28rpx;
  color: #303133;
}

.stop-tag {
  margin-left: 14rpx;
  padding: 2rpx 12rpx;
  border-radius: 8rpx;
  font-size: 20rpx;
  color: #2f7df6;
  background-color: #eaf2ff;
}

.location-head {
  display: flex;
  align-items: center;
  padding: 12rpx 0 28rpx;
}

.location-block {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.location-block.right {
  align-items: flex-end;
}

.location-label {
  font-size: 22rpx;
  color: #909399;
}

.location-value {
  margin-top: 8rpx;
  font-size: 32rpx;
  font-weight: 600;
}

.location-value.current {
  color: #2f7df6;
}

.location-value.next {
  color: #ff9900;
}

.location-arrow {
  padding: 0 20rpx;
  font-size: 32rpx;
  color: #c0c4cc;
}

.progress-wrap {
  padding: 8rpx 0;
}

.progress-info {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12rpx;
}

.progress-label {
  font-size: 24rpx;
  color: #909399;
}

.progress-percent {
  font-size: 26rpx;
  font-weight: 600;
  color: #2f7df6;
}

.progress-track {
  height: 16rpx;
  border-radius: 8rpx;
  background-color: #eef1f6;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  border-radius: 8rpx;
  background: linear-gradient(90deg, #2f7df6, #63a4ff);
  transition: width 0.4s ease;
}

.eta-row {
  display: flex;
  margin-top: 24rpx;
  padding: 20rpx 0;
  border-top: 2rpx solid #f0f2f5;
}

.eta-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.eta-item + .eta-item {
  border-left: 2rpx solid #f0f2f5;
}

.eta-value {
  font-size: 30rpx;
  font-weight: 600;
  color: #303133;
}

.eta-value.run-running {
  color: #18b566;
}

.eta-value.run-delayed {
  color: #f56c6c;
}

.eta-value.run-arrived {
  color: #909399;
}

.eta-label {
  margin-top: 8rpx;
  font-size: 20rpx;
  color: #909399;
  text-align: center;
}

.sub-title {
  margin-top: 32rpx;
  padding-top: 28rpx;
  border-top: 2rpx solid #f0f2f5;
}

.location-stops {
  padding-top: 4rpx;
}

.location-stop {
  display: flex;
  align-items: center;
  min-height: 68rpx;
}

.loc-dot-wrap {
  position: relative;
  width: 36rpx;
  height: 68rpx;
  flex-shrink: 0;
  display: flex;
  align-items: center;
}

.loc-dot {
  width: 14rpx;
  height: 14rpx;
  border-radius: 7rpx;
  background-color: #dcdfe6;
  margin-left: 8rpx;
}

.state-passed .loc-dot {
  background-color: #c0c4cc;
}

.state-current .loc-dot {
  width: 18rpx;
  height: 18rpx;
  border-radius: 9rpx;
  margin-left: 6rpx;
  background-color: #2f7df6;
  box-shadow: 0 0 0 6rpx rgba(47, 125, 246, 0.15);
}

.state-next .loc-dot {
  background-color: #ff9900;
}

.loc-line {
  position: absolute;
  left: 14rpx;
  top: 46rpx;
  width: 2rpx;
  height: 34rpx;
  background-color: #e4e7ed;
}

.loc-body {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.loc-name {
  font-size: 26rpx;
  color: #303133;
}

.state-passed .loc-name {
  color: #a8abb2;
}

.state-current .loc-name {
  color: #2f7df6;
  font-weight: 600;
}

.loc-state {
  font-size: 20rpx;
  color: #b3b9c4;
}

.state-current .loc-state {
  color: #2f7df6;
}

.state-next .loc-state {
  color: #ff9900;
}

.action-card {
  background-color: #ffffff;
  border-radius: 20rpx;
  padding: 28rpx;
  box-shadow: 0 6rpx 20rpx rgba(31, 45, 61, 0.05);
}

.action-button {
  width: 100%;
  height: 88rpx;
  line-height: 88rpx;
  border-radius: 44rpx;
  font-size: 30rpx;
  font-weight: 600;
  margin-bottom: 20rpx;
}

.action-button:last-child {
  margin-bottom: 0;
}

.action-button.primary {
  color: #ffffff;
  background: linear-gradient(90deg, #2f7df6, #4f9bff);
  box-shadow: 0 10rpx 24rpx rgba(47, 125, 246, 0.25);
}

.action-button.finish {
  color: #ffffff;
  background: linear-gradient(90deg, #f56c6c, #ff8a8a);
  box-shadow: 0 10rpx 24rpx rgba(245, 108, 108, 0.25);
}

.action-button.outline {
  color: #ff9900;
  background-color: #fff8ed;
  border: 2rpx solid #ffd59a;
}

.action-button.outline-warning {
  color: #18b566;
  background-color: #eef9f3;
  border: 2rpx solid #a8e6c4;
}

.action-tip {
  margin-top: 20rpx;
  text-align: center;
  font-size: 22rpx;
  color: #909399;
}

.finished-tip {
  padding: 36rpx 0;
  text-align: center;
}

.finished-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #18b566;
}

.finished-desc {
  margin-top: 12rpx;
  font-size: 24rpx;
  color: #909399;
}
</style>
