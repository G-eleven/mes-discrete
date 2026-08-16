<template>
  <div>
    <el-row :gutter="16">
      <el-col :span="6" v-for="card in cards" :key="card.label">
        <el-card>
          <div class="stat">
            <div class="num" :style="{ color: card.color }">{{ card.value }}</div>
            <div class="label">{{ card.label }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top:16px">
      <el-col :span="12">
        <el-card>
          <template #header>生产中工单</template>
          <el-table :data="s.woProgress || []" size="small">
            <el-table-column prop="woNo" label="工单" width="150" />
            <el-table-column label="进度" min-width="180">
              <template #default="{ row }">
                <el-progress :percentage="pct(row)" :stroke-width="12" :text-inside="true" />
              </template>
            </el-table-column>
            <el-table-column label="OK/NG/计划" width="130">
              <template #default="{ row }">{{ row.okQty }}/{{ row.ngQty }}/{{ row.planQty }}</template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>最近过站</template>
          <el-table :data="s.recentLogs || []" size="small">
            <el-table-column prop="createTime" label="时间" width="160" />
            <el-table-column prop="sn" label="SN" min-width="170" show-overflow-tooltip />
            <el-table-column prop="stationCode" label="工位" width="110" />
            <el-table-column label="结果" width="70">
              <template #default="{ row }">
                <el-tag :type="row.result === 'OK' ? 'success' : 'danger'" size="small">{{ row.result }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-card style="margin-top:16px">
      <h3>建议体验路线</h3>
      <el-steps :active="4" align-center>
        <el-step title="基础数据" description="工艺路线(V1/V2对比)" />
        <el-step title="工单管理" description="创建→下达→开工" />
        <el-step title="过站模拟器" description="绑定→过站→NG→维修→重测" />
        <el-step title="良率报表" description="FPY 双口径" />
        <el-step title="追溯" description="问题批次反查" />
      </el-steps>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, computed, onMounted } from 'vue'
import request from '../../api/request'

const s = reactive({ inProgressWo: 0, todayCheckin: 0, openDefects: 0, fpy: 0, woProgress: [], recentLogs: [] })

const cards = computed(() => [
  { label: '生产中工单', value: s.inProgressWo, color: '#409eff' },
  { label: '今日过站次数', value: s.todayCheckin, color: '#67c23a' },
  { label: '待修不良单', value: s.openDefects, color: '#f56c6c' },
  { label: '综合 FPY 一次直通率', value: (s.fpy ?? 0) + '%', color: '#e6a23c' }
])

const pct = row => row.planQty ? Math.min(100, Math.round(row.okQty / row.planQty * 100)) : 0

onMounted(async () => {
  Object.assign(s, await request.get('/dashboard/summary'))
})
</script>

<style scoped>
.stat { text-align: center; padding: 8px 0; }
.num { font-size: 28px; font-weight: 700; }
.label { color: #909399; margin-top: 6px; }
</style>
