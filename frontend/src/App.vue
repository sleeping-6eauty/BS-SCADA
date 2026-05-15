// AlarmModal.vue 임시 확인용으로 추가해둔 코드 (실제로는 App.vue에서 AlarmModal 사용X)
<script setup>
import AlarmModal from './components/AlarmModal.vue'
</script>

<template>
  <div style="padding: 40px; background: #f5f7fb; min-height: 100vh;">
    <AlarmModal />
  </div>
  <RouterView />
</template>