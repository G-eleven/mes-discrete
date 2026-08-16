import request from './request'

export const pageMaterial = params => request.get('/material/page', { params })
export const listMaterial = params => request.get('/material/list', { params })
export const saveMaterial = data => request.post('/material/save', data)
export const deleteMaterial = id => request.delete(`/material/${id}`)
