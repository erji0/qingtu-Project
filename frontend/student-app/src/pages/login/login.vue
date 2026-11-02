<template>
  <view class="login-page">
    <view class="brand">
      <view class="brand-logo">青</view>
      <text class="brand-name">青途 · 校园班车</text>
      <text class="brand-desc">学生端 · 查询班次，轻松预约</text>
    </view>

    <view class="qt-card login-card">
      <text class="card-title">账号登录</text>
      <view class="form-item">
        <text class="label">账号</text>
        <input
          v-model="form.username"
          class="input"
          type="text"
          placeholder="请输入账号"
          placeholder-class="input-placeholder"
          confirm-type="next"
          maxlength="20"
        />
      </view>
      <view class="form-item">
        <text class="label">密码</text>
        <input
          v-model="form.password"
          class="input"
          :password="true"
          placeholder="请输入密码"
          placeholder-class="input-placeholder"
          confirm-type="done"
          maxlength="30"
          @confirm="submit"
        />
      </view>
      <button class="qt-btn login-btn" :loading="loading" :disabled="loading" @click="submit">
        登 录
      </button>
      <view class="register-link" @click="goRegister">
        <text class="link-text">还没有账号？注册新账号</text>
      </view>
    </view>

    <view class="demo-card" @click="fillDemo">
      <view class="demo-head">
        <text class="demo-title">演示账号</text>
        <text class="demo-tip">点击自动填充</text>
      </view>
      <text class="demo-line">账号：student　密码：123456</text>
    </view>

    <text class="footer">校园校车智能调度系统</text>
  </view>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { login } from '@/api'
import { getToken, setAuth } from '@/api/request'

const form = reactive({
  username: '',
  password: ''
})
const loading = ref(false)

onLoad(() => {
  if (getToken()) {
    uni.reLaunch({ url: '/pages/index/index' })
  }
})

function fillDemo() {
  form.username = 'student'
  form.password = '123456'
  uni.showToast({ title: '已填充演示账号', icon: 'none' })
}

function goRegister() {
  uni.navigateTo({ url: '/pages/register/register' })
}

async function submit() {
  if (loading.value) return
  if (!form.username.trim()) {
    uni.showToast({ title: '请输入账号', icon: 'none' })
    return
  }
  if (!form.password) {
    uni.showToast({ title: '请输入密码', icon: 'none' })
    return
  }
  loading.value = true
  try {
    const data = await login({
      username: form.username.trim(),
      password: form.password
    })
    setAuth(data)
    uni.showToast({ title: '登录成功', icon: 'success' })
    setTimeout(() => {
      uni.reLaunch({ url: '/pages/index/index' })
    }, 600)
  } catch (e) {
    // 错误提示已由请求层统一处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  padding: 80rpx 40rpx 48rpx;
  background: linear-gradient(180deg, #8BA3C7 0%, #A9BCD6 40%, #f5f7fb 40%, #f5f7fb 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
}

.brand {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 48rpx;
}

.brand-logo {
  width: 108rpx;
  height: 108rpx;
  line-height: 108rpx;
  border-radius: 28rpx;
  background-color: #ffffff;
  color: #8BA3C7;
  font-size: 48rpx;
  font-weight: 700;
  text-align: center;
  box-shadow: 0 12rpx 30rpx rgba(13, 58, 166, 0.25);
}

.brand-name {
  margin-top: 24rpx;
  color: #ffffff;
  font-size: 40rpx;
  font-weight: 700;
  letter-spacing: 2rpx;
}

.brand-desc {
  margin-top: 12rpx;
  color: rgba(255, 255, 255, 0.78);
  font-size: 24rpx;
}

.login-card {
  width: 100%;
  padding: 40rpx 32rpx;
}

.card-title {
  display: block;
  margin-bottom: 28rpx;
  font-size: 34rpx;
  font-weight: 700;
  color: #1f2733;
}

.form-item {
  margin-bottom: 28rpx;
}

.label {
  display: block;
  margin-bottom: 12rpx;
  font-size: 26rpx;
  color: #5f6b80;
}

.input {
  width: 100%;
  height: 92rpx;
  padding: 0 24rpx;
  background-color: #f6f8fc;
  border: 2rpx solid #e8ecf3;
  border-radius: 16rpx;
  font-size: 30rpx;
  color: #1f2733;
}

.input-placeholder {
  color: #b0b8c6;
}

.login-btn {
  margin-top: 12rpx;
}

.register-link {
  padding: 28rpx 0 8rpx;
  text-align: center;
}

.link-text {
  font-size: 26rpx;
  color: #8BA3C7;
}

.demo-card {
  width: 100%;
  margin-top: 32rpx;
  padding: 24rpx 28rpx;
  border-radius: 20rpx;
  background-color: #ffffff;
  border: 2rpx dashed #D5DFEC;
}

.demo-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.demo-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #8BA3C7;
}

.demo-tip {
  font-size: 22rpx;
  color: #9aa3b2;
}

.demo-line {
  display: block;
  margin-top: 12rpx;
  font-size: 26rpx;
  color: #5f6b80;
}

.footer {
  margin-top: auto;
  padding-top: 48rpx;
  font-size: 22rpx;
  color: #9aa3b2;
}
</style>
