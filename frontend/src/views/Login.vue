<template>
  <div class="login-wrap">
    <div class="login-box">
      <div class="brand">
        <el-icon :size="34"><OfficeBuilding /></el-icon>
        <div>
          <div class="title">基层法院诉前调解平台</div>
          <div class="sub">排期 · 调解 · 协议履行 · 转立案一体化</div>
        </div>
      </div>
      <el-form :model="form" @keyup.enter="doLogin" class="form">
        <el-form-item>
          <el-input v-model="form.username" size="large" placeholder="账号" prefix-icon="User" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" size="large" placeholder="密码" prefix-icon="Lock" show-password />
        </el-form-item>
        <el-button type="primary" size="large" style="width:100%" :loading="loading" @click="doLogin">登 录</el-button>
      </el-form>
      <el-divider>演示账号（点击填充）</el-divider>
      <div class="accounts">
        <el-tag v-for="a in accounts" :key="a.u" class="acc-tag" :type="a.type" @click="fill(a)">
          {{ a.label }}：{{ a.u }} / {{ a.p }}
        </el-tag>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '../api.js'
import { auth } from '../store.js'

const router = useRouter()
const form = reactive({ username: 'window', password: 'window123' })
const loading = ref(false)

const accounts = [
  { u: 'window', p: 'window123', label: '立案窗口', type: 'danger' },
  { u: 'zhang', p: 'mediator123', label: '调解员', type: 'success' },
  { u: 'party', p: 'party123', label: '当事人', type: 'primary' },
  { u: 'assistant', p: 'assistant123', label: '法官助理', type: 'warning' },
  { u: 'execution', p: 'exec123', label: '执行联络', type: 'info' },
  { u: 'admin', p: 'admin123', label: '管理员', type: '' }
]
const fill = a => { form.username = a.u; form.password = a.p }

const doLogin = async () => {
  if (!form.username || !form.password) return ElMessage.warning('请输入账号密码')
  loading.value = true
  try {
    const data = await api.post('/auth/login', form)
    auth.set(data.token, data.user)
    ElMessage.success(`欢迎，${data.user.realName}（${data.user.roleLabel}）`)
    router.push('/')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-wrap {
  min-height: 100vh;
  display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, #1d3c6e 0%, #2b5b94 50%, #3a7bd5 100%);
}
.login-box {
  width: 460px; background: #fff; border-radius: 12px; padding: 36px 40px;
  box-shadow: 0 12px 40px rgba(0,0,0,.25);
}
.brand { display: flex; align-items: center; gap: 12px; color: #1d3c6e; margin-bottom: 24px; }
.brand .title { font-size: 22px; font-weight: 700; }
.brand .sub { font-size: 12px; color: #888; margin-top: 4px; }
.form { margin-top: 10px; }
.accounts { display: flex; flex-wrap: wrap; gap: 8px; }
.acc-tag { cursor: pointer; }
</style>
