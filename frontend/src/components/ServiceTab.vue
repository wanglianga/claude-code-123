<template>
  <div>
    <!-- 电子送达 -->
    <el-card shadow="never" class="section-card">
      <template #header>
        <div class="card-head"><b>电子送达</b>
          <el-button type="primary" size="small" @click="serveVisible=true" v-if="canServe">发起送达</el-button>
        </div>
      </template>
      <el-table :data="services" size="small" stripe>
        <el-table-column label="时间" width="150">
          <template #default="{row}">{{ fmt(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column prop="documentName" label="文书" min-width="140"/>
        <el-table-column label="受送达人" width="160">
          <template #default="{row}">{{ row.receiverName }} <span class="muted">{{ row.receiverPhone }}</span></template>
        </el-table-column>
        <el-table-column label="渠道" width="90">
          <template #default="{row}">{{ channelText(row.channel) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{row}">
            <el-tag size="small" :type="row.status==='READ'?'success':row.status==='FAILED'?'danger':'primary'">
              {{ serveStatus(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="内容" min-width="200"/>
      </el-table>
    </el-card>

    <!-- 回访 -->
    <el-card shadow="never" class="section-card">
      <template #header>
        <div class="card-head"><b>履行回访</b>
          <el-button type="primary" size="small" @click="visitVisible=true" v-if="canVisit">新增回访</el-button>
        </div>
      </template>
      <el-table :data="visits" size="small" stripe>
        <el-table-column label="回访时间" width="150">
          <template #default="{row}">{{ fmt(row.visitTime) }}</template>
        </el-table-column>
        <el-table-column label="方式" width="80">
          <template #default="{row}">{{ visitChannel(row.channel) }}</template>
        </el-table-column>
        <el-table-column prop="target" label="对象" width="100"/>
        <el-table-column label="结果" width="110">
          <template #default="{row}">
            <el-tag size="small" :type="visitType(row.result)">{{ visitResult(row.result) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="回访内容" min-width="240"/>
        <el-table-column prop="visitorName" label="回访人" width="100"/>
      </el-table>
    </el-card>

    <!-- 司法确认 -->
    <el-card shadow="never" class="section-card">
      <template #header><b>司法确认材料</b></template>
      <el-table :data="judicialConfirms" size="small" stripe>
        <el-table-column prop="confirmNo" label="编号" width="170"/>
        <el-table-column prop="claims" label="申请事项" min-width="260" show-overflow-tooltip/>
        <el-table-column label="状态" width="110">
          <template #default="{row}">
            <el-tag size="small" :type="jcType(row.status)">{{ jcText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="rulingNo" label="裁定书号" width="180"/>
        <el-table-column label="操作" width="170">
          <template #default="{row}">
            <el-button v-if="row.status==='DRAFT'" link type="primary" size="small" @click="submitJc(row)">提交申请</el-button>
            <el-button v-if="row.status==='SUBMITTED' && canJa" link type="success" size="small" @click="openRule(row)">裁定</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 转立案 -->
    <el-card shadow="never">
      <template #header>
        <div class="card-head"><b>转立案与连续过程材料</b>
          <div>
            <el-button type="warning" size="small" @click="downloadPackage">
              <el-icon><Download /></el-icon> 导出连续过程材料包
            </el-button>
            <el-button type="danger" size="small" @click="transferVisible=true"
                       v-if="canTransfer && caseInfo.status!=='TRANSFERRED'">申请转立案</el-button>
          </div>
        </div>
      </template>
      <el-table :data="transfers" size="small" stripe>
        <el-table-column prop="applicantName" label="申请人" width="140"/>
        <el-table-column label="原因" width="130">
          <template #default="{row}">{{ reasonText(row.reasonCode) }}</template>
        </el-table-column>
        <el-table-column prop="reason" label="说明" min-width="220" show-overflow-tooltip/>
        <el-table-column label="状态" width="100">
          <template #default="{row}">
            <el-tag size="small" :type="row.status==='ACCEPTED'?'success':row.status==='REJECTED'?'danger':'warning'">
              {{ {SUBMITTED:'待审核', ACCEPTED:'已立案', REJECTED:'不予立案'}[row.status] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="filingNo" label="立案案号" width="200"/>
        <el-table-column label="操作" width="120">
          <template #default="{row}">
            <el-button v-if="row.status==='SUBMITTED' && canJa" link type="success" size="small" @click="openReview(row)">审核</el-button>
            <el-button link type="primary" size="small" @click="showMaterials(row)">材料清单</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-alert type="info" :closable="false" show-icon style="margin-top:10px"
                title="系统自动归集提交、分流、排期改期、失联缺料、调解笔录、协议签署、电子送达、付款逾期、催办回访等全过程记录，避免在调解与立案之间反复补材料。"/>
    </el-card>

    <!-- 送达弹窗 -->
    <el-dialog v-model="serveVisible" title="电子送达" width="480px">
      <el-form label-width="88px">
        <el-form-item label="文书名称">
          <el-select v-model="serveForm.documentName" filterable allow-create style="width:100%">
            <el-option v-for="d in ['调解通知书','排期通知书','调解协议书','履行催告书','转立案告知书']" :key="d" :label="d" :value="d"/>
          </el-select>
        </el-form-item>
        <el-form-item label="受送达人"><el-input v-model="serveForm.receiverName"/></el-form-item>
        <el-form-item label="手机号"><el-input v-model="serveForm.receiverPhone"/></el-form-item>
        <el-form-item label="送达方式">
          <el-radio-group v-model="serveForm.channel">
            <el-radio value="ONLINE">平台站内</el-radio>
            <el-radio value="SMS">短信</el-radio>
            <el-radio value="EMAIL">邮件</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="送达内容"><el-input v-model="serveForm.content" type="textarea" :rows="3"/></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="serveVisible=false">取消</el-button>
        <el-button type="primary" @click="submitServe">立即送达</el-button>
      </template>
    </el-dialog>

    <!-- 回访弹窗 -->
    <el-dialog v-model="visitVisible" title="新增履行回访" width="480px">
      <el-form label-width="80px">
        <el-form-item label="回访方式">
          <el-radio-group v-model="visitForm.channel">
            <el-radio value="PHONE">电话</el-radio>
            <el-radio value="ONLINE">线上</el-radio>
            <el-radio value="ONSITE">上门</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="回访对象">
          <el-select v-model="visitForm.target" style="width:100%">
            <el-option label="申请人" value="申请人"/><el-option label="被申请人" value="被申请人"/><el-option label="双方" value="双方"/>
          </el-select>
        </el-form-item>
        <el-form-item label="回访结果">
          <el-radio-group v-model="visitForm.result">
            <el-radio value="PERFORMED">已按约履行</el-radio>
            <el-radio value="PARTIAL">部分履行</el-radio>
            <el-radio value="BREACH">未履行</el-radio>
            <el-radio value="OBJECTION">提出异议</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="回访内容"><el-input v-model="visitForm.content" type="textarea" :rows="3"/></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visitVisible=false">取消</el-button>
        <el-button type="primary" @click="submitVisit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 转立案弹窗 -->
    <el-dialog v-model="transferVisible" title="申请转立案" width="500px">
      <el-form label-width="90px">
        <el-form-item label="申请人">
          <el-input v-model="transferForm.applicantName" :placeholder="caseInfo.applicantName+' / '+caseInfo.respondentName"/>
        </el-form-item>
        <el-form-item label="转立案原因">
          <el-select v-model="transferForm.reasonCode" style="width:100%">
            <el-option v-for="r in reasons" :key="r.v" :label="r.l" :value="r.v"/>
          </el-select>
        </el-form-item>
        <el-form-item label="情况说明"><el-input v-model="transferForm.reason" type="textarea" :rows="3"/></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="transferVisible=false">取消</el-button>
        <el-button type="danger" @click="submitTransfer">提交申请（自动归集过程材料）</el-button>
      </template>
    </el-dialog>

    <!-- 审核弹窗 -->
    <el-dialog v-model="reviewVisible" title="法官助理审核转立案" width="480px">
      <el-form label-width="90px">
        <el-form-item label="审核结果">
          <el-radio-group v-model="reviewForm.accept">
            <el-radio :value="true">接收立案</el-radio>
            <el-radio :value="false">不予接收</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="立案案号" v-if="reviewForm.accept">
          <el-input v-model="reviewForm.filingNo" placeholder="如：（2026）京0105民初XXXX号"/>
        </el-form-item>
        <el-form-item label="审核意见"><el-input v-model="reviewForm.opinion" type="textarea" :rows="2"/></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewVisible=false">取消</el-button>
        <el-button type="success" @click="submitReview">提交审核</el-button>
      </template>
    </el-dialog>

    <!-- 裁定弹窗 -->
    <el-dialog v-model="ruleVisible" title="司法确认裁定" width="440px">
      <el-form label-width="90px">
        <el-form-item label="裁定结果">
          <el-radio-group v-model="ruleForm.confirm">
            <el-radio :value="true">确认有效</el-radio>
            <el-radio :value="false">不予确认</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="裁定书号"><el-input v-model="ruleForm.rulingNo"/></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="ruleVisible=false">取消</el-button>
        <el-button type="success" @click="submitRule">提交裁定</el-button>
      </template>
    </el-dialog>

    <!-- 材料清单 -->
    <el-dialog v-model="materialVisible" title="连续过程材料清单" width="720px">
      <pre class="material-text">{{ materialText }}</pre>
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
  services: { type: Array, default: () => [] },
  visits: { type: Array, default: () => [] },
  judicialConfirms: { type: Array, default: () => [] },
  transfers: { type: Array, default: () => [] }
})
const emit = defineEmits(['changed'])

const canServe = computed(() => !['PARTY'].includes(auth.role))
const canVisit = computed(() => ['EXECUTION', 'MEDIATOR', 'WINDOW', 'ADMIN'].includes(auth.role))
const canJa = computed(() => ['JUDGE_ASSISTANT', 'ADMIN'].includes(auth.role))
const canTransfer = computed(() => ['PARTY', 'MEDIATOR', 'WINDOW', 'JUDGE_ASSISTANT', 'ADMIN'].includes(auth.role))

const reasons = [
  { v: 'UNREACHABLE', l: '被申请人失联' },
  { v: 'EVIDENCE_MISSING', l: '证据材料缺失' },
  { v: 'RESCHEDULED_MANY', l: '双方多次改期' },
  { v: 'OVERDUE', l: '付款逾期未履行' },
  { v: 'PARTIAL', l: '部分履行后拒不履行' },
  { v: 'NO_AGREEMENT', l: '调解不成' },
  { v: 'PARTY_REQUEST', l: '一方当事人申请' }
]

const serveVisible = ref(false)
const serveForm = reactive({ documentName: '调解通知书', receiverName: '', receiverPhone: '', channel: 'ONLINE', content: '' })
const visitVisible = ref(false)
const visitForm = reactive({ channel: 'PHONE', target: '被申请人', result: 'PERFORMED', content: '' })
const transferVisible = ref(false)
const transferForm = reactive({ applicantName: '', reasonCode: 'OVERDUE', reason: '' })
const reviewVisible = ref(false)
const reviewRow = ref(null)
const reviewForm = reactive({ accept: true, filingNo: '', opinion: '' })
const ruleVisible = ref(false)
const ruleRow = ref(null)
const ruleForm = reactive({ confirm: true, rulingNo: '' })
const materialVisible = ref(false)
const materialText = ref('')

async function submitServe() {
  if (!serveForm.documentName) return ElMessage.warning('请填写文书名称')
  await http.post('/services', { caseId: props.caseInfo.id, ...serveForm })
  ElMessage.success('已电子送达')
  serveVisible.value = false
  emit('changed')
}

async function submitVisit() {
  await http.post('/visits', { caseId: props.caseInfo.id, ...visitForm })
  ElMessage.success('回访已保存')
  visitVisible.value = false
  emit('changed')
}

async function submitTransfer() {
  if (!transferForm.applicantName) return ElMessage.warning('请填写申请人')
  await http.post('/transfers', { caseId: props.caseInfo.id, ...transferForm })
  ElMessage.success('转立案申请已提交，连续过程材料已自动归集')
  transferVisible.value = false
  emit('changed')
}

function openReview(row) {
  reviewRow.value = row
  reviewForm.accept = true; reviewForm.filingNo = ''; reviewForm.opinion = ''
  reviewVisible.value = true
}
async function submitReview() {
  if (reviewForm.accept && !reviewForm.filingNo) return ElMessage.warning('接收立案需填写立案案号')
  await http.post(`/transfers/${reviewRow.value.id}/review`, reviewForm)
  ElMessage.success('审核完成')
  reviewVisible.value = false
  emit('changed')
}

async function submitJc(row) {
  await http.post(`/judicial/${row.id}/submit`)
  ElMessage.success('司法确认申请已提交')
  emit('changed')
}
function openRule(row) { ruleRow.value = row; ruleForm.confirm = true; ruleForm.rulingNo = ''; ruleVisible.value = true }
async function submitRule() {
  if (!ruleForm.rulingNo) return ElMessage.warning('请填写裁定书号')
  await http.post(`/judicial/${ruleRow.value.id}/rule`, ruleForm)
  ElMessage.success('裁定结果已登记')
  ruleVisible.value = false
  emit('changed')
}

async function showMaterials(row) {
  const text = await fetchMaterial()
  materialText.value = text
  materialVisible.value = true
}

async function fetchMaterial() {
  const token = localStorage.getItem('token')
  const resp = await fetch(`/api/transfers/cases/${props.caseInfo.id}/material-package`, {
    headers: { 'X-Token': token }
  })
  return await resp.text()
}

async function downloadPackage() {
  const text = await fetchMaterial()
  const blob = new Blob([text], { type: 'text/plain;charset=utf-8' })
  const a = document.createElement('a')
  a.href = URL.createObjectURL(blob)
  a.download = `${props.caseInfo.caseNo}-连续过程材料.txt`
  a.click()
  URL.revokeObjectURL(a.href)
}

function fmt(t) { return t ? t.replace('T', ' ').substring(0, 16) : '' }
function channelText(c) { return { SMS: '短信', EMAIL: '邮件', ONLINE: '平台站内' }[c] }
function serveStatus(s) { return { PENDING: '已发起', DELIVERED: '已送达', READ: '已读', FAILED: '失败' }[s] }
function visitChannel(c) { return { PHONE: '电话', ONLINE: '线上', ONSITE: '上门' }[c] }
function visitResult(r) { return { PERFORMED: '按约履行', PARTIAL: '部分履行', BREACH: '未履行', OBJECTION: '提出异议' }[r] }
function visitType(r) { return { PERFORMED: 'success', PARTIAL: 'warning', BREACH: 'danger', OBJECTION: 'info' }[r] }
function jcText(s) { return { DRAFT: '材料已生成', SUBMITTED: '已提交', CONFIRMED: '已确认', REJECTED: '不予确认' }[s] }
function jcType(s) { return { DRAFT: 'info', SUBMITTED: 'warning', CONFIRMED: 'success', REJECTED: 'danger' }[s] }
function reasonText(c) { return reasons.find(r => r.v === c)?.l || c }
</script>

<style scoped>
.card-head { display: flex; justify-content: space-between; align-items: center; }
.material-text { white-space: pre-wrap; font-size: 12px; line-height: 1.7; max-height: 60vh; overflow: auto; }
</style>
