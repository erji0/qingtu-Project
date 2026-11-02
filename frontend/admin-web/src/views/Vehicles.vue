<template>
  <div class="page">
    <div class="page-header">
      <div>
        <div class="page-title">车辆管理</div>
        <div class="page-subtitle">维护车辆信息、容量与运行状态</div>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增车辆</el-button>
    </div>

    <div class="panel">
      <div class="toolbar">
        <span class="filter-label">车辆状态</span>
        <el-select v-model="statusFilter" placeholder="全部状态" clearable style="width: 180px" @change="loadVehicles">
          <el-option v-for="(label, value) in VEHICLE_STATUS_LABELS" :key="value" :label="label" :value="value" />
        </el-select>
        <span class="muted">共 {{ vehicles.length }} 辆车</span>
      </div>

      <el-table v-loading="loading" :data="vehicles" class="table-card" empty-text="暂无车辆，请先新增" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="plateNo" label="车牌号" min-width="130">
          <template #default="{ row }">
            <span class="plate">{{ row.plateNo }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="model" label="车型" min-width="150" show-overflow-tooltip />
        <el-table-column prop="capacity" label="核载人数" width="110">
          <template #default="{ row }">
            <span class="mono">{{ row.capacity }} 人</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="VEHICLE_STATUS_TAG[row.status] || 'info'" effect="light" size="small">
              {{ labelOf(VEHICLE_STATUS_LABELS, row.status) }}
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

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑车辆' : '新增车辆'" width="520px" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="车牌号" prop="plateNo">
          <el-input v-model="form.plateNo" maxlength="20" placeholder="例如：苏A·88888" />
        </el-form-item>
        <el-form-item label="车型" prop="model">
          <el-input v-model="form.model" maxlength="50" placeholder="例如：宇通 45 座大巴" />
        </el-form-item>
        <el-form-item label="核载人数" prop="capacity">
          <el-input-number v-model="form.capacity" :min="1" :max="999" controls-position="right" />
          <span class="form-tip">单位：人</span>
        </el-form-item>
        <el-form-item label="车辆状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio-button v-for="(label, value) in VEHICLE_STATUS_LABELS" :key="value" :value="value">
              {{ label }}
            </el-radio-button>
          </el-radio-group>
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
import { vehicleApi } from '../api/admin'
import {
  VEHICLE_STATUS_LABELS,
  VEHICLE_STATUS_TAG,
  formatDateTime,
  labelOf
} from '../utils/constants'

const loading = ref(false)
const saving = ref(false)
const vehicles = ref([])
const statusFilter = ref('')
const dialogVisible = ref(false)
const editingId = ref(null)
const formRef = ref(null)

const form = reactive({
  plateNo: '',
  model: '',
  capacity: 45,
  status: 'AVAILABLE'
})

const rules = {
  plateNo: [{ required: true, message: '请输入车牌号', trigger: 'blur' }],
  model: [{ required: true, message: '请输入车型', trigger: 'blur' }],
  capacity: [{ required: true, message: '请输入核载人数', trigger: 'blur' }],
  status: [{ required: true, message: '请选择车辆状态', trigger: 'change' }]
}

async function loadVehicles() {
  loading.value = true
  try {
    vehicles.value = (await vehicleApi.list(statusFilter.value)) || []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  Object.assign(form, { plateNo: '', model: '', capacity: 45, status: 'AVAILABLE' })
  dialogVisible.value = true
}

function openEdit(row) {
  editingId.value = row.id
  Object.assign(form, {
    plateNo: row.plateNo,
    model: row.model,
    capacity: row.capacity,
    status: row.status
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
      plateNo: form.plateNo.trim(),
      model: form.model.trim(),
      capacity: form.capacity,
      status: form.status
    }
    if (editingId.value) {
      await vehicleApi.update(editingId.value, payload)
      ElMessage.success('车辆已更新')
    } else {
      await vehicleApi.create(payload)
      ElMessage.success('车辆已创建')
    }
    dialogVisible.value = false
    await loadVehicles()
  } catch (e) {
    // 错误提示已由请求拦截器处理
  } finally {
    saving.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除车辆「${row.plateNo}」吗？`, '删除车辆', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch (e) {
    return
  }
  try {
    await vehicleApi.remove(row.id)
    ElMessage.success('车辆已删除')
    await loadVehicles()
  } catch (e) {
    // 错误提示已由请求拦截器处理
  }
}

onMounted(loadVehicles)
</script>

<style scoped>
.filter-label {
  font-size: 13px;
  color: var(--qt-text-secondary);
}

.plate {
  font-weight: 600;
  color: #1f2937;
}

.form-tip {
  margin-left: 10px;
  font-size: 12px;
  color: #9ca3af;
}
</style>
