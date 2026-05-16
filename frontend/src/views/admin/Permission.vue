<script setup>
import { computed, ref } from 'vue'
import AppTopbar from '@/components/AppTopbar.vue'

const selectedUserId = ref('sminee')
const checkedUserIds = ref(['sminee', 'jkim'])
const equipSearch = ref('')
const currentPage = ref(1)

const summaryCards = [
  { title: '전체 사용자', value: 24, icon: '👤', tone: 'blue' },
  { title: '관리자', value: 3, icon: '🛡', tone: 'purple' },
  { title: '운영자', value: 9, icon: '⚙', tone: 'green' },
  { title: '일반 사용자', value: 12, icon: '👥', tone: 'orange' },
]

const users = [
  {
    id: 'sminee',
    name: '이수민',
    role: 'operator',
    roleLabel: '운영자',
    equipment: 'Conveyor D1',
    status: 'active',
    statusLabel: '활성',
    lastLogin: '2024-05-24 08:58',
    email: 'sminee@company.com',
    avatarTone: 'green',
    assignedEquipIds: ['robot-a1', 'conveyor-d1'],
  },
  {
    id: 'jkim',
    name: '김지훈',
    role: 'admin',
    roleLabel: '관리자',
    equipment: 'Robot A1, Nutrunner B2',
    status: 'active',
    statusLabel: '활성',
    lastLogin: '2024-05-24 10:20',
    email: 'jkim@company.com',
    avatarTone: 'purple',
    assignedEquipIds: ['robot-a1', 'nutrunner-b2'],
  },
]

const allEquipment = [
  { id: 'robot-a1', name: 'Robot A1' },
  { id: 'nutrunner-b2', name: 'Nutrunner B2' },
  { id: 'conveyor-d1', name: 'Conveyor D1' },
  { id: 'press-c1', name: 'Press C1' },
  { id: 'welding-e1', name: 'Welding E1' },
  { id: 'agv-f1', name: 'AGV F1' },
]

const selectedUser = computed(
  () => users.find((u) => u.id === selectedUserId.value) ?? users[0],
)

const filteredEquipmentOptions = computed(() => {
  const q = equipSearch.value.trim().toLowerCase()
  if (!q) return allEquipment
  return allEquipment.filter((e) => e.name.toLowerCase().includes(q))
})

const selectedEquipTags = computed(() =>
  allEquipment.filter((e) => selectedUser.value.assignedEquipIds.includes(e.id)),
)

const selectedCount = computed(() => checkedUserIds.value.length)

const isUserChecked = (id) => checkedUserIds.value.includes(id)

const toggleUserCheck = (id) => {
  if (isUserChecked(id)) {
    checkedUserIds.value = checkedUserIds.value.filter((v) => v !== id)
  } else {
    checkedUserIds.value = [...checkedUserIds.value, id]
  }
}

const toggleAllUsers = (event) => {
  checkedUserIds.value = event.target.checked ? users.map((u) => u.id) : []
}

const selectUser = (id) => {
  selectedUserId.value = id
}

const isEquipAssigned = (equipId) => selectedUser.value.assignedEquipIds.includes(equipId)

const toggleEquip = (equipId) => {
  const user = users.find((u) => u.id === selectedUserId.value)
  if (!user) return
  if (user.assignedEquipIds.includes(equipId)) {
    user.assignedEquipIds = user.assignedEquipIds.filter((id) => id !== equipId)
  } else {
    user.assignedEquipIds.push(equipId)
  }
  user.equipment = allEquipment
    .filter((e) => user.assignedEquipIds.includes(e.id))
    .map((e) => e.name)
    .join(', ')
}

const removeEquipTag = (equipId) => {
  if (selectedUser.value.assignedEquipIds.includes(equipId)) {
    toggleEquip(equipId)
  }
}
</script>

<template>
  <div class="permission-page">
    <AppTopbar active-menu="사용자 관리" />

    <main class="content">
      <header class="page-head">
        <div>
          <h1>사용자 관리</h1>
          <p>사용자 계정과 권한을 관리하고 담당 설비를 할당합니다.</p>
        </div>
      </header>

      <div class="main-layout">
        <div class="left-column">
          <section class="summary-grid">
            <article v-for="card in summaryCards" :key="card.title" class="summary-card">
              <div class="summary-icon" :class="card.tone">{{ card.icon }}</div>
              <div>
                <p>{{ card.title }}</p>
                <strong>{{ card.value }}</strong>
              </div>
            </article>
          </section>

          <section class="panel table-panel">
          <div class="action-bar">
            <div class="action-buttons">
              <button type="button" class="btn primary">+ 사용자 추가</button>
              <button type="button" class="btn danger-outline">🗑 사용자 삭제</button>
              <button type="button" class="btn outline">🛡 권한 변경</button>
              <button type="button" class="btn outline">🔗 설비 할당</button>
            </div>
            <button type="button" class="refresh-btn" title="새로고침">↻</button>
          </div>

          <div class="table-wrap">
            <table class="data-table">
              <thead>
                <tr>
                  <th class="col-check">
                    <input
                      type="checkbox"
                      :checked="checkedUserIds.length === users.length"
                      @change="toggleAllUsers"
                    />
                  </th>
                  <th>이름</th>
                  <th>아이디</th>
                  <th>역할</th>
                  <th>담당 설비</th>
                  <th>상태</th>
                  <th>최근 로그인</th>
                  <th>관리</th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="user in users"
                  :key="user.id"
                  :class="{ selected: selectedUserId === user.id }"
                  @click="selectUser(user.id)"
                >
                  <td class="col-check" @click.stop>
                    <input
                      type="checkbox"
                      :checked="isUserChecked(user.id)"
                      @change="toggleUserCheck(user.id)"
                    />
                  </td>
                  <td class="name-cell">{{ user.name }}</td>
                  <td>{{ user.id }}</td>
                  <td><span class="role-badge" :class="user.role">{{ user.roleLabel }}</span></td>
                  <td class="equip-cell">{{ user.equipment }}</td>
                  <td><span class="status-badge" :class="user.status">{{ user.statusLabel }}</span></td>
                  <td>{{ user.lastLogin }}</td>
                  <td class="manage-cell" @click.stop>
                    <button type="button" class="icon-btn edit" title="수정">✎</button>
                    <button type="button" class="icon-btn delete" title="삭제">🗑</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <div class="table-footer">
            <span>선택된 사용자 {{ selectedCount }}명 | 전체 {{ users.length }}건</span>
            <div class="pagination">
              <button type="button" class="active">1</button>
            </div>
            <select class="filter-select small">
              <option>10 / 페이지</option>
            </select>
          </div>
          </section>
        </div>

        <aside class="panel side-panel">
          <h2>사용자 상세 및 설비 할당</h2>

          <div class="profile-block">
            <div class="avatar" :class="selectedUser.avatarTone">👤</div>
            <dl class="profile-list">
              <div><dt>이름</dt><dd>{{ selectedUser.name }}</dd></div>
              <div><dt>아이디</dt><dd>{{ selectedUser.id }}</dd></div>
              <div>
                <dt>역할</dt>
                <dd><span class="role-badge" :class="selectedUser.role">{{ selectedUser.roleLabel }}</span></dd>
              </div>
              <div>
                <dt>상태</dt>
                <dd><span class="status-badge" :class="selectedUser.status">{{ selectedUser.statusLabel }}</span></dd>
              </div>
              <div><dt>이메일</dt><dd>{{ selectedUser.email }}</dd></div>
            </dl>
          </div>

          <h3>담당 설비 할당</h3>
          <label class="equip-search">
            <span>⌕</span>
            <input v-model="equipSearch" type="search" placeholder="설비 검색" />
          </label>

          <div class="equip-check-grid">
            <label
              v-for="equip in filteredEquipmentOptions"
              :key="equip.id"
              class="equip-check"
            >
              <input
                type="checkbox"
                :checked="isEquipAssigned(equip.id)"
                @change="toggleEquip(equip.id)"
              />
              <span>{{ equip.name }}</span>
            </label>
          </div>

          <h3>선택된 설비</h3>
          <div class="tag-list">
            <span v-for="tag in selectedEquipTags" :key="tag.id" class="equip-tag">
              {{ tag.name }}
              <button type="button" @click="removeEquipTag(tag.id)">×</button>
            </span>
          </div>

          <div class="side-actions">
            <button type="button" class="btn navy full">할당 저장</button>
            <button type="button" class="btn outline full">사용자 수정</button>
            <button type="button" class="btn danger-outline full">🗑 사용자 삭제</button>
          </div>
        </aside>
      </div>
    </main>
  </div>
</template>

<style scoped>
.permission-page {
  min-width: 1280px;
  min-height: 100vh;
  background: #f5f7fb;
}

.content {
  padding: 22px 32px 30px;
}

.page-head h1 {
  margin: 0 0 6px;
  font-size: 28px;
  font-weight: 950;
  color: #0d2448;
}

.page-head p {
  margin: 0 0 18px;
  font-size: 14px;
  font-weight: 700;
  color: #6b7c94;
}

.main-layout {
  display: grid;
  grid-template-columns: 1fr 340px;
  gap: 16px;
  align-items: stretch;
}

.left-column {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-width: 0;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.summary-card {
  height: 118px;
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 22px 28px;
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 5px 16px rgba(13, 36, 72, 0.11);
}

.summary-icon {
  width: 72px;
  height: 72px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  font-size: 32px;
}

.summary-icon.blue { background: #e4f0ff; }
.summary-icon.purple { background: #efe8ff; }
.summary-icon.green { background: #e1f6ef; }
.summary-icon.orange { background: #fff0df; }

.summary-card p {
  margin: 0 0 4px;
  font-size: 15px;
  font-weight: 900;
  color: #4a5f7a;
}

.summary-card strong {
  font-size: 40px;
  font-weight: 950;
  color: #0d2448;
  line-height: 1;
}

.panel {
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 5px 16px rgba(13, 36, 72, 0.11);
  padding: 16px 18px;
}

.action-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
  gap: 12px;
}

.action-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.btn {
  height: 36px;
  padding: 0 14px;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 900;
  cursor: pointer;
  border: 1px solid transparent;
}

.btn.primary {
  background: #1890ff;
  color: #fff;
  border-color: #1890ff;
}

.btn.outline {
  background: #fff;
  color: #126de0;
  border-color: #b8d4f5;
}

.btn.danger-outline {
  background: #fff;
  color: #ff3045;
  border-color: #ffb8c0;
}

.btn.navy {
  background: linear-gradient(180deg, #071f49, #002e68);
  color: #fff;
}

.btn.full {
  width: 100%;
}

.refresh-btn {
  width: 36px;
  height: 36px;
  border: 1px solid #d8e1ed;
  border-radius: 6px;
  background: #fff;
  font-size: 18px;
  font-weight: 900;
  color: #4a5f7a;
  cursor: pointer;
}

.data-table {
  width: 100%;
  border-collapse: separate;
  border-spacing: 0;
  border: 1px solid #e2e7ee;
  border-radius: 10px;
  overflow: hidden;
  font-size: 13px;
}

.data-table th,
.data-table td {
  height: 44px;
  padding: 8px 12px;
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

.col-check {
  width: 44px;
}

.name-cell {
  text-align: left;
  font-weight: 950;
  color: #0d2448;
}

.equip-cell {
  text-align: left;
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
}

.role-badge,
.status-badge {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 900;
}

.role-badge.admin {
  background: #efe8ff;
  color: #722ed1;
}

.role-badge.operator {
  background: #e1f6ef;
  color: #12a985;
}

.role-badge.user {
  background: #fff0df;
  color: #df7922;
}

.status-badge.active {
  background: #e1f6ef;
  color: #12a985;
}

.status-badge.inactive {
  background: #ffe7eb;
  color: #fa2c45;
}

.status-badge.pending {
  background: #f0f2f5;
  color: #6b7c94;
}

.manage-cell {
  display: flex;
  justify-content: center;
  gap: 8px;
}

.icon-btn {
  width: 30px;
  height: 30px;
  border: 1px solid #dfe5ee;
  border-radius: 6px;
  background: #fff;
  cursor: pointer;
  font-size: 14px;
}

.icon-btn.edit {
  color: #1890ff;
}

.icon-btn.delete {
  color: #ff3045;
}

.table-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 14px;
  font-size: 13px;
  font-weight: 800;
  color: #4a5f7a;
}

.pagination {
  display: flex;
  gap: 8px;
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

.filter-select.small {
  height: 32px;
  border: 1px solid #d8e1ed;
  border-radius: 6px;
  padding: 0 10px;
  font-size: 12px;
  font-weight: 800;
  color: #0d2448;
  background: #fff;
}

.side-panel {
  display: flex;
  flex-direction: column;
  min-height: 100%;
}

.table-panel {
  flex: 1;
}

.side-panel h2 {
  margin: 0 0 16px;
  font-size: 18px;
  font-weight: 950;
  color: #0d2448;
}

.side-panel h3 {
  margin: 18px 0 10px;
  font-size: 14px;
  font-weight: 950;
  color: #0d2448;
}

.profile-block {
  display: flex;
  gap: 14px;
  padding-bottom: 16px;
  border-bottom: 1px solid #edf1f6;
}

.avatar {
  width: 72px;
  height: 72px;
  flex-shrink: 0;
  display: grid;
  place-items: center;
  border-radius: 50%;
  font-size: 32px;
  background: #e1f6ef;
}

.avatar.green { background: #e1f6ef; }
.avatar.purple { background: #efe8ff; }

.profile-list {
  margin: 0;
  padding: 0;
  flex: 1;
}

.profile-list > div {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  padding: 5px 0;
  font-size: 13px;
}

.profile-list dt {
  margin: 0;
  font-weight: 800;
  color: #6b7c94;
}

.profile-list dd {
  margin: 0;
  font-weight: 900;
  color: #0d2448;
}

.equip-search {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 36px;
  padding: 0 12px;
  border: 1px solid #d8e1ed;
  border-radius: 6px;
  margin-bottom: 12px;
}

.equip-search input {
  flex: 1;
  border: 0;
  outline: none;
  font-size: 13px;
  font-weight: 700;
  color: #0d2448;
}

.equip-check-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
}

.equip-check {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 800;
  color: #0d2448;
  cursor: pointer;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  min-height: 32px;
}

.equip-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 10px;
  background: #eef6ff;
  border: 1px solid #b8d4f5;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 900;
  color: #126de0;
}

.equip-tag button {
  border: 0;
  background: transparent;
  color: #6b7c94;
  font-size: 14px;
  cursor: pointer;
  line-height: 1;
}

.side-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 20px;
}

@media (max-width: 1400px) {
  .permission-page {
    min-width: 1200px;
  }
}
</style>
