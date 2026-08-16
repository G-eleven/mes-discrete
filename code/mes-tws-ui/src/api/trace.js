import request from './request'

export const traceForward = sn => request.get(`/trace/forward/${sn}`)
export const traceReverse = batchNo => request.get(`/trace/reverse/${batchNo}`)
export const traceTasks = () => request.get('/trace/tasks')
