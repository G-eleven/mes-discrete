<template>
  <div>
    <el-row :gutter="14">
      <el-col :span="6"><el-card><div class="stat"><div class="num">{{ s.finished }}</div><div class="label">完工整机</div></div></el-card></el-col>
      <el-col :span="6"><el-card><div class="stat"><div class="num blue">{{ s.fpy }}%</div><div class="label">FPY 一次直通率（不含返修）</div></div></el-card></el-col>
      <el-col :span="6"><el-card><div class="stat"><div class="num green">{{ s.finalYield }}%</div><div class="label">最终良率（含返修，剔除报废）</div></div></el-card></el-col>
      <el-col :span="6"><el-card><div class="stat"><div class="num red">{{ s.scrapped }}</div><div class="label">报废</div></div></el-card></el-col>
    </el-row>

    <el-row :gutter="14" style="margin-top:14px">
      <el-col :span="14">
        <el-card>
          <template #header>按日良率趋势（FPY vs 产量）</template>
          <div ref="trendChart" style="height:300px"></div>
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card>
          <template #header>不良柏拉图 Top10</template>
          <div ref="paretoChart" style="height:300px"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-card style="margin-top:14px">
      <template #header>按工单良率明细</template>
      <el-table :data="s.rows || []" border stripe>
        <el-table-column prop="woNo" label="工单" width="160" />
        <el-table-column prop="planQty" label="计划" width="80" />
        <el-table-column prop="finished" label="完工" width="80" />
        <el-table-column prop="firstPass" label="一次直通" width="90" />
        <el-table-column prop="scrapped" label="报废" width="70" />
        <el-table-column label="FPY" min-width="220">
          <template #default="{ row }">
            <el-progress :percentage="pct(row.firstPass, row.finished)" :stroke-width="14" :text-inside="true" />
          </template>
        </el-table-column>
        <el-table-column label="最终良率" min-width="180">
          <template #default="{ row }">
            {{ pct(row.finished, row.finished + row.scrapped) }}%
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import * as echarts from 'echarts'
import { fpySummary, fpyDaily, fpyPareto } from '../../api/quality'

const s = reactive({ finished: 0, firstPass: 0, scrapped: 0, fpy: 0, finalYield: 0, rows: [] })
const trendChart = ref(null)
const paretoChart = ref(null)
const pct = (a, b) => b ? Math.round(a * 1000 / b) / 10 : 0

onMounted(async () => {
  const summary = await fpySummary({})
  Object.assign(s, summary)

  const daily = await fpyDaily()
  const trend = echarts.init(trendChart.value)
  trend.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['FPY %', '完工产量'] },
    xAxis: { type: 'category', data: daily.map(d => String(d.day)) },
    yAxis: [
      { type: 'value', name: 'FPY %', min: v => Math.floor(v.min - 2), max: 100 },
      { type: 'value', name: '完工产量' }
    ],
    series: [
      { name: 'FPY %', type: 'line', smooth: true, data: daily.map(d => d.fpy),
        itemStyle: { color: '#409eff' }, markLine: { data: [{ yAxis: 95, name: '目标 95%' }] } },
      { name: '完工产量', type: 'bar', yAxisIndex: 1, data: daily.map(d => d.finished),
        itemStyle: { color: '#67c23a', opacity: 0.5 } }
    ]
  })

  const pareto = await fpyPareto({})
  const total = pareto.reduce((m, p) => m + Number(p.cnt), 0)
  let acc = 0
  const cumulative = pareto.map(p => { acc += Number(p.cnt); return Math.round(acc * 1000 / total) / 10 })
  const chart = echarts.init(paretoChart.value)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['不良数', '累计占比 %'] },
    xAxis: { type: 'category', data: pareto.map(p => p.defectCode), axisLabel: { rotate: 30 } },
    yAxis: [{ type: 'value' }, { type: 'value', max: 100 }],
    series: [
      { name: '不良数', type: 'bar', data: pareto.map(p => p.cnt), itemStyle: { color: '#f56c6c' } },
      { name: '累计占比 %', type: 'line', yAxisIndex: 1, data: cumulative, itemStyle: { color: '#e6a23c' } }
    ]
  })
  window.addEventListener('resize', () => { trend.resize(); chart.resize() })
})
</script>

<style scoped>
.stat { text-align: center; padding: 10px 0; }
.num { font-size: 30px; font-weight: 700; color: #409eff; }
.num.blue { color: #409eff; } .num.green { color: #67c23a; } .num.red { color: #f56c6c; }
.label { color: #909399; margin-top: 6px; font-size: 13px; }
</style>
