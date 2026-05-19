const API_BASE = 'http://localhost:8080'

const authHeaders = () => {
  const token = localStorage.getItem('token')
  return {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
  }
}

export async function getUsers({ role, status, page = 0, size = 100 } = {}) {
  const params = new URLSearchParams({ page, size })
  if (role) params.append('role', role)
  if (status) params.append('status', status)
  const res = await fetch(`${API_BASE}/api/users?${params}`, {
    headers: authHeaders(),
  })
  const data = await res.json()
  if (!res.ok) throw new Error(data.message || '사용자 목록 조회 실패')
  return data.data
}

export async function createUser({ username, email, password, role }) {
  const res = await fetch(`${API_BASE}/api/users`, {
    method: 'POST',
    headers: authHeaders(),
    body: JSON.stringify({ username, email, password, role }),
  })
  const data = await res.json()
  if (!res.ok) throw new Error(data.message || '사용자 생성 실패')
  return data
}

export async function updateUser(userId, payload) {
  const res = await fetch(`${API_BASE}/api/users/${userId}`, {
    method: 'PUT',
    headers: authHeaders(),
    body: JSON.stringify(payload),
  })
  const data = await res.json()
  if (!res.ok) throw new Error(data.message || '사용자 수정 실패')
  return data
}

export async function deleteUser(userId) {
  const res = await fetch(`${API_BASE}/api/users/${userId}`, {
    method: 'DELETE',
    headers: authHeaders(),
  })
  const data = await res.json()
  if (!res.ok) throw new Error(data.message || '사용자 삭제 실패')
  return data
}

export async function getUserEquipments(userId) {
  const res = await fetch(`${API_BASE}/api/users/${userId}/equipments`, {
    headers: authHeaders(),
  })
  const data = await res.json()
  if (!res.ok) throw new Error(data.message || '설비 목록 조회 실패')
  return data.data
}

export async function assignEquipmentsBatch(userId, equipmentIds) {
  const res = await fetch(`${API_BASE}/api/users/${userId}/equipments/batch`, {
    method: 'POST',
    headers: authHeaders(),
    body: JSON.stringify({ equipmentIds }),
  })
  const data = await res.json()
  if (!res.ok) throw new Error(data.message || '설비 배정 실패')
  return data.data
}
