import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'

/**
 * 路由与菜单。
 * meta.roles 为空 = 所有登录用户可见；router.beforeEach 做登录校验 + 角色过滤。
 */
const routes = [
  { path: '/login', name: 'Login', component: () => import('../views/login/Login.vue'), meta: { public: true } },
  {
    path: '/',
    component: () => import('../layout/AppLayout.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: 'Dashboard', component: () => import('../views/dashboard/Dashboard.vue'), meta: { title: '总览', icon: 'Odometer' } },
      { path: 'base/material', name: 'Material', component: () => import('../views/base/Material.vue'), meta: { title: '物料管理', icon: 'Box', roles: ['admin'] } }
    ]
  }
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach(to => {
  const token = localStorage.getItem('satoken')
  if (!to.meta.public && !token) return { path: '/login' }
  // 角色过滤：菜单可见性控制（后端接口另有 @SaCheckRole 兜底）
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
