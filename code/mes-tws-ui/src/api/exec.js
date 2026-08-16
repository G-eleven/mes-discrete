import request from './request'

export const checkin = data => request.post('/station/checkin', data)
export const stationContext = params => request.get('/station/context', { params })
export const bind = data => request.post('/station/bind', data)
export const loading = data => request.post('/station/loading', data)

export const pageSn = params => request.get('/sn/page', { params })
export const nextSn = params => request.get('/sn/next', { params })
export const generateMachine = workOrderId => request.post('/sn/generate-machine', { workOrderId })
export const registerComponent = data => request.post('/sn/register-component', data)
export const bindingsOf = parentSn => request.get('/sn/bindings', { params: { parentSn } })

export const pageLog = params => request.get('/log/page', { params })
