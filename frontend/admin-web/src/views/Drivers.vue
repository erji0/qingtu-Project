<template>
  <div class="page">
    <div class="page-header">
      <div>
        <div class="page-title">司机管理</div>
        <div class="page-subtitle">维护司机账号，创建后可在班次分配中选择</div>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增司机</el-button>
    </div>

    <div class="panel">
      <el-table v-loading="loading" :data="drivers" class="table-card" empty-text="暂无司机，请先新增" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" label="账号" min-width="120" />
        <el-table-column prop="name" label="姓名" min-width="110" />
        <el-table-column prop="phone" label="手机号" min-width="140">
          <template #default="{ row }">
            <span class="muted mono">{{ row.phone || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" effect="light" size="small">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="170">
          <template #default="{ row }">
            <span class="muted mono">{{ formatDateTime(row.createTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button
              link
              :type="row.status === 1 ? 'warning' : 'success'"
              @click="toggleStatus(row)"
            >
              {{ row.status === 1 ? '停用' : '启用' }}
            </el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑司机' : '新增司机'" width="480px" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="账号" prop="username">
          <el-input v-model="form.username" maxlength="20" placeholder="例如：driver03" :disabled="!!editingId" />
        </el-form-item>
        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" maxlength="20" placeholder="例如：张师傅" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" maxlength="11" placeholder="11 位手机号" />
        </el-form-item>
        <el-form-item label="密码" :prop="editingId ? '' : 'password'">
          <el-input v-model="form.password" type="password" show-password maxlength="30" :placeholder="editingId ? '留空则不修改' : '请输入密码'" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { driverApi } from '../api/admin'
import { formatDateTime } from '../utils/constants'

const loading = ref(false)
const saving = ref(false)
const drivers = ref([])
const dialogVisible = ref(false)
const editingId = ref(null)
const formRef = ref(null)

const form = reactive({
  username: '',
  name: '',
  phone: '',
  password: ''
})

const rules = {
  username: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { min: 4, max: 20, message: '账号长度需为 4-20 个字符', trigger: 'blur' }
  ],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [{ pattern: /^1\d{10}$/, message: '请输入正确的手机号', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 30, message: '密码长度需为 6-30 个字符', trigger: 'blur' }
  ]
}

async function loadDrivers() {
  loading.value = true
  try {
    drivers.value = (await driverApi.list()) || []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  Object.assign(form, { username: '', name: '', phone: '', password: '' })
  dialogVisible.value = true
}

function openEdit(row) {
  editingId.value = row.id
  Object.assign(form, {
    username: row.username,
    name: row.name,
    phone: row.phone || '',
    password: ''
  })
  dialogVisible.value = true
}

function resetForm() {
  formRef.value?.clearValidate()
}

async function handleSubmit() {
  try {
    await formRef.value.validate()
  } catch (e) {
    return
  }
  saving.value = true
  try {
    const payload = {
      username: form.username.trim(),
      name: form.name.trim(),
      phone: form.phone.trim()
    }
    if (!editingId.value) {
      payload.password = form.password
    } else if (form.password) {
      payload.password = form.password
    }
    if (editingId.value) {
      await driverApi.update(editingId.value, payload)
      ElMessage.success('司机已更新')
    } else {
      await driverApi.create(payload)
      ElMessage.success('司机已创建')
    }
    dialogVisible.value = false
    await loadDrivers()
  } catch (e) {
    // 错误提示已由请求拦截器处理
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row) {
  const next = row.status === 1 ? 0 : 1
  const action = next === 1 ? '启用' : '停用'
  try {
    await ElMessageBox.confirm(`确定要${action}司机「${row.name}」吗？`, `${action}司机`, {
      confirmButtonText: action,
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch (e) {
    return
  }
  try {
    await driverApi.updateStatus(row.id, next)
    ElMessage.success(`${action}成功`)
    await loadDrivers()
  } catch (e) {
    // 错误提示已由请求拦截器处理
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除司机「${row.name}」吗？`, '删除司机', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch (e) {
    return
  }
  try {
    await driverApi.remove(row.id)
    ElMessage.success('司机已删除')
    await loadDrivers()
  } catch (e) {
    // 错误提示已由请求拦截器处理
  }
}

onMounted(loadDrivers)
</script>

<style scoped>
.filter-label {
  font-size: 13px;
  color: var(--qt-text-secondary);
}
</style>
