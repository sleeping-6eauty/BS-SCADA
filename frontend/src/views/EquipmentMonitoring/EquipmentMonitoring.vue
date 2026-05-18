<template>
  <div class="equipment-page">
    <AppTopbar active-menu="설비 현황" />

    <main class="page-body">
      <nav class="view-tabs" aria-label="설비 현황 보기 전환">
        <button
          type="button"
          :class="{ active: activeView === 'layout' }"
          @click="activeView = 'layout'"
        >
          라인 레이아웃
        </button>
        <button
          type="button"
          :class="{ active: activeView === 'list' }"
          @click="activeView = 'list'"
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
                <span>{{ zone.name }}</span>
                <em>({{ zone.code }})</em>
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
                      <svg v-if="station.type === 'plf'" viewBox="0 0 80 58">
                        <path d="M10 44h60M16 44V24l14 8 12-14 22 16v10M22 27l36 13M16 20l48 14M30 32v12M48 30v14M64 34v10" />
                      </svg>
                      <svg v-else-if="station.type === 'jig'" viewBox="0 0 80 58">
                        <path d="M14 37h52M20 36l7-14h27l8 14M28 22l10-8h16l8 8M26 38v8M58 38v8M18 46h10M52 46h10M12 30h8M60 30h8" />
                      </svg>
                      <svg v-else-if="station.type === 'rob'" viewBox="0 0 80 58">
                        <path d="M22 45h28M29 45V34M26 34l-9 8M33 30l16-16M47 13l12 11M59 24l7-7M53 30l8 6" />
                        <circle cx="31" cy="29" r="8" />
                        <circle cx="50" cy="13" r="7" />
                      </svg>
                      <svg v-else-if="station.type === 'wld'" viewBox="0 0 80 58">
                        <path d="M20 42l24-24 12 12-24 24-12-12ZM42 20l8-8M54 18l9-3M57 27h10M52 35l7 7M35 24l11 11" />
                      </svg>
                      <svg v-else-if="station.type === 'slr'" viewBox="0 0 80 58">
                        <path d="M18 44h46M25 40h26M38 40V18M48 38V14M55 36V20M31 30c2-9 9-15 18-16M30 34h-8M58 18h6M22 36v8M64 34v10" />
                      </svg>
                      <svg v-else viewBox="0 0 80 58">
                        <path d="M18 20h44v28H18zM28 20l4-7h16l4 7" />
                        <circle cx="40" cy="34" r="10" />
                      </svg>
                    </span>
                    <strong>{{ station.label }}</strong>
                    <span class="node-status">{{ layoutStatusText[station.status] }}</span>
                  </button>

                  <div v-if="stationIndex < line.stations.length - 1" class="conveyor-link" aria-hidden="true">
                    <span>CNV</span>
                    <div class="conveyor-track">
                      <i v-for="index in 4" :key="index"></i>
                    </div>
                  </div>
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
              <input v-model="equipmentSearch" type="search" placeholder="설비명, 라인, 유형, 상태 검색" />
            </label>
          </div>

          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>설비명</th>
                  <th>라인</th>
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
        <section class="info-card detail-card">
          <template v-if="selectedEquipment">
            <div class="side-header">
              <h3>설비 상세 정보</h3>
              <button type="button" @click="goToEquipmentDetail">더보기 ›</button>
            </div>

            <div class="equipment-summary">
              <div>
                <h4>{{ selectedEquipment?.name ?? '-' }}</h4>
                <span class="side-status" :class="selectedEquipment?.status">
                  {{ statusText[selectedEquipment?.status] ?? '알 수 없음' }}
                </span>
              </div>
            </div>

            <dl class="detail-list">
              <div><dt>제조사</dt><dd>{{ currentEquipmentDetail.manufacturer ?? '-' }}</dd></div>
              <div><dt>설비 ID</dt><dd>{{ currentEquipmentDetail.equipment_id ?? '-' }}</dd></div>
              <div><dt>설비 위치</dt><dd>{{ currentEquipmentDetail.zone }} - {{ currentEquipmentDetail.line_no }}</dd></div>
              <div><dt>설비 유형</dt><dd>{{ selectedEquipmentTypeLabel }}</dd></div>
              <div><dt>마지막 업데이트</dt><dd>{{ latestLog.data?.timestamp ?? '2024-05-24 10:30:45' }}</dd></div>
            </dl>

            <h4 class="sub-title">주요 데이터</h4>
            <div class="metric-grid">
              <div class="metric-box">
                <span>가동 상태</span>
                <strong>{{ statusText[selectedEquipment?.status] ?? '-' }}</strong>
              </div>
              <div class="metric-box">
                <span>가동 시간</span>
                <strong>{{ runningTime }}</strong>
              </div>
              <div v-if="currentSensorData.sensor1" class="metric-box">
                <span>{{ currentSensorData.sensor1.label }}</span>
                <strong>{{ currentSensorData.sensor1.value.toFixed(1) }} {{ currentSensorData.sensor1.unit }}</strong>
              </div>
              <div v-if="currentSensorData.sensor2" class="metric-box">
                <span>{{ currentSensorData.sensor2.label }}</span>
                <strong>{{ currentSensorData.sensor2.value.toFixed(1) }} {{ currentSensorData.sensor2.unit }}</strong>
              </div>
              <div v-if="currentSensorData.sensor3" class="metric-box">
                <span>{{ currentSensorData.sensor3.label }}</span>
                <strong>{{ currentSensorData.sensor3.value.toFixed(1) }} {{ currentSensorData.sensor3.unit }}</strong>
              </div>
              <div v-if="currentSensorData.sensor4" class="metric-box">
                <span>{{ currentSensorData.sensor4.label }}</span>
                <strong>{{ currentSensorData.sensor4.value.toFixed(0) }} {{ currentSensorData.sensor4.unit }}</strong>
              </div>
            </div>

            <div class="recent-alarm-section">
              <div class="side-header compact">
                <h4 class="sub-title">최근 알람</h4>
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
            </div>
          </template>
        </section>
      </aside>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { fetchEquipments, fetchLatestLog, fetchEquipmentRunningTime } from '../../api/mockEquipmentApi'
import AppTopbar from '@/components/AppTopbar.vue'

const router = useRouter()
const activeView = ref('layout')
const equipmentSearch = ref('')
const currentPage = ref(1)
const rowsPerPage = 10

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
  robot: '산업용로봇',
  nutrunner: '너트러너',
}

const createStation = (lineNo, type, zone, status) => ({
  id: `${type}-${lineNo}`,
  label: `${type.toUpperCase()}-${lineNo}`,
  type,
  status: status ?? 'running',
  zone,
  line: `Line ${lineNo}`,
  manufacturer: 'BS-SCADA',
  equipment_id: `${type}-${lineNo}`,
  equipment_name: `${type.toUpperCase()}-${lineNo}`,
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
  { key: 'line-1', no: 1, label: 'Line 1', stations: createLineStations(1, { slr: 'idle' }) },
  { key: 'line-2', no: 2, label: 'Line 2', stations: createLineStations(2, { rob: 'idle' }) },
  { key: 'line-3', no: 3, label: 'Line 3', stations: createLineStations(3, { rob: 'stop' }) },
]

// 설비 유형별 센서 데이터 라벨
const getSensorDisplayLabels = (type) => {
  if (type === 'robot') {
    return {
      sensor1: { label: '현재 온도', key: 'weld_voltage_dc', unit: '℃' },
      sensor2: { label: '전류', key: 'weld_current_dc', unit: 'A' },
      sensor3: { label: '사이클 타임', key: 'weld_speed', unit: 's' },
      sensor4: { label: '생산 수량', key: 'production_count', unit: 'EA' },
    }
  } else if (type === 'nutrunner') {
    return {
      sensor1: { label: '현재 온도', key: 'weld_voltage_ac', unit: '℃' },
      sensor2: { label: '전류', key: 'weld_current_ac', unit: 'A' },
      sensor3: { label: '사이클 타임', key: 'cycle_time', unit: 's' },
      sensor4: { label: '생산 수량', key: 'production_count', unit: 'EA' },
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

const selectLayoutStation = (station) => {
  selectedEquipment.value = {
    id: station.id,
    name: station.label,
    status: station.status,
    type: station.type === 'rob' ? 'robot' : 'nutrunner',
    zone: station.zone,
    line: station.line,
    layoutType: station.type,
    manufacturer: station.manufacturer,
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
    layoutType: row.rawType === 'robot' ? 'rob' : 'jig',
  }
}

const goToEquipmentDetail = () => {
  router.push('/equipment-detail')
}

const goToAlarmPage = () => {
  router.push('/equipment-alarm')
}

// 설비별 최신 로그 데이터 캐시
const equipmentLogCache = reactive({})

// 테이블 데이터
const equipmentRows = computed(() => {
  if (!equipment.list.length) return []
  
  return equipment.list.map((eq) => {
    const cachedLog = equipmentLogCache[eq.equipment_id] || latestLog.data
    const status = cachedLog?.status || 'unknown'
    
    return {
      id: eq.equipment_id,
      name: eq.equipment_name,
      line: `${eq.zone} - ${eq.line_no}`,
      lineNo: eq.line_no,
      zone: eq.zone,
      rawType: eq.type,
      typeName: eq.type === 'robot' ? '용접 로봇' : '너트러너',
      status: status,
      alarm: status === 'stop' ? 'danger' : status === 'idle' ? 'warning' : 'normal',
      runningTime: runningTime.value,
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
  
  try {
    latestLog.loading = true
    
    // 최신 로그 데이터 로드
    const logData = await fetchLatestLog(newEquipment.id)
    latestLog.data = logData
    equipmentLogCache[newEquipment.id] = logData
    
    // 가동 시간 로드
    const timeData = await fetchEquipmentRunningTime(newEquipment.id)
    runningTime.value = timeData.running_time || '00:00:00'
    
    // 레이아웃 상태 업데이트
    const layoutItem = layoutItems.value.find(item => item.id === newEquipment.id)
    if (layoutItem) {
      layoutItem.status = logData.status
    }
  } catch (err) {
    console.error('Failed to fetch equipment data:', err)
  } finally {
    latestLog.loading = false
  }
}, { immediate: true })

// 현재 설비의 상세 정보
const currentEquipmentDetail = computed(() => {
  if (!selectedEquipment.value) return {}
  
  const equipDetail = equipment.list.find(eq => eq.equipment_id === selectedEquipment.value.id)
  return equipDetail || {
    equipment_id: selectedEquipment.value.equipment_id ?? selectedEquipment.value.id,
    equipment_name: selectedEquipment.value.equipment_name ?? selectedEquipment.value.name,
    manufacturer: selectedEquipment.value.manufacturer ?? 'BS-SCADA',
    zone: selectedEquipment.value.zone,
    line_no: selectedEquipment.value.line_no ?? selectedEquipment.value.line,
    type: selectedEquipment.value.layoutType ?? selectedEquipment.value.type,
  }
})

const selectedEquipmentTypeLabel = computed(() => {
  const type = currentEquipmentDetail.value.type ?? selectedEquipment.value?.layoutType ?? selectedEquipment.value?.type
  return equipmentTypeLabels[type] ?? equipmentTypeLabels[selectedEquipment.value?.type] ?? '-'
})

// 현재 설비의 센서 데이터 라벨
const currentSensorLabels = computed(() => {
  return getSensorDisplayLabels(selectedEquipment.value?.type)
})

// 현재 설비의 측정 센서 데이터
const currentSensorData = computed(() => {
  if (!latestLog.data) return {}
  
  const labels = currentSensorLabels.value
  const data = {}
  
  Object.entries(labels).forEach(([key, label]) => {
    data[key] = {
      label: label.label,
      value: latestLog.data[label.key] || 0,
      unit: label.unit,
    }
  })
  
  return data
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
  grid-template-columns: minmax(0, 1fr) 340px;
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

.zone-heading span,
.zone-heading em {
  font-style: normal;
  font-size: 13px;
  font-weight: 900;
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
  display: flex;
  align-items: center;
  gap: 7px;
  transform: translateY(-50%);
  pointer-events: none;
}

.conveyor-link span {
  color: #58667a;
  font-size: 12px;
  font-weight: 800;
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
  border-color: #27bd78;
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
  border-color: #ffae18;
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
  border-color: #ff3030;
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
  border-color: #a5afbd;
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
  overflow: hidden;
}

.info-card {
  min-height: 0;
  padding: 14px;
}

.detail-card {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: auto;
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

.side-header.compact {
  margin-top: 16px;
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
  margin: 18px 0 12px;
  font-size: 20px;
  font-weight: 950;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.metric-box {
  min-width: 0;
  padding: 12px;
  border: 1px solid #e3eaf4;
  border-radius: 9px;
  background: #fff;
  box-shadow: 0 3px 10px rgba(32, 57, 92, 0.04);
}

.metric-box span {
  display: block;
  margin-bottom: 5px;
  color: #7d8898;
  font-size: 11px;
  font-weight: 800;
}

.metric-box strong {
  color: #243a58;
  font-size: 15px;
  font-weight: 900;
}

.recent-alarm-section {
  margin-top: 6px;
  padding-top: 12px;
  border-top: 1px solid #edf2f8;
}

.recent-alarm-list {
  display: grid;
  gap: 10px;
  margin: 0;
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
    grid-template-columns: minmax(0, 1fr) 300px;
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
