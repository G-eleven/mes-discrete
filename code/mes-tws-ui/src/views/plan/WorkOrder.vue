<template>
  <el-card>
    <div class="toolbar">
      <el-input v-model="query.keyword" placeholder="工单号" clearable style="width:200px" @keyup.enter="load" />
      <el-select v-model="query.status" placeholder="状态" clearable style="width:150px;margin-left:8px">
        <el-option v-for="(label, value) in statusMap" :key="value" :label="label" :value="value" />
      </el-select>
      <el-button type="primary" style="margin-left:8px" @click="load">查询</el-button>
      <el-button type="primary" plain :disabled="!canCreate" @click="openCreate">创建工单</el-button>
    </div>

    <el-table :data="rows" v-loading="loading" border stripe>
      <el-table-column prop="woNo" label="工单号" width="150">
        <template #default="{ row }">
          <el-link type="primary" @click="$router.push(`/plan/wo/${row.id}`)">{{ row.woNo }}</el-link>
        </template>
      </el-table-column>
      <el-table-column prop="productMaterialName" label="成品" min-width="150" />
      <el-table-column label="路线" width="200">
        <template #default="{ row }">{{ row.routingName }} V{{ row.routingVersion }}</template>
      </el-table-column>
      <el-table-column label="计划/OK/NG" width="130">
        <template #default="{ row }">{{ row.planQty }} / {{ row.okQty }} / {{ row.ngQty }}</template>
      </el-table-column>
      <el-table-column label="进度" width="160">
        <template #default="{ row }">
          <el-progress :percentage="progressOf(row)" :stroke-width="12" />
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusTag[row.status]">{{ statusMap[row.status] }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="planStartDate" label="计划起止" width="200">
        <template #default="{ row }">{{ row.planStartDate || '-' }} ~ {{ row.planEndDate || '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <template v-for="act in actionsOf(row)" :key="act.key">
            <el-button link :type="act.type" @click="doTransition(row, act.key)">{{ act.label }}</el-button>
          </template>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination style="margin-top:12px;justify-content:flex-end" layout="total, prev, pager, next"
      :total="total" v-model:current-page="query.page" v-model:page-size="query.size" @change="load" />

    <el-dialog v-model="createDlg.visible" title="创建工单" width="560px">
      <el-form :model="createDlg.form" label-width="100px">
        <el-form-item label="成品物料" required>
          <el-select v-model="createDlg.form.productMaterialId" filterable style="width:100%"
                     @change="onProductChange">
            <el-option v-for="m in products" :key="m.id" :label="`${m.materialCode} ${m.materialName}`" :value="m.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="BOM">
          <el-select v-model="createDlg.form.bomId" clearable style="width:100%">
            <el-option v-for="b in boms" :key="b.id" :label="`${b.bomCode} ${b.version}`" :value="b.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="工艺路线" required>
          <el-select v-model="createDlg.form.routingId" style="width:100%">
            <el-option v-for="r in routings" :key="r.id" :label="`${r.routingName} V${r.version}`" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="计划数量" required>
          <el-input-number v-model="createDlg.form.planQty" :min="1" :max="99999" />
        </el-form-item>
        <el-form-item label="计划起止">
          <el-date-picker v-model="dateRange" type="daterange" value-format="YYYY-MM-DD"
                          start-placeholder="开始" end-placeholder="结束" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDlg.visible = false">取消</el-button>
        <el-button type="primary" @click="create">创建（生成工序快照）</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { reactive, ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageWo, createWo, transitionWo } from '../../api/wo'
import { releasedRouting } from '../../api/base'
import { listMaterial } from '../../api/material'
import { pageBom } from '../../api/base'

const statusMap = {
  CREATED: '已创建', RELEASED: '已下达', IN_PROGRESS: '生产中', PAUSED: '已暂停', COMPLETED: '已完工', CLOSED: '已关闭'
}
const statusTag = { CREATED: 'info', RELEASED: '', IN_PROGRESS: 'success', PAUSED: 'warning', COMPLETED: 'success', CLOSED: 'info' }

const query = reactive({ page: 1, size: 10, keyword: '', status: '' })
const rows = ref([])
const total = ref(0)
const loading = ref(false)
const role = localStorage.getItem('roleCode') || ''
const canCreate = computed(() => ['admin', 'planner'].includes(role))

const products = ref([])
const boms = ref([])
const routings = ref([])
const dateRange = ref(null)
const createDlg = reactive({ visible: false, form: {} })

const progressOf = row => row.planQty ? Math.min(100, Math.round(row.okQty / row.planQty * 100)) : 0

function actionsOf(row) {
  const map = {
    CREATED: [{ key: 'release', label: '下达', type: 'primary' }],
    RELEASED: [{ key: 'start', label: '开工', type: 'success' }],
    IN_PROGRESS: [{ key: 'pause', label: '暂停', type: 'warning' }, { key: 'complete', label: '完工', type: 'success' }],
    PAUSED: [{ key: 'resume', label: '恢复', type: 'success' }],
    COMPLETED: [{ key: 'close', label: '关闭', type: 'info' }],
    CLOSED: []
  }
  return canCreate.value ? (map[row.status] || []) : []
}

async function load() {
  loading.value = true
  try {
    const r = await pageWo(query)
    rows.value = r.list
    total.value = r.total
  } finally { loading.value = false }
}

async function openCreate() {
  createDlg.form = { productMaterialId: undefined, bomId: undefined, routingId: undefined, planQty: 500 }
  createDlg.visible = true
}

async function onProductChange(pid) {
  createDlg.form.routingId = undefined
  createDlg.form.bomId = undefined
  if (!pid) { routings.value = []; boms.value = []; return }
  routings.value = await releasedRouting({ productMaterialId: pid })
  const bp = await pageBom({ size: 50 })
  boms.value = bp.list.filter(b => b.productMaterialId === pid && b.status === 1)
}

async function create() {
  const f = createDlg.form
  await createWo({
    ...f,
    planStartDate: dateRange.value?.[0] || null,
    planEndDate: dateRange.value?.[1] || null
  })
  ElMessage.success('工单已创建，工艺路线已快照')
  createDlg.visible = false
  load()
}

async function doTransition(row, action) {
  const labels = { release: '下达', start: '开工', pause: '暂停', resume: '恢复', complete: '完工', close: '关闭' }
  await ElMessageBox.confirm(`确认对工单 ${row.woNo} 执行【${labels[action]}】？`, '操作确认')
  await transitionWo(row.id, action)
  ElMessage.success('操作成功')
  load()
}

onMounted(async () => {
  products.value = await listMaterial({ materialType: 'PRODUCT' })
  load()
})
</script>

<style scoped>
.toolbar { display: flex; margin-bottom: 12px; }
</style>
