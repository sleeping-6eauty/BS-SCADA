<script setup>
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
</script>

<template>
  <header class="topbar">
    <div class="brand">
      <div class="brand-icon">⚙</div>
      <strong>SFaaS 설비 모니터링 시스템</strong>
    </div>

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
      <div class="time">◷ 2024-05-24 10:30:45</div>
      <div class="admin">👤 관리자</div>
      <button class="bell" type="button">🔔<span>2</span></button>
    </div>
  </header>
</template>

<style scoped>
.topbar { height: 70px; display: flex; align-items: center; background: linear-gradient(90deg, #071f49, #002e68); color: #fff; box-shadow: 0 4px 14px rgba(4, 24, 56, .2); }
.brand { width: 305px; height: 100%; display: flex; align-items: center; gap: 12px; padding: 0 30px; border-right: 1px solid rgba(255,255,255,.12); font-size: 19px; }
.brand-icon { width: 34px; height: 34px; display: grid; place-items: center; border: 1px solid rgba(255,255,255,.45); border-radius: 10px; }
.gnb { flex: 1; min-width: 0; display: flex; height: 100%; }
.gnb a { min-width: 118px; display: grid; place-items: center; font-size: 16px; font-weight: 800; color: rgba(255,255,255,.9); position: relative; white-space: nowrap; }
.gnb a.active::after { content: ''; position: absolute; left: 18px; right: 18px; bottom: 0; height: 5px; background: #16c7d8; border-radius: 8px 8px 0 0; }
.top-actions { flex-shrink: 0; margin-left: auto; height: 100%; display: flex; align-items: center; }
.time, .admin, .bell { height: 100%; display: flex; align-items: center; gap: 10px; padding: 0 22px; font-weight: 800; border-left: 1px solid rgba(255,255,255,.12); }
.bell { position: relative; border: 0; color: #fff; background: transparent; font-size: 22px; cursor: pointer; }
.bell span { position: absolute; top: 16px; right: 15px; min-width: 18px; height: 18px; padding: 0 5px; border-radius: 999px; background: #ff2d47; color: #fff; font-size: 12px; }

@media (max-width: 1280px) {
  .gnb a { min-width: 104px; font-size: 15px; }
  .brand { width: 280px; }
}
</style>
