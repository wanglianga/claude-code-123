<template>
  <div>
    <el-card shadow="never">
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="关键字">
          <el-input v-model="query.keyword" placeholder="编号/申请人/被申请人" clearable style="width:220px" @keyup.enter="load" />
        </el-form-item>
        <el-form-item label="案由">
          <el-select v-model="query.type" placeholder="全部" clearable style="width:170px">
            <el-option v-for="(l, k) in CASE_TYPES" :key="k" :label="l" :value="k" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width:150px">
            <el-option v-for="(l, k) in CASE_STATUS" :key="k" :label="l" :value="k" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load">查询</el-button>
          <el-button @click="query = { keyword: '', type: '', status: '' }; load()">重置</el-button>
          <el-button type="success" @click="$router.push('/cases/new')">
            <el-icon><Plus /></el-icon>新建申请
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="mt" shadow="never">
      <el-table :data="rows" stripe @row-click="r => $router.push(`/cases/${r.id}`)">
        <el-table-column prop="caseNo" label="调解编号" width="160" />
        <el-table-column label="案由" width="150">
          <template #default="{ row }">{{ CASE_TYPES[row.caseType] }}</template>
        </el-table-column>
        <el-table-column prop="applicantName" label="申请人" width="110" />
        <el-table-column prop="respondentName" label="被申请人" width="120" />
        <el-table-column label="诉求金额(元)" width="130" align="right">
          <template #default="{ row }">{{ money(row.claimAmount) }}</template>
        </el-table-column>
        <el-table-column label="紧急" width="80">
          <template #default="{ row }">
            <el-tag size="small" :type="URGENCY_TAG[row.urgency]">{{ URGENCY[row.urgency] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="风险" min-width="200">
          <template #default="{ row }">
            <el-tag v-if="row.respondentMissing" type="danger" size="small" class="risk">失联</el-tag>
            <el-tag v-if="row.evidenceComplete === false" type="danger" size="small" class="risk">缺材料</el-tag>
            <el-tag v-if="row.rescheduleCount >= 2" type="warning" size="small" class="risk">改期{{ row.rescheduleCount }}次</el-tag>
            <el-tag v-if="row.transferRequested" type="warning" size="small" class="risk">申请转立案</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="130">
          <template #default="{ row }">
            <el-tag size="small" :type="STATUS_TAG[row.status]">{{ CASE_STATUS[row.status] }}</el-tag>
          </template>
        </el-table-column>
        <template #empty>暂无案件</template>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import api from '../api.js'
import { auth } from '../store.js'
import { CASE_TYPES, CASE_STATUS, STATUS_TAG, URGENCY, URGENCY_TAG } from '../constants.js'

const query = reactive({ keyword: '', type: '', status: '' })
const rows = ref([])

const money = v => v == null ? '—' : Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2 })

const load = async () => {
  const params = {}
  if (query.keyword) params.keyword = query.keyword
  if (query.type) params.type = query.type
  if (query.status) params.status = query.status
  rows.value = await api.get('/cases', { params })
}
onMounted(load)
</script>

<style scoped>
.mt { margin-top: 14px; }
.risk { margin-right: 4px; }
:deep(.el-table__row) { cursor: pointer; }
</style>
