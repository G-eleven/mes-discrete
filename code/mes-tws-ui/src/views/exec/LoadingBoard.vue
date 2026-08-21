<template>
  <div>
    <el-alert type="info" :closable="false" style="margin-bottom:12px"
              title="工位上料看板：按工单 MBOM 算出每个工位应上什么料，红绿灯标识齐套状态；扫码上料时校验批次物料是否匹配工位需求，防上错料" />

    <el-card style="margin-bottom:14px">
      <div style="display:flex;align-items:center;gap:24px;flex-wrap:wrap">
        <el-select v-model="woId" filterable placeholder="选择生产中工单" style="width:320px" @change="loadBoard">
          <el-option v-for="w in wos" :key="w.id" :value="w.id"
                     :label="`${w.woNo}（${w.status === 'IN_PROGRESS' ? '生产中' : w.status}）`" />
        </el-select>
        <div style="display:flex;gap:12px">
          <div class="metric">
            <div class="num" style="color:var(--el-color-danger)">{{ stats.short }}</div>
            <div class="lab">待上料</div>
          </div>
          <div class="metric">
            <div class="num" style="color:var(--el-color-success)">{{ stats.loaded }}</div>
            <div class="lab">已齐套</div>
          </div>
          <div class="metric">
            <div class="num" style="color:var(--el-color-warning)">{{ stats.lowWarn }}</div>
            <div class="lab">低量预警</div>
          </div>
        </div>
      </div>
    </el-card>

    <el-row :gutter="14">
      <el-col :span="14">
        <el-card>
          <template #header>需上料工位（点击卡片选中进行上料）</template>
          <div v-if="!stations.length" class="empty">请选择工单</div>
          <div class="grid">
            <div v-for="s in stations" :key="s.stationCode"
                 class="station-card" :class="[statusClass(s.summary), {active: selected===s.stationCode}]"
                 @click="selectStation(s)">
              <div class="card-head">
                <span class="st-name">{{ s.stationCode }} {{ s.stationName }}</span>
                <span class="badge" :class="statusClass(s.summary)">{{ statusText(s.summary) }}</span>
              </div>
              <div v-for="it in s.items" :key="it.materialId" class="mat-row">
                <span class="mat-code">{{ it.materialCode }}</span>
                <span class="mat-name">{{ it.materialName }}</span>
                <span class="qty" :class="it.status">
                  {{ it.loadedQty }}/{{ it.requiredQty }}{{ it.status === 'SHORT' ? ' 缺' : it.status === 'LOW_WARN' ? ' ⚠' : ' ✓' }}
                </span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="10">
        <el-card style="margin-bottom:14px">
          <template #header>
            上料防错<span v-if="selected" class="hint"> · {{ selected }}</span>
          </template>
          <div v-if="!selected" class="empty">点击左侧工位卡片</div>
          <div v-else>
            <div class="hint" style="margin-bottom:6px">应上物料（点击选择要上的料，缺料项标红优先）</div>
            <div v-for="it in currentItems" :key="it.materialId"
                 class="should-row" :class="{active: form.materialId === it.materialId, short: it.status === 'SHORT'}"
                 @click="pickMaterial(it)">
              <span>{{ it.materialCode }} {{ it.materialName }} <span class="hint">{{ it.spec }}</span></span>
              <span>应上 {{ it.requiredQty }} {{ it.unit }}</span>
            </div>

            <el-form label-width="72px" style="margin-top:14px">
              <el-form-item label="已选">
                <span style="font-weight:500">{{ form.materialCode || '未选' }}</span>
              </el-form-item>
              <el-form-item label="批次">
                <el-select v-model="form.batchNo" filterable placeholder="扫/选物料批次" style="width:100%">
                  <el-option v-for="b in batches" :key="b.batchNo"
                             :label="`${b.batchNo}（${b.supplier || ''} 剩${b.remainQty != null ? b.remainQty : b.quantity}）`"
                             :value="b.batchNo" />
                </el-select>
              </el-form-item>
              <el-form-item label="上料量">
                <el-input-number v-model="form.loadingQty" :min="1" style="width:100%" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :loading="submitting" @click="doLoad">
                  扫码上料（校验物料匹配）
                </el-button>
              </el-form-item>
            </el-form>
            <el-alert v-if="msg" :type="ok ? 'success' : 'error'" :title="msg" :closable="false" style="margin-top:8px" />
          </div>
        </el-card>

        <el-card>
          <template #header>当前上料台账 · {{ selected || '未选工位' }}</template>
          <el-table v-if="ledger.length" :data="ledger" size="small" border>
            <el-table-column prop="materialCode" label="物料" min-width="100" show-overflow-tooltip />
            <el-table-column prop="batchNo" label="批次" min-width="120" show-overflow-tooltip />
            <el-table-column label="剩余/上料" width="110">
              <template #default="{ row }">
                <span :style="row.lowWarn ? 'color:var(--el-color-warning);font-weight:500' : ''">
                  {{ row.remainQty }}/{{ row.loadingQty }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="operator" label="人" width="60" />
          </el-table>
          <div v-else class="empty">暂无台账</div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { loadingBoard, loadingLedger, kitting, doLoading } from '../../api/loading'
import { pageWo } from '../../api/wo'
import { listBatch } from '../../api/base'

const wos = ref([])
const stations = ref([])
const batches = ref([])
const woId = ref(null)
const selected = ref('')
const currentItems = ref([])
const ledger = ref([])
const form = reactive({ materialId: null, materialCode: '', batchNo: '', loadingQty: 100 })
const submitting = ref(false)
const msg = ref('')
const ok = ref(false)

const stats = computed(() => {
  let short = 0, loaded = 0, lowWarn = 0
  for (const s of stations.value) {
    if (s.summary.short > 0) short++
    else if (s.summary.lowWarn > 0) lowWarn++
    else loaded++
  }
  return { short, loaded, lowWarn }
})

async function loadBase() {
  const wp = await pageWo({ size: 50, status: 'IN_PROGRESS' })
  wos.value = wp.list
  batches.value = await listBatch()
  if (wos.value.length) {
    woId.value = wos.value[0].id
    await loadBoard()
  }
}

async function loadBoard() {
  if (!woId.value) return
  selected.value = ''
  currentItems.value = []
  ledger.value = []
  stations.value = await loadingBoard({ woId: woId.value })
}

function statusClass(summary) {
  if (summary.short > 0) return 'short'
  if (summary.lowWarn > 0) return 'low'
  return 'ok'
}

function statusText(summary) {
  if (summary.short > 0) return '待上料'
  if (summary.lowWarn > 0) return '低量预警'
  return '已齐套'
}

async function selectStation(s) {
  selected.value = s.stationCode
  const k = await kitting({ woId: woId.value, stationCode: s.stationCode })
  currentItems.value = k.items || []
  const first = currentItems.value.find(i => i.status === 'SHORT') || currentItems.value[0]
  if (first) pickMaterial(first)
  await loadLedger()
}

function pickMaterial(it) {
  form.materialId = it.materialId
  form.materialCode = it.materialCode
  form.batchNo = ''
  msg.value = ''
}

async function loadLedger() {
  ledger.value = await loadingLedger({ woId: woId.value, stationCode: selected.value })
}

async function doLoad() {
  if (!form.materialId || !form.batchNo) {
    return ElMessage.warning('请选择物料与批次')
  }
  submitting.value = true
  msg.value = ''
  try {
    await doLoading({
      workOrderId: woId.value,
      stationCode: selected.value,
      materialId: form.materialId,
      batchNo: form.batchNo,
      loadingQty: form.loadingQty
    })
    ok.value = true
    msg.value = `上料成功：${form.materialCode} / ${form.batchNo}`
    const st = selected.value
    await loadBoard()
    const s = stations.value.find(x => x.stationCode === st)
    if (s) await selectStation(s)
  } catch (e) {
    ok.value = false
    msg.value = e.message || '上料失败'
  } finally {
    submitting.value = false
  }
}

onMounted(loadBase)
</script>

<style scoped>
.metric { background: var(--el-fill-color-light); border-radius: 8px; padding: 6px 16px; text-align: center; }
.metric .num { font-size: 22px; font-weight: 500; line-height: 1.2; }
.metric .lab { font-size: 12px; color: var(--el-text-color-secondary); }
.hint { color: var(--el-text-color-secondary); font-size: 12px; }
.empty { color: var(--el-text-color-secondary); font-size: 13px; padding: 20px 0; text-align: center; }
.grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; }
.station-card { border: 1px solid var(--el-border-color); border-left-width: 3px; border-radius: 8px; padding: 10px 12px; cursor: pointer; transition: box-shadow .15s; }
.station-card:hover { box-shadow: 0 0 0 1px var(--el-color-primary-light-5); }
.station-card.active { box-shadow: 0 0 0 2px var(--el-color-primary-light-5); }
.station-card.short { border-left-color: var(--el-color-danger); background: var(--el-color-danger-light-9); }
.station-card.low { border-left-color: var(--el-color-warning); background: var(--el-color-warning-light-9); }
.station-card.ok { border-left-color: var(--el-color-success); background: var(--el-color-success-light-9); }
.card-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.st-name { font-size: 13px; font-weight: 500; }
.badge { font-size: 11px; padding: 2px 8px; border-radius: 10px; color: #fff; }
.badge.short { background: var(--el-color-danger); }
.badge.low { background: var(--el-color-warning); }
.badge.ok { background: var(--el-color-success); }
.mat-row { display: flex; align-items: center; gap: 8px; font-size: 12px; padding: 3px 0; }
.mat-code { color: var(--el-text-color-secondary); min-width: 70px; }
.mat-name { flex: 1; }
.qty { font-weight: 500; }
.qty.SHORT { color: var(--el-color-danger); }
.qty.LOW_WARN { color: var(--el-color-warning); }
.qty.OK { color: var(--el-color-success); }
.should-row { display: flex; justify-content: space-between; align-items: center; padding: 7px 10px; border: 1px solid var(--el-border-color); border-radius: 6px; margin-bottom: 6px; cursor: pointer; font-size: 12px; }
.should-row:hover { border-color: var(--el-color-primary-light-5); }
.should-row.active { border-color: var(--el-color-primary); background: var(--el-color-primary-light-9); }
.should-row.short { border-left: 3px solid var(--el-color-danger); }
</style>
