<script setup>
import { computed, ref, watch, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import mqtt from 'mqtt'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import AppTopbar from '@/components/AppTopbar.vue'
import { getEquipmentStatus } from '@/api/dashboard'

const defaultBrokerUrl = window.location.protocol === 'https:'
  ? 'wss://broker.mqttdashboard.com:8884/mqtt'
  : 'ws://broker.mqttdashboard.com:8000/mqtt'
const brokerUrl = import.meta.env.VITE_MQTT_BROKER_URL ?? defaultBrokerUrl
const topicFilter = import.meta.env.VITE_MQTT_TOPIC ?? 'factory/equipment/+/realtime'
const mqttUsername = import.meta.env.VITE_MQTT_USERNAME
const mqttPassword = import.meta.env.VITE_MQTT_PASSWORD

const router = useRouter()
const searchQuery = ref('')
const selectedLine = ref('전체 라인')
const sortOrder = ref('잔존 수명 낮은 순')
const viewMode = ref('card')
const rowsPerPage = ref(10)
const currentPage = ref(1)
const selectedId = ref('PLF-001')
const isLoading = ref(true)

const equipmentDefs = [
  { code: 'PLF', type: '패널 투입 장치', icon: '🏗️', manufacturer: '현대자동화' },
  { code: 'JIG', type: '차체 지그', icon: '📐', manufacturer: 'Daewon Precision' },
  { code: 'ROB', type: '산업용 로봇', icon: '🤖', manufacturer: 'ABB' },
  { code: 'WLD', type: '점 용접기', icon: '🔥', manufacturer: 'KUKA' },
  { code: 'SLR', type: '실러 도포 장비', icon: '🖌️', manufacturer: 'Nordson' },
  { code: 'VSI', type: '비전 검사기', icon: '👁️', manufacturer: 'Cognex' },
  { code: 'CNV', type: '컨베이어', icon: '→', manufacturer: 'Siemens' },
]

const zones = ['Zone A', 'Zone B', 'Zone C']

const STATUS_MAP = { RUN: 'running', STOP: 'stop', IDLE: 'idle', ALARM: 'alarm' }
const STATUS_LABEL_MAP = { RUN: '가동', STOP: '정지', IDLE: '대기', ALARM: '알람' }

const parseTopicEquipmentId = (topic) => {
  const match = /^factory\/equipment\/([^/]+)\/realtime$/i.exec(topic ?? '')
  return match?.[1] ?? ''
}

const buildEquipmentList = () => {
  const list = []
  let lifeSeed = 82

  equipmentDefs.forEach((def, typeIdx) => {
    for (let unit = 1; unit <= 3; unit += 1) {
      const zone = zones[typeIdx % zones.length]
      const lineNo = ((typeIdx + unit) % 3) + 1
      const line = `${zone} - Line ${lineNo}`
      const life = Math.max(12, (lifeSeed + typeIdx * 7 - unit * 11) % 91)
      lifeSeed -= 3
      const id = `${def.code}-${String(unit).padStart(3, '0')}`

      list.push({
        id,
        name: id,
        type: def.type,
        status: 'unknown',
        statusLabel: '-',
        life,
        line,
        icon: def.icon,
        manufacturer: def.manufacturer,
        equipId: id,
        location: line,
        lastUpdate: '-',
        runtime: '-',
        temp: '-',
        current: '-',
        cycleTime: '-',
        production: '-',
      })
    }
  })

  return list
}

const equipmentList = ref(buildEquipmentList())

const recentAlarms = [
  { title: 'PLF-001 투입 센서 이상', time: '2024-05-24 10:15', level: 'warning', label: '경고' },
  { title: 'WLD-002 용접 전류 과다', time: '2024-05-24 09:32', level: 'warning', label: '경고' },
  { title: 'ROB-003 축 구동 오류', time: '2024-05-24 08:21', level: 'danger', label: '위험' },
  { title: 'VSI-001 비전 통신 지연', time: '2024-05-24 07:58', level: 'warning', label: '경고' },
]

const refreshEquipments = async () => {
  try {
    const data = await getEquipmentStatus()
    if (!Array.isArray(data)) return

    for (const remote of data) {
      const remoteId = remote.equipmentId ?? remote.equipment_id
      const idx = equipmentList.value.findIndex((item) => item.id === remoteId)
      if (idx === -1) continue

      const item = equipmentList.value[idx]
      equipmentList.value[idx] = {
        ...item,
        life: remote.healthScore != null ? Math.round(remote.healthScore) : item.life,
        lastUpdate: remote.lastUpdate ?? remote.updatedAt ?? item.lastUpdate,
      }
    }
  } catch {
    // 백엔드 미연결 시 기존 목업 유지
  }
}

let stompClient = null
let mqttClient = null

const connectMqtt = () => {
  mqttClient = mqtt.connect(brokerUrl, {
    clientId: `bs-scada-life-${Math.random().toString(16).slice(2, 10)}`,
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
    try {
      const payload = JSON.parse(payloadBuffer.toString())
      if (!payload || typeof payload !== 'object') return

      const equipmentId = String(payload.equipment_id ?? parseTopicEquipmentId(topic)).trim()
      if (!equipmentId) return

      const rawStatus = String(payload.status ?? '').toUpperCase()
      const status = STATUS_MAP[rawStatus]
      if (!status) return

      const statusLabel = STATUS_LABEL_MAP[rawStatus]

      const idx = equipmentList.value.findIndex((item) => item.id === equipmentId)
      if (idx !== -1) {
        equipmentList.value[idx] = { ...equipmentList.value[idx], status, statusLabel }
      }
    } catch {
      // ignore parse errors
    }
  })
}

onMounted(async () => {
  await refreshEquipments()
  isLoading.value = false
  connectMqtt()

  stompClient = new Client({
    webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
    onConnect: () => {
      stompClient.subscribe('/topic/dashboard/summary', () => {
        refreshEquipments()
      })
    },
    onStompError: (frame) => {
      console.error('STOMP 오류:', frame)
    },
    reconnectDelay: 5000,
  })
  stompClient.activate()
})

onUnmounted(() => {
  mqttClient?.end(true)
  mqttClient = null
  stompClient?.deactivate()
  stompClient = null
})

const matchesSearch = (item, query) => {
  if (!query) return true
  const haystack = [item.name, item.type, item.equipId, item.line, item.location, item.id]
    .join(' ')
    .toLowerCase()
  return haystack.includes(query)
}

const matchesLine = (item, lineFilter) => {
  if (lineFilter === '전체 라인') return true
  return item.line.includes(lineFilter)
}

const sortEquipment = (items) => {
  const sorted = [...items]
  if (sortOrder.value === '잔존 수명 높은 순') {
    return sorted.sort((a, b) => b.life - a.life)
  }
  return sorted.sort((a, b) => a.life - b.life)
}

const filteredEquipment = computed(() => {
  const query = searchQuery.value.trim().toLowerCase()
  const filtered = equipmentList.value.filter(
    (item) => matchesSearch(item, query) && matchesLine(item, selectedLine.value),
  )
  return sortEquipment(filtered)
})

const selectedEquipment = computed(
  () => equipmentList.value.find((e) => e.id === selectedId.value) ?? equipmentList.value[0],
)

const totalPages = computed(() =>
  Math.max(1, Math.ceil(filteredEquipment.value.length / rowsPerPage.value)),
)

const pagedTableRows = computed(() => {
  const start = (currentPage.value - 1) * rowsPerPage.value
  return filteredEquipment.value.slice(start, start + rowsPerPage.value)
})

watch([searchQuery, selectedLine, sortOrder, rowsPerPage], () => {
  currentPage.value = 1
})

watch(filteredEquipment, (list) => {
  if (!list.some((item) => item.id === selectedId.value) && list.length > 0) {
    selectedId.value = list[0].id
  }
})

const selectEquipment = (id) => {
  selectedId.value = id
}

const setPage = (page) => {
  currentPage.value = Math.min(Math.max(page, 1), totalPages.value)
}

const goToAlarmPage = () => {
  router.push('/equipment-alarm')
}

const lifeColor = (life) => {
  if (life >= 60) return 'green'
  if (life >= 40) return 'yellow'
  return 'red'
}

</script>

<template>
  <div class="life-page">
    <AppTopbar active-menu="수명 관리" />

    <main class="content">
      <div class="main-layout">
        <div class="left-column">
          <section class="panel life-overview-panel">
            <div class="panel-header">
              <h2>설비 잔존 수명 현황</h2>
            </div>

            <div class="toolbar-row">
              <div class="view-toggle" aria-label="보기 방식 선택">
                <button
                  type="button"
                  :class="{ active: viewMode === 'card' }"
                  @click="viewMode = 'card'"
                >
                  카드형
                </button>
                <button
                  type="button"
                  :class="{ active: viewMode === 'list' }"
                  @click="viewMode = 'list'"
                >
                  목록형
                </button>
              </div>
              <select v-model="selectedLine" class="filter-select">
                <option>전체 라인</option>
                <option>Zone A</option>
                <option>Zone B</option>
                <option>Zone C</option>
              </select>
              <select v-model="sortOrder" class="filter-select">
                <option>잔존 수명 낮은 순</option>
                <option>잔존 수명 높은 순</option>
              </select>
              <label class="search-box">
                <span class="search-icon">⌕</span>
                <input
                  v-model="searchQuery"
                  type="search"
                  class="search-input"
                  placeholder="설비명, 유형, ID 검색 (예: PLF-001)"
                />
              </label>
            </div>

            <div class="result-row">
              <span class="result-count">{{ filteredEquipment.length }}대</span>
            </div>

            <div v-if="viewMode === 'card'" class="equipment-scroll">
              <div v-if="isLoading" class="equipment-grid">
                <div v-for="n in 8" :key="n" class="equip-card skeleton-card">
                  <div class="card-top">
                    <div class="skel skel-icon"></div>
                    <div class="card-title">
                      <div class="skel skel-name"></div>
                      <div class="skel skel-type"></div>
                    </div>
                    <div class="skel skel-badge"></div>
                  </div>
                  <div class="card-life">
                    <div class="skel skel-life-label"></div>
                    <div class="skel skel-life-value"></div>
                    <div class="skel skel-bar"></div>
                  </div>
                </div>
              </div>
              <template v-else>
                <div v-if="filteredEquipment.length === 0" class="empty-state">
                  검색 결과가 없습니다.
                </div>
                <div v-else class="equipment-grid">
                  <article
                    v-for="item in filteredEquipment"
                    :key="item.id"
                    class="equip-card"
                    :class="{ selected: selectedId === item.id }"
                    @click="selectEquipment(item.id)"
                  >
                    <div class="card-top">
                      <span class="card-icon">{{ item.icon }}</span>
                      <div class="card-title">
                        <strong>{{ item.name }}</strong>
                        <span>{{ item.type }}</span>
                      </div>
                      <span class="status-badge" :class="item.status">{{ item.statusLabel }}</span>
                    </div>
                    <div class="card-life">
                      <span class="life-label">잔존 수명</span>
                      <strong class="life-value" :class="lifeColor(item.life)">{{ item.life }}%</strong>
                      <div class="battery-bar">
                        <i :class="lifeColor(item.life)" :style="{ width: item.life + '%' }"></i>
                      </div>
                    </div>
                  </article>
                </div>
              </template>
            </div>

            <div v-else class="list-view">
              <div class="table-wrap">
                <table class="data-table life-list-table">
                  <thead>
                    <tr>
                      <th>설비명</th>
                      <th>라인</th>
                      <th>설비 유형</th>
                      <th>상태</th>
                      <th>잔존 수명</th>
                    </tr>
                  </thead>
                  <tbody>
                    <template v-if="isLoading">
                      <tr v-for="n in rowsPerPage" :key="n">
                        <td><div class="skel skel-td"></div></td>
                        <td><div class="skel skel-td"></div></td>
                        <td><div class="skel skel-td"></div></td>
                        <td><div class="skel skel-td-sm"></div></td>
                        <td><div class="skel skel-td"></div></td>
                      </tr>
                    </template>
                    <template v-else>
                      <tr v-if="filteredEquipment.length === 0">
                        <td colspan="6" class="empty-row">검색 결과가 없습니다.</td>
                      </tr>
                      <tr
                        v-for="row in pagedTableRows"
                        :key="row.id"
                        :class="{ selected: selectedId === row.id }"
                        @click="selectEquipment(row.id)"
                      >
                        <td class="name-cell">{{ row.name }}</td>
                        <td>{{ row.line }}</td>
                        <td>{{ row.type }}</td>
                        <td>
                          <i class="status-dot" :class="row.status"></i>
                          {{ row.statusLabel }}
                        </td>
                        <td class="life-cell">
                          <strong :class="lifeColor(row.life)">{{ row.life }}%</strong>
                          <b class="track"><i :class="lifeColor(row.life)" :style="{ width: row.life + '%' }"></i></b>
                        </td>
                      </tr>
                    </template>
                  </tbody>
                </table>
              </div>
              <div class="table-footer">
                <div class="rows-control">
                  <span>행 표시</span>
                  <select v-model.number="rowsPerPage" class="filter-select small">
                    <option :value="7">7</option>
                    <option :value="10">10</option>
                    <option :value="15">15</option>
                  </select>
                </div>
                <div class="pagination">
                  <button type="button" @click="setPage(currentPage - 1)">‹</button>
                  <button
                    v-for="page in totalPages"
                    :key="page"
                    type="button"
                    :class="{ active: page === currentPage }"
                    @click="setPage(page)"
                  >
                    {{ page }}
                  </button>
                  <button type="button" @click="setPage(currentPage + 1)">›</button>
                </div>
                <div class="page-size">
                  <select class="filter-select small">
                    <option>10 / 페이지</option>
                    <option>20 / 페이지</option>
                  </select>
                </div>
              </div>
            </div>
          </section>
        </div>

        <aside class="right-column">
          <section class="panel detail-panel">
            <div class="panel-header">
              <h2>설비 상세 정보</h2>
              <button type="button" class="more-btn">더보기 ›</button>
            </div>

            <div class="detail-header">
              <span class="detail-icon">{{ selectedEquipment.icon }}</span>
              <div>
                <strong>{{ selectedEquipment.name }}</strong>
                <span class="status-badge" :class="selectedEquipment.status">
                  {{ selectedEquipment.statusLabel }}
                </span>
              </div>
            </div>

            <dl class="info-list">
              <div><dt>제조사</dt><dd>{{ selectedEquipment.manufacturer }}</dd></div>
              <div><dt>설비 ID</dt><dd>{{ selectedEquipment.equipId }}</dd></div>
              <div><dt>설치 위치</dt><dd>{{ selectedEquipment.location }}</dd></div>
              <div><dt>설비 유형</dt><dd>{{ selectedEquipment.type }}</dd></div>
              <div><dt>마지막 업데이트</dt><dd>{{ selectedEquipment.lastUpdate }}</dd></div>
            </dl>

            <h3 class="sub-title">주요 데이터</h3>
            <div class="metrics-grid">
              <div class="metric-box">
                <span>가동 상태</span>
                <strong class="running-text">{{ selectedEquipment.statusLabel === '가동' ? 'Running' : selectedEquipment.statusLabel }}</strong>
              </div>
              <div class="metric-box">
                <span>가동 시간</span>
                <strong>{{ selectedEquipment.runtime }}</strong>
              </div>
              <div class="metric-box">
                <span>현재 온도</span>
                <strong>{{ selectedEquipment.temp }}</strong>
              </div>
              <div class="metric-box">
                <span>전류</span>
                <strong>{{ selectedEquipment.current }}</strong>
              </div>
              <div class="metric-box">
                <span>사이클 타임</span>
                <strong>{{ selectedEquipment.cycleTime }}</strong>
              </div>
              <div class="metric-box">
                <span>생산 수량</span>
                <strong>{{ selectedEquipment.production }}</strong>
              </div>
            </div>
          </section>

          <section class="panel alarm-panel">
            <div class="panel-header">
              <h2>최근 알람</h2>
              <button type="button" class="more-btn" @click="goToAlarmPage">더보기 ›</button>
            </div>
            <ul class="alarm-list">
              <li v-for="(alarm, idx) in recentAlarms" :key="idx" :class="alarm.level">
                <span class="alarm-icon">{{ alarm.level === 'danger' ? '⚠' : '△' }}</span>
                <div class="alarm-body">
                  <strong>{{ alarm.title }}</strong>
                  <span>{{ alarm.time }}</span>
                </div>
                <em :class="alarm.level">{{ alarm.label }}</em>
              </li>
            </ul>
          </section>
        </aside>
      </div>
    </main>
  </div>
</template>

<style scoped>
.life-page {
  min-width: 1180px;
  min-height: 100vh;
  background: #f5f7fb;
}

.content {
  padding: 22px 32px 30px;
}

.main-layout {
  display: grid;
  grid-template-columns: 1fr 442px;
  gap: 16px;
  align-items: stretch;
}

.left-column {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-width: 0;
  min-height: 0;
}

.right-column {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.life-overview-panel {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
}

.panel {
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 5px 16px rgba(13, 36, 72, 0.11);
  padding: 16px 18px;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 950;
  letter-spacing: -0.02em;
  color: #0d2448;
}

.view-toggle {
  display: inline-flex;
  flex: none;
  padding: 3px;
  border: 1px solid #d8e1ed;
  border-radius: 6px;
  background: #f7f9fc;
}

.view-toggle button {
  min-width: 70px;
  height: 30px;
  border: 0;
  border-radius: 4px;
  background: transparent;
  color: #4a5f7a;
  font-size: 13px;
  font-weight: 900;
  cursor: pointer;
}

.view-toggle button.active {
  background: #fff;
  color: #126de0;
  box-shadow: 0 2px 8px rgba(13, 36, 72, 0.1);
}

.toolbar-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.search-box {
  flex: 1 1 280px;
  min-width: 260px;
  display: flex;
  align-items: center;
  gap: 10px;
  height: 36px;
  padding: 0 14px;
  background: #fff;
  border: 1px solid #d8e1ed;
  border-radius: 6px;
  box-shadow: 0 2px 8px rgba(13, 36, 72, 0.06);
}

.search-icon {
  color: #6b7c94;
  font-size: 16px;
  font-weight: 900;
}

.search-input {
  flex: 1;
  border: 0;
  outline: none;
  font-size: 13px;
  font-weight: 700;
  color: #0d2448;
  background: transparent;
}

.search-input::placeholder {
  color: #9aa8bc;
}

.result-count {
  font-size: 13px;
  font-weight: 850;
  color: #6b7c94;
}

.result-row {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 10px;
}

.life-overview-panel .equipment-scroll {
  flex: 1;
  min-height: 332px;
  overflow-y: auto;
  padding-right: 4px;
  scrollbar-width: thin;
  scrollbar-color: #c5d0e0 transparent;
}

.life-overview-panel .equipment-scroll::-webkit-scrollbar {
  width: 6px;
}

.life-overview-panel .equipment-scroll::-webkit-scrollbar-thumb {
  background: #c5d0e0;
  border-radius: 99px;
}

.empty-state,
.empty-row {
  text-align: center;
  color: #6b7c94;
  font-weight: 800;
  font-size: 14px;
}

.empty-state {
  padding: 48px 16px;
}

.filter-select {
  flex: 0 0 156px;
  height: 36px;
  border: 1px solid #d8e1ed;
  border-radius: 6px;
  padding: 0 10px;
  color: #0d2448;
  font-weight: 800;
  font-size: 13px;
  background: #fff;
}

.filter-select.small {
  flex: 0 0 auto;
  height: 32px;
  font-size: 12px;
}

.list-view {
  display: flex;
  flex: 1;
  min-height: 0;
  flex-direction: column;
}

.equipment-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.equip-card {
  border: 2px solid #e8edf4;
  border-radius: 12px;
  padding: 14px 14px 12px;
  cursor: pointer;
  transition: border-color 0.15s, box-shadow 0.15s;
  background: #fff;
}

.equip-card:hover {
  border-color: #b8d4f5;
}

.equip-card.selected {
  border-color: #1890ff;
  box-shadow: 0 0 0 1px rgba(24, 144, 255, 0.25);
}

.card-top {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 12px;
}

.card-icon {
  font-size: 22px;
  line-height: 1;
  flex-shrink: 0;
}

.card-title {
  flex: 1;
  min-width: 0;
}

.card-title strong {
  display: block;
  font-size: 14px;
  font-weight: 950;
  color: #0d2448;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.card-title span {
  font-size: 12px;
  font-weight: 700;
  color: #6b7c94;
}

.status-badge {
  flex-shrink: 0;
  padding: 3px 10px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 900;
}

.status-badge.unknown {
  background: #f1f3f7;
  color: #9aa8bc;
}

.status-badge.running {
  background: #e1f6ef;
  color: #12a985;
}

.status-badge.idle {
  background: #dbeafe;
  color: #2563eb;
}

.status-badge.stop {
  background: #fff0df;
  color: #f97316;
}

.status-badge.alarm {
  background: #ffe7eb;
  color: #ef4444;
}

.card-life {
  margin-bottom: 10px;
}

.life-label {
  font-size: 12px;
  font-weight: 800;
  color: #6b7c94;
}

.life-value {
  display: block;
  font-size: 28px;
  font-weight: 950;
  line-height: 1.2;
  margin: 2px 0 8px;
}

.life-value.green { color: #12b58f; }
.life-value.yellow { color: #ff9f1a; }
.life-value.red { color: #ff3045; }

.battery-bar {
  height: 10px;
  background: #e8e9eb;
  border-radius: 4px;
  overflow: hidden;
  border: 1px solid #d5dbe5;
}

.battery-bar i {
  display: block;
  height: 100%;
  border-radius: 3px;
  transition: width 0.2s;
}

.battery-bar i.green { background: linear-gradient(90deg, #12b58f, #1dd4a8); }
.battery-bar i.yellow { background: linear-gradient(90deg, #ff9f1a, #ffb84d); }
.battery-bar i.red { background: linear-gradient(90deg, #ff3045, #ff6b7a); }

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  font-weight: 800;
  color: #6b7c94;
  padding-top: 8px;
  border-top: 1px solid #edf1f6;
}

.card-footer strong {
  color: #0d2448;
  font-weight: 900;
}

.card-footer strong.urgent {
  color: #ff3045;
}

.table-wrap {
  overflow-x: auto;
}

.data-table {
  width: 100%;
  border-collapse: separate;
  border-spacing: 0;
  table-layout: fixed;
  border: 1px solid #e2e7ee;
  border-radius: 10px;
  overflow: hidden;
  font-size: 13px;
}

.data-table th,
.data-table td {
  height: 36px;
  padding: 6px 10px;
  text-align: center;
  font-weight: 850;
  border-bottom: 1px solid #edf1f6;
  white-space: nowrap;
}

.data-table th {
  background: #fafafa;
  color: #142b50;
  font-size: 12px;
  font-weight: 950;
}

.data-table tbody tr {
  cursor: pointer;
  transition: background 0.12s;
}

.data-table tbody tr:hover {
  background: #f7faff;
}

.data-table tbody tr.selected {
  background: #eef6ff;
}

.data-table tr:last-child td {
  border-bottom: 0;
}

.name-cell {
  text-align: left;
  font-weight: 950;
  color: #0d2448;
}

.status-dot {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  margin-right: 6px;
  vertical-align: middle;
}

.status-dot.unknown { background: #c5d0e0; }
.status-dot.running { background: #14b993; }
.status-dot.idle { background: #2563eb; }
.status-dot.stop { background: #f97316; }
.status-dot.alarm { background: #ef4444; }

.life-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.life-cell strong {
  min-width: 38px;
  text-align: right;
  font-weight: 950;
}

.life-cell strong.green { color: #12b58f; }
.life-cell strong.yellow { color: #ff9f1a; }
.life-cell strong.red { color: #ff3045; }

.track {
  flex: 1;
  max-width: 80px;
  height: 8px;
  background: #e8e9eb;
  border-radius: 99px;
  overflow: hidden;
}

.track i {
  display: block;
  height: 100%;
  border-radius: 99px;
}

.track i.green { background: #12b58f; }
.track i.yellow { background: #ff9f1a; }
.track i.red { background: #ff3045; }

.data-table td.urgent {
  color: #ff3045;
  font-weight: 950;
}

.table-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 14px;
  padding-top: 4px;
}

.rows-control,
.page-size {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 800;
  color: #4a5f7a;
}

.pagination {
  display: flex;
  gap: 8px;
  align-items: center;
}

.pagination button {
  min-width: 36px;
  height: 34px;
  border: 1px solid #dfe5ee;
  background: #fff;
  border-radius: 6px;
  font-weight: 900;
  color: #122a4c;
  cursor: pointer;
}

.pagination button.active {
  background: linear-gradient(180deg, #0fc6d2, #0896b7);
  color: #fff;
  border-color: transparent;
}

.more-btn {
  border: 0;
  background: transparent;
  color: #0d2448;
  font-weight: 950;
  font-size: 13px;
  cursor: pointer;
}

.detail-panel {
  padding-bottom: 18px;
}

.detail-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  padding-bottom: 14px;
  border-bottom: 1px solid #edf1f6;
}

.detail-icon {
  font-size: 32px;
}

.detail-header strong {
  display: block;
  font-size: 18px;
  font-weight: 950;
  color: #0d2448;
  margin-bottom: 6px;
}

.info-list {
  margin: 0 0 16px;
  padding: 0;
}

.info-list > div {
  display: flex;
  justify-content: space-between;
  padding: 7px 0;
  font-size: 13px;
  border-bottom: 1px solid #f3f6fa;
}

.info-list dt {
  margin: 0;
  font-weight: 800;
  color: #6b7c94;
}

.info-list dd {
  margin: 0;
  font-weight: 900;
  color: #0d2448;
}

.sub-title {
  margin: 0 0 10px;
  font-size: 15px;
  font-weight: 950;
  color: #0d2448;
}

.metrics-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}

.metric-box {
  background: #f7f9fc;
  border: 1px solid #e8edf4;
  border-radius: 10px;
  padding: 12px 14px;
}

.metric-box span {
  display: block;
  font-size: 12px;
  font-weight: 800;
  color: #6b7c94;
  margin-bottom: 6px;
}

.metric-box strong {
  font-size: 15px;
  font-weight: 950;
  color: #0d2448;
}

.running-text {
  color: #12b58f !important;
}

.alarm-panel {
  display: flex;
  flex-direction: column;
}

.alarm-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.alarm-list li {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px 0;
  border-bottom: 1px solid #edf1f6;
}

.alarm-list li:last-child {
  border-bottom: 0;
}

.alarm-icon {
  font-size: 16px;
  line-height: 1.4;
}

.alarm-list li.warning .alarm-icon {
  color: #faad14;
}

.alarm-list li.danger .alarm-icon {
  color: #ff3045;
}

.alarm-body {
  flex: 1;
  min-width: 0;
}

.alarm-body strong {
  display: block;
  font-size: 14px;
  font-weight: 950;
  color: #0d2448;
  margin-bottom: 4px;
}

.alarm-body span {
  font-size: 12px;
  font-weight: 700;
  color: #6b7c94;
}

.alarm-list em {
  font-style: normal;
  font-size: 12px;
  font-weight: 900;
  padding: 3px 10px;
  border-radius: 6px;
  flex-shrink: 0;
}

.alarm-list em.warning {
  background: #fff7e6;
  color: #d48806;
}

.alarm-list em.danger {
  background: #fff1f0;
  color: #cf1322;
}

.green { color: #12b58f; }
.yellow { color: #ff9f1a; }
.red { color: #ff3045; }

@media (max-width: 1400px) {
  .main-layout {
    grid-template-columns: 1fr 442px;
  }

  .equipment-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 1280px) {
  .life-page {
    min-width: 1100px;
  }
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

.skel {
  background: linear-gradient(90deg, #e8edf4 25%, #f0f4f9 50%, #e8edf4 75%);
  background-size: 200% 100%;
  animation: shimmer 1.4s infinite linear;
  border-radius: 4px;
}

.skeleton-card {
  pointer-events: none;
}

.skel-icon {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  flex-shrink: 0;
}

.skel-name {
  height: 14px;
  width: 70%;
  margin-bottom: 6px;
}

.skel-type {
  height: 11px;
  width: 50%;
}

.skel-badge {
  width: 44px;
  height: 22px;
  border-radius: 6px;
  flex-shrink: 0;
}

.skel-life-label {
  height: 11px;
  width: 48px;
  margin-bottom: 6px;
}

.skel-life-value {
  height: 32px;
  width: 64px;
  margin-bottom: 10px;
}

.skel-bar {
  height: 10px;
  width: 100%;
}

.skel-footer-item {
  height: 12px;
  width: 45%;
}

.skel-td {
  height: 14px;
  width: 80%;
  margin: 0 auto;
}

.skel-td-sm {
  height: 14px;
  width: 48px;
  margin: 0 auto;
}
</style>
