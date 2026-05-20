<template>
  <div class="alarm-page">
    <AppTopbar active-menu="알람 관리" />

    <main class="page-body">
      <!-- Left Sidebar -->
      <aside class="sidebar">
        <div class="sidebar-cards">
          <section class="side-card">
            <h3>설비 선택</h3>

            <div class="equipment-list-scroll">
              <button
                v-for="item in equipmentList"
                :key="item.id"
                type="button"
                class="equipment-item"
                :class="{ selected: item.id === selectedEquipmentId }"
                @click="selectEquipment(item.id)"
              >
                <span class="equip-icon">{{ item.icon }}</span>
                <span>
                  <strong>{{ item.name }}</strong>
                  <small>ㄴ {{ item.id }}</small>
                </span>
              </button>
            </div>
          </section>

          <section class="side-card assigned-card">
            <h3>담당 설비 목록</h3>

            <div class="equipment-list-scroll">
              <button
                v-for="item in assignedEquipmentList"
                :key="`assigned-${item.id}`"
                type="button"
                class="equipment-item"
                :class="{ selected: item.id === selectedEquipmentId }"
                @click="selectEquipment(item.id)"
              >
                <span class="equip-icon">{{ item.icon }}</span>
                <span>
                  <strong>{{ item.name }}</strong>
                  <small>ㄴ {{ item.id }}</small>
                </span>
              </button>
            </div>
          </section>
        </div>
      </aside>

      <!-- Main Content -->
      <section class="content-area">
        <div class="main-column">
          <!-- Line Chart -->
          <article class="panel chart-panel">
            <div class="panel-header">
              <div>
                <p class="panel-label">Alarm Trend</p>
                <h2>알람 발생 추이</h2>
              </div>

              <div class="panel-actions">
                <select v-model="trendPeriod">
                  <option value="day">일별</option>
                  <option value="week">주별</option>
                  <option value="month">월별</option>
                </select>
                <button class="calendar-button">▣</button>
              </div>
            </div>

            <div class="line-chart-wrap">
              <svg class="line-chart" viewBox="0 0 900 230" preserveAspectRatio="none">
                <g class="grid-lines">
                  <line
                    v-for="tick in lineTicks"
                    :key="tick"
                    x1="35"
                    x2="880"
                    :y1="lineY(tick)"
                    :y2="lineY(tick)"
                  />
                </g>

                <g class="y-labels">
                  <text
                    v-for="tick in lineTicks"
                    :key="`line-y-${tick}`"
                    x="18"
                    :y="lineY(tick) + 4"
                  >
                    {{ tick }}
                  </text>
                </g>

                <polyline v-if="trendData.length" class="line-path" :points="linePoints" />

                <g v-for="(point, index) in trendData" :key="point.date">
                  <circle
                    class="line-dot"
                    :cx="lineX(index)"
                    :cy="lineY(point.value)"
                    r="5.2"
                  />
                  <text
                    class="point-value"
                    :x="lineX(index)"
                    :y="lineY(point.value) - 13"
                  >
                    {{ point.value }}
                  </text>
                  <text class="x-label" :x="lineX(index)" y="212">
                    {{ point.date }}
                  </text>
                </g>
                <text v-if="!trendData.length" class="empty-chart-text" x="450" y="116">
                  알람 발생 추이 데이터가 없습니다.
                </text>
              </svg>
            </div>
          </article>

          <!-- Table -->
          <article class="panel table-panel">
            <div class="panel-header table-title">
              <div>
                <p class="panel-label">Alarm List</p>
                <h2>알람 & 장비 목록</h2>
              </div>
            </div>

            <div class="table-wrap">
              <table>
                <colgroup>
                  <col style="width: 180px" />
                  <col style="width: 140px" />
                  <col style="width: 160px" />
                  <col style="width: 110px" />
                </colgroup>

                <thead>
                  <tr>
                    <th>발생 시간</th>
                    <th>설비명</th>
                    <th>알람 유형</th>
                    <th>상태</th>
                  </tr>
                </thead>

                <tbody>
                  <tr v-if="loading">
                    <td colspan="4">알람 목록을 불러오는 중입니다.</td>
                  </tr>
                  <tr v-else-if="alarmRows.length === 0">
                    <td colspan="4">표시할 알람이 없습니다.</td>
                  </tr>
                  <template v-else>
                    <tr
                      v-for="row in paginatedAlarmRows"
                      :key="row.id"
                      :class="{ selected: row.id === selectedAlarmId || row.alarmId === selectedAlarmId }"
                      @click="selectAlarm(row)"
                    >
                      <td>{{ row.time }}</td>
                      <td>{{ row.equipment }}</td>
                      <td>{{ row.type }}</td>
                      <td>
                        <span :class="['badge', row.stateClass]">
                          {{ row.stateLabel }}
                        </span>
                      </td>
                    </tr>
                  </template>
                </tbody>
              </table>
            </div>

            <div class="table-footer">
              <strong>전체 {{ alarmRows.length }}건</strong>

              <div class="pagination">
                <button
                  type="button"
                  :disabled="currentPage === 1"
                  @click="goToPage(currentPage - 1)"
                >
                  ‹
                </button>
                <button
                  v-for="page in paginationPages"
                  :key="page"
                  type="button"
                  :class="{ active: page === currentPage }"
                  @click="goToPage(page)"
                >
                  {{ page }}
                </button>
                <button
                  type="button"
                  :disabled="currentPage === totalPages"
                  @click="goToPage(currentPage + 1)"
                >
                  ›
                </button>
              </div>

              <span class="table-hint">{{ currentPage }} / {{ totalPages }} 페이지</span>
            </div>
          </article>
        </div>

        <!-- Right Detail Column -->
        <aside class="detail-column">
          <section class="detail-card selected-detail">
            <div class="detail-card-header">
              <div>
                <p class="panel-label">Selected Equipment</p>
                <h3>선택 설비 상세 정보</h3>
              </div>
            </div>

            <div class="detail-content">
              <div class="detail-badges">
                <span :class="['badge', detailAlarm?.stateClass || 'done']">
                  {{ detailAlarm?.stateLabel || '-' }}
                </span>
              </div>

              <h2>{{ selectedEquipmentName }} - {{ detailAlarm?.type || '알람 상세' }}</h2>

              <dl class="summary-list">
                <div>
                  <dt>발생 시간</dt>
                  <dd>{{ detailAlarm?.time || '-' }}</dd>
                </div>
              </dl>

              <div class="alarm-info-box">
                <h4>알람 및 설비 정보</h4>

                <dl>
                  <div v-for="[label, value] in detailRows" :key="label">
                    <dt>{{ label }}</dt>
                    <dd>{{ value }}</dd>
                  </div>
                </dl>
              </div>
            </div>
          </section>

          <section class="detail-card history-card">
            <div class="history-title">
              <p class="panel-label">Action Memo</p>
              <h3>조치 이력 & 메모</h3>
            </div>

            <div class="history-memo-wrap">
              <div v-if="errorMessage" class="error-box">
                {{ errorMessage }}
              </div>

              <div class="memo-section">
                <h4>메모 작성</h4>

                <textarea
                  v-model="memoInput"
                  class="memo-input"
                  placeholder="조치 내용이나 메모를 입력하세요..."
                ></textarea>

                <select v-model="selectedStatus" class="status-select">
                  <option value="">상태 선택</option>
                  <option value="OPEN">미조치</option>
                  <option value="IN_PROGRESS">조치중</option>
                  <option value="RESOLVED">완료</option>
                </select>

                <button @click="addMemo" class="memo-button" :disabled="!memoAlarmId || savingMemo">
                  {{ savingMemo ? '저장 중...' : '메모 및 상태 저장' }}
                </button>
              </div>

              <div class="memo-list" v-if="memoList.length > 0">
                <h4>메모 목록</h4>

                <div
                  v-for="(memo, index) in memoList"
                  :key="index"
                  class="memo-item"
                >
                  <div class="memo-header">
                    <span class="memo-time">{{ memo.time }}</span>
                  </div>

                  <p class="memo-text">{{ memo.text }}</p>
                </div>
              </div>

              <div class="empty-memo" v-else>
                아직 등록된 메모가 없습니다.
              </div>
            </div>
          </section>
        </aside>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import AppTopbar from '@/components/AppTopbar.vue'
import {
  getAlarmDetail,
  getAlarmLog,
  getAlarmLogsByEquipment,
  getEquipmentAlarmStatistics,
  getEquipments,
  getEquipmentNames,
  getMyEquipments,
  patchAlarmMemo,
} from '@/api/alarm.js'

const route = useRoute()
const selectedEquipmentId = ref(String(route.query.equipmentId ?? ''))
const trendPeriod = ref('day')
const selectedFrequencyLine = ref('all')
const selectedAlarmId = ref('')
const selectedStatus = ref('')
const memoInput = ref('')
const loading = ref(false)
const savingMemo = ref(false)
const errorMessage = ref('')
const currentPage = ref(1)
const pageSize = ref(10)

const trendData = ref([])
const frequencyData = ref([])
const alarmRows = ref([])
const alarmDetail = ref(null)
const equipmentList = ref([])
const assignedEquipmentList = ref([])

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

const toNumericId = (value) => {
  if (value === undefined || value === null || value === '') return ''
  const text = String(value)
  return /^\d+$/.test(text) ? text : ''
}

const normalizeLineNo = (value) => {
  if (value === undefined || value === null || value === '') return '미지정 라인'
  const text = String(value).trim()
  if (!text) return '미지정 라인'
  if (/^line\s*\d+$/i.test(text)) {
    return `Line ${text.match(/\d+/)?.[0] ?? text}`
  }
  if (/^\d+$/.test(text)) return `Line ${text}`
  return text
}

const sortLineLabel = (a, b) => {
  const aNo = Number(String(a).match(/\d+/)?.[0])
  const bNo = Number(String(b).match(/\d+/)?.[0])
  if (Number.isFinite(aNo) && Number.isFinite(bNo) && aNo !== bNo) return aNo - bNo
  return String(a).localeCompare(String(b), 'ko-KR', { numeric: true })
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

const normalizeTrend = (payload) =>
  asArray(payload).map((item) => ({
    date: String(pick(item, ['date', 'period', 'label', 'created_at', 'createdAt'])),
    value: Number(pick(item, ['count', 'alarm_count', 'alarmCount', 'value'], 0)),
  }))

const normalizeEquipment = (item) => {
  const id = String(pick(item, ['equipment_id', 'equipmentId', 'id'], ''))
  const name = String(pick(item, ['equipment_name', 'equipmentName', 'name'], id || '-'))
  return {
    id,
    name,
    lineNo: normalizeLineNo(pick(item, ['line_no', 'lineNo', 'line', 'location'], '')),
    icon: getEquipmentIcon(name || id),
  }
}

const normalizeAlarmRow = (row) => {
  const equipmentId = String(pick(row, ['equipment_id', 'equipmentId'], ''))
  const alarmId = toNumericId(pick(row, ['alarm_id', 'alarmId'], ''))
  const rowTime = pick(row, ['timestamp', 'created_at', 'createdAt', 'time'], '')
  const rowId = alarmId || String(pick(row, ['id', 'log_id', 'logId'], `${equipmentId}-${rowTime}`))
  const rawStatus = String(pick(row, ['alarm_status', 'alarmStatus', 'status'], '-'))
  return {
    id: rowId,
    alarmId,
    equipmentId,
    time: formatDateTime(rowTime),
    equipment: String(pick(row, ['equipment_name', 'equipmentName', 'equipment_id', 'equipmentId'], '-')),
    type: String(pick(row, ['alarm_type', 'alarmType', 'type'], '-')),
    state: rawStatus,
    stateLabel: getStatusLabel(rawStatus),
    stateClass: getStatusClass(rawStatus),
    manager: String(pick(row, ['user_id', 'userId', 'manager', 'username'], '-')),
    memo: String(pick(row, ['alarm_memo', 'alarmMemo', 'memo'], '')),
    raw: row,
  }
}

const selectedAlarm = computed(() =>
  alarmRows.value.find((row) => row.alarmId === selectedAlarmId.value || row.id === selectedAlarmId.value)
)

const totalPages = computed(() =>
  Math.max(1, Math.ceil(alarmRows.value.length / pageSize.value))
)

const paginatedAlarmRows = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return alarmRows.value.slice(start, start + pageSize.value)
})

const paginationPages = computed(() => {
  const maxVisible = 5
  const half = Math.floor(maxVisible / 2)
  const start = Math.max(1, Math.min(currentPage.value - half, totalPages.value - maxVisible + 1))
  const end = Math.min(totalPages.value, start + maxVisible - 1)
  return Array.from({ length: end - start + 1 }, (_, index) => start + index)
})

const goToPage = (page) => {
  currentPage.value = Math.min(Math.max(1, page), totalPages.value)
}

const clampCurrentPage = () => {
  goToPage(currentPage.value)
}

const detailAlarmSource = computed(() => {
  const detail = alarmDetail.value
  if (Array.isArray(detail)) return detail
  if (Array.isArray(detail?.alarms)) return detail.alarms
  if (detail?.alarm_log) return [detail.alarm_log]
  if (detail?.alarmLog) return [detail.alarmLog]
  return detail ? [detail] : []
})

const selectedEquipmentName = computed(() =>
  equipmentList.value.find((item) => item.id === selectedEquipmentId.value)?.name
    ?? assignedEquipmentList.value.find((item) => item.id === selectedEquipmentId.value)?.name
    ?? selectedAlarm.value?.equipment
    ?? selectedEquipmentId.value
    ?? '-'
)

const lineOptions = computed(() =>
  [...new Set(equipmentList.value.map((item) => item.lineNo).filter(Boolean))]
    .sort(sortLineLabel)
)

const detailAlarm = computed(() => {
  const raw = detailAlarmSource.value.find((alarm) => {
    const alarmId = toNumericId(pick(alarm, ['alarm_id', 'alarmId'], ''))
    return alarmId && alarmId === selectedAlarmId.value
  }) ?? detailAlarmSource.value[0]
  return raw ? normalizeAlarmRow(raw) : selectedAlarm.value
})

const memoAlarmId = computed(() =>
  toNumericId(selectedAlarm.value?.alarmId) || toNumericId(detailAlarm.value?.alarmId)
)

const equipmentDetail = computed(() =>
  alarmDetail.value?.equipment ?? alarmDetail.value?.equipmentDetail ?? alarmDetail.value?.equipment_info ?? {}
)

const detailRows = computed(() => {
  const alarm = detailAlarm.value?.raw ?? {}
  const equipment = equipmentDetail.value ?? {}
  return [
    ['알람 ID', pick(alarm, ['alarm_id', 'alarmId', 'id'], selectedAlarm.value?.alarmId ?? '-')],
    ['설비 ID', pick(alarm, ['equipment_id', 'equipmentId'], selectedEquipmentId.value ?? '-')],
    ['알람 유형', pick(alarm, ['alarm_type', 'alarmType', 'type'], detailAlarm.value?.type ?? '-')],
    ['상태', getStatusLabel(pick(alarm, ['alarm_status', 'alarmStatus', 'status'], detailAlarm.value?.state ?? '-'))],
    ['설비명', pick(equipment, ['equipment_name', 'equipmentName', 'name'], selectedEquipmentName.value)],
    ['제조사', pick(equipment, ['manufacturer', 'maker'], '-')],
    ['위치', pick(equipment, ['location', 'zone', 'line_no', 'lineNo'], '-')],
  ]
})

const memoList = computed(() => {
  const memo = pick(detailAlarm.value?.raw, ['alarm_memo', 'alarmMemo', 'memo'], '')
  if (!memo) return []
  return [{
    time: detailAlarm.value?.time ?? '-',
    text: memo,
  }]
})

const getEquipmentIcon = (name = '') => {
  const lower = String(name).toLowerCase()
  if (lower.includes('robot')) return '⚙'
  if (lower.includes('nut')) return '⌘'
  if (lower.includes('press')) return '◎'
  if (lower.includes('conveyor') || lower.includes('cnv')) return '▤'
  if (lower.includes('weld')) return '◈'
  if (lower.includes('agv')) return '▣'
  return '□'
}

const selectEquipment = (equipmentId) => {
  currentPage.value = 1
  selectedEquipmentId.value = equipmentId
  const alarm = alarmRows.value.find((row) => row.equipmentId === equipmentId)
  selectedAlarmId.value = alarm?.alarmId || ''
}

const selectAlarm = (row) => {
  selectedEquipmentId.value = row.equipmentId
  selectedAlarmId.value = row.alarmId
  selectedStatus.value = normalizeStatusValue(row.state)
  memoInput.value = row.memo
}

const loadTrend = async () => {
  if (!selectedEquipmentId.value) {
    trendData.value = []
    return
  }
  trendData.value = normalizeTrend(await getEquipmentAlarmStatistics(selectedEquipmentId.value, trendPeriod.value))
}

const loadEquipmentLists = async () => {
  const [allEquipments, assignedEquipments] = await Promise.all([
    getEquipments().catch(() => getEquipmentNames()),
    getMyEquipments(),
  ])
  equipmentList.value = asArray(allEquipments).map(normalizeEquipment).filter((item) => item.id)
  assignedEquipmentList.value = asArray(assignedEquipments).map(normalizeEquipment).filter((item) => item.id)
}

const loadCounts = async () => {
  const payload = await getAlarmLog()
  const allRows = asArray(payload).map(normalizeAlarmRow)
  const countsByEquipment = allRows.reduce((counts, row) => {
    if (!row.equipmentId) return counts
    counts.set(row.equipmentId, (counts.get(row.equipmentId) ?? 0) + 1)
    return counts
  }, new Map())

  const equipmentTargets = equipmentList.value.length
    ? equipmentList.value
    : [...new Set(allRows.map((row) => row.equipmentId).filter(Boolean))]
      .map((id) => ({ id, name: id, lineNo: '미지정 라인' }))

  frequencyData.value = equipmentTargets
    .filter((item) => selectedFrequencyLine.value === 'all' || item.lineNo === selectedFrequencyLine.value)
    .map((item) => ({
      id: item.id,
      name: item.name || item.id,
      lineNo: item.lineNo,
      value: countsByEquipment.get(item.id) ?? 0,
    }))
    .sort((a, b) => b.value - a.value || a.name.localeCompare(b.name, 'ko-KR', { numeric: true }))
}

const loadLogs = async () => {
  const payload = selectedEquipmentId.value
    ? await getAlarmLogsByEquipment(selectedEquipmentId.value)
    : await getAlarmLog()
  alarmRows.value = asArray(payload).map(normalizeAlarmRow)
  clampCurrentPage()

  const selectedAlarmExists = alarmRows.value.some((row) => row.alarmId === selectedAlarmId.value)
  if (!selectedAlarmExists) {
    const firstAlarm = alarmRows.value[0]
    selectedAlarmId.value = firstAlarm?.alarmId || ''
    selectedStatus.value = firstAlarm?.state ? normalizeStatusValue(firstAlarm.state) : ''
    memoInput.value = firstAlarm?.memo || ''
  }
}

const loadDetail = async () => {
  if (!selectedEquipmentId.value) return
  alarmDetail.value = await getAlarmDetail(selectedEquipmentId.value)
  selectedStatus.value = detailAlarm.value?.state
    ? normalizeStatusValue(detailAlarm.value.state)
    : selectedStatus.value
  memoInput.value = pick(detailAlarm.value?.raw, ['alarm_memo', 'alarmMemo', 'memo'], '')
}

const loadPageData = async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    await loadEquipmentLists()
    if (!selectedEquipmentId.value) {
      const firstEquipment = assignedEquipmentList.value[0] ?? equipmentList.value[0]
      if (firstEquipment) selectEquipment(firstEquipment.id)
    }
    await Promise.all([loadTrend(), loadLogs(), loadCounts(), loadDetail()])
  } catch (err) {
    errorMessage.value = err.message || '알람 데이터를 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

const addMemo = async () => {
  const alarmId = memoAlarmId.value
  if (!alarmId || savingMemo.value) return
  savingMemo.value = true
  errorMessage.value = ''
  try {
    const fallbackStatus = detailAlarm.value?.state || selectedAlarm.value?.state
    await patchAlarmMemo(alarmId, {
      alarm_memo: memoInput.value.trim(),
      alarm_status: selectedStatus.value
        || (fallbackStatus ? normalizeStatusValue(fallbackStatus) : undefined),
    })
    await Promise.all([loadLogs(), loadDetail()])
  } catch (err) {
    errorMessage.value = err.message || '알람 메모 저장에 실패했습니다.'
  } finally {
    savingMemo.value = false
  }
}

watch(trendPeriod, loadTrend)
watch(selectedFrequencyLine, loadCounts)
watch(selectedEquipmentId, async () => {
  currentPage.value = 1
  await Promise.all([loadTrend(), loadLogs(), loadCounts(), loadDetail()])
})

onMounted(loadPageData)

const lineMax = computed(() => Math.max(10, Math.ceil(Math.max(0, ...trendData.value.map((point) => point.value)) / 10) * 10))
const barMax = computed(() => Math.max(10, Math.ceil(Math.max(0, ...frequencyData.value.map((bar) => bar.value)) / 10) * 10))
const lineTicks = computed(() => [lineMax.value, lineMax.value * 0.75, lineMax.value * 0.5, lineMax.value * 0.25, 0].map(Math.round))
const barTicks = computed(() => [barMax.value, barMax.value * 0.75, barMax.value * 0.5, barMax.value * 0.25, 0].map(Math.round))

const lineX = (index) => 48 + index * (820 / Math.max(1, trendData.value.length - 1))
const lineY = (value) => 186 - (value / lineMax.value) * 166

const linePoints = computed(() =>
  trendData.value.map((point, index) => `${lineX(index)},${lineY(point.value)}`).join(' ')
)

const barWidth = computed(() => Math.max(26, Math.min(58, 640 / Math.max(1, frequencyData.value.length) - 18)))
const barGap = computed(() => Math.max(28, (820 - (barWidth.value * frequencyData.value.length)) / Math.max(1, frequencyData.value.length)))
const barX = (index) => 66 + index * (barWidth.value + barGap.value)
const barY = (value) => 166 - (value / barMax.value) * 146
const barHeight = (value) => 166 - barY(value)
</script>

<style scoped>
* {
  box-sizing: border-box;
}

.alarm-page {
  width: 100%;
  min-height: 100vh;
  color: #172033;
  background: #ffffff;
  font-family: 'Pretendard', 'Noto Sans KR', Arial, sans-serif;
  overflow-x: hidden;
}

/* 전체 레이아웃 */
.page-body {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  min-height: calc(100vh - 70px);
  background: #ffffff;
}

/* 사이드바 */
.sidebar {
  min-height: calc(100vh - 70px);
  padding: 20px 16px;
  background: #0f2747;
  color: #ffffff;
  border-right: 1px solid #d9e2ef;
}

.sidebar-cards {
  position: fixed;
  top: 90px;
  left: 16px;
  width: 248px;
  z-index: 5;
}

.side-card {
  padding: 16px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.12);
}

.side-card + .side-card {
  margin-top: 16px;
}

.side-card h3 {
  margin: 0 0 14px;
  font-size: 15px;
  font-weight: 850;
  letter-spacing: -0.02em;
}

.equipment-list-scroll {
  max-height: 280px;
  padding-right: 4px;
  overflow-y: auto;
}

.equipment-list-scroll::-webkit-scrollbar {
  width: 6px;
}

.equipment-list-scroll::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.28);
}

.equipment-item {
  width: 100%;
  min-height: 46px;
  padding: 8px 14px;
  border: 0;
  border-radius: 14px;
  color: rgba(255, 255, 255, 0.84);
  background: transparent;
  display: grid;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  font-weight: 750;
  text-align: left;
  cursor: pointer;
  transition: 0.18s ease;
  grid-template-columns: 30px minmax(0, 1fr);
}

.equipment-item + .equipment-item {
  margin-top: 6px;
}

.equipment-item:hover {
  background: rgba(255, 255, 255, 0.09);
}

.equipment-item.selected {
  background: #ffffff;
  color: #0f2747;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.16);
}

.equipment-item strong {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.equipment-item small {
  display: block;
  margin-top: 3px;
  color: rgba(255, 255, 255, 0.58);
  font-size: 12px;
  font-weight: 750;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.equipment-item.selected small {
  color: #64748b;
}

.equip-icon {
  width: 30px;
  height: 30px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.12);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 17px;
}

.equipment-item.selected .equip-icon {
  background: #eaf2ff;
}

.assigned-card {
  min-height: 0;
}

/* 본문 */
.content-area {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 420px;
  align-items: start;
  gap: 22px;
  padding: 24px;
  background: #ffffff;
}

.main-column {
  display: flex;
  flex-direction: column;
  gap: 22px;
  min-width: 0;
}

.detail-column {
  display: flex;
  flex-direction: column;
  gap: 22px;
  position: sticky;
  top: 24px;
}

/* 공통 카드 */
.panel,
.detail-card {
  background: #ffffff;
  border: 1px solid #e5eaf2;
  border-radius: 22px;
  box-shadow: 0 12px 32px rgba(15, 39, 71, 0.08);
}

.panel {
  padding: 22px 26px;
  overflow: hidden;
}

.chart-panel {
  min-height: 320px;
}

.chart-panel.compact {
  min-height: 290px;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.panel-label {
  margin: 0 0 5px;
  font-size: 12px;
  font-weight: 850;
  color: #2563eb;
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.panel-header h2,
.detail-card h3,
.history-title h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 900;
  color: #172033;
  letter-spacing: -0.035em;
}

.panel-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.panel-actions select,
.table-footer select {
  height: 38px;
  min-width: 124px;
  padding: 0 34px 0 12px;
  border: 1px solid #d8e0ec;
  border-radius: 11px;
  background: #ffffff;
  color: #334155;
  font-size: 13px;
  font-weight: 750;
  outline: none;
}

.calendar-button {
  width: 40px;
  height: 38px;
  border: 1px solid #d8e0ec;
  border-radius: 11px;
  background: #f8fafc;
  color: #1d4ed8;
  font-size: 15px;
  cursor: pointer;
}

/* 차트 */
.line-chart-wrap {
  height: 230px;
}

.bar-chart-wrap {
  height: 210px;
}

.line-chart,
.bar-chart {
  width: 100%;
  height: 100%;
  display: block;
}

.grid-lines line {
  stroke: #e8edf5;
  stroke-width: 1;
}

.y-labels text,
.x-label,
.bar-label {
  fill: #64748b;
  font-size: 12px;
  font-weight: 700;
}

.line-path {
  fill: none;
  stroke: #2563eb;
  stroke-width: 3.2;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.line-dot {
  fill: #2563eb;
  stroke: #ffffff;
  stroke-width: 2;
}

.point-value,
.bar-value {
  fill: #172033;
  font-size: 12px;
  font-weight: 850;
  text-anchor: middle;
}

.empty-chart-text {
  fill: #64748b;
  font-size: 14px;
  font-weight: 800;
  text-anchor: middle;
}

.x-label,
.bar-label {
  text-anchor: middle;
}

.bar-rect {
  fill: #38bdf8;
  opacity: 0.95;
}

/* 테이블 */
.table-panel {
  min-height: 460px;
  display: flex;
  flex-direction: column;
}

.table-title {
  margin-bottom: 12px;
}

.table-wrap {
  flex: 1;
  border: 1px solid #e5eaf2;
  border-radius: 18px;
  overflow-x: auto;
  overflow-y: visible;
  background: #ffffff;
}

table {
  width: 100%;
  min-width: 720px;
  border-collapse: separate;
  border-spacing: 0;
  table-layout: auto;
  font-size: 14px;
}

th,
td {
  height: 50px;
  padding: 0 16px;
  border-bottom: 1px solid #edf1f7;
  text-align: center;
  white-space: nowrap;
  vertical-align: middle;
}

thead th {
  background: #f8fafc;
  color: #334155;
  font-size: 13px;
  font-weight: 850;
}

tbody tr {
  background: #ffffff;
}

tbody tr:nth-child(even) {
  background: #fbfdff;
}

tbody tr:hover {
  background: #f1f7ff;
}

tbody tr.selected {
  background: #eaf2ff;
}

tbody tr:last-child td {
  border-bottom: 0;
}

/* 배지 */
.badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 64px;
  height: 27px;
  padding: 0 12px;
  border-radius: 999px;
  font-size: 12px;
  line-height: 27px;
  font-weight: 850;
}

.badge.high {
  background: #fee2e2;
  color: #dc2626;
  border: 1px solid #fecaca;
}

.badge.progress {
  background: #fff7ed;
  color: #f97316;
  border: 1px solid #fed7aa;
}

.badge.done {
  background: #dcfce7;
  color: #15803d;
  border: 1px solid #bbf7d0;
}

.table-footer {
  display: grid;
  grid-template-columns: 140px minmax(0, 1fr) 130px;
  align-items: center;
  gap: 12px;
  margin-top: 14px;
  font-size: 14px;
  color: #475569;
}

.table-footer strong {
  font-weight: 850;
  color: #172033;
}

.table-hint {
  justify-self: end;
  font-size: 13px;
  font-weight: 800;
  color: #64748b;
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 9px;
}

.pagination button {
  width: 34px;
  height: 34px;
  border: 1px solid transparent;
  border-radius: 11px;
  background: transparent;
  color: #475569;
  font-size: 14px;
  font-weight: 850;
  cursor: pointer;
}

.pagination button:first-child,
.pagination button:last-child {
  border-color: #d8e0ec;
  background: #ffffff;
  color: #1d4ed8;
}

.pagination button.active {
  background: #1d4ed8;
  color: #ffffff;
  box-shadow: 0 6px 14px rgba(29, 78, 216, 0.24);
}

.pagination button:disabled {
  color: #94a3b8;
  background: #f8fafc;
  border-color: #e2e8f0;
  cursor: not-allowed;
  box-shadow: none;
}

/* 오른쪽 상세 영역 */
.detail-card {
  overflow: hidden;
}

.detail-card-header {
  min-height: 70px;
  padding: 18px 24px;
  display: flex;
  align-items: center;
  border-bottom: 1px solid #e5eaf2;
  background: #f8fafc;
}

.detail-content {
  padding: 22px 24px;
  overflow: visible;
}

.selected-detail {
  min-height: 380px;
}

.detail-badges {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
}

.detail-content h2 {
  margin: 0 0 18px;
  font-size: 23px;
  font-weight: 900;
  color: #111827;
  letter-spacing: -0.045em;
}

.summary-list,
.alarm-info-box dl {
  margin: 0;
}

.summary-list div,
.alarm-info-box dl div {
  display: grid;
  grid-template-columns: 92px minmax(0, 1fr);
  gap: 12px;
  align-items: start;
}

.summary-list dt,
.alarm-info-box dt {
  font-size: 13px;
  font-weight: 850;
  color: #64748b;
}

.summary-list dd,
.alarm-info-box dd {
  margin: 0;
  font-size: 14px;
  font-weight: 700;
  color: #273449;
  line-height: 1.5;
  word-break: keep-all;
}

.alarm-info-box {
  margin-top: 20px;
  padding: 18px;
  border: 1px solid #e5eaf2;
  border-radius: 18px;
  background: #fbfdff;
}

.alarm-info-box h4 {
  margin: 0 0 16px;
  font-size: 16px;
  font-weight: 900;
  color: #172033;
}

.alarm-info-box dl div + div {
  margin-top: 15px;
}

/* 메모 카드 */
.history-card {
  min-height: 390px;
  padding: 22px 24px;
}

.history-title {
  margin-bottom: 16px;
}

.history-memo-wrap {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.memo-section {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.memo-section h4,
.memo-list h4 {
  margin: 0;
  font-size: 14px;
  font-weight: 850;
  color: #273449;
}

.memo-input {
  width: 100%;
  height: 84px;
  padding: 13px;
  border: 1px solid #d8e0ec;
  border-radius: 16px;
  background: #ffffff;
  color: #273449;
  font-size: 13px;
  font-family: inherit;
  resize: none;
  outline: none;
}

.memo-input:focus {
  border-color: #2563eb;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.12);
}

.memo-input::placeholder {
  color: #94a3b8;
}

.status-select {
  height: 40px;
  padding: 0 12px;
  border: 1px solid #d8e0ec;
  border-radius: 13px;
  background: #ffffff;
  color: #273449;
  font-size: 13px;
  font-weight: 800;
  outline: none;
}

.error-box {
  padding: 12px 14px;
  border: 1px solid #fecaca;
  border-radius: 14px;
  background: #fef2f2;
  color: #b91c1c;
  font-size: 13px;
  font-weight: 800;
}

.memo-button {
  height: 40px;
  border: 0;
  border-radius: 13px;
  background: #1d4ed8;
  color: #ffffff;
  font-size: 13px;
  font-weight: 850;
  cursor: pointer;
  transition: 0.18s ease;
}

.memo-button:hover {
  background: #1e40af;
}

.memo-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.memo-item {
  padding: 13px;
  border: 1px solid #e5eaf2;
  border-radius: 16px;
  background: #fbfdff;
}

.memo-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 7px;
}

.memo-time {
  font-size: 12px;
  font-weight: 750;
  color: #64748b;
}

.memo-delete {
  width: 25px;
  height: 25px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: #94a3b8;
  font-size: 18px;
  font-weight: 700;
  cursor: pointer;
}

.memo-delete:hover {
  background: #fee2e2;
  color: #dc2626;
}

.memo-text {
  margin: 0;
  font-size: 13px;
  font-weight: 650;
  color: #273449;
  line-height: 1.45;
  word-break: break-word;
}

.empty-memo {
  padding: 18px;
  border: 1px dashed #cbd5e1;
  border-radius: 16px;
  background: #f8fafc;
  color: #64748b;
  font-size: 13px;
  font-weight: 700;
  text-align: center;
}

/* 반응형 */
@media (max-width: 1500px) {
  .page-body {
    grid-template-columns: 260px minmax(0, 1fr);
  }

  .sidebar-cards {
    width: 228px;
  }

  .content-area {
    grid-template-columns: minmax(0, 1fr) 380px;
    gap: 18px;
    padding: 20px;
  }

  .panel {
    padding: 20px 22px;
  }
}

@media (max-width: 1200px) {
  .page-body {
    grid-template-columns: 1fr;
  }

  .sidebar {
    display: none;
  }

  .sidebar-cards {
    display: none;
  }

  .content-area {
    grid-template-columns: 1fr;
  }

  .detail-column {
    position: static;
  }
}

@media (max-width: 760px) {
  .content-area {
    padding: 14px;
  }

  .panel-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .table-footer {
    grid-template-columns: 1fr;
  }

  .pagination {
    justify-content: flex-start;
    flex-wrap: wrap;
  }
}
</style>
