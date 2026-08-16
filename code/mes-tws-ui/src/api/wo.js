import request from './request'

export const pageWo = params => request.get('/wo/page', { params })
export const getWo = id => request.get(`/wo/${id}`)
export const createWo = data => request.post('/wo/create', data)
export const transitionWo = (id, action) => request.post(`/wo/${id}/${action}`)
