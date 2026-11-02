<template>
  <view class="register-page">
    <view class="page-head">
      <text class="head-title">注册新账号</text>
      <text class="head-desc">填写信息后即可预约校园班车</text>
    </view>

    <view class="qt-card form-card">
      <view class="form-item">
        <text class="label">账号 <text class="required">*</text></text>
        <input
          v-model="form.username"
          class="input"
          type="text"
          placeholder="4-20 个字符"
          placeholder-class="input-placeholder"
          maxlength="20"
        />
      </view>
      <view class="form-item">
        <text class="label">密码 <text class="required">*</text></text>
        <input
          v-model="form.password"
          class="input"
          :password="true"
          placeholder="6-30 个字符"
          placeholder-class="input-placeholder"
          maxlength="30"
        />
      </view>
      <view class="form-item">
        <text class="label">姓名 <text class="required">*</text></text>
        <input
          v-model="form.name"
          class="input"
          type="text"
          placeholder="请输入真实姓名"
          placeholder-class="input-placeholder"
          maxlength="20"
        />
      </view>
      <view class="form-item">
        <text class="label">手机号</text>
        <input
          v-model="form.phone"
          class="input"
          type="number"
          placeholder="选填"
          placeholder-class="input-placeholder"
          maxlength="11"
        />
      </view>

      <button class="qt-btn submit-btn" :loading="loading" :disabled="loading" @click="submit">
        注册并登录
      </button>
      <view class="back-link" @click="backToLogin">
        <text class="link-text">已有账号，返回登录</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { register } from '@/api'
import { setAuth } from '@/api/request'

const form = reactive({
  username: '',
  password: '',
  name: '',
  phone: ''
})
const loading = ref(false)

function backToLogin() {
  uni.navigateBack({
    fail: () => {
      uni.reLaunch({ url: '/pages/login/login' })
    }
  })
}

function showTip(title) {
  uni.showToast({ title, icon: 'none' })
}

async function submit() {
  if (loading.value) return
  const username = form.username.trim()
  const password = form.password
  const name = form.name.trim()
  const phone = form.phone.trim()

  if (username.length < 4 || username.length > 20) {
    showTip('账号长度需为 4-20 个字符')
    return
  }
  if (password.length < 6 || password.length > 30) {
    showTip('密码长度需为 6-30 个字符')
    return
  }
  if (!name) {
    showTip('请输入姓名')
    return
  }

  loading.value = true
  try {
    const data = await register({
      username,
      password,
      name,
      phone
    })
    setAuth(data)
    uni.showToast({ title: '注册成功', icon: 'success' })
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
.register-page {
  min-height: 100vh;
  padding: 48rpx 32rpx;
  background: linear-gradient(180deg, #1e5eff 0%, #3f7bff 28%, #f5f7fb 28%, #f5f7fb 100%);
}

.page-head {
  padding: 16rpx 8rpx 40rpx;
}

.head-title {
  display: block;
  font-size: 42rpx;
  font-weight: 700;
  color: #ffffff;
}

.head-desc {
  display: block;
  margin-top: 12rpx;
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.8);
}

.form-card {
  padding: 40rpx 32rpx;
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

.required {
  color: #ef4444;
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

.submit-btn {
  margin-top: 16rpx;
}

.back-link {
  padding: 28rpx 0 8rpx;
  text-align: center;
}

.link-text {
  font-size: 26rpx;
  color: #1e5eff;
}
</style>
