<template>
  <el-card>
    <div class="toolbar">
      <el-input v-model="query.keyword" placeholder="SN" clearable style="width:180px" @keyup.enter="load" />
      <el-select v-model="query.snType" placeholder="类型" clearable style="width:110px;margin-left:8px">
        <el-option v-for="(label, value) in snTypeMap" :key="value" :label="label" :value="value" />
      </el-select>
      <el-select v-model="query.status" placeholder="状态" clearable style="width:110px;margin-left:8px">
        <el-option v-for="(label, value) in statusMap" :key="value" :label="label" :value="value" />
      </el-select>
      <el-select v-model="query.workOrderId" placeholder="工单" clearable filterable style="width:180px;margin-left:8px">
        <el-option v-for="w in wos" :key="w.id" :value="w.id" :label="w.woNo" />
      </el-select>
      <el-button type="primary" style="margin-left:8px" @click="load">查询</el-button>
      <el-button type="primary" plain @click="genDlg.visible = true">生成 SN</el-button>
    </div>

    <el-table :data="rows" v-loading="loading" border stripe>
      <el-table-column prop="sn" label="SN" min-width="220" show-overflow-tooltip />
      <el-table-column label="类型" width="90">
        <template #default="{ row }">{{ snTypeMap[row.snType] || row.snType }}</template>
      </el-table-column>
      <el-table-column label="工单" width="150">
        <template #default="{ row }">{{ woNo(row.workOrderId) }}</template>
      </el-table-column>
      <el-table-column prop="batchNo" label="批次" width="140" />
      <el-table-column prop="parentSn" label="父SN" min-width="200" show-overflow-tooltip />
      <el-table-column prop="currentSeq" label="进度seq" width="85" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusTag[row.status]">{{ statusMap[row.status] }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="firmwareVersion" label="固件" width="80" />
    </el-table>
    <el-pagination style="margin-top:12px;justify-content:flex-end" layout="total, prev, pager, next, sizes"
      :total="total" v-model:current-page="query.page" v-model:page-size="query.size"
      :page-sizes="[10, 20, 50]" @change="load" />

    <el-dialog v-model="genDlg.visible" title="生成 / 注册 SN" width="520px">
      <el-tabs>
        <el-tab-pane label="整机 SN（按工单）">
          <el-form label-width="90px">
            <el-form-item label="工单">
              <el-select v-model="genDlg.woId" filterable style="width:100%">
                <el-option v-for="w in wos.filter(x => !x.snGenerated)" :key="w.id" :value="w.id"
                           :label="`${w.woNo}（计划 ${w.planQty}）`" />
              </el-select>
            </el-form-item>
            <el-button type="primary" @click="genMachine">生成（数量=计划数量，一次性）</el-button>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="部件 SN（来料注册）">
          <el-form label-width="90px">
            <el-form-item label="部件类型">
              <el-select v-model="genDlg.snType" style="width:150px">
                <el-option value="LEFT" label="左耳" /><el-option value="RIGHT" label="右耳" /><el-option value="CASE" label="充电盒" />
              </el-select>
            </el-form-item>
            <el-form-item label="来料批次">
              <el-select v-model="genDlg.batchNo" filterable clearable style="width:220px">
                <el-option v-for="b in batches" :key="b.batchNo" :label="b.batchNo" :value="b.batchNo" />
              </el-select>
            </el-form-item>
            <el-form-item label="数量">
              <el-input-number v-model="genDlg.count" :min="1" :max="5000" />
            </el-form-item>
            <el-button type="primary" @click="genComponent">注册</el-button>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { pageSn, generateMachine, registerComponent } from '../../api/exec'
import { pageWo } from '../../api/wo'
import { listBatch } from '../../api/base'

const snTypeMap = { MACHINE: '整机', LEFT: '左耳', RIGHT: '右耳', CASE: '充电盒', BOX: '彩盒', CARTON: '中箱' }
const statusMap = { INIT: '已注册', IN_LINE: '在制', NG: '不良待修', RETEST: '待重测', DONE: '完工', SCRAP: '报废' }
const statusTag = { INIT: 'info', IN_LINE: '', NG: 'danger', RETEST: 'warning', DONE: 'success', SCRAP: 'danger' }

const query = reactive({ page: 1, size: 10, keyword: '', snType: '', status: '', workOrderId: null })
const rows = ref([])
const total = ref(0)
const loading = ref(false)
const wos = ref([])
const batches = ref([])
const genDlg = reactive({ visible: false, woId: null, snType: 'LEFT', batchNo: '', count: 50 })

const woNo = id => (wos.value.find(w => w.id === id) || {}).woNo || id || '-'

async function load() {
  loading.value = true
  try {
    const r = await pageSn(query)
    rows.value = r.list
    total.value = r.total
  } finally { loading.value = false }
}

async function genMachine() {
  if (!genDlg.woId) return
  const n = await generateMachine(genDlg.woId)
  ElMessage.success(`已生成 ${n} 个整机 SN`)
  genDlg.visible = false
  load()
}

async function genComponent() {
  const n = await registerComponent({ snType: genDlg.snType, batchNo: genDlg.batchNo, count: genDlg.count })
  ElMessage.success(`已注册 ${n} 个部件 SN`)
  genDlg.visible = false
  load()
}

onMounted(async () => {
  const wp = await pageWo({ size: 50 })
  wos.value = wp.list
  batches.value = await listBatch()
  load()
})
</script>

<style scoped>
.toolbar { display: flex; margin-bottom: 12px; flex-wrap: wrap; gap: 4px; }
</style>
