<template>
  <el-card>
    <div class="toolbar">
      <el-input v-model="query.keyword" placeholder="BOM 编码" clearable style="width:200px" @keyup.enter="load" />
      <el-button type="primary" style="margin-left:8px" @click="load">查询</el-button>
      <el-button type="primary" plain @click="openEditor()">新建 BOM</el-button>
    </div>
    <el-table :data="rows" v-loading="loading" border stripe>
      <el-table-column prop="bomCode" label="BOM 编码" width="160" />
      <el-table-column label="成品" min-width="200">
        <template #default="{ row }">{{ materialLabel(row.productMaterialId) }}</template>
      </el-table-column>
      <el-table-column prop="version" label="版本" width="90" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEditor(row.id)">编辑</el-button>
          <el-popconfirm title="确认删除该 BOM？" @confirm="del(row.id)">
            <template #reference><el-button link type="danger">删除</el-button></template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination style="margin-top:12px;justify-content:flex-end" layout="total, prev, pager, next"
      :total="total" v-model:current-page="query.page" v-model:page-size="query.size" @change="load" />

    <!-- 主从编辑 -->
    <el-dialog v-model="editor.visible" :title="editor.bom.id ? '编辑 BOM' : '新建 BOM'" width="720px">
      <el-form inline>
        <el-form-item label="BOM 编码"><el-input v-model="editor.bom.bomCode" /></el-form-item>
        <el-form-item label="成品物料">
          <el-select v-model="editor.bom.productMaterialId" filterable style="width:240px">
            <el-option v-for="m in products" :key="m.id" :label="`${m.materialCode} ${m.materialName}`" :value="m.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="版本"><el-input v-model="editor.bom.version" style="width:100px" /></el-form-item>
        <el-form-item label="状态"><el-switch v-model="editor.bom.status" :active-value="1" :inactive-value="0" /></el-form-item>
      </el-form>
      <el-divider content-position="left">子件明细</el-divider>
      <el-table :data="editor.items" border size="small">
        <el-table-column label="子件物料" min-width="240">
          <template #default="{ row }">
            <el-select v-model="row.childMaterialId" filterable style="width:100%">
              <el-option v-for="m in children" :key="m.id" :label="`${m.materialCode} ${m.materialName}`" :value="m.id" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="用量" width="140">
          <template #default="{ row }">
            <el-input-number v-model="row.quantity" :min="0.01" :step="1" size="small" style="width:120px" />
          </template>
        </el-table-column>
        <el-table-column label="投料工位" width="200">
          <template #default="{ row }">
            <el-select v-model="row.operationCode" filterable clearable placeholder="选工序" size="small" style="width:100%">
              <el-option v-for="op in operations" :key="op.operationCode" :label="`${op.operationCode} ${op.operationName}`" :value="op.operationCode" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80">
          <template #default="{ $index }">
            <el-button link type="danger" @click="editor.items.splice($index, 1)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-button style="margin-top:8px;width:100%" @click="editor.items.push({ childMaterialId: undefined, quantity: 1, operationCode: '' })">
        + 添加子件
      </el-button>
      <template #footer>
        <el-button @click="editor.visible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { pageBom, getBom, saveBom, deleteBom, listOperation } from '../../api/base'
import { listMaterial } from '../../api/material'

const query = reactive({ page: 1, size: 10, keyword: '' })
const rows = ref([])
const total = ref(0)
const loading = ref(false)
const materials = ref([])
const products = ref([])
const children = ref([])
const operations = ref([])
const editor = reactive({ visible: false, bom: {}, items: [] })

const materialLabel = id => {
  const m = materials.value.find(x => x.id === id)
  return m ? `${m.materialCode} ${m.materialName}` : id
}

async function load() {
  loading.value = true
  try {
    const r = await pageBom(query)
    rows.value = r.list
    total.value = r.total
  } finally { loading.value = false }
}

async function openEditor(id) {
  if (id) {
    const d = await getBom(id)
    editor.bom = d.bom
    editor.items = d.items.map(it => ({ childMaterialId: it.childMaterialId, quantity: it.quantity, operationCode: it.operationCode || '' }))
  } else {
    editor.bom = { version: 'V1.0', status: 1 }
    editor.items = [{ childMaterialId: undefined, quantity: 1, operationCode: '' }]
  }
  editor.visible = true
}

async function save() {
  await saveBom({ bom: editor.bom, items: editor.items })
  ElMessage.success('保存成功')
  editor.visible = false
  load()
}

async function del(id) {
  await deleteBom(id)
  ElMessage.success('删除成功')
  load()
}

onMounted(async () => {
  materials.value = await listMaterial()
  products.value = materials.value.filter(m => m.materialType === 'PRODUCT')
  children.value = materials.value.filter(m => m.materialType !== 'PRODUCT')
  operations.value = await listOperation()
  load()
})
</script>

<style scoped>
.toolbar { display: flex; margin-bottom: 12px; }
</style>
