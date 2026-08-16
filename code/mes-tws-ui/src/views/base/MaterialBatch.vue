<template>
  <el-card>
    <div class="toolbar">
      <el-input v-model="query.keyword" placeholder="批次号" clearable style="width:220px" @keyup.enter="load" />
      <el-button type="primary" style="margin-left:8px" @click="load">查询</el-button>
      <el-button type="primary" plain @click="openDialog()">登记批次</el-button>
    </div>
    <el-table :data="rows" v-loading="loading" border stripe>
      <el-table-column prop="batchNo" label="批次号" width="160" />
      <el-table-column label="物料" min-width="200">
        <template #default="{ row }">{{ materialName(row.materialId) }}</template>
      </el-table-column>
      <el-table-column prop="supplier" label="供应商" width="120" />
      <el-table-column prop="arriveTime" label="到料时间" width="170" />
      <el-table-column prop="quantity" label="数量" width="90" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '可用' : '冻结' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination style="margin-top:12px;justify-content:flex-end" layout="total, prev, pager, next"
      :total="total" v-model:current-page="query.page" v-model:page-size="query.size" @change="load" />

    <el-dialog v-model="dialog.visible" :title="dialog.form.id ? '编辑批次' : '登记来料批次'" width="480px">
      <el-form :model="dialog.form" label-width="90px">
        <el-form-item label="物料" required>
          <el-select v-model="dialog.form.materialId" filterable style="width:100%">
            <el-option v-for="m in batchMaterials" :key="m.id" :label="`${m.materialCode} ${m.materialName}`" :value="m.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="批次号" required>
          <el-input v-model="dialog.form.batchNo" placeholder="如 PCLB240901" />
        </el-form-item>
        <el-form-item label="供应商"><el-input v-model="dialog.form.supplier" /></el-form-item>
        <el-form-item label="数量"><el-input-number v-model="dialog.form.quantity" :min="0" /></el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="dialog.form.status" :active-value="1" :inactive-value="0"
                     active-text="可用" inactive-text="冻结" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog.visible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { pageBatch, saveBatch } from '../../api/base'
import { listMaterial } from '../../api/material'

const query = reactive({ page: 1, size: 10, keyword: '' })
const rows = ref([])
const total = ref(0)
const loading = ref(false)
const dialog = reactive({ visible: false, form: {} })
const materials = ref([])

// 批次只挂在"批次管理"的物料上（关键料/半成品）
const batchMaterials = ref([])
const materialName = id => {
  const m = materials.value.find(x => x.id === id)
  return m ? `${m.materialCode} ${m.materialName}` : id
}

async function load() {
  loading.value = true
  try {
    const r = await pageBatch(query)
    rows.value = r.list
    total.value = r.total
  } finally { loading.value = false }
}

function openDialog(row) {
  dialog.form = row ? { ...row } : { status: 1, quantity: 0 }
  dialog.visible = true
}

async function save() {
  await saveBatch(dialog.form)
  ElMessage.success('保存成功')
  dialog.visible = false
  load()
}

onMounted(async () => {
  materials.value = await listMaterial()
  batchMaterials.value = materials.value.filter(m => m.batchManaged === 1)
  load()
})
</script>

<style scoped>
.toolbar { display: flex; margin-bottom: 12px; }
</style>
