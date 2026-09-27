<template>
  <div>
    <el-card shadow="never" class="section-card">
      <el-form :inline="true" size="small">
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width:130px" @change="load">
            <el-option v-for="s in STATUS_OPTIONS" :key="s.value" :label="s.label" :value="s.value"/>
          </el-select>
        </el-form-item>
        <el-form-item label="案由">
          <el-select v-model="query.causeType" placeholder="全部" clearable style="width:130px" @change="load">
            <el-option v-for="c in CAUSE_OPTIONS" :key="c" :label="c" :value="c"/>
          </el-select>
        </el-form-item>
        <el-form-item label="紧急程度">
          <el-select v-model="query.urgency" placeholder="全部" clearable style="width:110px" @change="load">
            <el-option label="特急" value="VERY_URGENT"/>
            <el-option label="紧急" value="URGENT"/>
            <el-option label="普通" value="NORMAL"/>
          </el-select>
        </el-form-item>
        <el-form-item label="调解员">
          <el-select v-model="query.mediatorId" placeholder="全部" clearable filterable style="width:130px" @change="load">
            <el-option v-for="m in mediators" :key="m.id" :label="m.realName" :value="m.id"/>
          </el-select>
        </el-form-item>
        <el-form-item label="关键字">
          <el-input v-model="query.keyword" placeholder="案号/标题/当事人" clearable style="width:190px"
                    @keyup.enter="load" @clear="load"/>
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="query.onlyMine" @change="load" v-if="auth.isParty">仅我的申请</el-checkbox>
          <el-button type="primary" @click="load"><el-icon><Search /></el-icon> 查询</el-button>
          <el-button v-if="['WINDOW','ADMIN'].includes(auth.role)" type="success" @click="$router.push('/cases-new')">
            <el-icon><Plus /></el-icon> 登记案件
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <el-table :data="cases" stripe v-loading="loading" @row-click="goDetail" :row-style="{cursor:'pointer'}">
        <el-table-column label="案号 / 标题" min-width="260">
          <template #default="{row}">
            <div class="case-title">{{ row.title }}</div>
            <div class="muted">{{ row.caseNo }}</div>
          </template>
        </el-table-column>
        <el-table-column label="案由" width="110">
          <template #default="{row}"><el-tag size="small" effect="plain">{{ row.causeType }}</el-tag></template>
        </el-table-column>
        <el-table-column label="当事人" width="170">
          <template #default="{row}">
            <div>申：{{ row.applicantName }}</div>
            <div class="muted">被：{{ row.respondentName }}</div>
          </template>
        </el-table-column>
        <el-table-column label="诉求金额" width="110" align="right">
          <template #default="{row}">¥{{ formatMoney(row.claimAmount) }}</template>
        </el-table-column>
        <el-table-column label="紧急" width="70" align="center">
          <template #default="{row}">
            <el-tag v-if="row.urgency==='VERY_URGENT'" type="danger" size="small">特急</el-tag>
            <el-tag v-else-if="row.urgency==='URGENT'" type="warning" size="small">紧急</el-tag>
            <span v-else class="muted">普通</span>
          </template>
        </el-table-column>
        <el-table-column label="方式" width="70" align="center">
          <template #default="{row}">
            <el-icon v-if="row.willingOnline" color="#409eff"><VideoCamera /></el-icon>
            <el-icon v-else color="#909399"><OfficeBuilding /></el-icon>
          </template>
        </el-table-column>
        <el-table-column label="风险标记" width="180">
          <template #default="{row}">
            <el-space :size="4" wrap>
              <el-tag v-if="row.respondentUnreachable" type="danger" size="small">失联</el-tag>
              <el-tag v-if="row.evidenceMissing" type="warning" size="small">缺料</el-tag>
              <el-tag v-if="row.rescheduleCount>=2" type="info" size="small">改期{{row.rescheduleCount}}次</el-tag>
              <el-tag v-if="row.remoteParty" type="primary" size="small">外地</el-tag>
            </el-space>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{row}">
            <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{row}">
            <el-button v-if="row.status==='PENDING_TRIAGE' && ['WINDOW','ADMIN'].includes(auth.role)"
                       type="primary" link size="small" @click.stop="triage(row)">分流</el-button>
            <el-button type="primary" link size="small" @click.stop="goDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 分流弹窗 -->
    <el-dialog v-model="triageVisible" title="立案窗口分流" width="520px">
      <el-form label-width="100px">
        <el-form-item label="案件">
          <span>{{ triageRow?.caseNo }} {{ triageRow?.title }}</span>
        </el-form-item>
        <el-form-item label="推荐调解员">
          <el-radio-group v-model="triageForm.mediatorId" size="small">
            <el-radio-button v-for="m in recommendList" :key="m.id" :value="m.id">
              {{ m.realName }}<span v-if="m.matched" style="color:#67c23a">（专长匹配）</span>
            </el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="调解方式">
          <el-radio-group v-model="triageForm.mediationMode">
            <el-radio value="ONLINE">线上调解</el-radio>
            <el-radio value="OFFLINE">线下调解</el-radio>
          </el-radio-group>
          <div class="muted" v-if="triageRow?.remoteParty">该案涉及外地当事人，建议优先线上调解</div>
        </el-form-item>
        <el-form-item label="分流意见">
          <el-input v-model="triageForm.remark" type="textarea" :rows="2" placeholder="分流考虑：案由匹配、紧急程度、双方意愿等"/>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="triageVisible=false">取消</el-button>
        <el-button type="primary" @click="submitTriage">确认分流</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import http from '../api/http'
import { auth, STATUS_OPTIONS, CAUSE_OPTIONS, statusLabel, statusType } from '../api/auth'

const router = useRouter()
const cases = ref([])
const mediators = ref([])
const loading = ref(false)
const query = reactive({ status: '', causeType: '', urgency: '', mediatorId: null, keyword: '', onlyMine: false })

const triageVisible = ref(false)
const triageRow = ref(null)
const triageForm = reactive({ mediatorId: null, mediationMode: 'ONLINE', remark: '' })
const recommendList = ref([])

async function load() {
  loading.value = true
  try {
    cases.value = await http.get('/cases', { params: query })
  } finally {
    loading.value = false
  }
}

async function triage(row) {
  triageRow.value = row
  triageForm.mediationMode = row.willingOnline ? 'ONLINE' : 'OFFLINE'
  triageForm.remark = ''
  triageForm.mediatorId = null
  recommendList.value = await http.get('/schedules/mediators', { params: { causeType: row.causeType } })
  const matched = recommendList.value.find(m => m.matched)
  if (matched) triageForm.mediatorId = matched.id
  triageVisible.value = true
}

async function submitTriage() {
  if (!triageForm.mediatorId) return ElMessage.warning('请选择调解员')
  await http.post(`/cases/${triageRow.value.id}/triage`, triageForm)
  ElMessage.success('分流完成，已进入调解中心待排期')
  triageVisible.value = false
  load()
}

function goDetail(row) {
  router.push(`/cases/${row.id}`)
}

function formatMoney(v) {
  return v == null ? '0' : Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 0 })
}

onMounted(async () => {
  mediators.value = await http.get('/users', { params: { role: 'MEDIATOR' } })
  load()
})
</script>

<style scoped>
.case-title { font-weight: 600; color: #303133; }
</style>
