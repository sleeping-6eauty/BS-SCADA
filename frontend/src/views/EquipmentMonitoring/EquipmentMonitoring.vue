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
              <span class="legend running"></span> 가동 (Running)
              <span class="legend idle"></span> 대기 (Idle)
              <span class="legend stop"></span> 정지 (Stop)
              <span class="legend unknown"></span> 알 수 없음 (Unknown)
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
              <button type="button" @click="goToEquipmentDetail">더보기 ›</button>
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

            <div v-if="isConveyorSelected" class="inverter-control">
              <div class="control-header">
                <h4>인버터 제어</h4>
                <span>API 미연결</span>
              </div>
              <div class="control-grid">
                <div class="control-box frequency-control">
                  <span>주파수</span>
                  <div class="frequency-input-row">
                    <input
                      v-model.number="frequencyDraft"
                      type="number"
                      min="0"
                      max="60"
                      step="1"
                      aria-label="컨베이어 주파수"
                    />
                    <strong>Hz</strong>
                  </div>
                  <button type="button" class="confirm-button" @click="confirmConveyorFrequency">확인</button>
                </div>
                <div class="control-box">
                  <span>회전 여부</span>
                  <button
                    type="button"
                    class="state-button"
                    :class="{ active: currentConveyorControl.rotating }"
                    @click="setConveyorRotation(true)"
                  >
                    {{ currentConveyorControl.rotating ? '회전 중' : '회전 시작' }}
                  </button>
                </div>
                <div class="control-box">
                  <span>정지 여부</span>
                  <button
                    type="button"
                    class="state-button stop"
                    :class="{ active: currentConveyorControl.stopped }"
                    @click="setConveyorRotation(false)"
                  >
                    {{ currentConveyorControl.stopped ? '정지 중' : '정지' }}
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
              <li v-for="alarm in recentAlarms" :key="alarm.id">
                <span class="alarm-mark" :class="alarm.level">!</span>
                <div>
                  <strong>{{ alarm.title }}</strong>
                  <small>{{ alarm.time }}</small>
                </div>
                <em :class="alarm.level">{{ alarm.label }}</em>
              </li>
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

const router = useRouter()
const activeView = ref('layout')
const equipmentSearch = ref('')
const currentPage = ref(1)
const rowsPerPage = 10
const mqttRealtimeTopic = 'factory/equipment/+/realtime'
const mqttBrokerUrl = import.meta.env.VITE_MQTT_URL ?? 'ws://localhost:8000'

const statusText = {
  running: '가동',
  idle: '대기',
  stop: '정지',
  unknown: '알 수 없음',
}

const alarmText = {
  normal: '정상',
  warning: '경고',
  danger: '위험',
}

const recentAlarms = [
  { id: 'alarm-1', title: '토크 이상 감지', time: '2024-05-24 10:15:32', level: 'warning', label: '경고' },
  { id: 'alarm-2', title: '오버 과열', time: '2024-05-24 09:28:16', level: 'danger', label: '위험' },
  { id: 'alarm-3', title: '통신 지연', time: '2024-05-24 09:13:07', level: 'warning', label: '경고' },
]

const layoutStatusText = {
  running: '가동',
  idle: '대기',
  stop: '정지',
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
  manufacturer: 'BS-SCADA',
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

const statusCodeMap = {
  RUN: 'running',
  RUNNING: 'running',
  IDLE: 'idle',
  WAIT: 'idle',
  STOP: 'stop',
  STOPPED: 'stop',
  UNKNOWN: 'unknown',
}

const realtimeEquipmentData = reactive({})
let mqttClient = null

// 설비 유형별 센서 데이터 라벨
const getSensorDisplayLabels = (type) => {
  if (type === 'cnv') {
    return {
      sensor1: { label: '모터 내부 온도(℃)', key: 'motor_temperature_c', decimals: 1 },
      sensor2: { label: '모터 전류(A)', key: 'motor_current_c', decimals: 1 },
      sensor3: { label: '이동 속도(m/s)', key: 'moving_speed_m_s', decimals: 2 },
    }
  }
  if (type === 'plf') {
    return {
      sensor1: { label: '모터 전류(A)', key: 'moter_current_a', decimals: 1 },
      sensor2: { label: '진공 압력(kPa)', key: 'vaccum_pressure_kpa', decimals: 1 },
      sensor3: { label: '위치 오차(mm)', key: 'position_error_mm', decimals: 2 },
    }
  }
  if (type === 'jig') {
    return {
      sensor1: { label: '클램프 압력(bar)', key: 'clamp_pressure_bar', decimals: 2 },
      sensor2: { label: '공압 압력(bar)', key: 'pneumatic_press_bar', decimals: 2 },
      sensor3: { label: '클램프 위치(mm)', key: 'clamp_position_mm', decimals: 1 },
    }
  }
  if (type === 'robot' || type === 'rob') {
    return {
      sensor1: { label: '로봇 스위블(deg)', key: 'robot_swivel', decimals: 1 },
      sensor2: { label: '로봇 수평축(mm)', key: 'robot_horizontal', decimals: 1 },
      sensor3: { label: '로봇 수직축(mm)', key: 'robot_vertical', decimals: 1 },
      sensor4: { label: '툴 오프셋 오차(mm)', key: 'tool_offset_er', decimals: 2 },
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

const goToEquipmentDetail = () => {
  router.push('/equipment-detail')
}

const goToAlarmPage = () => {
  router.push('/equipment-alarm')
}

const isConveyorSelected = computed(() => selectedEquipment.value?.layoutType === 'cnv')

const currentConveyorControl = computed(() => {
  const id = selectedEquipment.value?.equipment_id ?? selectedEquipment.value?.id
  return conveyorControls[id] ?? { frequency: 0, rotating: false, stopped: true }
})

const confirmConveyorFrequency = () => {
  const id = selectedEquipment.value?.equipment_id ?? selectedEquipment.value?.id
  if (!id || !conveyorControls[id]) return

  const nextFrequency = Number(frequencyDraft.value)
  if (!Number.isFinite(nextFrequency)) return
  conveyorControls[id].frequency = Math.min(Math.max(nextFrequency, 0), 60)
}

const setConveyorRotation = (rotating) => {
  const id = selectedEquipment.value?.equipment_id ?? selectedEquipment.value?.id
  if (!id || !conveyorControls[id]) return
  conveyorControls[id].rotating = rotating
  conveyorControls[id].stopped = !rotating
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

const handleMqttMessage = (_topic, message) => {
  try {
    const payload = JSON.parse(message.toString())
    applyRealtimePayload(payload)
  } catch (err) {
    console.warn('Failed to parse equipment realtime MQTT payload:', err)
  }
}

const connectMqtt = () => {
  mqttClient = mqtt.connect(mqttBrokerUrl, {
    reconnectPeriod: 3000,
    connectTimeout: 5000,
    clean: true,
  })

  mqttClient.on('connect', () => {
    mqttClient.subscribe(mqttRealtimeTopic, (err) => {
      if (err) console.warn('Failed to subscribe equipment realtime MQTT topic:', err)
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
      alarm: status === 'stop' ? 'danger' : status === 'idle' ? 'warning' : 'normal',
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
watch(selectedEquipment, async (newEquipment) => {
  if (!newEquipment || !newEquipment.id) return

  if (newEquipment.layoutType === 'cnv') {
    const control = conveyorControls[newEquipment.id]
    frequencyDraft.value = control?.frequency ?? 0
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
  const value = Number(latestLog.data?.[field.key])
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
  background: #ffb435;
}

.legend.stop {
  background: #ff4f63;
}

.legend.unknown {
  background: #a5afbd;
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
  border-color: #fff5e4;
  background: #fff5e4;
}

.conveyor-link.stop .conveyor-track {
  border-color: #ffecec;
  background: #ffecec;
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
  background: #fff8eb;
  border-color: #fff8eb;
}

.equipment-node.idle .node-icon,
.side-status.idle {
  color: #ffae18;
}

.equipment-node.idle .node-status {
  color: #f49a00;
  background: #fff5e4;
}

.equipment-node.stop {
  background: #fff1f2;
  border-color: #fff1f2;
}

.equipment-node.stop .node-icon,
.side-status.stop {
  color: #ff3030;
}

.equipment-node.stop .node-status {
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
.alarm-pill.warning {
  background: #fff5e2;
  color: #f0a11a;
}

.status-pill.stop,
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
  background: #fff0df;
  color: #df7922;
}

.status-badge.stop {
  background: #ffe7eb;
  color: #fa2c45;
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
  margin: 0 0 16px;
  padding: 12px;
  border: 1px solid #dfe8f4;
  border-radius: 10px;
  background: #f8fbff;
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
  padding: 0;
  list-style: none;
}

.recent-alarm-list li {
  display: grid;
  grid-template-columns: 24px minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
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
