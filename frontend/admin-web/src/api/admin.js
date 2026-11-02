import request from './request'

function clean(params) {
  return Object.fromEntries(
    Object.entries(params || {}).filter(([, value]) => value !== '' && value !== null && value !== undefined)
  )
}

export const authApi = {
  login: (data) => request.post('/auth/login', data)
}

export const routeApi = {
  list: () => request.get('/admin/routes'),
  create: (data) => request.post('/admin/routes', data),
  update: (id, data) => request.put(`/admin/routes/${id}`, data),
  remove: (id) => request.delete(`/admin/routes/${id}`)
}

export const stopApi = {
  list: (routeId) => request.get('/admin/stops', { params: clean({ routeId }) }),
  create: (data) => request.post('/admin/stops', data),
  update: (id, data) => request.put(`/admin/stops/${id}`, data),
  remove: (id) => request.delete(`/admin/stops/${id}`)
}

export const vehicleApi = {
  list: (status) => request.get('/admin/vehicles', { params: clean({ status }) }),
  create: (data) => request.post('/admin/vehicles', data),
  update: (id, data) => request.put(`/admin/vehicles/${id}`, data),
  remove: (id) => request.delete(`/admin/vehicles/${id}`)
}

export const shiftApi = {
  list: (params) => request.get('/admin/shifts', { params: clean(params) }),
  drivers: () => request.get('/admin/shifts/drivers'),
  detail: (id) => request.get(`/admin/shifts/${id}`),
  create: (data) => request.post('/admin/shifts', data),
  update: (id, data) => request.put(`/admin/shifts/${id}`, data),
  assign: (id, data) => request.put(`/admin/shifts/${id}/assign`, data),
  remove: (id) => request.delete(`/admin/shifts/${id}`)
}

export const reservationApi = {
  list: (params) => request.get('/admin/reservations', { params: clean(params) })
}

export const statisticsApi = {
  overview: (date) => request.get('/admin/statistics', { params: clean({ date }) })
}

export const dispatchApi = {
  suggestions: (params) => request.get('/admin/dispatch/suggestions', { params: clean(params) }),
  generate: (date) => request.post('/admin/dispatch/generate', null, { params: clean({ date }) }),
  confirm: (id) => request.put(`/admin/dispatch/suggestions/${id}/confirm`),
  ignore: (id) => request.put(`/admin/dispatch/suggestions/${id}/ignore`)
}

export const monitorApi = {
  locations: () => request.get('/admin/monitor/locations')
}

export const driverApi = {
  list: () => request.get('/admin/drivers'),
  create: (data) => request.post('/admin/drivers', data),
  update: (id, data) => request.put(`/admin/drivers/${id}`, data),
  remove: (id) => request.delete(`/admin/drivers/${id}`),
  updateStatus: (id, status) => request.put(`/admin/drivers/${id}/status`, null, { params: { status } })
}
