const MOCK_DELAY = 250

// API 응답 맵핑: equipmentId -> 설비 정보
const EQUIPMENT_DATA = {
  'robot-a1': {
    equipment_id: 'robot-a1',
    equipment_name: 'Robot A1',
    line_no: 'Line 1',
    zone: 'Zone A',
    cycle_time: 12.8,
    manufacturer: 'ABB',
    replacement_date: '2026-05-24',
    type: 'robot',
  },
  'nutrunner-a2': {
    equipment_id: 'nutrunner-a2',
    equipment_name: 'Nutrunner A2',
    line_no: 'Line 1',
    zone: 'Zone A',
    cycle_time: 10.5,
    manufacturer: 'Atlas Copco',
    replacement_date: '2027-03-15',
    type: 'nutrunner',
  },
  'robot-a3': {
    equipment_id: 'robot-a3',
    equipment_name: 'Robot A3',
    line_no: 'Line 2',
    zone: 'Zone A',
    cycle_time: 12.2,
    manufacturer: 'ABB',
    replacement_date: '2026-09-10',
    type: 'robot',
  },
  'nutrunner-a4': {
    equipment_id: 'nutrunner-a4',
    equipment_name: 'Nutrunner A4',
    line_no: 'Line 1',
    zone: 'Zone B',
    cycle_time: 11.0,
    manufacturer: 'Atlas Copco',
    replacement_date: '2027-06-20',
    type: 'nutrunner',
  },
  'robot-b1': {
    equipment_id: 'robot-b1',
    equipment_name: 'Robot B1',
    line_no: 'Line 2',
    zone: 'Zone B',
    cycle_time: 13.1,
    manufacturer: 'ABB',
    replacement_date: '2026-12-05',
    type: 'robot',
  },
  'nutrunner-b2': {
    equipment_id: 'nutrunner-b2',
    equipment_name: 'Nutrunner B2',
    line_no: 'Line 1',
    zone: 'Zone C',
    cycle_time: 9.8,
    manufacturer: 'Atlas Copco',
    replacement_date: '2027-02-28',
    type: 'nutrunner',
  },
  'robot-b3': {
    equipment_id: 'robot-b3',
    equipment_name: 'Robot B3',
    line_no: 'Line 2',
    zone: 'Zone C',
    cycle_time: 12.5,
    manufacturer: 'ABB',
    replacement_date: '2026-08-12',
    type: 'robot',
  },
}

// 설비별 센서 데이터 (로그 데이터)
const SENSOR_DATA_MAP = {
  'robot-a1': {
    log_id: 'log-001',
    equipment_id: 'robot-a1',
    timestamp: '2024-05-24 10:30:45',
    status: 'running',
    weld_voltage_dc: 24.5,
    weld_current_dc: 150.2,
    weld_voltage_ac: 0,
    weld_current_ac: 0,
    weld_speed: 120,
  },
  'nutrunner-a2': {
    log_id: 'log-002',
    equipment_id: 'nutrunner-a2',
    timestamp: '2024-05-24 10:30:40',
    status: 'running',
    weld_voltage_dc: 0,
    weld_current_dc: 0,
    weld_voltage_ac: 380,
    weld_current_ac: 25.3,
    weld_speed: 0,
  },
  'robot-a3': {
    log_id: 'log-003',
    equipment_id: 'robot-a3',
    timestamp: '2024-05-24 10:30:35',
    status: 'idle',
    weld_voltage_dc: 0,
    weld_current_dc: 0,
    weld_voltage_ac: 0,
    weld_current_ac: 0,
    weld_speed: 0,
  },
  'nutrunner-a4': {
    log_id: 'log-004',
    equipment_id: 'nutrunner-a4',
    timestamp: '2024-05-24 10:30:30',
    status: 'running',
    weld_voltage_dc: 0,
    weld_current_dc: 0,
    weld_voltage_ac: 380,
    weld_current_ac: 22.1,
    weld_speed: 0,
  },
  'robot-b1': {
    log_id: 'log-005',
    equipment_id: 'robot-b1',
    timestamp: '2024-05-24 10:30:25',
    status: 'running',
    weld_voltage_dc: 25.2,
    weld_current_dc: 155.8,
    weld_voltage_ac: 0,
    weld_current_ac: 0,
    weld_speed: 125,
  },
  'nutrunner-b2': {
    log_id: 'log-006',
    equipment_id: 'nutrunner-b2',
    timestamp: '2024-05-24 10:30:20',
    status: 'stop',
    weld_voltage_dc: 0,
    weld_current_dc: 0,
    weld_voltage_ac: 0,
    weld_current_ac: 0,
    weld_speed: 0,
  },
  'robot-b3': {
    log_id: 'log-007',
    equipment_id: 'robot-b3',
    timestamp: '2024-05-24 10:30:15',
    status: 'running',
    weld_voltage_dc: 24.8,
    weld_current_dc: 152.5,
    weld_voltage_ac: 0,
    weld_current_ac: 0,
    weld_speed: 122,
  },
}

// 가동 시간 데이터
const RUNNING_TIME_MAP = {
  'robot-a1': '02:45:12',
  'nutrunner-a2': '02:30:18',
  'robot-a3': '00:00:00',
  'nutrunner-a4': '02:44:01',
  'robot-b1': '02:04:10',
  'nutrunner-b2': '00:00:00',
  'robot-b3': '01:58:44',
}

const mockData = {
  zoneRows: [
    {
      line: 'Zone A',
      items: [
        { label: 'Robot A1', status: 'running' },
        { label: 'Nutrunner A2', status: 'running' },
        { label: 'Robot A3', status: 'idle' },
        { label: 'Nutrunner A4', status: 'running' },
      ],
    },
    {
      line: 'Zone B',
      items: [
        { label: 'Robot B1', status: 'running' },
        { label: 'Nutrunner B2', status: 'stop' },
        { label: 'Robot B3', status: 'running' },
      ],
    },
    {
      line: 'Zone C',
      items: [
        { label: 'Nutrunner C1', status: 'running' },
        { label: 'Robot C2', status: 'idle' },
        { label: 'Nutrunner C3', status: 'running' },
      ],
    },
    {
      line: 'Zone D',
      items: [
        { label: 'Nutrunner D1', status: 'running' },
        { label: 'Robot D2', status: 'running' },
        { label: 'Robot D3', status: 'stop' },
      ],
    },
  ],

  equipmentRows: [
    { name: 'Robot A1', type: '용접 로봇', status: 'running', alarm: 'normal', uptime: '18:24:12', lastCheck: '2024-05-24 09:00' },
    { name: 'Nutrunner A2', type: '너트러너', status: 'running', alarm: 'normal', uptime: '17:58:11', lastCheck: '2024-05-24 08:30' },
    { name: 'Robot A3', type: '용접 로봇', status: 'idle', alarm: 'normal', uptime: '05:12:58', lastCheck: '2024-05-23 18:20' },
    { name: 'Nutrunner A4', type: '너트러너', status: 'running', alarm: 'normal', uptime: '09:04:30', lastCheck: '2024-05-24 07:50' },
    { name: 'Robot B1', type: '용접 로봇', status: 'running', alarm: 'normal', uptime: '20:03:17', lastCheck: '2024-05-24 09:10' },
    { name: 'Nutrunner B2', type: '너트러너', status: 'stop', alarm: 'danger', uptime: '02:13:08', lastCheck: '2024-05-24 10:05' },
    { name: 'Robot B3', type: '용접 로봇', status: 'running', alarm: 'normal', uptime: '16:42:09', lastCheck: '2024-05-24 08:40' },
  ],

  recentAlarms: [
    { title: 'Nutrunner B2 고장', time: '2024-05-24 10:10', level: '위험' },
    { title: 'Robot D3 비가동', time: '2024-05-24 10:18', level: '경고' },
    { title: 'Robot A3 상태 변경', time: '2024-05-24 09:55', level: '정보' },
  ],

  equipmentDetail: {
    name: 'Robot A1',
    model: 'ABB',
    id: 'RB-A1-001',
    location: 'Zone A - Line 1',
    status: 'running',
    uptime: '02:45:12',
    temperature: '42.6°C',
    current: '7.2 A',
    speed: '12.8 m/s',
    output: '1,256 EA',
    lastUpdate: '2024-05-24 10:30:45',
  },
}

const delay = (value) =>
  new Promise((resolve) => {
    setTimeout(() => resolve(value), MOCK_DELAY)
  })

export function fetchZoneRows() {
  return delay(mockData.zoneRows)
}

export function fetchEquipmentRows() {
  return delay(mockData.equipmentRows)
}

export function fetchRecentAlarms() {
  return delay(mockData.recentAlarms)
}

export function fetchEquipmentDetail() {
  return delay(mockData.equipmentDetail)
}

/**
 * GET /equipment
 * 모든 설비 목록 반환
 */
export function fetchEquipments() {
  return delay(Object.values(EQUIPMENT_DATA))
}

/**
 * GET /equipments/{equipmentId}/runningtime
 * 특정 설비의 가동시간 반환
 */
export function fetchEquipmentRunningTime(equipmentId) {
  const runningTime = RUNNING_TIME_MAP[equipmentId] || '00:00:00'
  return delay({
    equipment_id: equipmentId,
    running_time: runningTime,
    timestamp: '2024-05-24 10:30:45',
  })
}

/**
 * GET /logs/equipments/{equipmentId}/latest
 * 특정 설비의 최신 로그 데이터 반환
 * 응답: {log_id, equipment_id, timestamp, status, weld_voltage_dc, weld_current_dc, weld_voltage_ac, weld_current_ac, weld_speed}
 */
export function fetchLatestLog(equipmentId) {
  const logData = SENSOR_DATA_MAP[equipmentId] || {
    log_id: 'log-000',
    equipment_id: equipmentId,
    timestamp: '2024-05-24 10:30:45',
    status: 'unknown',
    weld_voltage_dc: 0,
    weld_current_dc: 0,
    weld_voltage_ac: 0,
    weld_current_ac: 0,
    weld_speed: 0,
  }
  return delay(logData)
}
