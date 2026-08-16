<template>
  <el-card>
    <div class="toolbar">
      <el-input v-model="query.sn" placeholder="SN" clearable style="width:200px" @keyup.enter="load" />
      <el-select v-model="query.status" placeholder="状态" clearable style="width:120px;margin-left:8px">
        <el-option label="待修 OPEN" value="OPEN" /><el-option label="已修复" value="REPAIRED" /><el-option label="报废" value="SCRAP" />
      </el-select>
      <el-select v-model="query.workOrderId" placeholder="工单" clearable filterable style="width:170px;margin-left:8px">
        <el-option v-for="w in wos" :key="w.id" :value="w.id" :label="w.woNo" />
      </el-select>
      <el-button type="primary" style="margin-left:8px" @click="load">查询</el-button>
      <el-button type="warning" plain @click="manualDlg.visible = true">复检开单</el-button>
    </div>

    <el-table :data="rows" v-loading="loading" border stripe>
      <el-table-column prop="defect.id" label="单号" width="70">
        <template #default="{ row }">{{ row.defect.id }}</template>
      </el-table-column>
      <el-table-column prop="woNo" label="工单" width="150" />
      <el-table-column prop="defect.sn" label="SN" min-width="200" show-overflow-tooltip />
      <el-table-column prop="defect.defectCode" label="不良码" width="90">
        <template #default="{ row }">
          <el-tag type="danger" size="small">{{ row.defect.defectCode }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="defect.defectDesc" label="描述" min-width="180" show-overflow-tooltip />
      <el-table-column prop="defect.stationCode" label="发现工位" width="120" />
      <el-table-column label="维修轮次" width="90">
        <template #default="{ row }">第 {{ row.defect.repairRound + 1 }} 轮</template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="statusTag[row.defect.status]">{{ statusMap[row.defect.status] }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="170" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.defect.status === 'OPEN' && canRepair" link type="warning"
                     @click="openRepair(row.defect)">维修处理</el-button>
          <el-button link type="info" @click="showHistory(row.defect.sn)">维修史</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination style="margin-top:12px;justify-content:flex-end" layout="total, prev, pager, next"
      :total="total" v-model:current-page="query.page" v-model:page-size="query.size" @change="load" />

    <!-- 维修对话框 -->
    <el-dialog v-model="repairDlg.visible" :title="`维修处理：${repairDlg.sn}`" width="520px">
      <el-alert type="info" :closable="false" style="margin-bottom:12px"
                title="OK=修好并回流重测（SN 置 RETEST，重测轮次+1，不污染 FPY）；NG=报废" />
      <el-form :model="repairDlg.form" label-width="90px">
        <el-form-item label="维修措施" required>
          <el-input v-model="repairDlg.form.action" type="textarea" :rows="2" placeholder="如：更换左耳整机并重新耦合绑定" />
        </el-form-item>
        <el-form-item label="根因">
          <el-input v-model="repairDlg.form.rootCause" placeholder="如：左耳PCBA问题批次贴装问题麦克风" />
        </el-form-item>
        <el-form-item label="换料批次">
          <el-select v-model="repairDlg.form.changeBatchNo" clearable filterable style="width:100%">
            <el-option v-for="b in batches" :key="b.batchNo" :label="b.batchNo" :value="b.batchNo" />
          </el-select>
        </el-form-item>
        <el-form-item label="结果" required>
          <el-radio-group v-model="repairDlg.form.result">
            <el-radio-button value="OK">OK 修好回流</el-radio-button>
            <el-radio-button value="NG">NG 报废</el-radio-button>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="repairDlg.visible = false">取消</el-button>
        <el-button type="primary" @click="doRepair">提交维修</el-button>
      </template>
    </el-dialog>

    <!-- 维修历史 -->
    <el-dialog v-model="historyDlg.visible" :title="`维修历史：${historyDlg.sn}`" width="640px">
      <el-table :data="historyDlg.rows" border size="small">
        <el-table-column prop="createTime" label="时间" width="160" />
        <el-table-column prop="action" label="措施" min-width="150" />
        <el-table-column prop="rootCause" label="根因" min-width="150" />
        <el-table-column prop="changeBatchNo" label="换料批次" width="120" />
        <el-table-column prop="result" label="结果" width="70" />
        <el-table-column prop="repairer" label="维修人" width="90" />
      </el-table>
    </el-dialog>

    <!-- 复检开单 -->
    <el-dialog v-model="manualDlg.visible" title="复检/抽检开不良单" width="480px">
      <el-form :model="manualDlg.form" label-width="90px">
        <el-form-item label="SN" required><el-input v-model="manualDlg.form.sn" /></el-form-item>
        <el-form-item label="不良代码" required>
          <el-select v-model="manualDlg.form.defectCode" filterable style="width:100%">
            <el-option v-for="d in defectCodes" :key="d.defectCode" :label="`${d.defectCode} ${d.defectName}`" :value="d.defectCode" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述"><el-input v-model="manualDlg.form.desc" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="manualDlg.visible = false">取消</el-button>
        <el-button type="primary" @click="doManual">开单（SN 停线）</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { reactive, ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { pageDefect, repairDefect, manualDefect, repairsOf } from '../../api/quality'
import { pageWo } from '../../api/wo'
import { listDefectCode, listBatch } from '../../api/base'

const statusMap = { OPEN: '待修', REPAIRED: '已修复', SCRAP: '报废' }
const statusTag = { OPEN: 'danger', REPAIRED: 'success', SCRAP: 'info' }
const canRepair = computed(() => ['admin', 'quality'].includes(localStorage.getItem('roleCode') || ''))

const query = reactive({ page: 1, size: 10, sn: '', status: 'OPEN', workOrderId: null })
const rows = ref([])
const total = ref(0)
const loading = ref(false)
const wos = ref([])
const batches = ref([])
const defectCodes = ref([])
const repairDlg = reactive({ visible: false, sn: '', form: {} })
const historyDlg = reactive({ visible: false, sn: '', rows: [] })
const manualDlg = reactive({ visible: false, form: { sn: '', defectCode: undefined, desc: '' } })

async function load() {
  loading.value = true
  try {
    const r = await pageDefect(query)
    rows.value = r.list
    total.value = r.total
  } finally { loading.value = false }
}

function openRepair(defect) {
  repairDlg.sn = defect.sn
  repairDlg.form = { defectId: defect.id, action: '', rootCause: '', changeBatchNo: '', result: 'OK' }
  repairDlg.visible = true
}

async function doRepair() {
  await repairDefect(repairDlg.form)
  ElMessage.success('维修已登记，SN 已' + (repairDlg.form.result === 'OK' ? '回流待重测' : '报废'))
  repairDlg.visible = false
  load()
}

async function showHistory(sn) {
  historyDlg.sn = sn
  historyDlg.rows = await repairsOf(sn)
  historyDlg.visible = true
}

async function doManual() {
  await manualDefect(manualDlg.form)
  ElMessage.success('已开单')
  manualDlg.visible = false
  manualDlg.form = { sn: '', defectCode: undefined, desc: '' }
  load()
}

onMounted(async () => {
  const wp = await pageWo({ size: 50 })
  wos.value = wp.list
  batches.value = await listBatch()
  defectCodes.value = await listDefectCode()
  load()
})
</script>

<style scoped>
.toolbar { display: flex; margin-bottom: 12px; flex-wrap: wrap; }
</style>
