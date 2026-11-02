<template>
  <div class="page">
    <div class="page-header">
      <div>
        <div class="page-title">预约记录</div>
        <div class="page-subtitle">查看学生预约班次记录</div>
      </div>
      <el-button :icon="Refresh" @click="loadReservations">刷新</el-button>
    </div>

    <div class="panel">
      <div class="toolbar">
        <span class="filter-label">预约日期</span>
        <el-date-picker
          v-model="date"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="全部日期"
          clearable
          style="width: 170px"
          @change="loadReservations"
        />
        <div class="toolbar-spacer"></div>
        <span class="muted">共 {{ reservations.length }} 条记录</span>
      </div>

      <el-table
        v-loading="loading"
        :data="reservations"
        class="table-card"
        empty-text="暂无预约记录"
        stripe
      >
        <el-table-column prop="studentName" label="学生姓名" min-width="120">
          <template #default="{ row }">
            <span class="student-name">{{ row.studentName || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="routeName" label="线路" min-width="160" show-overflow-tooltip />
        <el-table-column prop="shiftDate" label="日期" width="120">
          <template #default="{ row }">
            <span class="mono">{{ row.shiftDate || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="发车时间" width="110">
          <template #default="{ row }">
            <span class="time-text">{{ formatTime(row.departTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="预约状态" width="110">
          <template #default="{ row }">
            <el-tag :type="RESERVATION_STATUS_TAG[row.status] || 'info'" effect="light" size="small">
              {{ labelOf(RESERVATION_STATUS_LABELS, row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="预约时间" min-width="170">
          <template #default="{ row }">
            <span class="muted mono">{{ formatDateTime(row.createTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="车辆" min-width="120">
          <template #default="{ row }">
            <span class="muted">{{ row.vehiclePlate || '未分配' }}</span>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { reservationApi } from '../api/admin'
import {
  RESERVATION_STATUS_LABELS,
  RESERVATION_STATUS_TAG,
  formatDateTime,
  formatTime,
  labelOf,
  todayString
} from '../utils/constants'

const loading = ref(false)
const date = ref(todayString())
const reservations = ref([])

async function loadReservations() {
  loading.value = true
  try {
    reservations.value = (await reservationApi.list({ date: date.value })) || []
  } finally {
    loading.value = false
  }
}

onMounted(loadReservations)
</script>

<style scoped>
.filter-label {
  font-size: 13px;
  color: var(--qt-text-secondary);
}

.student-name {
  font-weight: 600;
  color: #1f2937;
}

.time-text {
  font-weight: 600;
  color: var(--qt-primary);
  font-variant-numeric: tabular-nums;
}
</style>
