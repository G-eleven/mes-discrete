<template>
  <el-card>
    <div class="toolbar">
      <span class="tip">不良代码字典：过站 NG / 维修登记时选用</span>
      <el-button type="primary" plain @click="openDialog()">新增不良代码</el-button>
    </div>
    <el-table :data="rows" border stripe>
      <el-table-column prop="defectCode" label="不良代码" width="120" />
      <el-table-column prop="defectName" label="不良名称" min-width="200" />
      <el-table-column label="分类" width="120">
        <template #default="{ row }">
          <el-tag :type="catTag[row.category] || 'info'">{{ catMap[row.category] || row.category }}</el-tag>
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

    <el-dialog v-model="dialog.visible" :title="dialog.form.id ? '编辑' : '新增不良代码'" width="440px">
      <el-form :model="dialog.form" label-width="90px">
        <el-form-item label="不良代码" required><el-input v-model="dialog.form.defectCode" placeholder="如 D12" /></el-form-item>
        <el-form-item label="不良名称" required><el-input v-model="dialog.form.defectName" /></el-form-item>
        <el-form-item label="分类">
          <el-select v-model="dialog.form.category" style="width:100%">
            <el-option v-for="(label, value) in catMap" :key="value" :label="label" :value="value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态"><el-switch v-model="dialog.form.status" :active-value="1" :inactive-value="0" /></el-form-item>
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
import { listDefectCode, saveDefectCode } from '../../api/base'

const catMap = { APPEARANCE: '外观', ACOUSTIC: '声学', FUNC: '功能', OTHER: '其他' }
const catTag = { APPEARANCE: 'warning', ACOUSTIC: 'danger', FUNC: 'danger', OTHER: 'info' }
const rows = ref([])
const dialog = reactive({ visible: false, form: {} })

async function load() { rows.value = await listDefectCode() }
function openDialog(row) { dialog.form = row ? { ...row } : { category: 'OTHER', status: 1 }; dialog.visible = true }
async function save() {
  await saveDefectCode(dialog.form)
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
