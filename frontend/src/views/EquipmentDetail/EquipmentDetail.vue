<script setup>
import AppTopbar from '@/components/AppTopbar.vue'

const equipmentDetailFrameUrl = 'http://222.108.180.36:11005/?Pro=loy_260429#%EC%84%A4%EB%B9%84_%EC%83%81%EC%84%B8'
</script>

<template>
  <div class="equipment-detail-page">
    <AppTopbar active-menu="설비 상세" />

    <main class="iframe-wrap">
      <iframe
        class="equipment-frame"
        :src="equipmentDetailFrameUrl"
        title="설비 상세"
      ></iframe>
    </main>
  </div>
</template>

<style scoped>
.equipment-detail-page { min-width: 1180px; min-height: 100vh; background: #f5f7fb; }
.iframe-wrap { height: calc(100vh - 70px); background: #fff; }
.equipment-frame { width: 100%; height: 100%; display: block; border: 0; background: #fff; }

@media (max-width: 1280px) {
  .equipment-detail-page { min-width: 1100px; }
}
</style>
