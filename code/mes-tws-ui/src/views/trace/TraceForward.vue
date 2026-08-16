<template>
  <div>
    <el-card>
      <div class="toolbar">
        <el-input v-model="sn" placeholder="输入整机/部件 SN，如 WO20260801001-0021" clearable style="width:360px" @keyup.enter="doTrace">
          <template #append><el-button @click="doTrace">追溯</el-button></template>
        </el-input>
        <span v-if="data" class="cost">耗时 {{ data.costMs }} ms（纸质时代约 2 天）</span>
      </div>
    </el-card>

    <div v-if="data" style="margin-top:14px">
      <el-row :gutter="14">
        <el-col :span="8">
          <el-card>
            <template #header>SN 档案</template>
            <el-descriptions :column="1" border size="small">
              <el-descriptions-item label="SN">{{ data.sn.sn }}</el-descriptions-item>
              <el-descriptions-item label="类型">{{ snTypeMap[data.sn.snType] }}</el-descriptions-item>
              <el-descriptions-item label="状态">
                <el-tag :type="snTag[data.sn.status]" size="small">{{ snStatusMap[data.sn.status] }}</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="所属工单">{{ data.woNo || '-' }}</el-descriptions-item>
              <el-descriptions-item label="来料批次">{{ data.sn.batchNo || '-' }}</el-descriptions-item>
              <el-descriptions-item label="固件版本">{{ data.sn.firmwareVersion || '-' }}</el-descriptions-item>
              <el-descriptions-item label="工序进度">seq={{ data.sn.currentSeq }}</el-descriptions-item>
              <el-descriptions-item v-if="data.parent" label="所属父SN">
                <el-link type="primary" @click="sn = data.parent.sn; doTrace()">{{ data.parent.sn }}</el-link>
              </el-descriptions-item>
            </el-descriptions>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card v-if="data.children">
            <template #header>绑定子件（一机多码）</template>
            <el-table :data="data.children" border size="small">
              <el-table-column label="类型" width="70">
                <template #default="{ row }">{{ snTypeMap[row.bindType] }}</template>
              </el-table-column>
              <el-table-column prop="childSn" label="子件 SN" min-width="160" show-overflow-tooltip />
              <el-table-column prop="batchNo" label="批次" width="130" />
              <el-table-column label="状态" width="80">
                <template #default="{ row }">
                  <el-tag size="small" :type="snTag[row.childStatus]">{{ snStatusMap[row.childStatus] }}</el-tag>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
          <el-card v-if="data.defects && data.defects.length">
            <template #header>不良与维修史</template>
            <el-table :data="data.defects" border size="small">
              <el-table-column prop="defectCode" label="不良码" width="80" />
              <el-table-column prop="stationCode" label="工位" width="110" />
              <el-table-column label="状态" width="80">
                <template #default="{ row }">
                  <el-tag size="small" :type="{ OPEN: 'danger', REPAIRED: 'success', SCRAP: 'info' }[row.status]">{{ row.status }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="defectDesc" label="描述" min-width="140" show-overflow-tooltip />
            </el-table>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card>
            <template #header>维修记录</template>
            <el-table :data="data.repairs" border size="small" :empty-text="'无维修记录（一次直通）'">
              <el-table-column prop="defectCode" label="不良" width="70" />
              <el-table-column prop="repair.action" label="措施" min-width="120">
                <template #default="{ row }">{{ row.repair.action }}</template>
              </el-table-column>
              <el-table-column prop="repair.rootCause" label="根因" min-width="130">
                <template #default="{ row }">{{ row.repair.rootCause || '-' }}</template>
              </el-table-column>
              <el-table-column label="结果" width="60">
                <template #default="{ row }">
                  <el-tag size="small" :type="row.repair.result === 'OK' ? 'success' : 'danger'">{{ row.repair.result }}</el-tag>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-col>
      </el-row>

      <el-card style="margin-top:14px">
        <template #header>过站时间线（{{ data.timeline.length }} 条）</template>
        <el-timeline>
          <el-timeline-item v-for="(log, idx) in data.timeline" :key="idx"
                            :type="log.result === 'NG' ? 'danger' : (log.recordType === 'BINDING' ? 'warning' : 'success')"
                            :timestamp="log.createTime" placement="top">
            <b>{{ log.stationCode }}</b> · {{ log.operationCode }}
            <el-tag v-if="log.retestRound > 0" size="small" type="warning" style="margin-left:6px">重测第{{ log.retestRound }}轮</el-tag>
            <el-tag v-if="log.result" size="small" :type="log.result === 'OK' ? 'success' : 'danger'" style="margin-left:6px">{{ log.result }}</el-tag>
            <el-tag v-if="log.ngCode" size="small" type="danger" style="margin-left:6px">{{ log.ngCode }}</el-tag>
            <div class="testdata">{{ pretty(log.testData) }}</div>
          </el-timeline-item>
        </el-timeline>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { traceForward } from '../../api/trace'

const route = useRoute()
const sn = ref('')
const data = ref(null)
const snTypeMap = { MACHINE: '整机', LEFT: '左耳', RIGHT: '右耳', CASE: '充电盒' }
const snStatusMap = { INIT: '已注册', IN_LINE: '在制', NG: '不良待修', RETEST: '待重测', DONE: '完工', SCRAP: '报废' }
const snTag = { INIT: 'info', IN_LINE: '', NG: 'danger', RETEST: 'warning', DONE: 'success', SCRAP: 'danger' }

const pretty = s => {
  if (!s) return ''
  try { return JSON.stringify(JSON.parse(s)) } catch (e) { return s }
}

async function doTrace() {
  if (!sn.value) return
  data.value = await traceForward(sn.value.trim())
  ElMessage.success(`追溯完成，命中 ${data.value.timeline.length} 条流转记录`)
}

// 支持从反向追溯页带参跳入
onMounted(async () => {
  if (route.query.sn) {
    sn.value = String(route.query.sn)
    doTrace()
  }
})
</script>

<style scoped>
.toolbar { display: flex; align-items: center; gap: 14px; }
.cost { color: #909399; font-size: 13px; }
.testdata { color: #909399; font-size: 12px; margin-top: 4px; word-break: break-all; }
</style>
