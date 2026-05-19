package com.example.backend.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.backend.domain.Equipment;
import com.example.backend.domain.EquipmentStatus;
import com.example.backend.domain.EquipmentStatusLog;
import com.example.backend.dto.EquipmentCreateRequest;
import com.example.backend.dto.LogEntryDto;
import com.example.backend.dto.dashboard.DashboardAlarmDto;
import com.example.backend.dto.equipment.EquipmentNameDto;
import com.example.backend.dto.equipment.EquipmentRemainingLifeDto;
import com.example.backend.dto.equipment.EquipmentSummaryDto;
import com.example.backend.mapper.EquipmentMapper;
import com.example.backend.mapper.EquipmentMetricsMapper;
import com.example.backend.mapper.LogMapper;

@Service
public class EquipmentService {

    private static final DateTimeFormatter MYSQL_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final EquipmentMapper equipmentMapper;
    private final LogMapper logMapper;
    private final EquipmentMetricsMapper equipmentMetricsMapper;

    @Autowired
    public EquipmentService(EquipmentMapper equipmentMapper,
                            LogMapper logMapper,
                            EquipmentMetricsMapper equipmentMetricsMapper) {
        this.equipmentMapper = equipmentMapper;
        this.logMapper = logMapper;
        this.equipmentMetricsMapper = equipmentMetricsMapper;
    }

    // ──────────────────────────────────────────────────────────────
    //  DateTime helpers
    // ──────────────────────────────────────────────────────────────

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) return null;
        String trimmed = value.trim();
        if (trimmed.contains("T")) return LocalDateTime.parse(trimmed);
        return LocalDateTime.parse(trimmed, MYSQL_FMT);
    }

    private String fmt(LocalDateTime dt) {
        return dt.truncatedTo(ChronoUnit.SECONDS).format(MYSQL_FMT);
    }

    private String[] resolveDateRange(String fromStr, String toStr) {
        LocalDateTime to = parseDateTime(toStr);
        if (to == null) to = LocalDateTime.now();
        LocalDateTime from = parseDateTime(fromStr);
        if (from == null) from = to.minusHours(1);
        return new String[]{fmt(from), fmt(to)};
    }

    private String getPrefix(String equipmentId) {
        if (equipmentId == null || equipmentId.isBlank()) return "";
        int idx = equipmentId.indexOf('-');
        return idx > 0 ? equipmentId.substring(0, idx) : equipmentId.substring(0, Math.min(3, equipmentId.length()));
    }

    // ──────────────────────────────────────────────────────────────
    //  CRUD
    // ──────────────────────────────────────────────────────────────

    public List<Equipment> listEquipments(String zone, String status, Boolean assignedToMe) {
        List<Equipment> equipments = equipmentMapper.findAll();
        if (zone != null && !zone.isBlank()) {
            equipments = equipments.stream()
                    .filter(e -> e.getZone() != null && e.getZone().equalsIgnoreCase(zone))
                    .collect(Collectors.toList());
        }
        return equipments;
    }

    public Equipment createEquipment(EquipmentCreateRequest request) {
        Equipment equipment = new Equipment();
        equipment.setEquipmentId(request.getEquipmentId());
        equipment.setEquipmentName(request.getEquipmentName());
        equipment.setLineNo(request.getLineNo());
        equipment.setZone(request.getZone());
        equipment.setManufacturer(request.getManufacturer());
        equipment.setCycleTime(request.getCycleTime());
        equipment.setHealthScore(request.getHealthScore());
        equipment.setRemainingLife(request.getRemainingLife());
        equipment.setReplacementDate(request.getReplacementDate());
        equipmentMapper.insert(equipment);
        return equipment;
    }

    public Equipment findByEquipmentCode(String equipmentId) {
        return equipmentMapper.findByEquipmentCode(equipmentId);
    }

    public List<EquipmentNameDto> listEquipmentNames() {
        return equipmentMapper.findAll().stream()
                .map(e -> new EquipmentNameDto(e.getEquipmentId(), e.getEquipmentName()))
                .collect(Collectors.toList());
    }

    public EquipmentRemainingLifeDto getRemainingLife(String equipmentId) {
        Equipment equipment = equipmentMapper.findByEquipmentCode(equipmentId);
        if (equipment == null) return null;
        return new EquipmentRemainingLifeDto(equipment.getEquipmentId(), equipment.getEquipmentName(),
                equipment.getRemainingLife(), equipment.getReplacementDate());
    }

    public Equipment updateEquipment(String equipmentId, EquipmentCreateRequest request) {
        Equipment existing = equipmentMapper.findByEquipmentCode(equipmentId);
        if (existing == null) throw new IllegalArgumentException("Equipment not found");
        existing.setEquipmentName(request.getEquipmentName());
        existing.setLineNo(request.getLineNo());
        existing.setZone(request.getZone());
        existing.setManufacturer(request.getManufacturer());
        existing.setCycleTime(request.getCycleTime());
        existing.setHealthScore(request.getHealthScore());
        existing.setRemainingLife(request.getRemainingLife());
        existing.setReplacementDate(request.getReplacementDate());
        equipmentMapper.update(existing);
        return existing;
    }

    public void deleteEquipmentByCode(String equipmentId) {
        Equipment existing = equipmentMapper.findByEquipmentCode(equipmentId);
        if (existing == null) throw new IllegalArgumentException("Equipment not found");
        equipmentMapper.deleteById(existing.getEquipmentId());
    }

    // ──────────────────────────────────────────────────────────────
    //  Metrics / Summary
    // ──────────────────────────────────────────────────────────────

    public EquipmentSummaryDto getEquipmentSummary(String equipmentId, String fromStr, String toStr) {
        String[] range = resolveDateRange(fromStr, toStr);
        return equipmentMetricsMapper.findEquipmentSummary(equipmentId, range[0], range[1]);
    }

    public List<EquipmentStatusLog> getEquipmentStatusHistory(String equipmentId, String fromStr, String toStr) {
        String[] range = resolveDateRange(fromStr, toStr);
        return equipmentMetricsMapper.findEquipmentStatusHistory(equipmentId, range[0], range[1]);
    }

    public List<LogEntryDto> getEquipmentSensorLogs(String equipmentId, String fromStr, String toStr) {
        if (equipmentId == null || equipmentId.isBlank()) return Collections.emptyList();
        String prefix = getPrefix(equipmentId);
        String[] range = resolveDateRange(fromStr, toStr);
        return switch (prefix) {
            case "PLF" -> logMapper.findPanelFeederLogs(equipmentId, range[0], range[1]);
            case "JIG" -> logMapper.findBodyJigLogs(equipmentId, range[0], range[1]);
            case "ROB" -> logMapper.findRobotLogs(equipmentId, range[0], range[1]);
            case "WLD" -> logMapper.findSpotWelderLogs(equipmentId, range[0], range[1]);
            case "SLR" -> logMapper.findSealerLogs(equipmentId, range[0], range[1]);
            case "CNV" -> logMapper.findConveyorLogs(equipmentId, range[0], range[1]);
            case "VSI" -> logMapper.findVisionInspectionLogs(equipmentId, range[0], range[1]);
            default -> Collections.emptyList();
        };
    }

    public List<DashboardAlarmDto> getEquipmentAlarms(String equipmentId, String fromStr, String toStr) {
        String[] range = resolveDateRange(fromStr, toStr);
        return equipmentMetricsMapper.findEquipmentAlarms(equipmentId, range[0], range[1]);
    }
}
