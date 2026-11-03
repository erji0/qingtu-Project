<template>
  <view class="live-page">
    <!-- 线路列表 -->
    <template v-if="!selectedRoute">
      <view v-if="loading" class="qt-empty">正在加载线路...</view>

      <template v-else-if="routes.length > 0">
        <view class="page-header">
          <text class="header-title">🚌 实时公交</text>
          <text class="header-sub">查询校车到你最近的站点还要多久</text>
        </view>

        <view class="route-list">
          <view
            v-for="route in routes"
            :key="route.id"
            class="route-card"
            @tap="openRoute(route)"
          >
            <view class="route-info">
              <text class="route-name">{{ route.name }}</text>
              <view class="active-badge" v-if="route.activeCount > 0">
                <view class="dot"></view>
                <text>{{ route.activeCount }} 辆运行中</text>
              </view>
              <view class="active-badge gray" v-else>
                <view class="dot gray"></view>
                <text>暂无运行车辆</text>
              </view>
            </view>
            <uni-icons type="right" size="18" color="#8BA3C7" />
          </view>
        </view>

        <view class="refresh-hint">
          <uni-icons type="reload" size="14" color="#8a93a6" />
          <text>下拉刷新数据</text>
        </view>
      </template>

      <view v-else class="qt-empty">暂无线路信息</view>
    </template>

    <!-- 站点详情 -->
    <template v-else>
      <view class="detail-header">
        <view class="back-btn" @tap="backToList">
          <uni-icons type="left" size="20" color="#8BA3C7" />
        </view>
        <view class="detail-title">
          <text class="title">{{ selectedRoute.name }}</text>
          <view class="active-badge mini" v-if="selectedRoute.activeCount > 0">
            <view class="dot"></view>
            <text>{{ selectedRoute.activeCount }} 辆在线</text>
          </view>
        </view>
      </view>

      <view v-if="detailLoading" class="qt-empty">刷新中...</view>

      <template v-else>
        <view class="stops-container">
          <view
            v-for="(stop, index) in stops"
            :key="stop.id"
            class="stop-item"
          >
            <!-- 站点标识 -->
            <view class="stop-marker">
              <view class="line-top" v-if="index > 0"></view>
              <view class="stop-dot" :class="{ start: index === 0, end: index === stops.length - 1 }">
                <text v-if="index === 0">起</text>
                <text v-else-if="index === stops.length - 1">终</text>
                <text v-else>{{ index + 1 }}</text>
              </view>
              <view class="line-bottom" v-if="index < stops.length - 1"></view>
            </view>

            <!-- 站点信息 -->
            <view class="stop-content">
              <text class="stop-name">{{ stop.name }}</text>

              <!-- 附近车辆 -->
              <view class="vehicles" v-if="stop.vehicles && stop.vehicles.length > 0">
                <view
                  v-for="(v, vIdx) in stop.vehicles.slice(0, 3)"
                  :key="vIdx"
                  class="vehicle-item"
                  :class="{ arrived: v.etaMinutes === 0 }"
                >
                  <text class="plate">{{ v.plateNo }}</text>
                  <text class="eta">
                    <template v-if="v.etaMinutes === 0">已到达</template>
                    <template v-else-if="v.etaMinutes === 1">即将到站</template>
                    <template v-else>约 {{ v.etaMinutes }} 分钟</template>
                  </text>
                  <text class="from" v-if="v.currentStopName">（{{ v.currentStopName }} 发车）</text>
                </view>
                <view class="more" v-if="stop.vehicles.length > 3">
                  还有 {{ stop.vehicles.length - 3 }} 辆...
                </view>
              </view>

              <view class="no-vehicle" v-else>
                暂无车辆
              </view>
            </view>
          </view>
        </view>

        <view class="refresh-footer">
          <uni-icons type="reload" size="14" color="#8BA3C7" />
          <text>每 10 秒自动刷新</text>
        </view>
      </template>
    </template>
  </view>
</template>

<script>
import { getLiveRoutes, getLiveRouteDetail } from '@/api/index.js'

export default {
  data() {
    return {
      loading: false,
      routes: [],
      selectedRoute: null,
      stops: [],
      detailLoading: false,
      timer: null
    }
  },
  onLoad() {
    this.fetchRoutes()
  },
  onShow() {
    if (this.selectedRoute) {
      this.startAutoRefresh()
    }
  },
  onHide() {
    this.stopAutoRefresh()
  },
  onUnload() {
    this.stopAutoRefresh()
  },
  onPullDownRefresh() {
    if (this.selectedRoute) {
      this.fetchDetail(this.selectedRoute.id).then(() => uni.stopPullDownRefresh())
    } else {
      this.fetchRoutes().then(() => uni.stopPullDownRefresh())
    }
  },
  methods: {
    async fetchRoutes() {
      this.loading = true
      try {
        const res = await getLiveRoutes()
        this.routes = res.data || []
      } catch (e) {
        uni.showToast({ title: '加载失败', icon: 'none' })
      } finally {
        this.loading = false
      }
    },
    async fetchDetail(routeId) {
      this.detailLoading = true
      try {
        const res = await getLiveRouteDetail(routeId)
        const data = res.data
        this.selectedRoute = data.route
        this.stops = data.stops || []
      } catch (e) {
        uni.showToast({ title: '加载失败', icon: 'none' })
      } finally {
        this.detailLoading = false
      }
    },
    openRoute(route) {
      this.selectedRoute = route
      this.fetchDetail(route.id)
      this.startAutoRefresh()
    },
    backToList() {
      this.stopAutoRefresh()
      this.selectedRoute = null
      this.stops = []
      this.fetchRoutes()
    },
    startAutoRefresh() {
      this.stopAutoRefresh()
      this.timer = setInterval(() => {
        if (this.selectedRoute) {
          this.fetchDetail(this.selectedRoute.id)
        }
      }, 10000)
    },
    stopAutoRefresh() {
      if (this.timer) {
        clearInterval(this.timer)
        this.timer = null
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.live-page {
  min-height: 100vh;
  background: #f5f7fb;
  padding-bottom: 120rpx;
}

.page-header {
  padding: 60rpx 32rpx 32rpx;

  .header-title {
    font-size: 44rpx;
    font-weight: 600;
    color: #2E3F57;
  }

  .header-sub {
    display: block;
    margin-top: 12rpx;
    font-size: 26rpx;
    color: #8a93a6;
  }
}

.route-list {
  padding: 0 24rpx;

  .route-card {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 32rpx 28rpx;
    margin-bottom: 20rpx;
    background: #fff;
    border-radius: 16rpx;
    box-shadow: 0 2rpx 12rpx rgba(139, 163, 199, 0.08);

    .route-info {
      display: flex;
      flex-direction: column;
      gap: 12rpx;

      .route-name {
        font-size: 32rpx;
        font-weight: 600;
        color: #2E3F57;
      }
    }
  }
}

.active-badge {
  display: inline-flex;
  align-items: center;
  gap: 8rpx;
  padding: 6rpx 16rpx;
  background: rgba(139, 163, 199, 0.1);
  border-radius: 24rpx;
  font-size: 22rpx;
  color: #6F829F;

  &.gray {
    background: rgba(138, 147, 166, 0.1);
    color: #8a93a6;
  }

  &.mini {
    padding: 4rpx 12rpx;
    font-size: 20rpx;
  }

  .dot {
    width: 12rpx;
    height: 12rpx;
    background: #8BA3C7;
    border-radius: 50%;
    animation: pulse 1.5s infinite;

    &.gray {
      background: #c8cfdb;
      animation: none;
    }
  }
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

.refresh-hint, .refresh-footer {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8rpx;
  margin-top: 40rpx;
  font-size: 24rpx;
  color: #8a93a6;
}

.detail-header {
  display: flex;
  align-items: center;
  padding: 20rpx 24rpx 24rpx;
  background: #fff;

  .back-btn {
    width: 60rpx;
    height: 60rpx;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .detail-title {
    flex: 1;
    padding-left: 12rpx;

    .title {
      font-size: 34rpx;
      font-weight: 600;
      color: #2E3F57;
      display: block;
    }
  }
}

.stops-container {
  padding: 24rpx 32rpx 32rpx;
  background: #fff;
  margin-top: 20rpx;
}

.stop-item {
  display: flex;
  min-height: 140rpx;
}

.stop-marker {
  width: 80rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  flex-shrink: 0;

  .line-top, .line-bottom {
    width: 4rpx;
    flex: 1;
    background: #C5D1E3;
  }

  .stop-dot {
    width: 48rpx;
    height: 48rpx;
    border-radius: 50%;
    background: #fff;
    border: 4rpx solid #8BA3C7;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 22rpx;
    color: #6F829F;
    font-weight: 600;
    flex-shrink: 0;

    &.start, &.end {
      background: #8BA3C7;
      color: #fff;
      border-color: #8BA3C7;
    }
  }
}

.stop-content {
  flex: 1;
  padding-bottom: 24rpx;
}

.stop-name {
  font-size: 30rpx;
  font-weight: 600;
  color: #2E3F57;
  margin-bottom: 12rpx;
}

.vehicles {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.vehicle-item {
  display: flex;
  align-items: baseline;
  gap: 8rpx;
  padding: 8rpx 16rpx;
  background: rgba(139, 163, 199, 0.08);
  border-radius: 8rpx;
  font-size: 24rpx;

  &.arrived {
    background: rgba(22, 163, 74, 0.08);
    color: #16a34a;
  }

  .plate {
    font-weight: 600;
    color: #6F829F;
  }

  .eta {
    color: #2E3F57;
    font-weight: 500;
  }

  .from {
    color: #8a93a6;
    font-size: 22rpx;
  }
}

.more {
  font-size: 22rpx;
  color: #8a93a6;
}

.no-vehicle {
  font-size: 24rpx;
  color: #c8cfdb;
  font-style: italic;
}

.qt-empty {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 200rpx 0;
  font-size: 28rpx;
  color: #8a93a6;
}
</style>
