<template>
  <div class="page">
    <div class="page-header">
      <div>
        <div class="page-title">车辆监控</div>
        <div class="page-subtitle">
          每 10 秒自动刷新运行中车辆位置
          <span v-if="lastUpdated" class="last-update">· 最近更新 {{ lastUpdated }}</span>
        </div>
      </div>
      <el-button :icon="Refresh" :loading="loading" @click="fetchLocations(true)">立即刷新</el-button>
    </div>

    <div v-loading="loading" class="monitor-wrap">
      <el-empty v-if="!loading && !locations.length" description="暂无运行中车辆" class="monitor-empty" />

      <div v-if="locations.length" class="monitor-grid">
        <div v-for="item in locations" :key="item.shiftId" class="monitor-card">
          <div class="monitor-head">
            <div class="head-left">
              <div class="bus-icon">
                <el-icon><Van /></el-icon>
              </div>
              <div>
                <div class="route-name">{{ item.routeName || '未知线路' }}</div>
                <div class="plate-no">{{ item.plateNo || '未分配' }}</div>
              </div>
            </div>
            <el-tag :type="RUN_STATUS_TAG[item.runStatus] || 'info'" effect="light" size="small">
              {{ labelOf(RUN_STATUS_LABELS, item.runStatus) }}
            </el-tag>
          </div>

          <div class="stop-flow">
            <div class="stop-block current">
              <span class="stop-label">当前站</span>
              <span class="stop-name">{{ item.currentStopName || '-' }}</span>
            </div>
            <div class="stop-arrow">
              <el-icon><Right /></el-icon>
            </div>
            <div class="stop-block next">
              <span class="stop-label">下一站</span>
              <span class="stop-name">{{ item.nextStopName || '-' }}</span>
            </div>
          </div>

          <div class="progress-row">
            <el-progress
              :percentage="progressOf(item.progressPercent)"
              :stroke-width="10"
              :color="progressColor(item)"
              :format="(p) => `${p}%`"
            />
          </div>

          <div class="eta-row">
            <div class="eta-item">
              <span class="eta-value">{{ item.etaNextStopMinutes ?? 0 }}<small>分钟</small></span>
              <span class="eta-label">到下一站</span>
            </div>
            <div class="eta-divider"></div>
            <div class="eta-item">
              <span class="eta-value">{{ item.etaArriveMinutes ?? 0 }}<small>分钟</small></span>
              <span class="eta-label">到终点</span>
            </div>
            <div class="eta-divider"></div>
            <div class="eta-item">
              <span class="eta-value">{{ (item.stops || []).length }}<small>站</small></span>
              <span class="eta-label">全程站点</span>
            </div>
          </div>

          <div class="monitor-footer">
            <span>经纬度：{{ item.latitude ?? '-' }}, {{ item.longitude ?? '-' }}</span>
            <span>{{ formatDateTime(item.updateTime) }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { Refresh, Right, Van } from '@element-plus/icons-vue'
import { monitorApi } from '../api/admin'
import {
  RUN_STATUS_LABELS,
  RUN_STATUS_TAG,
  formatDateTime,
  labelOf
} from '../utils/constants'

const POLL_INTERVAL = 10000

const loading = ref(false)
const locations = ref([])
const lastUpdated = ref('')
let timer = null

function progressOf(value) {
  const num = Number(value) || 0
  return Math.max(0, Math.min(100, Math.round(num)))
}

function progressColor(item) {
  if (item.runStatus === 'DELAYED') return '#d97706'
  if (item.runStatus === 'ARRIVED') return '#0f9d58'
  return '#2563eb'
}

async function fetchLocations(showLoading = false) {
  if (showLoading) loading.value = true
  try {
    locations.value = (await monitorApi.locations()) || []
    lastUpdated.value = new Date().toLocaleTimeString('zh-CN', { hour12: false })
  } catch (e) {
    // 错误提示已由请求拦截器处理，轮询继续
  } finally {
    if (showLoading) loading.value = false
  }
}

onMounted(() => {
  fetchLocations(true)
  timer = setInterval(() => fetchLocations(false), POLL_INTERVAL)
})

onBeforeUnmount(() => {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
})
</script>

<style scoped>
.last-update {
  color: #9ca3af;
}

.monitor-wrap {
  min-height: 220px;
}

.monitor-empty {
  background: #fff;
  border-radius: 12px;
  padding: 60px 0;
}

.monitor-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
  gap: 16px;
}

.monitor-card {
  background: #fff;
  border-radius: 12px;
  padding: 18px;
  border: 1px solid rgba(37, 99, 235, 0.1);
  box-shadow: 0 1px 2px rgba(15, 42, 92, 0.06), 0 6px 18px rgba(15, 42, 92, 0.04);
}

.monitor-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.head-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.bus-icon {
  width: 42px;
  height: 42px;
  border-radius: 11px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #eaf1fe;
  color: var(--qt-primary);
  font-size: 20px;
}

.route-name {
  font-size: 15px;
  font-weight: 600;
  color: #111827;
}

.plate-no {
  margin-top: 3px;
  font-size: 12px;
  color: var(--qt-text-secondary);
  letter-spacing: 0.5px;
}

.stop-flow {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 18px 0 14px;
}

.stop-block {
  flex: 1;
  min-width: 0;
  padding: 10px 12px;
  border-radius: 9px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  background: #f6f9ff;
  border: 1px solid #e5eefc;
}

.stop-block.next {
  background: #fbfdff;
  border-color: var(--qt-border);
}

.stop-label {
  font-size: 11px;
  color: #9ca3af;
}

.stop-name {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.stop-block.current .stop-name {
  color: var(--qt-primary);
}

.stop-arrow {
  flex: none;
  color: #9ca3af;
}

.progress-row {
  margin-bottom: 14px;
}

.progress-row :deep(.el-progress__text) {
  font-size: 12px !important;
  color: #6b7280;
  min-width: 36px;
}

.eta-row {
  display: flex;
  align-items: center;
  padding: 12px 0;
  border-top: 1px dashed var(--qt-border);
  border-bottom: 1px dashed var(--qt-border);
}

.eta-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 3px;
}

.eta-value {
  font-size: 20px;
  font-weight: 700;
  color: #111827;
  line-height: 1.2;
  font-variant-numeric: tabular-nums;
}

.eta-value small {
  margin-left: 3px;
  font-size: 11px;
  font-weight: 400;
  color: var(--qt-text-secondary);
}

.eta-label {
  font-size: 12px;
  color: #9ca3af;
}

.eta-divider {
  width: 1px;
  height: 26px;
  background: var(--qt-border);
}

.monitor-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-top: 12px;
  font-size: 11px;
  color: #9ca3af;
  flex-wrap: wrap;
}
</style>
