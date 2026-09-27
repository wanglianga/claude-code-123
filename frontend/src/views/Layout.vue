<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="logo">
        <el-icon :size="22"><School /></el-icon>
        <span>诉前调解平台</span>
      </div>
      <el-menu :default-active="$route.path" router background-color="#163a5f"
               text-color="#c7d6e5" active-text-color="#ffd666">
        <el-menu-item index="/dashboard">
          <el-icon><DataAnalysis /></el-icon><span>统计看板</span>
        </el-menu-item>
        <el-menu-item index="/cases">
          <el-icon><Folder /></el-icon><span>案件管理</span>
        </el-menu-item>
        <el-menu-item index="/cases-new" v-if="canCreate">
          <el-icon><EditPen /></el-icon><span>新建/登记案件</span>
        </el-menu-item>
        <el-menu-item index="/import" v-if="!auth.isParty">
          <el-icon><Upload /></el-icon><span>批量导入</span>
        </el-menu-item>
        <el-menu-item index="/mediators" v-if="['ADMIN','WINDOW'].includes(auth.role)">
          <el-icon><UserFilled /></el-icon><span>人员管理</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div class="crumb">
          <el-icon><Location /></el-icon>
          <span>{{ $route.name }}</span>
        </div>
        <div class="userbox">
          <el-tag :type="roleTagType" effect="dark" round>{{ roleLabel }}</el-tag>
          <span class="uname">{{ auth.user?.realName }}</span>
          <span class="muted">{{ auth.user?.title }}</span>
          <el-button link type="primary" @click="logout"><el-icon><SwitchButton /></el-icon> 退出</el-button>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { auth, ROLE_LABELS } from '../api/auth'

const router = useRouter()
const roleLabel = computed(() => ROLE_LABELS[auth.role] || auth.role)
const roleTagType = computed(() => ({
  ADMIN: 'danger', WINDOW: 'warning', MEDIATOR: 'primary',
  PARTY: '', JUDGE_ASSISTANT: 'success', EXECUTION: 'info'
}[auth.role] || ''))
const canCreate = computed(() => ['PARTY', 'WINDOW', 'ADMIN'].includes(auth.role))

function logout() {
  auth.logout()
  router.push('/login')
}
</script>

<style scoped>
.layout { height: 100vh; }
.aside { background: #163a5f; }
.logo {
  height: 60px; display: flex; align-items: center; gap: 8px;
  color: #fff; font-weight: 700; font-size: 17px;
  padding-left: 18px; background: #0f2c49;
}
.el-menu { border-right: none; }
.header {
  background: #fff; display: flex; align-items: center; justify-content: space-between;
  border-bottom: 1px solid #e6eaf0;
}
.crumb { display: flex; align-items: center; gap: 6px; font-weight: 600; color: #304156; }
.userbox { display: flex; align-items: center; gap: 10px; }
.uname { font-weight: 600; color: #304156; }
.main { background: #f2f4f8; padding: 16px; overflow-y: auto; }
</style>
