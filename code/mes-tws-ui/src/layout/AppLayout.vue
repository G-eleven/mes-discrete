<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="logo">TWS 装配 MES</div>
      <el-menu :default-active="$route.path" router background-color="#001529"
               text-color="#a6adb4" active-text-color="#ffffff">
        <el-menu-item v-for="item in menus" :key="item.path" :index="'/' + item.path">
          <el-icon><component :is="item.meta.icon" /></el-icon>
          <span>{{ item.meta.title }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div class="title">{{ $route.meta.title }}</div>
        <el-dropdown @command="onCommand">
          <span class="user">
            <el-icon><User /></el-icon>
            {{ userStore.nickName || userStore.username }}
            <el-tag size="small" style="margin-left:6px">{{ roleLabel }}</el-tag>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 从路由表第一层 children 生成菜单（学习版菜单写死在前端，按角色过滤）
const menus = computed(() => {
  const root = router.options.routes.find(r => r.path === '/')
  const role = userStore.roleCode || localStorage.getItem('roleCode') || ''
  return (root?.children || []).filter(r => !r.meta?.roles || r.meta.roles.includes(role))
})

const roleLabel = computed(() => ({
  admin: '管理员', planner: '计划员', quality: '质量员', operator: '操作工'
}[userStore.roleCode || localStorage.getItem('roleCode') || ''] || ''))

async function onCommand(cmd) {
  if (cmd === 'logout') {
    await userStore.logout()
    router.push('/login')
  }
}
</script>

<style scoped>
.layout { height: 100vh; }
.aside { background: #001529; }
.logo { height: 56px; line-height: 56px; color: #fff; font-weight: 600; text-align: center; letter-spacing: 2px; }
.aside .el-menu { border-right: none; }
.header { display: flex; align-items: center; justify-content: space-between; border-bottom: 1px solid #e4e7ed; background: #fff; }
.title { font-size: 16px; font-weight: 600; }
.user { display: flex; align-items: center; cursor: pointer; gap: 4px; }
.main { background: #f5f7fa; }
</style>
