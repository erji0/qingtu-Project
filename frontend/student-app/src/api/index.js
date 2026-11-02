import request from './request'

export function login(data) {
  return request({
    url: '/auth/login',
    method: 'POST',
    data: { ...data, role: 'STUDENT' }
  })
}

export function register(data) {
  return request({
    url: '/auth/register',
    method: 'POST',
    data
  })
}

export function getRoutes() {
  return request({ url: '/student/routes' })
}

export function getShifts(params = {}) {
  return request({ url: '/student/shifts', data: params })
}

export function getShiftDetail(id) {
  return request({ url: `/student/shifts/${id}` })
}

export function getShiftLocation(id, silent = false) {
  return request({ url: `/student/shifts/${id}/location`, silent })
}

export function createReservation(shiftId) {
  return request({
    url: '/student/reservations',
    method: 'POST',
    data: { shiftId }
  })
}

export function getReservations() {
  return request({ url: '/student/reservations' })
}

export function cancelReservation(id) {
  return request({
    url: `/student/reservations/${id}/cancel`,
    method: 'PUT'
  })
}

export function signReservation(id) {
  return request({
    url: `/student/reservations/${id}/sign`,
    method: 'PUT'
  })
}
