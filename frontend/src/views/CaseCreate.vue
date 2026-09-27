<template>
  <el-card shadow="never">
    <template #header><b>新建诉前调解申请</b></template>
    <el-form :model="form" label-width="130px" style="max-width:820px">
      <el-divider content-position="left">案由与紧急程度</el-divider>
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="案由" required>
            <el-select v-model="form.caseType" placeholder="请选择" style="width:100%">
              <el-option v-for="(l, k) in CASE_TYPES" :key="k" :label="l" :value="k" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="紧急程度">
            <el-radio-group v-model="form.urgency">
              <el-radio-button v-for="(l, k) in URGENCY" :key="k" :value="k">{{ l }}</el-radio-button>
            </el-radio-group>
          </el-form-item>
        </el-col>
      </el-row>

      <el-divider content-position="left">当事人信息</el-divider>
      <el-row :gutter="16">
        <el-col :span="12"><el-form-item label="申请人姓名" required><el-input v-model="form.applicantName" /></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="申请人电话"><el-input v-model="form.applicantPhone" /></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="被申请人姓名" required><el-input v-model="form.respondentName" /></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="被申请人电话" required><el-input v-model="form.respondentPhone" /></el-form-item></el-col>
        <el-col :span="24"><el-form-item label="被申请人地址"><el-input v-model="form.respondentAddress" /></el-form-item></el-col>
        <el-col :span="12">
          <el-form-item label="外地当事人">
            <el-switch v-model="form.respondentOutOfTown" active-text="被申请人在外地（优先线上）" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="愿意线上调解">
            <el-switch v-model="form.onlineWilling" active-text="接受线上视频调解" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-divider content-position="left">诉求与证据</el-divider>
      <el-form-item label="诉求金额(元)" required>
        <el-input-number v-model="form.claimAmount" :min="0" :precision="2" :controls="false" style="width:220px" />
      </el-form-item>
      <el-form-item label="诉求摘要"><el-input v-model="form.claimSummary" type="textarea" :rows="2" /></el-form-item>
      <el-form-item label="证据清单">
        <el-input v-model="form.evidenceList" type="textarea" :rows="3"
                  placeholder="逐项列出，分号或换行分隔，如：借条原件；转账凭证；微信聊天记录" />
      </el-form-item>

      <el-form-item>
        <el-button type="primary" :loading="loading" @click="submit">提交申请（进入分流）</el-button>
        <el-button @click="$router.back()">返回</el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '../api.js'
import { CASE_TYPES, URGENCY } from '../constants.js'

const router = useRouter()
const loading = ref(false)
const form = reactive({
  caseType: 'LOAN', urgency: 'NORMAL',
  applicantName: '', applicantPhone: '',
  respondentName: '', respondentPhone: '', respondentAddress: '',
  respondentOutOfTown: false,
  claimAmount: 10000, claimSummary: '', evidenceList: '', onlineWilling: true
})

const submit = async () => {
  if (!form.applicantName || !form.respondentName || !form.respondentPhone) {
    return ElMessage.warning('请填写申请人、被申请人姓名及被申请人电话')
  }
  loading.value = true
  try {
    const c = await api.post('/cases', form)
    ElMessage.success(`申请已提交，调解编号：${c.caseNo}，等待立案窗口分流`)
    router.push(`/cases/${c.id}`)
  } finally {
    loading.value = false
  }
}
</script>
