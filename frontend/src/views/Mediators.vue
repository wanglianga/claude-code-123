<template>
  <div>
    <el-card shadow="never" class="section-card">
      <template #header>
        <div class="card-head">
          <b>人员与调解员管理</b>
          <el-button type="primary" size="small" @click="openCreate">新增人员</el-button>
        </div>
      </template>
      <el-form :inline="true" size="small">
        <el-form-item label="角色">
          <el-select v-model="role" clearable placeholder="全部角色" style="width:150px" @change="load">
            <el-option v-for="(l,v) in ROLE_LABELS" :key="v" :label="l" :value="v"/>
          </el-select>
        </el-form-item>
      </el-form>
      <el-table :data="users" size="small" stripe>
        <el-table-column prop="username" label="账号" width="120"/>
        <el-table-column prop="realName" label="姓名" width="110"/>
        <el-table-column label="角色" width="110">
          <template #default="{row}">
            <el-tag size="small" :type="roleType(row.role)">{{ ROLE_LABELS[row.role] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="职务/部门" min-width="160"/>
        <el-table-column prop="phone" label="电话" width="130"/>
        <el-table-column prop="specialties" label="调解员专长案由" min-width="200"/>
        <el-table-column label="外地/线上" width="90" align="center">
          <template #default="{row}">
            <el-tag v-if="row.specialties && row.id" size="small" :type="isRemote(row.id)?'primary':'info'">
              {{ isRemote(row.id) ? '线上组' : '本地' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{row}">
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="visible" :title="form.id?'编辑人员':'新增人员'" width="500px">
      <el-form :model="form" label-width="110px">
        <el-form-item label="账号" v-if="!form.id"><el-input v-model="form.username"/></el-form-item>
        <el-form-item label="姓名"><el-input v-model="form.realName"/></el-form-item>
        <el-form-item label="密码"><el-input v-model="form.password" :placeholder="form.id?'留空则不修改':'默认123456'"/></el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.role" style="width:100%">
            <el-option v-for="(l,v) in ROLE_LABELS" :key="v" :label="l" :value="v"/>
          </el-select>
        </el-form-item>
        <el-form-item label="职务/部门"><el-input v-model="form.title"/></el-form-item>
        <el-form-item label="电话"><el-input v-model="form.phone"/></el-form-item>
        <el-form-item label="专长案由" v-if="form.role==='MEDIATOR'">
          <el-select v-model="specialtyArr" multiple filterable allow-create default-first-option
                     placeholder="选择或输入案由" style="width:100%">
            <el-option v-for="c in CAUSE_OPTIONS" :key="c" :label="c" :value="c"/>
          </el-select>
        </el-form-item>
        <el-form-item label="线上/外地" v-if="form.role==='MEDIATOR'">
          <el-switch v-model="form.remote"/>
          <span class="muted">擅长处理外地当事人线上调解</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible=false">取消</el-button>
        <el-button type="primary" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import http from '../api/http'
import { ROLE_LABELS, CAUSE_OPTIONS } from '../api/auth'

const users = ref([])
const role = ref('')
const visible = ref(false)
const form = reactive({})
const specialtyArr = ref([])
const remoteMap = ref({})

function roleType(r) {
  return { ADMIN: 'danger', WINDOW: 'warning', MEDIATOR: 'primary', PARTY: '', JUDGE_ASSISTANT: 'success', EXECUTION: 'info' }[r]
}
function isRemote(id) { return remoteMap.value[id] }

async function load() {
  users.value = await http.get('/users', { params: { role: role.value } })
  // 角色筛选接口只返回启用用户，远程标记取全量数据补充
  const all = await http.get('/users')
  remoteMap.value = Object.fromEntries(all.map(u => [u.id, u.remote]))
}

function openCreate() {
  Object.keys(form).forEach(k => delete form[k])
  form.role = 'MEDIATOR'
  specialtyArr.value = []
  visible.value = true
}

function openEdit(row) {
  Object.assign(form, row, { password: '' })
  specialtyArr.value = row.specialties ? row.specialties.split(/[,，]/).filter(Boolean) : []
  visible.value = true
}

async function submit() {
  const payload = { ...form }
  if (payload.role === 'MEDIATOR') payload.specialties = specialtyArr.value.join(',')
  if (form.id) {
    if (!payload.password) delete payload.password
    await http.put(`/users/${form.id}`, payload)
  } else {
    if (!form.username) return ElMessage.warning('请填写账号')
    await http.post('/users', payload)
  }
  ElMessage.success('保存成功')
  visible.value = false
  load()
}

onMounted(load)
</script>

<style scoped>
.card-head { display: flex; justify-content: space-between; align-items: center; }
</style>
