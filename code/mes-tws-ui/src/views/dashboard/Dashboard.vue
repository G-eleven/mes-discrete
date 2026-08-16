<template>
  <div>
    <el-row :gutter="16">
      <el-col :span="6" v-for="card in cards" :key="card.label">
        <el-card>
          <div class="stat">
            <div class="num">{{ card.value }}</div>
            <div class="label">{{ card.label }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>
    <el-card style="margin-top:16px">
      <h3>欢迎使用</h3>
      <p>这是 TWS 整机装配 MES 学习版。左侧菜单按模块逐步开放：基础数据 → 工单管理 → 生产执行（过站模拟器）→ 质量管理 → 追溯管理。</p>
      <p>种子数据已内置两张工单：WO20260801001（已完工，含历史不良与维修记录）、WO20260816002（生产中，含 2 条待维修不良）。</p>
    </el-card>
  </div>
</template>

<script setup>
// M1 占位总览页，M3 后接入真实统计接口
const cards = [
  { label: '功能建设中', value: '—' },
  { label: '功能建设中', value: '—' },
  { label: '功能建设中', value: '—' },
  { label: '功能建设中', value: '—' }
]
</script>

<style scoped>
.stat { text-align: center; padding: 8px 0; }
.num { font-size: 28px; font-weight: 700; color: #409eff; }
.label { color: #909399; margin-top: 6px; }
</style>
