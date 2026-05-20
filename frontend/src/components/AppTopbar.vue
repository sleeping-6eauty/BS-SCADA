<script setup>
import { onMounted, onUnmounted, ref } from 'vue'
import logoImage from '@/assets/logo.png'
import AlarmModal from '@/components/AlarmModal.vue'
import { getAlarmLog, getMyEquipments } from '@/api/alarm.js'

defineProps({
  activeMenu: {
    type: String,
    default: '대시보드',
  },
})

const menuItems = [
  { label: '대시보드', to: '/dashboard' },
  { label: '설비 현황', to: '/equipment-monitor' },
  { label: '설비 상세', to: '/equipment-detail' },
  { label: '알람 관리', to: '/equipment-alarm' },
  { label: '수명 관리', to: '/life' },
  { label: '사용자 관리', to: '/admin/permission', adminOnly: true },
]

const currentTime = ref('')
let timerId
let alarmTimerId

const API_BASE = 'http://localhost:8080'

const currentUserName = ref('사용자')
const currentUserId = ref(null)
const isAdmin = ref(false)
const openAlarmCount = ref(0)

const pad = (value) => String(value).padStart(2, '0')

const formatDateTime = (date) => {
  const year = date.getFullYear()
  const month = pad(date.getMonth() + 1)
  const day = pad(date.getDate())
  const hours = pad(date.getHours())
  const minutes = pad(date.getMinutes())
  const seconds = pad(date.getSeconds())

  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
}

const updateCurrentTime = () => {
  currentTime.value = formatDateTime(new Date())
}

const showAlarmModal = ref(false)
const bellWrapRef = ref(null)

const toggleAlarmModal = () => {
  showAlarmModal.value = !showAlarmModal.value
  if (showAlarmModal.value) {
    loadOpenAlarmCount()
  }
}

const handleClickOutside = (e) => {
  if (bellWrapRef.value && !bellWrapRef.value.contains(e.target)) {
    showAlarmModal.value = false
  }
}

const asArray = (payload) => {
  if (Array.isArray(payload)) return payload
  if (Array.isArray(payload?.items)) return payload.items
  if (Array.isArray(payload?.content)) return payload.content
  if (Array.isArray(payload?.rows)) return payload.rows
  if (Array.isArray(payload?.list)) return payload.list
  return []
}

const pick = (obj, keys, fallback = '') => {
  for (const key of keys) {
    if (obj?.[key] !== undefined && obj?.[key] !== null && obj?.[key] !== '') return obj[key]
  }
  return fallback
}

const normalizeId = (value) => String(value ?? '').trim()

const getStoredUser = () => {
  try {
    return JSON.parse(localStorage.getItem('user') || '{}')
  } catch {
    return {}
  }
}

const checkAdmin = (role) => String(role ?? '').toUpperCase().replace(/^ROLE_/, '') === 'ADMIN'

const loadCurrentUser = async () => {
  const storedUser = getStoredUser()
  currentUserId.value = storedUser.id ?? null
  currentUserName.value = storedUser.name || storedUser.username || '사용자'
  isAdmin.value = checkAdmin(storedUser.role)

  const token = localStorage.getItem('token')
  if (!token) return

  try {
    const res = await fetch(`${API_BASE}/api/auth/me`, {
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
      },
    })
    const user = await res.json().catch(() => ({}))
    if (!res.ok) return

    currentUserId.value = user.userId ?? storedUser.id ?? null
    currentUserName.value = user.name || storedUser.name || storedUser.username || '사용자'
    isAdmin.value = checkAdmin(user.role)
  } catch {
    currentUserName.value = storedUser.name || storedUser.username || '사용자'
  }
}

const loadOpenAlarmCount = async () => {
  try {
    const [alarmPayload, equipmentPayload] = await Promise.all([
      getAlarmLog(),
      getMyEquipments(),
    ])

    const assignedEquipmentIds = new Set(
      asArray(equipmentPayload)
        .map((item) => normalizeId(pick(item, ['equipment_id', 'equipmentId', 'id'])))
        .filter(Boolean)
    )

    const userId = normalizeId(currentUserId.value)
    openAlarmCount.value = asArray(alarmPayload).filter((alarm) => {
      const equipmentId = normalizeId(pick(alarm, ['equipment_id', 'equipmentId']))
      const status = String(pick(alarm, ['alarm_status', 'alarmStatus', 'status'])).toUpperCase()
      const alarmUserId = normalizeId(pick(alarm, ['user_id', 'userId']))
      const isMine = !alarmUserId || !userId || alarmUserId === userId
      return assignedEquipmentIds.has(equipmentId) && status === 'OPEN' && isMine
    }).length
  } catch {
    openAlarmCount.value = 0
  }
}

onMounted(() => {
  updateCurrentTime()
  timerId = window.setInterval(updateCurrentTime, 1000)
  loadCurrentUser().finally(loadOpenAlarmCount)
  alarmTimerId = window.setInterval(loadOpenAlarmCount, 30000)
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  window.clearInterval(timerId)
  window.clearInterval(alarmTimerId)
  document.removeEventListener('click', handleClickOutside)
})
</script>

<template>
  <header class="topbar">
    <RouterLink class="brand" to="/dashboard" aria-label="대시보드로 이동">
      <img class="brand-logo" :src="logoImage" alt="BS-SCADA logo" />
      <strong>BS-SCADA</strong>
    </RouterLink>

    <nav class="gnb">
      <template v-for="item in menuItems" :key="item.label">
        <template v-if="!item.adminOnly || isAdmin">
          <RouterLink
            v-if="item.to"
            :to="item.to"
            :class="{ active: item.label === activeMenu }"
          >
            {{ item.label }}
          </RouterLink>
          <a v-else :class="{ active: item.label === activeMenu }">
            {{ item.label }}
          </a>
        </template>
      </template>
    </nav>

    <div class="top-actions">
      <div class="time">◷ {{ currentTime }}</div>
      <div class="admin">👤 {{ currentUserName }}</div>
      <div class="bell-wrap" ref="bellWrapRef">
        <button class="bell" type="button" @click="toggleAlarmModal">🔔<span>{{ openAlarmCount }}</span></button>
        <AlarmModal v-if="showAlarmModal" class="alarm-modal-popup" />
      </div>
    </div>
  </header>
  <div class="topbar-spacer" aria-hidden="true"></div>
</template>

<style scoped>
.topbar { position: fixed; top: 0; left: 0; z-index: 1000; width: 100%; height: 70px; display: flex; align-items: center; background: linear-gradient(90deg, #071f49, #002e68); color: #fff; box-shadow: 0 4px 14px rgba(4, 24, 56, .2); }
.topbar-spacer { height: 70px; flex: 0 0 70px; }
.brand { width: 250px; height: 100%; display: flex; align-items: center; gap: 12px; padding: 0 30px; border-right: 1px solid rgba(255,255,255,.12); font-size: 20px; font-weight: 900; white-space: nowrap; }
.brand-logo { width: 42px; height: 42px; flex: 0 0 42px; display: block; object-fit: cover; border-radius: 12px; }
.gnb { flex: 1; min-width: 0; display: flex; height: 100%; }
.gnb a { min-width: 118px; display: grid; place-items: center; font-size: 16px; font-weight: 800; color: rgba(255,255,255,.9); position: relative; white-space: nowrap; }
.gnb a.active::after { content: ''; position: absolute; left: 18px; right: 18px; bottom: 0; height: 5px; background: #16c7d8; border-radius: 8px 8px 0 0; }
.top-actions { flex-shrink: 0; margin-left: auto; height: 100%; display: flex; align-items: center; }
.time, .admin { height: 100%; display: flex; align-items: center; gap: 10px; padding: 0 22px; font-weight: 800; border-left: 1px solid rgba(255,255,255,.12); }
.bell-wrap { position: relative; height: 100%; display: flex; align-items: center; border-left: 1px solid rgba(255,255,255,.12); }
.bell { height: 100%; display: flex; align-items: center; gap: 10px; padding: 0 22px; font-weight: 800; position: relative; border: 0; color: #fff; background: transparent; font-size: 22px; cursor: pointer; }
.bell span { position: absolute; top: 16px; right: 15px; min-width: 18px; height: 18px; padding: 0 5px; border-radius: 999px; background: #ff2d47; color: #fff; font-size: 12px; }
.alarm-modal-popup { position: absolute; top: calc(100% + 8px); right: 0; z-index: 1000; }

@media (max-width: 1280px) {
  .gnb a { min-width: 104px; font-size: 15px; }
  .brand { width: 220px; }
}
</style>
