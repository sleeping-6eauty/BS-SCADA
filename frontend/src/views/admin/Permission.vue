<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import AppTopbar from '@/components/AppTopbar.vue'
import {
  getUsers,
  createUser as apiCreateUser,
  updateUser as apiUpdateUser,
  deleteUser as apiDeleteUser,
  getUserEquipments,
  assignEquipmentsBatch,
  getEquipmentNames,
} from '@/api/user.js'

const selectedUserId = ref(null)
const checkedUserIds = ref([])
const userSearch = ref('')
const equipSearch = ref('')
const isAddingUser = ref(false)
const isEditingUser = ref(false)
const loading = ref(false)
const errorMsg = ref('')

const roleOptions = [
  { value: 'ADMIN', label: '관리자' },
  { value: 'USER', label: '일반 사용자' },
]

const statusOptions = [
  { value: 'active', label: '활성' },
  { value: 'inactive', label: '비활성' },
  { value: 'pending', label: '대기' },
]

const getRoleLabel = (role) => (role === 'ADMIN' ? '관리자' : '일반 사용자')
const getStatusLabel = (status) => {
  if (status === 'active') return '활성'
  if (status === 'inactive') return '비활성'
  return '대기'
}
const getAvatarTone = (role) => (role === 'ADMIN' ? 'purple' : 'green')

const users = reactive([])

const mapUserFromApi = (apiUser, equips = []) => ({
  userId: apiUser.userId,
  name: apiUser.name,
  email: apiUser.email,
  role: apiUser.role || 'USER',
  roleLabel: getRoleLabel(apiUser.role),
  status: apiUser.status || 'active',
  statusLabel: getStatusLabel(apiUser.status),
  lastLogin: '-',
  avatarTone: getAvatarTone(apiUser.role),
  equipment: equips.map((e) => e.equipmentId).join(', '),
  assignedEquipIds: equips.map((e) => e.equipmentId),
})

const createEmptyUser = () => ({
  userId: null,
  name: '',
  role: 'USER',
  roleLabel: '일반 사용자',
  equipment: '',
  status: 'active',
  statusLabel: '활성',
  lastLogin: '-',
  email: '',
  password: '',
  avatarTone: 'green',
  assignedEquipIds: [],
})

const createDraftFromUser = (user) => {
  if (!user) return createEmptyUser()
  return {
    userId: user.userId,
    name: user.name,
    role: user.role,
    roleLabel: user.roleLabel,
    equipment: user.equipment,
    status: user.status,
    statusLabel: user.statusLabel,
    lastLogin: user.lastLogin,
    email: user.email,
    password: '',
    avatarTone: user.avatarTone,
    assignedEquipIds: [...user.assignedEquipIds],
  }
}

const newUser = reactive(createEmptyUser())

const summaryCards = computed(() => [
  { title: '전체 사용자', value: users.length, icon: '👥', tone: 'blue' },
  { title: '관리자', value: users.filter((u) => u.role === 'ADMIN').length, icon: '🛡️', tone: 'purple' },
  { title: '일반 사용자', value: users.filter((u) => u.role === 'USER').length, icon: '👤', tone: 'orange' },
])

const allEquipment = ref([])

const selectedUser = computed(
  () => users.find((u) => u.userId === selectedUserId.value) ?? users[0] ?? null,
)

const filteredUsers = computed(() => {
  const query = userSearch.value.trim().toLowerCase()
  if (!query) return users
  return users.filter((user) =>
    [user.name, user.roleLabel, user.equipment, user.statusLabel, user.email]
      .join(' ')
      .toLowerCase()
      .includes(query),
  )
})

const filteredEquipmentOptions = computed(() => {
  const q = equipSearch.value.trim().toLowerCase()
  if (!q) return allEquipment.value
  return allEquipment.value.filter((e) => e.name.toLowerCase().includes(q))
})

const isDetailEditable = computed(() => isAddingUser.value || isEditingUser.value)

const activeAssignedEquipIds = computed(() => {
  if (isDetailEditable.value) return newUser.assignedEquipIds
  return selectedUser.value?.assignedEquipIds ?? []
})

const selectedEquipTags = computed(() =>
  allEquipment.value.filter((e) => activeAssignedEquipIds.value.includes(e.id)),
)

const selectedCount = computed(() => checkedUserIds.value.length)

const isUserChecked = (userId) => checkedUserIds.value.includes(userId)

const toggleUserCheck = (userId) => {
  if (isUserChecked(userId)) {
    checkedUserIds.value = checkedUserIds.value.filter((v) => v !== userId)
  } else {
    checkedUserIds.value = [...checkedUserIds.value, userId]
  }
}

const toggleAllUsers = (event) => {
  checkedUserIds.value = event.target.checked ? filteredUsers.value.map((u) => u.userId) : []
}

const selectUser = async (userId) => {
  isAddingUser.value = false
  isEditingUser.value = false
  selectedUserId.value = userId
  equipSearch.value = ''
  try {
    const equips = await getUserEquipments(userId)
    const user = users.find((u) => u.userId === userId)
    if (user) {
      user.assignedEquipIds = equips.map((e) => e.equipmentId)
      user.equipment = equips.map((e) => e.equipmentId).join(', ')
    }
  } catch {
    // 설비 로드 실패 시 무시
  }
  Object.assign(newUser, createDraftFromUser(selectedUser.value))
}

const editUser = (userId = selectedUserId.value) => {
  isAddingUser.value = false
  isEditingUser.value = true
  selectedUserId.value = userId
  Object.assign(newUser, createDraftFromUser(selectedUser.value))
  equipSearch.value = ''
}

const resetNewUser = () => {
  Object.assign(newUser, createEmptyUser())
}

const startAddUser = () => {
  resetNewUser()
  equipSearch.value = ''
  isAddingUser.value = true
  isEditingUser.value = true
}

const cancelAddUser = () => {
  isAddingUser.value = false
  isEditingUser.value = false
  Object.assign(newUser, createDraftFromUser(selectedUser.value))
}

const isEquipAssigned = (equipId) => activeAssignedEquipIds.value.includes(equipId)

const toggleEquip = (equipId) => {
  if (!isDetailEditable.value) return
  if (newUser.assignedEquipIds.includes(equipId)) {
    newUser.assignedEquipIds = newUser.assignedEquipIds.filter((id) => id !== equipId)
  } else {
    newUser.assignedEquipIds.push(equipId)
  }
}

const removeEquipTag = (equipId) => {
  if (activeAssignedEquipIds.value.includes(equipId)) {
    toggleEquip(equipId)
  }
}

const canCreateUser = computed(() =>
  newUser.name.trim() !== '' &&
  newUser.email.trim() !== '' &&
  newUser.password.trim() !== '',
)

const canSaveUser = computed(() =>
  newUser.name.trim() !== '' &&
  newUser.email.trim() !== '',
)

const loadUsers = async (selectAfter = null) => {
  loading.value = true
  errorMsg.value = ''
  try {
    const apiUsers = await getUsers()
    const equipsList = await Promise.all(
      apiUsers.map((u) => getUserEquipments(u.userId).catch(() => [])),
    )
    users.splice(0, users.length, ...apiUsers.map((u, i) => mapUserFromApi(u, equipsList[i])))

    const targetId = selectAfter ?? selectedUserId.value
    const stillExists = users.some((u) => u.userId === targetId)

    if (users.length > 0) {
      await selectUser(stillExists ? targetId : users[0].userId)
    } else {
      selectedUserId.value = null
      isAddingUser.value = true
      isEditingUser.value = true
      resetNewUser()
    }

    checkedUserIds.value = users.map((u) => u.userId)
  } catch (e) {
    errorMsg.value = e.message
  } finally {
    loading.value = false
  }
}

const saveNewUser = async () => {
  if (!canCreateUser.value) return
  loading.value = true
  errorMsg.value = ''
  try {
    const created = await apiCreateUser({
      username: newUser.name.trim(),
      email: newUser.email.trim(),
      password: newUser.password.trim(),
      role: newUser.role,
    })
    if (newUser.assignedEquipIds.length > 0) {
      await assignEquipmentsBatch(created.userId, newUser.assignedEquipIds)
    }
    isAddingUser.value = false
    isEditingUser.value = false
    await loadUsers(created.userId)
  } catch (e) {
    errorMsg.value = e.message
  } finally {
    loading.value = false
  }
}

const saveSelectedUser = async () => {
  if (!canSaveUser.value) return
  loading.value = true
  errorMsg.value = ''
  try {
    const currentId = selectedUserId.value
    const payload = {
      username: newUser.name.trim(),
      email: newUser.email.trim(),
      role: newUser.role,
      status: newUser.status,
    }
    if (newUser.password.trim()) payload.password = newUser.password.trim()
    await apiUpdateUser(currentId, payload)
    await assignEquipmentsBatch(currentId, newUser.assignedEquipIds)
    isEditingUser.value = false
    await loadUsers(currentId)
  } catch (e) {
    errorMsg.value = e.message
  } finally {
    loading.value = false
  }
}

const showDeleteConfirm = ref(false)
const pendingDeleteIds = ref([])

const requestDelete = (ids) => {
  if (ids.length === 0) return
  pendingDeleteIds.value = ids
  showDeleteConfirm.value = true
}

const cancelDelete = () => {
  showDeleteConfirm.value = false
  pendingDeleteIds.value = []
}

const deleteUsersByIds = async (ids) => {
  if (ids.length === 0) return
  loading.value = true
  errorMsg.value = ''
  try {
    await Promise.all(ids.map((userId) => apiDeleteUser(userId)))
    checkedUserIds.value = checkedUserIds.value.filter((id) => !ids.includes(id))
    await loadUsers()
  } catch (e) {
    errorMsg.value = e.message
  } finally {
    loading.value = false
  }
}

const confirmDelete = async () => {
  showDeleteConfirm.value = false
  await deleteUsersByIds(pendingDeleteIds.value)
  pendingDeleteIds.value = []
}

const deleteCheckedUsers = () => {
  requestDelete([...checkedUserIds.value])
}

const deleteSelectedUser = () => {
  if (!selectedUser.value) return
  requestDelete([selectedUser.value.userId])
}

const handleSave = () => {
  if (isAddingUser.value) {
    saveNewUser()
  } else {
    saveSelectedUser()
  }
}

const refreshUsers = () => {
  userSearch.value = ''
  equipSearch.value = ''
  isAddingUser.value = false
  isEditingUser.value = false
  loadUsers()
}

const loadEquipmentNames = async () => {
  try {
    const names = await getEquipmentNames()
    allEquipment.value = names.map((e) => ({ id: e.equipmentId, name: e.equipmentId }))
  } catch {
    // 설비 목록 로드 실패 시 빈 목록 유지
  }
}

onMounted(() => {
  loadEquipmentNames()
  loadUsers()
})
</script>

<template>
  <div class="permission-page">
    <AppTopbar active-menu="사용자 관리" />

    <!-- 삭제 확인 모달 -->
    <div v-if="showDeleteConfirm" class="confirm-overlay" @click.self="cancelDelete">
      <div class="confirm-modal">
        <p>정말 삭제하시겠습니까?</p>
        <div class="confirm-actions">
          <button type="button" class="btn navy" @click="confirmDelete">네</button>
          <button type="button" class="btn outline" @click="cancelDelete">아니오</button>
        </div>
      </div>
    </div>

    <main class="content">
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
            <button type="button" class="btn danger-outline" @click="deleteCheckedUsers">🗑 선택항목 삭제</button>
            <label class="user-search">
              <span>⌕</span>
              <input v-model="userSearch" type="search" placeholder="이름, 이메일, 역할, 담당 설비 검색" />
            </label>
            <button type="button" class="refresh-btn" title="새로고침" @click="refreshUsers">↻</button>
          </div>

          <div class="table-wrap">
            <table class="data-table">
              <thead>
                <tr>
                  <th class="col-check">
                    <input
                      type="checkbox"
                      :checked="filteredUsers.length > 0 && filteredUsers.every((user) => isUserChecked(user.id))"
                      @change="toggleAllUsers"
                    />
                  </th>
                  <th>이름</th>
                  <th>이메일</th>
                  <th>역할</th>
                  <th>담당 설비</th>
                  <th>상태</th>
                  <th>관리</th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="user in filteredUsers"
                  :key="user.userId"
                  :class="{ selected: selectedUserId === user.userId }"
                  @click="selectUser(user.userId)"
                >
                  <td class="col-check" @click.stop>
                    <input
                      type="checkbox"
                      :checked="isUserChecked(user.userId)"
                      @change="toggleUserCheck(user.userId)"
                    />
                  </td>
                  <td class="name-cell">{{ user.name }}</td>
                  <td>{{ user.email }}</td>
                  <td><span class="role-badge" :class="user.role.toLowerCase()">{{ user.roleLabel }}</span></td>
                  <td class="equip-cell">{{ user.equipment }}</td>
                  <td><span class="status-badge" :class="user.status">{{ user.statusLabel }}</span></td>
                  <td class="manage-cell" @click.stop>
                    <button type="button" class="icon-btn edit" title="수정" @click="editUser(user.userId)">✎</button>
                    <button type="button" class="icon-btn delete" title="삭제" @click="requestDelete([user.userId])">🗑</button>
                  </td>
                </tr>
                <tr v-if="filteredUsers.length === 0">
                  <td colspan="7" class="empty-row">검색 결과가 없습니다.</td>
                </tr>
              </tbody>
            </table>
          </div>

          <div class="table-footer">
            <span>선택된 사용자 {{ selectedCount }}명 | 전체 {{ filteredUsers.length }}건</span>
            <div class="pagination">
              <button type="button" class="active">1</button>
            </div>
          </div>
          </section>
        </div>

        <aside class="panel side-panel">
          <h2>사용자 상세</h2>

          <div class="profile-block">
            <dl v-if="!isDetailEditable && selectedUser" class="profile-list">
              <div><dt>이름</dt><dd>{{ selectedUser.name }}</dd></div>
              <div><dt>이메일</dt><dd>{{ selectedUser.email }}</dd></div>
              <div>
                <dt>역할</dt>
                <dd><span class="role-badge" :class="selectedUser.role.toLowerCase()">{{ selectedUser.roleLabel }}</span></dd>
              </div>
              <div>
                <dt>상태</dt>
                <dd><span class="status-badge" :class="selectedUser.status">{{ selectedUser.statusLabel }}</span></dd>
              </div>
            </dl>
            <p v-if="!isDetailEditable && !selectedUser && loading" class="loading-msg">불러오는 중...</p>
            <dl v-if="isDetailEditable" class="profile-list form-list">
              <div>
                <dt>이름</dt>
                <dd><input v-model="newUser.name" type="text" placeholder="이름 입력" :disabled="isEditingUser && !isAddingUser" /></dd>
              </div>
              <div>
                <dt>이메일</dt>
                <dd><input v-model="newUser.email" type="email" placeholder="email@company.com" :disabled="isEditingUser && !isAddingUser" /></dd>
              </div>
              <div v-if="isAddingUser">
                <dt>비밀번호</dt>
                <dd><input v-model="newUser.password" type="password" placeholder="비밀번호 입력" /></dd>
              </div>
              <div>
                <dt>역할</dt>
                <dd>
                  <select v-model="newUser.role">
                    <option v-for="role in roleOptions" :key="role.value" :value="role.value">
                      {{ role.label }}
                    </option>
                  </select>
                </dd>
              </div>
              <div>
                <dt>상태</dt>
                <dd>
                  <select v-model="newUser.status">
                    <option v-for="status in statusOptions" :key="status.value" :value="status.value">
                      {{ status.label }}
                    </option>
                  </select>
                </dd>
              </div>
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
                :disabled="!isDetailEditable"
                @change="toggleEquip(equip.id)"
              />
              <span>{{ equip.name }}</span>
            </label>
          </div>

          <h3>선택된 설비</h3>
          <div class="tag-list">
            <span v-for="tag in selectedEquipTags" :key="tag.id" class="equip-tag">
              {{ tag.name }}
              <button v-if="isDetailEditable" type="button" @click="removeEquipTag(tag.id)">×</button>
            </span>
          </div>

          <p v-if="errorMsg" class="side-error">{{ errorMsg }}</p>

          <div class="side-actions">
            <button
              v-if="!isDetailEditable"
              type="button"
              class="btn outline full"
              @click="editUser()"
            >
              수정
            </button>
            <button
              v-else
              type="button"
              class="btn navy full"
              :disabled="loading || (isAddingUser ? !canCreateUser : !canSaveUser)"
              @click="handleSave"
            >
              {{ loading ? '저장 중...' : '저장' }}
            </button>
            <button v-if="isDetailEditable" type="button" class="btn outline full" @click="cancelAddUser">취소</button>
            <button v-if="!isAddingUser" type="button" class="btn danger-outline full" :disabled="loading" @click="deleteSelectedUser">🗑 사용자 삭제</button>
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
  height: 100vh;
  overflow: hidden;
  background: #f5f7fb;
}

.content {
  height: calc(100vh - 78px);
  padding: 16px 32px 30px;
  box-sizing: border-box;
}

.main-layout {
  display: grid;
  grid-template-columns: 1fr 442px;
  gap: 16px;
  align-items: stretch;
  height: 100%;
  min-height: 0;
}

.left-column {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-width: 0;
  min-height: 0;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.summary-card {
  height: 142px;
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

.action-bar > .btn {
  flex-shrink: 0;
  min-width: 130px;
  height: 40px;
  padding: 0 18px;
  font-size: 14px;
}

.user-search {
  flex: 1;
  min-width: 240px;
  height: 40px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 12px;
  border: 1px solid #d8e1ed;
  border-radius: 6px;
  background: #fff;
}

.user-search span {
  color: #6b7c94;
  font-size: 15px;
  font-weight: 900;
}

.user-search input {
  flex: 1;
  min-width: 0;
  border: 0;
  outline: none;
  color: #0d2448;
  font-size: 13px;
  font-weight: 700;
}

.user-search input::placeholder {
  color: #9aa8bc;
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

.btn:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.refresh-btn {
  width: 40px;
  height: 40px;
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

.empty-row {
  color: #6b7c94;
  font-weight: 850;
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

.role-badge.user {
  background: #fff0df;
  color: #df7922;
}

.side-error {
  margin: 0 0 8px;
  padding: 8px 12px;
  border-radius: 6px;
  background: #fff0f0;
  border: 1px solid #ffcdd2;
  color: #c62828;
  font-size: 12px;
  font-weight: 700;
}

.loading-msg {
  margin: 8px 0;
  color: #9aa8bc;
  font-size: 13px;
  font-weight: 700;
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
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 14px;
  font-size: 13px;
  font-weight: 800;
  color: #4a5f7a;
}

.pagination {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
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
  height: 100%;
  min-height: 0;
  padding-top: 14px;
  padding-bottom: 14px;
}

.table-panel {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.table-wrap {
  flex: 1;
  min-height: 0;
  overflow: auto;
}

.side-panel h2 {
  margin: 0 0 10px;
  font-size: 18px;
  font-weight: 950;
  color: #0d2448;
}

.side-panel h3 {
  margin: 12px 0 8px;
  font-size: 18px;
  font-weight: 950;
  color: #0d2448;
}

.side-panel .tag-list + .side-actions,
.side-panel h3 + .tag-list {
  margin-top: 0;
}

.side-panel h3:nth-of-type(2) {
  font-size: 14px;
}

.profile-block {
  padding-bottom: 12px;
  border-bottom: 1px solid #edf1f6;
}

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
  padding: 4px 0;
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

.form-list input,
.form-list select {
  width: 100%;
  height: 34px;
  border: 1px solid #d8e1ed;
  border-radius: 6px;
  padding: 0 10px;
  color: #0d2448;
  background: #fff;
  font-size: 13px;
  font-weight: 800;
}

.form-list input::placeholder {
  color: #9aa8bc;
}

.equip-search {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 32px;
  padding: 0 12px;
  border: 1px solid #d8e1ed;
  border-radius: 6px;
  margin-bottom: 10px;
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
  grid-auto-flow: column;
  grid-template-rows: repeat(3, 32px);
  grid-auto-columns: 120px;
  gap: 6px 8px;
  overflow-x: auto;
  overflow-y: hidden;
  padding-bottom: 8px;
}

.equip-check {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  height: 32px;
  font-size: 13px;
  font-weight: 800;
  color: #0d2448;
  cursor: pointer;
}

.equip-check span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  min-height: 32px;
  margin-bottom: 12px;
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
  margin-top: 10px;
}

.side-actions .btn {
  height: 34px;
}

@media (max-width: 1400px) {
  .permission-page {
    min-width: 1200px;
  }
}

.confirm-overlay {
  position: fixed;
  inset: 0;
  background: rgba(13, 36, 72, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.confirm-modal {
  background: #fff;
  border-radius: 14px;
  padding: 32px 36px;
  box-shadow: 0 8px 32px rgba(13, 36, 72, 0.18);
  min-width: 260px;
  text-align: center;
}

.confirm-modal p {
  margin: 0 0 24px;
  font-size: 16px;
  font-weight: 900;
  color: #0d2448;
}

.confirm-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.confirm-actions .btn {
  min-width: 80px;
  height: 38px;
}

.form-list input:disabled {
  background: #f5f7fb;
  color: #9aa8bc;
  cursor: not-allowed;
}
</style>
