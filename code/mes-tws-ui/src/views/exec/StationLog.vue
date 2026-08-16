<template>
  <el-card>
    <div class="toolbar">
      <el-input v-model="query.sn" placeholder="SN" clearable style="width:200px" @keyup.enter="load" />
      <el-select v-model="query.recordType" placeholder="类型" clearable style="width:120px;margin-left:8px">
        <el-option label="过站" value="CHECKIN" /><el-option label="上料" value="LOADING" /><el-option label="绑定" value="BINDING" />
      </el-select>
      <el-select v-model="query.stationCode" placeholder="工位" clearable filterable style="width:160px;margin-left:8px">
        <el-option v-for="s in stations" :key="s.stationCode" :label="s.stationCode" :value="s.stationCode" />
      </el-select>
      <el-select v-model="query.workOrderId" placeholder="工单" clearable filterable style="width:170px;margin-left:8px">
        <el-option v-for="w in wos" :key="w.id" :value="w.id" :label="w.woNo" />
      </el-select>
      <el-button type="primary" style="margin-left:8px" @click="load">查询</el-button>
    </div>

    <el-table :data="rows" v-loading="loading" border stripe>
      <el-table-column prop="createTime" label="时间" width="170" />
      <el-table-column prop="sn" label="SN" min-width="190" show-overflow-tooltip />
      <el-table-column label="类型" width="80">
        <template #default="{ row }">{{ typeMap[row.recordType] }}</template>
      </el-table-column>
      <el-table-column prop="stationCode" label="工位" width="120" />
      <el-table-column prop="operationCode" label="工序" width="130" />
      <el-table-column label="结果" width="70">
        <template #default="{ row }">
          <el-tag v-if="row.result" :type="row.result === 'OK' ? 'success' : 'danger'" size="small">{{ row.result }}</el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="ngCode" label="不良码" width="80" />
      <el-table-column label="轮次" width="70">
        <template #default="{ row }">{{ row.retestRound > 0 ? '重测' + row.retestRound : '首过' }}</template>
      </el-table-column>
      <el-table-column prop="batchNo" label="批次" width="130" />
      <el-table-column label="测试数据" min-width="220" show-overflow-tooltip>
        <template #default="{ row }">{{ pretty(row.testData) }}</template>
      </el-table-column>
      <el-table-column prop="operator" label="操作工" width="90" />
    </el-table>
    <el-pagination style="margin-top:12px;justify-content:flex-end" layout="total, prev, pager, next, sizes"
      :total="total" v-model:current-page="query.page" v-model:page-size="query.size"
      :page-sizes="[10, 20, 50]" @change="load" />
  </el-card>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { pageLog } from '../../api/exec'
import { pageWo } from '../../api/wo'
import { listStation } from '../../api/base'

const typeMap = { CHECKIN: '过站', LOADING: '上料', BINDING: '绑定' }
const query = reactive({ page: 1, size: 20, sn: '', recordType: '', stationCode: '', workOrderId: null })
const rows = ref([])
const total = ref(0)
const loading = ref(false)
const stations = ref([])
const wos = ref([])

const pretty = s => {
  if (!s) return '-'
  try { return JSON.stringify(JSON.parse(s)) } catch (e) { return s }
}

async function load() {
  loading.value = true
  try {
    const r = await pageLog(query)
    rows.value = r.list
    total.value = r.total
  } finally { loading.value = false }
}

onMounted(async () => {
  stations.value = await listStation()
  const wp = await pageWo({ size: 50 })
  wos.value = wp.list
  load()
})
</script>

<style scoped>
.toolbar { display: flex; margin-bottom: 12px; flex-wrap: wrap; }
</style>
