<template>
  <div>
    <!-- 窗口分流 -->
    <el-alert v-if="c.status === 'PENDING_TRIAGE'" type="error" show-icon :closable="false" class="mb"
              title="案件待立案窗口分流：核验材料 → 确定线上/线下 → 指派调解员" />
    <el-card v-if="canTriage" shadow="never" class="mb">
      <template #header><b>立案窗口分流</b></template>
      <el-form :model="triage" label-width="110px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="指派调解员">
              <el-select v-model="triage.mediatorId" placeholder="选择调解员" style="width:100%">
                <el-option v-for="m in mediators" :key="m.id"
                           :label="`${m.realName}（${m.mediationRoom || ''}）专长：${expertiseText(m.expertise)}`"
                           :value="m.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="调解方式">
              <el-radio-group v-model="triage.channel">
                <el-radio-button value="ONLINE">线上调解</el-radio-button>
                <el-radio-button value="OFFLINE">线下调解</el-radio-button>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="材料核验">
              <el-switch v-model="evidenceOk" active-text="材料齐备" inactive-text="材料缺失（先行调解并登记补正）" />
            </el-form-item>
          </el-col>
          <el-col v-if="!evidenceOk" :span="12">
            <el-form-item label="缺失材料"><el-input v-model="triage.evidenceGapNote" placeholder="如：缺少收费标准备案表" /></el-form-item>
          </el-col>
          <el-col :span="24"><el-form-item label="分流意见"><el-input v-model="triage.triageOpinion" type="textarea" :rows="2" /></el-form-item></el-col>
        </el-row>
        <el-button type="primary" @click="doTriage">完成分流</el-button>
      </el-form>
    </el-card>

    <!-- 排期 -->
    <el-card shadow="never">
      <template #header>
        <div class="head">
          <b>调解排期记录</b>
          <el-button v-if="canSchedule" type="primary" size="small" @click="openSchedule()">
            <el-icon><Calendar /></el-icon>{{ c.status === 'SCHEDULED' || c.status === 'MEDIATING' ? '追加排期' : '安排调解' }}
          </el-button>
        </div>
      </template>
      <el-table :data="data.schedules" stripe>
        <el-table-column label="次数" width="70"><template #default="{ row }">第{{ row.roundNo }}次</template></el-table-column>
        <el-table-column label="时间" min-width="230">
          <template #default="{ row }">{{ fmt(row.startTime) }} ~ {{ fmt(row.endTime).substring(11) }}</template>
        </el-table-column>
        <el-table-column label="方式" width="90">
          <template #default="{ row }">{{ CHANNEL[row.channel] }}</template>
        </el-table-column>
        <el-table-column prop="location" label="地点/会议" min-width="180" />
        <el-table-column label="出席确认" width="150">
          <template #default="{ row }">
            <el-tag size="small" :type="row.applicantConfirmed ? 'success' : 'info'" class="ck">申{{ row.applicantConfirmed ? '✓' : '…' }}</el-tag>
            <el-tag size="small" :type="row.respondentConfirmed ? 'success' : 'info'" class="ck">被{{ row.respondentConfirmed ? '✓' : '…' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="scheduleTag(row.status)">{{ scheduleLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="170">
          <template #default="{ row }">
            <template v-if="row.status === 'PLANNED'">
              <el-button v-if="auth.is('MEDIATOR','WINDOW','ADMIN')" size="small" link type="warning" @click="openReschedule(row)">改期</el-button>
              <el-button v-if="auth.is('PARTY')" size="small" link type="primary" @click="confirmAttend(row)">确认出席</el-button>
            </template>
          </template>
        </el-table-column>
        <el-table-column prop="note" label="备注" min-width="160" />
        <template #empty>暂无排期记录</template>
      </el-table>
    </el-card>

    <!-- 排期弹窗 -->
    <el-dialog v-model="schedVisible" :title="editing ? '改期（原记录保留留痕）' : '安排调解'" width="620px">
      <el-form label-width="100px">
        <template v-if="!editing">
          <el-form-item label="调解员">
            <el-select v-model="sched.mediatorId" style="width:100%" @change="loadSuggest">
              <el-option v-for="m in mediators" :key="m.id" :label="m.realName" :value="m.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="调解方式">
            <el-radio-group v-model="sched.channel">
              <el-radio-button value="ONLINE">线上</el-radio-button>
              <el-radio-button value="OFFLINE">线下</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="地点/会议">
            <el-input v-model="sched.location" :placeholder="sched.channel === 'ONLINE' ? '线上调解室视频号/链接' : '线下调解室'" />
          </el-form-item>
          <el-form-item label="时间">
            <el-date-picker v-model="timeRange" type="datetimerange" range-separator="至"
                            start-placeholder="开始" end-placeholder="结束" value-format="YYYY-MM-DDTHH:mm:ss"
                            style="width:100%" @change="loadSuggest" />
          </el-form-item>
          <el-form-item label="智能推荐">
            <div v-if="suggests.length" class="sugg">
              <el-tag v-for="s in suggests" :key="s.id" class="sugg-tag"
                      :type="s.score >= 30 ? 'success' : s.score >= 10 ? 'warning' : 'info'"
                      @click="sched.mediatorId = s.id">
                {{ s.realName }} 在办{{ s.activeCount }}/{{ s.capacity }}
                <span v-if="s.expertiseMatch">·专长✓</span>
                <span :style="{ color: s.timeFree ? '#67c23a' : '#f56c6c' }">{{ s.timeFree ? '·时间空闲✓' : '·时间冲突' }}</span>
              </el-tag>
              <div class="tip">按专长匹配、在办量、时间冲突综合推荐，点击标签可直接选用</div>
            </div>
            <span v-else class="tip">选择时间后显示调解员推荐</span>
          </el-form-item>
        </template>
        <template v-else>
          <el-form-item label="改期原因">
            <el-select v-model="resched.reason" allow-create filterable style="width:100%">
              <el-option value="双方时间冲突" label="双方时间冲突" />
              <el-option value="被申请人务工外出" label="被申请人务工外出" />
              <el-option value="申请人身体不适" label="申请人身体不适" />
              <el-option value="被申请人失联待联系" label="被申请人失联待联系" />
              <el-option value="补充证据需要时间" label="补充证据需要时间" />
            </el-select>
          </el-form-item>
          <el-form-item label="新时间">
            <el-date-picker v-model="reschedRange" type="datetimerange" range-separator="至"
                            start-placeholder="新开始" end-placeholder="新结束"
                            value-format="YYYY-MM-DDTHH:mm:ss" style="width:100%" />
          </el-form-item>
        </template>
        <el-form-item label="备注"><el-input v-model="sched.note" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="schedVisible = false">取消</el-button>
        <el-button type="primary" @click="submitSchedule">{{ editing ? '确认改期' : '确认排期' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../../api.js'
import { auth } from '../../store.js'
import { CHANNEL } from '../../constants.js'

const props = defineProps({ data: Object })
const emit = defineEmits(['changed'])
const c = computed(() => props.data.case)

const mediators = ref([])
const loadMediators = async () => { mediators.value = await api.get('/mediators') }
onMounted(loadMediators)

const expertiseText = e => (e || '').split(',').map(x => ({
  LOAN: '借贷', PROPERTY_FEE: '物业', TRAFFIC: '交通', NEIGHBOR: '邻里', SALES: '买卖'
}[x] || x)).join('、')

const canTriage = computed(() => auth.is('WINDOW', 'ADMIN') && (c.value.status === 'PENDING_TRIAGE' || c.value.status === 'TRIAGED'))
const canSchedule = computed(() => auth.is('WINDOW', 'MEDIATOR', 'ADMIN')
  && ['TRIAGED', 'SCHEDULED', 'MEDIATING', 'PENDING_TRIAGE'].includes(c.value.status))

const evidenceOk = ref(true)
const triage = reactive({ mediatorId: null, channel: 'ONLINE', triageOpinion: '', evidenceComplete: true, evidenceGapNote: '' })

const doTriage = async () => {
  if (!triage.mediatorId) return ElMessage.warning('请选择调解员')
  triage.evidenceComplete = evidenceOk.value
  await api.post(`/cases/${c.value.id}/triage`, triage)
  ElMessage.success('分流完成，案件进入排期环节')
  emit('changed')
}

// ---- 排期 ----
const schedVisible = ref(false)
const editing = ref(null)
const sched = reactive({ mediatorId: null, channel: 'ONLINE', location: '', startTime: '', endTime: '', note: '' })
const timeRange = ref([])
const resched = reactive({ reason: '双方时间冲突' })
const reschedRange = ref([])
const suggests = ref([])

const openSchedule = () => {
  editing.value = null
  sched.mediatorId = c.value.mediatorId
  sched.channel = c.value.channel || 'ONLINE'
  sched.location = ''
  sched.note = ''
  timeRange.value = []
  suggests.value = []
  schedVisible.value = true
}
const openReschedule = row => {
  editing.value = row
  reschedRange.value = []
  schedVisible.value = true
}

const loadSuggest = async () => {
  sched.mediatorId = sched.mediatorId || c.value.mediatorId
  if (timeRange.value?.length === 2) {
    suggests.value = await api.get(`/cases/${c.value.id}/mediator-suggest`, {
      params: { start: timeRange.value[0], end: timeRange.value[1] }
    })
  }
}

const submitSchedule = async () => {
  if (editing.value) {
    if (!reschedRange.value?.length) return ElMessage.warning('请选择新的时间')
    await api.post(`/schedules/${editing.value.id}/reschedule`, {
      reason: resched.reason, startTime: reschedRange.value[0], endTime: reschedRange.value[1]
    })
    ElMessage.success('改期完成，原排期已保留，改期次数已累计')
  } else {
    if (!sched.mediatorId || !timeRange.value?.length) return ElMessage.warning('请选择调解员和时间')
    await api.post(`/cases/${c.value.id}/schedules`, {
      mediatorId: sched.mediatorId, channel: sched.channel, location: sched.location,
      startTime: timeRange.value[0], endTime: timeRange.value[1], note: sched.note
    })
    ElMessage.success('排期成功，已自动向双方生成电子送达待办')
  }
  schedVisible.value = false
  emit('changed')
}

const confirmAttend = async row => {
  const party = auth.user?.id === c.value.applicantUserId ? 'APPLICANT' : 'APPLICANT'
  await api.post(`/schedules/${row.id}/confirm`, { party })
  ElMessage.success('已确认出席')
  emit('changed')
}

const fmt = t => t ? t.replace('T', ' ').substring(0, 16) : ''
const scheduleLabel = s => ({ PLANNED: '已排期', RESCHEDULED: '已改期', FINISHED: '已完成', CANCELLED: '已取消' }[s])
const scheduleTag = s => ({ PLANNED: 'primary', RESCHEDULED: 'warning', FINISHED: 'success', CANCELLED: 'info' }[s])
</script>

<style scoped>
.mb { margin-bottom: 14px; }
.head { display: flex; justify-content: space-between; align-items: center; }
.ck { margin-right: 4px; }
.sugg { display: flex; flex-wrap: wrap; gap: 8px; align-items: center; }
.sugg-tag { cursor: pointer; }
.tip { color: #909399; font-size: 12px; margin-left: 4px; }
</style>
