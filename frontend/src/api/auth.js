import { reactive } from 'vue'

export const auth = reactive({
  token: localStorage.getItem('token') || '',
  user: JSON.parse(localStorage.getItem('user') || 'null'),
  set(token, user) {
    this.token = token
    this.user = user
    localStorage.setItem('token', token)
    localStorage.setItem('user', JSON.stringify(user))
  },
  logout() {
    this.token = ''
    this.user = null
    localStorage.clear()
  },
  get role() {
    return this.user?.role
  },
  get isParty() {
    return this.user?.role === 'PARTY'
  }
})

export const ROLE_LABELS = {
  ADMIN: '管理员',
  WINDOW: '立案窗口',
  MEDIATOR: '调解员',
  PARTY: '当事人',
  JUDGE_ASSISTANT: '法官助理',
  EXECUTION: '执行联络'
}

export const STATUS_OPTIONS = [
  { value: 'PENDING_TRIAGE', label: '待分流', type: 'info' },
  { value: 'PENDING_SCHEDULE', label: '待排期', type: 'warning' },
  { value: 'SCHEDULED', label: '已排期', type: 'primary' },
  { value: 'MEDIATING', label: '调解中', type: 'primary' },
  { value: 'AGREED', label: '已达成协议', type: 'success' },
  { value: 'PERFORMING', label: '履行中', type: 'warning' },
  { value: 'COMPLETED', label: '已履行完毕', type: 'success' },
  { value: 'FAILED', label: '调解不成', type: 'danger' },
  { value: 'TRANSFERRED', label: '已转立案', type: 'danger' }
]

export const CAUSE_OPTIONS = ['民间借贷', '物业费', '交通事故', '邻里纠纷', '小额买卖合同']

export function statusLabel(v) {
  return STATUS_OPTIONS.find(s => s.value === v)?.label || v
}

export function statusType(v) {
  return STATUS_OPTIONS.find(s => s.value === v)?.type || 'info'
}
