import request from './request'

/* 基础数据通用接口：批次/工序/产线/工位/不良代码 */
export const pageBatch = params => request.get('/material-batch/page', { params })
export const listBatch = params => request.get('/material-batch/list', { params })
export const saveBatch = data => request.post('/material-batch/save', data)

export const listOperation = () => request.get('/operation/list')
export const saveOperation = data => request.post('/operation/save', data)

export const listLine = () => request.get('/line/list')
export const saveLine = data => request.post('/line/save', data)
export const listStation = params => request.get('/station/list', { params })
export const saveStation = data => request.post('/station/save', data)

export const listDefectCode = () => request.get('/defect-code/list')
export const saveDefectCode = data => request.post('/defect-code/save', data)

/* BOM */
export const pageBom = params => request.get('/bom/page', { params })
export const getBom = id => request.get(`/bom/${id}`)
export const saveBom = data => request.post('/bom/save', data)
export const deleteBom = id => request.delete(`/bom/${id}`)

/* 工艺路线 */
export const pageRouting = params => request.get('/routing/page', { params })
export const releasedRouting = params => request.get('/routing/released', { params })
export const getRouting = id => request.get(`/routing/${id}`)
export const saveRouting = data => request.post('/routing/save', data)
export const publishRouting = id => request.post(`/routing/${id}/publish`)
export const deleteRouting = id => request.delete(`/routing/${id}`)
