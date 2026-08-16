<template>
  <div>
    <el-card>
      <div class="toolbar">
        <el-select v-model="batchNo" filterable allow-create default-first-option placeholder="选择或输入批次号（试试问题批次 PCLB240801）" style="width:360px">
          <el-option v-for="b in batches" :key="b.batchNo" :label="`${b.batchNo}（${b.supplier || ''}）`" :value="b.batchNo" />
        </el-select>
        <el-button type="primary" @click="doTrace">反向追溯</el-button>
        <span v-if="data" class="cost">耗时 {{ data.costMs }} ms</span>
      </div>
      <el-alert v-if="data" :type="data.batch.status === 0 ? 'warning' : 'success'" :closable="false" style="margin-top:10px"
                :title="`批次 ${data.batch.batchNo} / ${data.batch.supplier || '-'} / ${data.batch.status === 0 ? '已冻结' : '可用'}：共 ${data.total} 个部件 SN，已装配 ${data.machines.length} 台整机，${data.unbound} 个部件未装配（含拦截停线）`" />
    </el-card>

    <el-card v-if="data" style="margin-top:14px">
      <template #header>受影响整机清单（客户审厂追溯报告的核心表）</template>
      <el-table :data="data.machines" border stripe>
        <el-table-column prop="partSn" label="部件 SN" min-width="200" show-overflow-tooltip />
        <el-table-column label="部件类型" width="90">
          <template #default="{ row }">{{ snTypeMap[row.partType] }}</template>
        </el-table-column>
        <el-table-column label="整机 SN" min-width="180">
          <template #default="{ row }">
            <el-link type="primary" @click="goForward(row.machine.sn)">{{ row.machine.sn }}</el-link>
          </template>
        </el-table-column>
        <el-table-column prop="woNo" label="工单" width="150" />
        <el-table-column label="整机状态" width="100">
          <template #default="{ row }">
            <el-tag :type="snTag[row.machine.status]" size="small">{{ snStatusMap[row.machine.status] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="整机不良数" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.defectCnt > 0" type="danger" size="small">{{ row.defectCnt }}</el-tag>
            <span v-else>0</span>
          </template>
        </el-table-column>
        <el-table-column label="固件" width="80">
          <template #default="{ row }">{{ row.machine.firmwareVersion || '-' }}</template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { traceReverse } from '../../api/trace'
import { listBatch } from '../../api/base'

const router = useRouter()
const batches = ref([])
const batchNo = ref('')
const data = ref(null)
const snTypeMap = { MACHINE: '整机', LEFT: '左耳', RIGHT: '右耳', CASE: '充电盒' }
const snStatusMap = { INIT: '已注册', IN_LINE: '在制', NG: '不良待修', RETEST: '待重测', DONE: '完工', SCRAP: '报废' }
const snTag = { INIT: 'info', IN_LINE: '', NG: 'danger', RETEST: 'warning', DONE: 'success', SCRAP: 'danger' }

function goForward(sn) {
  router.push({ path: '/trace/forward', query: { sn } })
}

async function doTrace() {
  if (!batchNo.value) return
  data.value = await traceReverse(batchNo.value.trim())
  ElMessage.success(`追溯完成：命中 ${data.value.machines.length} 台整机`)
}

onMounted(async () => {
  batches.value = await listBatch()
})
</script>

<style scoped>
.toolbar { display: flex; gap: 10px; align-items: center; }
.cost { color: #909399; font-size: 13px; }
</style>
