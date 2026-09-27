<template>
  <div>
    <!-- 调解笔录 -->
    <el-card shadow="never" class="mb">
      <template #header>
        <div class="head">
          <b>调解笔录（争议焦点 / 双方让步）</b>
          <el-button v-if="canMediate" type="primary" size="small" @click="recordVisible = true">
            <el-icon><Edit /></el-icon>新增调解笔录
          </el-button>
        </div>
      </template>
      <el-timeline>
        <el-timeline-item v-for="r in data.records" :key="r.id" :timestamp="`第${r.roundNo}次 · ${fmt(r.createdAt)}`"
                          :type="r.outcome === 'SUCCESS' ? 'success' : r.outcome === 'FAIL' ? 'danger' : 'primary'">
          <el-card shadow="hover">
            <p><b>争议焦点：</b>{{ r.focusPoints || '—' }}</p>
            <p><b>申请人让步：</b>{{ r.applicantConcession || '—' }}</p>
            <p><b>被申请人让步：</b>{{ r.respondentConcession || '—' }}</p>
            <p><b>协商过程：</b>{{ r.processNote || '—' }}</p>
            <el-tag size="small" :type="r.outcome === 'SUCCESS' ? 'success' : r.outcome === 'FAIL' ? 'danger' : 'info'">
              {{ r.outcome === 'SUCCESS' ? '达成协议' : r.outcome === 'FAIL' ? '调解不成' : '继续调解' }}
            </el-tag>
          </el-card>
        </el-timeline-item>
        <template #empty><el-empty description="尚无调解笔录" :image-size="70" /></template>
      </el-timeline>
    </el-card>

    <!-- 调解协议 -->
    <el-card shadow="never" class="mb">
      <template #header><b>调解协议 · 线上签署 · 人脸核验</b></template>
      <div v-if="a" class="agreement-box">
        <el-descriptions :column="3" border size="small">
          <el-descriptions-item label="协议金额">¥{{ Number(a.totalAmount).toLocaleString() }}</el-descriptions-item>
          <el-descriptions-item label="履行方式">{{ a.installment ? '分期履行' : '一次性履行' }}</el-descriptions-item>
          <el-descriptions-item label="履行期限">{{ a.deadline }}</el-descriptions-item>
          <el-descriptions-item label="付款计划" :span="2">{{ a.paymentPlan }}</el-descriptions-item>
          <el-descriptions-item label="担保人">{{ a.guarantorName || '无' }} {{ a.guarantorPhone }}</el-descriptions-item>
          <el-descriptions-item label="司法确认">
            <el-tag size="small" :type="a.judicialConfirmRequested ? 'warning' : 'info'">
              {{ a.judicialConfirmRequested ? '当事人已申请' : '未申请' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="申请人签署">
            <el-tag size="small" :type="a.applicantSigned ? 'success' : 'info'">
              {{ a.applicantSigned ? '已签 ' + fmt(a.applicantSignedAt) : '未签' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="被申请人签署">
            <el-tag size="small" :type="a.respondentSigned ? 'success' : 'info'">
              {{ a.respondentSigned ? '已签 ' + fmt(a.respondentSignedAt) : '未签' }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>
        <el-divider content-position="left">协议正文</el-divider>
        <div class="agreement-text">{{ a.content }}</div>
      </div>

      <el-form v-if="canDraft" :model="agr" label-width="110px" class="mt">
        <el-row :gutter="12">
          <el-col :span="8"><el-form-item label="协议金额(元)"><el-input-number v-model="agr.totalAmount" :min="0" :precision="2" :controls="false" style="width:100%" /></el-form-item></el-col>
          <el-col :span="8">
            <el-form-item label="履行方式">
              <el-switch v-model="agr.installment" active-text="分期" inactive-text="一次性" />
            </el-form-item>
          </el-col>
          <el-col :span="8"><el-form-item label="履行期限"><el-date-picker v-model="agr.deadline" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="付款计划"><el-input v-model="agr.paymentPlan" placeholder="如：分3期，每月10日前付10000元" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="担保人姓名"><el-input v-model="agr.guarantorName" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="担保人电话"><el-input v-model="agr.guarantorPhone" /></el-form-item></el-col>
          <el-col :span="8">
            <el-form-item label="司法确认">
              <el-switch v-model="agr.judicialConfirmRequested" active-text="当事人申请司法确认" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-button type="primary" @click="saveAgreement">{{ a ? '更新协议' : '起草协议（自动生成规范文本）' }}</el-button>
      </el-form>

      <!-- 人脸核验 + 签署 -->
      <el-divider content-position="left" v-if="a" />
      <div v-if="a" class="sign-area">
        <el-button @click="faceVisible = true"><el-icon><Avatar /></el-icon>人脸核验（模拟）</el-button>
        <el-tag v-for="f in data.faceVerifications" :key="f.id" size="small" class="ml"
                :type="f.passed ? 'success' : 'danger'">
          {{ f.partyName || f.party }} 核验{{ f.passed ? '通过' : '未通过' }}（{{ f.score }}分）
        </el-tag>
        <div class="mt">
          <el-button type="success" :disabled="a.applicantSigned" @click="sign('APPLICANT')">申请人签署</el-button>
          <el-button type="success" :disabled="a.respondentSigned" @click="sign('RESPONDENT')">被申请人签署</el-button>
          <span class="tip">（须先人脸核验通过；双方签署后协议生效）</span>
        </div>
      </div>
    </el-card>

    <!-- 笔录弹窗 -->
    <el-dialog v-model="recordVisible" title="新增调解笔录" width="640px">
      <el-form :model="rec" label-width="100px">
        <el-form-item label="争议焦点"><el-input v-model="rec.focusPoints" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="申请人让步"><el-input v-model="rec.applicantConcession" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="被申请人让步"><el-input v-model="rec.respondentConcession" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="协商过程"><el-input v-model="rec.processNote" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="调解结果">
          <el-radio-group v-model="rec.outcome">
            <el-radio value="CONTINUE">继续调解</el-radio>
            <el-radio value="SUCCESS">达成协议</el-radio>
            <el-radio value="FAIL">调解不成</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="recordVisible = false">取消</el-button>
        <el-button type="primary" @click="saveRecord">保存笔录</el-button>
      </template>
    </el-dialog>

    <!-- 人脸核验弹窗 -->
    <el-dialog v-model="faceVisible" title="人脸核验（演示模拟）" width="440px">
      <el-alert type="info" :closable="false" show-icon class="mb"
                title="演示说明：输入任意非 0000 的身份证后四位即模拟活体检测+人脸比对通过（85-99分）；输入 0000 模拟不通过。" />
      <el-form :model="face" label-width="100px">
        <el-form-item label="核验对象">
          <el-radio-group v-model="face.party">
            <el-radio value="APPLICANT">申请人</el-radio>
            <el-radio value="RESPONDENT">被申请人</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="姓名"><el-input v-model="face.partyName" /></el-form-item>
        <el-form-item label="身份证后四位"><el-input v-model="face.idCardTail" maxlength="4" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="faceVisible = false">取消</el-button>
        <el-button type="primary" @click="doFace">开始核验</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../../api.js'
import { auth } from '../../store.js'

const props = defineProps({ data: Object })
const emit = defineEmits(['changed'])
const c = computed(() => props.data.case)
const a = computed(() => props.data.agreement)

const canMediate = computed(() => auth.is('MEDIATOR', 'WINDOW', 'ADMIN')
  && ['SCHEDULED', 'MEDIATING', 'TRIAGED'].includes(c.value.status))
const canDraft = computed(() => auth.is('MEDIATOR', 'WINDOW', 'ADMIN')
  && ['SCHEDULED', 'MEDIATING', 'MEDIATION_SUCCESS', 'JUDICIAL_CONFIRMING', 'PERFORMING'].includes(c.value.status))

const recordVisible = ref(false)
const rec = reactive({ focusPoints: '', applicantConcession: '', respondentConcession: '', processNote: '', outcome: 'CONTINUE' })
const saveRecord = async () => {
  await api.post(`/cases/${c.value.id}/records`, rec)
  recordVisible.value = false
  ElMessage.success('笔录已保存并写入案件时间线')
  emit('changed')
}

const agr = reactive({
  totalAmount: 10000, installment: false, deadline: '', paymentPlan: '',
  guarantorName: '', guarantorPhone: '', judicialConfirmRequested: false
})
const saveAgreement = async () => {
  await api.post(`/cases/${c.value.id}/agreement`, agr)
  ElMessage.success('协议已起草，等待双方人脸核验后签署')
  emit('changed')
}
// 已有协议时异步回填
watch(a, v => {
  if (v) {
    Object.assign(agr, {
      totalAmount: Number(v.totalAmount), installment: v.installment,
      deadline: v.deadline, paymentPlan: v.paymentPlan || '',
      guarantorName: v.guarantorName || '', guarantorPhone: v.guarantorPhone || '',
      judicialConfirmRequested: v.judicialConfirmRequested
    })
  }
}, { immediate: true })

const faceVisible = ref(false)
const face = reactive({ party: 'APPLICANT', partyName: '', idCardTail: '' })
const doFace = async () => {
  const r = await api.post(`/cases/${c.value.id}/face-verify`, face)
  faceVisible.value = false
  ElMessage[r.passed ? 'success' : 'error'](`核验完成：${r.score}分，${r.note}`)
  emit('changed')
}

const sign = async party => {
  await api.post(`/cases/${c.value.id}/sign`, { party })
  ElMessage.success('电子签署完成')
  emit('changed')
}

const fmt = t => t ? t.replace('T', ' ').substring(0, 16) : ''
</script>

<style scoped>
.mb { margin-bottom: 14px; }
.mt { margin-top: 10px; }
.ml { margin-left: 8px; }
.head { display: flex; justify-content: space-between; align-items: center; }
.agreement-text { white-space: pre-wrap; background: #f7f9fc; padding: 14px; border-radius: 6px; font-size: 13px; line-height: 1.9; border: 1px solid #e4e9f2; }
.sign-area { padding: 0 4px; }
.tip { color: #909399; font-size: 12px; margin-left: 10px; }
:deep(.el-timeline-item p) { margin: 4px 0; font-size: 13px; }
</style>
