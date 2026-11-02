<template>
  <div class="page">
    <div class="page-header">
      <div>
        <div class="page-title">班次管理</div>
        <div class="page-subtitle">按日期与线路安排班次，分配车辆和司机</div>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增班次</el-button>
    </div>

    <div class="panel">
      <div class="toolbar">
        <el-date-picker
          v-model="filters.date"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="选择日期"
          style="width: 170px"
          @change="loadShifts"
        />
        <el-select
          v-model="filters.routeId"
          placeholder="全部线路"
          clearable
          style="width: 220px"
          @change="loadShifts"
        >
          <el-option v-for="route in routes" :key="route.id" :label="route.name" :value="route.id" />
        </el-select>
        <el-button :icon="Refresh" @click="loadShifts">刷新</el-button>
        <div class="toolbar-spacer"></div>
        <span class="muted">共 {{ shifts.length }} 个班次</span>
      </div>

      <el-table v-loading="loading" :data="shifts" class="table-card" empty-text="暂无班次" stripe>
        <el-table-column prop="routeName" label="线路" min-width="150" show-overflow-tooltip />
        <el-table-column prop="shiftDate" label="日期" width="120">
          <template #default="{ row }">
            <span class="mono">{{ row.shiftDate }}</span>
          </template>
        </el-table-column>
        <el-table-column label="发车时间" width="110">
          <template #default="{ row }">
            <span class="time-text">{{ formatTime(row.departTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="车辆" min-width="130">
          <template #default="{ row }">
            <span :class="{ muted: !row.vehicleId }">{{ row.vehiclePlate }}</span>
          </template>
        </el-table-column>
        <el-table-column label="司机" min-width="110">
          <template #default="{ row }">
            <span :class="{ muted: !row.driverId }">{{ row.driverName }}</span>
          </template>
        </el-table-column>
        <el-table-column label="预约 / 容量" width="120">
          <template #default="{ row }">
            <span class="mono">{{ row.reservedCount || 0 }} / {{ row.capacity || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="SHIFT_STATUS_TAG[row.status] || 'info'" effect="light" size="small">
              {{ labelOf(SHIFT_STATUS_LABELS, row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="success" @click="openAssign(row)">分配</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 新增 / 编辑班次 -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑班次' : '新增班次'"
      width="560px"
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="线路" prop="routeId">
          <el-select v-model="form.routeId" placeholder="请选择线路" style="width: 100%">
            <el-option v-for="route in routes" :key="route.id" :label="route.name" :value="route.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="班次日期" prop="shiftDate">
          <el-date-picker
            v-model="form.shiftDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="请选择日期"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="发车时间" prop="departTime">
          <el-time-picker
            v-model="form.departTime"
            format="HH:mm:ss"
            value-format="HH:mm:ss"
            placeholder="请选择发车时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="车辆">
          <el-select v-model="form.vehicleId" placeholder="暂不分配" clearable style="width: 100%">
            <el-option
              v-for="vehicle in vehicles"
              :key="vehicle.id"
              :label="vehicleOptionLabel(vehicle)"
              :value="vehicle.id"
              :disabled="vehicle.status === 'DISABLED'"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="司机">
          <el-select v-model="form.driverId" placeholder="暂不分配" clearable style="width: 100%">
            <el-option
              v-for="driver in drivers"
              :key="driver.id"
              :label="driverOptionLabel(driver)"
              :value="driver.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="班次状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option
              v-for="(label, value) in SHIFT_STATUS_LABELS"
              :key="value"
              :label="label"
              :value="value"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 分配车辆 / 司机 -->
    <el-dialog v-model="assignVisible" title="分配车辆与司机" width="480px">
      <div class="assign-tip">
        <el-icon><InfoFilled /></el-icon>
        {{ assignTarget?.routeName }} · {{ assignTarget?.shiftDate }} {{ formatTime(assignTarget?.departTime) }}
      </div>
      <el-form ref="assignFormRef" :model="assignForm" :rules="assignRules" label-width="80px">
        <el-form-item label="车辆" prop="vehicleId">
          <el-select v-model="assignForm.vehicleId" placeholder="请选择车辆" style="width: 100%">
            <el-option
              v-for="vehicle in vehicles"
              :key="vehicle.id"
              :label="vehicleOptionLabel(vehicle)"
              :value="vehicle.id"
              :disabled="vehicle.status === 'DISABLED'"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="司机" prop="driverId">
          <el-select v-model="assignForm.driverId" placeholder="请选择司机" style="width: 100%">
            <el-option
              v-for="driver in drivers"
              :key="driver.id"
              :label="driverOptionLabel(driver)"
              :value="driver.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" :loading="assigning" @click="handleAssign">确认分配</el-button>
      </template>
    </el-dialog>

    <!-- 班次详情 -->
    <el-dialog v-model="detailVisible" title="班次详情" width="720px">
      <div v-loading="detailLoading">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="线路">{{ detail.routeName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="日期">{{ detail.shiftDate || '-' }}</el-descriptions-item>
          <el-descriptions-item label="发车时间">{{ formatTime(detail.departTime) }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="SHIFT_STATUS_TAG[detail.status] || 'info'" size="small" effect="light">
              {{ labelOf(SHIFT_STATUS_LABELS, detail.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="车辆">{{ detail.vehiclePlate || '-' }}</el-descriptions-item>
          <el-descriptions-item label="司机">{{ detail.driverName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="预约 / 容量">
            {{ detail.reservedCount || 0 }} / {{ detail.capacity || 0 }}
          </el-descriptions-item>
          <el-descriptions-item label="起止站点">
            {{ detail.startStopName || '-' }} → {{ detail.endStopName || '-' }}
          </el-descriptions-item>
        </el-descriptions>

        <div class="detail-stops-title">站点列表（{{ (detail.stops || []).length }} 个）</div>
        <el-table :data="detail.stops || []" size="small" empty-text="该线路暂无站点" border>
          <el-table-column prop="seq" label="顺序" width="70" />
          <el-table-column prop="name" label="站点名称" min-width="150" />
          <el-table-column label="纬度" width="120">
            <template #default="{ row }">{{ row.latitude ?? '-' }}</template>
          </el-table-column>
          <el-table-column label="经度" width="120">
            <template #default="{ row }">{{ row.longitude ?? '-' }}</template>
          </el-table-column>
        </el-table>
      </div>
      <template #footer>
        <el-button type="primary" @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { InfoFilled, Plus, Refresh } from '@element-plus/icons-vue'
import { routeApi, shiftApi, vehicleApi } from '../api/admin'
import {
  SHIFT_STATUS_LABELS,
  SHIFT_STATUS_TAG,
  VEHICLE_STATUS_LABELS,
  formatTime,
  labelOf,
  todayString
} from '../utils/constants'

const loading = ref(false)
const saving = ref(false)
const assigning = ref(false)
const detailLoading = ref(false)

const routes = ref([])
const vehicles = ref([])
const drivers = ref([])
const shifts = ref([])

const filters = reactive({
  date: todayString(),
  routeId: ''
})

const dialogVisible = ref(false)
const editingId = ref(null)
const formRef = ref(null)
const form = reactive({
  routeId: null,
  vehicleId: null,
  driverId: null,
  shiftDate: todayString(),
  departTime: '07:30:00',
  status: 'SCHEDULED'
})

const rules = {
  routeId: [{ required: true, message: '请选择线路', trigger: 'change' }],
  shiftDate: [{ required: true, message: '请选择班次日期', trigger: 'change' }],
  departTime: [{ required: true, message: '请选择发车时间', trigger: 'change' }]
}

const assignVisible = ref(false)
const assignTarget = ref(null)
const assignFormRef = ref(null)
const assignForm = reactive({
  vehicleId: null,
  driverId: null
})

const assignRules = {
  vehicleId: [{ required: true, message: '请选择车辆', trigger: 'change' }],
  driverId: [{ required: true, message: '请选择司机', trigger: 'change' }]
}

const detailVisible = ref(false)
const detail = ref({})

function vehicleOptionLabel(vehicle) {
  const status = vehicle.status === 'DISABLED' ? `（${VEHICLE_STATUS_LABELS.DISABLED}）` : ''
  return `${vehicle.plateNo} · ${vehicle.capacity} 座${status}`
}

function driverOptionLabel(driver) {
  return driver.name ? `${driver.name}（${driver.username}）` : driver.username
}

async function loadBaseData() {
  const [routeRes, vehicleRes, driverRes] = await Promise.allSettled([
    routeApi.list(),
    vehicleApi.list(),
    shiftApi.drivers()
  ])
  if (routeRes.status === 'fulfilled') routes.value = routeRes.value || []
  if (vehicleRes.status === 'fulfilled') vehicles.value = vehicleRes.value || []
  if (driverRes.status === 'fulfilled') drivers.value = driverRes.value || []
}

async function loadShifts() {
  loading.value = true
  try {
    shifts.value = (await shiftApi.list({ date: filters.date, routeId: filters.routeId })) || []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  Object.assign(form, {
    routeId: routes.value[0]?.id || null,
    vehicleId: null,
    driverId: null,
    shiftDate: filters.date || todayString(),
    departTime: '07:30:00',
    status: 'SCHEDULED'
  })
  dialogVisible.value = true
}

function openEdit(row) {
  editingId.value = row.id
  Object.assign(form, {
    routeId: row.routeId,
    vehicleId: row.vehicleId,
    driverId: row.driverId,
    shiftDate: row.shiftDate,
    departTime: row.departTime,
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
      routeId: form.routeId,
      vehicleId: form.vehicleId,
      driverId: form.driverId,
      shiftDate: form.shiftDate,
      departTime: form.departTime,
      status: form.status
    }
    if (editingId.value) {
      await shiftApi.update(editingId.value, payload)
      ElMessage.success('班次已更新')
    } else {
      await shiftApi.create(payload)
      ElMessage.success('班次已创建')
    }
    dialogVisible.value = false
    await loadShifts()
  } catch (e) {
    // 错误提示已由请求拦截器处理
  } finally {
    saving.value = false
  }
}

function openAssign(row) {
  assignTarget.value = row
  Object.assign(assignForm, {
    vehicleId: row.vehicleId,
    driverId: row.driverId
  })
  assignVisible.value = true
}

async function handleAssign() {
  try {
    await assignFormRef.value.validate()
  } catch (e) {
    return
  }
  assigning.value = true
  try {
    await shiftApi.assign(assignTarget.value.id, {
      vehicleId: assignForm.vehicleId,
      driverId: assignForm.driverId
    })
    ElMessage.success('车辆与司机已分配')
    assignVisible.value = false
    await loadShifts()
  } catch (e) {
    // 错误提示已由请求拦截器处理
  } finally {
    assigning.value = false
  }
}

async function openDetail(row) {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = {}
  try {
    const data = await shiftApi.detail(row.id)
    detail.value = data || {}
  } catch (e) {
    // 错误提示已由请求拦截器处理
  } finally {
    detailLoading.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定删除 ${row.shiftDate} ${formatTime(row.departTime)} 的「${row.routeName}」班次吗？`,
      '删除班次',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )
  } catch (e) {
    return
  }
  try {
    await shiftApi.remove(row.id)
    ElMessage.success('班次已删除')
    await loadShifts()
  } catch (e) {
    // 错误提示已由请求拦截器处理
  }
}

onMounted(async () => {
  await loadBaseData()
  await loadShifts()
})
</script>

<style scoped>
.time-text {
  font-weight: 600;
  color: var(--qt-primary);
  font-variant-numeric: tabular-nums;
}

.assign-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 16px;
  padding: 10px 12px;
  border-radius: 8px;
  background: #f4f8ff;
  font-size: 13px;
  color: #374151;
}

.detail-stops-title {
  margin: 18px 0 10px;
  font-size: 14px;
  font-weight: 600;
  color: #374151;
}
</style>
