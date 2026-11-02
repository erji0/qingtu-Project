<template>
  <div class="page">
    <div class="page-header">
      <div>
        <div class="page-title">预约统计</div>
        <div class="page-subtitle">按日期查看预约概览、线路热度与班次上座率</div>
      </div>
      <div class="toolbar">
        <el-date-picker
          v-model="date"
          type="date"
          value-format="YYYY-MM-DD"
          :clearable="false"
          style="width: 170px"
          @change="loadStatistics"
        />
        <el-button type="primary" :icon="MagicStick" :loading="generating" @click="handleGenerate">
          生成调度建议
        </el-button>
      </div>
    </div>

    <div class="stat-grid">
      <StatCard label="班次数" :value="stats.totalShifts || 0" hint="统计日班次总数" :icon="Tickets" />
      <StatCard
        label="预约数"
        :value="stats.totalReservations || 0"
        hint="统计日有效预约"
        :icon="Calendar"
        icon-bg="#e8f7ef"
        icon-color="#0f9d58"
      />
      <StatCard
        label="学生数"
        :value="stats.totalStudents || 0"
        hint="平台注册学生"
        :icon="User"
        icon-bg="#fff4e5"
        icon-color="#d97706"
      />
      <StatCard
        label="车辆数"
        :value="stats.totalVehicles || 0"
        hint="系统车辆总数"
        :icon="Van"
        icon-bg="#eef0ff"
        icon-color="#4f46e5"
      />
      <StatCard
        label="平均上座率"
        :value="stats.avgOccupancyRate ?? 0"
        suffix="%"
        hint="整体运力利用率"
        :icon="Odometer"
        icon-bg="#e8f6fb"
        icon-color="#0891b2"
      />
    </div>

    <div class="panel">
      <div class="panel-title">线路热度</div>
      <el-table
        v-loading="loading"
        :data="stats.routeStats || []"
        class="table-card"
        empty-text="暂无线路统计数据"
        stripe
      >
        <el-table-column type="index" label="排名" width="80" />
        <el-table-column prop="routeName" label="线路" min-width="180" show-overflow-tooltip />
        <el-table-column prop="shiftCount" label="班次数" width="110" />
        <el-table-column prop="capacity" label="总运力" width="110" />
        <el-table-column prop="reservedCount" label="预约数" width="110" />
        <el-table-column label="上座率" min-width="220">
          <template #default="{ row }">
            <div class="rate-cell">
              <el-progress
                :percentage="Math.min(Number(row.occupancyRate) || 0, 100)"
                :stroke-width="8"
                :show-text="false"
                :color="rateColor(row.occupancyRate)"
              />
              <span class="rate-text">{{ row.occupancyRate ?? 0 }}%</span>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="panel">
      <div class="panel-title">班次上座率</div>
      <el-table
        v-loading="loading"
        :data="stats.shiftStats || []"
        class="table-card"
        empty-text="暂无班次统计数据"
        stripe
      >
        <el-table-column prop="routeName" label="线路" min-width="160" show-overflow-tooltip />
        <el-table-column label="发车时间" width="110">
          <template #default="{ row }">
            <span class="time-text">{{ formatTime(row.departTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="vehiclePlate" label="车辆" min-width="120" />
        <el-table-column label="预约 / 容量" width="120">
          <template #default="{ row }">
            <span class="mono">{{ row.reservedCount || 0 }} / {{ row.capacity || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="上座率" min-width="220">
          <template #default="{ row }">
            <div class="rate-cell">
              <el-progress
                :percentage="Math.min(Number(row.occupancyRate) || 0, 100)"
                :stroke-width="8"
                :show-text="false"
                :color="rateColor(row.occupancyRate)"
              />
              <span class="rate-text">{{ row.occupancyRate ?? 0 }}%</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="SHIFT_STATUS_TAG[row.status] || 'info'" effect="light" size="small">
              {{ labelOf(SHIFT_STATUS_LABELS, row.status) }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { Calendar, MagicStick, Odometer, Tickets, User, Van } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import StatCard from '../components/StatCard.vue'
import { dispatchApi, statisticsApi } from '../api/admin'
import {
  SHIFT_STATUS_LABELS,
  SHIFT_STATUS_TAG,
  formatTime,
  labelOf,
  todayString
} from '../utils/constants'

const date = ref(todayString())
const loading = ref(false)
const generating = ref(false)
const stats = ref({})

async function loadStatistics() {
  loading.value = true
  try {
    stats.value = (await statisticsApi.overview(date.value)) || {}
  } finally {
    loading.value = false
  }
}

async function handleGenerate() {
  generating.value = true
  try {
    const count = await dispatchApi.generate(date.value)
    if (count > 0) {
      ElMessage.success(`本次新增 ${count} 条调度建议`)
    } else {
      ElMessage.info('暂无需处理的调度建议')
    }
  } catch (e) {
    // 错误提示已由请求拦截器处理
  } finally {
    generating.value = false
  }
}

function rateColor(rate) {
  const value = Number(rate) || 0
  if (value >= 80) return '#2563eb'
  if (value >= 50) return '#0f9d58'
  if (value >= 30) return '#d97706'
  return '#dc2626'
}

onMounted(loadStatistics)
</script>

<style scoped>
.rate-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.rate-cell :deep(.el-progress) {
  flex: 1;
  min-width: 90px;
}

.rate-text {
  width: 52px;
  text-align: right;
  font-size: 13px;
  color: #374151;
  font-variant-numeric: tabular-nums;
}

.time-text {
  font-weight: 600;
  color: var(--qt-primary);
  font-variant-numeric: tabular-nums;
}
</style>
