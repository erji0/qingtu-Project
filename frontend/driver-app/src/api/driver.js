import request from './request'

/** 司机登录 */
export function login(data) {
  return request({
    url: '/auth/login',
    method: 'POST',
    data
  })
}

/** 获取当前司机的任务列表 */
export function getTasks(silent = false) {
  return request({
    url: '/driver/tasks',
    showErrorToast: !silent
  })
}

/** 任务详情 */
export function getTaskDetail(id) {
  return request({
    url: `/driver/tasks/${id}`
  })
}

/** 开始任务 */
export function startTask(id) {
  return request({
    url: `/driver/tasks/${id}/start`,
    method: 'PUT'
  })
}

/** 结束任务 */
export function finishTask(id) {
  return request({
    url: `/driver/tasks/${id}/finish`,
    method: 'PUT'
  })
}

/** 更新运行状态：RUNNING / DELAYED */
export function updateRunStatus(id, runStatus) {
  return request({
    url: `/driver/tasks/${id}/run-status`,
    method: 'PUT',
    data: { runStatus }
  })
}

/** 获取任务车辆位置 */
export function getTaskLocation(id, silent = false) {
  return request({
    url: `/driver/tasks/${id}/location`,
    showErrorToast: !silent
  })
}
