<template>
  <div>
    <el-card shadow="never" class="mb">
      <template #header><b>批量案件导入（CSV）</b></template>
      <el-alert type="info" :closable="false" show-icon class="mb"
                title="表头：案由,申请人,申请人电话,被申请人,被申请人电话,被申请人地址,是否外地,诉求金额,诉求摘要,证据清单,愿意线上调解,紧急程度。支持中文案由与引号包裹字段。" />
      <div class="ops">
        <el-upload :show-file-list="false" :before-upload="onFile" accept=".csv,text/csv">
          <el-button type="primary"><el-icon><Upload /></el-icon>选择 CSV 文件</el-button>
        </el-upload>
        <el-button @click="loadTemplate">载入模板示例</el-button>
        <el-button type="success" :disabled="!csv.trim()" @click="doImport">开始导入</el-button>
      </div>
      <el-input v-model="csv" type="textarea" :rows="12" class="mt"
                placeholder="可直接粘贴 CSV 内容，或选择文件载入" />
    </el-card>

    <el-card v-if="result" shadow="never">
      <template #header><b>导入结果</b></template>
      <el-result :icon="result.failed === 0 ? 'success' : 'warning'"
                 :title="`成功 ${result.success} 件，失败 ${result.failed} 行`">
        <template #sub-title>
          <div v-if="result.caseNos?.length" class="nos">生成编号：{{ result.caseNos.join('，') }}</div>
          <div v-for="(e, i) in result.errors" :key="i" class="err">{{ e }}</div>
        </template>
      </el-result>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../api.js'

const csv = ref('')
const result = ref(null)

const onFile = file => {
  const reader = new FileReader()
  reader.onload = () => { csv.value = reader.result }
  reader.readAsText(file, 'UTF-8')
  return false
}

const loadTemplate = async () => {
  csv.value = await api.get('/import/template')
}

const doImport = async () => {
  result.value = await api.post('/import/cases', { csv: csv.value })
  if (result.value.success > 0) ElMessage.success(`成功导入 ${result.value.success} 件`)
}
</script>

<style scoped>
.mb { margin-bottom: 14px; }
.mt { margin-top: 12px; }
.ops { display: flex; gap: 10px; }
.nos { color: #67c23a; margin-bottom: 6px; }
.err { color: #f56c6c; font-size: 13px; }
</style>
