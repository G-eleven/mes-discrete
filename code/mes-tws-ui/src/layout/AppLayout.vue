<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="logo">TWS 装配 MES</div>
      <el-menu :default-active="$route.path" router background-color="#001529"
               text-color="#a6adb4" active-text-color="#ffffff" :unique-opened="true">
        <!-- 无 group 的路由：一级菜单 -->
        <el-menu-item v-for="item in topMenus" :key="item.path" :index="'/' + item.path">
          <el-icon><component :is="item.meta.icon" /></el-icon>
          <span>{{ item.meta.title }}</span>
        </el-menu-item>
        <!-- 有 group 的路由：折叠为二级菜单 -->
        <el-sub-menu v-for="(items, group) in groupedMenus" :key="group" :index="group">
          <template #title>
            <el-icon><Folder /></el-icon><span>{{ group }}</span>
          </template>
          <el-menu-item v-for="item in items" :key="item.path" :index="'/' + item.path">
            <el-icon><component :is="item.meta.icon" /></el-icon>
            <span>{{ item.meta.title }}</span>
          </el-menu-item>
        </el-sub-menu>
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
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'

const router = useRouter()
const userStore = useUserStore()

// 可见路由 = 未隐藏 + （无角色限制 或 角色匹配）
const visible = computed(() => {
  const root = router.options.routes.find(r => r.path === '/')
  const role = userStore.roleCode || localStorage.getItem('roleCode') || ''
  return (root?.children || []).filter(r =>
    !r.meta?.hidden && (!r.meta?.roles || r.meta.roles.includes(role)))
})

const topMenus = computed(() => visible.value.filter(r => !r.meta?.group))
// 按 group 归类
const groupedMenus = computed(() => {
  const map = {}
  visible.value.filter(r => r.meta?.group).forEach(r => {
    (map[r.meta.group] = map[r.meta.group] || []).push(r)
  })
  return map
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
