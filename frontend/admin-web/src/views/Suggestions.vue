<template>
  <div class="page">
    <div class="page-header">
      <div>
        <div class="page-title">调度建议</div>
        <div class="page-subtitle">根据预约与运力情况生成的智能调度建议</div>
      </div>
      <el-button :icon="Refresh" @click="loadSuggestions">刷新</el-button>
    </div>

    <div class="panel">
      <div class="toolbar">
        <span class="filter-label">处理状态</span>
        <el-select v-model="filters.status" style="width: 170px" @change="loadSuggestions">
          <el-option label="全部状态" value="" />
          <el-option
            v-for="(label, value) in SUGGESTION_STATUS_LABELS"
            :key="value"
            :label="label"
            :value="value"
          />
        </el-select>
        <span class="filter-label">班次日期</span>
        <el-date-picker
          v-model="filters.date"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="全部日期"
          clearable
          style="width: 170px"
          @change="loadSuggestions"
        />
        <div class="toolbar-spacer"></div>
        <span class="muted">共 {{ suggestions.length }} 条建议</span>
      </div>

      <div v-loading="loading" class="suggestion-list">
        <el-empty
          v-if="!loading && !suggestions.length"
          description="暂无调度建议，可在预约统计页生成"
          class="empty-tip"
        />

        <div v-for="item in suggestions" :key="item.id" class="suggestion-card">
          <div class="card-head">
            <div class="head-left">
              <el-tag :type="SUGGESTION_TYPE_TAG[item.type] || 'info'" effect="dark" size="small">
                {{ labelOf(SUGGESTION_TYPE_LABELS, item.type) }}
              </el-tag>
              <span class="route-name">{{ item.routeName || '-' }}</span>
              <span class="shift-time">
                {{ item.shiftDate || '-' }} · {{ formatTime(item.departTime) }}
              </span>
            </div>
            <el-tag :type="SUGGESTION_STATUS_TAG[item.status] || 'info'" effect="plain" size="small">
              {{ labelOf(SUGGESTION_STATUS_LABELS, item.status) }}
            </el-tag>
          </div>

          <div class="reason">
            <el-icon><InfoFilled /></el-icon>
            <span>{{ item.reason || '-' }}</span>
          </div>

          <div class="info-grid">
            <div class="info-item">
              <span class="info-label">当前车辆</span>
              <span class="info-value">{{ item.vehiclePlate || '未分配' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">推荐车辆</span>
              <span :class="['info-value', item.suggestedVehiclePlate ? 'recommend' : 'muted']">
                {{ item.suggestedVehiclePlate || '—' }}
              </span>
            </div>
            <div class="info-item">
              <span class="info-label">预约 / 容量</span>
              <span class="info-value mono">{{ item.reservedCount || 0 }} / {{ item.capacity || 0 }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">建议时间</span>
              <span class="info-value muted mono">{{ formatDateTime(item.createTime) }}</span>
            </div>
          </div>

          <div v-if="item.status === 'PENDING'" class="card-actions">
            <el-button size="small" type="primary" @click="handleConfirm(item)">确认执行</el-button>
            <el-button size="small" @click="handleIgnore(item)">忽略</el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { InfoFilled, Refresh } from '@element-plus/icons-vue'
import { dispatchApi } from '../api/admin'
import {
  SUGGESTION_STATUS_LABELS,
  SUGGESTION_STATUS_TAG,
  SUGGESTION_TYPE_LABELS,
  SUGGESTION_TYPE_TAG,
  formatDateTime,
  formatTime,
  labelOf
} from '../utils/constants'

const route = useRoute()
const loading = ref(false)
const suggestions = ref([])

const filters = reactive({
  status: route.query.status === 'PENDING' ? 'PENDING' : '',
  date: ''
})

async function loadSuggestions() {
  loading.value = true
  try {
    suggestions.value = (await dispatchApi.suggestions({
      status: filters.status,
      date: filters.date
    })) || []
  } finally {
    loading.value = false
  }
}

async function handleConfirm(item) {
  const target = item.suggestedVehiclePlate
    ? `推荐车辆「${item.suggestedVehiclePlate}」`
    : '该建议'
  try {
    await ElMessageBox.confirm(`确认执行该调度建议吗？将应用${target}。`, '确认执行', {
      confirmButtonText: '确认执行',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch (e) {
    return
  }
  try {
    await dispatchApi.confirm(item.id)
    ElMessage.success('调度建议已执行')
    await loadSuggestions()
  } catch (e) {
    // 错误提示已由请求拦截器处理
  }
}

async function handleIgnore(item) {
  try {
    await ElMessageBox.confirm('确定忽略该调度建议吗？忽略后不会再执行。', '忽略建议', {
      confirmButtonText: '忽略',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch (e) {
    return
  }
  try {
    await dispatchApi.ignore(item.id)
    ElMessage.success('调度建议已忽略')
    await loadSuggestions()
  } catch (e) {
    // 错误提示已由请求拦截器处理
  }
}

onMounted(loadSuggestions)
</script>

<style scoped>
.filter-label {
  font-size: 13px;
  color: var(--qt-text-secondary);
}

.suggestion-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-height: 120px;
}

.suggestion-card {
  border: 1px solid var(--qt-border);
  border-radius: 12px;
  padding: 16px 18px;
  background: #fff;
  transition: box-shadow 0.2s, border-color 0.2s;
}

.suggestion-card:hover {
  border-color: #c7d7fb;
  box-shadow: 0 6px 18px rgba(37, 99, 235, 0.08);
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.head-left {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.route-name {
  font-size: 15px;
  font-weight: 600;
  color: #111827;
}

.shift-time {
  font-size: 13px;
  color: var(--qt-text-secondary);
  font-variant-numeric: tabular-nums;
}

.reason {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin: 12px 0;
  padding: 10px 12px;
  border-radius: 8px;
  background: #f6f9ff;
  font-size: 13px;
  line-height: 1.7;
  color: #374151;
}

.reason .el-icon {
  margin-top: 3px;
  color: var(--qt-primary);
  flex: none;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 12px 18px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.info-label {
  font-size: 12px;
  color: #9ca3af;
}

.info-value {
  font-size: 14px;
  color: #1f2937;
}

.info-value.recommend {
  color: var(--qt-primary);
  font-weight: 600;
}

.card-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px dashed var(--qt-border);
}
</style>
