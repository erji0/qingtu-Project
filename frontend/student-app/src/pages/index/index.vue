<template>
  <view class="home-page">
    <view class="hero">
      <view class="hero-row">
        <view class="hero-left">
          <text class="hello">你好，{{ userName || '同学' }}</text>
          <text class="hero-sub">选好线路，准时出发</text>
        </view>
        <picker mode="date" :value="date" @change="onDateChange">
          <view class="date-pill">
            <uni-icons type="calendar" size="15" color="#ffffff" />
            <text class="date-text">{{ prettyDate }}</text>
          </view>
        </picker>
      </view>
    </view>

    <view class="content">
      <scroll-view class="route-scroll" scroll-x :show-scrollbar="false">
        <view class="route-chips">
          <view
            class="chip"
            :class="{ active: routeId === null }"
            @click="selectRoute(null)"
          >
            全部线路
          </view>
          <view
            v-for="route in routes"
            :key="route.id"
            class="chip"
            :class="{ active: routeId === route.id }"
            @click="selectRoute(route.id)"
          >
            {{ route.name }}
          </view>
        </view>
      </scroll-view>

      <view class="section-head">
        <text class="section-title">班次列表</text>
        <text class="section-count">{{ loading ? '加载中' : shifts.length + ' 个班次' }}</text>
      </view>

      <view v-if="loading" class="qt-empty">正在加载班次...</view>

      <view v-else-if="!shifts.length" class="qt-empty">
        <uni-icons type="info" size="44" color="#b0b8c6" />
        <text class="qt-empty-title">当天暂无班次</text>
        <text class="empty-tip">可以切换日期或线路后再看看</text>
      </view>

      <view v-else class="shift-list">
        <view
          v-for="shift in sortedShifts"
          :key="shift.id"
          class="qt-card shift-card"
          @click="openDetail(shift.id)"
        >
          <view class="shift-head">
            <view class="route-info">
              <text class="route-name">{{ shift.routeName }}</text>
              <text class="qt-tag" :class="statusClass(shift.status)">{{ statusText(shift.status) }}</text>
            </view>
            <view class="time-box">
              <text class="depart-time">{{ formatTime(shift.departTime) }}</text>
              <text class="time-label">发车</text>
            </view>
          </view>

          <view class="stops-line">
            <text class="stop-name">{{ shift.startStopName || '-' }}</text>
            <uni-icons type="right" size="14" color="#9aa3b2" />
            <text class="stop-name">{{ shift.endStopName || '-' }}</text>
            <text class="stop-count">{{ shift.stopCount || 0 }} 站</text>
          </view>

          <view class="meta-row">
            <text class="meta">{{ shift.vehiclePlate || '车辆待分配' }}</text>
            <text class="dot">·</text>
            <text class="meta">{{ shift.driverName || '司机待分配' }}</text>
            <text
              class="seat-tag"
              :class="remainingSeats(shift) <= 0 ? 'seat-full' : 'seat-ok'"
            >
              {{ remainingSeats(shift) <= 0 ? '已满座' : '余 ' + remainingSeats(shift) + ' 座' }}
            </text>
          </view>

          <view class="seat-bar">
            <view class="seat-fill" :style="{ width: seatPercent(shift) + '%' }"></view>
          </view>
          <view class="seat-caption">
            <text>已预约 {{ shift.reservedCount || 0 }} / {{ shift.capacity || 0 }} 座</text>
            <text class="detail-link">查看详情</text>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad, onShow, onPullDownRefresh } from '@dcloudio/uni-app'
import { getRoutes, getShifts } from '@/api'
import { getUser } from '@/api/request'

const SHIFT_STATUS_TEXT = {
  SCHEDULED: '待发车',
  RUNNING: '运行中',
  FINISHED: '已完成',
  CANCELLED: '已取消'
}

const routes = ref([])
const shifts = ref([])
const routeId = ref(null)
const date = ref(todayString())
const loading = ref(false)
const userName = ref('')

let shownOnce = false

const prettyDate = computed(() => {
  const d = parseDate(date.value)
  const week = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][d.getDay()]
  const label = `${d.getMonth() + 1}月${d.getDate()}日 ${week}`
  return date.value === todayString() ? `今天 ${label}` : label
})

// 待发车班次置顶，同组内按发车时间升序
const sortedShifts = computed(() => {
  return shifts.value.slice().sort((a, b) => {
    const aPending = a.status === 'SCHEDULED' ? 0 : 1
    const bPending = b.status === 'SCHEDULED' ? 0 : 1
    if (aPending !== bPending) return aPending - bPending
    return String(a.departTime || '').localeCompare(String(b.departTime || ''))
  })
})

onLoad(() => {
  const user = getUser()
  userName.value = user && user.name ? user.name : ''
  loadRoutes()
  loadShifts()
})

onShow(() => {
  if (shownOnce) {
    loadShifts()
  }
  shownOnce = true
})

onPullDownRefresh(() => {
  loadShifts().finally(() => {
    uni.stopPullDownRefresh()
  })
})

async function loadRoutes() {
  try {
    const data = await getRoutes()
    routes.value = (data || []).filter((route) => route.status !== 0)
    if (routeId.value !== null && !routes.value.some((r) => r.id === routeId.value)) {
      routeId.value = null
    }
  } catch (e) {
    routes.value = []
  }
}

async function loadShifts() {
  loading.value = true
  try {
    const data = await getShifts({
      date: date.value,
      routeId: routeId.value === null ? undefined : routeId.value
    })
    shifts.value = data || []
  } catch (e) {
    shifts.value = []
  } finally {
    loading.value = false
  }
}

function selectRoute(id) {
  if (routeId.value === id) return
  routeId.value = id
  loadShifts()
}

function onDateChange(e) {
  date.value = e.detail.value
  loadShifts()
}

function openDetail(id) {
  uni.navigateTo({ url: `/pages/shift-detail/shift-detail?id=${id}` })
}

function statusText(status) {
  return SHIFT_STATUS_TEXT[status] || status || '未知'
}

function statusClass(status) {
  if (status === 'RUNNING') return 'qt-tag-green'
  if (status === 'FINISHED') return 'qt-tag-gray'
  if (status === 'CANCELLED') return 'qt-tag-red'
  return 'qt-tag-blue'
}

function remainingSeats(shift) {
  if (shift.remainingSeats !== null && shift.remainingSeats !== undefined) {
    return Math.max(0, Number(shift.remainingSeats))
  }
  return Math.max(0, Number(shift.capacity || 0) - Number(shift.reservedCount || 0))
}

function seatPercent(shift) {
  const capacity = Number(shift.capacity || 0)
  if (!capacity) return 0
  const reserved = Number(shift.reservedCount || 0)
  return Math.min(100, Math.round((reserved / capacity) * 100))
}

function pad(n) {
  return n < 10 ? `0${n}` : `${n}`
}

function todayString() {
  const d = new Date()
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

function parseDate(value) {
  const [y, m, d] = value.split('-').map(Number)
  return new Date(y, m - 1, d)
}

function formatTime(time) {
  return time ? String(time).slice(0, 5) : '--:--'
}
</script>

<style scoped>
.home-page {
  min-height: 100vh;
  background-color: #f5f7fb;
}

.hero {
  padding: 36rpx 32rpx 56rpx;
  background: linear-gradient(135deg, #8BA3C7 0%, #A9BCD6 100%);
  border-radius: 0 0 36rpx 36rpx;
}

.hero-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}

.hero-left {
  display: flex;
  flex-direction: column;
}

.hello {
  font-size: 38rpx;
  font-weight: 700;
  color: #ffffff;
}

.hero-sub {
  margin-top: 10rpx;
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.78);
}

.date-pill {
  display: flex;
  align-items: center;
  gap: 8rpx;
  padding: 12rpx 20rpx;
  border-radius: 28rpx;
  background-color: rgba(255, 255, 255, 0.18);
  border: 2rpx solid rgba(255, 255, 255, 0.3);
}

.date-text {
  font-size: 24rpx;
  color: #ffffff;
}

.content {
  padding: 24rpx 24rpx 40rpx;
}

.route-scroll {
  width: 100%;
  white-space: nowrap;
}

.route-chips {
  display: inline-flex;
  padding: 4rpx 0;
}

.chip {
  display: inline-block;
  margin-right: 16rpx;
  padding: 14rpx 26rpx;
  border-radius: 32rpx;
  background-color: #ffffff;
  color: #5f6b80;
  font-size: 26rpx;
  border: 2rpx solid #e8ecf3;
}

.chip.active {
  color: #ffffff;
  background-color: #8BA3C7;
  border-color: #8BA3C7;
  font-weight: 600;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 32rpx 4rpx 20rpx;
}

.section-title {
  font-size: 32rpx;
  font-weight: 700;
  color: #1f2733;
}

.section-count {
  font-size: 24rpx;
  color: #9aa3b2;
}

.empty-tip {
  display: block;
  margin-top: 10rpx;
  font-size: 24rpx;
  color: #b0b8c6;
}

.shift-list {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

.shift-card {
  padding: 28rpx 26rpx;
}

.shift-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}

.route-info {
  display: flex;
  align-items: center;
  gap: 12rpx;
  flex: 1;
  min-width: 0;
}

.route-name {
  font-size: 30rpx;
  font-weight: 700;
  color: #1f2733;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.time-box {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  margin-left: 20rpx;
}

.depart-time {
  font-size: 40rpx;
  font-weight: 700;
  color: #8BA3C7;
  line-height: 1.1;
}

.time-label {
  margin-top: 4rpx;
  font-size: 20rpx;
  color: #9aa3b2;
}

.stops-line {
  display: flex;
  align-items: center;
  gap: 10rpx;
  margin-top: 20rpx;
  padding: 16rpx 18rpx;
  background-color: #f7f9fe;
  border-radius: 12rpx;
}

.stop-name {
  font-size: 26rpx;
  color: #3d4657;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.stop-count {
  margin-left: auto;
  flex-shrink: 0;
  font-size: 22rpx;
  color: #9aa3b2;
}

.meta-row {
  display: flex;
  align-items: center;
  margin-top: 18rpx;
  font-size: 24rpx;
  color: #8a93a6;
}

.dot {
  margin: 0 8rpx;
  color: #cbd2df;
}

.seat-tag {
  margin-left: auto;
  padding: 4rpx 14rpx;
  border-radius: 20rpx;
  font-size: 22rpx;
  font-weight: 600;
}

.seat-ok {
  color: #16a34a;
  background-color: #e8f8ef;
}

.seat-full {
  color: #dc2626;
  background-color: #fdecec;
}

.seat-bar {
  height: 8rpx;
  margin-top: 18rpx;
  background-color: #eef1f7;
  border-radius: 4rpx;
  overflow: hidden;
}

.seat-fill {
  height: 100%;
  background: linear-gradient(90deg, #8BA3C7, #A9BCD6);
  border-radius: 4rpx;
}

.seat-caption {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 10rpx;
  font-size: 22rpx;
  color: #9aa3b2;
}

.detail-link {
  color: #8BA3C7;
}
</style>
