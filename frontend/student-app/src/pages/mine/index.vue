<template>
  <view class="mine-page">
    <view class="hero">
      <view class="avatar">{{ avatarText }}</view>
      <view class="hero-info">
        <text class="user-name">{{ user.name || '同学' }}</text>
        <text class="user-account">账号：{{ user.username || '-' }}</text>
      </view>
      <text class="role-pill">{{ roleText }}</text>
    </view>

    <view class="content">
      <view class="qt-card info-card">
        <view class="info-row">
          <text class="row-label">姓名</text>
          <text class="row-value">{{ user.name || '-' }}</text>
        </view>
        <view class="info-row">
          <text class="row-label">账号</text>
          <text class="row-value">{{ user.username || '-' }}</text>
        </view>
        <view class="info-row">
          <text class="row-label">身份</text>
          <text class="row-value">{{ roleText }}</text>
        </view>
      </view>

      <button class="qt-btn logout-btn" @click="logout">退出登录</button>
      <text class="version">青途 · 校园班车学生端 v1.0.0</text>
    </view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { clearAuth, getToken, getUser } from '@/api/request'

const user = ref({})

const roleText = computed(() => {
  const role = user.value.role
  if (role === 'STUDENT') return '学生'
  if (role === 'ADMIN') return '管理员'
  if (role === 'DRIVER') return '司机'
  return '学生'
})

const avatarText = computed(() => {
  const name = user.value.name || ''
  return name ? name.charAt(0) : '学'
})

onShow(() => {
  if (!getToken()) {
    uni.reLaunch({ url: '/pages/login/login' })
    return
  }
  user.value = getUser() || {}
})

function logout() {
  uni.showModal({
    title: '退出登录',
    content: '确定要退出当前账号吗？',
    confirmText: '退出',
    confirmColor: '#ef4444',
    success: (res) => {
      if (!res.confirm) return
      clearAuth()
      uni.showToast({ title: '已退出登录', icon: 'none' })
      setTimeout(() => {
        uni.reLaunch({ url: '/pages/login/login' })
      }, 400)
    }
  })
}
</script>

<style scoped>
.mine-page {
  min-height: 100vh;
  background-color: #f5f7fb;
}

.hero {
  display: flex;
  align-items: center;
  padding: 48rpx 32rpx 64rpx;
  background: linear-gradient(135deg, #8BA3C7 0%, #A9BCD6 100%);
  border-radius: 0 0 36rpx 36rpx;
}

.avatar {
  width: 108rpx;
  height: 108rpx;
  line-height: 108rpx;
  border-radius: 50%;
  text-align: center;
  font-size: 44rpx;
  font-weight: 700;
  color: #8BA3C7;
  background-color: #ffffff;
  flex-shrink: 0;
}

.hero-info {
  flex: 1;
  min-width: 0;
  margin-left: 24rpx;
  display: flex;
  flex-direction: column;
}

.user-name {
  font-size: 38rpx;
  font-weight: 700;
  color: #ffffff;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-account {
  margin-top: 10rpx;
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.78);
}

.role-pill {
  padding: 8rpx 20rpx;
  border-radius: 24rpx;
  font-size: 22rpx;
  font-weight: 600;
  color: #ffffff;
  background-color: rgba(255, 255, 255, 0.22);
}

.content {
  padding: 24rpx;
}

.info-card {
  padding: 8rpx 26rpx;
}

.info-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 26rpx 0;
  border-bottom: 2rpx solid #f0f3f9;
}

.info-row:last-child {
  border-bottom: none;
}

.row-label {
  font-size: 26rpx;
  color: #8a93a6;
}

.row-value {
  font-size: 28rpx;
  color: #1f2733;
  font-weight: 500;
}

.logout-btn {
  margin-top: 40rpx;
  background-color: #ffffff;
  color: #ef4444;
  border: 2rpx solid #f3d4d4;
}

.version {
  display: block;
  margin-top: 32rpx;
  text-align: center;
  font-size: 22rpx;
  color: #b0b8c6;
}
</style>
