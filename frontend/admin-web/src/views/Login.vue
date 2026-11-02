<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-brand">
        <div class="login-logo">
          <div class="login-logo-badge">
            <el-icon><Van /></el-icon>
          </div>
          <span>清途校车</span>
        </div>

        <div>
          <h2>校园校车<br />智能调度管理平台</h2>
          <p>统一管理线路、站点、车辆与班次，实时掌握预约与运力，让每一次发车都更高效。</p>
        </div>

        <div class="login-features">
          <div class="login-feature">
            <el-icon><DataLine /></el-icon>预约数据与上座率一目了然
          </div>
          <div class="login-feature">
            <el-icon><MagicStick /></el-icon>智能调度建议辅助决策
          </div>
          <div class="login-feature">
            <el-icon><Monitor /></el-icon>车辆位置实时监控
          </div>
        </div>
      </div>

      <div class="login-form-wrap">
        <h3>管理员登录</h3>
        <p class="login-tip">请使用管理员账号登录后台</p>

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="handleLogin">
          <el-form-item label="账号" prop="username">
            <el-input v-model="form.username" size="large" placeholder="请输入管理员账号" clearable @keyup.enter="handleLogin">
              <template #prefix>
                <el-icon><User /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              size="large"
              type="password"
              show-password
              placeholder="请输入密码"
              @keyup.enter="handleLogin"
            >
              <template #prefix>
                <el-icon><Lock /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-button type="primary" class="login-submit" :loading="loading" @click="handleLogin">
            登 录
          </el-button>
        </el-form>

        <div class="login-footer">仅限管理员访问 · 清途校园校车调度系统</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { authApi } from '../api/admin'
import { setAuth } from '../utils/auth'

const router = useRouter()
const route = useRoute()

const formRef = ref(null)
const loading = ref(false)
const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入管理员账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  if (loading.value) return
  try {
    await formRef.value.validate()
  } catch (e) {
    return
  }

  loading.value = true
  try {
    const data = await authApi.login({
      username: form.username.trim(),
      password: form.password,
      role: 'ADMIN'
    })
    if (data.role !== 'ADMIN') {
      ElMessage.error('该账号不是管理员，无法登录管理端')
      return
    }
    setAuth(data.token, {
      userId: data.userId,
      username: data.username,
      name: data.name,
      role: data.role
    })
    ElMessage.success('登录成功')
    router.replace(route.query.redirect || '/dashboard')
  } catch (e) {
    // 错误提示已由请求拦截器处理
  } finally {
    loading.value = false
  }
}
</script>
