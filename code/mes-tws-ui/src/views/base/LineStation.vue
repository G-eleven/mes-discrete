<template>
  <el-card>
    <div class="toolbar">
      <span class="tip">工位绑定工序（过站扫的是工位码）；同一工序可部署在多条产线的多个工位</span>
      <span>
        <el-button type="primary" plain @click="lineDialog.visible = true">新增产线</el-button>
        <el-button type="primary" plain @click="openStation()">新增工位</el-button>
      </span>
    </div>
    <el-table :data="rows" border stripe>
      <el-table-column prop="stationCode" label="工位码" width="140" />
      <el-table-column prop="stationName" label="工位名称" min-width="160" />
      <el-table-column label="所属产线" width="160">
        <template #default="{ row }">{{ lineName(row.lineId) }}</template>
      </el-table-column>
      <el-table-column prop="operationCode" label="绑定工序" width="140" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openStation(row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="lineDialog.visible" title="新增产线" width="440px">
      <el-form :model="lineDialog.form" label-width="80px">
        <el-form-item label="产线编码" required><el-input v-model="lineDialog.form.lineCode" placeholder="如 L3" /></el-form-item>
        <el-form-item label="产线名称" required><el-input v-model="lineDialog.form.lineName" /></el-form-item>
        <el-form-item label="车间"><el-input v-model="lineDialog.form.workshop" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="lineDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="saveLine">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="stationDialog.visible" :title="stationDialog.form.id ? '编辑工位' : '新增工位'" width="440px">
      <el-form :model="stationDialog.form" label-width="80px">
        <el-form-item label="工位码" required><el-input v-model="stationDialog.form.stationCode" placeholder="如 L1-FCT" /></el-form-item>
        <el-form-item label="工位名称" required><el-input v-model="stationDialog.form.stationName" /></el-form-item>
        <el-form-item label="产线" required>
          <el-select v-model="stationDialog.form.lineId" style="width:100%">
            <el-option v-for="l in lines" :key="l.id" :label="`${l.lineCode} ${l.lineName}`" :value="l.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="绑定工序" required>
          <el-select v-model="stationDialog.form.operationCode" filterable style="width:100%">
            <el-option v-for="o in operations" :key="o.operationCode"
                       :label="`${o.operationCode} ${o.operationName}`" :value="o.operationCode" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态"><el-switch v-model="stationDialog.form.status" :active-value="1" :inactive-value="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="stationDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="saveStation">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { listLine, saveLine as saveLineApi, listStation, saveStation as saveStationApi, listOperation } from '../../api/base'

const rows = ref([])
const lines = ref([])
const operations = ref([])
const lineDialog = reactive({ visible: false, form: { status: 1 } })
const stationDialog = reactive({ visible: false, form: {} })

const lineName = id => {
  const l = lines.value.find(x => x.id === id)
  return l ? `${l.lineCode} ${l.lineName}` : id
}

async function load() {
  ;[lines.value, operations.value, rows.value] = await Promise.all([
    listLine(), listOperation(), listStation()
  ])
}
function openStation(row) {
  stationDialog.form = row ? { ...row } : { status: 1 }
  stationDialog.visible = true
}
async function saveLine() {
  await saveLineApi(lineDialog.form)
  ElMessage.success('保存成功')
  lineDialog.visible = false
  lineDialog.form = { status: 1 }
  load()
}
async function saveStation() {
  await saveStationApi(stationDialog.form)
  ElMessage.success('保存成功')
  stationDialog.visible = false
  load()
}
onMounted(load)
</script>

<style scoped>
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.tip { color: #909399; font-size: 13px; }
</style>
