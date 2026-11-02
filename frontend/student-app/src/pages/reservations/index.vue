<template>
  <view class="res-page">
    <view class="hero">
      <view class="hero-text">
        <text class="hero-title">我的预约</text>
        <text class="hero-sub">共 {{ reservations.length }} 条记录 · 有效 {{ activeCount }} 条</text>
      </view>
      <view class="hero-badge">{{ activeCount }}</view>
    </view>

    <view class="content">
      <view v-if="loading" class="qt-empty">正在加载预约记录...</view>

      <view v-else-if="!reservations.length" class="qt-empty">
        <uni-icons type="calendar" size="48" color="#b9c4d8" />
        <text class="qt-empty-title">还没有预约记录</text>
        <text class="empty-tip">去班次列表选择一条线路出发吧</text>
        <button class="qt-btn-light go-btn" @click="goShifts">去查看班次</button>
      </view>

      <view v-else class="res-list">
        <view
          v-for="item in reservations"
          :key="item.id"
          class="qt-card res-card"
          :class="{ cancelled: isCancelled(item) }"
          @click="openShift(item)"
        >
          <view class="res-head">
            <text class="route-name">{{ item.routeName || '线路信息缺失' }}</text>
            <text class="qt-tag" :class="statusTag(item).cls">{{ statusTag(item).text }}</text>
          </view>

          <view class="res-time">
            <text class="res-date">{{ item.shiftDate || '-' }}</text>
            <text class="res-depart">{{ formatTime(item.departTime) }}</text>
            <text class="res-depart-label">发车</text>
          </view>

          <view class="res-meta">
            <text class="meta-text">车辆 {{ item.vehiclePlate || '待分配' }}</text>
            <text class="meta-text">剩余 {{ remainingSeats(item) }} / {{ item.capacity || 0 }} 座</text>
          </view>

          <view class="res-footer">
            <view class="footer-left">
              <text class="create-time">预约于 {{ formatDateTime(item.createTime) }}</text>
              <text v-if="isCancelled(item)" class="cancel-time">取消于 {{ formatDateTime(item.cancelTime) }}</text>
            </view>
            <view class="footer-actions" @click.stop>
              <button
                v-if="canCancel(item)"
                class="mini-btn cancel-btn"
                :loading="cancellingId === item.id"
                :disabled="cancellingId === item.id"
                @click.stop="askCancel(item)"
              >
                取消预约
              </button>
              <button
                v-if="canLocate(item)"
                class="mini-btn locate-btn"
                @click.stop="openLocation(item)"
              >
                查看位置
              </button>
            </view>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onShow, onPullDownRefresh } from '@dcloudio/uni-app'
import { getReservations, cancelReservation } from '@/api'
import { getToken } from '@/api/request'

const reservations = ref([])
const loading = ref(true)
const cancellingId = ref(null)

const activeCount = computed(
  () => reservations.value.filter((item) => item.status === 'RESERVED').length
)

onShow(() => {
  if (!getToken()) {
    uni.reLaunch({ url: '/pages/login/login' })
    return
  }
  loadReservations()
})

onPullDownRefresh(() => {
  loadReservations().finally(() => {
    uni.stopPullDownRefresh()
  })
})

async function loadReservations() {
  loading.value = true
  try {
    reservations.value = (await getReservations()) || []
  } catch (e) {
    reservations.value = []
  } finally {
    loading.value = false
  }
}

function isCancelled(item) {
  return item.status === 'CANCELLED'
}

function canCancel(item) {
  return item.status === 'RESERVED' && item.shiftStatus === 'SCHEDULED'
}

function canLocate(item) {
  return item.status === 'RESERVED' && item.shiftStatus === 'RUNNING'
}

function statusTag(item) {
  if (isCancelled(item)) return { text: '已取消', cls: 'qt-tag-gray' }
  if (item.shiftStatus === 'RUNNING') return { text: '运行中', cls: 'qt-tag-blue' }
  if (item.shiftStatus === 'FINISHED') return { text: '已完成', cls: 'qt-tag-gray' }
  if (item.shiftStatus === 'CANCELLED') return { text: '班次取消', cls: 'qt-tag-red' }
  return { text: '已预约', cls: 'qt-tag-green' }
}

function remainingSeats(item) {
  if (item.remainingSeats !== null && item.remainingSeats !== undefined) {
    return Math.max(0, Number(item.remainingSeats))
  }
  return Math.max(0, Number(item.capacity || 0) - Number(item.reservedCount || 0))
}

function askCancel(item) {
  uni.showModal({
    title: '取消预约',
    content: '确定取消该班次的预约吗？取消后座位将被释放。',
    confirmText: '确认取消',
    confirmColor: '#ef4444',
    success: (res) => {
      if (res.confirm) {
        doCancel(item)
      }
    }
  })
}

async function doCancel(item) {
  if (cancellingId.value) return
  cancellingId.value = item.id
  try {
    await cancelReservation(item.id)
    uni.showToast({ title: '已取消预约', icon: 'success' })
    await loadReservations()
  } catch (e) {
    // 错误提示已由请求层统一处理
  } finally {
    cancellingId.value = null
  }
}

function openShift(item) {
  if (!item.shiftId) return
  uni.navigateTo({ url: `/pages/shift-detail/shift-detail?id=${item.shiftId}` })
}

function openLocation(item) {
  uni.navigateTo({ url: `/pages/location/location?id=${item.shiftId}` })
}

function goShifts() {
  uni.switchTab({ url: '/pages/index/index' })
}

function formatTime(time) {
  return time ? String(time).slice(0, 5) : '--:--'
}

function formatDateTime(value) {
  if (!value) return '-'
  return String(value).slice(0, 16)
}
</script>

<style scoped>
.res-page {
  min-height: 100vh;
  background-color: #f5f7fb;
}

.hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 36rpx 32rpx 48rpx;
  background: linear-gradient(135deg, #1e5eff 0%, #4d82ff 100%);
  border-radius: 0 0 36rpx 36rpx;
}

.hero-text {
  display: flex;
  flex-direction: column;
}

.hero-title {
  font-size: 38rpx;
  font-weight: 700;
  color: #ffffff;
}

.hero-sub {
  margin-top: 10rpx;
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.78);
}

.hero-badge {
  width: 84rpx;
  height: 84rpx;
  line-height: 84rpx;
  border-radius: 50%;
  text-align: center;
  font-size: 34rpx;
  font-weight: 700;
  color: #1e5eff;
  background-color: #ffffff;
}

.content {
  padding: 24rpx;
}

.empty-tip {
  display: block;
  margin-top: 10rpx;
  font-size: 24rpx;
  color: #b0b8c6;
}

.go-btn {
  width: 280rpx;
  margin: 40rpx auto 0;
}

.res-list {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

.res-card {
  padding: 26rpx;
}

.res-card.cancelled {
  background-color: #f7f8fa;
  box-shadow: none;
}

.res-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.route-name {
  flex: 1;
  min-width: 0;
  font-size: 30rpx;
  font-weight: 700;
  color: #1f2733;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cancelled .route-name {
  color: #9aa3b2;
}

.res-time {
  display: flex;
  align-items: baseline;
  margin-top: 18rpx;
}

.res-date {
  font-size: 26rpx;
  color: #5f6b80;
}

.res-depart {
  margin-left: 20rpx;
  font-size: 40rpx;
  font-weight: 700;
  color: #1e5eff;
}

.cancelled .res-depart {
  color: #9aa3b2;
}

.res-depart-label {
  margin-left: 8rpx;
  font-size: 20rpx;
  color: #9aa3b2;
}

.res-meta {
  display: flex;
  align-items: center;
  gap: 24rpx;
  margin-top: 16rpx;
  padding: 16rpx 18rpx;
  border-radius: 12rpx;
  background-color: #f7f9fe;
}

.cancelled .res-meta {
  background-color: #f0f2f6;
}

.meta-text {
  font-size: 23rpx;
  color: #8a93a6;
}

.res-footer {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-top: 20rpx;
  padding-top: 18rpx;
  border-top: 2rpx solid #f0f3f9;
}

.cancelled .res-footer {
  border-top-color: #e8ebf0;
}

.footer-left {
  display: flex;
  flex-direction: column;
}

.create-time {
  font-size: 21rpx;
  color: #b0b8c6;
}

.cancel-time {
  margin-top: 6rpx;
  font-size: 21rpx;
  color: #9aa3b2;
}

.footer-actions {
  display: flex;
  gap: 16rpx;
}

.mini-btn {
  height: 64rpx;
  line-height: 62rpx;
  padding: 0 26rpx;
  border-radius: 32rpx;
  font-size: 24rpx;
  font-weight: 500;
  border: none;
  margin: 0;
}

.cancel-btn {
  color: #ef4444;
  background-color: #fdecec;
}

.locate-btn {
  color: #1e5eff;
  background-color: #eaf0ff;
}
</style>
