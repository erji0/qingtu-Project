<template>
  <div class="page">
    <div class="page-header">
      <div>
        <div class="page-title">仪表盘</div>
        <div class="page-subtitle">{{ today }} · 今日运营概览</div>
      </div>
      <el-button :icon="Refresh" :loading="loading" @click="loadData">刷新数据</el-button>
    </div>

    <div class="stat-grid">
      <StatCard label="班次数" :value="stats.totalShifts || 0" hint="今日排班总数" :icon="Tickets" />
      <StatCard
        label="预约数"
        :value="stats.totalReservations || 0"
        hint="今日有效预约"
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
        hint="今日运力利用率"
        :icon="Odometer"
        icon-bg="#e8f6fb"
        icon-color="#0891b2"
      />
    </div>

    <div class="suggestion-banner">
      <div class="banner-left">
        <div class="banner-icon">
          <el-icon><MagicStick /></el-icon>
        </div>
        <div>
          <div class="banner-title">待处理调度建议</div>
          <div class="banner-text">系统检测到运力与预约需求存在差异，建议及时处理</div>
        </div>
      </div>
      <div class="banner-right">
        <div class="banner-count">{{ pendingCount }}</div>
        <span class="banner-unit">条待处理</span>
        <el-button type="primary" plain @click="goSuggestions">去处理</el-button>
      </div>
    </div>

    <div class="panel">
      <div class="panel-title-row">
        <div class="panel-title">今日班次</div>
        <span class="muted">共 {{ shifts.length }} 个班次</span>
      </div>
      <el-table v-loading="loading" :data="shifts" class="table-card" empty-text="今日暂无班次安排">
        <el-table-column prop="routeName" label="线路" min-width="150" show-overflow-tooltip />
        <el-table-column label="发车时间" width="120">
          <template #default="{ row }">
            <span class="mono">{{ formatTime(row.departTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="vehiclePlate" label="车辆" min-width="120" />
        <el-table-column prop="driverName" label="司机" min-width="110" />
        <el-table-column label="预约 / 容量" width="130">
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
        <el-table-column label="起止站点" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="muted">{{ row.startStopName || '-' }} → {{ row.endStopName || '-' }}</span>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Calendar, Odometer, Refresh, Tickets, User, Van } from '@element-plus/icons-vue'
import StatCard from '../components/StatCard.vue'
import { dispatchApi, shiftApi, statisticsApi } from '../api/admin'
import {
  SHIFT_STATUS_LABELS,
  SHIFT_STATUS_TAG,
  formatTime,
  labelOf,
  todayString
} from '../utils/constants'

const router = useRouter()
const today = todayString()

const loading = ref(false)
const stats = ref({})
const pendingCount = ref(0)
const shifts = ref([])

async function loadData() {
  loading.value = true
  const [statsRes, suggestionRes, shiftRes] = await Promise.allSettled([
    statisticsApi.overview(today),
    dispatchApi.suggestions({ status: 'PENDING' }),
    shiftApi.list({ date: today })
  ])
  if (statsRes.status === 'fulfilled') stats.value = statsRes.value || {}
  if (suggestionRes.status === 'fulfilled') pendingCount.value = (suggestionRes.value || []).length
  if (shiftRes.status === 'fulfilled') shifts.value = shiftRes.value || []
  loading.value = false
}

function goSuggestions() {
  router.push({ path: '/suggestions', query: { status: 'PENDING' } })
}

onMounted(loadData)
</script>

<style scoped>
.suggestion-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  flex-wrap: wrap;
  padding: 16px 20px;
  border-radius: 12px;
  border: 1px solid #dbe7ff;
  background: linear-gradient(135deg, #f0f6ff 0%, #ffffff 70%);
}

.banner-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.banner-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #eaf1fe;
  color: var(--qt-primary);
  font-size: 20px;
}

.banner-title {
  font-size: 14px;
  font-weight: 600;
  color: #111827;
}

.banner-text {
  margin-top: 3px;
  font-size: 12px;
  color: var(--qt-text-secondary);
}

.banner-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.banner-count {
  font-size: 26px;
  font-weight: 700;
  color: var(--qt-primary);
  line-height: 1;
}

.banner-unit {
  font-size: 12px;
  color: var(--qt-text-secondary);
  margin-right: 8px;
}

.panel-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.panel-title-row .panel-title {
  margin-bottom: 0;
}
</style>
