<template>
  <div class="page">
    <div class="page-header">
      <div>
        <div class="page-title">站点管理</div>
        <div class="page-subtitle">按线路维护站点顺序与坐标信息</div>
      </div>
      <el-button type="primary" :icon="Plus" :disabled="!selectedRouteId" @click="openCreate">新增站点</el-button>
    </div>

    <div class="panel">
      <div class="toolbar">
        <span class="filter-label">所属线路</span>
        <el-select
          v-model="selectedRouteId"
          placeholder="请选择线路"
          style="width: 260px"
          @change="loadStops"
        >
          <el-option v-for="route in routes" :key="route.id" :label="route.name" :value="route.id">
            <span>{{ route.name }}</span>
            <el-tag v-if="route.status === 0" size="small" type="info" class="option-tag">停用</el-tag>
          </el-option>
        </el-select>
        <span class="muted">共 {{ stops.length }} 个站点</span>
      </div>

      <el-empty v-if="!selectedRouteId" description="请选择线路查看站点" class="empty-tip" />

      <el-table
        v-else
        v-loading="loading"
        :data="stops"
        class="table-card"
        empty-text="该线路暂无站点"
        stripe
      >
        <el-table-column prop="seq" label="顺序" width="90">
          <template #default="{ row }">
            <span class="seq-badge">{{ row.seq }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="站点名称" min-width="200" show-overflow-tooltip />
        <el-table-column label="纬度" width="140">
          <template #default="{ row }">
            <span class="mono muted">{{ row.latitude ?? '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="经度" width="140">
          <template #default="{ row }">
            <span class="mono muted">{{ row.longitude ?? '-' }}</span>
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

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑站点' : '新增站点'" width="520px" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="所属线路" prop="routeId">
          <el-select v-model="form.routeId" placeholder="请选择线路" style="width: 100%">
            <el-option v-for="route in routes" :key="route.id" :label="route.name" :value="route.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="站点名称" prop="name">
          <el-input v-model="form.name" maxlength="50" placeholder="请输入站点名称" show-word-limit />
        </el-form-item>
        <el-form-item label="站点顺序" prop="seq">
          <el-input-number v-model="form.seq" :min="1" :max="999" controls-position="right" />
          <span class="form-tip">数值越小越靠前</span>
        </el-form-item>
        <el-form-item label="纬度">
          <el-input-number
            v-model="form.latitude"
            :precision="6"
            :step="0.000001"
            :controls="false"
            placeholder="例如 30.123456"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="经度">
          <el-input-number
            v-model="form.longitude"
            :precision="6"
            :step="0.000001"
            :controls="false"
            placeholder="例如 120.123456"
            style="width: 100%"
          />
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
import { routeApi, stopApi } from '../api/admin'

const loading = ref(false)
const saving = ref(false)
const routes = ref([])
const stops = ref([])
const selectedRouteId = ref(null)
const dialogVisible = ref(false)
const editingId = ref(null)
const formRef = ref(null)

const form = reactive({
  routeId: null,
  name: '',
  seq: 1,
  latitude: null,
  longitude: null
})

const rules = {
  routeId: [{ required: true, message: '请选择所属线路', trigger: 'change' }],
  name: [{ required: true, message: '请输入站点名称', trigger: 'blur' }],
  seq: [{ required: true, message: '请输入站点顺序', trigger: 'blur' }]
}

async function loadRoutes() {
  routes.value = (await routeApi.list()) || []
  if (!selectedRouteId.value && routes.value.length) {
    selectedRouteId.value = routes.value[0].id
    await loadStops()
  }
}

async function loadStops() {
  if (!selectedRouteId.value) {
    stops.value = []
    return
  }
  loading.value = true
  try {
    const data = (await stopApi.list(selectedRouteId.value)) || []
    stops.value = [...data].sort((a, b) => (a.seq || 0) - (b.seq || 0))
  } finally {
    loading.value = false
  }
}

function nextSeq() {
  const max = stops.value.reduce((m, s) => Math.max(m, s.seq || 0), 0)
  return max + 1
}

function openCreate() {
  editingId.value = null
  Object.assign(form, {
    routeId: selectedRouteId.value,
    name: '',
    seq: nextSeq(),
    latitude: null,
    longitude: null
  })
  dialogVisible.value = true
}

function openEdit(row) {
  editingId.value = row.id
  Object.assign(form, {
    routeId: row.routeId,
    name: row.name,
    seq: row.seq,
    latitude: row.latitude,
    longitude: row.longitude
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
      routeId: form.routeId,
      name: form.name.trim(),
      seq: form.seq,
      latitude: form.latitude,
      longitude: form.longitude
    }
    if (editingId.value) {
      await stopApi.update(editingId.value, payload)
      ElMessage.success('站点已更新')
    } else {
      await stopApi.create(payload)
      ElMessage.success('站点已创建')
    }
    dialogVisible.value = false
    await loadStops()
  } catch (e) {
    // 错误提示已由请求拦截器处理
  } finally {
    saving.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除站点「${row.name}」吗？`, '删除站点', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch (e) {
    return
  }
  try {
    await stopApi.remove(row.id)
    ElMessage.success('站点已删除')
    await loadStops()
  } catch (e) {
    // 错误提示已由请求拦截器处理
  }
}

onMounted(loadRoutes)
</script>

<style scoped>
.filter-label {
  font-size: 13px;
  color: var(--qt-text-secondary);
}

.option-tag {
  margin-left: 8px;
}

.seq-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 24px;
  height: 24px;
  padding: 0 6px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  color: var(--qt-primary);
  background: #F3F6F9;
}

.form-tip {
  margin-left: 10px;
  font-size: 12px;
  color: #9ca3af;
}
</style>
