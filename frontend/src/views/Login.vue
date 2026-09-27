<template>
  <div class="login-bg">
    <div class="login-card">
      <div class="brand">
        <el-icon :size="34" color="#1d4e89"><School /></el-icon>
        <div>
          <div class="title">基层法院诉前调解排期与协议履行平台</div>
          <div class="subtitle">分流 · 排期 · 调解 · 签署 · 履行 · 转立案 全流程协同</div>
        </div>
      </div>
      <el-form :model="form" @keyup.enter="onLogin" class="login-form">
        <el-form-item>
          <el-input v-model="form.username" size="large" placeholder="账号" :prefix-icon="User" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" size="large" placeholder="密码"
                    :prefix-icon="Lock" show-password />
        </el-form-item>
        <el-button type="primary" size="large" style="width:100%" :loading="loading" @click="onLogin">
          登 录
        </el-button>
      </el-form>
      <el-divider>演示账号（点击自动填充）</el-divider>
      <div class="accounts">
        <el-tag v-for="a in accounts" :key="a.u" class="acc-tag" :type="a.type" effect="plain"
                @click="fill(a)">
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
import { User, Lock } from '@element-plus/icons-vue'
import http from '../api/http'
import { auth } from '../api/auth'

const router = useRouter()
const form = reactive({ username: 'admin', password: 'admin123' })
const loading = ref(false)

const accounts = [
  { u: 'admin', p: 'admin123', label: '管理员', type: 'danger' },
  { u: 'window', p: 'window123', label: '立案窗口', type: 'warning' },
  { u: 'mediator1', p: 'mediator123', label: '调解员', type: 'primary' },
  { u: 'ja', p: 'ja123', label: '法官助理', type: 'success' },
  { u: 'exec', p: 'exec123', label: '执行联络', type: 'info' },
  { u: 'party1', p: 'party123', label: '当事人', type: '' }
]

function fill(a) {
  form.username = a.u
  form.password = a.p
}

async function onLogin() {
  if (!form.username || !form.password) return ElMessage.warning('请输入账号密码')
  loading.value = true
  try {
    const res = await http.post('/auth/login', form)
    auth.set(res.token, res.user)
    ElMessage.success(`欢迎，${res.user.realName}`)
    router.push('/dashboard')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-bg {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1d4e89 0%, #2e7bbf 55%, #4aa3df 100%);
}
.login-card {
  width: 460px;
  background: #fff;
  border-radius: 12px;
  padding: 34px 38px 28px;
  box-shadow: 0 18px 50px rgba(0, 0, 0, .25);
}
.brand { display: flex; gap: 12px; align-items: center; margin-bottom: 22px; }
.title { font-size: 19px; font-weight: 700; color: #1d2b3a; line-height: 1.4; }
.subtitle { font-size: 12px; color: #7b8794; margin-top: 4px; }
.accounts { display: flex; flex-wrap: wrap; gap: 8px; }
.acc-tag { cursor: pointer; }
.login-form { margin-top: 8px; }
</style>
