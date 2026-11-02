<template>
  <view class="detail-page">
    <view v-if="loading" class="qt-empty">正在加载班次信息...</view>

    <view v-else-if="!detail" class="qt-empty">
      <uni-icons type="info" size="44" color="#b0b8c6" />
      <text class="qt-empty-title">班次信息加载失败</text>
      <text class="empty-tip">请返回列表重新选择</text>
    </view>

    <template v-else>
      <view class="hero">
        <view class="hero-top">
          <text class="route-name">{{ detail.routeName }}</text>
          <text class="status-pill" :class="statusClass(detail.status)">{{ statusText(detail.status) }}</text>
        </view>
        <view class="hero-time">
          <text class="time-big">{{ formatTime(detail.departTime) }}</text>
          <text class="time-date">{{ formatDate(detail.shiftDate) }}</text>
        </view>
        <view class="hero-stops">
          <text class="hero-stop">{{ detail.startStopName || '-' }}</text>
          <uni-icons type="right" size="14" color="rgba(255,255,255,0.8)" />
          <text class="hero-stop">{{ detail.endStopName || '-' }}</text>
        </view>
      </view>

      <view class="content">
        <view class="qt-card info-card">
          <view class="info-grid">
            <view class="info-item">
              <text class="info-label">车辆</text>
              <text class="info-value">{{ detail.vehiclePlate || '待分配' }}</text>
              <text class="info-sub">{{ detail.vehicleModel || '' }}</text>
            </view>
            <view class="info-item">
              <text class="info-label">司机</text>
              <text class="info-value">{{ detail.driverName || '待分配' }}</text>
              <text class="info-sub">负责本班次</text>
            </view>
            <view class="info-item">
              <text class="info-label">剩余座位</text>
              <text class="info-value" :class="remaining <= 0 ? 'text-danger' : 'text-primary'">
                {{ remaining }} / {{ detail.capacity || 0 }}
              </text>
              <text class="info-sub">已预约 {{ detail.reservedCount || 0 }} 人</text>
            </view>
            <view class="info-item">
              <text class="info-label">站点数量</text>
              <text class="info-value">{{ detail.stopCount || (detail.stops ? detail.stops.length : 0) }} 站</text>
              <text class="info-sub">按顺序停靠</text>
            </view>
          </view>
        </view>

        <view class="qt-card stops-card">
          <view class="card-head">
            <text class="card-title">途经站点</text>
            <text class="card-sub">按发车顺序停靠</text>
          </view>

          <view v-if="!detail.stops || !detail.stops.length" class="empty-inline">
            暂无站点信息
          </view>

          <view v-else class="timeline">
            <view
              v-for="(stop, index) in detail.stops"
              :key="stop.id"
              class="timeline-item"
            >
              <view class="timeline-axis">
                <view class="timeline-dot" :class="{ 'dot-highlight': index === 0 || index === detail.stops.length - 1 }">
                  {{ index + 1 }}
                </view>
                <view v-if="index !== detail.stops.length - 1" class="timeline-line"></view>
              </view>
              <view class="timeline-content">
                <text class="stop-name">{{ stop.name }}</text>
                <text v-if="index === 0" class="stop-flag">起点</text>
                <text v-else-if="index === detail.stops.length - 1" class="stop-flag">终点</text>
              </view>
            </view>
          </view>
        </view>

        <view class="tip-card">
          <uni-icons type="info" size="16" color="#1e5eff" />
          <text class="tip-text">预约成功后请提前到站候车，车辆发车后不可取消预约。</text>
        </view>
      </view>

      <view class="action-bar">
        <button
          v-if="detail.status === 'RUNNING'"
          class="action-btn ghost-btn"
          @click="goLocation"
        >
          查看车辆位置
        </button>
        <button
          v-if="detail.myReservationStatus === 'RESERVED'"
          class="action-btn qt-btn"
          :class="{ 'full-btn': detail.status !== 'RUNNING' }"
          :loading="signing"
          :disabled="signing"
          @click="sign"
        >
          签到
        </button>
        <button
          v-else-if="detail.myReservationStatus === 'SIGNED'"
          class="action-btn signed-btn"
          :class="{ 'full-btn': detail.status !== 'RUNNING' }"
          disabled
        >
          已签到
        </button>
        <button
          v-else
          class="action-btn qt-btn"
          :class="{ 'full-btn': detail.status !== 'RUNNING' }"
          :loading="reserving"
          :disabled="!canReserve"
          @click="reserve"
        >
          {{ reserveText }}
        </button>
      </view>
    </template>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getShiftDetail, createReservation, signReservation } from '@/api'

const STATUS_TEXT = {
  SCHEDULED: '待发车',
  RUNNING: '运行中',
  FINISHED: '已完成',
  CANCELLED: '已取消'
}

const id = ref('')
const detail = ref(null)
const loading = ref(true)
const reserving = ref(false)
const signing = ref(false)

const remaining = computed(() => {
  if (!detail.value) return 0
  if (detail.value.remainingSeats !== null && detail.value.remainingSeats !== undefined) {
    return Math.max(0, Number(detail.value.remainingSeats))
  }
  return Math.max(0, Number(detail.value.capacity || 0) - Number(detail.value.reservedCount || 0))
})

const canReserve = computed(() => {
  if (!detail.value || reserving.value) return false
  return (
    detail.value.status !== 'FINISHED' &&
    detail.value.status !== 'CANCELLED' &&
    remaining.value > 0
  )
})

const reserveText = computed(() => {
  if (!detail.value) return '预约班次'
  if (reserving.value) return '预约中...'
  if (detail.value.status === 'FINISHED') return '班次已结束'
  if (detail.value.status === 'CANCELLED') return '班次已取消'
  if (remaining.value <= 0) return '座位已满'
  if (detail.value.status === 'RUNNING') return '仍可预约'
  return '立即预约'
})

onLoad((options) => {
  id.value = options && options.id ? options.id : ''
  if (!id.value) {
    loading.value = false
    return
  }
  loadDetail()
})

async function loadDetail() {
  loading.value = true
  try {
    detail.value = await getShiftDetail(id.value)
  } catch (e) {
    detail.value = null
  } finally {
    loading.value = false
  }
}

async function reserve() {
  if (!canReserve.value) return
  reserving.value = true
  try {
    await createReservation(detail.value.id)
    uni.showToast({ title: '预约成功', icon: 'success' })
    setTimeout(() => {
      uni.switchTab({ url: '/pages/reservations/index' })
    }, 700)
  } catch (e) {
    // 错误提示已由请求层统一处理
  } finally {
    reserving.value = false
  }
}

async function sign() {
  if (signing.value || !detail.value || !detail.value.myReservationId) return
  signing.value = true
  try {
    await signReservation(detail.value.myReservationId)
    detail.value.myReservationStatus = 'SIGNED'
    uni.showToast({ title: '签到成功', icon: 'success' })
  } catch (e) {
    // 错误提示已由请求层统一处理
  } finally {
    signing.value = false
  }
}

function goLocation() {
  uni.navigateTo({ url: `/pages/location/location?id=${detail.value.id}` })
}

function statusText(status) {
  return STATUS_TEXT[status] || status || '未知'
}

function statusClass(status) {
  if (status === 'RUNNING') return 'pill-green'
  if (status === 'FINISHED') return 'pill-gray'
  if (status === 'CANCELLED') return 'pill-red'
  return 'pill-white'
}

function formatTime(time) {
  return time ? String(time).slice(0, 5) : '--:--'
}

function formatDate(date) {
  return date || ''
}
</script>

<style scoped>
.detail-page {
  min-height: 100vh;
  padding-bottom: 180rpx;
  background-color: #f5f7fb;
}

.hero {
  padding: 36rpx 32rpx 44rpx;
  background: linear-gradient(135deg, #1e5eff 0%, #4d82ff 100%);
  border-radius: 0 0 36rpx 36rpx;
  color: #ffffff;
}

.hero-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.route-name {
  font-size: 36rpx;
  font-weight: 700;
  color: #ffffff;
}

.status-pill {
  padding: 6rpx 18rpx;
  border-radius: 24rpx;
  font-size: 22rpx;
  font-weight: 600;
}

.pill-white {
  color: #ffffff;
  background-color: rgba(255, 255, 255, 0.22);
}

.pill-green {
  color: #ffffff;
  background-color: rgba(22, 163, 74, 0.85);
}

.pill-gray {
  color: #ffffff;
  background-color: rgba(255, 255, 255, 0.16);
}

.pill-red {
  color: #ffffff;
  background-color: rgba(239, 68, 68, 0.85);
}

.hero-time {
  display: flex;
  align-items: baseline;
  margin-top: 24rpx;
}

.time-big {
  font-size: 64rpx;
  font-weight: 700;
  line-height: 1;
  color: #ffffff;
}

.time-date {
  margin-left: 18rpx;
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.82);
}

.hero-stops {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-top: 24rpx;
  padding: 14rpx 18rpx;
  border-radius: 14rpx;
  background-color: rgba(255, 255, 255, 0.14);
}

.hero-stop {
  font-size: 26rpx;
  color: #ffffff;
}

.content {
  padding: 24rpx;
}

.info-card {
  padding: 28rpx 20rpx;
}

.info-grid {
  display: flex;
  flex-wrap: wrap;
}

.info-item {
  width: 50%;
  padding: 0 10rpx;
  margin-bottom: 24rpx;
  display: flex;
  flex-direction: column;
}

.info-item:last-child,
.info-item:nth-last-child(2) {
  margin-bottom: 0;
}

.info-label {
  font-size: 22rpx;
  color: #9aa3b2;
}

.info-value {
  margin-top: 8rpx;
  font-size: 30rpx;
  font-weight: 600;
  color: #1f2733;
}

.text-primary {
  color: #1e5eff;
}

.text-danger {
  color: #ef4444;
}

.info-sub {
  margin-top: 6rpx;
  font-size: 20rpx;
  color: #b0b8c6;
}

.stops-card {
  margin-top: 20rpx;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24rpx;
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
  line-height: 40rpx;
  border-radius: 50%;
  text-align: center;
  font-size: 22rpx;
  color: #5f6b80;
  background-color: #eef1f7;
}

.dot-highlight {
  color: #ffffff;
  background-color: #1e5eff;
}

.timeline-line {
  width: 2rpx;
  flex: 1;
  min-height: 54rpx;
  background-color: #e2e8f4;
}

.timeline-content {
  display: flex;
  align-items: center;
  flex: 1;
  padding-bottom: 30rpx;
}

.stop-name {
  font-size: 28rpx;
  color: #1f2733;
}

.stop-flag {
  margin-left: 12rpx;
  padding: 2rpx 12rpx;
  border-radius: 8rpx;
  font-size: 20rpx;
  color: #1e5eff;
  background-color: #eaf0ff;
}

.tip-card {
  display: flex;
  align-items: flex-start;
  gap: 10rpx;
  margin-top: 20rpx;
  padding: 20rpx 24rpx;
  border-radius: 14rpx;
  background-color: #eaf0ff;
}

.tip-text {
  flex: 1;
  font-size: 23rpx;
  line-height: 1.6;
  color: #4a5a7a;
}

.action-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  gap: 20rpx;
  padding: 20rpx 24rpx calc(20rpx + env(safe-area-inset-bottom));
  background-color: #ffffff;
  box-shadow: 0 -6rpx 24rpx rgba(31, 39, 51, 0.08);
}

.action-btn {
  flex: 1;
  width: auto;
  height: 88rpx;
  line-height: 88rpx;
  margin: 0;
  border-radius: 44rpx;
  font-size: 30rpx;
  font-weight: 600;
  border: none;
  text-align: center;
}

.ghost-btn {
  flex: 0 0 34%;
  color: #1e5eff;
  background-color: #eaf0ff;
}

.full-btn {
  flex: 1;
}

.signed-btn {
  color: #16a34a;
  background-color: #e8f8ef;
}

.empty-tip {
  display: block;
  margin-top: 10rpx;
  font-size: 24rpx;
  color: #b0b8c6;
}
</style>
