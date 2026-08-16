<template>
  <el-card>
    <div class="toolbar">
      <span class="tip">工序字典：全厂统一维护，工位绑定工序、工艺路线引用工序</span>
      <el-button type="primary" plain @click="openDialog()">新增工序</el-button>
    </div>
    <el-table :data="rows" border stripe>
      <el-table-column prop="operationCode" label="工序编码" width="160" />
      <el-table-column prop="operationName" label="工序名称" min-width="200" />
      <el-table-column label="类型" width="120">
        <template #default="{ row }">
          <el-tag>{{ typeMap[row.operationType] || row.operationType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialog.visible" :title="dialog.form.id ? '编辑工序' : '新增工序'" width="440px">
      <el-form :model="dialog.form" label-width="90px">
        <el-form-item label="工序编码" required><el-input v-model="dialog.form.operationCode" placeholder="如 OP-ACOUS-L" /></el-form-item>
        <el-form-item label="工序名称" required><el-input v-model="dialog.form.operationName" /></el-form-item>
        <el-form-item label="类型" required>
          <el-select v-model="dialog.form.operationType" style="width:100%">
            <el-option v-for="(label, value) in typeMap" :key="value" :label="label" :value="value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="dialog.form.status" :active-value="1" :inactive-value="0" />
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
import { listOperation, saveOperation } from '../../api/base'

const typeMap = { NORMAL: '普通', TEST: '测试', BIND: '绑定', AGING: '老化', PACK: '包装', IQC: '检验' }
const rows = ref([])
const dialog = reactive({ visible: false, form: {} })

async function load() { rows.value = await listOperation() }
function openDialog(row) {
  dialog.form = row ? { ...row } : { operationType: 'NORMAL', status: 1 }
  dialog.visible = true
}
async function save() {
  await saveOperation(dialog.form)
  ElMessage.success('保存成功')
  dialog.visible = false
  load()
}
onMounted(load)
</script>

<style scoped>
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.tip { color: #909399; font-size: 13px; }
</style>
