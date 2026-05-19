<template>
  <div class="alarm-page">
    <AppTopbar active-menu="알람 관리" />

    <main class="page-body">
      <!-- Left Sidebar -->
      <aside class="sidebar">
        <section class="side-card">
          <h3>설비 선택</h3>

          <button
            v-for="item in equipmentList"
            :key="item.name"
            class="equipment-item"
            :class="{ selected: item.name === selectedEquipment }"
            @click="selectedEquipment = item.name"
          >
            <span class="equip-icon">{{ item.icon }}</span>
            <span>{{ item.name }}</span>
          </button>
        </section>

        <section class="side-card assigned-card">
          <h3>담당 설비 목록</h3>

          <button
            v-for="item in equipmentList"
            :key="`assigned-${item.name}`"
            class="equipment-item"
            :class="{ selected: item.name === selectedEquipment }"
            @click="selectedEquipment = item.name"
          >
            <span class="equip-icon">{{ item.icon }}</span>
            <span>{{ item.name }}</span>
          </button>
        </section>
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
                <select>
                  <option>일별</option>
                  <option>주별</option>
                  <option>월별</option>
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

                <polyline class="line-path" :points="linePoints" />

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
              </svg>
            </div>
          </article>

          <!-- Bar Chart -->
          <article class="panel chart-panel compact">
            <div class="panel-header">
              <div>
                <p class="panel-label">Equipment Frequency</p>
                <h2>설비별 알람 발생 빈도</h2>
              </div>

              <div class="panel-actions single">
                <select>
                  <option>최근 7일</option>
                  <option>최근 30일</option>
                </select>
              </div>
            </div>

            <div class="bar-chart-wrap">
              <svg class="bar-chart" viewBox="0 0 900 210" preserveAspectRatio="none">
                <g class="grid-lines">
                  <line
                    v-for="tick in barTicks"
                    :key="tick"
                    x1="35"
                    x2="880"
                    :y1="barY(tick)"
                    :y2="barY(tick)"
                  />
                </g>

                <g class="y-labels">
                  <text
                    v-for="tick in barTicks"
                    :key="`bar-y-${tick}`"
                    x="18"
                    :y="barY(tick) + 4"
                  >
                    {{ tick }}
                  </text>
                </g>

                <g v-for="(bar, index) in frequencyData" :key="bar.name">
                  <rect
                    class="bar-rect"
                    :x="barX(index)"
                    :y="barY(bar.value)"
                    :width="barWidth"
                    :height="barHeight(bar.value)"
                    rx="8"
                  />
                  <text
                    class="bar-value"
                    :x="barX(index) + barWidth / 2"
                    :y="barY(bar.value) - 10"
                  >
                    {{ bar.value }}
                  </text>
                  <text
                    class="bar-label"
                    :x="barX(index) + barWidth / 2"
                    y="194"
                  >
                    {{ bar.name }}
                  </text>
                </g>
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
                  <col style="width: 110px" />
                  <col style="width: 120px" />
                </colgroup>

                <thead>
                  <tr>
                    <th>발생 시간</th>
                    <th>설비명</th>
                    <th>알람 유형</th>
                    <th>심각도</th>
                    <th>상태</th>
                    <th>담당자</th>
                  </tr>
                </thead>

                <tbody>
                  <tr v-for="row in alarmRows" :key="`${row.time}-${row.equipment}`">
                    <td>{{ row.time }}</td>
                    <td>{{ row.equipment }}</td>
                    <td>{{ row.type }}</td>
                    <td>
                      <span :class="['badge', row.severity.toLowerCase()]">
                        {{ row.severity }}
                      </span>
                    </td>
                    <td>
                      <span :class="['badge', row.state === '조치중' ? 'progress' : 'done']">
                        {{ row.state }}
                      </span>
                    </td>
                    <td>{{ row.manager }}</td>
                  </tr>
                </tbody>
              </table>
            </div>

            <div class="table-footer">
              <strong>전체 134건</strong>

              <div class="pagination">
                <button>‹</button>
                <button class="active">1</button>
                <button>2</button>
                <button>3</button>
                <button>4</button>
                <button>5</button>
                <button>›</button>
              </div>

              <select>
                <option>10 / 페이지</option>
                <option>20 / 페이지</option>
              </select>
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
                <span class="badge high">High</span>
                <span class="badge progress">조치중</span>
              </div>

              <h2>Robot A1 - 모터 과열</h2>

              <dl class="summary-list">
                <div>
                  <dt>발생 시간</dt>
                  <dd>2024-05-24 10:25:33</dd>
                </div>
              </dl>

              <div class="alarm-info-box">
                <h4>알람 내용</h4>

                <dl>
                  <div>
                    <dt>발생 설명</dt>
                    <dd>모터 과열 온도가 허용 범위를 초과했습니다.</dd>
                  </div>

                  <div>
                    <dt>발생 위치</dt>
                    <dd>Zone A - Line 1</dd>
                  </div>

                  <div>
                    <dt>현재 상태</dt>
                    <dd>모터 온도: 82.4 °C (상한: 80 °C)</dd>
                  </div>

                  <div>
                    <dt>권장 조치</dt>
                    <dd>모터 냉각 확인 및 부하 점검 필요</dd>
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
              <div class="memo-section">
                <h4>메모 작성</h4>

                <textarea
                  v-model="memoInput"
                  class="memo-input"
                  placeholder="조치 내용이나 메모를 입력하세요..."
                ></textarea>

                <button @click="addMemo" class="memo-button">
                  메모 추가
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
                    <button @click="removeMemo(index)" class="memo-delete">
                      ×
                    </button>
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
import { computed, ref } from 'vue'
import AppTopbar from '@/components/AppTopbar.vue'

const selectedEquipment = ref('Robot A1')
const memoInput = ref('')
const memoList = ref([])

const addMemo = () => {
  if (memoInput.value.trim()) {
    const now = new Date()

    const timeStr = now
      .toLocaleString('ko-KR', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
      })
      .replace(/\. /g, '-')
      .replace('.', '')

    memoList.value.unshift({
      time: timeStr,
      text: memoInput.value
    })

    memoInput.value = ''
  }
}

const removeMemo = index => {
  memoList.value.splice(index, 1)
}

const equipmentList = [
  { name: 'Robot A1', icon: '⚙' },
  { name: 'Nutrunner B2', icon: '🛠' },
  { name: 'Press C1', icon: '◎' },
  { name: 'Conveyor D1', icon: '▤' },
  { name: 'Welding E1', icon: '⌘' },
  { name: 'AGV F1', icon: '▣' }
]

const trendData = [
  { date: '05-11', value: 8 },
  { date: '05-12', value: 12 },
  { date: '05-13', value: 16 },
  { date: '05-14', value: 9 },
  { date: '05-15', value: 14 },
  { date: '05-16', value: 18 },
  { date: '05-17', value: 16 },
  { date: '05-18', value: 11 },
  { date: '05-19', value: 13 },
  { date: '05-20', value: 22 },
  { date: '05-21', value: 17 },
  { date: '05-22', value: 19 },
  { date: '05-23', value: 16 },
  { date: '05-24', value: 14 }
]

const frequencyData = [
  { name: 'Robot A1', value: 28 },
  { name: 'Nutrunner B2', value: 22 },
  { name: 'Press C1', value: 16 },
  { name: 'Conveyor D1', value: 15 },
  { name: 'Robot A2', value: 12 },
  { name: 'Nutrunner B1', value: 10 },
  { name: 'AGV F1', value: 8 },
  { name: 'Welding E1', value: 7 },
  { name: 'Others', value: 6 }
]

const alarmRows = [
  {
    time: '2024-05-24 10:25:33',
    equipment: 'Robot A1',
    type: '모터 과열',
    severity: 'High',
    state: '조치중',
    manager: '김지훈'
  },
  {
    time: '2024-05-24 10:18:17',
    equipment: 'Nutrunner B2',
    type: '토크 과부하',
    severity: 'Medium',
    state: '조치중',
    manager: '이수인'
  },
  {
    time: '2024-05-24 09:31:05',
    equipment: 'Press C1',
    type: '압력 센서 이상',
    severity: 'High',
    state: '조치중',
    manager: '박준호'
  },
  {
    time: '2024-05-24 08:47:21',
    equipment: 'Conveyor D1',
    type: '장비 과부하',
    severity: 'Medium',
    state: '완료',
    manager: '최민석'
  },
  {
    time: '2024-05-24 07:22:11',
    equipment: 'Robot A2',
    type: '비전 인식',
    severity: 'Low',
    state: '완료',
    manager: '김지훈'
  },
  {
    time: '2024-05-23 16:15:44',
    equipment: 'Nutrunner B1',
    type: '진동 과다',
    severity: 'High',
    state: '완료',
    manager: '이수인'
  },
  {
    time: '2024-05-23 14:03:33',
    equipment: 'AGV F1',
    type: '배터리 경고',
    severity: 'Medium',
    state: '완료',
    manager: '박준호'
  },
  {
    time: '2024-05-23 14:11:09',
    equipment: 'Welding E1',
    type: '용접 품질 경고',
    severity: 'Low',
    state: '완료',
    manager: '최민석'
  }
]

const lineTicks = [40, 30, 20, 10, 0]
const barTicks = [40, 30, 20, 10, 0]

const lineX = index => 48 + index * (820 / (trendData.length - 1))
const lineY = value => 186 - value * 3.7

const linePoints = computed(() =>
  trendData.map((point, index) => `${lineX(index)},${lineY(point.value)}`).join(' ')
)

const barWidth = 38
const barGap = 50
const barX = index => 66 + index * (barWidth + barGap)
const barY = value => 166 - value * 3.15
const barHeight = value => 166 - barY(value)
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

.equipment-item {
  width: 100%;
  height: 46px;
  padding: 0 14px;
  border: 0;
  border-radius: 14px;
  background: transparent;
  color: rgba(255, 255, 255, 0.84);
  display: grid;
  grid-template-columns: 30px 1fr;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  font-weight: 750;
  text-align: left;
  cursor: pointer;
  transition: 0.18s ease;
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
  min-height: 320px;
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
  min-width: 860px;
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

.badge.medium {
  background: #ffedd5;
  color: #ea580c;
  border: 1px solid #fed7aa;
}

.badge.low {
  background: #dbeafe;
  color: #2563eb;
  border: 1px solid #bfdbfe;
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