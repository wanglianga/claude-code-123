<template>
  <div v-loading="loading">
    <!-- 案件头 -->
    <el-card shadow="never" class="section-card" v-if="detail">
      <div class="case-head">
        <div>
          <div class="title-line">
            <el-tag :type="statusType(detail.caseInfo.status)" size="large">
              {{ statusLabel(detail.caseInfo.status) }}
            </el-tag>
            <span class="title">{{ detail.caseInfo.title }}</span>
          </div>
          <div class="muted no">{{ detail.caseInfo.caseNo }}</div>
        </div>
        <div class="head-tags">
          <el-tag v-if="detail.caseInfo.respondentUnreachable" type="danger">被申请人失联</el-tag>
          <el-tag v-if="detail.caseInfo.evidenceMissing" type="warning">材料缺失</el-tag>
          <el-tag v-if="detail.caseInfo.rescheduleCount>=2" type="info">改期{{detail.caseInfo.rescheduleCount}}次</el-tag>
          <el-tag v-if="detail.caseInfo.remoteParty" type="primary">外地当事人</el-tag>
          <el-tag v-if="detail.caseInfo.urgency==='VERY_URGENT'" type="danger">特急</el-tag>
          <el-tag v-else-if="detail.caseInfo.urgency==='URGENT'" type="warning">紧急</el-tag>
        </div>
      </div>
      <el-descriptions :column="4" border size="small" style="margin-top:12px">
        <el-descriptions-item label="案由">{{ detail.caseInfo.causeType }}</el-descriptions-item>
        <el-descriptions-item label="诉求金额">¥{{ formatMoney(detail.caseInfo.claimAmount) }}</el-descriptions-item>
        <el-descriptions-item label="调解方式">
          {{ detail.caseInfo.mediationMode==='ONLINE'?'线上':detail.caseInfo.mediationMode==='OFFLINE'?'线下':'未指定' }}
        </el-descriptions-item>
        <el-descriptions-item label="调解员">
          {{ detail.mediator?.realName || '未指派' }}
          <span class="muted" v-if="detail.mediator?.specialties">（{{detail.mediator.specialties}}）</span>
        </el-descriptions-item>
        <el-descriptions-item label="申请人">
          {{ detail.caseInfo.applicantName }} <span class="muted">{{ detail.caseInfo.applicantPhone }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="被申请人" :span="2">
          {{ detail.caseInfo.respondentName }} <span class="muted">{{ detail.caseInfo.respondentPhone }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="愿意线上调解">
          {{ detail.caseInfo.willingOnline ? '是' : '否' }}
        </el-descriptions-item>
        <el-descriptions-item label="被申请人地址" :span="3">{{ detail.caseInfo.respondentAddress || '—' }}</el-descriptions-item>
        <el-descriptions-item label="纠纷说明" :span="4">{{ detail.caseInfo.description || '—' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 页签 -->
    <el-card shadow="never" v-if="detail">
      <el-tabs v-model="tab">
        <el-tab-pane label="调解排期" name="schedule">
          <ScheduleTab :case-info="detail.caseInfo" :schedules="detail.schedules" @changed="load"/>
        </el-tab-pane>
        <el-tab-pane label="调解过程" name="mediation">
          <MediationTab :case-info="detail.caseInfo" :evidences="detail.evidences"
                        :records="detail.mediationRecords" @changed="load"/>
        </el-tab-pane>
        <el-tab-pane label="协议与履行" name="agreement">
          <AgreementTab :case-info="detail.caseInfo" :active-agreement="detail.activeAgreement"
                        :plans="detail.paymentPlans" :signatures="detail.signatures"
                        :judicial-confirms="detail.judicialConfirms" @changed="load"/>
        </el-tab-pane>
        <el-tab-pane label="送达/回访/转立案" name="service">
          <ServiceTab :case-info="detail.caseInfo" :services="detail.services" :visits="detail.visits"
                      :judicial-confirms="detail.judicialConfirms" :transfers="detail.transfers"
                      @changed="load"/>
        </el-tab-pane>
        <el-tab-pane :label="`过程时间线（${detail.timeline.length}）`" name="timeline">
          <CaseTimeline :events="detail.timeline"/>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import http from '../api/http'
import { statusLabel, statusType } from '../api/auth'
import ScheduleTab from '../components/ScheduleTab.vue'
import MediationTab from '../components/MediationTab.vue'
import AgreementTab from '../components/AgreementTab.vue'
import ServiceTab from '../components/ServiceTab.vue'
import CaseTimeline from '../components/CaseTimeline.vue'

const route = useRoute()
const detail = ref(null)
const loading = ref(false)
const tab = ref('schedule')

async function load() {
  loading.value = true
  try {
    detail.value = await http.get(`/cases/${route.params.id}`)
  } finally {
    loading.value = false
  }
}

function formatMoney(v) {
  return v == null ? '0' : Number(v).toLocaleString('zh-CN')
}

onMounted(load)
</script>

<style scoped>
.case-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; }
.title-line { display: flex; align-items: center; gap: 10px; }
.title { font-size: 17px; font-weight: 700; color: #1d2b3a; }
.no { margin-top: 4px; }
.head-tags { display: flex; gap: 6px; flex-wrap: wrap; justify-content: flex-end; }
</style>
