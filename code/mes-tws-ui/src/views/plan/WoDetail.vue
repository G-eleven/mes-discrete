<template>
  <div v-if="wo">
    <el-card>
      <template #header>
        <div class="card-head">
          <span>工单 {{ wo.woNo }}
            <el-tag :type="statusTag[wo.status]" style="margin-left:10px">{{ statusMap[wo.status] }}</el-tag>
          </span>
          <el-button @click="$router.back()">返回</el-button>
        </div>
      </template>
      <el-descriptions :column="3" border>
        <el-descriptions-item label="成品">{{ vo?.productMaterialCode }} {{ vo?.productMaterialName }}</el-descriptions-item>
        <el-descriptions-item label="工艺路线（快照版本）">{{ vo?.routingName }} V{{ wo.routingVersion }}</el-descriptions-item>
        <el-descriptions-item label="计划数量">{{ wo.planQty }}</el-descriptions-item>
        <el-descriptions-item label="过站 OK">{{ wo.okQty }}</el-descriptions-item>
        <el-descriptions-item label="过站 NG">{{ wo.ngQty }}</el-descriptions-item>
        <el-descriptions-item label="创建人">{{ wo.createBy }}</el-descriptions-item>
        <el-descriptions-item label="计划起止">{{ wo.planStartDate || '-' }} ~ {{ wo.planEndDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="乐观锁版本">v{{ wo.version }}</el-descriptions-item>
        <el-descriptions-item label="状态说明">创建即快照，路线升版不影响本单</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card style="margin-top:14px">
      <template #header>工序快照（plan_wo_operation，创建时从路线复制）</template>
      <el-table :data="operations" border stripe>
        <el-table-column prop="seq" label="顺序" width="80" sortable />
        <el-table-column prop="operationCode" label="工序编码" width="140" />
        <el-table-column prop="operationName" label="工序名称" min-width="180" />
        <el-table-column label="类型" width="90">
          <template #default="{ row }">
            <el-tag size="small">{{ typeMap[row.operationType] || row.operationType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="checkRules" label="防呆规则（快照）" min-width="320">
          <template #default="{ row }">
            <code class="rules">{{ pretty(row.checkRules) }}</code>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { pageWo, getWo } from '../../api/wo'

const route = useRoute()
const wo = ref(null)
const vo = ref(null)
const operations = ref([])
const statusMap = { CREATED: '已创建', RELEASED: '已下达', IN_PROGRESS: '生产中', PAUSED: '已暂停', COMPLETED: '已完工', CLOSED: '已关闭' }
const statusTag = { CREATED: 'info', RELEASED: '', IN_PROGRESS: 'success', PAUSED: 'warning', COMPLETED: 'success', CLOSED: 'info' }
const typeMap = { NORMAL: '普通', TEST: '测试', BIND: '绑定', AGING: '老化', PACK: '包装', IQC: '检验' }

const pretty = s => {
  if (!s) return '-'
  try { return JSON.stringify(JSON.parse(s)) } catch (e) { return s }
}

onMounted(async () => {
  const d = await getWo(route.params.id)
  wo.value = d.wo
  operations.value = d.operations
  // 列表接口取关联名称（物料/路线）
  const p = await pageWo({ keyword: d.wo.woNo, size: 1 })
  vo.value = p.list[0] || null
})
</script>

<style scoped>
.card-head { display: flex; justify-content: space-between; align-items: center; }
.rules { font-size: 12px; color: #67c23a; word-break: break-all; }
</style>
