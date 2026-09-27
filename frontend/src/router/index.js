import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/login', component: () => import('../views/Login.vue') },
  {
    path: '/',
    component: () => import('../views/Layout.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: '看板', component: () => import('../views/Dashboard.vue') },
      { path: 'cases', name: '案件管理', component: () => import('../views/CaseList.vue') },
      { path: 'cases/:id', name: '案件详情', component: () => import('../views/CaseDetail.vue') },
      { path: 'cases-new', name: '新建案件', component: () => import('../views/CaseNew.vue') },
      { path: 'import', name: '批量导入', component: () => import('../views/BatchImport.vue') },
      { path: 'mediators', name: '人员管理', component: () => import('../views/Mediators.vue') }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.path !== '/login' && !token) next('/login')
  else next()
})

export default router
