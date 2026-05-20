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

export async function getAlarmStatistics(period = 'day') {
  const params = new URLSearchParams({ period })
  const res = await fetch(`${API_BASE}/api/alarms/statistics?${params}`, {
    headers: authHeaders(),
  })
  return readJson(res, '알람 발생 추이 조회 실패')
}

export async function getEquipmentAlarmCount(equipmentId, days = 7) {
  const params = new URLSearchParams({ days })
  const res = await fetch(`${API_BASE}/api/alarms/${encodeURIComponent(equipmentId)}/count?${params}`, {
    headers: authHeaders(),
  })
  return readJson(res, '설비별 알람 건수 조회 실패')
}

export async function getEquipments() {
  const res = await fetch(`${API_BASE}/api/equipments`, {
    headers: authHeaders(),
  })
  return readJson(res, '전체 설비 목록 조회 실패')
}

export async function getAlarmLog() {
  const res = await fetch(`${API_BASE}/api/alarms/log`, {
    headers: authHeaders(),
  })
  return readJson(res, '알람 목록 조회 실패')
}

export async function getAlarmLogsByEquipment(equipmentId) {
  const res = await fetch(`${API_BASE}/api/alarms/${encodeURIComponent(equipmentId)}`, {
    headers: authHeaders(),
  })
  return readJson(res, '선택 설비 알람 목록 조회 실패')
}

export async function getEquipmentNames() {
  const res = await fetch(`${API_BASE}/api/equipments/names`, {
    headers: authHeaders(),
  })
  return readJson(res, '전체 설비 목록 조회 실패')
}

export async function getMyEquipments() {
  const res = await fetch(`${API_BASE}/api/auth/me/equipments`, {
    headers: authHeaders(),
  })
  return readJson(res, '담당 설비 목록 조회 실패')
}

export async function getAlarmDetail(equipmentId) {
  const res = await fetch(`${API_BASE}/api/alarms/detail/${encodeURIComponent(equipmentId)}`, {
    headers: authHeaders(),
  })
  return readJson(res, '알람 상세 정보 조회 실패')
}

export async function patchAlarmMemo(alarmId, { alarm_memo, alarm_status }) {
  const res = await fetch(`${API_BASE}/api/alarms/memo/${encodeURIComponent(alarmId)}`, {
    method: 'PATCH',
    headers: authHeaders(),
    body: JSON.stringify({ alarmMemo: alarm_memo, alarmStatus: alarm_status }),
  })
  return readJson(res, '알람 메모 수정 실패')
}
