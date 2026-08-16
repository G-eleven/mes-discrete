import request from './request'

export const pageDefect = params => request.get('/defect/page', { params })
export const repairDefect = data => request.post('/defect/repair', data)
export const manualDefect = data => request.post('/defect/manual', data)
export const repairsOf = sn => request.get('/defect/repairs', { params: { sn } })

export const fpySummary = params => request.get('/fpy/summary', { params })
export const fpyDaily = () => request.get('/fpy/daily')
export const fpyPareto = params => request.get('/fpy/pareto', { params })

export const faiList = params => request.get('/fai/list', { params })
export const faiSave = data => request.post('/fai/save', data)
