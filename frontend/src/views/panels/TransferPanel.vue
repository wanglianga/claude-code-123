<template>
  <div>
    <!-- 司法确认 -->
    <el-card shadow="never" class="mb">
      <template #header><b>司法确认</b></template>
      <el-descriptions :column="2" border size="small" v-if="a">
        <el-descriptions-item label="是否申请">
          <el-tag size="small" :type="a.judicialConfirmRequested ? 'warning' : 'info'">
            {{ a.judicialConfirmRequested ? '已申请，审查中' : '未申请' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="协议生效时间">{{ a.effectiveAt ? fmt(a.effectiveAt) : '—' }}</el-descriptions-item>
      </el-descriptions>
      <el-empty v-else description="尚未达成调解协议" :image-size="60" />
      <div v-if="a" class="mt">
        <el-button type="warning" @click="downloadJudicial"><el-icon><Printer /></el-icon>生成/下载司法确认申请书</el-button>
        <el-button v-if="auth.is('ASSISTANT','ADMIN')" type="primary" @click="confirmPass">审查通过（模拟法官确认）</el-button>
        <span class="tip" v-if="c.status === 'JUDICIAL_CONFIRMING'">当前：司法确认审查中，确认后案件进入履行阶段</span>
      </div>
    </el-card>

    <!-- 转立案 -->
    <el-card shadow="never">
      <template #header><b>转立案（连续材料自动汇编，避免反复补材料）</b></template>

      <el-alert class="mb" type="success" :closable="false" show-icon
                title="系统已把申请、分流、排期/改期、笔录、协议、签署、履行、回访、送达、失联/缺件等全部过程记录汇编成卷，转立案时随案自动移交，当事人无需在调解与立案窗口间重复补交。" />

      <div v-if="tm">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="材料状态">
            <el-tag size="small" :type="tm.status === 'ACCEPTED' ? 'success' : 'warning'">
              {{ tm.status === 'ACCEPTED' ? '立案窗口已接收' : '已生成，待接收' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="转立案原因">{{ TRANSFER_REASONS[tm.reasonType] || tm.reasonType }}</el-descriptions-item>
          <el-descriptions-item label="生成时间">{{ fmt(tm.preparedAt) }}</el-descriptions-item>
          <el-descriptions-item label="接收时间">{{ tm.acceptedAt ? fmt(tm.acceptedAt) : '—' }}</el-descriptions-item>
          <el-descriptions-item label="材料清单" :span="2"><div class="pre">{{ tm.materialList }}</div></el-descriptions-item>
        </el-descriptions>
      </div>

      <el-form :model="tr" label-width="110px" class="mt">
        <el-form-item label="转立案原因">
          <el-select v-model="tr.reasonType" style="width:360px">
            <el-option v-for="(l, k) in TRANSFER_REASONS" :key="k" :label="l" :value="k" />
          </el-select>
        </el-form-item>
        <el-form-item label="原因说明"><el-input v-model="tr.reasonDetail" type="textarea" :rows="2" placeholder="补充说明（可选）" /></el-form-item>
        <el-form-item>
          <el-button type="danger" @click="prepareTransfer">
            <el-icon><FolderAdd /></el-icon>{{ tm ? '重新汇编材料包' : '生成转立案材料包' }}
          </el-button>
          <el-button v-if="tm && tm.status === 'PREPARED' && auth.is('WINDOW','ADMIN','ASSISTANT')"
                     type="primary" @click="acceptVisible = true">
            窗口接收并正式转立案
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 接收转立案弹窗 -->
    <el-dialog v-model="acceptVisible" title="正式转立案" width="440px">
      <el-alert type="warning" :closable="false" show-icon class="mb" title="接收后全部调解卷宗随案进入诉讼程序，无需当事人另交材料。" />
      <el-form label-width="110px">
        <el-form-item label="诉讼案号">
          <el-input v-model="litigationNo" placeholder="如：(2026)xx民初字第xx号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="acceptVisible = false">取消</el-button>
        <el-button type="primary" @click="acceptTransfer">确认接收转立案</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../../api.js'
import { auth } from '../../store.js'
import { TRANSFER_REASONS } from '../../constants.js'

const props = defineProps({ data: Object })
const emit = defineEmits(['changed'])
const c = computed(() => props.data.case)
const a = computed(() => props.data.agreement)
const tm = computed(() => props.data.transferMaterial)

const tr = reactive({ reasonType: 'MISSING', reasonDetail: '' })
const acceptVisible = ref(false)
const litigationNo = ref('')

const prepareTransfer = async () => {
  await api.post(`/cases/${c.value.id}/transfer`, tr)
  ElMessage.success('转立案材料包已生成，全过程连续材料随卷待移交')
  emit('changed')
}
const acceptTransfer = async () => {
  await api.post(`/cases/${c.value.id}/transfer/accept`, { litigationCaseNo: litigationNo.value || undefined })
  acceptVisible.value = false
  ElMessage.success('案件已正式转立案，调解卷宗一次移交完成')
  emit('changed')
}

const downloadJudicial = () => {
  const token = localStorage.getItem('token')
  fetch(`/api/cases/${c.value.id}/judicial-confirm-doc`, { headers: { Authorization: `Bearer ${token}` } })
    .then(r => r.blob()).then(blob => {
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `司法确认申请书_${c.value.caseNo}.txt`
      a.click()
      ElMessage.success('司法确认申请书已下载')
    })
}

// 法官助理审查通过 → 案件进入履行跟踪
const confirmPass = async () => {
  await api.post(`/cases/${c.value.id}/judicial-confirm/pass`)
  ElMessage.success('司法确认审查通过，确认裁定电子送达双方，案件进入履行跟踪')
  emit('changed')
}

const fmt = t => t ? t.replace('T', ' ').substring(0, 16) : ''
</script>

<style scoped>
.mb { margin-bottom: 14px; }
.mt { margin-top: 10px; }
.tip { color: #909399; font-size: 12px; margin-left: 10px; }
.pre { white-space: pre-wrap; font-size: 13px; line-height: 1.8; }
</style>
