<template>
  <div class="equipment-page">
    <AppTopbar activeMenu="설비 현황" />

    <main class="page-body">
      <section class="left-column">
        <section class="content-panel line-layout-section">
          <div class="panel-header">
            <div class="title-wrap">
              <div class="section-icon">⚙</div>
              <h2>라인 레이아웃 <span>(차체 용접 공정)</span></h2>
            </div>

            <div class="legend-wrap">
              <span class="legend running"></span> 가동 (Running)
              <span class="legend idle"></span> 대기 (Idle)
              <span class="legend stop"></span> 정지 (Stop)
              <span class="legend unknown"></span> 알 수 없음 (Unknown)
              <select class="filter-select">
                <option>전체</option>
                <option>Zone A</option>
                <option>Zone B</option>
                <option>Zone C</option>
              </select>
            </div>
          </div>

          <div class="layout-card">
            <div class="layout-grid">
              <button
                v-for="item in layoutItems"
                :key="item.id"
                class="equipment-node"
                :class="item.status"
                @click="selectedEquipment = item"
              >
                <div class="node-top">
                  <span class="node-zone">{{ item.zone }}</span>
                  <span class="node-line">{{ item.line }}</span>
                </div>
                <div class="node-icon">{{ item.type === 'robot' ? '🦾' : '♙' }}</div>
                <strong>{{ item.name }}</strong>
              </button>
            </div>
          </div>
        </section>

        <section class="content-panel table-section">
          <div class="table-title">설비 현황 목록</div>

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
                  <th>마지막 업데이트</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="row in equipmentRows" :key="row.id">
                  <td>{{ row.name }}</td>
                  <td>{{ row.line }}</td>
                  <td>{{ row.typeName }}</td>
                  <td><span class="status-pill" :class="row.status">{{ statusText[row.status] }}</span></td>
                  <td><span class="alarm-pill" :class="row.alarm">{{ alarmText[row.alarm] }}</span></td>
                  <td>{{ row.runningTime ?? '-' }}</td>
                  <td>{{ row.updatedAt }}</td>
                </tr>
              </tbody>
            </table>
          </div>

          <div class="pagination">
            <span>전체 {{ equipmentRows.length }}건</span>
            <select>
              <option>10개씩</option>
              <option>20개씩</option>
            </select>
            <div class="page-buttons">
              <button>‹</button>
              <button class="current">1</button>
              <button>2</button>
              <button>3</button>
              <button>4</button>
              <button>5</button>
              <button>...</button>
              <button>›</button>
            </div>
          </div>
        </section>
      </section>

      <aside class="side-panel">
        <section class="info-card detail-card">
          <div class="side-header">
            <h3>설비 상세 정보</h3>
            <button>더보기 ›</button>
          </div>

          <div class="equipment-summary">
            <div class="summary-icon">{{ selectedEquipment?.type === 'robot' ? '🦾' : '♙' }}</div>
            <div>
              <h4>{{ selectedEquipment?.name ?? '-' }}</h4>
              <span class="side-status" :class="selectedEquipment?.status">
                ● {{ statusText[selectedEquipment?.status] ?? '알 수 없음' }}
              </span>
            </div>
          </div>

          <dl class="detail-list">
            <div><dt>제조사</dt><dd>{{ currentEquipmentDetail.manufacturer ?? '-' }}</dd></div>
            <div><dt>설비 ID</dt><dd>{{ currentEquipmentDetail.equipment_id ?? '-' }}</dd></div>
            <div><dt>설비 위치</dt><dd>{{ currentEquipmentDetail.zone }} - {{ currentEquipmentDetail.line_no }}</dd></div>
            <div><dt>설비 유형</dt><dd>{{ selectedEquipment?.type === 'robot' ? '용접 로봇' : '너트러너' }}</dd></div>
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
        </section>
      </aside>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { fetchEquipments, fetchLatestLog, fetchEquipmentRunningTime } from '../../api/mockEquipmentApi'
import AppTopbar from '../../components/AppTopbar.vue'

const statusText = {
  running: '가동',
  idle: '대기',
  stop: '정지',
  unknown: '알 수 없음'
}

const alarmText = {
  normal: '정상',
  warning: '경고',
  danger: '위험'
}

// 설비별 센서 데이터 타입 (로봇 vs 너트러너)
const getSensorDisplayLabels = (type) => {
  if (type === 'robot') {
    return {
      sensor1: { label: '용접전압(DC)', key: 'weld_voltage_dc', unit: 'V' },
      sensor2: { label: '용접전류(DC)', key: 'weld_current_dc', unit: 'A' },
      sensor3: { label: '용접속도', key: 'weld_speed', unit: 'm/s' },
      sensor4: { label: '생산 수량', key: 'production_count', unit: 'EA' },
    }
  } else if (type === 'nutrunner') {
    return {
      sensor1: { label: '전압(AC)', key: 'weld_voltage_ac', unit: 'V' },
      sensor2: { label: '전류(AC)', key: 'weld_current_ac', unit: 'A' },
      sensor3: { label: '토크', key: 'cycle_time', unit: 'Nm' },
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
const selectedEquipment = ref({ id: null, name: '설비 선택 대기', status: 'unknown', type: 'robot', zone: '', line: '' })
const latestLog = reactive({
  data: null,
  loading: false,
})
const runningTime = ref('00:00:00')

// 각 설비별 로그 데이터 캐시
const equipmentLogCache = reactive({})

// 테이블 데이터 (선택된 설비가 변경될 때 업데이트됨)
const equipmentRows = computed(() => {
  if (!equipment.list.length) return []
  
  return equipment.list.map((eq) => {
    const cachedLog = equipmentLogCache[eq.equipment_id] || latestLog.data
    const status = cachedLog?.status || 'unknown'
    
    return {
      id: eq.equipment_id,
      name: eq.equipment_name,
      line: `${eq.zone} - ${eq.line_no}`,
      typeName: eq.type === 'robot' ? '용접 로봇' : '너트러너',
      status: status,
      alarm: status === 'stop' ? 'danger' : status === 'idle' ? 'warning' : 'normal',
      runningTime: runningTime.value,
      updatedAt: cachedLog?.timestamp || '2024-05-24 10:30:45'
    }
  })
})

// 초기 데이터 로드
onMounted(async () => {
  try {
    equipment.loading = true
    const equipmentsData = await fetchEquipments()
    equipment.list = equipmentsData
    
    // 레이아웃 아이템 구성 및 각 설비의 로그 데이터 로드
    layoutItems.value = await Promise.all(
      equipmentsData.map(async (eq) => {
        // 각 설비의 로그 데이터 로드 및 캐시
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
    
    // 첫 번째 설비 선택
    if (layoutItems.value.length > 0) {
      selectedEquipment.value = layoutItems.value[0]
    }
  } catch (err) {
    equipment.error = err.message
    console.error('Failed to fetch equipment list:', err)
  } finally {
    equipment.loading = false
  }
})

// 선택된 설비가 변경될 때 로그 데이터 로드
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
  return equipDetail || {}
})

// 현재 설비의 센서 데이터 레이블
const currentSensorLabels = computed(() => {
  return getSensorDisplayLabels(selectedEquipment.value?.type)
})

// 현재 설비의 특정 센서 데이터
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
  font-family: Pretendard, Inter, system-ui, -apple-system, BlinkMacSystemFont, sans-serif;
  display: flex;
  flex-direction: column;
}

.page-body {
  height: calc(100vh - 70px);
  display: grid;
  grid-template-columns: minmax(0, 1fr) 330px;
  gap: 12px;
  padding: 10px 12px 12px;
  overflow: hidden;
}

.left-column {
  min-width: 0;
  min-height: 0;
  height: 100%;
  display: grid;
  grid-template-rows: minmax(0, 1fr) auto;
  gap: 12px;
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
  font-size: 15px;
  font-weight: 800;
  white-space: nowrap;
}

h2 span {
  color: #52647a;
  font-weight: 700;
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
  padding: 14px;
  border: 1px solid #e1e8f2;
  border-radius: 18px;
  overflow: hidden;
}

.layout-grid {
  height: 100%;
  display: grid;
  grid-template-columns: repeat(4, minmax(150px, 1fr));
  grid-template-rows: repeat(2, minmax(130px, 1fr));
  gap: 14px;
  align-items: stretch;
}

.equipment-node {
  min-width: 0;
  min-height: 0;
  position: relative;
  z-index: 1;
  padding: 16px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
  border: 1.5px solid #d8e4f0;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 8px 22px rgba(22, 56, 99, 0.08);
  cursor: pointer;
  overflow: hidden;
}

.equipment-node strong {
  font-size: 15px;
  color: #173251;
  line-height: 1.2;
}

.node-top {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.node-zone,
.node-line {
  font-size: 12px;
  font-weight: 800;
  color: #5a6c89;
}

.node-line {
  color: #10243f;
}

.node-icon {
  font-size: clamp(24px, 2.4vw, 32px);
  line-height: 1;
  align-self: center;
  margin-top: 4px;
}

.equipment-node.running {
  border-color: #7fe0b8;
}

.equipment-node.running .node-icon,
.side-status.running {
  color: #20c985;
}

.equipment-node.idle {
  border-color: #ffc46b;
}

.equipment-node.idle .node-icon,
.side-status.idle {
  color: #ffb435;
}

.equipment-node.stop {
  border-color: #ff8a97;
}

.equipment-node.stop .node-icon,
.side-status.stop {
  color: #ff4f63;
}

.table-section {
  min-height: 0;
  padding: 12px;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  gap: 8px;
  overflow: hidden;
}

.table-title {
  font-size: 15px;
  font-weight: 900;
  color: #0d386f;
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
  font-size: 12px;
}

th {
  position: sticky;
  top: 0;
  z-index: 2;
  padding: 8px;
  background: #f3f7fc;
  color: #38536f;
  font-weight: 800;
  text-align: center;
  border-bottom: 1px solid #e2eaf5;
}

td {
  padding: 7px 8px;
  text-align: center;
  color: #24405f;
  border-bottom: 1px solid #edf2f8;
}

tbody tr:hover {
  background: #f9fbff;
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
  font-size: 11px;
  font-weight: 800;
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
  min-height: 30px;
  display: flex;
  align-items: center;
  gap: 12px;
  color: #6d7b8f;
  font-size: 12px;
  overflow: hidden;
}

.page-buttons {
  margin-left: auto;
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
  overflow: hidden;
}

.side-header h3 {
  font-size: 16px;
  font-weight: 900;
}

.side-header button {
  border: 0;
  background: transparent;
  color: #627087;
  font-size: 12px;
  font-weight: 800;
  cursor: pointer;
}

.equipment-summary {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-top: 18px;
  padding-bottom: 16px;
  border-bottom: 1px solid #edf2f8;
}

.summary-icon {
  width: 46px;
  height: 46px;
  flex: 0 0 46px;
  display: grid;
  place-items: center;
  border-radius: 10px;
  background: #f1f6fc;
  font-size: 24px;
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
  font-size: 14px;
  font-weight: 900;
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

@media (max-width: 1200px) {
  .page-body {
    grid-template-columns: minmax(0, 1fr) 300px;
  }

  .layout-grid {
    grid-template-columns: repeat(3, minmax(150px, 1fr));
    grid-template-rows: repeat(3, minmax(110px, 1fr));
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

  .layout-grid {
    grid-template-columns: repeat(2, minmax(150px, 1fr));
    grid-template-rows: none;
    grid-auto-rows: 150px;
  }

  .topbar {
    overflow-x: auto;
  }
}
</style>
