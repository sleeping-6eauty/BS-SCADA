<script setup>
import { onMounted, onUnmounted, ref } from 'vue'
import logoImage from '@/assets/logo.png'
import AlarmModal from '@/components/AlarmModal.vue'

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
  { label: '사용자 관리', to: '/admin/permission' },
]

const currentTime = ref('')
let timerId

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
}

const handleClickOutside = (e) => {
  if (bellWrapRef.value && !bellWrapRef.value.contains(e.target)) {
    showAlarmModal.value = false
  }
}

onMounted(() => {
  updateCurrentTime()
  timerId = window.setInterval(updateCurrentTime, 1000)
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  window.clearInterval(timerId)
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
    </nav>

    <div class="top-actions">
      <div class="time">◷ {{ currentTime }}</div>
      <div class="admin">👤 관리자</div>
      <div class="bell-wrap" ref="bellWrapRef">
        <button class="bell" type="button" @click="toggleAlarmModal">🔔<span>2</span></button>
        <AlarmModal v-if="showAlarmModal" class="alarm-modal-popup" />
      </div>
    </div>
  </header>
</template>

<style scoped>
.topbar { height: 70px; display: flex; align-items: center; background: linear-gradient(90deg, #071f49, #002e68); color: #fff; box-shadow: 0 4px 14px rgba(4, 24, 56, .2); }
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
