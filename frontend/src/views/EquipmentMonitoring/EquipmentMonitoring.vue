<template>
  <div class="equipment-page">
    <AppTopbar active-menu="설비 현황" />

    <main class="page-body">
      <nav class="view-tabs" aria-label="설비 현황 보기 전환">
        <button
          type="button"
          :class="{ active: activeView === 'layout' }"
          @click="setActiveView('layout')"
        >
          라인 레이아웃
        </button>
        <button
          type="button"
          :class="{ active: activeView === 'list' }"
          @click="setActiveView('list')"
        >
          설비 목록
        </button>
      </nav>

      <section class="left-column">
        <section v-if="activeView === 'layout'" class="content-panel line-layout-section">
          <div class="panel-header">
            <div class="title-wrap">
              <h2>차체 공정 레이아웃</h2>
              <span class="title-tooltip" tabindex="0" aria-label="공정 내 설비 위치와 가동 상태를 확인할 수 있습니다.">
                ?
                <em>공정 내 설비 위치와 가동 상태를 확인할 수 있습니다.</em>
              </span>
            </div>

            <div class="legend-wrap">
              <span class="legend running"></span> 가동 (RUN)
              <span class="legend idle"></span> 대기 (IDLE)
              <span class="legend stop"></span> 정지 (STOP)
              <span class="legend alarm"></span> 알람 (ALARM)
            </div>
          </div>

          <div class="layout-card">
            <div class="layout-matrix">
              <div class="line-axis"></div>
              <div v-for="zone in zoneColumns" :key="zone.key" class="zone-heading">
                <strong>{{ zone.title }}</strong>
                <span>{{ zone.name }}({{ zone.code }})</span>
              </div>

              <template v-for="line in productionLines" :key="line.key">
                <div class="line-heading">
                  <span>{{ line.no }}</span>
                  <strong>{{ line.label }}</strong>
                </div>

                <div
                  v-for="(station, stationIndex) in line.stations"
                  :key="station.id"
                  class="station-slot"
                >
                  <button
                    type="button"
                    class="equipment-node"
                    :class="[station.status, station.type]"
                    @click="selectLayoutStation(station)"
                  >
                    <span class="node-icon" aria-hidden="true">
                      {{ equipmentTypeIcons[station.type] ?? '🏗️' }}
                    </span>
                    <strong>{{ station.label }}</strong>
                    <span class="node-status">{{ layoutStatusText[station.status] }}</span>
                  </button>

                  <button
                    v-if="stationIndex < line.stations.length - 1"
                    type="button"
                    class="conveyor-link"
                    :class="line.conveyor.status"
                    :aria-label="`${line.label} ${line.conveyor.label} 상세 보기`"
                    @click="selectLayoutStation(line.conveyor)"
                  >
                    <span>{{ line.conveyor.equipment_id }}</span>
                    <div class="conveyor-track">
                      <i v-for="index in 4" :key="index"></i>
                    </div>
                  </button>
                </div>
              </template>
            </div>
          </div>
        </section>

        <section v-else class="content-panel table-section">
          <div class="table-header">
            <div class="table-title">설비 목록</div>
            <label class="table-search">
              <span>⌕</span>
              <input v-model="equipmentSearch" type="search" placeholder="설비명, 위치, 유형, 상태 검색" />
            </label>
          </div>

          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>설비명</th>
                  <th>위치</th>
                  <th>설비 유형</th>
                  <th>상태</th>
                  <th>알람 상태</th>
                  <th>가동 시간</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="row in paginatedEquipmentRows" :key="row.id" @click="selectEquipmentRow(row)">
                  <td>{{ row.name }}</td>
                  <td>{{ row.line }}</td>
                  <td>{{ row.typeName }}</td>
                  <td><span class="status-pill" :class="row.status">{{ statusText[row.status] }}</span></td>
                  <td><span class="alarm-pill" :class="row.alarm">{{ alarmText[row.alarm] }}</span></td>
                  <td>{{ row.runningTime ?? '-' }}</td>
                </tr>
                <tr v-if="paginatedEquipmentRows.length === 0">
                  <td colspan="6" class="empty-row">검색 결과가 없습니다.</td>
                </tr>
              </tbody>
            </table>
          </div>

          <div class="pagination">
            <span>전체 {{ filteredEquipmentRows.length }}건</span>
            <div class="page-buttons">
              <button type="button" :disabled="currentPage === 1" @click="currentPage -= 1">‹</button>
              <button
                v-for="page in totalPages"
                :key="page"
                type="button"
                :class="{ current: currentPage === page }"
                @click="currentPage = page"
              >
                {{ page }}
              </button>
              <button type="button" :disabled="currentPage === totalPages" @click="currentPage += 1">›</button>
            </div>
          </div>
        </section>
      </section>

      <aside class="side-panel">
        <template v-if="selectedEquipment">
          <section class="info-card detail-card">
            <div class="side-header">
              <h3>설비 상세 정보</h3>
            </div>

            <div class="detail-header">
              <span class="detail-icon">{{ selectedEquipmentIcon }}</span>
              <div>
                <strong>{{ selectedEquipment?.name ?? '-' }}</strong>
                <span class="status-badge" :class="selectedEquipment?.status">
                  {{ statusText[selectedEquipment?.status] ?? '알 수 없음' }}
                </span>
              </div>
            </div>

            <dl class="info-list">
              <div><dt>제조사</dt><dd>{{ currentEquipmentDetail.manufacturer ?? '-' }}</dd></div>
              <div><dt>설비 ID</dt><dd>{{ currentEquipmentDetail.equipment_id ?? '-' }}</dd></div>
              <div><dt>설비 위치</dt><dd>{{ currentEquipmentLocation }}</dd></div>
              <div><dt>설비 유형</dt><dd>{{ selectedEquipmentTypeLabel }}</dd></div>
              <div><dt>마지막 업데이트</dt><dd>{{ currentEquipmentLastUpdate }}</dd></div>
            </dl>

            <div v-if="isConveyorSelected" class="inverter-control" :class="{ locked: !isConveyorControlSupported }">
              <div class="control-header">
                <h4>인버터 제어</h4>
                <span>{{ controlStatusText }}</span>
              </div>
              <div class="control-grid">
                <!-- 주파수 카드 -->
                <div class="control-box frequency-control">
                  <span>주파수</span>
                  <template v-if="inverterEditMode.frequency">
                    <div class="frequency-input-row">
                      <input
                        v-model.number="frequencyDraft"
                        type="number"
                        min="1"
                        step="1"
                        aria-label="컨베이어 주파수"
                      />
                      <strong>Hz</strong>
                    </div>
                    <div class="edit-actions">
                      <button type="button" class="apply-button" @click="applyFrequency">확인</button>
                      <button type="button" class="cancel-button" @click="cancelEditFrequency">취소</button>
                    </div>
                  </template>
                  <template v-else>
                    <strong class="control-value">{{ currentConveyorControl.frequency }} Hz</strong>
                    <button type="button" class="edit-button" @click="startEditFrequency">수정</button>
                  </template>
                </div>

                <!-- 가동 여부 카드 -->
                <div class="control-box">
                  <span>가동 여부</span>
                  <div class="state-display" :class="{ active: currentConveyorControl.rotating }">
                    {{ currentConveyorControl.rotating ? '가동 중' : '정지' }}
                  </div>
                  <button
                    type="button"
                    class="state-button"
                    :class="{ active: currentConveyorControl.rotating }"
                    @click="turnOnConveyor"
                  >
                    가동 시작
                  </button>
                </div>

                <!-- 정지 여부 카드 -->
                <div class="control-box">
                  <span>정지 여부</span>
                  <div class="state-display stop" :class="{ active: currentConveyorControl.stopped }">
                    {{ currentConveyorControl.stopped ? '정지 중' : '가동 중' }}
                  </div>
                  <button
                    type="button"
                    class="state-button stop"
                    :class="{ active: currentConveyorControl.stopped }"
                    @click="turnOffConveyor"
                  >
                    정지
                  </button>
                </div>
              </div>
            </div>

            <h4 class="sub-title">주요 데이터</h4>
            <div class="metrics-grid">
              <div v-for="metric in currentMetricCards" :key="metric.key" class="metric-box">
                <span>{{ metric.label }}</span>
                <strong>{{ metric.valueText }}</strong>
              </div>
            </div>
          </section>

          <section class="info-card alarm-card">
            <div class="side-header">
              <h3>최근 알람</h3>
              <button type="button" @click="goToAlarmPage">더보기 ›</button>
            </div>
            <ul class="recent-alarm-list">
              <li v-for="alarm in selectedRecentAlarms" :key="alarm.id">
                <span class="alarm-mark" :class="alarm.level">!</span>
                <div>
                  <strong>{{ alarm.title }}</strong>
                  <small>{{ alarm.time }}</small>
                </div>
                <em :class="alarm.level">{{ alarm.label }}</em>
              </li>
              <li v-if="selectedRecentAlarms.length === 0" class="empty-alarm-row">수신된 알람이 없습니다.</li>
            </ul>
          </section>
        </template>
      </aside>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRouter } from 'vue-router'
import mqtt from 'mqtt'
import { fetchEquipments, fetchLatestLog, fetchEquipmentRunningTime } from '../../api/mockEquipmentApi'
import AppTopbar from '@/components/AppTopbar.vue'

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'
const router = useRouter()
const activeView = ref('layout')
const equipmentSearch = ref('')
const currentPage = ref(1)
const rowsPerPage = 10

const defaultBrokerUrl = (() => {
  if (typeof window !== 'undefined' && window.location.protocol === 'https:') {
    return 'wss://broker.mqttdashboard.com:8884/mqtt'
  }
  return 'ws://broker.mqttdashboard.com:8000/mqtt'
})()

const rawBrokerUrl = import.meta.env.VITE_MQTT_BROKER_URL ?? import.meta.env.VITE_MQTT_URL ?? defaultBrokerUrl
const mqttRealtimeTopic = import.meta.env.VITE_MQTT_TOPIC ?? 'factory/equipment/+/realtime'
const mqttAlarmTopic = import.meta.env.VITE_MQTT_ALARM_TOPIC ?? 'factory/equipment/+/alarm'
const mqttUsername = import.meta.env.VITE_MQTT_USERNAME
const mqttPassword = import.meta.env.VITE_MQTT_PASSWORD

const statusText = {
  running: '가동',
  idle: '대기',
  stop: '정지',
  alarm: '알람',
  unknown: '알 수 없음',
}

const alarmText = {
  normal: '정상',
  warning: '경고',
  danger: '위험',
}

const recentAlarms = ref([])

const layoutStatusText = {
  running: '가동',
  idle: '대기',
  stop: '정지',
  alarm: '알람',
  unknown: '알 수 없음',
}

const zoneColumns = [
  { key: 'zone-a', title: 'ZONE A', name: '패널투입장치', code: 'PLF' },
  { key: 'zone-b', title: 'ZONE B', name: '차체지그', code: 'JIG' },
  { key: 'zone-c', title: 'ZONE C', name: '산업용로봇', code: 'ROB' },
  { key: 'zone-d', title: 'ZONE D', name: '점용접기', code: 'WLD' },
  { key: 'zone-e', title: 'ZONE E', name: '실러도포장비', code: 'SLR' },
  { key: 'zone-f', title: 'ZONE F', name: '비전검사기', code: 'VSI' },
]

const equipmentTypeLabels = {
  plf: '패널투입장치',
  jig: '차체지그',
  rob: '산업용로봇',
  wld: '점용접기',
  slr: '실러도포장비',
  vsi: '비전검사기',
  cnv: '컨베이어',
  robot: '산업용로봇',
  nutrunner: '너트러너',
}

const equipmentTypeIcons = {
  plf: '🏗️',
  jig: '📐',
  rob: '🤖',
  wld: '🔥',
  slr: '🖌️',
  vsi: '👁️',
  cnv: '🔄',
  robot: '🤖',
  nutrunner: '📐',
}

const monitoringEquipmentTypes = [
  { code: 'PLF', type: 'plf', zone: 'Zone A', label: '패널투입장치', manufacturer: 'SCHMALZ' },
  { code: 'JIG', type: 'jig', zone: 'Zone B', label: '차체지그', manufacturer: 'MISUMI' },
  { code: 'ROB', type: 'rob', zone: 'Zone C', label: '산업용로봇', manufacturer: 'Hi6-N00' },
  { code: 'WLD', type: 'wld', zone: 'Zone D', label: '점용접기', manufacturer: 'ARO' },
  { code: 'SLR', type: 'slr', zone: 'Zone E', label: '실러도포장비', manufacturer: 'Durr' },
  { code: 'VSI', type: 'vsi', zone: 'Zone F', label: '비전검사기', manufacturer: 'VITRONIC' },
  { code: 'CNV', type: 'cnv', zone: 'Zone G', label: '컨베이어', manufacturer: 'Hyundai Rotem' },
]

const monitoringEquipmentTypeByCode = monitoringEquipmentTypes.reduce((map, item) => {
  map[item.code] = item
  return map
}, {})

const monitoringEquipmentCatalog = monitoringEquipmentTypes.flatMap((item, typeIndex) =>
  [1, 2, 3].map((sequence, sequenceIndex) => ({
    id: `${item.code}-${String(sequence).padStart(3, '0')}`,
    name: `${item.code}-${String(sequence).padStart(3, '0')}`,
    line: `${item.zone} - Line ${sequence}`,
    lineNo: `Line ${sequence}`,
    zone: item.zone,
    rawType: item.type,
    typeName: item.label,
    manufacturer: item.manufacturer,
    status: ['running', 'running', 'idle', 'running', 'stop', 'running', 'running'][(typeIndex + sequenceIndex) % 7],
    runningTime:
      (typeIndex + sequenceIndex) % 5 === 4
        ? '00:00:00'
        : `02:${String(10 + typeIndex * 3 + sequenceIndex).padStart(2, '0')}:${String(12 + typeIndex + sequenceIndex * 4).padStart(2, '0')}`,
  })),
)

const formatEquipmentId = (code, lineNo) => `${code}-${String(lineNo).padStart(3, '0')}`

const createStation = (lineNo, type, zone, status) => {
  const code = type.toUpperCase()
  const meta = monitoringEquipmentTypeByCode[code] ?? {}
  const equipmentId = formatEquipmentId(code, lineNo)

  return {
    id: equipmentId,
    label: equipmentId,
    type,
    status: status ?? 'running',
    zone,
    line: `Line ${lineNo}`,
    manufacturer: meta.manufacturer ?? 'BS-SCADA',
    equipment_id: equipmentId,
    equipment_name: equipmentId,
    line_no: `Line ${lineNo}`,
  }
}

const createConveyorStation = (lineNo, status = 'running') => ({
  id: formatEquipmentId('CNV', lineNo),
  label: formatEquipmentId('CNV', lineNo),
  type: 'cnv',
  status,
  zone: 'Line Conveyor',
  line: `Line ${lineNo}`,
  manufacturer: monitoringEquipmentTypeByCode.CNV?.manufacturer ?? 'BS-SCADA',
  equipment_id: formatEquipmentId('CNV', lineNo),
  equipment_name: formatEquipmentId('CNV', lineNo),
  line_no: `Line ${lineNo}`,
})

const createLineStations = (lineNo, statuses) => [
  createStation(lineNo, 'plf', 'Zone A', statuses.plf),
  createStation(lineNo, 'jig', 'Zone B', statuses.jig),
  createStation(lineNo, 'rob', 'Zone C', statuses.rob),
  createStation(lineNo, 'wld', 'Zone D', statuses.wld),
  createStation(lineNo, 'slr', 'Zone E', statuses.slr),
  createStation(lineNo, 'vsi', 'Zone F', statuses.vsi),
]

const productionLines = [
  { key: 'line-1', no: 1, label: 'Line 1', conveyor: createConveyorStation(1), stations: createLineStations(1, { slr: 'idle' }) },
  { key: 'line-2', no: 2, label: 'Line 2', conveyor: createConveyorStation(2, 'idle'), stations: createLineStations(2, { rob: 'idle' }) },
  { key: 'line-3', no: 3, label: 'Line 3', conveyor: createConveyorStation(3), stations: createLineStations(3, { rob: 'stop' }) },
]

const conveyorControls = reactive({
  'CNV-001': { frequency: 45, rotating: true, stopped: false },
  'CNV-002': { frequency: 35, rotating: false, stopped: true },
  'CNV-003': { frequency: 40, rotating: true, stopped: false },
})
const frequencyDraft = ref(0)
const rotationDraft = ref(false)
const stopDraft = ref(false)
const inverterEditMode = reactive({ frequency: false, rotation: false, stop: false })
const controlStatusText = ref('상태 확인 전')

const statusCodeMap = {
  RUN: 'running',
  RUNNING: 'running',
  IDLE: 'idle',
  WAIT: 'idle',
  STOP: 'stop',
  STOPPED: 'stop',
  ALARM: 'alarm',
  UNKNOWN: 'unknown',
}

const realtimeEquipmentData = reactive({})
let mqttClient = null

const authHeaders = () => {
  const token = localStorage.getItem('token')
  return token ? { Authorization: `Bearer ${token}` } : {}
}

const fetchWithTimeout = async (url, options = {}, timeoutMs = 5000) => {
  const controller = new AbortController()
  const tid = setTimeout(() => controller.abort(), timeoutMs)
  try {
    return await fetch(url, { ...options, signal: controller.signal })
  } finally {
    clearTimeout(tid)
  }
}

const safeParseJson = async (res) => {
  try { return await res.json() } catch { return null }
}

const parsePayload = (buffer) => {
  try {
    const parsed = JSON.parse(buffer.toString())
    return parsed && typeof parsed === 'object' ? parsed : null
  } catch {
    return null
  }
}

const parseTopicEquipmentId = (topic, suffix) => {
  const match = new RegExp(`^factory/equipment/([^/]+)/${suffix}$`, 'i').exec(topic ?? '')
  return match?.[1] ?? ''
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

  const normalizedInput = value
    .replace(/^mqtt-dashboard\.com(:|$)/i, 'broker.mqttdashboard.com$1')
    .replace(/^broker\.mqtt-dashboard\.com(:|$)/i, 'broker.mqttdashboard.com$1')
  const url = new URL(`ws://${normalizedInput}`)
  if (!url.port || url.port === '1883') url.port = '8000'
  if (!url.pathname || url.pathname === '/') url.pathname = '/mqtt'
  return url.href
}

// 설비 유형별 센서 데이터 라벨
const getSensorDisplayLabels = (type) => {
  if (type === 'cnv') {
    return {
      sensor1: { label: '모터 내부 온도(℃)', key: 'motor_temperature_c', decimals: 1 },
      sensor2: { label: '모터 전류(A)', key: ['motor_current_c', 'motor_current_a', 'motorCurrentA'], decimals: 1 },
      sensor3: { label: '이동 속도(m/s)', key: 'moving_speed_m_s', decimals: 2 },
    }
  }
  if (type === 'plf') {
    return {
      sensor1: { label: '모터 전류(A)', key: ['moter_current_a', 'motor_current_a', 'motorCurrentA'], decimals: 1 },
      sensor2: { label: '진공 압력(kPa)', key: ['vaccum_pressure_kpa', 'vacuum_pressure_kpa', 'vacuumPressureKpa'], decimals: 1 },
      sensor3: { label: '위치 오차(mm)', key: 'position_error_mm', decimals: 2 },
    }
  }
  if (type === 'jig') {
    return {
      sensor1: { label: '클램프 압력(bar)', key: 'clamp_pressure_bar', decimals: 2 },
      sensor2: { label: '공압 압력(bar)', key: ['pneumatic_press_bar', 'pneumatic_pressure_bar', 'pneumaticPressureBar'], decimals: 2 },
      sensor3: { label: '클램프 위치(mm)', key: 'clamp_position_mm', decimals: 1 },
    }
  }
  if (type === 'robot' || type === 'rob') {
    return {
      sensor1: { label: '로봇 스위블(deg)', key: 'robot_swivel', decimals: 1 },
      sensor2: { label: '로봇 수평축(mm)', key: 'robot_horizontal', decimals: 1 },
      sensor3: { label: '로봇 수직축(mm)', key: 'robot_vertical', decimals: 1 },
      sensor4: { label: '툴 오프셋 오차(mm)', key: ['tool_offset_er', 'tool_offset_error_mm', 'toolOffsetErrorMm'], decimals: 2 },
    }
  }
  if (type === 'wld') {
    return {
      sensor1: { label: '용접 전압 DC(V)', key: 'weld_voltage_dc', decimals: 1 },
      sensor2: { label: '용접 전류 DC(A)', key: 'weld_current_dc', decimals: 1 },
      sensor3: { label: '용접 전류 AC(A)', key: 'weld_current_ac', decimals: 1 },
      sensor4: { label: '용접 속도(mm/s)', key: 'weld_speed', decimals: 1 },
    }
  }
  if (type === 'slr') {
    return {
      sensor1: { label: '토출 압력(bar)', key: 'dispense_pressure_bar', decimals: 2 },
      sensor2: { label: '실러 온도(℃)', key: 'sealer_temperature_c', decimals: 1 },
      sensor3: { label: '유량(ml/s)', key: 'flow_rate_ml_s', decimals: 2 },
    }
  }
  return {}
}

// 상태 데이터
const equipment = reactive({
  list: [],
  loading: false,
  error: null,
})

const layoutItems = ref([])
const selectedEquipment = ref(null)
const latestLog = reactive({
  data: null,
  loading: false,
})
const runningTime = ref('00:00:00')

const setActiveView = (view) => {
  if (activeView.value === view) return

  activeView.value = view
  selectedEquipment.value = null
  latestLog.data = null
  latestLog.loading = false
  runningTime.value = '00:00:00'
}

const selectLayoutStation = (station) => {
  const meta = getEquipmentMeta(station.equipment_id ?? station.id)
  selectedEquipment.value = {
    id: station.id,
    name: station.label,
    status: station.status,
    type: station.type,
    zone: station.zone,
    line: station.line,
    layoutType: station.type,
    manufacturer: station.manufacturer ?? meta.manufacturer,
    equipment_id: station.equipment_id,
    equipment_name: station.equipment_name,
    line_no: station.line_no,
  }
}

const selectEquipmentRow = (row) => {
  selectedEquipment.value = {
    id: row.id,
    name: row.name,
    status: row.status,
    type: row.rawType,
    zone: row.zone,
    line: row.lineNo,
    layoutType: row.rawType,
    manufacturer: row.manufacturer,
    equipment_id: row.id,
    equipment_name: row.name,
    line_no: row.lineNo,
  }
}

const startEditFrequency = () => {
  frequencyDraft.value = currentConveyorControl.value.frequency || 1
  inverterEditMode.frequency = true
}
const cancelEditFrequency = () => {
  frequencyDraft.value = currentConveyorControl.value.frequency || 1
  inverterEditMode.frequency = false
}
const applyFrequency = async () => {
  await confirmConveyorFrequency()
  inverterEditMode.frequency = false
}

const startEditRotation = () => {
  rotationDraft.value = currentConveyorControl.value.rotating
  inverterEditMode.rotation = true
}
const cancelEditRotation = () => { inverterEditMode.rotation = false }
const applyRotation = async () => {
  if (rotationDraft.value) await turnOnConveyor()
  inverterEditMode.rotation = false
}

const startEditStop = () => {
  stopDraft.value = currentConveyorControl.value.stopped
  inverterEditMode.stop = true
}
const cancelEditStop = () => { inverterEditMode.stop = false }
const applyStop = async () => {
  if (stopDraft.value) await turnOffConveyor()
  inverterEditMode.stop = false
}

const goToEquipmentDetail = () => {
  const id = selectedEquipment.value?.equipment_id ?? selectedEquipment.value?.id
  router.push({ name: 'equipment-detail', query: id ? { id } : {} })
}

const goToAlarmPage = () => {
  const id = selectedEquipment.value?.equipment_id ?? selectedEquipment.value?.id
  router.push({ name: 'equipment-alarm', query: id ? { equipmentId: id } : {} })
}

const isConveyorSelected = computed(() => selectedEquipment.value?.layoutType === 'cnv')
const isConveyorControlSupported = computed(() => selectedEquipment.value?.id === 'CNV-001')

const currentConveyorControl = computed(() => {
  const id = selectedEquipment.value?.equipment_id ?? selectedEquipment.value?.id
  return conveyorControls[id] ?? { frequency: 0, rotating: false, stopped: true }
})

const confirmConveyorFrequency = () => {
  const id = selectedEquipment.value?.equipment_id ?? selectedEquipment.value?.id
  if (!id || !conveyorControls[id]) return
  if (!isConveyorControlSupported.value) {
    controlStatusText.value = 'CNV-001만 제어 가능'
    return
  }

  const nextFrequency = Number(frequencyDraft.value)
  if (!Number.isFinite(nextFrequency)) return
  conveyorControls[id].frequency = Math.max(nextFrequency, 1)
}

const setConveyorRotationState = (id, rotating) => {
  if (!id || !conveyorControls[id]) return
  conveyorControls[id].rotating = rotating
  conveyorControls[id].stopped = !rotating
}

const applyControlStatus = (equipmentId, data) => {
  if (!equipmentId || !conveyorControls[equipmentId] || !data) return
  const status = String(data.currentStatus ?? '').toUpperCase()
  const lastFrequency = Number(data.lastFrequency)

  setConveyorRotationState(equipmentId, status === 'RUN')
  if (Number.isFinite(lastFrequency)) {
    conveyorControls[equipmentId].frequency = lastFrequency
    if (selectedEquipment.value?.id === equipmentId && !inverterEditMode.frequency) {
      frequencyDraft.value = lastFrequency
    }
  }
}

const fetchConveyorControlStatus = async (equipmentId) => {
  if (!equipmentId || !conveyorControls[equipmentId]) return
  if (equipmentId !== 'CNV-001') {
    controlStatusText.value = 'CNV-001만 제어 가능'
    return
  }

  try {
    controlStatusText.value = '상태 확인 중'
    const res = await fetchWithTimeout(
      `${API_BASE}/api/equipments/${encodeURIComponent(equipmentId)}/control/status`,
      { headers: authHeaders() },
      5000,
    )
    const body = await safeParseJson(res)
    if (!res.ok) throw new Error(body?.message ?? `HTTP ${res.status}`)

    // success 필드 없어도 data가 있으면 적용 (Node-RED 응답 포맷 유연 처리)
    const data = body?.data ?? body
    if (data) applyControlStatus(equipmentId, data)
    controlStatusText.value = 'API 연결'
  } catch (err) {
    controlStatusText.value = err.name === 'AbortError' ? '연결 타임아웃' : '백엔드 연결 실패'
    console.warn('Failed to fetch conveyor control status:', err)
  }
}

const turnOnConveyor = async () => {
  const id = selectedEquipment.value?.equipment_id ?? selectedEquipment.value?.id
  if (!id || !conveyorControls[id]) return
  if (!isConveyorControlSupported.value) {
    controlStatusText.value = 'CNV-001만 제어 가능'
    return
  }

  confirmConveyorFrequency()
  setConveyorRotationState(id, true)
  // 레이아웃 노드 + 선택된 설비 상태 즉시 반영
  const convOnStation = productionLines.flatMap((l) => [l.conveyor, ...l.stations]).find((s) => s.id === id)
  if (convOnStation) convOnStation.status = 'running'
  if (selectedEquipment.value?.id === id) selectedEquipment.value = { ...selectedEquipment.value, status: 'running' }
  controlStatusText.value = '가동 명령 전송 중'

  try {
    const res = await fetchWithTimeout(
      `${API_BASE}/api/equipments/${encodeURIComponent(id)}/control/on`,
      {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', ...authHeaders() },
        body: JSON.stringify({ frequency: conveyorControls[id].frequency }),
      },
      5000,
    )
    const body = await safeParseJson(res)
    if (!res.ok) throw new Error(body?.message ?? `HTTP ${res.status}`)
    controlStatusText.value = '가동 명령 완료'
  } catch (err) {
    controlStatusText.value = err.name === 'AbortError' ? '연결 타임아웃 (로컬 적용)' : '가동 명령 실패 (로컬 적용)'
    console.warn('Failed to turn on conveyor:', err)
  }
}

const turnOffConveyor = async () => {
  const id = selectedEquipment.value?.equipment_id ?? selectedEquipment.value?.id
  if (!id || !conveyorControls[id]) return
  if (!isConveyorControlSupported.value) {
    controlStatusText.value = 'CNV-001만 제어 가능'
    return
  }

  setConveyorRotationState(id, false)
  const convOffStation = productionLines.flatMap((l) => [l.conveyor, ...l.stations]).find((s) => s.id === id)
  if (convOffStation) convOffStation.status = 'stop'
  if (selectedEquipment.value?.id === id) selectedEquipment.value = { ...selectedEquipment.value, status: 'stop' }
  controlStatusText.value = '정지 명령 전송 중'

  try {
    const res = await fetchWithTimeout(
      `${API_BASE}/api/equipments/${encodeURIComponent(id)}/control/off`,
      { method: 'POST', headers: authHeaders() },
      5000,
    )
    const body = await safeParseJson(res)
    if (!res.ok) throw new Error(body?.message ?? `HTTP ${res.status}`)
    controlStatusText.value = '정지 명령 완료'
  } catch (err) {
    controlStatusText.value = err.name === 'AbortError' ? '연결 타임아웃 (로컬 적용)' : '정지 명령 실패 (로컬 적용)'
    console.warn('Failed to turn off conveyor:', err)
  }
}

// 설비별 최신 로그 데이터 캐시
const equipmentLogCache = reactive({})

const shouldApplyLogStatus = (logData) => {
  return logData?.status && logData.status !== 'unknown'
}

const normalizeStatus = (status) => {
  const normalizedStatus = String(status ?? '').trim().toUpperCase()
  return statusCodeMap[normalizedStatus] ?? String(status ?? 'unknown').toLowerCase()
}

const normalizeLineNo = (lineNo) => {
  const value = String(lineNo ?? '').trim()
  if (!value) return ''
  if (/^line\s*/i.test(value)) return value.replace(/\s+/g, '')
  return `Line${value}`
}

const formatEquipmentLocation = (lineNo, zone) => {
  const line = normalizeLineNo(lineNo)
  const normalizedZone = String(zone ?? '').trim().replace(/\s+/g, '')
  return [line, normalizedZone].filter(Boolean).join(' - ') || '-'
}

const getEquipmentCode = (equipmentId) => String(equipmentId ?? '').split('-')[0]?.toUpperCase() ?? ''

const getEquipmentMeta = (equipmentId) => monitoringEquipmentTypeByCode[getEquipmentCode(equipmentId)] ?? {}

const getRealtimeData = (equipmentId) => realtimeEquipmentData[equipmentId] ?? equipmentLogCache[equipmentId]

const formatRunSeconds = (seconds) => {
  const totalSeconds = Number(seconds)
  if (!Number.isFinite(totalSeconds)) return null

  const safeSeconds = Math.max(0, Math.floor(totalSeconds))
  const hours = Math.floor(safeSeconds / 3600)
  const minutes = Math.floor((safeSeconds % 3600) / 60)
  const remainSeconds = safeSeconds % 60
  return [hours, minutes, remainSeconds].map((unit) => String(unit).padStart(2, '0')).join(':')
}

const getRunningTimeText = (equipmentId, fallback = '00:00:00') => {
  const realtimeData = getRealtimeData(equipmentId)
  return formatRunSeconds(realtimeData?.accumulated_run_hours) ?? fallback
}

const getLocationForEquipment = (equipmentId, lineNo, zone) => {
  const meta = getEquipmentMeta(equipmentId)
  if (meta.type === 'cnv') return normalizeLineNo(lineNo) || '-'
  return formatEquipmentLocation(lineNo, zone ?? meta.zone)
}

const applyRealtimePayload = (payload) => {
  const equipmentId = String(payload?.equipment_id ?? '').trim()
  if (!equipmentId) return

  const meta = getEquipmentMeta(equipmentId)
  const realtimeData = {
    ...payload,
    equipment_id: equipmentId,
    equipment_name: payload.equipment_name ?? meta.label ?? equipmentId,
    manufacturer: payload.manufacturer ?? meta.manufacturer ?? 'BS-SCADA',
    zone: payload.zone ?? meta.zone,
    type: payload.type ?? meta.type,
    status: normalizeStatus(payload.status),
  }

  realtimeEquipmentData[equipmentId] = realtimeData
  equipmentLogCache[equipmentId] = realtimeData

  const station = productionLines
    .flatMap((line) => [line.conveyor, ...line.stations])
    .find((item) => item.id === equipmentId)

  if (station) {
    station.status = realtimeData.status
    station.manufacturer = realtimeData.manufacturer
    station.equipment_name = realtimeData.equipment_name
    station.line_no = normalizeLineNo(realtimeData.line_no) || station.line_no
    station.zone = realtimeData.zone ?? station.zone
  }

  if (selectedEquipment.value?.id === equipmentId) {
    selectedEquipment.value = {
      ...selectedEquipment.value,
      status: realtimeData.status,
      manufacturer: realtimeData.manufacturer,
      zone: realtimeData.zone ?? selectedEquipment.value.zone,
      line_no: normalizeLineNo(realtimeData.line_no) || selectedEquipment.value.line_no,
      equipment_name: realtimeData.equipment_name,
    }
    latestLog.data = realtimeData
    runningTime.value = getRunningTimeText(equipmentId, runningTime.value)
  }
}

const alarmLevelMap = {
  NORMAL: 'normal',
  WARN: 'warning',
  WARNING: 'warning',
  DANGER: 'danger',
  ALARM: 'danger',
  CRITICAL: 'danger',
}

const alarmLabelMap = {
  normal: '정상',
  warning: '경고',
  danger: '위험',
}

const normalizeAlarmLevel = (status) => {
  return alarmLevelMap[String(status ?? '').trim().toUpperCase()] ?? 'warning'
}

const isFullEquipmentId = (id) => /^[A-Z]+-\d{3}$/i.test(id)

const applyAlarmPayload = (payload, topic) => {
  const rawId = String(payload?.equipment_id ?? '').trim()
  const topicId = parseTopicEquipmentId(topic, 'alarm')
  // CODE-NNN 형식(예: CNV-001)인 ID를 우선 사용 — prefix만 있는 ID는 무시하여 오필터링 방지
  const equipmentId = isFullEquipmentId(rawId) ? rawId
    : isFullEquipmentId(topicId) ? topicId
    : (rawId || topicId)
  if (!equipmentId || !isFullEquipmentId(equipmentId)) return

  const level = normalizeAlarmLevel(payload.alarm_status)
  const nextAlarm = {
    id: `${equipmentId}-${payload.timestamp ?? Date.now()}-${payload.alarm_type ?? 'alarm'}`,
    equipmentId,
    title: payload.alarm_type ?? '-',
    time: payload.timestamp ?? '-',
    level,
    label: alarmLabelMap[level] ?? payload.alarm_status ?? '-',
  }

  recentAlarms.value = [
    nextAlarm,
    ...recentAlarms.value.filter((alarm) => alarm.id !== nextAlarm.id),
  ].slice(0, 50)
}

const handleMqttMessage = (topic, message) => {
  const payload = parsePayload(message)
  if (!payload) return

  if (/\/alarm$/i.test(topic ?? '')) {
    applyAlarmPayload(payload, topic)
    return
  }

  if (/\/realtime$/i.test(topic ?? '')) {
    applyRealtimePayload({
      ...payload,
      equipment_id: payload.equipment_id ?? parseTopicEquipmentId(topic, 'realtime'),
    })
  }
}

const connectMqtt = () => {
  mqttClient = mqtt.connect(resolveBrokerUrlForBrowser(rawBrokerUrl), {
    clientId: `bs-scada-equipment-${Math.random().toString(16).slice(2, 10)}`,
    username: mqttUsername,
    password: mqttPassword,
    reconnectPeriod: 3000,
    connectTimeout: 10000,
    clean: true,
  })

  mqttClient.on('connect', () => {
    mqttClient.subscribe([mqttRealtimeTopic, mqttAlarmTopic], { qos: 0 }, (err) => {
      if (err) console.warn('Failed to subscribe equipment MQTT topics:', err)
    })
  })

  mqttClient.on('message', handleMqttMessage)
  mqttClient.on('error', (err) => {
    console.warn('Equipment realtime MQTT connection error:', err)
  })
}

// 테이블 데이터
const equipmentRows = computed(() => {
  return monitoringEquipmentCatalog.map((eq) => {
    const cachedLog = getRealtimeData(eq.id)
    const status = shouldApplyLogStatus(cachedLog) ? cachedLog.status : eq.status

    return {
      id: eq.id,
      name: eq.name,
      line: getLocationForEquipment(eq.id, cachedLog?.line_no ?? eq.lineNo, cachedLog?.zone ?? eq.zone),
      lineNo: normalizeLineNo(cachedLog?.line_no ?? eq.lineNo),
      zone: cachedLog?.zone ?? eq.zone,
      rawType: eq.rawType,
      typeName: cachedLog?.equipment_name ?? eq.typeName,
      manufacturer: cachedLog?.manufacturer ?? eq.manufacturer,
      status: status,
      alarm: status === 'alarm' || status === 'stop' ? 'danger' : status === 'idle' ? 'warning' : 'normal',
      runningTime: getRunningTimeText(eq.id, eq.runningTime),
      updatedAt: cachedLog?.timestamp || '2024-05-24 10:30:45'
    }
  })
})

const filteredEquipmentRows = computed(() => {
  const query = equipmentSearch.value.trim().toLowerCase()
  if (!query) return equipmentRows.value

  return equipmentRows.value.filter((row) =>
    [row.name, row.line, row.typeName, statusText[row.status], alarmText[row.alarm]]
      .join(' ')
      .toLowerCase()
      .includes(query),
  )
})

const totalPages = computed(() => Math.max(1, Math.ceil(filteredEquipmentRows.value.length / rowsPerPage)))

const paginatedEquipmentRows = computed(() => {
  const start = (currentPage.value - 1) * rowsPerPage
  return filteredEquipmentRows.value.slice(start, start + rowsPerPage)
})

watch(equipmentSearch, () => {
  currentPage.value = 1
})

watch(totalPages, (nextTotalPages) => {
  if (currentPage.value > nextTotalPages) {
    currentPage.value = nextTotalPages
  }
})

// 초기 데이터 로드
onMounted(async () => {
  connectMqtt()

  try {
    equipment.loading = true
    const equipmentsData = await fetchEquipments()
    equipment.list = equipmentsData
    
    // 레이아웃 아이템 구성 및 설비별 로그 데이터 로드
    layoutItems.value = await Promise.all(
      equipmentsData.map(async (eq) => {
        // 설비 로그 데이터 로드 및 캐시
        try {
          const logData = await fetchLatestLog(eq.equipment_id)
          equipmentLogCache[eq.equipment_id] = logData
        } catch (err) {
          console.warn(`Failed to fetch log for equipment ${eq.equipment_id}:`, err)
        }
        
        return {
          id: eq.equipment_id,
          name: eq.equipment_name,
          status: equipmentLogCache[eq.equipment_id]?.status || 'unknown',
          type: eq.type,
          zone: eq.zone,
          line: eq.line_no,
        }
      })
    )
  } catch (err) {
    equipment.error = err.message
    console.error('Failed to fetch equipment list:', err)
  } finally {
    equipment.loading = false
  }
})

// 선택 설비가 바뀌면 로그 데이터 로드
watch(selectedEquipment, async (newEquipment, oldEquipment) => {
  if (!newEquipment || !newEquipment.id) return

  const idChanged = newEquipment.id !== oldEquipment?.id

  if (newEquipment.layoutType === 'cnv') {
    if (idChanged) {
      // 설비가 바뀔 때만 편집 모드 초기화 + 상태 조회
      inverterEditMode.frequency = false
      inverterEditMode.rotation = false
      inverterEditMode.stop = false
      const control = conveyorControls[newEquipment.id]
      frequencyDraft.value = control?.frequency ?? 1
      fetchConveyorControlStatus(newEquipment.id)
    }
    return  // MQTT 업데이트 등 동일 ID 재실행은 이하 로직 불필요
  }
  
  try {
    latestLog.loading = true

    const realtimeData = getRealtimeData(newEquipment.id)
    if (realtimeData) {
      latestLog.data = realtimeData
      runningTime.value = getRunningTimeText(newEquipment.id, runningTime.value)
      return
    }
    
    // 최신 로그 데이터 로드
    const logData = await fetchLatestLog(newEquipment.id)
    if (shouldApplyLogStatus(logData)) {
      latestLog.data = logData
      equipmentLogCache[newEquipment.id] = logData
    } else {
      latestLog.data = null
    }
    
    // 가동 시간 로드
    const timeData = await fetchEquipmentRunningTime(newEquipment.id)
    runningTime.value = timeData.running_time || '00:00:00'
    
    // 레이아웃 상태 업데이트
    const layoutItem = layoutItems.value.find(item => item.id === newEquipment.id)
    if (layoutItem) {
      if (shouldApplyLogStatus(logData)) {
        layoutItem.status = logData.status
      }
    }
  } catch (err) {
    console.error('Failed to fetch equipment data:', err)
  } finally {
    latestLog.loading = false
  }
}, { immediate: true })

onBeforeUnmount(() => {
  if (!mqttClient) return
  mqttClient.end(true)
  mqttClient = null
})

// 현재 설비의 상세 정보
const currentEquipmentDetail = computed(() => {
  if (!selectedEquipment.value) return {}
  
  const equipDetail = equipment.list.find(eq => eq.equipment_id === selectedEquipment.value.id)
  const realtimeData = getRealtimeData(selectedEquipment.value.id) ?? {}
  const meta = getEquipmentMeta(selectedEquipment.value.id)
  const selectedManufacturer = selectedEquipment.value.manufacturer
  const manufacturer = realtimeData.manufacturer
    ?? (selectedManufacturer === 'BS-SCADA' && meta.manufacturer ? meta.manufacturer : selectedManufacturer)
    ?? meta.manufacturer
    ?? 'BS-SCADA'

  return {
    ...(equipDetail || {}),
    ...realtimeData,
    equipment_id: selectedEquipment.value.equipment_id ?? selectedEquipment.value.id,
    equipment_name: realtimeData.equipment_name ?? selectedEquipment.value.equipment_name ?? selectedEquipment.value.name,
    manufacturer,
    zone: realtimeData.zone ?? selectedEquipment.value.zone,
    line_no: normalizeLineNo(realtimeData.line_no ?? selectedEquipment.value.line_no ?? selectedEquipment.value.line),
    type: realtimeData.type ?? selectedEquipment.value.layoutType ?? selectedEquipment.value.type,
  }
})

const currentEquipmentLocation = computed(() => {
  return getLocationForEquipment(
    currentEquipmentDetail.value.equipment_id,
    currentEquipmentDetail.value.line_no,
    currentEquipmentDetail.value.zone,
  )
})

const currentEquipmentLastUpdate = computed(() => {
  return latestLog.data?.timestamp ?? getRealtimeData(selectedEquipment.value?.id)?.timestamp ?? '-'
})

const selectedRecentAlarms = computed(() => {
  const selectedId = selectedEquipment.value?.id
  if (!selectedId) return []
  return recentAlarms.value.filter((alarm) => alarm.equipmentId === selectedId).slice(0, 3)
})

const selectedEquipmentTypeLabel = computed(() => {
  const realtimeTypeName = getRealtimeData(selectedEquipment.value?.id)?.equipment_name
  if (realtimeTypeName) return realtimeTypeName

  const type = currentEquipmentDetail.value.type ?? selectedEquipment.value?.layoutType ?? selectedEquipment.value?.type
  return equipmentTypeLabels[type] ?? equipmentTypeLabels[selectedEquipment.value?.type] ?? '-'
})

const selectedEquipmentIcon = computed(() => {
  const type = currentEquipmentDetail.value.type ?? selectedEquipment.value?.layoutType ?? selectedEquipment.value?.type
  return equipmentTypeIcons[type] ?? equipmentTypeIcons[selectedEquipment.value?.type] ?? '🏗️'
})

// 현재 설비의 센서 데이터 라벨
const currentSensorLabels = computed(() => {
  return getSensorDisplayLabels(selectedEquipment.value?.type)
})

const formatMetricValue = (field) => {
  const keys = Array.isArray(field.key) ? field.key : [field.key]
  const rawValue = keys.map((key) => latestLog.data?.[key]).find((value) => value !== undefined && value !== null)
  const value = Number(rawValue)
  if (!Number.isFinite(value)) return '-'
  return value.toFixed(field.decimals ?? 1)
}

const currentMetricCards = computed(() => {
  const sensorCards = Object.entries(currentSensorLabels.value).map(([key, field]) => ({
    key,
    label: field.label,
    valueText: formatMetricValue(field),
  }))

  return [
    { key: 'running-time', label: '가동 시간', valueText: runningTime.value || '-' },
    ...sensorCards,
  ]
})
</script>

<style scoped>
* {
  box-sizing: border-box;
}

.equipment-page {
  height: 100vh;
  overflow: hidden;
  background: #f4f7fb;
  color: #0f2748;
  display: flex;
  flex-direction: column;
}

.page-body {
  height: calc(100vh - 70px);
  display: grid;
  grid-template-columns: minmax(0, 1fr) 442px;
  grid-template-rows: auto minmax(0, 1fr);
  gap: 12px;
  padding: 10px 12px 12px;
  overflow: hidden;
}

.view-tabs {
  grid-column: 1 / -1;
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 44px;
  padding: 4px;
  border: 1px solid #dfe8f4;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 6px 18px rgba(35, 63, 104, 0.05);
}

.view-tabs button {
  height: 34px;
  min-width: 132px;
  padding: 0 18px;
  border: 1px solid transparent;
  border-radius: 8px;
  background: transparent;
  color: #51627a;
  font-size: 14px;
  font-weight: 900;
  cursor: pointer;
}

.view-tabs button.active {
  color: #fff;
  background: #073c7b;
  box-shadow: 0 8px 18px rgba(7, 60, 123, 0.18);
}

.left-column {
  min-width: 0;
  min-height: 0;
  height: 100%;
  display: flex;
  overflow: hidden;
}

.content-panel,
.info-card {
  background: #fff;
  border: 1px solid #e3eaf4;
  border-radius: 14px;
  box-shadow: 0 8px 24px rgba(35, 63, 104, 0.06);
}

.line-layout-section {
  width: 100%;
  min-height: 0;
  padding: 12px;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  gap: 12px;
  overflow: hidden;
}

.panel-header,
.side-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.panel-header {
  min-width: 0;
}

.title-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  flex: 0 0 auto;
  justify-content: flex-start;
}

.section-icon {
  color: #356aa0;
  font-size: 21px;
}

h2,
h3,
h4 {
  margin: 0;
}

h2 {
  font-size: 20px;
  font-weight: 950;
  white-space: nowrap;
  text-align: left;
}

h2 span {
  color: #52647a;
  font-weight: 700;
}

.title-tooltip {
  position: relative;
  width: 20px;
  height: 20px;
  display: inline-grid;
  place-items: center;
  flex: 0 0 20px;
  border: 1px solid #b9c6d8;
  border-radius: 50%;
  color: #48617f;
  background: #fff;
  font-size: 12px;
  font-weight: 950;
  cursor: help;
}

.title-tooltip em {
  position: absolute;
  left: 0;
  top: 28px;
  z-index: 20;
  width: 300px;
  display: none;
  padding: 10px 12px;
  border-radius: 8px;
  color: #fff;
  background: #08234d;
  box-shadow: 0 10px 24px rgba(0, 0, 0, 0.18);
  font-style: normal;
  font-size: 15px;
  line-height: 1.45;
  font-weight: 750;
}

.title-tooltip:hover em,
.title-tooltip:focus em {
  display: block;
}

.legend-wrap {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 7px;
  color: #627087;
  font-size: 12px;
  white-space: nowrap;
  min-width: 0;
  overflow: hidden;
}

.legend {
  width: 8px;
  height: 8px;
  flex: 0 0 8px;
  display: inline-block;
  border-radius: 50%;
}

.legend.running {
  background: #20c985;
}

.legend.idle {
  background: #3b82f6;
}

.legend.stop {
  background: #ffb435;
}

.legend.alarm {
  background: #ff4f63;
}

.filter-select,
.pagination select {
  height: 30px;
  padding: 0 10px;
  border: 1px solid #d5e0ee;
  border-radius: 9px;
  background: #fff;
  color: #0f2748;
  font-weight: 700;
}

.layout-card {
  min-height: 0;
  padding: 0;
  border: 1px solid #e1e8f2;
  border-radius: 12px;
  overflow: hidden;
  background: #fff;
}

.layout-matrix {
  height: 100%;
  display: grid;
  grid-template-columns: 92px repeat(6, minmax(140px, 1fr));
  grid-template-rows: 92px repeat(3, minmax(150px, 1fr));
  overflow: auto;
}

.line-axis,
.zone-heading,
.line-heading,
.station-slot {
  border-right: 1px solid #e3e9f2;
  border-bottom: 1px solid #e3e9f2;
}

.zone-heading {
  display: grid;
  align-content: center;
  justify-items: center;
  gap: 5px;
  color: #09295a;
  text-align: center;
  background: #fbfdff;
}

.zone-heading strong {
  font-size: 15px;
  font-weight: 950;
}

.zone-heading span {
  font-style: normal;
  font-size: 13px;
  font-weight: 900;
  white-space: nowrap;
}

.line-heading {
  display: grid;
  place-items: center;
  align-content: center;
  gap: 12px;
  color: #082d64;
  background: #fff;
}

.line-heading span {
  width: 46px;
  height: 46px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  color: #fff;
  background: linear-gradient(180deg, #083c84, #062d67);
  box-shadow: 0 7px 15px rgba(4, 37, 88, 0.22);
  font-size: 22px;
  font-weight: 950;
}

.line-heading strong {
  font-size: 16px;
  font-weight: 950;
}

.station-slot {
  position: relative;
  min-width: 0;
  display: grid;
  place-items: center;
  padding: 14px;
}

.equipment-node {
  position: relative;
  z-index: 1;
  width: 108px;
  min-height: 132px;
  padding: 14px 12px 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 9px;
  border: 1.5px solid #d8e4f0;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 8px 18px rgba(26, 53, 88, 0.06);
  cursor: pointer;
}

.equipment-node:hover,
.equipment-node:focus-visible {
  border-color: #0f75d8;
  box-shadow: 0 0 0 3px rgba(15, 117, 216, 0.13), 0 8px 18px rgba(26, 53, 88, 0.06);
  outline: none;
}

.equipment-node strong {
  font-size: 13px;
  color: #09295a;
  line-height: 1.2;
  font-weight: 950;
}

.node-icon {
  width: 60px;
  height: 46px;
  display: grid;
  place-items: center;
  font-size: 36px;
  line-height: 1;
}

.node-icon svg {
  width: 100%;
  height: 100%;
  fill: none;
  stroke: currentColor;
  stroke-width: 4;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.node-status {
  min-width: 48px;
  height: 23px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0 10px;
  border-radius: 7px;
  font-size: 12px;
  font-weight: 950;
}

.conveyor-link {
  position: absolute;
  left: calc(50% + 54px);
  top: 50%;
  z-index: 2;
  width: calc(100% - 108px);
  height: 42px;
  display: grid;
  align-items: end;
  padding: 0;
  border: 0;
  background: transparent;
  transform: translateY(-50%);
  cursor: pointer;
}

.conveyor-link span {
  position: absolute;
  left: 50%;
  top: 0;
  transform: translateX(-50%);
  min-width: 58px;
  color: #58667a;
  font-size: 12px;
  font-weight: 950;
  line-height: 1;
  padding: 2px 7px;
  border-radius: 999px;
  background: #fff;
  border: 1px solid #c8d1df;
  text-align: center;
  white-space: nowrap;
}

.conveyor-track {
  position: relative;
  flex: 1;
  min-width: 58px;
  height: 26px;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  border: 2px solid #8c98a9;
  background: #f4f7fb;
}

.conveyor-link:hover .conveyor-track,
.conveyor-link:focus-visible .conveyor-track {
  border-color: #0f75d8;
  box-shadow: 0 0 0 3px rgba(15, 117, 216, 0.13);
}

.conveyor-link.running .conveyor-track {
  border-color: #e7f8ef;
  background: #e7f8ef;
}

.conveyor-link.idle .conveyor-track {
  border-color: #edf5ff;
  background: #edf5ff;
}

.conveyor-link.stop .conveyor-track {
  border-color: #fff5e2;
  background: #fff5e2;
}

.conveyor-link.alarm .conveyor-track {
  border-color: #ffecef;
  background: #ffecef;
}

.conveyor-track::before,
.conveyor-track::after {
  content: '';
  position: absolute;
  top: 50%;
  width: 7px;
  height: 24px;
  border-radius: 5px;
  background: #7d8797;
  transform: translateY(-50%);
}

.conveyor-track::before {
  left: -6px;
}

.conveyor-track::after {
  right: -6px;
}

.conveyor-track i {
  border-right: 1px solid #9aa5b5;
}

.conveyor-track i:last-child {
  border-right: 0;
}

.equipment-node.running {
  background: #f0fbf5;
  border-color: #f0fbf5;
}

.equipment-node.running .node-icon,
.side-status.running {
  color: #17b56c;
}

.equipment-node.running .node-status {
  color: #11a765;
  background: #e7f8ef;
}

.equipment-node.idle {
  background: #edf5ff;
  border-color: #edf5ff;
}

.equipment-node.idle .node-icon,
.side-status.idle {
  color: #2f6fdf;
}

.equipment-node.idle .node-status {
  color: #2f6fdf;
  background: #dfeeff;
}

.equipment-node.stop {
  background: #fff8eb;
  border-color: #fff8eb;
}

.equipment-node.stop .node-icon,
.side-status.stop {
  color: #ffae18;
}

.equipment-node.stop .node-status {
  color: #f49a00;
  background: #fff5e4;
}

.equipment-node.alarm {
  background: #fff1f2;
  border-color: #fff1f2;
}

.equipment-node.alarm .node-icon,
.side-status.alarm {
  color: #ff3030;
}

.equipment-node.alarm .node-status {
  color: #ff3030;
  background: #ffecec;
}

.equipment-node.unknown {
  background: #f4f6f9;
  border-color: #f4f6f9;
}

.equipment-node.unknown .node-icon,
.side-status.unknown {
  color: #a5afbd;
}

.equipment-node.unknown .node-status {
  color: #7d8797;
  background: #f0f3f7;
}

.table-section {
  width: 100%;
  min-height: 0;
  padding: 12px;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  gap: 8px;
  overflow: hidden;
}

.table-title {
  font-size: 20px;
  font-weight: 900;
  color: #0d386f;
}

.table-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.table-search {
  width: min(420px, 42%);
  height: 36px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 12px;
  border: 1px solid #d5e0ee;
  border-radius: 8px;
  background: #fff;
}

.table-search span {
  color: #718096;
  font-size: 14px;
  font-weight: 900;
}

.table-search input {
  flex: 1;
  min-width: 0;
  height: 100%;
  border: 0;
  outline: 0;
  color: #0f2748;
  background: transparent;
  font-size: 13px;
  font-weight: 700;
}

.table-search input::placeholder {
  color: #98a6ba;
}

.table-wrap {
  min-height: 0;
  overflow: auto;
  border: 1px solid #e4ebf5;
  border-radius: 10px;
}

table {
  width: 100%;
  min-width: 760px;
  border-collapse: collapse;
  font-size: 14px;
}

th {
  position: sticky;
  top: 0;
  z-index: 2;
  padding: 11px 10px;
  background: #f3f7fc;
  color: #38536f;
  font-weight: 900;
  text-align: center;
  border-bottom: 1px solid #e2eaf5;
}

td {
  padding: 12px 10px;
  text-align: center;
  color: #24405f;
  border-bottom: 1px solid #edf2f8;
  font-weight: 800;
}

tbody tr:hover {
  background: #f9fbff;
}

tbody tr {
  cursor: pointer;
}

.empty-row {
  height: 72px;
  color: #6d7b8f;
  font-weight: 850;
}

.status-pill,
.alarm-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 46px;
  height: 22px;
  padding: 0 9px;
  border-radius: 7px;
  font-size: 12px;
  font-weight: 900;
}

.status-pill.running,
.alarm-pill.normal {
  background: #e7faf2;
  color: #15a86d;
}

.status-pill.idle,
.status-badge.idle {
  background: #edf5ff;
  color: #2f6fdf;
}

.status-pill.stop {
  background: #fff5e2;
  color: #f0a11a;
}

.status-pill.alarm,
.status-badge.alarm {
  background: #ffecef;
  color: #f04a5d;
}

.alarm-pill.warning {
  background: #fff5e2;
  color: #f0a11a;
}

.alarm-pill.danger {
  background: #ffecef;
  color: #f04a5d;
}

.pagination {
  position: relative;
  min-height: 30px;
  display: flex;
  align-items: center;
  gap: 12px;
  color: #6d7b8f;
  font-size: 12px;
  overflow: hidden;
}

.page-buttons {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 8px;
}

.page-buttons button {
  min-width: 28px;
  height: 28px;
  border: 1px solid #d7e2f0;
  border-radius: 8px;
  background: #fff;
  color: #35516e;
  font-weight: 700;
}

.page-buttons button:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}

.page-buttons .current {
  background: #073c7b;
  color: #fff;
}

.side-panel {
  min-width: 0;
  min-height: 0;
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 12px;
  overflow: hidden;
}

.info-card {
  min-height: 0;
  padding: 14px;
}

.detail-card {
  width: 100%;
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: auto;
}

.alarm-card {
  flex: 0 0 auto;
}

.side-header h3 {
  font-size: 20px;
  font-weight: 950;
}

.side-header button {
  border: 0;
  background: transparent;
  color: #627087;
  font-size: 12px;
  font-weight: 800;
  cursor: pointer;
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
  line-height: 1;
  flex-shrink: 0;
}

.detail-header strong {
  display: block;
  font-size: 18px;
  font-weight: 950;
  color: #0d2448;
  margin-bottom: 6px;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 46px;
  height: 22px;
  padding: 0 9px;
  border-radius: 7px;
  font-size: 11px;
  font-weight: 900;
}

.status-badge.running {
  background: #e1f6ef;
  color: #12a985;
}

.status-badge.idle {
  background: #edf5ff;
  color: #2f6fdf;
}

.status-badge.stop {
  background: #fff5e2;
  color: #f0a11a;
}

.status-badge.unknown {
  background: #f0f3f7;
  color: #7d8797;
}

.info-list {
  margin: 0 0 16px;
  padding: 0;
}

.info-list > div {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 7px 0;
  font-size: 13px;
  border-bottom: 1px solid #f3f6fa;
}

.info-list dt {
  margin: 0;
  font-weight: 800;
  color: #6b7c94;
  white-space: nowrap;
}

.info-list dd {
  margin: 0;
  font-weight: 900;
  color: #0d2448;
  text-align: right;
  word-break: keep-all;
}

.equipment-summary {
  display: flex;
  align-items: center;
  margin-top: 18px;
  padding-bottom: 16px;
  border-bottom: 1px solid #edf2f8;
}

.equipment-summary h4 {
  margin-bottom: 8px;
  font-size: 17px;
  font-weight: 900;
}

.side-status {
  font-size: 12px;
  font-weight: 900;
}

.detail-list {
  margin: 18px 0 12px;
}

.detail-list div {
  display: grid;
  grid-template-columns: 90px minmax(0, 1fr);
  gap: 8px;
  padding: 5px 0;
}

.detail-list dt {
  color: #6d7b8f;
  font-size: 12px;
  font-weight: 800;
}

.detail-list dd {
  margin: 0;
  color: #263d5a;
  font-size: 12px;
  font-weight: 800;
  word-break: keep-all;
}

.sub-title {
  margin: 0 0 10px;
  font-size: 15px;
  font-weight: 950;
  color: #0d2448;
}

.inverter-control {
  position: relative;
  margin: 0 0 16px;
  padding: 12px;
  border: 1px solid #dfe8f4;
  border-radius: 10px;
  background: #f8fbff;
}

.inverter-control.locked {
  pointer-events: none;
  user-select: none;
}

.inverter-control.locked::after {
  content: '🔒 CNV-001 전용 제어 영역';
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  background: rgba(190, 205, 225, 0.78);
  color: #3a4a5e;
  font-size: 13px;
  font-weight: 950;
  letter-spacing: 0.02em;
}

.control-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.control-header h4 {
  font-size: 15px;
  font-weight: 950;
  color: #0d2448;
}

.control-header span {
  color: #7d8898;
  font-size: 11px;
  font-weight: 850;
}

.control-grid {
  display: grid;
  grid-template-columns: minmax(150px, 1.15fr) repeat(2, minmax(0, 0.85fr));
  gap: 8px;
}

.control-box {
  min-width: 0;
  padding: 10px;
  border: 1px solid #e2eaf5;
  border-radius: 8px;
  background: #fff;
}

.control-box span {
  display: block;
  margin-bottom: 6px;
  color: #6b7c94;
  font-size: 12px;
  font-weight: 850;
}

.control-box strong {
  display: block;
  margin-bottom: 8px;
  color: #0d2448;
  font-size: 16px;
  font-weight: 950;
}

.frequency-input-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.frequency-input-row input {
  width: 92px;
  min-width: 0;
  height: 42px;
  padding: 0 10px;
  border: 1px solid #d4deec;
  border-radius: 9px;
  background: #fff;
  color: #0d2448;
  font-size: 18px;
  font-weight: 900;
  outline: 0;
  box-shadow: inset 0 1px 2px rgba(35, 63, 104, 0.04);
}

.frequency-input-row input:focus {
  border-color: #0f75d8;
  box-shadow: 0 0 0 3px rgba(15, 117, 216, 0.13);
}

.frequency-input-row strong {
  margin: 0;
  color: #0d2448;
  font-size: 22px;
  line-height: 1;
}

.confirm-button {
  width: 100%;
  height: 42px;
  border: 1px solid #073c7b;
  border-radius: 16px;
  background: #073c7b;
  color: #fff;
  font-size: 18px;
  font-weight: 950;
  cursor: pointer;
}

.state-button {
  min-width: 48px;
  height: 30px;
  border: 1px solid #c8d6e8;
  border-radius: 7px;
  background: #fff;
  color: #35516e;
  font-size: 12px;
  font-weight: 900;
  cursor: pointer;
}

.state-button {
  width: 100%;
}

.state-button.active {
  color: #fff;
  background: #12a985;
  border-color: #12a985;
}

.state-button.stop.active {
  background: #fa2c45;
  border-color: #fa2c45;
}

.control-value {
  display: block;
  margin: 6px 0 8px;
  font-size: 20px;
  font-weight: 950;
  color: #0d2448;
}

.state-display {
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 6px 0 8px;
  border-radius: 7px;
  background: #f0f3f7;
  color: #627087;
  font-size: 12px;
  font-weight: 900;
}

.state-display.active {
  background: #e7f8ef;
  color: #11a765;
}

.state-display.stop.active {
  background: #ffecec;
  color: #ff3030;
}

.edit-button {
  width: 100%;
  height: 30px;
  border: 1px solid #c8d6e8;
  border-radius: 7px;
  background: #f0f5fb;
  color: #2d4a6e;
  font-size: 12px;
  font-weight: 900;
  cursor: pointer;
}

.edit-actions {
  display: flex;
  gap: 6px;
  margin-top: 8px;
}

.apply-button {
  flex: 1;
  height: 30px;
  border: 1px solid #073c7b;
  border-radius: 7px;
  background: #073c7b;
  color: #fff;
  font-size: 12px;
  font-weight: 900;
  cursor: pointer;
}

.cancel-button {
  flex: 1;
  height: 30px;
  border: 1px solid #c8d6e8;
  border-radius: 7px;
  background: #fff;
  color: #627087;
  font-size: 12px;
  font-weight: 900;
  cursor: pointer;
}

.metrics-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}

.metric-box {
  min-width: 0;
  padding: 12px 14px;
  border: 1px solid #e8edf4;
  border-radius: 10px;
  background: #f7f9fc;
}

.metric-box span {
  display: block;
  margin-bottom: 6px;
  color: #6b7c94;
  font-size: 12px;
  font-weight: 800;
}

.metric-box strong {
  color: #0d2448;
  font-size: 15px;
  font-weight: 950;
}

.recent-alarm-list {
  display: grid;
  gap: 10px;
  margin: 12px 0 0;
  padding: 0 4px 0 0;
  list-style: none;
  max-height: 136px;
  overflow-y: auto;
}

.recent-alarm-list li {
  display: grid;
  grid-template-columns: 24px minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
}

.recent-alarm-list .empty-alarm-row {
  display: block;
  padding: 8px 0;
  color: #7d8898;
  font-size: 12px;
  font-weight: 850;
}

.alarm-mark {
  width: 22px;
  height: 22px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  color: #fff;
  font-size: 13px;
  font-weight: 950;
}

.alarm-mark.warning {
  background: #ffb435;
}

.alarm-mark.danger {
  background: #ff4f63;
}

.recent-alarm-list strong {
  display: block;
  color: #243a58;
  font-size: 13px;
  font-weight: 950;
}

.recent-alarm-list small {
  color: #7d8898;
  font-size: 11px;
  font-weight: 750;
}

.recent-alarm-list em {
  min-width: 42px;
  padding: 4px 8px;
  border-radius: 6px;
  font-style: normal;
  text-align: center;
  font-size: 11px;
  font-weight: 900;
}

.recent-alarm-list em.warning {
  color: #d48600;
  background: #fff5e2;
}

.recent-alarm-list em.danger {
  color: #e13245;
  background: #ffecef;
}

@media (max-width: 1200px) {
  .page-body {
    grid-template-columns: minmax(0, 1fr) 340px;
  }

  .layout-matrix {
    grid-template-columns: 82px repeat(6, minmax(128px, 1fr));
    grid-template-rows: 86px repeat(3, minmax(136px, 1fr));
  }

  .equipment-node {
    width: 96px;
    min-height: 118px;
  }

  .conveyor-link {
    left: calc(50% + 48px);
    width: calc(100% - 96px);
  }
}

@media (max-width: 980px) {
  .equipment-page {
    height: auto;
    min-height: 100vh;
    overflow: auto;
  }

  .page-body {
    height: auto;
    grid-template-columns: 1fr;
    overflow: visible;
  }

  .left-column {
    height: auto;
    overflow: visible;
  }

  .line-layout-section,
  .table-section,
  .side-panel,
  .detail-card {
    height: auto;
    overflow: visible;
  }

  .layout-matrix {
    min-width: 980px;
    min-height: 620px;
  }

}
</style>
