export const VEHICLE_STATUS_LABELS = {
  AVAILABLE: '可用',
  RUNNING: '运行中',
  DISABLED: '停用'
}

export const VEHICLE_STATUS_TAG = {
  AVAILABLE: 'success',
  RUNNING: 'primary',
  DISABLED: 'info'
}

export const SHIFT_STATUS_LABELS = {
  SCHEDULED: '待发车',
  RUNNING: '运行中',
  FINISHED: '已完成',
  CANCELLED: '已取消'
}

export const SHIFT_STATUS_TAG = {
  SCHEDULED: 'info',
  RUNNING: 'primary',
  FINISHED: 'success',
  CANCELLED: 'danger'
}

export const RESERVATION_STATUS_LABELS = {
  RESERVED: '已预约',
  CANCELLED: '已取消'
}

export const RESERVATION_STATUS_TAG = {
  RESERVED: 'success',
  CANCELLED: 'info'
}

export const SUGGESTION_TYPE_LABELS = {
  ADD_VEHICLE: '增加车辆',
  LARGER_VEHICLE: '更换大车',
  REDUCE_CAPACITY: '缩减运力',
  REALLOCATE: '运力重分配',
  REASSIGN_VEHICLE: '重新分配车辆'
}

export const SUGGESTION_TYPE_TAG = {
  ADD_VEHICLE: 'danger',
  LARGER_VEHICLE: 'warning',
  REDUCE_CAPACITY: 'info',
  REALLOCATE: 'primary',
  REASSIGN_VEHICLE: 'danger'
}

export const SUGGESTION_STATUS_LABELS = {
  PENDING: '待处理',
  CONFIRMED: '已确认',
  IGNORED: '已忽略'
}

export const SUGGESTION_STATUS_TAG = {
  PENDING: 'warning',
  CONFIRMED: 'success',
  IGNORED: 'info'
}

export const RUN_STATUS_LABELS = {
  RUNNING: '运行中',
  DELAYED: '延误',
  ARRIVED: '已到达'
}

export const RUN_STATUS_TAG = {
  RUNNING: 'primary',
  DELAYED: 'warning',
  ARRIVED: 'success'
}

export const ROUTE_STATUS_LABELS = {
  1: '启用',
  0: '停用'
}

export function labelOf(map, value) {
  return map[value] || value || '-'
}

export function todayString() {
  const now = new Date()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  return `${now.getFullYear()}-${month}-${day}`
}

export function formatTime(value) {
  if (!value) return '-'
  return String(value).slice(0, 5)
}

export function formatDateTime(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ')
}
