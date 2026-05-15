<script setup>
import { computed, ref } from 'vue'
import AppTopbar from '@/components/AppTopbar.vue'

const selectedLine = ref('전체')
const currentPage = ref(1)
const rowsPerPage = 7

const summaryCards = [
  { title: '총 설비 수', value: 21, unit: '대', icon: '🏭', tone: 'blue' },
  { title: '가동 설비 수', value: 15, unit: '대', icon: '▶', tone: 'green' },
  { title: '정지 설비 수', value: 4, unit: '대', icon: 'Ⅱ', tone: 'orange' },
  { title: '알람 발생 수', value: 2, unit: '건', icon: '🚨', tone: 'red' },
]

const failureRows = [
  { rank: 1, name: '용접로봇1', type: '용접 로봇', probability: 87, health: 13, color: 'red' },
  { rank: 2, name: '컨베이어3', type: '컨베이어', probability: 65, health: 35, color: 'orange' },
  { rank: 3, name: '차체지그2', type: '차체 지그', probability: 48, health: 52, color: 'yellow' },
  { rank: 4, name: '너트러너4', type: '너트러너', probability: 45, health: 55, color: 'yellow' },
  { rank: 5, name: '비전검사기1', type: '비전 검사기', probability: 20, health: 80, color: 'green' },
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

const totalPages = computed(() => Math.ceil(equipmentRows.length / rowsPerPage))
const pagedEquipmentRows = computed(() => {
  const start = (currentPage.value - 1) * rowsPerPage
  return equipmentRows.slice(start, start + rowsPerPage)
})

const setPage = (page) => {
  currentPage.value = Math.min(Math.max(page, 1), totalPages.value)
}
</script>

<template>
  <div class="dashboard-page">
    <header class="topbar">
      <div class="brand">
        <div class="brand-icon">⚙</div>
        <strong>SFaaS 설비 모니터링 시스템</strong>
      </div>
      <nav class="gnb">
        <a class="active">대시보드</a>
        <a>설비 현황</a>
        <a>설비 상세</a>
        <a>알람 관리</a>
        <a>수명 관리</a>
        <a>사용자 관리</a>
      </nav>
      <div class="top-actions">
        <div class="time">◷ 2024-05-24 10:30:45</div>
        <div class="admin">👤 관리자</div>
        <button class="bell" type="button">🔔<span>2</span></button>
      </div>
    </header>

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

      <section class="upper-grid equal-upper">
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
            <div class="donut"><span>OEE<br /><b>62.5%</b></span></div>
            <div class="oee-info">
              <div><i class="dot blue"></i>가동률 <strong>89.2%</strong></div>
              <div><i class="dot cyan"></i>성능률 <strong>87.5%</strong></div>
              <div><i class="dot orange"></i>품질률 <strong>80.1%</strong></div>
              <hr />
              <div class="total">OEE (종합 효율) <strong>62.5%</strong></div>
              <small>※ 선택 라인 기준 총 21대 설비 평균</small>
            </div>
          </div>
        </article>

        <article class="panel risk-card">
          <div class="panel-header">
            <h2>
              설비별 잠재 고장률 (상위 5)
              <span class="tooltip">?
                <em>
                  - 설비의 알람, 온도, 전류, 부하율, 사이클타임 등 실시간 데이터를 0~100점 위험도로 정규화한 뒤, 중요도에 따라 가중평균하여 산출한 설비 건강도 지표.<br />
                  - 점수가 낮을수록 고장 가능성이 높다고 판단할 수 있음.
                </em>
              </span>
            </h2>
          </div>
          <table class="data-table risk-table">
            <thead>
              <tr><th>순위</th><th>설비명</th><th>설비 유형</th><th>잠재 고장률</th><th>Health Score</th></tr>
            </thead>
            <tbody>
              <tr v-for="row in failureRows" :key="row.rank">
                <td>{{ row.rank }}</td>
                <td>{{ row.name }}</td>
                <td>{{ row.type }}</td>
                <td class="bar-cell"><span>{{ row.probability }}%</span><b class="track"><i :class="row.color" :style="{ width: row.probability + '%' }"></i></b></td>
                <td><span class="score" :class="row.color">{{ row.health }}</span></td>
              </tr>
            </tbody>
          </table>
        </article>
      </section>

      <section class="lower-grid equal-lower">
        <article class="panel detail-card">
          <div class="panel-header"><h2>설비 상세 현황</h2></div>
          <table class="data-table detail-table">
            <thead>
              <tr><th>설비명</th><th>상태</th><th>가동시간</th><th>최근 알람</th><th>마지막 점검일</th></tr>
            </thead>
            <tbody>
              <tr v-for="row in pagedEquipmentRows" :key="row.name">
                <td>{{ row.name }}</td>
                <td><i class="status-dot" :class="row.status.toLowerCase()"></i>{{ row.status }}</td>
                <td>{{ row.uptime }}</td>
                <td :class="{ danger: row.alarm !== '-' }">{{ row.alarm }}</td>
                <td>{{ row.check }}</td>
              </tr>
            </tbody>
          </table>
          <div class="pagination-wrap">
            <b>전체 {{ equipmentRows.length }}건</b>
            <div class="pagination">
              <button type="button" @click="setPage(currentPage - 1)">‹</button>
              <button v-for="page in totalPages" :key="page" type="button" :class="{ active: page === currentPage }" @click="setPage(page)">{{ page }}</button>
              <button type="button" @click="setPage(currentPage + 1)">›</button>
            </div>
          </div>
        </article>

        <article class="panel life-card">
          <div class="panel-header">
            <h2>설비별 잔존 수명 (상위 5)</h2>
            <button type="button" class="more-btn">더보기 ›</button>
          </div>
          <table class="data-table life-table">
            <thead>
              <tr><th>순위</th><th>설비명</th><th>설비 유형</th><th>잔존 수명</th><th>예상 교체 시기</th></tr>
            </thead>
            <tbody>
              <tr v-for="row in lifeRows" :key="row.rank">
                <td>{{ row.rank }}</td>
                <td>{{ row.name }}</td>
                <td>{{ row.type }}</td>
                <td class="life-cell"><b class="track"><i :class="row.color" :style="{ width: row.life + '%' }"></i></b><strong :class="row.color">{{ row.life }}%</strong></td>
                <td :class="row.color">{{ row.date }}</td>
              </tr>
            </tbody>
          </table>
        </article>
      </section>
    </main>
  </div>
</template>

<style scoped>
.dashboard-page { min-width: 1180px; min-height: 100vh; background: #f5f7fb; }
.topbar { height: 70px; display: flex; align-items: center; background: linear-gradient(90deg, #071f49, #002e68); color: #fff; box-shadow: 0 4px 14px rgba(4, 24, 56, .2); }
.brand { width: 305px; height: 100%; display: flex; align-items: center; gap: 12px; padding: 0 30px; border-right: 1px solid rgba(255,255,255,.12); font-size: 19px; }
.brand-icon { width: 34px; height: 34px; display: grid; place-items: center; border: 1px solid rgba(255,255,255,.45); border-radius: 10px; }
.gnb { display: flex; height: 100%; }
.gnb { flex: 1; min-width: 0; }
.gnb a { min-width: 118px; display: grid; place-items: center; font-size: 16px; font-weight: 800; color: rgba(255,255,255,.9); position: relative; white-space: nowrap; }
.gnb a.active::after { content: ''; position: absolute; left: 18px; right: 18px; bottom: 0; height: 5px; background: #16c7d8; border-radius: 8px 8px 0 0; }
.top-actions { margin-left: auto; height: 100%; display: flex; align-items: center; }
.time, .admin, .bell { height: 100%; display: flex; align-items: center; gap: 10px; padding: 0 22px; font-weight: 800; border-left: 1px solid rgba(255,255,255,.12); }
.bell { position: relative; border: 0; color: #fff; background: transparent; font-size: 22px; cursor: pointer; }
.bell span { position: absolute; top: 16px; right: 15px; min-width: 18px; height: 18px; padding: 0 5px; border-radius: 999px; background: #ff2d47; color: #fff; font-size: 12px; }
.content { padding: 22px 32px 30px; }
.summary-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 18px; margin-bottom: 16px; }
.summary-card { height: 142px; display: flex; align-items: center; gap: 22px; padding: 26px 32px; background: #fff; border-radius: 14px; box-shadow: 0 5px 16px rgba(13, 36, 72, .12); }
.summary-icon { width: 88px; height: 88px; display: grid; place-items: center; border-radius: 50%; font-size: 40px; }
.summary-icon.blue { background: #e4f0ff; color: #1474df; } .summary-icon.green { background: #e1f6ef; color: #12a985; } .summary-icon.orange { background: #fff0df; color: #df7922; } .summary-icon.red { background: #ffe7eb; color: #fa2c45; }
.summary-card p { margin: 0 0 4px; font-size: 16px; font-weight: 900; } .summary-card strong { font-size: 46px; line-height: 1; font-weight: 950; } .summary-card span { margin-left: 8px; font-weight: 800; }
.upper-grid, .lower-grid { display: grid; grid-template-columns: .95fr 1.05fr; gap: 16px; margin-bottom: 16px; }
.panel { background: #fff; border-radius: 14px; box-shadow: 0 5px 16px rgba(13, 36, 72, .11); padding: 16px 18px; overflow: visible; }
.equal-upper .panel { height: 340px; } .equal-lower .panel { height: 340px; }
.panel-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
h2 { margin: 0; font-size: 20px; font-weight: 950; letter-spacing: -.02em; }
.line-select { width: 116px; height: 36px; border: 1px solid #d8e1ed; border-radius: 6px; padding: 0 10px; color: #0d2448; font-weight: 800; background: #fff; }
.oee-body { height: calc(100% - 48px); display: flex; align-items: center; justify-content: space-around; }
.donut { width: 190px; height: 190px; border-radius: 50%; background: conic-gradient(#1375de 0 58%, #e8892c 58% 78%, #1599b6 78% 100%); position: relative; display: grid; place-items: center; }
.donut::after { content: ''; position: absolute; width: 112px; height: 112px; background: #fff; border-radius: 50%; } .donut span { position: relative; z-index: 1; text-align: center; font-weight: 900; } .donut b { font-size: 30px; }
.oee-info { width: 330px; font-size: 16px; font-weight: 850; } .oee-info div { display: flex; align-items: center; justify-content: space-between; margin: 10px 0; } .oee-info strong { color: #126de0; font-weight: 950; } .oee-info small { display: block; margin-top: 12px; font-weight: 800; }
.dot, .status-dot { display: inline-block; border-radius: 50%; margin-right: 10px; } .dot { width: 12px; height: 12px; } .blue { background: #1375de; color: #1375de; } .cyan { background: #1599b6; } .orange { background: #ff6b16; color: #ff6b16; } .yellow { background: #ff9f1a; color: #ff9f1a; } .green { background: #12b58f; color: #12b58f; } .red { background: #ff3045; color: #ff3045; }
.tooltip { position: relative; display: inline-grid; place-items: center; width: 19px; height: 19px; margin-left: 6px; border: 1px solid #274267; border-radius: 50%; font-size: 13px; font-weight: 950; cursor: help; }
.tooltip em { display: none; position: absolute; top: 26px; left: -10px; width: 470px; z-index: 10; padding: 13px 15px; background: #08234d; color: #fff; border-radius: 10px; box-shadow: 0 10px 24px rgba(0,0,0,.18); font-style: normal; font-size: 13px; line-height: 1.55; font-weight: 700; }
.tooltip:hover em { display: block; }
.data-table { width: 100%; border-collapse: separate; border-spacing: 0; table-layout: fixed; border: 1px solid #e2e7ee; border-radius: 10px; overflow: hidden; font-size: 14px; }
.data-table th, .data-table td { height: 34px; padding: 6px 10px; text-align: center; font-weight: 850; border-bottom: 1px solid #edf1f6; white-space: nowrap; }
.data-table th { background: #fafafa; color: #142b50; font-size: 13px; font-weight: 950; } .data-table tr:last-child td { border-bottom: 0; }
.bar-cell, .life-cell { display: flex; align-items: center; gap: 10px; } .bar-cell span { width: 38px; text-align: left; } .track { flex: 1; height: 8px; background: #e8e9eb; border-radius: 99px; overflow: hidden; } .track i { display: block; height: 100%; border-radius: 99px; }
.score { display: inline-block; min-width: 68px; padding: 3px 12px; border: 1px solid currentColor; border-radius: 7px; background: rgba(255,255,255,.85); font-weight: 950; }
.detail-card, .life-card { display: flex; flex-direction: column; }
.detail-table { font-size: 13px; } .detail-table th, .detail-table td { height: 28px; padding: 4px 8px; }
.status-dot { width: 10px; height: 10px; } .running { background: #14b993; } .idle { background: #ff951a; } .error { background: #ff3045; } .danger { color: #ff2138; font-weight: 950; }
.pagination-wrap { margin-top: auto; padding-top: 12px; display: grid; grid-template-columns: 1fr auto 1fr; align-items: center; }
.pagination-wrap b { justify-self: start; }
.pagination { grid-column: 2; justify-self: center; display: flex; gap: 10px; align-items: center; } .pagination button { min-width: 38px; height: 34px; border: 1px solid #dfe5ee; background: #fff; border-radius: 6px; font-weight: 900; color: #122a4c; cursor: pointer; } .pagination button.active { background: linear-gradient(180deg,#0fc6d2,#0896b7); color: #fff; border-color: transparent; }
.more-btn { border: 0; background: transparent; color: #0d2448; font-weight: 950; cursor: pointer; }
.life-table th, .life-table td { height: 42px; } .life-cell strong { min-width: 46px; background: transparent !important; font-weight: 950; }
.life-table td.red, .life-table td.orange, .life-table td.yellow, .life-table td.green { background: transparent; font-weight: 950; }
@media (max-width: 1280px) { .dashboard-page { min-width: 1200px; } .gnb a { min-width: 104px; font-size: 15px; } .brand { width: 280px; } }
</style>
