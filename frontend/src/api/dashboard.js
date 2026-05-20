const API_BASE = 'http://localhost:8080'

const authHeaders = () => {
  const token = localStorage.getItem('token')
  return {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
  }
}

const readJson = async (res, fallbackMessage) => {
  const data = await res.json().catch(() => ({}))
  if (!res.ok) throw new Error(data.message || fallbackMessage)
  return data.data ?? data
}

export async function getEquipmentStatus() {
  const res = await fetch(`${API_BASE}/api/dashboard/equipment-status`, {
    headers: authHeaders(),
  })
  return readJson(res, '설비 현황 조회 실패')
}
