<template>
  <div class="login-page">
    <el-card class="login-card">
      <h2 class="t">TWS 整机装配 MES</h2>
      <el-form :model="form" @keyup.enter="doLogin">
        <el-form-item>
          <el-input v-model="form.username" placeholder="用户名" size="large">
            <template #prefix><el-icon><User /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" placeholder="密码" size="large" show-password>
            <template #prefix><el-icon><Lock /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-button type="primary" size="large" style="width:100%" :loading="loading" @click="doLogin">登 录</el-button>
      </el-form>
      <div class="tips">
        <p>admin / planner1 / qc1 / op1，密码均为 123456</p>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../../stores/user'

const router = useRouter()
const userStore = useUserStore()
const form = reactive({ username: 'admin', password: '123456' })
const loading = ref(false)

async function doLogin() {
  loading.value = true
  try {
    await userStore.login(form.username, form.password)
    // 角色存一份供路由守卫/菜单过滤（仅前端显示用，后端仍独立鉴权）
    localStorage.setItem('roleCode', userStore.roleCode)
    ElMessage.success('登录成功')
    router.push('/')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page { height: 100vh; display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, #1f3a5f 0%, #0b1c33 100%); }
.login-card { width: 380px; padding: 8px 12px; }
.t { text-align: center; margin: 4px 0 20px; }
.tips { margin-top: 14px; color: #909399; font-size: 12px; text-align: center; }
</style>
