<template>
  <div>
    <div class="toolbar">
      <el-button type="primary" @click="openDialog" v-if="canOperate">
        <el-icon><Calendar /></el-icon> {{ hasActive ? '发起改期' : '安排调解' }}
      </el-button>
      <span class="muted" v-if="caseInfo.rescheduleCount>=2">
        <el-icon color="#e6a23c"><Warning /></el-icon>
        已改期 {{ caseInfo.rescheduleCount }} 次，将作为连续过程材料并影响转立案裁量
      </span>
    </div>

    <el-table :data="schedules" size="small" stripe>
      <el-table-column label="版本" width="70" align="center">
        <template #default="{row}">第{{ row.version }}版</template>
      </el-table-column>
      <el-table-column label="调解时间" min-width="200">
        <template #default="{row}">
          {{ fmt(row.scheduledStart) }} ~ {{ fmtTime(row.scheduledEnd) }}
        </template>
      </el-table-column>
      <el-table-column label="方式" width="80">
        <template #default="{row}">
          <el-tag size="small" :type="row.mode==='ONLINE'?'primary':'info'">
            {{ row.mode==='ONLINE' ? '线上' : '线下' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="location" label="地点/会议链接" min-width="220"/>
      <el-table-column label="状态" width="100">
        <template #default="{row}">
          <el-tag size="small" :type="schedType(row.status)">{{ schedText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180">
        <template #default="{row}">
          <el-button v-if="row.status==='PROPOSED'" link type="primary" size="small" @click="confirm(row)">当事人确认</el-button>
          <el-button v-if="['PROPOSED','CONFIRMED'].includes(row.status) && canOperate"
                     link type="success" size="small" @click="complete(row)">召开完成</el-button>
        </template>
      </el-table-column>
      <el-table-column prop="note" label="备注" min-width="140"/>
    </el-table>

    <el-dialog v-model="visible" :title="hasActive ? '改期（版本递增，旧排期自动取消）' : '安排调解'" width="520px">
      <el-form label-width="96px">
        <el-form-item label="开始时间">
          <el-date-picker v-model="form.start" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss"
                          placeholder="选择开始时间" style="width:100%"/>
        </el-form-item>
        <el-form-item label="结束时间">
          <el-date-picker v-model="form.end" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss"
                          placeholder="默认2小时" style="width:100%"/>
        </el-form-item>
        <el-form-item label="调解方式">
          <el-radio-group v-model="form.mode">
            <el-radio value="ONLINE">线上</el-radio>
            <el-radio value="OFFLINE">线下</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="地点/链接">
          <el-input v-model="form.location" :placeholder="form.mode==='ONLINE'?'线上会议链接':'调解室地点'"/>
        </el-form-item>
        <el-form-item label="改期原因/备注">
          <el-input v-model="form.note" type="textarea" :rows="2"/>
        </el-form-item>
        <el-form-item label="双方确认">
          <el-switch v-model="form.confirmed"/>
          <span class="muted">双方已确认可用时间</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible=false">取消</el-button>
        <el-button type="primary" @click="submit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import http from '../api/http'
import { auth } from '../api/auth'

const props = defineProps({
  caseInfo: Object,
  schedules: { type: Array, default: () => [] }
})
const emit = defineEmits(['changed'])

const canOperate = computed(() => ['WINDOW', 'MEDIATOR', 'ADMIN'].includes(auth.role))
const hasActive = computed(() => props.schedules.some(s => ['PROPOSED', 'CONFIRMED'].includes(s.status)))

const visible = ref(false)
const form = reactive({ start: '', end: '', mode: 'ONLINE', location: '', note: '', confirmed: true })

function openDialog() {
  form.start = ''; form.end = ''
  form.mode = props.caseInfo.mediationMode || 'ONLINE'
  form.location = ''; form.note = ''; form.confirmed = true
  visible.value = true
}

async function submit() {
  if (!form.start) return ElMessage.warning('请选择开始时间')
  await http.post('/schedules', { caseId: props.caseInfo.id, ...form })
  ElMessage.success(hasActive.value ? '改期成功，旧排期已取消' : '排期成功')
  visible.value = false
  emit('changed')
}

async function confirm(row) {
  await http.post(`/schedules/${row.id}/confirm`)
  ElMessage.success('当事人已确认时间')
  emit('changed')
}

async function complete(row) {
  await http.post(`/schedules/${row.id}/complete`, { note: '' })
  ElMessage.success('调解会议已完成')
  emit('changed')
}

function fmt(t) { return t ? t.replace('T', ' ').substring(0, 16) : '' }
function fmtTime(t) { return t ? t.substring(11, 16) : '' }
function schedText(s) { return { PROPOSED: '待确认', CONFIRMED: '已确认', COMPLETED: '已完成', CANCELLED: '已取消' }[s] }
function schedType(s) { return { PROPOSED: 'warning', CONFIRMED: 'primary', COMPLETED: 'success', CANCELLED: 'info' }[s] }
</script>

<style scoped>
.toolbar { margin-bottom: 10px; display: flex; align-items: center; gap: 12px; }
</style>
