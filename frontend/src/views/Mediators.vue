<template>
  <el-card shadow="never">
    <template #header><b>调解员名册（窗口分流与智能排期依据）</b></template>
    <el-table :data="rows" stripe>
      <el-table-column prop="realName" label="姓名" width="110" />
      <el-table-column prop="mediationRoom" label="调解室" width="130" />
      <el-table-column prop="phone" label="联系电话" width="140" />
      <el-table-column label="专长案由" min-width="260">
        <template #default="{ row }">
          <el-tag v-for="t in (row.expertise || '').split(',').filter(Boolean)" :key="t" size="small" class="tag">
            {{ TYPE_LABEL[t] || t }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="办案容量" width="110">
        <template #default="{ row }">{{ row.capacity }} 件</template>
      </el-table-column>
      <el-table-column label="账号" width="120">
        <template #default="{ row }">{{ row.username }}</template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import api from '../api.js'
import { CASE_TYPES as TYPE_LABEL } from '../constants.js'
const rows = ref([])
onMounted(async () => { rows.value = await api.get('/mediators') })
</script>

<style scoped>
.tag { margin-right: 6px; }
</style>
