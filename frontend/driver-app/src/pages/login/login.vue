<template>
  <view class="login-page">
    <view class="hero">
      <view class="brand-badge">青途</view>
      <view class="hero-title">校园巴士 · 司机端</view>
      <view class="hero-subtitle">安全驾驶 · 准点发车 · 智能调度</view>
    </view>

    <view class="login-card">
      <view class="card-heading">司机登录</view>

      <view class="field">
        <text class="field-label">账号</text>
        <input
          v-model="form.username"
          class="field-input"
          type="text"
          placeholder="请输入司机账号"
          placeholder-class="field-placeholder"
          :disabled="loading"
          @confirm="handleLogin"
        />
      </view>

      <view class="field">
        <text class="field-label">密码</text>
        <input
          v-model="form.password"
          class="field-input"
          type="password"
          password
          placeholder="请输入登录密码"
          placeholder-class="field-placeholder"
          :disabled="loading"
          @confirm="handleLogin"
        />
      </view>

      <button class="login-button" :loading="loading" :disabled="loading" @click="handleLogin">
        {{ loading ? '登录中...' : '登 录' }}
      </button>

      <view class="demo-tip">演示账号：driver01 / 123456</view>
    </view>
  </view>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { login } from '../../api/driver'
import { saveAuth } from '../../api/request'

const form = reactive({
  username: '',
  password: ''
})

const loading = ref(false)

function toast(message) {
  uni.showToast({ title: message, icon: 'none' })
}

async function handleLogin() {
  if (loading.value) return

  const username = form.username.trim()
  if (!username) {
    toast('请输入账号')
    return
  }
  if (!form.password) {
    toast('请输入密码')
    return
  }

  loading.value = true
  try {
    const data = await login({
      username,
      password: form.password,
      role: 'DRIVER'
    })
    saveAuth(data.token, {
      userId: data.userId,
      username: data.username,
      name: data.name,
      role: data.role
    })
    uni.showToast({ title: '登录成功', icon: 'success', duration: 1200 })
    setTimeout(() => {
      uni.reLaunch({ url: '/pages/tasks/tasks' })
    }, 500)
  } catch (e) {
    // 错误提示已由请求封装统一处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  padding: 0 48rpx 60rpx;
  background: linear-gradient(160deg, #8BA3C7 0%, #A9BCD6 38%, #f4f6fa 38%, #f4f6fa 100%);
}

.hero {
  padding: 96rpx 8rpx 72rpx;
  color: #ffffff;
}

.brand-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 104rpx;
  height: 104rpx;
  border-radius: 32rpx;
  background-color: rgba(255, 255, 255, 0.18);
  border: 2rpx solid rgba(255, 255, 255, 0.5);
  font-size: 34rpx;
  font-weight: 700;
  letter-spacing: 2rpx;
}

.hero-title {
  margin-top: 28rpx;
  font-size: 48rpx;
  font-weight: 700;
  letter-spacing: 2rpx;
}

.hero-subtitle {
  margin-top: 14rpx;
  font-size: 26rpx;
  opacity: 0.85;
}

.login-card {
  background-color: #ffffff;
  border-radius: 28rpx;
  padding: 48rpx 40rpx;
  box-shadow: 0 16rpx 48rpx rgba(31, 45, 61, 0.12);
}

.card-heading {
  font-size: 36rpx;
  font-weight: 600;
  color: #1f2d3d;
  margin-bottom: 40rpx;
}

.field {
  margin-bottom: 32rpx;
}

.field-label {
  display: block;
  font-size: 26rpx;
  color: #606266;
  margin-bottom: 14rpx;
}

.field-input {
  width: 100%;
  height: 92rpx;
  padding: 0 28rpx;
  font-size: 30rpx;
  color: #303133;
  background-color: #f5f7fb;
  border-radius: 16rpx;
}

.field-placeholder {
  color: #b3b9c4;
  font-size: 28rpx;
}

.login-button {
  width: 100%;
  height: 92rpx;
  line-height: 92rpx;
  margin-top: 44rpx;
  border-radius: 46rpx;
  background: linear-gradient(90deg, #8BA3C7, #A9BCD6);
  color: #ffffff;
  font-size: 32rpx;
  font-weight: 600;
  box-shadow: 0 12rpx 24rpx rgba(139, 163, 199, 0.28);
}

.login-button[disabled] {
  opacity: 0.7;
  color: #ffffff;
}

.demo-tip {
  margin-top: 30rpx;
  text-align: center;
  font-size: 24rpx;
  color: #909399;
}
</style>
