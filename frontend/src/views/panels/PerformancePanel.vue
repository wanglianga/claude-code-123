<template>
  <div>
    <el-card shadow="never" class="mb">
      <template #header>
        <div class="head">
          <b>履行付款计划</b>
          <div>
            <el-button v-if="canBuild && !data.plans.length" type="primary" size="small" @click="buildVisible = true">
              依据协议建立付款计划
            </el-button>
            <el-button v-if="canExec" size="small" @click="scanOverdue">立即扫描逾期</el-button>
          </div>
        </div>
      </template>
      <el-table :data="data.plans" stripe>
        <el-table-column label="期次" width="70"><template #default="{ row }">第{{ row.installmentNo }}期</template></el-table-column>
        <el-table-column prop="dueAmount" label="应付金额(元)" width="130" align="right" />
        <el-table-column prop="dueDate" label="应付日期" width="120" />
        <el-table-column prop="paidAmount" label="已付(元)" width="110" align="right" />
        <el-table-column prop="paidDate" label="到账日期" width="120" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="PLAN_STATUS[row.status].tag">{{ PLAN_STATUS[row.status].label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="note" label="备注" min-width="160" />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button v-if="canExec && row.status !== 'PAID'" size="small" type="primary" link @click="openPay(row)">确认到账</el-button>
          </template>
        </el-table-column>
        <template #empty>协议生效后建立履行节点；分期协议按每期生成</template>
      </el-table>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="head">
          <b>履行回访记录</b>
          <el-button v-if="canExec" type="primary" size="small" @click="followVisible = true">新增回访</el-button>
        </div>
      </template>
      <el-table :data="data.followUps" stripe>
        <el-table-column prop="followUpDate" label="回访日期" width="120" />
        <el-table-column label="方式" width="90">
          <template #default="{ row }">{{ { PHONE: '电话', ONLINE: '线上', ONSITE: '上门' }[row.way] }}</template>
        </el-table-column>
        <el-table-column label="结果" width="110">
          <template #default="{ row }">
            <el-tag size="small" :type="followTag(row.result)">{{ FOLLOW_RESULTS[row.result] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="operatorName" label="回访人" width="100" />
        <el-table-column prop="note" label="情况记录" min-width="220" />
        <template #empty>暂无回访记录</template>
      </el-table>
    </el-card>

    <!-- 建立付款计划 -->
    <el-dialog v-model="buildVisible" title="建立履行付款计划" width="640px">
      <el-alert v-if="a && !a.installment" type="info" :closable="false" show-icon class="mb"
                title="一次性履行协议，将自动按协议金额与履行期限生成单个节点" />
      <template v-if="a && a.installment">
        <div v-for="(it, i) in installments" :key="i" class="ins-row">
          <span>第{{ i + 1 }}期</span>
          <el-input-number v-model="it.dueAmount" :min="0" :precision="2" :controls="false" placeholder="金额" style="width:150px" />
          <el-date-picker v-model="it.dueDate" value-format="YYYY-MM-DD" placeholder="到期日" style="width:170px" />
          <el-button link type="danger" @click="installments.splice(i, 1)">删除</el-button>
        </div>
        <el-button size="small" @click="installments.push({ dueAmount: 0, dueDate: '' })">+ 添加一期</el-button>
      </template>
      <template #footer>
        <el-button @click="buildVisible = false">取消</el-button>
        <el-button type="primary" @click="buildPlans">生成</el-button>
      </template>
    </el-dialog>

    <!-- 确认到账 -->
    <el-dialog v-model="payVisible" title="确认履行到账（支持部分履行）" width="460px">
      <el-form :model="pay" label-width="110px">
        <el-form-item label="本期应收">¥{{ pay.dueAmount }}</el-form-item>
        <el-form-item label="本次到账(元)">
          <el-input-number v-model="pay.paidAmount" :min="0" :precision="2" :controls="false" style="width:200px" />
        </el-form-item>
        <el-form-item label="到账日期"><el-date-picker v-model="pay.paidDate" value-format="YYYY-MM-DD" style="width:200px" /></el-form-item>
        <el-form-item label="备注/凭证"><el-input v-model="pay.note" placeholder="银行流水号等" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="payVisible = false">取消</el-button>
        <el-button type="primary" @click="submitPay">确认</el-button>
      </template>
    </el-dialog>

    <!-- 回访 -->
    <el-dialog v-model="followVisible" title="新增履行回访" width="500px">
      <el-form :model="follow" label-width="90px">
        <el-form-item label="回访日期"><el-date-picker v-model="follow.followUpDate" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item>
        <el-form-item label="方式">
          <el-radio-group v-model="follow.way">
            <el-radio-button value="PHONE">电话</el-radio-button>
            <el-radio-button value="ONLINE">线上</el-radio-button>
            <el-radio-button value="ONSITE">上门</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="回访结果">
          <el-select v-model="follow.result" style="width:100%">
            <el-option v-for="(l, k) in FOLLOW_RESULTS" :key="k" :label="l" :value="k" />
          </el-select>
        </el-form-item>
        <el-form-item label="情况记录"><el-input v-model="follow.note" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="followVisible = false">取消</el-button>
        <el-button type="primary" @click="submitFollow">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../../api.js'
import { auth } from '../../store.js'
import { PLAN_STATUS, FOLLOW_RESULTS } from '../../constants.js'

const props = defineProps({ data: Object })
const emit = defineEmits(['changed'])
const c = computed(() => props.data.case)
const a = computed(() => props.data.agreement)

const canExec = computed(() => auth.is('EXECUTION', 'MEDIATOR', 'WINDOW', 'ADMIN'))
const canBuild = computed(() => canExec.value && a.value?.effectiveAt)

const buildVisible = ref(false)
const installments = ref([{ dueAmount: 0, dueDate: '' }, { dueAmount: 0, dueDate: '' }])
const buildPlans = async () => {
  await api.post(`/cases/${c.value.id}/plans`, { installments: installments.value })
  buildVisible.value = false
  ElMessage.success('履行计划已建立，到期未付将自动预警')
  emit('changed')
}

const payVisible = ref(false)
const pay = reactive({ id: null, dueAmount: 0, paidAmount: 0, paidDate: '', note: '' })
const openPay = row => {
  pay.id = row.id
  pay.dueAmount = Number(row.dueAmount) - Number(row.paidAmount || 0)
  pay.paidAmount = pay.dueAmount
  pay.paidDate = new Date().toISOString().substring(0, 10)
  pay.note = ''
  payVisible.value = true
}
const submitPay = async () => {
  await api.post(`/plans/${pay.id}/pay`, { paidAmount: pay.paidAmount, paidDate: pay.paidDate, note: pay.note })
  payVisible.value = false
  ElMessage.success('到账已确认并留痕')
  emit('changed')
}

const scanOverdue = async () => {
  const r = await api.post('/overdue/scan')
  ElMessage.success(r.newlyOverdue > 0 ? `扫描完成，新发现 ${r.newlyOverdue} 个逾期节点` : '扫描完成，暂无新增逾期')
  emit('changed')
}

const followVisible = ref(false)
const follow = reactive({ followUpDate: new Date().toISOString().substring(0, 10), way: 'PHONE', result: 'NORMAL', note: '' })
const submitFollow = async () => {
  await api.post(`/cases/${c.value.id}/follow-ups`, follow)
  followVisible.value = false
  ElMessage.success('回访已记录')
  emit('changed')
}

const followTag = r => ({ NORMAL: 'success', PARTIAL: 'warning', OVERDUE: 'danger', DONE: 'success', OBJECTION: 'info' }[r])
</script>

<style scoped>
.mb { margin-bottom: 14px; }
.head { display: flex; justify-content: space-between; align-items: center; }
.ins-row { display: flex; align-items: center; gap: 10px; margin-bottom: 10px; }
</style>
