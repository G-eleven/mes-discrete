<template>
  <el-card>
    <div class="toolbar">
      <el-input v-model="query.keyword" placeholder="路线编码/名称" clearable style="width:200px" @keyup.enter="load" />
      <el-button type="primary" style="margin-left:8px" @click="load">查询</el-button>
      <el-button type="primary" plain @click="openEditor()">新建路线</el-button>
    </div>
    <el-table :data="rows" v-loading="loading" border stripe>
      <el-table-column prop="routingCode" label="路线编码" width="130" />
      <el-table-column prop="routingName" label="路线名称" min-width="220" />
      <el-table-column prop="version" label="版本" width="80">
        <template #default="{ row }">V{{ row.version }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusTag[row.status]">{{ statusMap[row.status] }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="updateTime" label="更新时间" width="170" />
      <el-table-column label="操作" width="240" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEditor(row)">编辑</el-button>
          <el-button v-if="row.status === 1" link type="success" @click="publish(row)">发布</el-button>
          <el-button link type="primary" @click="newVersion(row)">复制为新版本</el-button>
          <el-button v-if="row.status !== 2" link type="danger" @click="del(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination style="margin-top:12px;justify-content:flex-end" layout="total, prev, pager, next"
      :total="total" v-model:current-page="query.page" v-model:page-size="query.size" @change="load" />

    <el-dialog v-model="editor.visible" :title="editor.routing.id ? '编辑路线' : '新建路线'" width="860px" top="5vh">
      <el-form inline>
        <el-form-item label="路线编码"><el-input v-model="editor.routing.routingCode" :disabled="!!editor.routing.id" /></el-form-item>
        <el-form-item label="路线名称"><el-input v-model="editor.routing.routingName" /></el-form-item>
        <el-form-item label="适用成品">
          <el-select v-model="editor.routing.productMaterialId" filterable style="width:220px">
            <el-option v-for="m in products" :key="m.id" :label="`${m.materialCode} ${m.materialName}`" :value="m.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <el-divider content-position="left">
        工序列表（按 seq 升序执行；check_rules 为防呆规则 JSON）
      </el-divider>
      <el-table :data="editor.operations" border size="small" max-height="380px">
        <el-table-column label="seq" width="90">
          <template #default="{ row }"><el-input-number v-model="row.seq" :min="10" :step="5" size="small" controls-position="right" style="width:80px" /></template>
        </el-table-column>
        <el-table-column label="工序" min-width="220">
          <template #default="{ row }">
            <el-select v-model="row.operationCode" filterable size="small">
              <el-option v-for="o in operations" :key="o.operationCode" :label="`${o.operationCode} ${o.operationName}`" :value="o.operationCode" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="防呆规则 check_rules" min-width="300">
          <template #default="{ row, $index }">
            <el-input v-model="row.checkRules" type="textarea" :rows="1" size="small" placeholder="可空，或点右侧模板"
                      @blur="validateRules(row)" />
            <el-dropdown size="small" style="margin-top:2px" @command="cmd => applyTemplate(cmd, $index)">
              <el-button link type="primary" size="small">插入模板</el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="left">左耳工序规则</el-dropdown-item>
                  <el-dropdown-item command="right">右耳工序规则</el-dropdown-item>
                  <el-dropdown-item command="case">盒工序规则</el-dropdown-item>
                  <el-dropdown-item command="bind">三码绑定规则</el-dropdown-item>
                  <el-dropdown-item command="firmware">固件版本校验</el-dropdown-item>
                  <el-dropdown-item command="loading">需上料批次</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="70">
          <template #default="{ $index }">
            <el-button link type="danger" size="small" @click="editor.operations.splice($index, 1)">删</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-button style="margin-top:8px;width:100%"
                 @click="editor.operations.push({ seq: nextSeq(), operationCode: undefined, checkRules: '' })">
        + 添加工序
      </el-button>
      <template #footer>
        <el-button @click="editor.visible = false">取消</el-button>
        <el-button type="primary" @click="save">保存（草稿）</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageRouting, getRouting, saveRouting, publishRouting, deleteRouting } from '../../api/base'
import { listMaterial } from '../../api/material'
import { listOperation } from '../../api/base'

const statusMap = { 0: '停用', 1: '草稿', 2: '已发布' }
const statusTag = { 0: 'info', 1: 'warning', 2: 'success' }
const templates = {
  left: '{"snType":"LEFT","requirePrev":true}',
  right: '{"snType":"RIGHT","requirePrev":true}',
  case: '{"snType":"CASE","requirePrev":true,"testItems":[{"key":"charge_cur","op":"ge","value":100}]}',
  bind: '{"snType":"MACHINE","needBinding":["LEFT","RIGHT","CASE"]}',
  firmware: '{"snType":"MACHINE","requirePrev":true,"testItems":[{"key":"firmware","op":"eq","value":"1.2.5"}]}',
  loading: '{"snType":"CASE","requirePrev":true,"requireLoading":true}'
}

const query = reactive({ page: 1, size: 10, keyword: '' })
const rows = ref([])
const total = ref(0)
const loading = ref(false)
const products = ref([])
const operations = ref([])
const editor = reactive({ visible: false, routing: {}, operations: [] })

function nextSeq() {
  const max = editor.operations.reduce((m, o) => Math.max(m, o.seq || 0), 0)
  return max + 10
}
function applyTemplate(cmd, index) { editor.operations[index].checkRules = templates[cmd] }
function validateRules(row) {
  const s = (row.checkRules || '').trim()
  if (!s) return
  try { JSON.parse(s) } catch (e) { ElMessage.warning('check_rules 不是合法 JSON，保存将被后端拒绝') }
}

async function load() {
  loading.value = true
  try {
    const r = await pageRouting(query)
    rows.value = r.list
    total.value = r.total
  } finally { loading.value = false }
}

async function openEditor(row) {
  if (row) {
    const d = await getRouting(row.id)
    editor.routing = d.routing
    editor.operations = d.operations.map(o => ({ ...o }))
    if (editor.routing.status === 2) {
      ElMessage.info('已发布版本不可修改，如需调整请"复制为新版本"')
    }
  } else {
    editor.routing = {}
    editor.operations = [{ seq: 10, operationCode: undefined, checkRules: '' }]
  }
  editor.visible = true
}

async function newVersion(row) {
  const d = await getRouting(row.id)
  editor.routing = { routingCode: d.routing.routingCode, routingName: d.routing.routingName, productMaterialId: d.routing.productMaterialId }
  editor.operations = d.operations.map(o => ({ seq: o.seq, operationCode: o.operationCode, checkRules: o.checkRules }))
  editor.visible = true
  ElMessage.success('已复制工序，保存后生成新版本草稿')
}

async function publish(row) {
  await ElMessageBox.confirm(`发布路线 ${row.routingCode} V${row.version} 后才能被工单引用，确认发布？`, '发布确认')
  await publishRouting(row.id)
  ElMessage.success('已发布')
  load()
}

async function save() {
  await saveRouting({ routing: editor.routing, operations: editor.operations })
  ElMessage.success('已保存为草稿')
  editor.visible = false
  load()
}

async function del(row) {
  await ElMessageBox.confirm(`确认删除路线 ${row.routingCode} V${row.version}？`, '删除确认')
  await deleteRouting(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(async () => {
  const mats = await listMaterial({ materialType: 'PRODUCT' })
  products.value = mats
  operations.value = await listOperation()
  load()
})
</script>

<style scoped>
.toolbar { display: flex; margin-bottom: 12px; }
</style>
