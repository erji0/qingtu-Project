<template>
  <div class="page">
    <div class="page-header">
      <div>
        <div class="page-title">线路管理</div>
        <div class="page-subtitle">维护校车线路基础信息与启用状态</div>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增线路</el-button>
    </div>

    <div class="panel">
      <el-table v-loading="loading" :data="routes" class="table-card" empty-text="暂无线路，请先新增">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="线路名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="description" label="线路描述" min-width="260" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="muted">{{ row.description || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="light" size="small">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="170">
          <template #default="{ row }">
            <span class="muted mono">{{ formatDateTime(row.createTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑线路' : '新增线路'" width="520px" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="线路名称" prop="name">
          <el-input v-model="form.name" maxlength="50" placeholder="例如：东校区—主校区" show-word-limit />
        </el-form-item>
        <el-form-item label="线路描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            maxlength="200"
            placeholder="请输入线路描述"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="停用" />
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
import { routeApi } from '../api/admin'
import { formatDateTime } from '../utils/constants'

const loading = ref(false)
const saving = ref(false)
const routes = ref([])
const dialogVisible = ref(false)
const editingId = ref(null)
const formRef = ref(null)

const form = reactive({
  name: '',
  description: '',
  status: 1
})

const rules = {
  name: [{ required: true, message: '请输入线路名称', trigger: 'blur' }]
}

async function loadRoutes() {
  loading.value = true
  try {
    routes.value = (await routeApi.list()) || []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  Object.assign(form, { name: '', description: '', status: 1 })
  dialogVisible.value = true
}

function openEdit(row) {
  editingId.value = row.id
  Object.assign(form, {
    name: row.name,
    description: row.description || '',
    status: row.status ?? 1
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
      name: form.name.trim(),
      description: form.description.trim(),
      status: form.status
    }
    if (editingId.value) {
      await routeApi.update(editingId.value, payload)
      ElMessage.success('线路已更新')
    } else {
      await routeApi.create(payload)
      ElMessage.success('线路已创建')
    }
    dialogVisible.value = false
    await loadRoutes()
  } catch (e) {
    // 错误提示已由请求拦截器处理
  } finally {
    saving.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除线路「${row.name}」吗？`, '删除线路', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch (e) {
    return
  }
  try {
    await routeApi.remove(row.id)
    ElMessage.success('线路已删除')
    await loadRoutes()
  } catch (e) {
    // 错误提示已由请求拦截器处理
  }
}

onMounted(loadRoutes)
</script>
