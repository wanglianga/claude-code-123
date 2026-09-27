<template>
  <div>
    <!-- 无协议：起草 -->
    <el-card v-if="!activeAgreement" shadow="never">
      <el-empty description="尚未生成调解协议">
        <el-button type="primary" @click="draftVisible=true" v-if="canDraft">起草调解协议</el-button>
      </el-empty>
    </el-card>

    <template v-else>
      <el-card shadow="never" class="section-card">
        <template #header>
          <div class="card-head">
            <b>调解协议 {{ activeAgreement.agreementNo }}
              <el-tag size="small" :type="activeAgreement.status==='SIGNED'?'success':'info'" style="margin-left:8px">
                {{ activeAgreement.status==='SIGNED' ? '已签署生效' : '待签署' }}
              </el-tag>
            </b>
            <div>
              <el-button size="small" @click="viewText=true">查看协议正文</el-button>
              <el-button size="small" type="warning" v-if="canConfirm"
                         :disabled="activeAgreement.status==='SIGNED' || hasJudicial"
                         @click="generateJudicial">生成司法确认材料</el-button>
            </div>
          </div>
        </template>
        <el-descriptions :column="3" border size="small">
          <el-descriptions-item label="协议金额">¥{{ activeAgreement.totalAmount }}</el-descriptions-item>
          <el-descriptions-item label="履行方式">
            {{ activeAgreement.paymentMethod==='INSTALLMENTS' ? '分期履行' : '一次性履行' }}
          </el-descriptions-item>
          <el-descriptions-item label="履行期限">{{ activeAgreement.deadline }}</el-descriptions-item>
          <el-descriptions-item label="担保人">{{ activeAgreement.guarantor || '无' }}</el-descriptions-item>
          <el-descriptions-item label="司法确认意向">
            {{ activeAgreement.applyJudicialConfirm ? '拟申请' : '不申请' }}
          </el-descriptions-item>
          <el-descriptions-item label="签署时间">
            {{ activeAgreement.signedAt ? fmt(activeAgreement.signedAt) : '—' }}
          </el-descriptions-item>
        </el-descriptions>
    </el-card>

      <!-- 签署 -->
      <el-card v-if="activeAgreement.status==='DRAFT'" shadow="never" class="section-card">
        <template #header><b>线上签署（需人脸核验）</b></template>
        <el-alert type="info" :closable="false" show-icon
                  title="签署前需完成人脸核验，核验相似度≥0.90方可签署；申请人与被申请人均签署后协议自动生效。"
                  style="margin-bottom:12px"/>
        <el-table :data="signatureRows" size="small" border>
          <el-table-column label="签署方" width="120"/>
          <el-table-column label="状态" width="120">
            <template #default="{row}">
              <el-tag size="small" :type="row.done?'success':'warning'">{{ row.done ? '已签署' : '待签署' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="人脸核验" width="120">
            <template #default="{row}">
              <span v-if="row.done" style="color:#67c23a">通过 {{ row.score }}</span>
              <span v-else class="muted">未核验</span>
            </template>
          </el-table-column>
          <el-table-column label="操作">
            <template #default="{row}">
              <el-button v-if="!row.done" type="primary" size="small" @click="openFace(row)">人脸核验并签署</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <!-- 履行节点 -->
      <el-card shadow="never" class="section-card">
        <template #header>
          <div class="card-head">
            <b>履行节点（分期 / 逾期 / 部分履行）</b>
            <el-button size="small" @click="scanOverdue" v-if="['EXECUTION','ADMIN','MEDIATOR'].includes(auth.role)">
              立即扫描逾期
            </el-button>
          </div>
        </template>
        <el-table :data="plans" size="small" stripe>
          <el-table-column label="期次" width="70" align="center">
            <template #default="{row}">第{{ row.periodNo }}期</template>
          </el-table-column>
          <el-table-column prop="dueDate" label="应付日期" width="120"/>
          <el-table-column label="应付金额" width="120" align="right">
            <template #default="{row}">¥{{ row.amount }}</template>
          </el-table-column>
          <el-table-column label="已付金额" width="120" align="right">
            <template #default="{row}">¥{{ row.paidAmount || 0 }}</template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{row}">
              <el-tag size="small" :type="planType(row.status)">{{ planText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="remark" label="备注" min-width="140"/>
          <el-table-column label="操作" width="180">
            <template #default="{row}">
              <el-button v-if="canExec && row.status!=='PAID'" link type="success" size="small" @click="openPay(row)">登记收款</el-button>
              <el-button v-if="canExec" link type="warning" size="small" @click="openRemind(row)">催告</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </template>

    <!-- 起草协议弹窗 -->
    <el-dialog v-model="draftVisible" title="起草调解协议" width="640px">
      <el-form :model="draftForm" label-width="100px" size="small">
        <el-form-item label="协议金额">
          <el-input-number v-model="draftForm.totalAmount" :min="0" :precision="2" style="width:100%"/>
        </el-form-item>
        <el-form-item label="履行方式">
          <el-radio-group v-model="draftForm.paymentMethod">
            <el-radio value="LUMP_SUM">一次性履行</el-radio>
            <el-radio value="INSTALLMENTS">分期履行</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="全部履行期限">
          <el-date-picker v-model="draftForm.deadline" type="date" value-format="YYYY-MM-DD" style="width:100%"/>
        </el-form-item>
        <el-form-item label="担保人"><el-input v-model="draftForm.guarantor"/></el-form-item>
        <el-form-item label="申请司法确认"><el-switch v-model="draftForm.applyJudicialConfirm"/></el-form-item>
        <el-form-item label="分期节点" v-if="draftForm.paymentMethod==='INSTALLMENTS'">
          <div style="width:100%">
            <div v-for="(p,i) in draftForm.plans" :key="i" style="display:flex;gap:8px;margin-bottom:6px">
              <el-date-picker v-model="p.dueDate" type="date" value-format="YYYY-MM-DD" placeholder="应付日期" style="flex:1"/>
              <el-input-number v-model="p.amount" :min="0" placeholder="金额" style="flex:1"/>
              <el-button circle type="danger" @click="draftForm.plans.splice(i,1)"><el-icon><Delete/></el-icon></el-button>
            </div>
            <el-button size="small" @click="draftForm.plans.push({dueDate:'',amount:null})">+ 增加一期</el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="draftVisible=false">取消</el-button>
        <el-button type="primary" @click="submitDraft">生成协议</el-button>
      </template>
    </el-dialog>

    <!-- 人脸核验弹窗（模拟） -->
    <el-dialog v-model="faceVisible" title="人脸核验" width="380px" :show-close="false"
               @closed="onFaceClosed">
      <div class="face-box" :class="{scanning: faceStep==='scanning'}">
        <el-icon :size="72" color="#409eff"><Avatar /></el-icon>
        <div class="face-line" v-if="faceStep==='scanning'"></div>
      </div>
      <div style="text-align:center;margin:10px 0" v-if="faceStep==='scanning'">
        正在比对人脸特征……
      </div>
      <div v-if="faceStep==='fail'" style="text-align:center;color:#f56c6c;margin:10px 0">
        人脸核验未通过，请正对摄像头重试
      </div>
      <template #footer>
        <template v-if="faceStep==='idle'">
          <el-button @click="faceVisible=false">取消</el-button>
          <el-button type="primary" @click="startFace"><el-icon><VideoCamera/></el-icon> 开始人脸核验</el-button>
        </template>
      </template>
    </el-dialog>

    <!-- 收款弹窗 -->
    <el-dialog v-model="payVisible" title="登记收款（可部分履行）" width="420px">
      <el-form label-width="90px">
        <el-form-item label="本期应付">¥{{ payRow?.amount }}，已付 ¥{{ payRow?.paidAmount || 0 }}</el-form-item>
        <el-form-item label="本次实收">
          <el-input-number v-model="payAmount" :min="0.01" :precision="2" style="width:100%"/>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="payRemark" type="textarea" :rows="2"/></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="payVisible=false">取消</el-button>
        <el-button type="success" @click="submitPay">确认收款</el-button>
      </template>
    </el-dialog>

    <!-- 催告弹窗 -->
    <el-dialog v-model="remindVisible" title="发起履行催告" width="420px">
      <el-input v-model="remindContent" type="textarea" :rows="3" placeholder="催告内容"/>
      <template #footer>
        <el-button @click="remindVisible=false">取消</el-button>
        <el-button type="warning" @click="submitRemind">发起催告</el-button>
      </template>
    </el-dialog>

    <!-- 协议正文 -->
    <el-dialog v-model="viewText" title="协议正文" width="680px">
      <pre class="agreement-text">{{ activeAgreement?.content }}</pre>
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
  activeAgreement: Object,
  plans: { type: Array, default: () => [] },
  signatures: { type: Array, default: () => [] },
  judicialConfirms: { type: Array, default: () => [] }
})
const emit = defineEmits(['changed'])

const canDraft = computed(() => ['MEDIATOR', 'WINDOW', 'ADMIN'].includes(auth.role))
const canConfirm = computed(() => ['MEDIATOR', 'JUDGE_ASSISTANT', 'ADMIN'].includes(auth.role))
const canExec = computed(() => ['EXECUTION', 'MEDIATOR', 'ADMIN'].includes(auth.role))
const hasJudicial = computed(() => props.judicialConfirms.length > 0)

const draftVisible = ref(false)
const draftForm = reactive({
  totalAmount: props.caseInfo.claimAmount, paymentMethod: 'LUMP_SUM',
  deadline: '', guarantor: '', applyJudicialConfirm: false, plans: []
})

function submitDraft() {
  if (!draftForm.deadline) return ElMessage.warning('请选择履行期限')
  http.post('/agreements', { caseId: props.caseInfo.id, ...draftForm }).then(() => {
    ElMessage.success('协议已生成，可组织双方线上签署')
    draftVisible.value = false
    emit('changed')
  })
}

// ---- 签署状态 ----
const signatureRows = computed(() => {
  const ap = props.signatures.find(s => s.signerRole === 'APPLICANT')
  const rp = props.signatures.find(s => s.signerRole === 'RESPONDENT')
  return [
    { role: 'APPLICANT', name: props.caseInfo.applicantName, done: !!ap, score: ap ? Number(ap.faceScore).toFixed(2) : '' },
    { role: 'RESPONDENT', name: props.caseInfo.respondentName, done: !!rp, score: rp ? Number(rp.faceScore).toFixed(2) : '' }
  ]
})

const faceVisible = ref(false)
const faceStep = ref('idle')
let faceTimer = null
const currentSigner = ref(null)

function openFace(row) {
  currentSigner.value = row
  faceStep.value = 'idle'
  faceVisible.value = true
}

function startFace() {
  faceStep.value = 'scanning'
  // 模拟摄像头活体检测与比对：约85%概率通过
  faceTimer = setTimeout(async () => {
    const pass = Math.random() > 0.15
    if (pass) {
      faceVisible.value = false
      const row = currentSigner.value
      await http.post(`/agreements/${props.activeAgreement.id}/sign`, {
        signerRole: row.role,
        signerName: row.name,
        facePass: true
      })
      ElMessage.success('人脸核验通过，签署完成')
      emit('changed')
    } else {
      faceStep.value = 'fail'
      setTimeout(() => { faceStep.value = 'idle' }, 1600)
    }
  }, 1800)
}

function onFaceClosed() {
  if (faceTimer) clearTimeout(faceTimer)
}

// ---- 收款 ----
const payVisible = ref(false)
const payRow = ref(null)
const payAmount = ref(0)
const payRemark = ref('')
function openPay(row) {
  payRow.value = row
  payAmount.value = Number(row.amount) - Number(row.paidAmount || 0)
  payRemark.value = ''
  payVisible.value = true
}
async function submitPay() {
  await http.post(`/performance/plans/${payRow.value.id}/pay`, { paidAmount: payAmount.value, remark: payRemark.value })
  ElMessage.success('收款已确认')
  payVisible.value = false
  emit('changed')
}

// ---- 催告 ----
const remindVisible = ref(false)
const remindRow = ref(null)
const remindContent = ref('')
function openRemind(row) { remindRow.value = row; remindContent.value = ''; remindVisible.value = true }
async function submitRemind() {
  await http.post(`/performance/plans/${remindRow.value.id}/remind`, { content: remindContent.value })
  ElMessage.success('催告已发起并留痕')
  remindVisible.value = false
  emit('changed')
}

async function scanOverdue() {
  const r = await http.post('/performance/scan-overdue')
  ElMessage.success(`扫描完成，新标记 ${r.overdueMarked} 个逾期节点`)
  emit('changed')
}

async function generateJudicial() {
  await http.post(`/judicial/generate/${props.activeAgreement.id}`)
  ElMessage.success('司法确认申请材料已生成')
  emit('changed')
}

function fmt(t) { return t ? t.replace('T', ' ').substring(0, 16) : '' }
function planText(s) { return { PENDING: '待履行', PAID: '已履行', OVERDUE: '已逾期', PARTIAL: '部分履行' }[s] }
function planType(s) { return { PENDING: 'info', PAID: 'success', OVERDUE: 'danger', PARTIAL: 'warning' }[s] }
</script>

<style scoped>
.card-head { display: flex; justify-content: space-between; align-items: center; }
.agreement-text { white-space: pre-wrap; font-family: "SimSun", serif; line-height: 1.8; font-size: 13px; }
.face-box {
  position: relative; height: 200px; border: 2px dashed #409eff; border-radius: 12px;
  display: flex; align-items: center; justify-content: center; background: #f4f9ff; overflow: hidden;
}
.face-line {
  position: absolute; left: 10%; right: 10%; height: 2px; background: linear-gradient(90deg, transparent, #67c23a, transparent);
  animation: scan 1.4s linear infinite;
}
@keyframes scan {
  0% { top: 12%; } 50% { top: 85%; } 100% { top: 12%; }
}
</style>
