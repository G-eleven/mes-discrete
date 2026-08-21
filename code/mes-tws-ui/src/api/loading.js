import request from './request'

/* 工位上料：齐套计算 / 看板 / 台账 / 上料防错 */
export const kitting = params => request.get('/loading/kitting', { params })
export const loadingBoard = params => request.get('/loading/board', { params })
export const loadingLedger = params => request.get('/loading/ledger', { params })
export const doLoading = data => request.post('/station/loading', data)
