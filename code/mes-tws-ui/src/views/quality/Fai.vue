<template>
  <el-card>
    <div class="toolbar">
      <span class="tip">首件检验 FAI：换线/换批次后首件确认，PASS 才允许批量过站</span>
      <el-button type="primary" plain @click="dlg.visible = true">登记首件</el-button>
    </div>
    <el-table :data="rows" border stripe>
      <el-table-column prop="createTime" label="时间" width="170" />
      <el-table-column label="工单" width="150">
        <template #default="{ row }">{{ woNo(row.workOrderId) }}</template>
      </el-table-column>
      <el-table-column prop="operationCode" label="工序" width="140" />
      <el-table-column prop="sn" label="首件 SN" min-width="180" />
      <el-table-column label="结果" width="90">
        <template #default="{ row }">
          <el-tag :type="row.result === 'PASS' ? 'success' : 'danger'">{{ row.result }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="checker" label="检验员" width="100" />
      <el-table-column prop="remark" label="备注" min-width="140" />
    </el-table>

    <el-dialog v-model="dlg.visible" title="登记首件检验" width="460px">
      <el-form :model="dlg.form" label-width="80px">
        <el-form-item label="工单">
          <el-select v-model="dlg.form.workOrderId" filterable style="width:100%">
            <el-option v-for="w in wos" :key="w.id" :value="w.id" :label="w.woNo" />
          </el-select>
        </el-form-item>
        <el-form-item label="工序">
          <el-select v-model="dlg.form.operationCode" filterable clearable style="width:100%">
            <el-option v-for="o in operations" :key="o.operationCode" :label="`${o.operationCode} ${o.operationName}`" :value="o.operationCode" />
          </el-select>
        </el-form-item>
        <el-form-item label="首件 SN"><el-input v-model="dlg.form.sn" /></el-form-item>
        <el-form-item label="结果">
          <el-radio-group v-model="dlg.form.result">
            <el-radio-button value="PASS">PASS</el-radio-button>
            <el-radio-button value="FAIL">FAIL</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="dlg.form.remark" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg.visible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { faiList, faiSave } from '../../api/quality'
import { pageWo } from '../../api/wo'
import { listOperation } from '../../api/base'

const rows = ref([])
const wos = ref([])
const operations = ref([])
const dlg = reactive({ visible: false, form: { result: 'PASS' } })
const woNo = id => (wos.value.find(w => w.id === id) || {}).woNo || id

async function load() { rows.value = await faiList({}) }
async function save() {
  await faiSave(dlg.form)
  ElMessage.success('已登记')
  dlg.visible = false
  dlg.form = { result: 'PASS' }
  load()
}
onMounted(async () => {
  const wp = await pageWo({ size: 50 })
  wos.value = wp.list
  operations.value = await listOperation()
  load()
})
</script>

<style scoped>
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.tip { color: #909399; font-size: 13px; }
</style>
