<template>
  <el-timeline>
    <el-timeline-item v-for="e in events" :key="e.id" :timestamp="fmt(e.createdAt)" placement="top"
                      :type="typeOf(e.type)" :icon="iconOf(e.type)" size="large">
      <el-card shadow="hover" :body-style="{padding:'10px 14px'}">
        <div class="ev-head">
          <el-tag size="small" :type="typeOf(e.type)">{{ typeText(e.type) }}</el-tag>
          <span class="ev-op">{{ e.operatorName || '系统' }}
            <span class="muted">（{{ roleText(e.operatorRole) }}）</span>
          </span>
        </div>
        <div class="ev-content">{{ e.content }}</div>
      </el-card>
    </el-timeline-item>
  </el-timeline>
</template>

<script setup>
defineProps({ events: { type: Array, default: () => [] } })

function fmt(t) {
  return t ? t.replace('T', ' ').substring(0, 16) : ''
}
function roleText(r) {
  return { WINDOW: '窗口', MEDIATOR: '调解员', PARTY: '当事人', JUDGE_ASSISTANT: '法官助理', EXECUTION: '执行联络', ADMIN: '管理员' }[r] || '系统'
}
function typeText(t) {
  return {
    CREATED: '提交申请', TRIAGED: '分流', SCHEDULED: '排期', RESCHEDULED: '改期',
    SESSION_HELD: '召开调解', UNREACHABLE: '失联登记', REACHED: '恢复联系',
    EVIDENCE_MISSING: '材料缺失', EVIDENCE_SUPPLEMENTED: '补充证据', MEDIATION_NOTE: '调解记录',
    AGREEMENT_DRAFTED: '协议起草', SIGNED: '签署', FACE_VERIFIED: '人脸核验',
    SERVED: '电子送达', PAYMENT_DUE: '付款节点', PAYMENT_PAID: '收款确认',
    PAYMENT_OVERDUE: '逾期', REMIND: '催办', VISIT: '回访',
    TRANSFER_REQUESTED: '申请转立案', TRANSFERRED: '转立案',
    JUDICIAL_APPLY: '司法确认', JUDICIAL_CONFIRMED: '司法确认裁定', STATUS_CHANGED: '状态变更'
  }[t] || t
}
function typeOf(t) {
  if (['PAYMENT_OVERDUE', 'UNREACHABLE', 'EVIDENCE_MISSING', 'TRANSFER_REQUESTED'].includes(t)) return 'danger'
  if (['SIGNED', 'PAYMENT_PAID', 'JUDICIAL_CONFIRMED', 'COMPLETED'].includes(t)) return 'success'
  if (['RESCHEDULED', 'REMIND', 'EVIDENCE_SUPPLEMENTED'].includes(t)) return 'warning'
  if (['SCHEDULED', 'TRIAGED', 'SERVED', 'FACE_VERIFIED'].includes(t)) return 'primary'
  return 'info'
}
function iconOf() { return undefined }
</script>

<style scoped>
.ev-head { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }
.ev-op { font-size: 12px; color: #606266; font-weight: 600; }
.ev-content { color: #303133; font-size: 13px; white-space: pre-wrap; line-height: 1.6; }
</style>
