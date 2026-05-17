<template>
  <div class="alarm-page">
    <AppTopbar activeMenu="알람 관리" />

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
          >
            <span class="equip-icon">{{ item.icon }}</span>
            <span>{{ item.name }}</span>
            <i :class="['status-dot', item.status]"></i>
          </button>

        </section>

        <section class="side-card assigned-card">
          <h3>담당 설비 목록</h3>
          <button
            v-for="item in equipmentList"
            :key="`assigned-${item.name}`"
            class="equipment-item"
            :class="{ selected: item.name === selectedEquipment }"
          >
            <span class="equip-icon">{{ item.icon }}</span>
            <span>{{ item.name }}</span>
            <i :class="['status-dot', item.status]"></i>
          </button>
        </section>
      </aside>

      <!-- Main Content -->
      <section class="content-area">
        <div class="main-column">
          <!-- Line Chart -->
          <article class="panel chart-panel">
            <div class="panel-header">
              <h2>알람 발생 추이 (건)</h2>
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
              <svg class="line-chart" viewBox="0 0 900 210" preserveAspectRatio="none">
                <g class="grid-lines">
                  <line v-for="tick in lineTicks" :key="tick" x1="35" x2="880" :y1="lineY(tick)" :y2="lineY(tick)" />
                </g>

                <g class="y-labels">
                  <text v-for="tick in lineTicks" :key="`line-y-${tick}`" x="18" :y="lineY(tick) + 4">{{ tick }}</text>
                </g>

                <polyline class="line-path" :points="linePoints" />

                <g v-for="(point, index) in trendData" :key="point.date">
                  <circle class="line-dot" :cx="lineX(index)" :cy="lineY(point.value)" r="4.8" />
                  <text class="point-value" :x="lineX(index)" :y="lineY(point.value) - 13">{{ point.value }}</text>
                  <text class="x-label" :x="lineX(index)" y="198">{{ point.date }}</text>
                </g>
              </svg>
            </div>
          </article>

          <!-- Bar Chart -->
          <article class="panel chart-panel compact">
            <div class="panel-header">
              <h2>설비별 알람 발생 빈도 (건)</h2>
              <div class="panel-actions single">
                <select>
                  <option>최근 7일</option>
                  <option>최근 30일</option>
                </select>
              </div>
            </div>

            <div class="bar-chart-wrap">
              <svg class="bar-chart" viewBox="0 0 900 180" preserveAspectRatio="none">
                <g class="grid-lines">
                  <line v-for="tick in barTicks" :key="tick" x1="35" x2="880" :y1="barY(tick)" :y2="barY(tick)" />
                </g>

                <g class="y-labels">
                  <text v-for="tick in barTicks" :key="`bar-y-${tick}`" x="18" :y="barY(tick) + 4">{{ tick }}</text>
                </g>

                <g v-for="(bar, index) in frequencyData" :key="bar.name">
                  <rect
                    class="bar-rect"
                    :x="barX(index)"
                    :y="barY(bar.value)"
                    :width="barWidth"
                    :height="barHeight(bar.value)"
                    rx="4"
                  />
                  <text class="bar-value" :x="barX(index) + barWidth / 2" :y="barY(bar.value) - 10">{{ bar.value }}</text>
                  <text class="bar-label" :x="barX(index) + barWidth / 2" y="164">{{ bar.name }}</text>
                </g>
              </svg>
            </div>
          </article>

          <!-- Table -->
          <article class="panel table-panel">
            <div class="panel-header table-title">
              <h2>알람 & 장비 목록</h2>
            </div>

            <div class="table-wrap">
              <table>
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
                      <span :class="['badge', row.severity.toLowerCase()]">{{ row.severity }}</span>
                    </td>
                    <td>
                      <span :class="['badge', row.state === '조치중' ? 'progress' : 'done']">{{ row.state }}</span>
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
              <h3>선택 설비 상세 정보</h3>
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
            <h3>조치 이력 & 메모</h3>
            <div class="history-memo-wrap">
              <div class="memo-section">
                <h4>메모 작성</h4>
                <textarea v-model="memoInput" class="memo-input" placeholder="조치 내용이나 메모를 입력하세요..."></textarea>
                <button @click="addMemo" class="memo-button">메모 추가</button>
              </div>

              <div class="memo-list" v-if="memoList.length > 0">
                <h4>메모 목록</h4>
                <div v-for="(memo, index) in memoList" :key="index" class="memo-item">
                  <div class="memo-header">
                    <span class="memo-time">{{ memo.time }}</span>
                    <button @click="removeMemo(index)" class="memo-delete">×</button>
                  </div>
                  <p class="memo-text">{{ memo.text }}</p>
                </div>
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
import AppTopbar from '../../components/AppTopbar.vue'

const selectedEquipment = ref('Robot A1')
const memoInput = ref('')
const memoList = ref([])

const addMemo = () => {
  if (memoInput.value.trim()) {
    const now = new Date()
    const timeStr = now.toLocaleString('ko-KR', { 
      year: 'numeric', 
      month: '2-digit', 
      day: '2-digit', 
      hour: '2-digit', 
      minute: '2-digit', 
      second: '2-digit' 
    }).replace(/\. /g, '-').replace('.', '')
    memoList.value.unshift({
      time: timeStr,
      text: memoInput.value
    })
    memoInput.value = ''
  }
}

const removeMemo = (index) => {
  memoList.value.splice(index, 1)
}

const equipmentList = [
  { name: 'Robot A1', icon: '⚙', status: 'green' },
  { name: 'Nutrunner B2', icon: '🛠', status: 'green' },
  { name: 'Press C1', icon: '◎', status: 'yellow' },
  { name: 'Conveyor D1', icon: '▤', status: 'green' },
  { name: 'Welding E1', icon: '⌘', status: 'red' },
  { name: 'AGV F1', icon: '▣', status: 'green' }
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
  { time: '2024-05-24 10:25:33', equipment: 'Robot A1', type: '모터 과열', severity: 'High', state: '조치중', manager: '김지훈' },
  { time: '2024-05-24 10:18:17', equipment: 'Nutrunner B2', type: '토크 과부하', severity: 'Medium', state: '조치중', manager: '이수인' },
  { time: '2024-05-24 09:31:05', equipment: 'Press C1', type: '압력 센서 이상', severity: 'High', state: '조치중', manager: '박준호' },
  { time: '2024-05-24 08:47:21', equipment: 'Conveyor D1', type: '장비 과부하', severity: 'Medium', state: '완료', manager: '최민석' },
  { time: '2024-05-24 07:22:11', equipment: 'Robot A2', type: '비전 인식', severity: 'Low', state: '완료', manager: '김지훈' },
  { time: '2024-05-23 16:15:44', equipment: 'Nutrunner B1', type: '진동 과다', severity: 'High', state: '완료', manager: '이수인' },
  { time: '2024-05-23 14:03:33', equipment: 'AGV F1', type: '배터리 경고', severity: 'Medium', state: '완료', manager: '박준호' },
  { time: '2024-05-23 14:11:09', equipment: 'Welding E1', type: '용접 품질 경고', severity: 'Low', state: '완료', manager: '최민석' }
]

const lineTicks = [40, 30, 20, 10, 0]
const barTicks = [40, 30, 20, 10, 0]

const lineX = (index) => 48 + index * (820 / (trendData.length - 1))
const lineY = (value) => 170 - value * 3.35

const linePoints = computed(() =>
  trendData.map((point, index) => `${lineX(index)},${lineY(point.value)}`).join(' ')
)

const barWidth = 34
const barGap = 54
const barX = (index) => 66 + index * (barWidth + barGap)
const barY = (value) => 138 - value * 2.7
const barHeight = (value) => 138 - barY(value)
</script>

<style scoped>
* {
  box-sizing: border-box;
}

.alarm-page {
  width: 100%;
  height: 100vh;
  color: #1f2933;
  background: #f6efe6;
  font-family: 'Pretendard', 'Noto Sans KR', Arial, sans-serif;
  overflow: hidden;
}

.topbar {
  height: 66px;
  padding: 0 26px;
  display: flex;
  align-items: center;
  background: linear-gradient(90deg, #002b5f 0%, #003d78 45%, #062f62 100%);
  color: #fff;
  box-shadow: 0 4px 18px rgba(0, 31, 73, 0.25);
}

.brand {
  width: 92px;
  display: flex;
  align-items: center;
}

.brand-icon {
  width: 42px;
  height: 42px;
  border: 2px solid rgba(255, 255, 255, 0.85);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 27px;
  font-weight: 800;
}

.top-menu {
  height: 100%;
  display: flex;
  align-items: center;
  gap: 64px;
  flex: 1;
}

.top-menu button {
  height: 100%;
  border: 0;
  background: transparent;
  color: rgba(255, 255, 255, 0.88);
  font-size: 18px;
  font-weight: 800;
  cursor: pointer;
  position: relative;
}

.top-menu button.active {
  color: #fff;
}

.top-menu button.active::after {
  content: '';
  position: absolute;
  left: 50%;
  bottom: 0;
  transform: translateX(-50%);
  width: 128px;
  height: 4px;
  border-radius: 999px 999px 0 0;
  background: #69c9ff;
  box-shadow: 0 0 14px rgba(105, 201, 255, 0.7);
}

.top-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 18px;
  font-weight: 800;
}

.time-icon,
.user-icon {
  font-size: 24px;
}

.current-time {
  margin-right: 36px;
}

.bell {
  margin-left: 28px;
  position: relative;
  font-size: 23px;
}

.bell span {
  position: absolute;
  right: -8px;
  top: -11px;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 999px;
  background: #ff2e2e;
  color: #fff;
  font-size: 11px;
  line-height: 18px;
  text-align: center;
}

.page-body {
  display: grid;
  grid-template-columns: 312px 1fr;
  height: calc(100vh - 70px);
  min-height: 0;
  overflow: hidden;
}

.sidebar {
  height: 100%;
  min-height: 0;
  padding: 14px;
  overflow: hidden;
  background: linear-gradient(180deg, #00376e 0%, #00264d 100%);
  color: #fff;
}

.side-card {
  padding: 14px 14px;
  border: 1px solid rgba(255, 255, 255, 0.11);
  border-radius: 8px;
  background: rgba(0, 32, 72, 0.42);
  box-shadow: inset 0 0 24px rgba(16, 97, 171, 0.2);
}

.side-card + .side-card {
  margin-top: 12px;
}

.side-card h3 {
  margin: 0 0 12px;
  font-size: 15px;
  font-weight: 900;
}

.equipment-item {
  width: 100%;
  height: 36px;
  padding: 0 14px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: rgba(255, 255, 255, 0.9);
  display: grid;
  grid-template-columns: 28px 1fr 12px;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 800;
  text-align: left;
  cursor: pointer;
}

.equipment-item.selected {
  background: linear-gradient(90deg, #0f5ea6 0%, #095293 100%);
}

.equip-icon {
  font-size: 20px;
  opacity: 0.9;
}

.status-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  justify-self: end;
}

.status-dot.green {
  background: #00c989;
}

.status-dot.yellow {
  background: #ffb02e;
}

.status-dot.red {
  background: #ff3b30;
}

.side-divider {
  height: 1px;
  margin: 14px -4px;
  background: rgba(255, 255, 255, 0.16);
}

.range-buttons {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  margin-bottom: 20px;
}

.range-buttons button {
  height: 38px;
  border: 1px solid rgba(255, 255, 255, 0.17);
  border-radius: 7px;
  background: #043a75;
  color: rgba(255, 255, 255, 0.85);
  font-size: 14px;
  font-weight: 800;
  cursor: pointer;
}

.range-buttons button.selected {
  background: #137bd3;
  color: #fff;
}

.auto-refresh {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  font-size: 15px;
  font-weight: 900;
}

.switch input {
  display: none;
}

.switch span {
  display: block;
  width: 46px;
  height: 26px;
  border-radius: 999px;
  background: #10436f;
  position: relative;
  cursor: pointer;
}

.switch span::after {
  content: '';
  position: absolute;
  top: 3px;
  left: 3px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: #fff;
  transition: 0.2s;
}

.switch input:checked + span {
  background: #1088e8;
}

.switch input:checked + span::after {
  left: 23px;
}

.setting-button {
  width: 100%;
  height: 48px;
  padding: 0 16px;
  border: 1px solid rgba(255, 255, 255, 0.13);
  border-radius: 8px;
  background: rgba(0, 35, 76, 0.38);
  color: rgba(255, 255, 255, 0.92);
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 15px;
  font-weight: 800;
  cursor: pointer;
}

.assigned-card {
  min-height: 0;
  height: calc(100vh - 66px - 380px);
}

.content-area {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 470px;
  gap: 14px;
  height: 100%;
  min-height: 0;
  padding: 14px 22px 14px 16px;
  overflow: hidden;
}

.main-column {
  display: grid;
  grid-template-rows: 28% 22% minmax(0, 1fr);
  gap: 12px;
  min-height: 0;
  overflow: hidden;
}

.panel,
.detail-card {
  background: rgba(255, 253, 250, 0.92);
  border: 1px solid #e7d8c6;
  border-radius: 10px;
  box-shadow: 0 2px 10px rgba(74, 43, 15, 0.05);
}

.panel {
  min-height: 0;
  padding: 14px 22px 10px;
  overflow: hidden;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.panel-header h2 {
  margin: 0;
  font-size: 17px;
  font-weight: 900;
  color: #20242a;
}

.panel-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.panel-actions select,
.table-footer select {
  height: 36px;
  min-width: 130px;
  padding: 0 38px 0 14px;
  border: 1px solid #e3d1bd;
  border-radius: 7px;
  background: #fffaf3;
  color: #5f4a35;
  font-weight: 800;
}

.calendar-button {
  width: 40px;
  height: 36px;
  border: 1px solid #e3d1bd;
  border-radius: 7px;
  background: #fffaf3;
  color: #003d78;
  font-size: 16px;
}

.line-chart-wrap {
  height: calc(100% - 46px);
  min-height: 130px;
}

.bar-chart-wrap {
  height: calc(100% - 42px);
  min-height: 105px;
}

.line-chart,
.bar-chart {
  width: 100%;
  height: 100%;
  display: block;
}

.grid-lines line {
  stroke: #e1d3c4;
  stroke-width: 1;
}

.y-labels text,
.x-label,
.bar-label {
  fill: #3b3f45;
  font-size: 12px;
  font-weight: 800;
}

.line-path {
  fill: none;
  stroke: #ff1d1d;
  stroke-width: 3;
}

.line-dot {
  fill: #ff1d1d;
}

.point-value,
.bar-value {
  fill: #20242a;
  font-size: 12px;
  font-weight: 900;
  text-anchor: middle;
}

.x-label,
.bar-label {
  text-anchor: middle;
}

.bar-rect {
  fill: url('#none');
  fill: #ef6654;
  opacity: 0.92;
}

.table-panel {
  display: flex;
  flex-direction: column;
  min-height: 0;
  padding-bottom: 12px;
}

.table-title {
  margin-bottom: 14px;
}

.table-wrap {
  flex: 1;
  min-height: 0;
  border: 1px solid #e2d0bc;
  border-radius: 4px;
  overflow: hidden;
}

table {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
  font-size: 15px;
}

th,
td {
  height: 28px;
  padding: 0 12px;
  border-right: 1px solid #e2d0bc;
  border-bottom: 1px solid #e2d0bc;
  text-align: center;
  white-space: nowrap;
}

th:last-child,
td:last-child {
  border-right: 0;
}

thead th {
  background: #efe3d4;
  color: #1f2933;
  font-weight: 900;
}

tbody tr:nth-child(even) {
  background: rgba(252, 245, 236, 0.86);
}

.badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 58px;
  height: 25px;
  padding: 0 12px;
  border-radius: 999px;
  font-size: 14px;
  line-height: 25px;
  font-weight: 900;
}

.badge.high {
  background: #ffe2df;
  color: #ff2121;
  border: 1px solid #ffb9b2;
}

.badge.medium {
  background: #fff0d8;
  color: #d96b00;
  border: 1px solid #ffd59c;
}

.badge.low {
  background: #dff0ff;
  color: #006db2;
  border: 1px solid #b7dcff;
}

.badge.progress {
  background: #fff0d8;
  color: #f08a00;
  border: 1px solid #ffd59c;
}

.badge.done {
  background: #dff4ef;
  color: #087b6f;
  border: 1px solid #bbe5db;
}

.badge.generated {
  min-width: 76px;
  background: #e9eef6;
  color: #28466f;
  border: 1px solid #d3dce8;
}

.table-footer {
  display: grid;
  grid-template-columns: 160px 1fr 140px;
  align-items: center;
  margin-top: 12px;
  font-size: 15px;
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 18px;
}

.pagination button {
  width: 34px;
  height: 34px;
  border: 0;
  border-radius: 7px;
  background: transparent;
  color: #111827;
  font-size: 17px;
  font-weight: 900;
  cursor: pointer;
}

.pagination button:first-child,
.pagination button:last-child {
  border: 1px solid #e2d0bc;
  background: #fffaf3;
  color: #003d78;
}

.pagination button.active {
  background: #003d78;
  color: #fff;
  box-shadow: 0 4px 12px rgba(0, 61, 120, 0.28);
}

.detail-column {
  display: grid;
  grid-template-rows: minmax(0, 1fr) minmax(0, 1fr);
  gap: 12px;
  min-height: 0;
  overflow: hidden;
}

.detail-card {
  overflow: hidden;
}

.detail-card-header {
  height: 48px;
  padding: 0 24px;
  display: flex;
  align-items: center;
  border-bottom: 1px solid #e6d5c2;
  background: rgba(252, 246, 239, 0.72);
}

.detail-card h3 {
  margin: 0;
  font-size: 17px;
  font-weight: 900;
}

.detail-content {
  height: calc(100% - 48px);
  padding: 16px 24px 20px;
  overflow: hidden;
}

.detail-badges {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.detail-content h2 {
  margin: 0 0 14px;
  font-size: 22px;
  font-weight: 900;
  color: #15191f;
}

.summary-list,
.alarm-info-box dl {
  margin: 0;
}

.summary-list div,
.alarm-info-box dl div {
  display: grid;
  grid-template-columns: 94px 1fr;
  gap: 12px;
  align-items: start;
}

.summary-list dt,
.alarm-info-box dt {
  font-size: 14px;
  font-weight: 900;
  color: #5b5f66;
}

.summary-list dd,
.alarm-info-box dd {
  margin: 0;
  font-size: 14px;
  font-weight: 800;
  color: #2a2f36;
}

.alarm-info-box {
  margin-top: 18px;
  padding: 16px 16px;
  border: 1px solid #e4d2bf;
  border-radius: 8px;
  background: rgba(255, 252, 248, 0.8);
}

.alarm-info-box h4 {
  margin: 0 0 20px;
  font-size: 17px;
  font-weight: 900;
}

.alarm-info-box dl div + div {
  margin-top: 14px;
}

.history-card {
  padding: 18px 24px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.history-card h3 {
  margin: 0 0 12px;
  font-size: 17px;
  font-weight: 900;
}

.history-memo-wrap {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.memo-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 0 0 auto;
  position: sticky;
  top: 0;
  background: rgba(255, 252, 248, 0.92);
  padding-bottom: 10px;
  z-index: 1;
}

.memo-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
}

.memo-section h4 {
  margin: 0;
  font-size: 14px;
  font-weight: 900;
  color: #2a2f36;
}

.memo-input {
  width: 100%;
  height: 60px;
  padding: 10px;
  border: 1px solid #d8d0c7;
  border-radius: 6px;
  background: #fffaf3;
  color: #2a2f36;
  font-size: 13px;
  font-family: inherit;
  resize: none;
}

.memo-input::placeholder {
  color: #9ca3af;
}

.memo-button {
  height: 32px;
  border: 0;
  border-radius: 6px;
  background: #003d78;
  color: #fff;
  font-size: 13px;
  font-weight: 800;
  cursor: pointer;
}

.memo-button:hover {
  background: #002951;
}

.memo-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-height: 0;
}

.memo-list h4 {
  margin: 0;
  font-size: 14px;
  font-weight: 900;
  color: #2a2f36;
}

.memo-item {
  padding: 10px;
  border: 1px solid #e6d5c2;
  border-radius: 6px;
  background: rgba(255, 252, 248, 0.8);
}

.memo-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.memo-time {
  font-size: 12px;
  font-weight: 800;
  color: #5b5f66;
}

.memo-delete {
  width: 24px;
  height: 24px;
  border: 0;
  background: transparent;
  color: #a5a5a5;
  font-size: 18px;
  font-weight: 600;
  cursor: pointer;
}

.memo-delete:hover {
  color: #ff4f63;
}

.memo-text {
  margin: 0;
  font-size: 13px;
  font-weight: 700;
  color: #2a2f36;
  line-height: 1.4;
  word-break: break-word;
}

.schedule-row {
  margin-top: 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.schedule-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.calendar-icon {
  color: #003d78;
  font-size: 20px;
}

.schedule-left strong {
  display: block;
  font-size: 18px;
  font-weight: 900;
}

.schedule-left p {
  margin: 5px 0 0;
  color: #6a6f77;
  font-size: 13px;
  font-weight: 700;
}

.d-day {
  min-width: 58px;
  height: 31px;
  padding: 0 10px;
  border-radius: 6px;
  background: #003d78;
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  font-weight: 900;
  box-shadow: 0 4px 10px rgba(0, 61, 120, 0.24);
}

.timeline {
  margin-top: 18px;
  position: relative;
}

.timeline::before {
  content: '';
  position: absolute;
  left: 6px;
  top: 12px;
  bottom: 24px;
  width: 2px;
  background: #d8d0c7;
}

.timeline-item {
  position: relative;
  display: grid;
  grid-template-columns: 24px 1fr;
  gap: 12px;
}

.timeline-item + .timeline-item {
  margin-top: 24px;
}

.timeline-dot {
  width: 13px;
  height: 13px;
  margin-top: 3px;
  border-radius: 50%;
  background: #003d78;
  z-index: 1;
}

.timeline-item.active .timeline-dot {
  background: #ff1919;
}

.timeline-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.timeline-top strong {
  font-size: 15px;
  font-weight: 900;
}

.timeline-item p {
  margin: 10px 0 0;
  color: #4a4f57;
  font-size: 14px;
  font-weight: 700;
  line-height: 1.45;
}



@media (max-width: 1500px) {
  .top-menu {
    gap: 38px;
  }

  .content-area {
    grid-template-columns: minmax(0, 1fr) 420px;
  }

  .page-body {
    grid-template-columns: 292px 1fr;
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

  .top-menu {
    gap: 22px;
  }
}
</style>
