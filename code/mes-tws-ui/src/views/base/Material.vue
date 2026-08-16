<template>
  <el-card>
    <!-- 查询区 -->
    <div class="toolbar">
      <el-input v-model="query.keyword" placeholder="物料编码/名称" clearable style="width:220px" @keyup.enter="load" />
      <el-select v-model="query.materialType" placeholder="物料类型" clearable style="width:150px;margin-left:8px">
        <el-option v-for="(label, value) in typeMap" :key="value" :label="label" :value="value" />
      </el-select>
      <el-button type="primary" style="margin-left:8px" @click="load">查询</el-button>
      <el-button type="primary" plain @click="openDialog()">新增物料</el-button>
    </div>

    <!-- 表格 -->
    <el-table :data="rows" v-loading="loading" border stripe>
      <el-table-column prop="materialCode" label="物料编码" width="140" />
      <el-table-column prop="materialName" label="物料名称" min-width="180" />
      <el-table-column label="类型" width="100">
        <template #default="{ row }">
          <el-tag :type="typeTag(row.materialType)">{{ typeMap[row.materialType] || row.materialType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="spec" label="规格" min-width="160" />
      <el-table-column prop="unit" label="单位" width="70" />
      <el-table-column label="批次管理" width="90">
        <template #default="{ row }">{{ row.batchManaged === 1 ? '是' : '否' }}</template>
      </el-table-column>
      <el-table-column prop="supplier" label="默认供应商" width="120" />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
          <el-popconfirm title="确认删除该物料？" @confirm="del(row.id)">
            <template #reference><el-button link type="danger">删除</el-button></template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination style="margin-top:12px;justify-content:flex-end" layout="total, prev, pager, next, sizes"
      :total="total" v-model:current-page="query.page" v-model:page-size="query.size"
      :page-sizes="[10, 20, 50]" @change="load" />

    <!-- 编辑弹窗 -->
    <el-dialog v-model="dialog.visible" :title="dialog.form.id ? '编辑物料' : '新增物料'" width="520px">
      <el-form :model="dialog.form" label-width="90px">
        <el-form-item label="物料编码" required>
          <el-input v-model="dialog.form.materialCode" placeholder="如 PCBA-LEFT" />
        </el-form-item>
        <el-form-item label="物料名称" required>
          <el-input v-model="dialog.form.materialName" />
        </el-form-item>
        <el-form-item label="类型" required>
          <el-select v-model="dialog.form.materialType" style="width:100%">
            <el-option v-for="(label, value) in typeMap" :key="value" :label="label" :value="value" />
          </el-select>
        </el-form-item>
        <el-form-item label="规格">
          <el-input v-model="dialog.form.spec" />
        </el-form-item>
        <el-form-item label="单位">
          <el-input v-model="dialog.form.unit" style="width:120px" />
        </el-form-item>
        <el-form-item label="批次管理">
          <el-switch v-model="dialog.form.batchManaged" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="供应商">
          <el-input v-model="dialog.form.supplier" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="dialog.form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="dialog.form.remark" type="textarea" />
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
import { pageMaterial, saveMaterial, deleteMaterial } from '../../api/material'

/** 物料类型字典 —— 全项目 CRUD 页的标准结构：查询区 + 表格 + 分页 + 弹窗编辑 */
const typeMap = { PRODUCT: '成品', SEMI: '半成品', KEY: '关键料', RAW: '原料', PACK: '包材' }
const typeTag = t => ({ PRODUCT: 'danger', SEMI: 'warning', KEY: 'danger', RAW: 'info', PACK: 'success' }[t] || 'info')

const query = reactive({ page: 1, size: 10, keyword: '', materialType: '' })
const rows = ref([])
const total = ref(0)
const loading = ref(false)
const dialog = reactive({ visible: false, form: {} })

async function load() {
  loading.value = true
  try {
    const r = await pageMaterial(query)
    rows.value = r.list
    total.value = r.total
  } finally {
    loading.value = false
  }
}

function openDialog(row) {
  dialog.form = row ? { ...row } : { materialType: 'RAW', unit: 'PCS', batchManaged: 0, status: 1 }
  dialog.visible = true
}

async function save() {
  await saveMaterial(dialog.form)
  ElMessage.success('保存成功')
  dialog.visible = false
  load()
}

async function del(id) {
  await deleteMaterial(id)
  ElMessage.success('删除成功')
  load()
}

onMounted(load)
</script>

<style scoped>
.toolbar { display: flex; margin-bottom: 12px; }
</style>
