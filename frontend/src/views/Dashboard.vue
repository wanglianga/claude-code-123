<template>
  <div>
    <!-- KPI -->
    <el-row :gutter="12">
      <el-col :span="6" v-for="k in kpis" :key="k.label">
        <el-card shadow="hover" class="kpi-card" :body-style="{padding:'16px'}">
          <div class="kpi-label">{{ k.label }}</div>
          <div class="kpi-value" :style="{color:k.color}">{{ k.value }}</div>
          <div class="kpi-sub">{{ k.sub }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 预警 -->
    <el-card class="section-card" shadow="never">
      <template #header><b><el-icon color="#e6a23c"><Warning /></el-icon> 风险预警</b></template>
      <el-space wrap>
        <el-alert :title="`逾期履行节点 ${alerts.overdueCount||0} 个`" type="error" :closable="false" show-icon style="width:auto"/>
        <el-alert :title="`被申请人失联 ${alerts.unreachableCount||0} 件`" type="warning" :closable="false" show-icon style="width:auto"/>
        <el-alert :title="`证据材料缺失 ${alerts.missingCount||0} 件`" type="warning" :closable="false" show-icon style="width:auto"/>
        <el-alert :title="`多次改期（≥2次） ${alerts.rescheduledCount||0} 件`" type="info" :closable="false" show-icon style="width:auto"/>
      </el-space>
      <div style="margin-top:10px">
        <el-button v-for="c in alertCases" :key="c.id" size="small" round @click="openCase(c.id)">
          {{ c.caseNo }} {{ c.title }}
        </el-button>
      </div>
    </el-card>

    <el-row :gutter="12">
      <el-col :span="12">
        <el-card shadow="never" class="section-card">
          <template #header><b>各案由调解情况</b></template>
          <div ref="causeChart" style="height:300px"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never" class="section-card">
          <template #header><b>案件状态分布</b></template>
          <div ref="statusChart" style="height:300px"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="12">
      <el-col :span="12">
        <el-card shadow="never" class="section-card">
          <template #header><b>转立案原因分布</b></template>
          <div ref="reasonChart" style="height:280px"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never" class="section-card">
          <template #header><b>协议履行概况</b></template>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="履行节点总数">{{ perf.planCount ?? 0 }}</el-descriptions-item>
            <el-descriptions-item label="已履行节点">{{ perf.paidPlanCount ?? 0 }}</el-descriptions-item>
            <el-descriptions-item label="逾期节点">
              <span style="color:#f56c6c;font-weight:600">{{ perf.overdueCount ?? 0 }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="部分履行节点">{{ perf.partialCount ?? 0 }}</el-descriptions-item>
            <el-descriptions-item label="应履行总额">¥{{ perf.totalDue ?? 0 }}</el-descriptions-item>
            <el-descriptions-item label="已履行总额">¥{{ perf.totalPaid ?? 0 }}</el-descriptions-item>
            <el-descriptions-item label="金额履行率" :span="2">
              <el-progress :percentage="perf.paidRate ?? 0" :stroke-width="16"/>
            </el-descriptions-item>
            <el-descriptions-item label="平均调解周期" :span="2">{{ data.avgMediationDays ?? 0 }} 天</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never">
      <template #header><b>调解员工作量与成效</b></template>
      <el-table :data="data.mediatorStats || []" size="small" stripe>
        <el-table-column prop="mediator" label="调解员" width="100"/>
        <el-table-column prop="specialties" label="专长案由"/>
        <el-table-column prop="total" label="承办" width="80" align="center"/>
        <el-table-column prop="agreed" label="达成协议" width="90" align="center"/>
        <el-table-column prop="transferred" label="转立案" width="80" align="center"/>
        <el-table-column prop="unresolved" label="在办" width="80" align="center"/>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import http from '../api/http'

const router = useRouter()
const data = reactive({})
const alerts = reactive({})
const causeChart = ref()
const statusChart = ref()
const reasonChart = ref()

const kpis = ref([])

const alertCases = ref([])

function openCase(id) {
  router.push(`/cases/${id}`)
}

async function load() {
  const [ov, al] = await Promise.all([
    http.get('/stats/overview'),
    http.get('/stats/alerts')
  ])
  Object.assign(data, ov)
  Object.assign(alerts, al)
  const k = ov.kpi
  kpis.value = [
    { label: '案件总数', value: k.totalCases, sub: `待处理 ${k.pendingCases} 件`, color: '#1d4e89' },
    { label: '调解成功率', value: k.agreementRate + '%', sub: `达成协议 ${k.agreementCount} 件`, color: '#67c23a' },
    { label: '协议履行率', value: k.performRate + '%', sub: `履行完毕 ${k.completedCount} 件`, color: '#409eff' },
    { label: '转立案率', value: k.transferRate + '%', sub: `转立案 ${k.transferredCount} 件 / 不成 ${k.failedCount} 件`, color: '#f56c6c' }
  ]
  alertCases.value = [
    ...(al.unreachableCases || []).map(x => ({ ...x })),
    ...(al.missingCases || []),
    ...(al.rescheduledCases || [])
  ].filter((v, i, arr) => arr.findIndex(x => x.id === v.id) === i)

  await nextTick()
  renderCharts()
}

function renderCharts() {
  const cause = echarts.init(causeChart.value)
  cause.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['案件数', '达成协议', '转立案'], bottom: 0 },
    grid: { top: 20, left: 40, right: 15, bottom: 40 },
    xAxis: { type: 'category', data: (data.byCause || []).map(x => x.cause), axisLabel: { interval: 0, fontSize: 11 } },
    yAxis: { type: 'value' },
    series: [
      { name: '案件数', type: 'bar', data: (data.byCause || []).map(x => x.total), itemStyle: { color: '#409eff' } },
      { name: '达成协议', type: 'bar', data: (data.byCause || []).map(x => x.agreed), itemStyle: { color: '#67c23a' } },
      { name: '转立案', type: 'bar', data: (data.byCause || []).map(x => x.transferred), itemStyle: { color: '#f56c6c' } }
    ]
  })

  const sd = Object.entries(data.statusDist || {})
  const status = echarts.init(statusChart.value)
  status.setOption({
    tooltip: { trigger: 'item' },
    legend: { type: 'scroll', bottom: 0 },
    series: [{
      type: 'pie', radius: ['40%', '68%'], center: ['50%', '45%'],
      data: sd.map(([name, value]) => ({ name, value })),
      label: { formatter: '{b}: {c}' }
    }]
  })

  const rd = Object.entries(data.transferReasons || {})
  const reason = echarts.init(reasonChart.value)
  reason.setOption({
    tooltip: { trigger: 'axis' },
    grid: { top: 20, left: 100, right: 30, bottom: 20 },
    xAxis: { type: 'value', minInterval: 1 },
    yAxis: { type: 'category', data: rd.map(x => x[0]) },
    series: [{
      type: 'bar', data: rd.map(x => x[1]),
      itemStyle: { color: '#e6a23c' },
      label: { show: true, position: 'right' }
    }]
  })

  window.addEventListener('resize', () => { cause.resize(); status.resize(); reason.resize() })
}

onMounted(load)
</script>

<style scoped>
.kpi-card { margin-bottom: 12px; }
.kpi-label { color: #7b8794; font-size: 13px; }
.kpi-value { font-size: 30px; font-weight: 700; margin: 4px 0; }
.kpi-sub { color: #9aa5b1; font-size: 12px; }
</style>
