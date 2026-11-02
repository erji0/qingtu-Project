<template>
  <view class="location-page">
    <view v-if="loading" class="qt-empty">正在获取车辆位置...</view>

    <view v-else-if="!location" class="empty-card">
      <uni-icons type="location" size="52" color="#b9c4d8" />
      <text class="empty-title">车辆尚未开始运行</text>
      <text class="empty-desc">班次开始后，这里会每 10 秒自动刷新车辆位置</text>
      <button class="qt-btn-light refresh-btn" @click="loadLocation(false)">重新获取</button>
    </view>

    <template v-else>
      <view class="status-hero">
        <view class="hero-head">
          <view class="hero-route">
            <text class="route-name">{{ location.routeName }}</text>
            <text class="plate-no">{{ location.plateNo || '车辆未分配' }}</text>
          </view>
          <text class="run-pill" :class="runStatusClass(location.runStatus)">
            {{ runStatusText(location.runStatus) }}
          </text>
        </view>

        <view class="stop-panels">
          <view class="stop-panel">
            <text class="panel-label">当前站</text>
            <text class="panel-name">{{ location.currentStopName || '-' }}</text>
          </view>
          <uni-icons type="right" size="18" color="rgba(255,255,255,0.85)" />
          <view class="stop-panel">
            <text class="panel-label">下一站</text>
            <text class="panel-name">{{ location.nextStopName || '-' }}</text>
          </view>
        </view>

        <view class="eta-row">
          <view class="eta-item">
            <text class="eta-value">{{ location.etaNextStopMinutes || 0 }}</text>
            <text class="eta-unit">分钟</text>
            <text class="eta-label">到下一站</text>
          </view>
          <view class="eta-divider"></view>
          <view class="eta-item">
            <text class="eta-value">{{ location.etaArriveMinutes || 0 }}</text>
            <text class="eta-unit">分钟</text>
            <text class="eta-label">到终点</text>
          </view>
        </view>
      </view>

      <view class="content">
        <view class="qt-card progress-card">
          <view class="progress-head">
            <text class="progress-title">行程进度</text>
            <text class="progress-value">{{ formatPercent(location.progressPercent) }}%</text>
          </view>
          <view class="progress-bar">
            <view class="progress-fill" :style="{ width: progressWidth }"></view>
          </view>
          <view class="progress-foot">
            <text class="foot-stop">{{ location.currentStopName || '起点' }}</text>
            <text class="foot-stop">{{ location.nextStopName || '终点' }}</text>
          </view>
          <view class="update-row">
            <uni-icons type="refresh" size="14" color="#9aa3b2" />
            <text class="update-text">每 10 秒自动刷新 · 更新于 {{ formatUpdateTime(location.updateTime) }}</text>
          </view>
        </view>

        <view class="qt-card stops-card">
          <view class="card-head">
            <text class="card-title">站点进度</text>
            <text class="card-sub">{{ location.stops ? location.stops.length : 0 }} 个站点</text>
          </view>

          <view v-if="!location.stops || !location.stops.length" class="empty-inline">
            暂无站点信息
          </view>

          <view v-else class="timeline">
            <view v-for="(stop, index) in location.stops" :key="stop.id" class="timeline-item">
              <view class="timeline-axis">
                <view class="timeline-dot" :class="stateClass(stop.state)">
                  <text class="dot-index">{{ index + 1 }}</text>
                </view>
                <view
                  v-if="index !== location.stops.length - 1"
                  class="timeline-line"
                  :class="{ 'line-passed': stop.state === 'PASSED' }"
                ></view>
              </view>
              <view class="timeline-content">
                <text class="stop-name" :class="stateTextClass(stop.state)">{{ stop.name }}</text>
                <text class="state-tag" :class="stateClass(stop.state)">{{ stateText(stop.state) }}</text>
              </view>
            </view>
          </view>
        </view>

        <button class="qt-btn-light manual-refresh" @click="loadLocation(false)">刷新位置</button>
      </view>
    </template>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad, onUnload } from '@dcloudio/uni-app'
import { getShiftLocation } from '@/api'

const RUN_STATUS_TEXT = {
  RUNNING: '正常运行',
  DELAYED: '延误',
  ARRIVED: '已到达'
}

const STOP_STATE_TEXT = {
  PASSED: '已经过',
  CURRENT: '当前站',
  NEXT: '下一站',
  WAITING: '待到达'
}

const id = ref('')
const location = ref(null)
const loading = ref(true)
let timer = null
let fetching = false

const progressWidth = computed(() => {
  const percent = Number(location.value && location.value.progressPercent)
  if (!Number.isFinite(percent)) return '0%'
  return `${Math.max(0, Math.min(100, percent))}%`
})

onLoad((options) => {
  id.value = options && options.id ? options.id : ''
  if (!id.value) {
    loading.value = false
    return
  }
  loadLocation(false)
  timer = setInterval(() => {
    loadLocation(true)
  }, 10000)
})

onUnload(() => {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
})

async function loadLocation(silent) {
  if (!id.value || fetching) return
  fetching = true
  try {
    const data = await getShiftLocation(id.value, silent)
    location.value = data || null
  } catch (e) {
    if (!location.value) {
      location.value = null
    }
  } finally {
    fetching = false
    loading.value = false
  }
}

function runStatusText(status) {
  return RUN_STATUS_TEXT[status] || status || '未知状态'
}

function runStatusClass(status) {
  if (status === 'DELAYED') return 'run-delayed'
  if (status === 'ARRIVED') return 'run-arrived'
  return 'run-running'
}

function stateClass(state) {
  if (state === 'PASSED') return 'state-passed'
  if (state === 'CURRENT') return 'state-current'
  if (state === 'NEXT') return 'state-next'
  return 'state-waiting'
}

function stateTextClass(state) {
  if (state === 'CURRENT') return 'name-current'
  if (state === 'NEXT') return 'name-next'
  if (state === 'PASSED') return 'name-passed'
  return 'name-waiting'
}

function stateText(state) {
  return STOP_STATE_TEXT[state] || '待到达'
}

function formatPercent(value) {
  const num = Number(value)
  return Number.isFinite(num) ? Math.round(num) : 0
}

function formatUpdateTime(value) {
  if (!value) return '--:--:--'
  const text = String(value)
  return text.length >= 19 ? text.slice(11, 19) : text
}
</script>

<style scoped>
.location-page {
  min-height: 100vh;
  background-color: #f5f7fb;
}

.empty-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin: 40rpx 24rpx;
  padding: 80rpx 40rpx;
  background-color: #ffffff;
  border-radius: 20rpx;
  box-shadow: 0 8rpx 28rpx rgba(139, 163, 199, 0.06);
}

.empty-title {
  margin-top: 24rpx;
  font-size: 32rpx;
  font-weight: 600;
  color: #1f2733;
}

.empty-desc {
  margin-top: 14rpx;
  text-align: center;
  font-size: 24rpx;
  line-height: 1.6;
  color: #9aa3b2;
}

.refresh-btn {
  width: 260rpx;
  margin-top: 36rpx;
}

.status-hero {
  padding: 32rpx 28rpx 40rpx;
  background: linear-gradient(135deg, #8BA3C7 0%, #A9BCD6 100%);
  border-radius: 0 0 36rpx 36rpx;
  color: #ffffff;
}

.hero-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}

.hero-route {
  display: flex;
  flex-direction: column;
}

.route-name {
  font-size: 34rpx;
  font-weight: 700;
  color: #ffffff;
}

.plate-no {
  margin-top: 8rpx;
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.8);
}

.run-pill {
  padding: 6rpx 18rpx;
  border-radius: 24rpx;
  font-size: 22rpx;
  font-weight: 600;
}

.run-running {
  color: #ffffff;
  background-color: rgba(22, 163, 74, 0.85);
}

.run-delayed {
  color: #ffffff;
  background-color: rgba(217, 119, 6, 0.9);
}

.run-arrived {
  color: #ffffff;
  background-color: rgba(255, 255, 255, 0.24);
}

.stop-panels {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-top: 28rpx;
  padding: 22rpx 20rpx;
  border-radius: 16rpx;
  background-color: rgba(255, 255, 255, 0.14);
}

.stop-panel {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.panel-label {
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.72);
}

.panel-name {
  margin-top: 8rpx;
  font-size: 30rpx;
  font-weight: 600;
  color: #ffffff;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.eta-row {
  display: flex;
  align-items: center;
  margin-top: 24rpx;
}

.eta-item {
  flex: 1;
  display: flex;
  align-items: baseline;
  justify-content: center;
}

.eta-value {
  font-size: 48rpx;
  font-weight: 700;
  color: #ffffff;
}

.eta-unit {
  margin-left: 6rpx;
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.8);
}

.eta-label {
  margin-left: 14rpx;
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.72);
}

.eta-divider {
  width: 2rpx;
  height: 56rpx;
  background-color: rgba(255, 255, 255, 0.24);
}

.content {
  padding: 24rpx;
}

.progress-card {
  padding: 28rpx 26rpx;
}

.progress-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.progress-title {
  font-size: 30rpx;
  font-weight: 700;
  color: #1f2733;
}

.progress-value {
  font-size: 34rpx;
  font-weight: 700;
  color: #8BA3C7;
}

.progress-bar {
  height: 16rpx;
  margin-top: 20rpx;
  border-radius: 8rpx;
  background-color: #eef1f7;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  border-radius: 8rpx;
  background: linear-gradient(90deg, #8BA3C7, #A9BCD6);
  transition: width 0.4s ease;
}

.progress-foot {
  display: flex;
  justify-content: space-between;
  margin-top: 12rpx;
}

.foot-stop {
  max-width: 46%;
  font-size: 22rpx;
  color: #8a93a6;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.update-row {
  display: flex;
  align-items: center;
  gap: 8rpx;
  margin-top: 20rpx;
  padding-top: 18rpx;
  border-top: 2rpx solid #f0f3f9;
}

.update-text {
  font-size: 22rpx;
  color: #9aa3b2;
}

.stops-card {
  margin-top: 20rpx;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 26rpx;
}

.card-title {
  font-size: 32rpx;
  font-weight: 700;
  color: #1f2733;
}

.card-sub {
  font-size: 22rpx;
  color: #9aa3b2;
}

.empty-inline {
  padding: 30rpx 0;
  text-align: center;
  font-size: 24rpx;
  color: #9aa3b2;
}

.timeline-item {
  display: flex;
}

.timeline-axis {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 56rpx;
  flex-shrink: 0;
}

.timeline-dot {
  width: 40rpx;
  height: 40rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.dot-index {
  font-size: 20rpx;
  font-weight: 600;
}

.state-passed {
  color: #ffffff;
  background-color: #9aa3b2;
}

.timeline-dot.state-current {
  color: #ffffff;
  background-color: #8BA3C7;
  box-shadow: 0 0 0 6rpx #dce6ff;
}

.timeline-dot.state-next {
  color: #ffffff;
  background-color: #f59e0b;
}

.timeline-dot.state-waiting {
  color: #8a93a6;
  background-color: #eef1f7;
}

.timeline-line {
  width: 2rpx;
  flex: 1;
  min-height: 54rpx;
  background-color: #e2e8f4;
}

.line-passed {
  background-color: #c7d2e4;
}

.timeline-content {
  display: flex;
  align-items: center;
  flex: 1;
  min-width: 0;
  padding-bottom: 30rpx;
}

.stop-name {
  font-size: 28rpx;
  color: #1f2733;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.name-current {
  color: #8BA3C7;
  font-weight: 600;
}

.name-next {
  color: #d97706;
  font-weight: 600;
}

.name-passed {
  color: #9aa3b2;
}

.name-waiting {
  color: #5f6b80;
}

.state-tag {
  margin-left: auto;
  flex-shrink: 0;
  padding: 4rpx 14rpx;
  border-radius: 16rpx;
  font-size: 20rpx;
}

.state-tag.state-passed {
  color: #6b7280;
  background-color: #f0f2f6;
}

.state-tag.state-current {
  color: #8BA3C7;
  background-color: #EDF1F7;
}

.state-tag.state-next {
  color: #d97706;
  background-color: #fff4e5;
}

.state-tag.state-waiting {
  color: #8a93a6;
  background-color: #f3f5f9;
}

.manual-refresh {
  width: 100%;
  margin-top: 24rpx;
}
</style>
