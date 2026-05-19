package com.example.backend.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.backend.dto.LogEntryDto;
import com.example.backend.mapper.LogMapper;

@Service
public class LogService {

    private static final DateTimeFormatter MYSQL_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private LogMapper logMapper;

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

    public List<LogEntryDto> findPanelFeederLogs(String equipmentId, String fromStr, String toStr) {
        String[] range = resolveDateRange(fromStr, toStr);
        return logMapper.findPanelFeederLogs(equipmentId, range[0], range[1]);
    }

    public List<LogEntryDto> findBodyJigLogs(String equipmentId, String fromStr, String toStr) {
        String[] range = resolveDateRange(fromStr, toStr);
        return logMapper.findBodyJigLogs(equipmentId, range[0], range[1]);
    }

    public List<LogEntryDto> findRobotLogs(String equipmentId, String fromStr, String toStr) {
        String[] range = resolveDateRange(fromStr, toStr);
        return logMapper.findRobotLogs(equipmentId, range[0], range[1]);
    }

    public List<LogEntryDto> findSpotWelderLogs(String equipmentId, String fromStr, String toStr) {
        String[] range = resolveDateRange(fromStr, toStr);
        return logMapper.findSpotWelderLogs(equipmentId, range[0], range[1]);
    }

    public List<LogEntryDto> findSealerLogs(String equipmentId, String fromStr, String toStr) {
        String[] range = resolveDateRange(fromStr, toStr);
        return logMapper.findSealerLogs(equipmentId, range[0], range[1]);
    }

    public List<LogEntryDto> findConveyorLogs(String equipmentId, String fromStr, String toStr) {
        String[] range = resolveDateRange(fromStr, toStr);
        return logMapper.findConveyorLogs(equipmentId, range[0], range[1]);
    }

    public List<LogEntryDto> findVisionInspectionLogs(String equipmentId, String fromStr, String toStr) {
        String[] range = resolveDateRange(fromStr, toStr);
        return logMapper.findVisionInspectionLogs(equipmentId, range[0], range[1]);
    }

    public List<LogEntryDto> findLatestByEquipmentCode(String equipmentId) {
        if (equipmentId == null || equipmentId.isBlank()) return Collections.emptyList();
        return logMapper.findLatestByEquipmentCode(equipmentId);
    }

    public List<LogEntryDto> findSensorLogs(String equipmentId, String fromStr, String toStr) {
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

    private String getPrefix(String equipmentId) {
        int idx = equipmentId.indexOf('-');
        return idx > 0 ? equipmentId.substring(0, idx) : equipmentId.substring(0, Math.min(3, equipmentId.length()));
    }
}
