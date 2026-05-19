<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import AppTopbar from '@/components/AppTopbar.vue'

const router = useRouter()
const selectedLine = ref('전체')
const oeeProgress = ref(0)

const summaryCards = [
  { title: '총 설비 수', value: 21, unit: '대', icon: '🏭', tone: 'blue' },
  { title: '가동 설비 수', value: 15, unit: '대', icon: '▶️', tone: 'green' },
  { title: '정지 설비 수', value: 4, unit: '대', icon: '⏸️', tone: 'orange' },
  { title: '알람 발생 수', value: 2, unit: '건', icon: '🚨', tone: 'red' },
]

const equipmentRows = [
  { name: 'Welding Robot 1', status: 'Running', uptime: '18:35:22', alarm: '2024-05-24 09:12', check: '2024-05-20' },
  { name: 'Welding Robot 2', status: 'Running', uptime: '17:20:11', alarm: '-', check: '2024-05-21' },
  { name: 'Conveyor 1', status: 'Idle', uptime: '05:12:08', alarm: '-', check: '2024-05-18' },
  { name: 'Conveyor 2', status: 'Error', uptime: '20:12:45', alarm: '2024-05-24 07:58', check: '2024-05-19' },
  { name: 'Conveyor 3', status: 'Running', uptime: '12:48:21', alarm: '-', check: '2024-05-22' },
  { name: 'Nutrunner 1', status: 'Running', uptime: '08:16:43', alarm: '-', check: '2024-05-19' },
  { name: 'Nutrunner 2', status: 'Idle', uptime: '06:41:30', alarm: '-', check: '2024-05-17' },
  { name: 'Nutrunner 3', status: 'Running', uptime: '11:03:09', alarm: '-', check: '2024-05-18' },
  { name: 'Nutrunner 4', status: 'Error', uptime: '02:45:33', alarm: '2024-05-24 06:47', check: '2024-05-17' },
  { name: 'Body Jig 1', status: 'Running', uptime: '16:22:18', alarm: '-', check: '2024-05-20' },
  { name: 'Body Jig 2', status: 'Error', uptime: '03:18:26', alarm: '2024-05-24 08:21', check: '2024-05-16' },
  { name: 'Body Jig 3', status: 'Running', uptime: '14:12:08', alarm: '-', check: '2024-05-18' },
  { name: 'Vision 1', status: 'Running', uptime: '09:44:53', alarm: '-', check: '2024-05-21' },
  { name: 'Vision 2', status: 'Idle', uptime: '07:31:41', alarm: '-', check: '2024-05-19' },
  { name: 'AGV 1', status: 'Running', uptime: '08:40:12', alarm: '-', check: '2024-05-16' },
  { name: 'AGV 2', status: 'Running', uptime: '10:26:51', alarm: '-', check: '2024-05-21' },
  { name: 'PLC Panel 1', status: 'Running', uptime: '22:05:16', alarm: '-', check: '2024-05-22' },
  { name: 'PLC Panel 2', status: 'Running', uptime: '21:18:44', alarm: '-', check: '2024-05-20' },
  { name: 'Temperature Sensor 1', status: 'Running', uptime: '19:11:05', alarm: '-', check: '2024-05-20' },
  { name: 'Current Sensor 1', status: 'Idle', uptime: '04:22:31', alarm: '-', check: '2024-05-18' },
  { name: 'Press Unit 1', status: 'Error', uptime: '01:15:24', alarm: '2024-05-24 07:58', check: '2024-05-18' },
]

const lifeRows = [
  { rank: 1, name: '용접로봇1', type: '용접 로봇', life: 13, date: '2024-08-15', color: 'red' },
  { rank: 2, name: '컨베이어3', type: '컨베이어', life: 28, date: '2024-11-02', color: 'orange' },
  { rank: 3, name: '차체지그2', type: '차체 지그', life: 45, date: '2025-02-18', color: 'yellow' },
  { rank: 4, name: '너트러너4', type: '너트러너', life: 60, date: '2025-05-30', color: 'green' },
  { rank: 5, name: '비전검사기1', type: '비전 검사기', life: 78, date: '2025-08-22', color: 'green' },
]

const donutStyle = computed(() => {
  const t = oeeProgress.value
  const p1 = (58 * t).toFixed(1)
  const p2 = (78 * t).toFixed(1)
  const p3 = (100 * t).toFixed(1)
  return {
    background: `conic-gradient(#1375de 0 ${p1}%, #e8892c ${p1}% ${p2}%, #1599b6 ${p2}% ${p3}%, #edf1f6 ${p3}% 100%)`,
  }
})

onMounted(() => {
  const duration = 1000
  const start = performance.now()
  const tick = (now) => {
    const t = Math.min((now - start) / duration, 1)
    // ease-out cubic
    oeeProgress.value = 1 - Math.pow(1 - t, 3)
    if (t < 1) requestAnimationFrame(tick)
  }
  requestAnimationFrame(tick)
})
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
        <!-- 좌상단: 종합설비효율 -->
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
              <span><b>62.5%</b></span>
            </div>
            <div class="oee-info">
              <div class="oee-metric">
                <span class="metric-label"><i class="dot blue"></i>가동률</span>
                <strong class="metric-val">89.2%</strong>
              </div>
              <div class="oee-operator">×</div>
              <div class="oee-metric">
                <span class="metric-label"><i class="dot orange"></i>성능률</span>
                <strong class="metric-val">87.5%</strong>
              </div>
              <div class="oee-operator">×</div>
              <div class="oee-metric">
                <span class="metric-label"><i class="dot cyan"></i>품질률</span>
                <strong class="metric-val">80.1%</strong>
              </div>
              <div class="oee-result-row">
                <div class="oee-equals">=</div>
                <div class="oee-total">
                  <span>OEE (종합설비효율)</span>
                  <strong>62.5%</strong>
                </div>
              </div>
            </div>
          </div>
        </article>

        <!-- 좌하단: 설비별 잔존 수명 -->
        <article class="panel life-card">
          <div class="panel-header">
            <h2>설비별 잔존 수명 (상위 5)</h2>
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
              <tr v-for="row in lifeRows" :key="row.rank">
                <td>{{ row.rank }}</td>
                <td>{{ row.name }}</td>
                <td>{{ row.type }}</td>
                <td class="life-cell"><b class="track"><i :class="row.color" :style="{ width: row.life + '%' }"></i></b><strong :class="row.color">{{ row.life }}%</strong></td>
              </tr>
            </tbody>
          </table>
        </article>

        <!-- 우측 전체: 설비 상세 현황 (2행 점유) -->
        <article class="panel detail-card">
          <div class="panel-header">
            <h2>설비 상세 현황</h2>
            <span class="detail-count">전체 {{ equipmentRows.length }}건</span>
          </div>
          <div class="detail-table-wrap">
            <table class="data-table detail-table">
              <thead>
                <tr><th>설비명</th><th>상태</th><th>가동시간</th><th>최근 알람</th><th>마지막 점검일</th></tr>
              </thead>
              <tbody>
                <tr v-for="row in equipmentRows" :key="row.name">
                  <td>{{ row.name }}</td>
                  <td><i class="status-dot" :class="row.status.toLowerCase()"></i>{{ row.status }}</td>
                  <td>{{ row.uptime }}</td>
                  <td :class="{ danger: row.alarm !== '-' }">{{ row.alarm }}</td>
                  <td>{{ row.check }}</td>
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

/* 요약 카드 */
.summary-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 18px; margin-bottom: 16px; }
.summary-card { height: 142px; display: flex; align-items: center; gap: 22px; padding: 26px 32px; background: #fff; border-radius: 14px; box-shadow: 0 5px 16px rgba(13, 36, 72, .12); }
.summary-icon { width: 88px; height: 88px; display: grid; place-items: center; border-radius: 50%; font-size: 40px; }
.summary-icon.blue { background: #e4f0ff; color: #1474df; }
.summary-icon.green { background: #e1f6ef; color: #12a985; }
.summary-icon.orange { background: #fff0df; color: #df7922; }
.summary-icon.red { background: #ffe7eb; color: #fa2c45; }
.summary-card p { margin: 0 0 4px; font-size: 16px; font-weight: 900; }
.summary-card strong { font-size: 46px; line-height: 1; font-weight: 950; }
.summary-card span { margin-left: 8px; font-weight: 800; }

/* 메인 그리드 */
.main-grid {
  display: grid;
  grid-template-columns: 0.75fr 1.25fr;
  grid-template-rows: 340px 340px;
  gap: 16px;
}

.panel { background: #fff; border-radius: 14px; box-shadow: 0 5px 16px rgba(13, 36, 72, .11); padding: 16px 18px; overflow: hidden; }
.panel-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
h2 { margin: 0; font-size: 20px; font-weight: 950; letter-spacing: -.02em; }

/* 카드 배치 */
.oee-card  { grid-column: 1; grid-row: 1; }
.life-card { grid-column: 1; grid-row: 2; }
.detail-card { grid-column: 2; grid-row: 1 / 3; display: flex; flex-direction: column; }

/* OEE 카드 */
.line-select { width: 116px; height: 36px; border: 1px solid #d8e1ed; border-radius: 6px; padding: 0 10px; color: #0d2448; font-weight: 800; background: #fff; }
.oee-body { height: calc(100% - 48px); display: flex; align-items: center; justify-content: center; gap: 24px; padding: 0 8px; }
.donut { width: 220px; height: 220px; flex-shrink: 0; border-radius: 50%; position: relative; display: grid; place-items: center; }
.donut::after { content: ''; position: absolute; width: 130px; height: 130px; background: #fff; border-radius: 50%; }
.donut span { position: relative; z-index: 1; text-align: center; font-weight: 900; }
.donut b { font-size: 28px; }

/* OEE 지표 */
.oee-info { display: flex; flex-direction: column; align-items: center; gap: 0; width: 190px; flex-shrink: 0; }
.oee-metric {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 7px 12px;
  border-radius: 8px;
  background: #f7faff;
  font-size: 14px;
  font-weight: 850;
}
.metric-label { display: flex; align-items: center; color: #4a5f7a; }
.metric-val { color: #126de0; font-weight: 950; font-size: 15px; }
.oee-operator {
  font-size: 16px;
  font-weight: 900;
  color: #9aa8bc;
  line-height: 1;
  padding: 3px 0;
}
.oee-result-row {
  display: flex;
  align-items: center;
  gap: 6px;
  width: 100%;
  padding-top: 4px;
}
.oee-equals {
  font-size: 20px;
  font-weight: 900;
  color: #126de0;
  flex-shrink: 0;
}
.oee-total {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex: 1;
  padding: 8px 12px;
  border-radius: 8px;
  background: linear-gradient(135deg, #e8f0fe, #f0f7ff);
  border: 1px solid #b8d4f5;
  text-align: center;
  gap: 2px;
}
.oee-total span { font-size: 12px; font-weight: 850; color: #4a5f7a; }
.oee-total strong { font-size: 20px; font-weight: 950; color: #126de0; }

/* 공통 색상 */
.dot, .status-dot { display: inline-block; border-radius: 50%; margin-right: 8px; }
.dot { width: 12px; height: 12px; flex-shrink: 0; }
.blue { background: #1375de; color: #1375de; }
.cyan { background: #1599b6; color: #1599b6; }
.orange { background: #ff6b16; color: #ff6b16; }
.yellow { background: #ff9f1a; color: #ff9f1a; }
.green { background: #12b58f; color: #12b58f; }
.red { background: #ff3045; color: #ff3045; }

/* 테이블 공통 */
.data-table { width: 100%; border-collapse: separate; border-spacing: 0; table-layout: fixed; border: 1px solid #e2e7ee; border-radius: 10px; overflow: hidden; font-size: 14px; }
.data-table th, .data-table td { height: 34px; padding: 6px 10px; text-align: center; font-weight: 850; border-bottom: 1px solid #edf1f6; white-space: nowrap; }
.data-table th { background: #fafafa; color: #142b50; font-size: 13px; font-weight: 950; }
.data-table tr:last-child td { border-bottom: 0; }

/* 설비 상세 현황 */
.detail-table-wrap { flex: 1; min-height: 0; overflow-y: auto; border-radius: 10px; }
.detail-table-wrap .data-table { border-radius: 0; border: none; }
.detail-table-wrap::-webkit-scrollbar { width: 6px; }
.detail-table-wrap::-webkit-scrollbar-thumb { background: #d0d9e8; border-radius: 3px; }
.detail-table-wrap::-webkit-scrollbar-track { background: transparent; }
.detail-count { font-size: 13px; font-weight: 800; color: #4a5f7a; }
.detail-table { font-size: 15px; }
.detail-table th, .detail-table td { height: 34px; padding: 6px 12px; }
.status-dot { width: 10px; height: 10px; }
.running { background: #14b993; }
.idle { background: #ff951a; }
.error { background: #ff3045; }
.danger { color: #ff2138; font-weight: 950; }

/* 잔존 수명 */
.life-card { display: flex; flex-direction: column; }
.life-table th, .life-table td { height: 42px; }
.life-cell { display: flex; align-items: center; gap: 10px; padding: 0 8px; }
.track { flex: 1; height: 8px; background: #e8e9eb; border-radius: 99px; overflow: hidden; display: block; min-width: 0; }
.track i { display: block; height: 100%; border-radius: 99px; }
.life-cell strong { width: 40px; flex-shrink: 0; text-align: right; background: transparent !important; font-weight: 950; }
.life-table td.red, .life-table td.orange, .life-table td.yellow, .life-table td.green { background: transparent; font-weight: 950; }
.more-btn { border: 0; background: transparent; color: #0d2448; font-weight: 950; cursor: pointer; }

@media (max-width: 1280px) { .dashboard-page { min-width: 1200px; } }
</style>
