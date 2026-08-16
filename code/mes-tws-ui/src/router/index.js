import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'

/**
 * 路由与菜单：meta.group 用于侧边栏分组（el-sub-menu）；
 * meta.roles 为空 = 所有登录用户可见。前端菜单过滤只是体验优化，
 * 真正的权限由后端 @SaCheckRole 独立校验。
 */
const routes = [
  { path: '/login', name: 'Login', component: () => import('../views/login/Login.vue'), meta: { public: true } },
  {
    path: '/',
    component: () => import('../layout/AppLayout.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: 'Dashboard', component: () => import('../views/dashboard/Dashboard.vue'), meta: { title: '总览', icon: 'Odometer' } },

      { path: 'plan/wo', name: 'WorkOrder', component: () => import('../views/plan/WorkOrder.vue'), meta: { title: '工单管理', icon: 'Tickets', group: '计划管理' } },
      { path: 'plan/wo/:id', name: 'WoDetail', component: () => import('../views/plan/WoDetail.vue'), meta: { title: '工单详情', icon: 'Tickets', group: '计划管理', hidden: true } },

      { path: 'exec/simulator', name: 'Simulator', component: () => import('../views/exec/Simulator.vue'), meta: { title: '过站模拟器', icon: 'Monitor', group: '生产执行' } },
      { path: 'exec/sn', name: 'SnList', component: () => import('../views/exec/SnList.vue'), meta: { title: 'SN 管理', icon: 'Barcode', group: '生产执行' } },
      { path: 'exec/log', name: 'StationLog', component: () => import('../views/exec/StationLog.vue'), meta: { title: '过站流水', icon: 'DocumentCopy', group: '生产执行' } },

      { path: 'quality/defect', name: 'DefectList', component: () => import('../views/quality/DefectList.vue'), meta: { title: '不良与维修', icon: 'WarningFilled', group: '质量管理' } },
      { path: 'quality/fpy', name: 'FpyReport', component: () => import('../views/quality/FpyReport.vue'), meta: { title: '良率报表', icon: 'DataAnalysis', group: '质量管理' } },
      { path: 'quality/fai', name: 'Fai', component: () => import('../views/quality/Fai.vue'), meta: { title: '首件检验', icon: 'CircleCheck', group: '质量管理' } },

      { path: 'trace/forward', name: 'TraceForward', component: () => import('../views/trace/TraceForward.vue'), meta: { title: '正向追溯', icon: 'Search', group: '追溯管理' } },
      { path: 'trace/reverse', name: 'TraceReverse', component: () => import('../views/trace/TraceReverse.vue'), meta: { title: '反向追溯', icon: 'Switch', group: '追溯管理' } },

      { path: 'base/material', name: 'Material', component: () => import('../views/base/Material.vue'), meta: { title: '物料管理', icon: 'Box', roles: ['admin'], group: '基础数据' } },
      { path: 'base/material-batch', name: 'MaterialBatch', component: () => import('../views/base/MaterialBatch.vue'), meta: { title: '物料批次', icon: 'Files', roles: ['admin', 'quality'], group: '基础数据' } },
      { path: 'base/bom', name: 'Bom', component: () => import('../views/base/Bom.vue'), meta: { title: 'BOM 管理', icon: 'SetUp', roles: ['admin'], group: '基础数据' } },
      { path: 'base/routing', name: 'Routing', component: () => import('../views/base/Routing.vue'), meta: { title: '工艺路线', icon: 'Guide', roles: ['admin'], group: '基础数据' } },
      { path: 'base/operation', name: 'Operation', component: () => import('../views/base/Operation.vue'), meta: { title: '工序定义', icon: 'List', roles: ['admin'], group: '基础数据' } },
      { path: 'base/line-station', name: 'LineStation', component: () => import('../views/base/LineStation.vue'), meta: { title: '产线工位', icon: 'Coordinate', roles: ['admin'], group: '基础数据' } },
      { path: 'base/defect-code', name: 'DefectCode', component: () => import('../views/base/DefectCode.vue'), meta: { title: '不良代码', icon: 'Warning', roles: ['admin', 'quality'], group: '基础数据' } }
    ]
  }
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach(to => {
  const token = localStorage.getItem('satoken')
  if (!to.meta.public && !token) return { path: '/login' }
  if (to.meta.roles && to.meta.roles.length && token) {
    const role = localStorage.getItem('roleCode') || ''
    if (!to.meta.roles.includes(role)) {
      ElMessage.warning('当前角色无权访问该页面')
      return { path: '/dashboard' }
    }
  }
  return true
})

export default router
