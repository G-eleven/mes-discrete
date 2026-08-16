<template>
  <div>
    <el-alert type="info" :closable="false" style="margin-bottom:12px"
              title="过站模拟器：模拟车间扫码枪。选工单与工位 → 扫/取 SN → OK/NG 判定；防呆规则链会在下方实时提示拦截原因" />

    <el-row :gutter="14">
      <!-- 左：过站 -->
      <el-col :span="14">
        <el-card>
          <template #header>过站判定</template>
          <el-form label-width="80px">
            <el-form-item label="工单">
              <el-select v-model="woId" filterable style="width:100%" @change="onWoChange">
                <el-option v-for="w in wos" :key="w.id" :value="w.id"
                           :label="`${w.woNo}（${w.status === 'IN_PROGRESS' ? '生产中' : w.status}）`" />
              </el-select>
            </el-form-item>
            <el-form-item label="工位">
              <el-select v-model="stationCode" filterable style="width:100%" @change="onStationChange">
                <el-option v-for="s in stations" :key="s.stationCode" :value="s.stationCode"
                           :label="`${s.stationCode} ${s.stationName}`" />
              </el-select>
            </el-form-item>
          </el-form>

          <el-alert v-if="ctx && ctx.exists === false" type="warning" :closable="false"
                    title="该工单的工序快照不含此工位工序" />
          <el-descriptions v-if="ctx && ctx.exists" :column="3" border size="small" style="margin-bottom:12px">
            <el-descriptions-item label="工序">{{ ctx.seq }} - {{ ctx.operationName }}</el-descriptions-item>
            <el-descriptions-item label="加工SN类型">
              <el-tag size="small">{{ snTypeMap[ctx.snType] || ctx.snType }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="特殊要求">
              <span v-if="ctx.needBinding && ctx.needBinding.length">需已绑定 {{ ctx.needBinding.join('/') }}</span>
              <span v-else-if="ctx.requireLoading">需先上料</span>
              <span v-else>—</span>
            </el-descriptions-item>
          </el-descriptions>

          <el-form label-width="80px" v-if="ctx && ctx.exists">
            <el-form-item label="SN">
              <div style="display:flex;width:100%;gap:8px">
                <el-input v-model="checkinForm.sn" placeholder="扫码/输入 SN" @keyup.enter="doCheckin" />
                <el-button @click="pickNext">取下一个</el-button>
              </div>
            </el-form-item>
            <el-form-item v-if="testItems.length" :label="item.key" v-for="item in testItems" :key="item.key">
              <el-input v-model="testData[item.key]" :placeholder="`${item.op} ${item.value}`" style="width:200px" />
              <span class="hint">规则：{{ item.op }} {{ item.value }}</span>
            </el-form-item>
            <el-form-item label="判定">
              <el-radio-group v-model="checkinForm.result">
                <el-radio-button value="OK" type="success">OK 良品</el-radio-button>
                <el-radio-button value="NG" type="danger">NG 不良</el-radio-button>
              </el-radio-group>
            </el-form-item>
            <el-form-item v-if="checkinForm.result === 'NG'" label="不良代码">
              <el-select v-model="checkinForm.ngCode" filterable style="width:260px">
                <el-option v-for="d in defectCodes" :key="d.defectCode"
                           :label="`${d.defectCode} ${d.defectName}`" :value="d.defectCode" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" size="large" :loading="submitting" @click="doCheckin">
                提交过站（Enter）
              </el-button>
            </el-form-item>
          </el-form>
          <el-alert v-if="lastMsg" :type="lastOk ? 'success' : 'error'" :title="lastMsg" :closable="false"
                    style="margin-top:8px" />
        </el-card>
      </el-col>

      <!-- 右：绑定/上料 + 工位流水 -->
      <el-col :span="10">
        <el-card style="margin-bottom:14px">
          <template #header>三码绑定</template>
          <el-input v-model="bindForm.machineSn" placeholder="整机 SN" style="margin-bottom:8px">
            <template #append>
              <el-button @click="pickMachine">取下一个整机</el-button>
            </template>
          </el-input>
          <el-input v-for="t in ['LEFT', 'RIGHT', 'CASE']" :key="t" v-model="bindForm[t]"
                    :placeholder="`${snTypeMap[t]} SN`" style="margin-bottom:8px">
            <template #prepend>{{ snTypeMap[t] }}</template>
            <template #append>
              <el-button @click="pickChild(t)">取件</el-button>
            </template>
          </el-input>
          <el-button type="primary" style="width:100%" @click="doBind">绑定（绑定站工位 {{ bindStation || '未选' }}）</el-button>
        </el-card>

        <el-card style="margin-bottom:14px">
          <template #header>工位上料</template>
          <div style="display:flex;gap:8px">
            <el-select v-model="loadingBatch" filterable placeholder="选择批次" style="flex:1">
              <el-option v-for="b in batches" :key="b.batchNo" :label="`${b.batchNo}（${b.supplier || ''}）`" :value="b.batchNo" />
            </el-select>
            <el-button type="primary" @click="doLoading">上料到 {{ stationCode || '当前工位' }}</el-button>
          </div>
        </el-card>

        <el-card>
          <template #header>本工位最近流水</template>
          <el-table :data="recentLogs" size="small" border>
            <el-table-column prop="sn" label="SN" min-width="150" show-overflow-tooltip />
            <el-table-column label="结果" width="70">
              <template #default="{ row }">
                <el-tag v-if="row.result" :type="row.result === 'OK' ? 'success' : 'danger'" size="small">{{ row.result }}</el-tag>
                <el-tag v-else size="small" type="info">{{ typeShort(row.recordType) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="时间" width="150" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { reactive, ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { checkin, stationContext, bind, loading, pageSn, nextSn, pageLog } from '../../api/exec'
import { pageWo } from '../../api/wo'
import { listStation, listDefectCode, listBatch } from '../../api/base'

const snTypeMap = { MACHINE: '整机', LEFT: '左耳', RIGHT: '右耳', CASE: '充电盒', BOX: '彩盒', CARTON: '中箱', MATERIAL: '物料' }

const wos = ref([])
const stations = ref([])
const defectCodes = ref([])
const batches = ref([])
const woId = ref(null)
const stationCode = ref('')
const ctx = ref(null)
const testData = reactive({})
const checkinForm = reactive({ sn: '', result: 'OK', ngCode: undefined })
const bindForm = reactive({ machineSn: '', LEFT: '', RIGHT: '', CASE: '' })
const loadingBatch = ref('')
const lastMsg = ref('')
const lastOk = ref(false)
const submitting = ref(false)
const recentLogs = ref([])

const testItems = computed(() => (ctx.value && ctx.value.testItems) || [])
const bindStation = computed(() => stations.value.find(s => s.stationCode?.includes('COUPLE'))?.stationCode)

async function loadBase() {
  const wp = await pageWo({ size: 50, status: 'IN_PROGRESS' })
  wos.value = wp.list
  if (wos.value.length) woId.value = wos.value[0].id
  stations.value = await listStation()
  defectCodes.value = await listDefectCode()
  batches.value = await listBatch()
  if (wos.value.length) onWoChange()
}

async function onWoChange() { if (stationCode.value) onStationChange() }

async function onStationChange() {
  ctx.value = null
  Object.keys(testData).forEach(k => delete testData[k])
  if (!woId.value || !stationCode.value) return
  ctx.value = await stationContext({ woId: woId.value, stationCode: stationCode.value })
  loadRecent()
  // 预填测试项的期望值（演示方便，可改）
  testItems.value.forEach(t => { testData[t.key] = String(t.value) })
}

async function loadRecent() {
  if (!stationCode.value) return
  const r = await pageLog({ stationCode: stationCode.value, size: 10 })
  recentLogs.value = r.list
}

function pickNext() {
  if (!woId.value || !ctx.value?.exists) return
  const beforeSeq = ctx.value.seq
  nextSn({ workOrderId: woId.value, beforeSeq, snType: ctx.value.snType }).then(sn => {
    if (sn) { checkinForm.sn = sn.sn; ElMessage.success(`已取出待过站 SN：${sn.sn}（进度 seq=${sn.currentSeq}）`) }
    else ElMessage.info('没有符合条件的待过站 SN')
  })
}

function pickMachine() {
  if (!woId.value) return
  nextSn({ workOrderId: woId.value, snType: 'MACHINE', beforeSeq: 80 }).then(sn => {
    if (sn) bindForm.machineSn = sn.sn
    else ElMessage.info('没有待绑定的整机 SN（先用工单详情生成整机 SN）')
  })
}

function pickChild(type) {
  if (!woId.value) return
  nextSn({ workOrderId: woId.value, snType: type }).then(sn => {
    if (sn) bindForm[type] = sn.sn
    else ElMessage.info(`没有可用的${snTypeMap[type]} SN（先在 SN 管理注册部件）`)
  })
}

async function doCheckin() {
  submitting.value = true
  lastMsg.value = ''
  try {
    const payload = { ...checkinForm, stationCode: stationCode.value, testData: { ...testData } }
    const r = await checkin(payload)
    lastOk.value = true
    lastMsg.value = r.message
    loadRecent()
  } catch (e) {
    lastOk.value = false
    lastMsg.value = e.message || '过站失败'
  } finally {
    submitting.value = false
  }
}

async function doBind() {
  if (!bindStation.value) return ElMessage.warning('未找到三码绑定工位')
  const children = ['LEFT', 'RIGHT', 'CASE']
    .filter(t => bindForm[t])
    .map(t => ({ sn: bindForm[t], bindType: t }))
  await bind({ machineSn: bindForm.machineSn, stationCode: bindStation.value, children })
  ElMessage.success('绑定成功')
  loadRecent()
}

async function doLoading() {
  if (!stationCode.value || !loadingBatch.value) return ElMessage.warning('请选择工位与批次')
  await loading({ stationCode: stationCode.value, batchNo: loadingBatch.value })
  ElMessage.success('上料成功')
  loadRecent()
}

const typeShort = t => ({ LOADING: '上料', BINDING: '绑定' }[t] || t)

onMounted(loadBase)
</script>

<style scoped>
.hint { color: #909399; font-size: 12px; margin-left: 8px; }
</style>
