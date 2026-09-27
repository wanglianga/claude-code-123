<template>
  <el-card shadow="never">
    <template #header><b>登记 / 提交纠纷</b></template>
    <el-form :model="form" label-width="130px" style="max-width:760px">
      <el-form-item label="纠纷标题" required>
        <el-input v-model="form.title" placeholder="如：张三诉李四民间借贷纠纷"/>
      </el-form-item>
      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="案由" required>
            <el-select v-model="form.causeType" style="width:100%" placeholder="请选择">
              <el-option v-for="c in CAUSE_OPTIONS" :key="c" :label="c" :value="c"/>
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="诉求金额（元）">
            <el-input-number v-model="form.claimAmount" :min="0" :precision="2" style="width:100%"/>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="申请人姓名" required><el-input v-model="form.applicantName"/></el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="申请人电话"><el-input v-model="form.applicantPhone"/></el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="被申请人姓名" required><el-input v-model="form.respondentName"/></el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="被申请人电话"><el-input v-model="form.respondentPhone"/></el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="被申请人地址"><el-input v-model="form.respondentAddress"/></el-form-item>
      <el-form-item label="证据清单">
        <el-input v-model="form.evidenceSummary" type="textarea" :rows="3"
                  placeholder="多个证据用分号或换行分隔，如：借条;转账凭证;微信聊天记录"/>
      </el-form-item>
      <el-form-item label="纠纷情况说明">
        <el-input v-model="form.description" type="textarea" :rows="3"/>
      </el-form-item>
      <el-row :gutter="12">
        <el-col :span="8">
          <el-form-item label="愿意线上调解">
            <el-switch v-model="form.willingOnline"/>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="紧急程度">
            <el-select v-model="form.urgency" style="width:100%">
              <el-option label="普通" value="NORMAL"/>
              <el-option label="紧急" value="URGENT"/>
              <el-option label="特急" value="VERY_URGENT"/>
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="外地当事人">
            <el-switch v-model="form.remoteParty"/>
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item>
        <el-button type="primary" :loading="saving" @click="submit">提交</el-button>
        <el-button @click="$router.back()">取消</el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import http from '../api/http'
import { CAUSE_OPTIONS } from '../api/auth'

const router = useRouter()
const saving = ref(false)
const form = reactive({
  title: '', causeType: '', claimAmount: 0,
  applicantName: '', applicantPhone: '',
  respondentName: '', respondentPhone: '', respondentAddress: '',
  evidenceSummary: '', description: '',
  willingOnline: true, urgency: 'NORMAL', remoteParty: false
})

async function submit() {
  if (!form.title || !form.causeType || !form.applicantName || !form.respondentName) {
    return ElMessage.warning('请完善必填项')
  }
  saving.value = true
  try {
    const c = await http.post('/cases', form)
    ElMessage.success('提交成功，案号：' + c.caseNo)
    router.push(`/cases/${c.id}`)
  } finally {
    saving.value = false
  }
}
</script>
