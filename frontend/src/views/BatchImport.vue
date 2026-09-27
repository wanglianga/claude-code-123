<template>
  <div>
    <el-card shadow="never" class="section-card">
      <template #header><b>批量案件导入（CSV，UTF-8 编码）</b></template>
      <el-alert type="info" :closable="false" show-icon style="margin-bottom:12px"
                title="表头顺序：标题,案由,诉求金额,申请人,申请人电话,被申请人,被申请人电话,被申请人地址,证据清单,愿意线上调解(是/否),紧急程度(普通/紧急/特急),是否外地(是/否)"/>
      <el-upload :show-file-list="false" :before-upload="beforeUpload" accept=".csv"
                 :http-request="doUpload">
        <el-button type="primary"><el-icon><Upload/></el-icon> 选择 CSV 文件导入</el-button>
      </el-upload>
      <el-button style="margin-left:10px" @click="downloadTemplate">下载导入模板</el-button>
      <el-descriptions v-if="lastBatch" :column="4" border size="small" style="margin-top:16px">
        <el-descriptions-item label="文件">{{ lastBatch.fileName }}</el-descriptions-item>
        <el-descriptions-item label="总行数">{{ lastBatch.totalCount }}</el-descriptions-item>
        <el-descriptions-item label="成功">
          <span style="color:#67c23a">{{ lastBatch.successCount }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="失败">
          <span :style="{color:lastBatch.failCount?'#f56c6c':'inherit'}">{{ lastBatch.failCount }}</span>
        </el-descriptions-item>
      </el-descriptions>
      <el-input v-if="lastBatch?.failDetail" type="textarea" :rows="5" readonly
                :model-value="lastBatch.failDetail" style="margin-top:10px"/>
    </el-card>

    <el-card shadow="never">
      <template #header><b>历史导入批次</b></template>
      <el-table :data="batches" size="small" stripe>
        <el-table-column prop="fileName" label="文件" min-width="180"/>
        <el-table-column prop="totalCount" label="总数" width="80" align="center"/>
        <el-table-column label="成功" width="80" align="center">
          <template #default="{row}"><span style="color:#67c23a">{{ row.successCount }}</span></template>
        </el-table-column>
        <el-table-column label="失败" width="80" align="center">
          <template #default="{row}"><span :style="{color:row.failCount?'#f56c6c':'inherit'}">{{ row.failCount }}</span></template>
        </el-table-column>
        <el-table-column label="时间" width="170">
          <template #default="{row}">{{ fmt(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column prop="failDetail" label="失败明细" show-overflow-tooltip min-width="200"/>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import http from '../api/http'

const batches = ref([])
const lastBatch = ref(null)

function beforeUpload(file) {
  if (!file.name.endsWith('.csv')) {
    ElMessage.error('仅支持 CSV 文件')
    return false
  }
  return true
}

async function doUpload(opt) {
  const fd = new FormData()
  fd.append('file', opt.file)
  try {
    lastBatch.value = await http.post('/imports/cases', fd, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    ElMessage.success(`导入完成：成功 ${lastBatch.value.successCount}，失败 ${lastBatch.value.failCount}`)
    load()
  } catch (e) {
    // http 拦截器已提示
  }
}

function downloadTemplate() {
  const content = '标题,案由,诉求金额,申请人,申请人电话,被申请人,被申请人电话,被申请人地址,证据清单,愿意线上调解,紧急程度,是否外地\n'
    + '陈某诉刘某民间借贷纠纷,民间借贷,15000,陈某,13811112222,刘某,13922223333,本市某区某路1号,借条;转账记录,是,紧急,否\n'
    + '某物业诉周某物业费纠纷,物业费,2600,某物业公司,010-66668888,周某,13733334444,本市某小区2-301,物业合同;欠费明细,否,普通,否\n'
  const blob = new Blob(['﻿' + content], { type: 'text/csv;charset=utf-8' })
  const a = document.createElement('a')
  a.href = URL.createObjectURL(blob)
  a.download = '案件导入模板.csv'
  a.click()
}

async function load() {
  batches.value = await http.get('/imports/batches')
}

function fmt(t) { return t ? t.replace('T', ' ').substring(0, 16) : '' }
onMounted(load)
</script>
