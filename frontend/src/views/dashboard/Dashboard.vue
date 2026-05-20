<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import mqtt from 'mqtt'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import AppTopbar from '@/components/AppTopbar.vue'

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'
const router = useRouter()
const selectedLine = ref('전체')
const oeeProgress = ref(0)

const defaultBrokerUrl = (() => {
  if (typeof window !== 'undefined' && window.location.protocol === 'https:') {
    return 'wss://broker.mqttdashboard.com:8884/mqtt'
  }
  return 'ws://broker.mqttdashboard.com:8000/mqtt'
})()

const rawBrokerUrl = import.meta.env.VITE_MQTT_BROKER_URL ?? defaultBrokerUrl
const topicFilter = import.meta.env.VITE_MQTT_TOPIC ?? 'factory/equipment/+/realtime'
const mqttUsername = import.meta.env.VITE_MQTT_USERNAME
const mqttPassword = import.meta.env.VITE_MQTT_PASSWORD

const equipmentRealtimeMap = ref({})
const alarmMetaByEquipment = ref({})
const alarmFetchInFlight = new Set()
const alarmLastFetchedAt = new Map()
const ALARM_REFRESH_MS = 30000
let mqttClient = null
let runtimeTimer = null
let stompClient = null

const oeeAll = ref(null)
const oeeByLine = ref({})
const lifeRows = ref([])
const isLifeLoading = ref(true)

const activeOee = computed(() => {
  if (selectedLine.value === '전체') return oeeAll.value
  const lineNo = selectedLine.value.replace('Line', '')
  return oeeByLine.value[lineNo] ?? null
})

const fmtPct = (v) => v != null ? (v * 100).toFixed(1) + '%' : '--'

const normalizeStatus = (status) => {
  const normalized = String(status ?? '').toUpperCase()
  if (normalized === 'RUN') return 'RUN'
  if (normalized === 'STOP') return 'STOP'
  if (normalized === 'ALARM') return 'ALARM'
  if (normalized === 'IDLE') return 'IDLE'
  return 'UNKNOWN'
}

const parseTopicEquipmentId = (topic) => {
  const match = /^factory\/equipment\/([^/]+)\/realtime$/i.exec(topic ?? '')
  return match?.[1] ?? ''
}

const toNonNegativeNumber = (value) => {
  const numeric = Number(value)
  if (!Number.isFinite(numeric) || numeric < 0) return 0
  return numeric
}

const parsePayload = (buffer) => {
  try {
    const parsed = JSON.parse(buffer.toString())
    return parsed && typeof parsed === 'object' ? parsed : null
  } catch {
    return null
  }
}

const authHeaders = () => {
  const token = localStorage.getItem('token')
  return token ? { Authorization: `Bearer ${token}` } : {}
}

const toMillis = (value) => {
  if (!value) return 0
  const normalized = String(value).replace(' ', 'T')
  const parsed = Date.parse(normalized)
  return Number.isNaN(parsed) ? 0 : parsed
}

const formatDateTime = (value) => {
  if (!value) return '-'
  const text = String(value)
  return text.includes('T') ? text.replace('T', ' ').slice(0, 19) : text.slice(0, 19)
}

const formatRuntime = (value) => {
  const sec = Math.floor(toNonNegativeNumber(value))
  const hh = String(Math.floor(sec / 3600)).padStart(2, '0')
  const mm = String(Math.floor((sec % 3600) / 60)).padStart(2, '0')
  const ss = String(sec % 60).padStart(2, '0')
  return `${hh}:${mm}:${ss}`
}

const resolveBrokerUrlForBrowser = (rawUrl) => {
  const value = String(rawUrl ?? '').trim()
  if (!value) return defaultBrokerUrl

  const normalizeHost = (hostname) => {
    const lower = String(hostname ?? '').toLowerCase()
    if (lower === 'mqtt-dashboard.com') return 'broker.mqttdashboard.com'
    if (lower === 'broker.mqtt-dashboard.com') return 'broker.mqttdashboard.com'
    return hostname
  }

  if (/^(wss?|mqtts?):\/\//i.test(value)) {
    const source = new URL(value)
    const protocol = source.protocol.toLowerCase()
    const isSecure = protocol === 'wss:' || protocol === 'mqtts:'
    source.hostname = normalizeHost(source.hostname)
    source.protocol = isSecure ? 'wss:' : 'ws:'
    if (!source.port) source.port = isSecure ? '8884' : '8000'
    if (!isSecure && source.port === '1883') source.port = '8000'
    if (isSecure && source.port === '8883') source.port = '8884'
    if (!source.pathname || source.pathname === '/') source.pathname = '/mqtt'
    return source.href
  }

  // host[:port] 형태 입력 대응
  const normalizedInput = value
    .replace(/^mqtt-dashboard\.com(:|$)/i, 'broker.mqttdashboard.com$1')
    .replace(/^broker\.mqtt-dashboard\.com(:|$)/i, 'broker.mqttdashboard.com$1')
  const url = new URL(`ws://${normalizedInput}`)
  if (!url.port || url.port === '1883') url.port = '8000'
  if (!url.pathname || url.pathname === '/') url.pathname = '/mqtt'
  return url.href
}

const realtimeRows = computed(() => {
  const rows = Object.values(equipmentRealtimeMap.value)
  rows.sort((a, b) => a.equipment_id.localeCompare(b.equipment_id))
  return rows
})

const totalEquipmentCount = computed(() => realtimeRows.value.length)
const runningEquipmentCount = computed(() => realtimeRows.value.filter((row) => row.status === 'RUN').length)
const idleEquipmentCount = computed(() => realtimeRows.value.filter((row) => row.status === 'IDLE').length)
const stoppedEquipmentCount = computed(() => realtimeRows.value.filter((row) => row.status === 'STOP').length)
const alarmEquipmentCount = computed(() => realtimeRows.value.filter((row) => row.status === 'ALARM').length)

const summaryCards = computed(() => ([
  { title: '총 설비 수', value: totalEquipmentCount.value, unit: '대', icon: '🏭', tone: 'blue' },
  { title: '가동 설비 수', value: runningEquipmentCount.value, unit: '대', icon: '▶', tone: 'green' },
  { title: '대기 설비 수', value: idleEquipmentCount.value, unit: '대', icon: '⏳', tone: 'sky' },
  { title: '정지 설비 수', value: stoppedEquipmentCount.value, unit: '대', icon: 'Ⅱ', tone: 'orange' },
  { title: '알람 발생 수', value: alarmEquipmentCount.value, unit: '건', icon: '🚨', tone: 'red' },
]))

const detailRows = computed(() => {
  return realtimeRows.value.map((row) => ({
    equipment_id: row.equipment_id,
    status: row.status,
    accumulated_run_hours: row.accumulated_run_hours,
    runtimeText: formatRuntime(row.accumulated_run_hours),
    recentAlarmAt: alarmMetaByEquipment.value[row.equipment_id]?.recentAlarmAt ?? '-',
    lastActionAt: alarmMetaByEquipment.value[row.equipment_id]?.lastActionAt ?? '-',
  }))
})

const donutStyle = computed(() => {
  const t = oeeProgress.value
  if (!activeOee.value) {
    return { background: `conic-gradient(#edf1f6 0 100%)` }
  }
  const { availability = 0, performance = 0, quality = 0, oee = 0 } = activeOee.value
  const total = (availability + performance + quality) || 1
  const oeeP = oee * 100
  const p1 = ((availability / total) * oeeP * t).toFixed(1)
  const p2 = (((availability + performance) / total) * oeeP * t).toFixed(1)
  const p3 = (oeeP * t).toFixed(1)
  return {
    background: `conic-gradient(#1375de 0 ${p1}%, #e8892c ${p1}% ${p2}%, #1599b6 ${p2}% ${p3}%, #edf1f6 ${p3}% 100%)`,
  }
})

const startOeeAnimation = () => {
  oeeProgress.value = 0
  const duration = 1000
  const start = performance.now()
  const tick = (now) => {
    const t = Math.min((now - start) / duration, 1)
    oeeProgress.value = 1 - Math.pow(1 - t, 3)
    if (t < 1) requestAnimationFrame(tick)
  }
  requestAnimationFrame(tick)
}

const fetchOeeData = async () => {
  try {
    const [allRes, linesRes] = await Promise.all([
      fetch(`${API_BASE}/api/dashboard/oee`, { headers: authHeaders() }),
      fetch(`${API_BASE}/api/dashboard/lines/oee`, { headers: authHeaders() }),
    ])
    const allBody = await allRes.json().catch(() => ({}))
    if (allRes.ok && allBody?.success && allBody?.data) {
      oeeAll.value = allBody.data
    }
    const linesBody = await linesRes.json().catch(() => ({}))
    if (linesRes.ok && linesBody?.success && Array.isArray(linesBody?.data)) {
      const map = {}
      for (const line of linesBody.data) {
        map[String(line.lineNo)] = line
      }
      oeeByLine.value = map
    }
  } catch {
    // 백엔드 미연결 시 유지
  }
}

const fetchLifeRows = async () => {
  try {
    const res = await fetch(`${API_BASE}/api/dashboard/equipment-status`, { headers: authHeaders() })
    const body = await res.json().catch(() => ({}))
    if (!res.ok || !body?.success || !Array.isArray(body?.data)) return
    const sorted = body.data
      .filter((e) => e.healthScore != null)
      .sort((a, b) => a.healthScore - b.healthScore)
      .slice(0, 5)
    lifeRows.value = sorted.map((e, idx) => {
      const life = Math.round(e.healthScore)
      const color = life < 30 ? 'red' : life < 60 ? 'orange' : 'green'
      return {
        rank: idx + 1,
        name: e.equipmentId ?? e.equipment_id ?? '',
        type: e.equipmentType ?? e.equipment_type ?? '',
        life,
        color,
      }
    })
  } catch {
    // 백엔드 미연결 시 유지
  }
}

const fetchAlarmMeta = async (equipmentId, force = false) => {
  if (!equipmentId) return
  if (alarmFetchInFlight.has(equipmentId)) return

  const now = Date.now()
  const lastFetch = alarmLastFetchedAt.get(equipmentId) ?? 0
  if (!force && now - lastFetch < ALARM_REFRESH_MS) return

  alarmFetchInFlight.add(equipmentId)
  try {
    const res = await fetch(`${API_BASE}/api/alarms/${encodeURIComponent(equipmentId)}`, {
      headers: authHeaders(),
    })
    const body = await res.json()
    if (!res.ok || !body?.success || !Array.isArray(body?.data)) return

    const rows = body.data
    if (rows.length === 0) {
      alarmMetaByEquipment.value = {
        ...alarmMetaByEquipment.value,
        [equipmentId]: { recentAlarmAt: '-', lastActionAt: '-' },
      }
      alarmLastFetchedAt.set(equipmentId, Date.now())
      return
    }

    const latestTimestampRow = rows.reduce((latest, item) => {
      return toMillis(item?.timestamp) > toMillis(latest?.timestamp) ? item : latest
    }, rows[0])

    const latestUpdatedRow = rows.reduce((latest, item) => {
      return toMillis(item?.updatedAt) > toMillis(latest?.updatedAt) ? item : latest
    }, rows[0])

    alarmMetaByEquipment.value = {
      ...alarmMetaByEquipment.value,
      [equipmentId]: {
        recentAlarmAt: formatDateTime(latestTimestampRow?.timestamp),
        lastActionAt: formatDateTime(latestUpdatedRow?.updatedAt),
      },
    }
    alarmLastFetchedAt.set(equipmentId, Date.now())
  } catch {
    // no-op
  } finally {
    alarmFetchInFlight.delete(equipmentId)
  }
}

const connectMqtt = () => {
  mqttClient = mqtt.connect(resolveBrokerUrlForBrowser(rawBrokerUrl), {
    clientId: `bs-scada-dashboard-${Math.random().toString(16).slice(2, 10)}`,
    username: mqttUsername,
    password: mqttPassword,
    reconnectPeriod: 3000,
    connectTimeout: 10000,
    clean: true,
  })

  mqttClient.on('connect', () => {
    mqttClient.subscribe(topicFilter, { qos: 0 })
  })

  mqttClient.on('message', (topic, payloadBuffer) => {
    const payload = parsePayload(payloadBuffer)
    if (!payload) return

    const equipmentId = String(payload.equipment_id ?? parseTopicEquipmentId(topic)).trim()
    if (!equipmentId) return

    const previousRow = equipmentRealtimeMap.value[equipmentId]
    const previousRuntimeSec = toNonNegativeNumber(previousRow?.accumulated_run_hours)
    const payloadRuntimeSec = toNonNegativeNumber(payload.accumulated_run_hours)
    const resolvedRuntimeSec = Math.max(previousRuntimeSec, payloadRuntimeSec)

    equipmentRealtimeMap.value = {
      ...equipmentRealtimeMap.value,
      [equipmentId]: {
        equipment_id: equipmentId,
        status: normalizeStatus(payload.status),
        accumulated_run_hours: resolvedRuntimeSec,
        timestamp: payload.timestamp ?? '',
        lastRuntimeTickMs: Date.now(),
      },
    }

    fetchAlarmMeta(equipmentId)
  })
}

const connectStomp = () => {
  stompClient = new Client({
    webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
    onConnect: () => {
      stompClient.subscribe('/topic/dashboard/summary', async () => {
        await Promise.all([fetchOeeData(), fetchLifeRows()])
        startOeeAnimation()
      })
    },
    onStompError: (frame) => console.error('STOMP 오류:', frame),
    reconnectDelay: 5000,
  })
  stompClient.activate()
}

const startRuntimeTicker = () => {
  if (runtimeTimer) clearInterval(runtimeTimer)

  runtimeTimer = setInterval(() => {
    const nowMs = Date.now()
    const current = equipmentRealtimeMap.value
    const entries = Object.entries(current)
    if (entries.length === 0) return

    let changed = false
    const nextMap = {}

    for (const [equipmentId, row] of entries) {
      const lastTickMs = Number(row.lastRuntimeTickMs) || nowMs
      const deltaSec = Math.floor((nowMs - lastTickMs) / 1000)

      if (deltaSec <= 0) {
        nextMap[equipmentId] = row
        continue
      }

      const shouldAccumulate = row.status === 'RUN'
      nextMap[equipmentId] = {
        ...row,
        accumulated_run_hours: toNonNegativeNumber(row.accumulated_run_hours) + (shouldAccumulate ? deltaSec : 0),
        lastRuntimeTickMs: lastTickMs + (deltaSec * 1000),
      }
      changed = true
    }

    if (changed) {
      equipmentRealtimeMap.value = nextMap
    }
  }, 1000)
}

onMounted(async () => {
  startOeeAnimation()
  await Promise.all([fetchOeeData(), fetchLifeRows()])
  isLifeLoading.value = false
  startOeeAnimation()
  connectMqtt()
  startRuntimeTicker()
  connectStomp()
})

onUnmounted(() => {
  if (mqttClient) {
    mqttClient.end(true)
    mqttClient = null
  }
  if (runtimeTimer) {
    clearInterval(runtimeTimer)
    runtimeTimer = null
  }
  stompClient?.deactivate()
  stompClient = null
})

watch(selectedLine, startOeeAnimation)
</script>

<template>
  <div class="dashboard-page">
    <AppTopbar active-menu="대시보드" />

    <main class="content">
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

      <section class="main-grid">
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
            <div class="donut" :style="donutStyle">
              <span><b>{{ fmtPct(activeOee?.oee) }}</b></span>
            </div>
            <div class="oee-info">
              <div class="oee-metric">
                <span class="metric-label"><i class="dot blue"></i>가동률</span>
                <strong class="metric-val">{{ fmtPct(activeOee?.availability) }}</strong>
              </div>
              <div class="oee-operator">×</div>
              <div class="oee-metric">
                <span class="metric-label"><i class="dot orange"></i>성능률</span>
                <strong class="metric-val">{{ fmtPct(activeOee?.performance) }}</strong>
              </div>
              <div class="oee-operator">×</div>
              <div class="oee-metric">
                <span class="metric-label"><i class="dot cyan"></i>품질률</span>
                <strong class="metric-val">{{ fmtPct(activeOee?.quality) }}</strong>
              </div>
              <div class="oee-result-row">
                <div class="oee-equals">=</div>
                <div class="oee-total">
                  <span>OEE (종합설비효율)</span>
                  <strong>{{ fmtPct(activeOee?.oee) }}</strong>
                </div>
              </div>
            </div>
          </div>
        </article>

        <article class="panel life-card">
          <div class="panel-header">
            <h2>설비별 잔존 수명 (하위 5)</h2>
            <button type="button" class="more-btn" @click="router.push('/life')">더보기 ›</button>
          </div>
          <table class="data-table life-table">
            <colgroup>
              <col style="width:52px" />
              <col style="width:90px" />
              <col style="width:90px" />
              <col />
            </colgroup>
            <thead>
              <tr><th>순위</th><th>설비명</th><th>설비 유형</th><th>잔존 수명</th></tr>
            </thead>
            <tbody>
              <template v-if="isLifeLoading">
                <tr v-for="n in 5" :key="n" class="skel-row">
                  <td><div class="skel skel-td-xs"></div></td>
                  <td><div class="skel skel-td-sm"></div></td>
                  <td><div class="skel skel-td-sm"></div></td>
                  <td><div class="skel skel-td-full"></div></td>
                </tr>
              </template>
              <template v-else>
                <tr v-if="lifeRows.length === 0">
                  <td colspan="4" class="empty-row">데이터가 없습니다.</td>
                </tr>
                <tr v-for="row in lifeRows" :key="row.rank">
                  <td>{{ row.rank }}</td>
                  <td>{{ row.name }}</td>
                  <td>{{ row.type }}</td>
                  <td class="life-cell"><b class="track"><i :class="row.color" :style="{ width: row.life + '%' }"></i></b><strong :class="row.color">{{ row.life }}%</strong></td>
                </tr>
              </template>
            </tbody>
          </table>
        </article>

        <article class="panel detail-card">
          <div class="panel-header">
            <h2>설비 상세 현황</h2>
            <span class="detail-count">전체 {{ detailRows.length }}건</span>
          </div>
          <div class="detail-table-wrap">
            <table class="data-table detail-table">
              <thead>
                <tr>
                  <th>설비명</th>
                  <th>설비상태</th>
                  <th>가동시간</th>
                  <th>최근알람</th>
                  <th>마지막 조치일</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="row in detailRows" :key="row.equipment_id">
                  <td>{{ row.equipment_id }}</td>
                  <td>
                    <span
                      class="status-text"
                      :class="{
                        'status-run': row.status === 'RUN',
                        'status-idle': row.status === 'IDLE',
                        'status-stop': row.status === 'STOP',
                        'status-alarm': row.status === 'ALARM',
                      }"
                    >
                      {{ row.status }}
                    </span>
                  </td>
                  <td>{{ row.accumulated_run_hours }} ({{ row.runtimeText }})</td>
                  <td>{{ row.recentAlarmAt }}</td>
                  <td>{{ row.lastActionAt }}</td>
                </tr>
                <tr v-if="detailRows.length === 0">
                  <td colspan="5" class="empty-row">MQTT 데이터 수신 대기 중입니다.</td>
                </tr>
              </tbody>
            </table>
          </div>
        </article>
      </section>
    </main>
  </div>
</template>

<style scoped>
.dashboard-page { min-width: 1180px; min-height: 100vh; background: #f5f7fb; }
.content { padding: 22px 32px 30px; }

.summary-grid { display: grid; grid-template-columns: repeat(5, 1fr); gap: 18px; margin-bottom: 16px; }
.summary-card { height: 142px; display: flex; align-items: center; gap: 22px; padding: 26px 32px; background: #fff; border-radius: 14px; box-shadow: 0 5px 16px rgba(13, 36, 72, .12); }
.summary-icon { width: 88px; height: 88px; display: grid; place-items: center; border-radius: 50%; font-size: 40px; }
.summary-icon.blue { background: #e4f0ff; color: #1474df; }
.summary-icon.sky { background: #dbeafe; color: #2563eb; }
.summary-icon.green { background: #e1f6ef; color: #12a985; }
.summary-icon.orange { background: #fff0df; color: #df7922; }
.summary-icon.red { background: #ffe7eb; color: #fa2c45; }
.summary-card p { margin: 0 0 4px; font-size: 16px; font-weight: 900; }
.summary-card strong { font-size: 46px; line-height: 1; font-weight: 950; }
.summary-card span { margin-left: 8px; font-weight: 800; }

.main-grid {
  display: grid;
  grid-template-columns: 0.75fr 1.25fr;
  grid-template-rows: 340px 340px;
  gap: 16px;
}

.panel { background: #fff; border-radius: 14px; box-shadow: 0 5px 16px rgba(13, 36, 72, .11); padding: 16px 18px; overflow: hidden; }
.panel-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
h2 { margin: 0; font-size: 20px; font-weight: 950; letter-spacing: -.02em; }

.oee-card  { grid-column: 1; grid-row: 1; }
.life-card { grid-column: 1; grid-row: 2; }
.detail-card { grid-column: 2; grid-row: 1 / 3; display: flex; flex-direction: column; }

.line-select { width: 116px; height: 36px; border: 1px solid #d8e1ed; border-radius: 6px; padding: 0 10px; color: #0d2448; font-weight: 800; background: #fff; }
.oee-body { height: calc(100% - 48px); display: flex; align-items: center; justify-content: center; gap: 24px; padding: 0 8px; }
.donut { width: 220px; height: 220px; flex-shrink: 0; border-radius: 50%; position: relative; display: grid; place-items: center; }
.donut::after { content: ''; position: absolute; width: 130px; height: 130px; background: #fff; border-radius: 50%; }
.donut span { position: relative; z-index: 1; text-align: center; font-weight: 900; }
.donut b { font-size: 28px; }

.oee-info { display: flex; flex-direction: column; align-items: center; width: 190px; flex-shrink: 0; }
.oee-metric { display: flex; align-items: center; justify-content: space-between; width: 100%; padding: 7px 12px; border-radius: 8px; background: #f7faff; font-size: 14px; font-weight: 850; }
.metric-label { display: flex; align-items: center; color: #4a5f7a; }
.metric-val { color: #126de0; font-weight: 950; font-size: 15px; }
.oee-operator { font-size: 16px; font-weight: 900; color: #9aa8bc; line-height: 1; padding: 3px 0; }
.oee-result-row { display: flex; align-items: center; gap: 6px; width: 100%; padding-top: 4px; }
.oee-equals { font-size: 20px; font-weight: 900; color: #126de0; flex-shrink: 0; }
.oee-total { display: flex; flex-direction: column; align-items: center; justify-content: center; flex: 1; padding: 8px 12px; border-radius: 8px; background: linear-gradient(135deg, #e8f0fe, #f0f7ff); border: 1px solid #b8d4f5; text-align: center; gap: 2px; }
.oee-total span { font-size: 12px; font-weight: 850; color: #4a5f7a; }
.oee-total strong { font-size: 20px; font-weight: 950; color: #126de0; }

.dot { display: inline-block; border-radius: 50%; margin-right: 8px; width: 12px; height: 12px; flex-shrink: 0; }
.blue { background: #1375de; color: #1375de; }
.cyan { background: #1599b6; color: #1599b6; }
.orange { background: #ff6b16; color: #ff6b16; }
.yellow { background: #ff9f1a; color: #ff9f1a; }
.green { background: #12b58f; color: #12b58f; }
.red { background: #ff3045; color: #ff3045; }

.data-table { width: 100%; border-collapse: separate; border-spacing: 0; table-layout: fixed; border: 1px solid #e2e7ee; border-radius: 10px; overflow: hidden; font-size: 14px; }
.data-table th, .data-table td { height: 34px; padding: 6px 10px; text-align: center; font-weight: 850; border-bottom: 1px solid #edf1f6; white-space: nowrap; }
.data-table th { background: #fafafa; color: #142b50; font-size: 13px; font-weight: 950; }
.data-table tr:last-child td { border-bottom: 0; }

.detail-table-wrap { flex: 1; min-height: 0; overflow-y: auto; border-radius: 10px; }
.detail-table-wrap .data-table { border-radius: 0; border: none; }
.detail-table-wrap::-webkit-scrollbar { width: 6px; }
.detail-table-wrap::-webkit-scrollbar-thumb { background: #d0d9e8; border-radius: 3px; }
.detail-table-wrap::-webkit-scrollbar-track { background: transparent; }
.detail-count { font-size: 13px; font-weight: 800; color: #4a5f7a; }
.detail-table { font-size: 15px; }
.detail-table th, .detail-table td { height: 34px; padding: 6px 12px; }
.empty-row { color: #6b8098; font-weight: 900; }
.status-text { font-weight: 900; }
.status-run { color: #16a34a; }
.status-idle { color: #2563eb; }
.status-stop { color: #f97316; }
.status-alarm { color: #ef4444; }

.life-card { display: flex; flex-direction: column; }
.life-table th, .life-table td { height: 42px; }
.life-cell { display: flex; align-items: center; gap: 10px; padding: 0 8px; }
.track { flex: 1; height: 8px; background: #e8e9eb; border-radius: 99px; overflow: hidden; display: block; min-width: 0; }
.track i { display: block; height: 100%; border-radius: 99px; }
.life-cell strong { width: 40px; flex-shrink: 0; text-align: right; background: transparent !important; font-weight: 950; }
.life-table td.red, .life-table td.orange, .life-table td.yellow, .life-table td.green { background: transparent; font-weight: 950; }
.more-btn { border: 0; background: transparent; color: #0d2448; font-weight: 950; cursor: pointer; }

@media (max-width: 1280px) { .dashboard-page { min-width: 1200px; } }

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}
.skel {
  background: linear-gradient(90deg, #e8edf4 25%, #f0f4f9 50%, #e8edf4 75%);
  background-size: 200% 100%;
  animation: shimmer 1.4s infinite linear;
  border-radius: 4px;
  display: block;
}
.skel-row td { vertical-align: middle; }
.skel-td-xs { height: 14px; width: 24px; margin: 0 auto; }
.skel-td-sm { height: 14px; width: 70%; margin: 0 auto; }
.skel-td-full { height: 14px; width: 90%; margin: 0 auto; }
</style>
