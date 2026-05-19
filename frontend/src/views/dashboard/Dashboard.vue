<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import mqtt from 'mqtt'
import AppTopbar from '@/components/AppTopbar.vue'

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'
const selectedLine = ref('전체')

const rawBrokerUrl = import.meta.env.VITE_MQTT_BROKER_URL ?? ''
const topicFilter = import.meta.env.VITE_MQTT_TOPIC ?? 'factory/equipment/+/realtime'
const topicPrefix = import.meta.env.VITE_MQTT_TOPIC_PREFIX ?? 'factory/equipment'
const configuredEquipmentIds = String(import.meta.env.VITE_MQTT_EQUIPMENT_IDS ?? '')
  .split(',')
  .map((id) => id.trim())
  .filter(Boolean)
const mqttUsername = import.meta.env.VITE_MQTT_USERNAME
const mqttPassword = import.meta.env.VITE_MQTT_PASSWORD

const mqttConnected = ref(false)
const subscriptionTopic = ref(topicFilter)
const connectionMessage = ref('')
const latestByEquipment = ref({})
const equipmentNameMap = ref({})
const alarmByEquipment = ref({})

const detailTableWrapRef = ref(null)
const currentPage = ref(1)
const rowsPerPage = ref(1)

const alarmFetchInFlight = new Set()
const alarmLastFetchedAt = new Map()
const ALARM_REFRESH_MS = 30000

let mqttClient = null
let alarmRefreshTimerId = null
let detailTableResizeObserver = null
let brokerUrlHint = ''

const failureRows = [
  { rank: 1, name: '용접로봇1', type: '용접 로봇', probability: 87, health: 13, color: 'red' },
  { rank: 2, name: '컨베이어3', type: '컨베이어', probability: 65, health: 35, color: 'orange' },
  { rank: 3, name: '차체지그2', type: '차체 지그', probability: 48, health: 52, color: 'yellow' },
  { rank: 4, name: '너트러너4', type: '너트러너', probability: 45, health: 55, color: 'yellow' },
  { rank: 5, name: '비전검사기1', type: '비전 검사기', probability: 20, health: 80, color: 'green' },
]

const lifeRows = [
  { rank: 1, name: '용접로봇1', type: '용접 로봇', life: 13, date: '2024-08-15', color: 'red' },
  { rank: 2, name: '컨베이어3', type: '컨베이어', life: 28, date: '2024-11-02', color: 'orange' },
  { rank: 3, name: '차체지그2', type: '차체 지그', life: 45, date: '2025-02-18', color: 'yellow' },
  { rank: 4, name: '너트러너4', type: '너트러너', life: 60, date: '2025-05-30', color: 'green' },
  { rank: 5, name: '비전검사기1', type: '비전 검사기', life: 78, date: '2025-08-22', color: 'green' },
]

const authHeaders = () => {
  const token = localStorage.getItem('token')
  return token ? { Authorization: `Bearer ${token}` } : {}
}

const normalizeStatus = (status) => {
  const normalized = String(status ?? '').toUpperCase()
  if (normalized === 'RUN') return 'RUN'
  if (normalized === 'STOP') return 'STOP'
  if (normalized === 'ALARM') return 'ALARM'
  return 'UNKNOWN'
}

const statusText = {
  RUN: '가동',
  STOP: '정지',
  ALARM: '알람',
  UNKNOWN: '미확인',
}

const parseTopicEquipmentId = (topic) => {
  const match = /^factory\/equipment\/([^/]+)\/realtime$/.exec(topic ?? '')
  return match?.[1] ?? ''
}

const toSafeInteger = (value) => {
  const numeric = Number(value)
  if (!Number.isFinite(numeric) || numeric < 0) return 0
  return Math.floor(numeric)
}

const formatRunTime = (secondsValue) => {
  const seconds = toSafeInteger(secondsValue)
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  const remainSeconds = seconds % 60
  return `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}:${String(remainSeconds).padStart(2, '0')}`
}

const parseMessagePayload = (buffer) => {
  try {
    const parsed = JSON.parse(buffer.toString())
    return parsed && typeof parsed === 'object' ? parsed : null
  } catch {
    return null
  }
}

const formatDateTime = (value) => {
  if (!value) return '-'
  const text = String(value)
  return text.includes('T') ? text.replace('T', ' ').slice(0, 19) : text.slice(0, 19)
}

const resolveBrokerUrlForBrowser = (rawUrl) => {
  const fallback = 'ws://broker.hivemq.com:8000/mqtt'
  const value = String(rawUrl ?? '').trim()
  if (!value) return fallback
  if (/^wss?:\/\//i.test(value)) return value

  if (/^mqtts?:\/\//i.test(value)) {
    const source = new URL(value)
    const secure = source.protocol.toLowerCase() === 'mqtts:'
    const port = source.port || (secure ? '8883' : '1883')
    source.protocol = secure ? 'wss:' : 'ws:'
    source.port = secure ? (port === '8883' ? '8884' : port) : (port === '1883' ? '8000' : port)
    if (!source.pathname || source.pathname === '/') source.pathname = '/mqtt'
    brokerUrlHint = `브라우저 연결용으로 ${source.href} 사용`
    return source.href
  }

  const hostPort = value.includes(':') ? value : `${value}:8000`
  const normalized = `ws://${hostPort}${value.endsWith('/mqtt') ? '' : '/mqtt'}`
  brokerUrlHint = `브라우저 연결용으로 ${normalized} 사용`
  return normalized
}

const brokerUrl = resolveBrokerUrlForBrowser(rawBrokerUrl)

const equipmentIdsForSubscription = computed(() => {
  const idsFromApi = Object.keys(equipmentNameMap.value)
  if (idsFromApi.length > 0) return idsFromApi
  if (configuredEquipmentIds.length > 0) return configuredEquipmentIds
  return []
})

const upsertRealtimeEquipment = (topic, payload) => {
  const equipmentId = String(payload.equipment_id ?? parseTopicEquipmentId(topic) ?? '').trim()
  if (!equipmentId) return

  latestByEquipment.value = {
    ...latestByEquipment.value,
    [equipmentId]: {
      equipment_id: equipmentId,
      line_no: String(payload.line_no ?? '-'),
      timestamp: String(payload.timestamp ?? '-'),
      status: normalizeStatus(payload.status),
      accumulated_run_seconds: toSafeInteger(payload.accumulated_run_hours),
    },
  }

  fetchLatestAlarmByEquipment(equipmentId)
}

const realtimeRows = computed(() => {
  const rows = Object.values(latestByEquipment.value)
  rows.sort((a, b) => a.equipment_id.localeCompare(b.equipment_id))
  return rows
})

const totalEquipmentCount = computed(() => realtimeRows.value.length)
const runningEquipmentCount = computed(() => realtimeRows.value.filter((row) => row.status === 'RUN').length)
const stoppedEquipmentCount = computed(() => realtimeRows.value.filter((row) => row.status === 'STOP').length)
const alarmEquipmentCount = computed(() => realtimeRows.value.filter((row) => row.status === 'ALARM').length)

const summaryCards = computed(() => ([
  { title: '총 설비 수', value: totalEquipmentCount.value, unit: '대', icon: '🏭', tone: 'blue' },
  { title: '가동 설비 수', value: runningEquipmentCount.value, unit: '대', icon: '▶', tone: 'green' },
  { title: '정지 설비 수', value: stoppedEquipmentCount.value, unit: '대', icon: 'Ⅱ', tone: 'orange' },
  { title: '알람 발생 수', value: alarmEquipmentCount.value, unit: '건', icon: '🚨', tone: 'red' },
]))

const detailTableRows = computed(() => {
  return realtimeRows.value.map((row) => {
    const alarmInfo = alarmByEquipment.value[row.equipment_id] ?? {}
    return {
      rowKey: row.equipment_id,
      equipmentName: equipmentNameMap.value[row.equipment_id] ?? row.equipment_id,
      statusCode: row.status,
      statusText: statusText[row.status] ?? statusText.UNKNOWN,
      runningTimeText: formatRunTime(row.accumulated_run_seconds),
      recentAlarmText: alarmInfo.recentAlarmText ?? '-',
      lastCheckText: alarmInfo.lastCheckText ?? '-',
    }
  })
})

const totalPages = computed(() => Math.max(1, Math.ceil(detailTableRows.value.length / rowsPerPage.value)))
const pagedDetailRows = computed(() => {
  const start = (currentPage.value - 1) * rowsPerPage.value
  return detailTableRows.value.slice(start, start + rowsPerPage.value)
})
const pageNumbers = computed(() => Array.from({ length: totalPages.value }, (_, i) => i + 1))
const showMqttBanner = computed(() => !mqttConnected.value || Boolean(connectionMessage.value))

watch(equipmentIdsForSubscription, () => {
  subscribeEquipmentTopics()
})

watch(totalPages, (nextValue) => {
  if (currentPage.value > nextValue) currentPage.value = nextValue
})

watch(
  () => detailTableRows.value.length,
  async () => {
    await nextTick()
    recalcRowsPerPage()
  },
)

const setPage = (page) => {
  currentPage.value = Math.min(Math.max(page, 1), totalPages.value)
}

const recalcRowsPerPage = () => {
  const wrapEl = detailTableWrapRef.value
  if (!wrapEl) return
  const headerRowEl = wrapEl.querySelector('thead tr')
  const bodyRowEl = wrapEl.querySelector('tbody tr')
  const headerHeight = headerRowEl?.getBoundingClientRect().height ?? 34
  const rowHeight = bodyRowEl?.getBoundingClientRect().height ?? 28
  const availableHeight = Math.max(0, wrapEl.clientHeight - headerHeight - 2)
  rowsPerPage.value = Math.max(1, Math.floor(availableHeight / Math.max(1, rowHeight)))
}

const loadEquipmentNames = async () => {
  try {
    const res = await fetch(`${API_BASE}/api/equipments/names`, { headers: authHeaders() })
    const body = await res.json()
    if (!res.ok || !body?.success || !Array.isArray(body?.data)) return
    const mapped = {}
    body.data.forEach((item) => {
      if (!item?.equipmentId) return
      mapped[item.equipmentId] = item.equipmentName || item.equipmentId
    })
    equipmentNameMap.value = mapped
    subscribeEquipmentTopics()
  } catch {
    // no-op
  }
}

const fetchLatestAlarmByEquipment = async (equipmentId, force = false) => {
  if (!equipmentId) return
  if (alarmFetchInFlight.has(equipmentId)) return
  const now = Date.now()
  const lastFetchedAt = alarmLastFetchedAt.get(equipmentId) ?? 0
  if (!force && now - lastFetchedAt < ALARM_REFRESH_MS) return

  alarmFetchInFlight.add(equipmentId)
  try {
    const res = await fetch(`${API_BASE}/api/alarms/${encodeURIComponent(equipmentId)}`, {
      headers: authHeaders(),
    })
    const body = await res.json()
    if (!res.ok || !body?.success || !Array.isArray(body?.data)) return
    const latestAlarm = body.data[0]
    alarmByEquipment.value = {
      ...alarmByEquipment.value,
      [equipmentId]: {
        recentAlarmText: latestAlarm?.alarmType || latestAlarm?.alarmText || latestAlarm?.alarmStatus || '-',
        lastCheckText: formatDateTime(latestAlarm?.updatedAt),
      },
    }
    alarmLastFetchedAt.set(equipmentId, Date.now())
  } catch {
    // no-op
  } finally {
    alarmFetchInFlight.delete(equipmentId)
  }
}

const refreshAllEquipmentAlarms = () => {
  realtimeRows.value.forEach((row) => fetchLatestAlarmByEquipment(row.equipment_id, true))
}

const buildEquipmentTopic = (equipmentId) => `${topicPrefix}/${equipmentId}/realtime`

const subscribeEquipmentTopics = () => {
  if (!mqttClient || !mqttConnected.value) return
  const topics = [...new Set([topicFilter, ...equipmentIdsForSubscription.value.map(buildEquipmentTopic)])]
  mqttClient.subscribe(topics, { qos: 0 }, (error, granted) => {
    if (error) {
      connectionMessage.value = `구독 실패: ${error.message}`
      return
    }
    const grantedTopics = (granted ?? []).map((item) => item.topic)
    subscriptionTopic.value = `${grantedTopics.length}개 토픽 구독 중 (와일드카드 포함)`
  })
}

const connectMqtt = () => {
  mqttClient = mqtt.connect(brokerUrl, {
    clientId: `bs-scada-web-${Math.random().toString(16).slice(2, 10)}`,
    username: mqttUsername,
    password: mqttPassword,
    reconnectPeriod: 3000,
    connectTimeout: 10000,
    clean: true,
  })

  mqttClient.on('connect', () => {
    mqttConnected.value = true
    connectionMessage.value = ''
    subscribeEquipmentTopics()
  })
  mqttClient.on('reconnect', () => {
    mqttConnected.value = false
    connectionMessage.value = 'MQTT 재연결 중입니다.'
  })
  mqttClient.on('error', (error) => {
    mqttConnected.value = false
    connectionMessage.value = `MQTT 오류: ${error.message}`
  })
  mqttClient.on('close', () => {
    mqttConnected.value = false
  })
  mqttClient.on('message', (topic, payloadBuffer) => {
    const payload = parseMessagePayload(payloadBuffer)
    if (!payload) return
    upsertRealtimeEquipment(topic, payload)
  })
}

onMounted(() => {
  loadEquipmentNames()
  connectMqtt()
  if (brokerUrlHint) connectionMessage.value = brokerUrlHint
  alarmRefreshTimerId = window.setInterval(refreshAllEquipmentAlarms, ALARM_REFRESH_MS)

  nextTick(() => {
    recalcRowsPerPage()
    if (detailTableWrapRef.value && typeof ResizeObserver !== 'undefined') {
      detailTableResizeObserver = new ResizeObserver(() => recalcRowsPerPage())
      detailTableResizeObserver.observe(detailTableWrapRef.value)
    }
  })
})

onUnmounted(() => {
  if (detailTableResizeObserver) {
    detailTableResizeObserver.disconnect()
    detailTableResizeObserver = null
  }
  if (alarmRefreshTimerId) {
    window.clearInterval(alarmRefreshTimerId)
    alarmRefreshTimerId = null
  }
  if (mqttClient) {
    mqttClient.end(true)
    mqttClient = null
  }
})
</script>

<template>
  <div class="dashboard-page">
    <AppTopbar active-menu="대시보드" />

    <main class="content">
      <section v-if="showMqttBanner" class="status-bar">
        <strong>MQTT 상태: {{ mqttConnected ? '연결됨' : '연결 안됨' }}</strong>
        <span>구독 토픽: {{ subscriptionTopic }}</span>
        <p v-if="connectionMessage">{{ connectionMessage }}</p>
      </section>

      <section class="summary-grid">
        <article v-for="card in summaryCards" :key="card.title" class="summary-card">
          <div class="summary-icon" :class="card.tone">{{ card.icon }}</div>
          <div>
            <p>{{ card.title }}</p>
            <strong>{{ card.value }}</strong>
            <span>{{ card.unit }}</span>
          </div>
        </article>
      </section>

      <section class="upper-grid equal-upper">
        <article class="panel oee-card">
          <div class="panel-header">
            <h2>종합설비효율 (OEE)</h2>
            <select v-model="selectedLine" class="line-select">
              <option>전체</option>
              <option>Line1</option>
              <option>Line2</option>
              <option>Line3</option>
            </select>
          </div>
          <div class="oee-body">
            <div class="donut"><span>OEE<br /><b>62.5%</b></span></div>
            <div class="oee-info">
              <div><i class="dot blue"></i>가동률 <strong>89.2%</strong></div>
              <div><i class="dot cyan"></i>성능률 <strong>87.5%</strong></div>
              <div><i class="dot orange"></i>품질률 <strong>80.1%</strong></div>
              <hr />
              <div class="total">OEE (종합 효율) <strong>62.5%</strong></div>
              <small>※ 선택 라인 기준 총 21대 설비 평균</small>
            </div>
          </div>
        </article>

        <article class="panel risk-card">
          <div class="panel-header">
            <h2>설비별 잠재 고장률 (상위 5)</h2>
          </div>
          <table class="data-table risk-table">
            <thead>
              <tr><th>순위</th><th>설비명</th><th>설비 유형</th><th>잠재 고장률</th><th>Health Score</th></tr>
            </thead>
            <tbody>
              <tr v-for="row in failureRows" :key="row.rank">
                <td>{{ row.rank }}</td>
                <td>{{ row.name }}</td>
                <td>{{ row.type }}</td>
                <td class="bar-cell"><span>{{ row.probability }}%</span><b class="track"><i :class="row.color" :style="{ width: row.probability + '%' }"></i></b></td>
                <td><span class="score" :class="row.color">{{ row.health }}</span></td>
              </tr>
            </tbody>
          </table>
        </article>
      </section>

      <section class="lower-grid equal-lower">
        <article class="panel detail-card">
          <div class="panel-header">
            <h2>설비 상세 현황</h2>
          </div>
          <div ref="detailTableWrapRef" class="detail-table-wrap">
            <table class="data-table detail-table">
              <thead>
                <tr><th>설비명</th><th>설비상태</th><th>가동시간</th><th>최근알람</th><th>마지막 점검일</th></tr>
              </thead>
              <tbody>
                <tr v-for="row in pagedDetailRows" :key="`${row.rowKey}-${row.lastCheckText}`">
                  <td>{{ row.equipmentName }}</td>
                  <td><span class="mqtt-status" :class="row.statusCode.toLowerCase()">{{ row.statusText }}</span></td>
                  <td>{{ row.runningTimeText }}</td>
                  <td>{{ row.recentAlarmText }}</td>
                  <td>{{ row.lastCheckText }}</td>
                </tr>
                <tr v-if="detailTableRows.length === 0">
                  <td colspan="5" class="empty-row">MQTT 데이터 수신 대기 중입니다.</td>
                </tr>
              </tbody>
            </table>
          </div>
          <div class="pagination-wrap">
            <b>전체 {{ realtimeRows.length }}건</b>
            <div class="pagination">
              <button type="button" :disabled="currentPage === 1" @click="setPage(currentPage - 1)">‹</button>
              <button
                v-for="page in pageNumbers"
                :key="page"
                type="button"
                :class="{ active: page === currentPage }"
                @click="setPage(page)"
              >
                {{ page }}
              </button>
              <button type="button" :disabled="currentPage === totalPages" @click="setPage(currentPage + 1)">›</button>
            </div>
          </div>
        </article>

        <article class="panel life-card">
          <div class="panel-header">
            <h2>설비별 잔존 수명 (상위 5)</h2>
            <button type="button" class="more-btn">더보기 ›</button>
          </div>
          <table class="data-table life-table">
            <thead>
              <tr><th>순위</th><th>설비명</th><th>설비 유형</th><th>잔존 수명</th><th>예상 교체 시기</th></tr>
            </thead>
            <tbody>
              <tr v-for="row in lifeRows" :key="row.rank">
                <td>{{ row.rank }}</td>
                <td>{{ row.name }}</td>
                <td>{{ row.type }}</td>
                <td class="life-cell"><b class="track"><i :class="row.color" :style="{ width: row.life + '%' }"></i></b><strong :class="row.color">{{ row.life }}%</strong></td>
                <td :class="row.color">{{ row.date }}</td>
              </tr>
            </tbody>
          </table>
        </article>
      </section>
    </main>
  </div>
</template>

<style scoped>
.dashboard-page { min-width: 1180px; min-height: 100vh; background: #f5f7fb; }
.content { padding: 22px 32px 30px; }
.status-bar { display: grid; gap: 2px; margin-bottom: 16px; padding: 10px 14px; border: 1px solid #dce6f3; border-radius: 8px; background: #fff; color: #173252; font-size: 13px; }
.status-bar strong { font-size: 14px; }
.status-bar p { margin: 0; color: #bd2d3d; font-weight: 700; }
.summary-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 18px; margin-bottom: 16px; }
.summary-card { height: 142px; display: flex; align-items: center; gap: 22px; padding: 26px 32px; background: #fff; border-radius: 14px; box-shadow: 0 5px 16px rgba(13, 36, 72, .12); }
.summary-icon { width: 88px; height: 88px; display: grid; place-items: center; border-radius: 50%; font-size: 40px; }
.summary-icon.blue { background: #e4f0ff; color: #1474df; } .summary-icon.green { background: #e1f6ef; color: #12a985; } .summary-icon.orange { background: #fff0df; color: #df7922; } .summary-icon.red { background: #ffe7eb; color: #fa2c45; }
.summary-card p { margin: 0 0 4px; font-size: 16px; font-weight: 900; } .summary-card strong { font-size: 46px; line-height: 1; font-weight: 950; } .summary-card span { margin-left: 8px; font-weight: 800; }
.upper-grid, .lower-grid { display: grid; grid-template-columns: .95fr 1.05fr; gap: 16px; margin-bottom: 16px; }
.panel { background: #fff; border-radius: 14px; box-shadow: 0 5px 16px rgba(13, 36, 72, .11); padding: 16px 18px; overflow: visible; }
.equal-upper .panel { height: 340px; } .equal-lower .panel { height: 340px; }
.panel-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
h2 { margin: 0; font-size: 20px; font-weight: 950; letter-spacing: 0; }
.line-select { width: 116px; height: 36px; border: 1px solid #d8e1ed; border-radius: 6px; padding: 0 10px; color: #0d2448; font-weight: 800; background: #fff; }
.oee-body { height: calc(100% - 48px); display: flex; align-items: center; justify-content: space-around; }
.donut { width: 190px; height: 190px; border-radius: 50%; background: conic-gradient(#1375de 0 58%, #e8892c 58% 78%, #1599b6 78% 100%); position: relative; display: grid; place-items: center; }
.donut::after { content: ''; position: absolute; width: 112px; height: 112px; background: #fff; border-radius: 50%; } .donut span { position: relative; z-index: 1; text-align: center; font-weight: 900; } .donut b { font-size: 30px; }
.oee-info { width: 330px; font-size: 16px; font-weight: 850; } .oee-info div { display: flex; align-items: center; justify-content: space-between; margin: 10px 0; } .oee-info strong { color: #126de0; font-weight: 950; } .oee-info small { display: block; margin-top: 12px; font-weight: 800; }
.dot { display: inline-block; border-radius: 50%; margin-right: 10px; width: 12px; height: 12px; } .blue { background: #1375de; color: #1375de; } .cyan { background: #1599b6; } .orange { background: #ff6b16; color: #ff6b16; } .yellow { background: #ff9f1a; color: #ff9f1a; } .green { background: #12b58f; color: #12b58f; } .red { background: #ff3045; color: #ff3045; }
.data-table { width: 100%; border-collapse: separate; border-spacing: 0; table-layout: fixed; border: 1px solid #e2e7ee; border-radius: 10px; overflow: hidden; font-size: 14px; }
.data-table th, .data-table td { height: 34px; padding: 6px 10px; text-align: center; font-weight: 850; border-bottom: 1px solid #edf1f6; white-space: nowrap; }
.data-table th { background: #fafafa; color: #142b50; font-size: 13px; font-weight: 950; } .data-table tr:last-child td { border-bottom: 0; }
.bar-cell, .life-cell { display: flex; align-items: center; gap: 10px; } .bar-cell span { width: 38px; text-align: left; } .track { flex: 1; height: 8px; background: #e8e9eb; border-radius: 99px; overflow: hidden; } .track i { display: block; height: 100%; border-radius: 99px; }
.score { display: inline-block; min-width: 68px; padding: 3px 12px; border: 1px solid currentColor; border-radius: 7px; background: rgba(255,255,255,.85); font-weight: 950; }
.detail-card, .life-card { display: flex; flex-direction: column; min-height: 0; }
.detail-table-wrap { min-height: 0; flex: 1; overflow: hidden; }
.detail-table { font-size: 13px; } .detail-table th, .detail-table td { height: 28px; padding: 4px 8px; }
.pagination-wrap { margin-top: 10px; min-height: 34px; display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.pagination-wrap b { font-size: 13px; color: #304d6f; }
.pagination { display: flex; gap: 8px; align-items: center; }
.pagination button { min-width: 34px; height: 30px; border: 1px solid #dfe5ee; background: #fff; border-radius: 6px; font-weight: 900; color: #122a4c; cursor: pointer; }
.pagination button:disabled { opacity: 0.45; cursor: not-allowed; }
.pagination button.active { background: linear-gradient(180deg, #0fc6d2, #0896b7); color: #fff; border-color: transparent; }
.more-btn { border: 0; background: transparent; color: #0d2448; font-weight: 950; cursor: pointer; }
.life-table th, .life-table td { height: 42px; } .life-cell strong { min-width: 46px; background: transparent !important; font-weight: 950; }
.life-table td.red, .life-table td.orange, .life-table td.yellow, .life-table td.green { background: transparent; font-weight: 950; }
.mqtt-status { display: inline-flex; align-items: center; justify-content: center; min-width: 64px; height: 24px; padding: 0 8px; border-radius: 6px; font-size: 12px; font-weight: 900; }
.mqtt-status.run { color: #0b8e5e; background: #e8f8f1; }
.mqtt-status.stop { color: #dc5b00; background: #fff1e3; }
.mqtt-status.alarm { color: #be2135; background: #ffebee; }
.mqtt-status.unknown { color: #687f97; background: #eef3f8; }
.empty-row { color: #6b8098; font-weight: 850; }
@media (max-width: 1280px) { .dashboard-page { min-width: 1200px; } }
</style>
