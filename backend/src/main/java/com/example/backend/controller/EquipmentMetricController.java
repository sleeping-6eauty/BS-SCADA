package com.example.backend.controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.domain.Equipment;
import com.example.backend.domain.EquipmentStatusLog;
import com.example.backend.dto.ApiResponse;
import com.example.backend.dto.EquipmentResponse;
import com.example.backend.dto.LogEntryDto;
import com.example.backend.dto.dashboard.DashboardAlarmDto;
import com.example.backend.dto.equipment.EquipmentMetricsDto;
import com.example.backend.dto.equipment.EquipmentNameDto;
import com.example.backend.dto.equipment.EquipmentRemainingLifeDto;
import com.example.backend.dto.equipment.EquipmentSummaryDto;
import com.example.backend.mapper.DashboardMapper;
import com.example.backend.service.EquipmentService;

@RestController
@RequestMapping("/api/equipments")
public class EquipmentMetricController {

    private static final DateTimeFormatter MYSQL_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final EquipmentService equipmentService;
    private final DashboardMapper dashboardMapper;

    @Autowired
    public EquipmentMetricController(EquipmentService equipmentService, DashboardMapper dashboardMapper) {
        this.equipmentService = equipmentService;
        this.dashboardMapper = dashboardMapper;
    }

    // ──────────────────────────────────────────────────────────────
    //  GET /api/equipments
    //  전체 설비 목록 (equipment_id, name, line_no, zone, cycle_time,
    //                  manufacturer, replacement_date 등 전체 정보)
    // ──────────────────────────────────────────────────────────────
    @GetMapping
    public ResponseEntity<ApiResponse<List<EquipmentResponse>>> listEquipments(
            @RequestParam(required = false) String zone) {
        List<EquipmentResponse> responses = equipmentService.listEquipments(zone, null, null)
                .stream()
                .map(e -> new EquipmentResponse(
                        e.getEquipmentId(), e.getEquipmentName(), e.getLineNo(), e.getZone(),
                        e.getManufacturer(), e.getCycleTime(), e.getHealthScore(),
                        e.getRemainingLife(), e.getReplacementDate()))
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(new ApiResponse<>(true, "Equipment list retrieved", responses));
    }

    // ──────────────────────────────────────────────────────────────
    //  GET /api/equipments/names
    //  전체 설비명 목록 {equipmentId, equipmentName}
    // ──────────────────────────────────────────────────────────────
    @GetMapping("/names")
    public ResponseEntity<ApiResponse<List<EquipmentNameDto>>> listEquipmentNames() {
        List<EquipmentNameDto> names = equipmentService.listEquipmentNames();
        return ResponseEntity.ok(new ApiResponse<>(true, "Equipment names retrieved", names));
    }

    // ──────────────────────────────────────────────────────────────
    //  GET /api/equipments/{equipmentId}/summary?from=...&to=...
    // ──────────────────────────────────────────────────────────────
    @GetMapping("/{equipmentId}/summary")
    public ResponseEntity<ApiResponse<EquipmentSummaryDto>> getEquipmentSummary(
            @PathVariable String equipmentId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {

        // Try equipment_summary table first
        EquipmentSummaryDto dto = equipmentService.getEquipmentSummary(equipmentId, from, to);

        if (dto == null) {
            // Calculate from raw logs
            dto = calcSummaryFromRaw(equipmentId, from, to);
        }

        if (dto == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(new ApiResponse<>(true, "Equipment summary retrieved", dto));
    }

    // ──────────────────────────────────────────────────────────────
    //  GET /api/equipments/{equipmentId}/metrics?from=...&to=...
    // ──────────────────────────────────────────────────────────────
    @GetMapping("/{equipmentId}/metrics")
    public ResponseEntity<ApiResponse<EquipmentMetricsDto>> getEquipmentMetrics(
            @PathVariable String equipmentId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {

        String[] range = resolveDateRange(from, to);
        String fromMysql = range[0];
        String toMysql = range[1];

        LocalDateTime fromDt = LocalDateTime.parse(fromMysql, MYSQL_FMT);
        LocalDateTime toDt = LocalDateTime.parse(toMysql, MYSQL_FMT);
        long plannedTimeSec = ChronoUnit.SECONDS.between(fromDt, toDt);

        java.util.Map<String, Object> durations = dashboardMapper.getStatusDurations(equipmentId, fromMysql, toMysql);
        long runTimeSec = toLong(durations.get("run_time_sec"));
        long alarmTimeSec = toLong(durations.get("alarm_time_sec"));

        int failureCount = dashboardMapper.getEquipmentFailureCount(equipmentId, fromMysql, toMysql);
        double availability = plannedTimeSec > 0 ? (double) runTimeSec / plannedTimeSec : 0.0;
        long mttfSec = failureCount > 0 ? runTimeSec / failureCount : 0;
        long mttrSec = failureCount > 0 ? alarmTimeSec / failureCount : 0;
        long mtbfSec = mttfSec + mttrSec;

        int alarmCount24h = dashboardMapper.getAlarmCount24h(equipmentId);

        Equipment equipment = equipmentService.findByEquipmentCode(equipmentId);
        double healthScore = equipment != null && equipment.getHealthScore() != null
                ? equipment.getHealthScore() : 0.0;
        double remainingLife = equipment != null && equipment.getRemainingLife() != null
                ? equipment.getRemainingLife() : 0.0;

        // Sensor abnormal ratio (last 1 hour)
        double sensorPenalty = 0.0;
        double sensorAbnormalRatio = 0.0;
        java.util.Map<String, Object> sensorData = getSensorAbnormalData(equipmentId);
        if (sensorData != null && !sensorData.isEmpty()) {
            long total = toLong(sensorData.get("totalCount"));
            if (total > 0) {
                sensorAbnormalRatio = (double) toLong(sensorData.get("abnormalCount")) / total;
            }
        }

        EquipmentMetricsDto dto = new EquipmentMetricsDto();
        dto.setAvailability(availability);
        dto.setFailureCount(failureCount);
        dto.setMttfSec(mttfSec);
        dto.setMttrSec(mttrSec);
        dto.setMtbfSec(mtbfSec);
        dto.setHealthScore(healthScore);
        dto.setRemainingLife(remainingLife);
        dto.setReplacementDate(equipment != null ? equipment.getReplacementDate() : null);
        dto.setAlarmCount24h(alarmCount24h);
        dto.setSensorAbnormalRatio(sensorAbnormalRatio);

        return ResponseEntity.ok(new ApiResponse<>(true, "Equipment metrics retrieved", dto));
    }

    // ──────────────────────────────────────────────────────────────
    //  GET /api/equipments/{equipmentId}/status-history?from=...&to=...
    // ──────────────────────────────────────────────────────────────
    @GetMapping("/{equipmentId}/status-history")
    public ResponseEntity<ApiResponse<List<EquipmentStatusLog>>> getStatusHistory(
            @PathVariable String equipmentId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        List<EquipmentStatusLog> history = equipmentService.getEquipmentStatusHistory(equipmentId, from, to);
        return ResponseEntity.ok(new ApiResponse<>(true, "Status history retrieved", history));
    }

    // ──────────────────────────────────────────────────────────────
    //  GET /api/equipments/{equipmentId}/sensor-logs?from=...&to=...
    // ──────────────────────────────────────────────────────────────
    @GetMapping("/{equipmentId}/sensor-logs")
    public ResponseEntity<ApiResponse<List<LogEntryDto>>> getSensorLogs(
            @PathVariable String equipmentId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        List<LogEntryDto> logs = equipmentService.getEquipmentSensorLogs(equipmentId, from, to);
        return ResponseEntity.ok(new ApiResponse<>(true, "Sensor logs retrieved", logs));
    }

    // ──────────────────────────────────────────────────────────────
    //  GET /api/equipments/{equipmentId}/remaining-life
    //  수명 정보 {equipmentId, equipmentName, remainingLife, replacementDate}
    // ──────────────────────────────────────────────────────────────
    @GetMapping("/{equipmentId}/remaining-life")
    public ResponseEntity<ApiResponse<EquipmentRemainingLifeDto>> getRemainingLife(
            @PathVariable String equipmentId) {
        EquipmentRemainingLifeDto dto = equipmentService.getRemainingLife(equipmentId);
        if (dto == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(new ApiResponse<>(true, "Remaining life retrieved", dto));
    }

    // ──────────────────────────────────────────────────────────────
    //  GET /api/equipments/{equipmentId}/runtime?from=...&to=...
    //  가동시간 (RUN 상태 누적 초)
    //  from/to 미지정 시 최근 1시간 기준
    // ──────────────────────────────────────────────────────────────
    @GetMapping("/{equipmentId}/runtime")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> getRuntime(
            @PathVariable String equipmentId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {

        String[] range = resolveDateRange(from, to);
        java.util.Map<String, Object> durations =
                dashboardMapper.getStatusDurations(equipmentId, range[0], range[1]);
        long runTimeSec = toLong(durations.get("run_time_sec"));

        Double accumulatedRunHours = dashboardMapper.getTotalAccumulatedRunHours(equipmentId);

        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("equipmentId", equipmentId);
        result.put("from", range[0]);
        result.put("to", range[1]);
        result.put("runTimeSec", runTimeSec);
        result.put("runTimeHours", runTimeSec / 3600.0);
        result.put("accumulatedRunHours", accumulatedRunHours != null ? accumulatedRunHours : 0.0);

        return ResponseEntity.ok(new ApiResponse<>(true, "Runtime retrieved", result));
    }

    // ──────────────────────────────────────────────────────────────
    //  GET /api/equipments/{equipmentId}/alarms?from=...&to=...
    // ──────────────────────────────────────────────────────────────
    @GetMapping("/{equipmentId}/alarms")
    public ResponseEntity<ApiResponse<List<DashboardAlarmDto>>> getAlarms(
            @PathVariable String equipmentId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        List<DashboardAlarmDto> alarms = equipmentService.getEquipmentAlarms(equipmentId, from, to);
        return ResponseEntity.ok(new ApiResponse<>(true, "Equipment alarms retrieved", alarms));
    }

    // ──────────────────────────────────────────────────────────────
    //  Private helpers
    // ──────────────────────────────────────────────────────────────

    private EquipmentSummaryDto calcSummaryFromRaw(String equipmentId, String fromStr, String toStr) {
        Equipment equipment = equipmentService.findByEquipmentCode(equipmentId);
        if (equipment == null) return null;

        String[] range = resolveDateRange(fromStr, toStr);
        String fromMysql = range[0];
        String toMysql = range[1];

        LocalDateTime fromDt = LocalDateTime.parse(fromMysql, MYSQL_FMT);
        LocalDateTime toDt = LocalDateTime.parse(toMysql, MYSQL_FMT);
        long plannedTimeSec = ChronoUnit.SECONDS.between(fromDt, toDt);

        java.util.Map<String, Object> durations = dashboardMapper.getStatusDurations(equipmentId, fromMysql, toMysql);
        long runTimeSec = toLong(durations.get("run_time_sec"));
        long idleTimeSec = toLong(durations.get("idle_time_sec"));
        long stopTimeSec = toLong(durations.get("stop_time_sec"));
        long alarmTimeSec = toLong(durations.get("alarm_time_sec"));

        int failureCount = dashboardMapper.getEquipmentFailureCount(equipmentId, fromMysql, toMysql);
        double availability = plannedTimeSec > 0 ? (double) runTimeSec / plannedTimeSec : 0.0;
        long mttfSec = failureCount > 0 ? runTimeSec / failureCount : 0;
        long mttrSec = failureCount > 0 ? alarmTimeSec / failureCount : 0;
        long mtbfSec = mttfSec + mttrSec;

        EquipmentSummaryDto dto = new EquipmentSummaryDto();
        dto.setEquipmentId(equipmentId);
        dto.setEquipmentName(equipment.getEquipmentName());
        dto.setLineNo(equipment.getLineNo());
        dto.setZone(equipment.getZone());
        dto.setRunTimeSec(runTimeSec);
        dto.setIdleTimeSec(idleTimeSec);
        dto.setStopTimeSec(stopTimeSec);
        dto.setAlarmTimeSec(alarmTimeSec);
        dto.setAvailability(availability);
        dto.setFailureCount(failureCount);
        dto.setMttfSec(mttfSec);
        dto.setMttrSec(mttrSec);
        dto.setMtbfSec(mtbfSec);
        dto.setHealthScore(equipment.getHealthScore() != null ? equipment.getHealthScore() : 0.0);
        dto.setRemainingLife(equipment.getRemainingLife() != null ? equipment.getRemainingLife() : 0.0);
        dto.setReplacementDate(equipment.getReplacementDate());
        return dto;
    }

    private java.util.Map<String, Object> getSensorAbnormalData(String equipmentId) {
        String prefix = getPrefix(equipmentId);
        return switch (prefix) {
            case "PLF" -> dashboardMapper.getPanelFeederAbnormalCount(equipmentId);
            case "JIG" -> dashboardMapper.getBodyJigAbnormalCount(equipmentId);
            case "ROB" -> dashboardMapper.getRobotAbnormalCount(equipmentId);
            case "WLD" -> dashboardMapper.getSpotWelderAbnormalCount(equipmentId);
            case "SLR" -> dashboardMapper.getSealerAbnormalCount(equipmentId);
            case "CNV" -> dashboardMapper.getConveyorAbnormalCount(equipmentId);
            default -> java.util.Collections.emptyMap();
        };
    }

    private String getPrefix(String equipmentId) {
        if (equipmentId == null || equipmentId.isBlank()) return "";
        int idx = equipmentId.indexOf('-');
        return idx > 0 ? equipmentId.substring(0, idx) : equipmentId.substring(0, Math.min(3, equipmentId.length()));
    }

    private long toLong(Object val) {
        if (val == null) return 0L;
        if (val instanceof Number) return ((Number) val).longValue();
        return Long.parseLong(val.toString());
    }

    private String[] resolveDateRange(String fromStr, String toStr) {
        LocalDateTime to = parseDateTime(toStr);
        if (to == null) to = LocalDateTime.now();
        LocalDateTime from = parseDateTime(fromStr);
        if (from == null) from = to.minusHours(1);
        return new String[]{
                from.truncatedTo(ChronoUnit.SECONDS).format(MYSQL_FMT),
                to.truncatedTo(ChronoUnit.SECONDS).format(MYSQL_FMT)
        };
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) return null;
        String trimmed = value.trim();
        if (trimmed.contains("T")) return LocalDateTime.parse(trimmed);
        return LocalDateTime.parse(trimmed, MYSQL_FMT);
    }
}
