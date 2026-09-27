<template>
  <div>
    <!-- 异常处置 -->
    <el-card shadow="never" class="section-card" v-if="canOperate">
      <template #header><b>异常情况登记（同步进入连续过程材料）</b></template>
      <el-space wrap>
        <el-button type="danger" plain :disabled="caseInfo.respondentUnreachable" @click="openFlag('unreachable')">
          登记被申请人失联
        </el-button>
        <el-button type="success" plain v-if="caseInfo.respondentUnreachable" @click="doReached">恢复联系</el-button>
        <el-button type="warning" plain :disabled="caseInfo.evidenceMissing" @click="openFlag('missing')">
          登记证据材料缺失
        </el-button>
        <el-button type="primary" plain @click="evidenceVisible=true">补充/核验证据</el-button>
      </el-space>
    </el-card>

    <!-- 证据清单 -->
    <el-card shadow="never" class="section-card">
      <template #header><b>证据清单</b></template>
      <el-table :data="evidences" size="small" stripe>
        <el-table-column prop="name" label="证据名称" min-width="200"/>
        <el-table-column prop="evidenceType" label="类型" width="100"/>
        <el-table-column label="核验" width="90" align="center">
          <template #default="{row}">
            <el-tag size="small" :type="row.verified?'success':'info'">{{ row.verified ? '已核验' : '待核验' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="160"/>
      </el-table>
    </el-card>

    <!-- 调解笔录 -->
    <el-card shadow="never">
      <template #header>
        <div class="card-head">
          <b>调解笔录（争议焦点 / 双方让步 / 付款方案 / 担保 / 履行期限）</b>
          <el-button type="primary" size="small" @click="openRecord" v-if="canOperate">新增调解记录</el-button>
        </div>
      </template>
      <el-empty v-if="!records.length" description="暂无调解记录"/>
      <el-collapse v-else>
        <el-collapse-item v-for="r in records" :key="r.id" :name="r.id">
          <template #title>
            <span>{{ fmt(r.sessionTime) }}　</span>
            <el-tag size="small" :type="resultType(r.result)">{{ resultText(r.result) }}</el-tag>
            <span class="muted" style="margin-left:8px">{{ r.participants }}</span>
          </template>
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="争议焦点">{{ r.focusIssues || '—' }}</el-descriptions-item>
            <el-descriptions-item label="双方让步">{{ r.concessions || '—' }}</el-descriptions-item>
            <el-descriptions-item label="付款方案">{{ r.paymentPlan || '—' }}</el-descriptions-item>
            <el-descriptions-item label="担保人">{{ r.guarantor || '—' }}</el-descriptions-item>
            <el-descriptions-item label="履行期限">{{ r.performanceDeadline || '—' }}</el-descriptions-item>
            <el-descriptions-item label="申请司法确认">
              <el-tag size="small" :type="r.applyJudicialConfirm?'success':'info'">
                {{ r.applyJudicialConfirm ? '是' : '否' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="备注">{{ r.remark || '—' }}</el-descriptions-item>
          </el-descriptions>
        </el-collapse-item>
      </el-collapse>
    </el-card>

    <!-- 失联/缺料弹窗 -->
    <el-dialog v-model="flagVisible" :title="flagType==='unreachable'?'登记被申请人失联':'登记证据材料缺失'" width="480px">
      <el-input v-model="flagRemark" type="textarea" :rows="3"
                :placeholder="flagType==='unreachable'?'联系时间、方式、处置经过……':'缺失材料名称、补正要求与期限……'"/>
      <template #footer>
        <el-button @click="flagVisible=false">取消</el-button>
        <el-button type="primary" @click="submitFlag">提交登记</el-button>
      </template>
    </el-dialog>

    <!-- 补充证据 -->
    <el-dialog v-model="evidenceVisible" title="补充/核验证据" width="460px">
      <el-form label-width="80px">
        <el-form-item label="证据名称"><el-input v-model="evForm.name"/></el-form-item>
        <el-form-item label="证据类型">
          <el-select v-model="evForm.evidenceType" style="width:100%">
            <el-option v-for="t in ['书证','物证','电子数据','视听资料','证人证言']" :key="t" :label="t" :value="t"/>
          </el-select>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="evForm.remark" type="textarea" :rows="2"/></el-form-item>
        <el-form-item label="已核验"><el-switch v-model="evForm.verified"/></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="evidenceVisible=false">取消</el-button>
        <el-button type="primary" @click="submitEvidence">保存</el-button>
      </template>
    </el-dialog>

    <!-- 新增笔录 -->
    <el-dialog v-model="recordVisible" title="新增调解记录" width="640px">
      <el-form :model="recordForm" label-width="96px" size="small">
        <el-form-item label="调解时间">
          <el-date-picker v-model="recordForm.sessionTime" type="datetime"
                          value-format="YYYY-MM-DDTHH:mm:ss" style="width:100%"/>
        </el-form-item>
        <el-form-item label="参会人员"><el-input v-model="recordForm.participants"/></el-form-item>
        <el-form-item label="争议焦点"><el-input v-model="recordForm.focusIssues" type="textarea" :rows="2"/></el-form-item>
        <el-form-item label="双方让步"><el-input v-model="recordForm.concessions" type="textarea" :rows="2"/></el-form-item>
        <el-form-item label="付款方案"><el-input v-model="recordForm.paymentPlan" type="textarea" :rows="2"
                      placeholder="如：分三期，9月10日还2万……"/></el-form-item>
        <el-row :gutter="8">
          <el-col :span="12">
            <el-form-item label="担保人"><el-input v-model="recordForm.guarantor"/></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="履行期限">
              <el-date-picker v-model="recordForm.performanceDeadline" type="date"
                              value-format="YYYY-MM-DD" style="width:100%"/>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="调解结果">
          <el-radio-group v-model="recordForm.result">
            <el-radio value="PENDING">继续调解</el-radio>
            <el-radio value="AGREED">达成一致</el-radio>
            <el-radio value="NO_AGREEMENT">未达成</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="司法确认"><el-switch v-model="recordForm.applyJudicialConfirm"/></el-form-item>
        <el-form-item label="备注"><el-input v-model="recordForm.remark" type="textarea" :rows="2"/></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="recordVisible=false">取消</el-button>
        <el-button type="primary" @click="submitRecord">保存记录</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '../api/http'
import { auth } from '../api/auth'

const props = defineProps({
  caseInfo: Object,
  evidences: { type: Array, default: () => [] },
  records: { type: Array, default: () => [] }
})
const emit = defineEmits(['changed'])

const canOperate = computed(() => ['MEDIATOR', 'WINDOW', 'ADMIN'].includes(auth.role))

const flagVisible = ref(false)
const flagType = ref('unreachable')
const flagRemark = ref('')
const evidenceVisible = ref(false)
const recordVisible = ref(false)

const evForm = reactive({ name: '', evidenceType: '书证', remark: '', verified: false })
const recordForm = reactive({
  sessionTime: '', participants: '', focusIssues: '', concessions: '', paymentPlan: '',
  guarantor: '', performanceDeadline: '', result: 'PENDING', applyJudicialConfirm: false, remark: ''
})

function openFlag(type) {
  flagType.value = type
  flagRemark.value = ''
  flagVisible.value = true
}

async function submitFlag() {
  const url = flagType.value === 'unreachable'
    ? `/mediation/cases/${props.caseInfo.id}/unreachable`
    : `/mediation/cases/${props.caseInfo.id}/evidence-missing`
  await http.post(url, { remark: flagRemark.value })
  ElMessage.success('已登记并写入案件时间线')
  flagVisible.value = false
  emit('changed')
}

async function doReached() {
  const { value } = await ElMessageBox.prompt('请填写恢复联系情况说明', '恢复联系', { confirmButtonText: '确定' })
  await http.post(`/mediation/cases/${props.caseInfo.id}/reached`, { remark: value })
  ElMessage.success('已标记恢复联系')
  emit('changed')
}

async function submitEvidence() {
  if (!evForm.name) return ElMessage.warning('请输入证据名称')
  await http.post('/mediation/evidence', { caseId: props.caseInfo.id, ...evForm })
  ElMessage.success('证据已补充')
  evidenceVisible.value = false
  evForm.name = ''; evForm.remark = ''; evForm.verified = false
  emit('changed')
}

function openRecord() {
  Object.assign(recordForm, {
    sessionTime: '', participants: '', focusIssues: '', concessions: '', paymentPlan: '',
    guarantor: '', performanceDeadline: '', result: 'PENDING', applyJudicialConfirm: false, remark: ''
  })
  recordVisible.value = true
}

async function submitRecord() {
  await http.post('/mediation/records', { caseId: props.caseInfo.id, ...recordForm })
  ElMessage.success('调解记录已保存')
  recordVisible.value = false
  emit('changed')
}

function fmt(t) { return t ? t.replace('T', ' ').substring(0, 16) : '' }
function resultText(r) { return { AGREED: '达成一致', NO_AGREEMENT: '未达成', PENDING: '继续调解' }[r] || r }
function resultType(r) { return { AGREED: 'success', NO_AGREEMENT: 'danger', PENDING: 'warning' }[r] || 'info' }
</script>

<style scoped>
.card-head { display: flex; justify-content: space-between; align-items: center; }
</style>
