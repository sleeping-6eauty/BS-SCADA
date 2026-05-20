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
      <div v-if="loading" class="alarm-empty">
        알람을 불러오는 중입니다.
      </div>

      <div v-else-if="errorMessage" class="alarm-empty error">
        {{ errorMessage }}
      </div>

      <div v-else-if="alarms.length === 0" class="alarm-empty">
        담당 설비 알람이 없습니다.
      </div>

      <template v-else>
        <div
          v-for="alarm in alarms"
          :key="alarm.id"
          class="alarm-item"
        >
          <div class="alarm-left">
            <div class="alarm-content">
              <h4>{{ alarm.title }}</h4>
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
      </template>
    </div>

    <button class="view-all-btn">
      전체 알람 보기
    </button>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { getAlarmLog, getMyEquipments } from '@/api/alarm.js'

const alarms = ref([])
const loading = ref(false)
const errorMessage = ref('')

const asArray = (payload) => {
  if (Array.isArray(payload)) return payload
  if (Array.isArray(payload?.items)) return payload.items
  if (Array.isArray(payload?.content)) return payload.content
  if (Array.isArray(payload?.rows)) return payload.rows
  if (Array.isArray(payload?.list)) return payload.list
  return []
}

const pick = (obj, keys, fallback = '-') => {
  for (const key of keys) {
    if (obj?.[key] !== undefined && obj?.[key] !== null && obj?.[key] !== '') return obj[key]
  }
  return fallback
}

const formatDateTime = (value) => {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  return date.toLocaleString('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false,
  }).replace(/\. /g, '-').replace('.', '')
}

const normalizeStatusValue = (status) => {
  const text = String(status ?? '').trim()
  const normalized = text.toUpperCase()
  if (['OPEN', '미조치', 'UNRESOLVED'].includes(normalized)) return 'OPEN'
  if (['IN_PROGRESS', '조치중', 'PROGRESS'].includes(normalized)) return 'IN_PROGRESS'
  if (['RESOLVED', '완료', 'DONE', 'CLOSED'].includes(normalized)) return 'RESOLVED'
  return text || '-'
}

const getStatusLabel = (status) => {
  const normalized = normalizeStatusValue(status)
  if (normalized === 'OPEN') return '미조치'
  if (normalized === 'IN_PROGRESS') return '조치중'
  if (normalized === 'RESOLVED') return '완료'
  return normalized
}

const getStatusClass = (status) => {
  const normalized = normalizeStatusValue(status)
  if (normalized === 'OPEN') return 'high'
  if (normalized === 'IN_PROGRESS') return 'progress'
  if (normalized === 'RESOLVED') return 'done'
  return 'done'
}

const normalizeAlarm = (row) => {
  const equipmentId = String(pick(row, ['equipment_id', 'equipmentId'], ''))
  const alarmType = String(pick(row, ['alarm_type', 'alarmType', 'type'], '-'))
  const alarmStatus = String(pick(row, ['alarm_status', 'alarmStatus', 'status'], '-'))
  const alarmTime = pick(row, ['timestamp', 'created_at', 'createdAt', 'time'], '')

  return {
    id: String(pick(row, ['alarm_id', 'alarmId', 'id', 'log_id', 'logId'], `${equipmentId}-${alarmTime}-${alarmType}`)),
    equipmentId,
    title: `${equipmentId || '-'} - ${alarmType}`,
    time: formatDateTime(alarmTime),
    level: getStatusClass(alarmStatus),
    levelText: getStatusLabel(alarmStatus),
  }
}

const normalizeEquipmentId = (item) =>
  String(pick(item, ['equipment_id', 'equipmentId', 'id'], '')).trim()

const loadMyAlarmLogs = async () => {
  loading.value = true
  errorMessage.value = ''

  try {
    const [alarmPayload, equipmentPayload] = await Promise.all([
      getAlarmLog(),
      getMyEquipments(),
    ])

    const assignedEquipmentIds = new Set(
      asArray(equipmentPayload)
        .map(normalizeEquipmentId)
        .filter(Boolean)
    )

    alarms.value = asArray(alarmPayload)
      .map(normalizeAlarm)
      .filter((alarm) => assignedEquipmentIds.has(alarm.equipmentId))
  } catch (err) {
    errorMessage.value = err.message || '담당 설비 알람을 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

onMounted(loadMyAlarmLogs)
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
  max-height: 460px;
  overflow-y: auto;
  border-radius: 16px;
  border: 1px solid #eef2f7;
}

.alarm-empty {
  padding: 24px 20px;
  background: #fff;
  color: #506080;
  font-size: 13px;
  font-weight: 700;
  text-align: center;
}

.alarm-empty.error {
  color: #dc2626;
  background: #fff5f5;
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
  min-width: 0;
  flex: 1;
}

.alarm-content h4 {
  margin: 0 0 6px;
  font-size: 14px;
  font-weight: 800;
  color: #162044;
  line-height: 1.3;
  word-break: break-word;
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
  min-width: max-content;
  height: 28px;
  padding: 0 10px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-left: 12px;
  flex-shrink: 0;
  white-space: nowrap;
}

.alarm-badge.high {
  color: #ef4444;
  background: #fff1f1;
  border: 1px solid #ffd4d4;
}

.alarm-badge.progress {
  color: #f59e0b;
  background: #fff8ea;
  border: 1px solid #ffe3a6;
}

.alarm-badge.done {
  color: #15803d;
  background: #dcfce7;
  border: 1px solid #bbf7d0;
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
