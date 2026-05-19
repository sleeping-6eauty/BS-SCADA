<template>
  <div class="alarm-modal">
    <div class="alarm-header">
      <h3>알람 알림</h3>
      <button class="more-btn">
        더보기
        <span>›</span>
      </button>
    </div>

    <div class="alarm-list">
      <div
        v-for="alarm in alarms"
        :key="alarm.id"
        class="alarm-item"
      >
        <div class="alarm-left">
          <div class="icon-wrapper" :class="alarm.level">
            <span class="alarm-icon">
              {{ alarm.icon }}
            </span>
          </div>

          <div class="alarm-content">
            <h4>{{ alarm.title }}</h4>
            <p>{{ alarm.location }}</p>
            <span>{{ alarm.time }}</span>
          </div>
        </div>

        <div
          class="alarm-badge"
          :class="alarm.level"
        >
          {{ alarm.levelText }}
        </div>
      </div>
    </div>

    <button class="view-all-btn">
      전체 알람 보기
    </button>
  </div>
</template>

<script setup>
const alarms = [
  {
    id: 1,
    title: '용접기1 - 모터 과열',
    location: 'Zone A - Line 1',
    time: '2024-05-24 10:25:33',
    level: 'high',
    levelText: 'High',
    icon: '🚨',
  },
  {
    id: 2,
    title: 'Nutrunner B4 - 토크 이상',
    location: 'Zone B - Line 2',
    time: '2024-05-24 09:42:17',
    level: 'medium',
    levelText: 'Medium',
    icon: '⚠',
  },
  {
    id: 3,
    title: 'Conveyor C2 - 센서 불량',
    location: 'Zone C - Line 3',
    time: '2024-05-24 09:15:08',
    level: 'medium',
    levelText: 'Medium',
    icon: '⚠',
  },
  {
    id: 4,
    title: 'AGV D1 - 배터리 경고',
    location: 'Zone D - Line 4',
    time: '2024-05-24 08:55:21',
    level: 'low',
    levelText: 'Low',
    icon: 'ⓘ',
  },
]
</script>

<style scoped>
.alarm-modal {
  width: 420px;
  background: #ffffff;
  border-radius: 20px;
  box-shadow:
    0 10px 40px rgba(15, 23, 42, 0.12),
    0 2px 10px rgba(15, 23, 42, 0.06);
  padding: 24px;
  border: 1px solid #eef2f7;
}

.alarm-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.alarm-header h3 {
  font-size: 16px;
  font-weight: 800;
  color: #1e2b5c;
  margin: 0;
}

.more-btn {
  border: none;
  background: transparent;
  color: #1e2b5c;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
}

.alarm-list {
  display: flex;
  flex-direction: column;
  gap: 0;
  overflow: hidden;
  border-radius: 16px;
  border: 1px solid #eef2f7;
}

.alarm-item {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 26px 20px;
  border-bottom: 1px solid #eef2f7;
  background: #fff;
}

.alarm-item:last-child {
  border-bottom: none;
}

.alarm-left {
  display: flex;
  gap: 18px;
}

.icon-wrapper {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.icon-wrapper.high {
  background: #fff1f1;
}

.icon-wrapper.medium {
  background: #fff8ea;
}

.icon-wrapper.low {
  background: #eef5ff;
}

.alarm-icon {
  font-size: 20px;
}

.alarm-content h4 {
  margin: 0 0 6px;
  font-size: 14px;
  font-weight: 800;
  color: #162044;
  line-height: 1.3;
}

.alarm-content p {
  margin: 0 0 4px;
  font-size: 12px;
  color: #506080;
  font-weight: 600;
}

.alarm-content span {
  font-size: 11px;
  color: #5f6f92;
  font-weight: 500;
}

.alarm-badge {
  min-width: 60px;
  height: 28px;
  padding: 0 10px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-left: 12px;
}

.alarm-badge.high {
  color: #ef4444;
  background: #fff1f1;
  border: 1px solid #ffd4d4;
}

.alarm-badge.medium {
  color: #f59e0b;
  background: #fff8ea;
  border: 1px solid #ffe3a6;
}

.alarm-badge.low {
  color: #3b82f6;
  background: #eef5ff;
  border: 1px solid #cfe1ff;
}

.view-all-btn {
  width: 100%;
  height: 40px;
  margin-top: 16px;
  border-radius: 10px;
  border: 1px solid #3b82f6;
  background: #ffffff;
  color: #2563eb;
  font-size: 14px;
  font-weight: 800;
  cursor: pointer;
  transition: all 0.2s ease;
}

.view-all-btn:hover {
  background: #eff6ff;
}
</style>