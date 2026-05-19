package com.example.backend.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.backend.domain.Equipment;
import com.example.backend.dto.DashboardSummary;
import com.example.backend.dto.dashboard.DashboardAlarmDto;
import com.example.backend.dto.dashboard.DashboardOverviewDto;
import com.example.backend.dto.dashboard.EquipmentStatusCardDto;
import com.example.backend.dto.dashboard.LineOeeDto;
import com.example.backend.dto.dashboard.SummaryGenerateResponse;
import com.example.backend.mapper.DashboardMapper;
import com.example.backend.mapper.EquipmentMapper;

@Service
public class DashboardService {

    private static final long TARGET_MTBF_SEC = 28800;
    private static final long TARGET_MTTR_SEC = 1800;

    private static final Map<String, String> LINE_CONVEYOR_MAP = Map.of(
            "1", "CNV-001",
            "2", "CNV-002",
            "3", "CNV-003"
    );

    private static final DateTimeFormatter MYSQL_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private DashboardMapper dashboardMapper;

    @Autowired
    private EquipmentMapper equipmentMapper;

    // ──────────────────────────────────────────────────────────────
    //  DateTime helpers
    // ──────────────────────────────────────────────────────────────

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) return null;
        String trimmed = value.trim();
        if (trimmed.contains("T")) return LocalDateTime.parse(trimmed);
        return LocalDateTime.parse(trimmed, MYSQL_FMT);
    }

    private String fmt(LocalDateTime value) {
        return value.truncatedTo(ChronoUnit.SECONDS).format(MYSQL_FMT);
    }

    private LocalDateTime defaultTo(String toStr) {
        LocalDateTime to = parseDateTime(toStr);
        return to != null ? to : LocalDateTime.now();
    }

    private LocalDateTime defaultFrom(String fromStr, LocalDateTime to) {
        LocalDateTime from = parseDateTime(fromStr);
        return from != null ? from : to.minusHours(1);
    }

    // ──────────────────────────────────────────────────────────────
    //  Number helpers
    // ──────────────────────────────────────────────────────────────

    private long toLong(Object val) {
        if (val == null) return 0L;
        if (val instanceof Number) return ((Number) val).longValue();
        return Long.parseLong(val.toString());
    }

    private double toDouble(Object val) {
        if (val == null) return 0.0;
        if (val instanceof Number) return ((Number) val).doubleValue();
        return Double.parseDouble(val.toString());
    }

    // ──────────────────────────────────────────────────────────────
    //  Public API methods
    // ──────────────────────────────────────────────────────────────

    public DashboardSummary getSummary() {
        List<Equipment> equipments = equipmentMapper.findAll();
        double avgHealthScore = equipments.stream()
                .filter(e -> e.getHealthScore() != null)
                .mapToDouble(e -> e.getHealthScore())
                .average()
                .orElse(0.0);

        List<EquipmentStatusCardDto> cards = dashboardMapper.getEquipmentStatusCards();
        long running = cards.stream().filter(c -> "RUN".equals(c.getCurrentStatus())).count();
        long alarm = cards.stream().filter(c -> "ALARM".equals(c.getCurrentStatus())).count();

        // OEE from the first line for a quick summary
        LocalDateTime to = LocalDateTime.now();
        LocalDateTime from = to.minusHours(1);
        LineOeeDto oeeDto = computeLineOee("1", fmt(from), fmt(to));
        double oee = oeeDto != null ? oeeDto.getOee() : 0.0;

        return new DashboardSummary(oee, equipments.size(), running, alarm, avgHealthScore);
    }

    public DashboardOverviewDto getOverview(String lineNo, String fromStr, String toStr) {
        LocalDateTime to = defaultTo(toStr);
        LocalDateTime from = defaultFrom(fromStr, to);
        String fromMysql = fmt(from);
        String toMysql = fmt(to);

        if (lineNo != null && !lineNo.isBlank()) {
            return buildOverview(lineNo, from, to, fromMysql, toMysql);
        }
        // Aggregate across all lines
        List<String> lineNos = dashboardMapper.getAllLineNos();
        if (lineNos.isEmpty()) return buildOverview("1", from, to, fromMysql, toMysql);

        DashboardOverviewDto agg = new DashboardOverviewDto();
        agg.setLineNo("ALL");
        double oeeSum = 0, availSum = 0, perfSum = 0, qualSum = 0, hsSum = 0;
        long totalAlarm = 0;
        Map<String, Long> statusCounts = new HashMap<>();

        for (String ln : lineNos) {
            DashboardOverviewDto dto = buildOverview(ln, from, to, fromMysql, toMysql);
            agg.setRunTimeSec(agg.getRunTimeSec() + dto.getRunTimeSec());
            agg.setIdleTimeSec(agg.getIdleTimeSec() + dto.getIdleTimeSec());
            agg.setStopTimeSec(agg.getStopTimeSec() + dto.getStopTimeSec());
            agg.setAlarmTimeSec(agg.getAlarmTimeSec() + dto.getAlarmTimeSec());
            agg.setTotalCount(agg.getTotalCount() + dto.getTotalCount());
            agg.setGoodCount(agg.getGoodCount() + dto.getGoodCount());
            agg.setBadCount(agg.getBadCount() + dto.getBadCount());
            oeeSum += dto.getOee();
            availSum += dto.getAvailability();
            perfSum += dto.getPerformance();
            qualSum += dto.getQuality();
            hsSum += dto.getAvgHealthScore();
            totalAlarm += dto.getActiveAlarmCount();
            if (dto.getEquipmentStatusCounts() != null) {
                dto.getEquipmentStatusCounts().forEach((k, v) ->
                        statusCounts.merge(k, v, Long::sum));
            }
        }
        int n = lineNos.size();
        agg.setOee(oeeSum / n);
        agg.setAvailability(availSum / n);
        agg.setPerformance(perfSum / n);
        agg.setQuality(qualSum / n);
        agg.setAvgHealthScore(hsSum / n);
        agg.setActiveAlarmCount(totalAlarm);
        agg.setEquipmentStatusCounts(statusCounts);
        return agg;
    }

    public LineOeeDto getLineOeeDto(String lineNo, String fromStr, String toStr) {
        LocalDateTime to = defaultTo(toStr);
        LocalDateTime from = defaultFrom(fromStr, to);
        String fromMysql = fmt(from);
        String toMysql = fmt(to);

        // Try line_summary first
        Map<String, Object> row = dashboardMapper.getLineSummary(lineNo, fromMysql, toMysql);
        if (row != null) {
            LineOeeDto dto = new LineOeeDto();
            dto.setLineNo(lineNo);
            dto.setAvailability(toDouble(row.get("availability")));
            dto.setPerformance(toDouble(row.get("performance")));
            dto.setQuality(toDouble(row.get("quality")));
            dto.setOee(toDouble(row.get("oee")));
            dto.setFrom(fromMysql);
            dto.setTo(toMysql);
            return dto;
        }
        return computeLineOee(lineNo, fromMysql, toMysql);
    }

    public List<LineOeeDto> getLineOee(String fromStr, String toStr) {
        LocalDateTime to = defaultTo(toStr);
        LocalDateTime from = defaultFrom(fromStr, to);
        String fromMysql = fmt(from);
        String toMysql = fmt(to);

        return dashboardMapper.getAllLineNos().stream()
                .map(ln -> {
                    Map<String, Object> row = dashboardMapper.getLineSummary(ln, fromMysql, toMysql);
                    if (row != null) {
                        LineOeeDto dto = new LineOeeDto();
                        dto.setLineNo(ln);
                        dto.setAvailability(toDouble(row.get("availability")));
                        dto.setPerformance(toDouble(row.get("performance")));
                        dto.setQuality(toDouble(row.get("quality")));
                        dto.setOee(toDouble(row.get("oee")));
                        dto.setFrom(fromMysql);
                        dto.setTo(toMysql);
                        return dto;
                    }
                    return computeLineOee(ln, fromMysql, toMysql);
                })
                .collect(Collectors.toList());
    }

    public List<DashboardAlarmDto> getRecentAlarms(int limit) {
        return dashboardMapper.getRecentAlarms(limit);
    }

    public List<EquipmentStatusCardDto> getEquipmentStatusCards() {
        return dashboardMapper.getEquipmentStatusCards();
    }

    // Kept for backward-compat with existing GET /summaries endpoint
    public SummaryGenerateResponse generateSummary(String fromStr, String toStr) {
        LocalDateTime to = defaultTo(toStr);
        LocalDateTime from = defaultFrom(fromStr, to);
        return new SummaryGenerateResponse("summary counts only", fmt(from), fmt(to),
                dashboardMapper.getAllLineNos().size(),
                equipmentMapper.findAll().size());
    }

    public SummaryGenerateResponse generateSummaries(String fromStr, String toStr) {
        LocalDateTime to = defaultTo(toStr);
        LocalDateTime from = defaultFrom(fromStr, to);
        String fromMysql = fmt(from);
        String toMysql = fmt(to);

        List<String> lineNos = dashboardMapper.getAllLineNos();
        for (String lineNo : lineNos) {
            generateLineSummary(lineNo, from, to, fromMysql, toMysql);
        }

        List<Equipment> equipments = equipmentMapper.findAll();
        for (Equipment equipment : equipments) {
            generateEquipmentSummary(equipment, from, to, fromMysql, toMysql);
        }

        return new SummaryGenerateResponse("summary generated", fromMysql, toMysql,
                lineNos.size(), equipments.size());
    }

    // ──────────────────────────────────────────────────────────────
    //  Private: OEE / overview computation
    // ──────────────────────────────────────────────────────────────

    private DashboardOverviewDto buildOverview(String lineNo,
            LocalDateTime from, LocalDateTime to,
            String fromMysql, String toMysql) {

        // Check line_summary cache
        Map<String, Object> cached = dashboardMapper.getLineSummary(lineNo, fromMysql, toMysql);
        if (cached != null) {
            DashboardOverviewDto dto = mapLineSummaryToOverview(lineNo, cached);
            enrichOverview(dto, lineNo, fromMysql, toMysql);
            return dto;
        }

        // Calculate from raw logs
        String conveyorId = LINE_CONVEYOR_MAP.getOrDefault(lineNo, "CNV-001");
        long plannedTimeSec = ChronoUnit.SECONDS.between(from, to);

        Map<String, Object> durations = dashboardMapper.getStatusDurations(conveyorId, fromMysql, toMysql);
        long runTimeSec = toLong(durations.get("run_time_sec"));
        long idleTimeSec = toLong(durations.get("idle_time_sec"));
        long stopTimeSec = toLong(durations.get("stop_time_sec"));
        long alarmTimeSec = toLong(durations.get("alarm_time_sec"));

        Map<String, Object> qualityCounts = dashboardMapper.getVisionQualityCounts(lineNo, fromMysql, toMysql);
        long totalCount = toLong(qualityCounts.get("total_count"));
        long goodCount = toLong(qualityCounts.get("good_count"));
        long badCount = toLong(qualityCounts.get("bad_count"));

        Double idealCycleTime = dashboardMapper.getIdealCycleTime(lineNo);
        double ict = idealCycleTime != null ? idealCycleTime : 0.0;

        double availability = plannedTimeSec > 0 ? (double) runTimeSec / plannedTimeSec : 0.0;
        double performance = runTimeSec > 0 ? Math.min(1.0, Math.max(0.0, (ict * totalCount) / runTimeSec)) : 0.0;
        double quality = totalCount > 0 ? (double) goodCount / totalCount : 0.0;
        double oee = availability * performance * quality;

        DashboardOverviewDto dto = new DashboardOverviewDto();
        dto.setLineNo(lineNo);
        dto.setOee(oee);
        dto.setAvailability(availability);
        dto.setPerformance(performance);
        dto.setQuality(quality);
        dto.setRunTimeSec(runTimeSec);
        dto.setIdleTimeSec(idleTimeSec);
        dto.setStopTimeSec(stopTimeSec);
        dto.setAlarmTimeSec(alarmTimeSec);
        dto.setTotalCount(totalCount);
        dto.setGoodCount(goodCount);
        dto.setBadCount(badCount);

        enrichOverview(dto, lineNo, fromMysql, toMysql);
        return dto;
    }

    private DashboardOverviewDto mapLineSummaryToOverview(String lineNo, Map<String, Object> row) {
        DashboardOverviewDto dto = new DashboardOverviewDto();
        dto.setLineNo(lineNo);
        dto.setOee(toDouble(row.get("oee")));
        dto.setAvailability(toDouble(row.get("availability")));
        dto.setPerformance(toDouble(row.get("performance")));
        dto.setQuality(toDouble(row.get("quality")));
        dto.setRunTimeSec(toLong(row.get("run_time_sec")));
        dto.setIdleTimeSec(toLong(row.get("idle_time_sec")));
        dto.setStopTimeSec(toLong(row.get("stop_time_sec")));
        dto.setAlarmTimeSec(toLong(row.get("alarm_time_sec")));
        dto.setTotalCount(toLong(row.get("total_count")));
        dto.setGoodCount(toLong(row.get("good_count")));
        dto.setBadCount(toLong(row.get("bad_count")));
        return dto;
    }

    private void enrichOverview(DashboardOverviewDto dto, String lineNo,
            String fromMysql, String toMysql) {
        dto.setActiveAlarmCount(dashboardMapper.getActiveAlarmCountByLine(lineNo));
        Double hs = dashboardMapper.getAvgHealthScoreByLine(lineNo);
        dto.setAvgHealthScore(hs != null ? hs : 0.0);

        Map<String, Object> statusCounts = dashboardMapper.getEquipmentStatusCountsByLine(lineNo);
        Map<String, Long> counts = new HashMap<>();
        if (statusCounts != null) {
            statusCounts.forEach((k, v) -> counts.put(k, toLong(v)));
        }
        dto.setEquipmentStatusCounts(counts);
    }

    private LineOeeDto computeLineOee(String lineNo, String fromMysql, String toMysql) {
        String conveyorId = LINE_CONVEYOR_MAP.getOrDefault(lineNo, "CNV-001");

        LocalDateTime from = LocalDateTime.parse(fromMysql, MYSQL_FMT);
        LocalDateTime to = LocalDateTime.parse(toMysql, MYSQL_FMT);
        long plannedTimeSec = ChronoUnit.SECONDS.between(from, to);

        Map<String, Object> durations = dashboardMapper.getStatusDurations(conveyorId, fromMysql, toMysql);
        long runTimeSec = toLong(durations.get("run_time_sec"));

        Map<String, Object> qualityCounts = dashboardMapper.getVisionQualityCounts(lineNo, fromMysql, toMysql);
        long totalCount = toLong(qualityCounts.get("total_count"));
        long goodCount = toLong(qualityCounts.get("good_count"));

        Double idealCycleTime = dashboardMapper.getIdealCycleTime(lineNo);
        double ict = idealCycleTime != null ? idealCycleTime : 0.0;

        double availability = plannedTimeSec > 0 ? (double) runTimeSec / plannedTimeSec : 0.0;
        double performance = runTimeSec > 0 ? Math.min(1.0, Math.max(0.0, (ict * totalCount) / runTimeSec)) : 0.0;
        double quality = totalCount > 0 ? (double) goodCount / totalCount : 0.0;

        LineOeeDto dto = new LineOeeDto();
        dto.setLineNo(lineNo);
        dto.setAvailability(availability);
        dto.setPerformance(performance);
        dto.setQuality(quality);
        dto.setOee(availability * performance * quality);
        dto.setFrom(fromMysql);
        dto.setTo(toMysql);
        return dto;
    }

    // ──────────────────────────────────────────────────────────────
    //  Private: Line summary generation
    // ──────────────────────────────────────────────────────────────

    private void generateLineSummary(String lineNo, LocalDateTime from, LocalDateTime to,
            String fromMysql, String toMysql) {
        String conveyorId = LINE_CONVEYOR_MAP.getOrDefault(lineNo, "CNV-001");
        long plannedTimeSec = ChronoUnit.SECONDS.between(from, to);

        Map<String, Object> durations = dashboardMapper.getStatusDurations(conveyorId, fromMysql, toMysql);
        long runTimeSec = toLong(durations.get("run_time_sec"));
        long idleTimeSec = toLong(durations.get("idle_time_sec"));
        long stopTimeSec = toLong(durations.get("stop_time_sec"));
        long alarmTimeSec = toLong(durations.get("alarm_time_sec"));

        Map<String, Object> qualityCounts = dashboardMapper.getVisionQualityCounts(lineNo, fromMysql, toMysql);
        long totalCount = toLong(qualityCounts.get("total_count"));
        long goodCount = toLong(qualityCounts.get("good_count"));
        long badCount = toLong(qualityCounts.get("bad_count"));

        Double idealCycleTime = dashboardMapper.getIdealCycleTime(lineNo);
        double ict = idealCycleTime != null ? idealCycleTime : 0.0;

        double availability = plannedTimeSec > 0 ? (double) runTimeSec / plannedTimeSec : 0.0;
        double performance = runTimeSec > 0 ? Math.min(1.0, Math.max(0.0, (ict * totalCount) / runTimeSec)) : 0.0;
        double quality = totalCount > 0 ? (double) goodCount / totalCount : 0.0;
        double oee = availability * performance * quality;

        dashboardMapper.deleteLineSummary(lineNo, fromMysql, toMysql);
        dashboardMapper.insertLineSummary(
                lineNo, fromMysql, toMysql,
                plannedTimeSec, runTimeSec, idleTimeSec, stopTimeSec, alarmTimeSec,
                totalCount, goodCount, badCount,
                availability, performance, quality, oee);
    }

    // ──────────────────────────────────────────────────────────────
    //  Private: Equipment summary generation
    // ──────────────────────────────────────────────────────────────

    private void generateEquipmentSummary(Equipment equipment,
            LocalDateTime from, LocalDateTime to,
            String fromMysql, String toMysql) {
        String equipmentId = equipment.getEquipmentId();
        long plannedTimeSec = ChronoUnit.SECONDS.between(from, to);

        // Status durations
        Map<String, Object> durations = dashboardMapper.getStatusDurations(equipmentId, fromMysql, toMysql);
        long runTimeSec = toLong(durations.get("run_time_sec"));
        long idleTimeSec = toLong(durations.get("idle_time_sec"));
        long stopTimeSec = toLong(durations.get("stop_time_sec"));
        long alarmTimeSec = toLong(durations.get("alarm_time_sec"));

        // Reliability metrics
        int failureCount = dashboardMapper.getEquipmentFailureCount(equipmentId, fromMysql, toMysql);
        double availability = plannedTimeSec > 0 ? (double) runTimeSec / plannedTimeSec : 0.0;
        long mttfSec = failureCount > 0 ? runTimeSec / failureCount : 0;
        long mttrSec = failureCount > 0 ? alarmTimeSec / failureCount : 0;
        long mtbfSec = mttfSec + mttrSec;

        // Health Score
        int alarmCount24h = dashboardMapper.getAlarmCount24h(equipmentId);
        double sensorPenalty = computeSensorPenalty(equipmentId);
        double healthScore = computeHealthScore(failureCount, mttfSec, mttrSec, mtbfSec, alarmCount24h, sensorPenalty);

        // Accumulated run hours (all time)
        Double totalRunHours = dashboardMapper.getTotalAccumulatedRunHours(equipmentId);
        double accumulatedRunHours = totalRunHours != null ? totalRunHours : 0.0;

        // Remaining life
        Integer expectedLifetimeHours = equipment.getExpectedLifetimeHours();
        double remainingLife = computeRemainingLife(accumulatedRunHours, expectedLifetimeHours, healthScore);

        // Replacement date
        Double avgDailyRunHours7d = dashboardMapper.getAvgDailyRunHours7d(equipmentId);
        LocalDate replacementDate = computeReplacementDate(accumulatedRunHours, expectedLifetimeHours, avgDailyRunHours7d);

        // Save equipment_summary
        dashboardMapper.deleteEquipmentSummary(equipmentId, fromMysql, toMysql);
        dashboardMapper.insertEquipmentSummary(
                equipmentId, fromMysql, toMysql,
                runTimeSec, idleTimeSec, stopTimeSec, alarmTimeSec,
                availability, 0.0, 0.0, 0.0,
                mttfSec, mtbfSec, mttrSec,
                healthScore, remainingLife, replacementDate);

        // Update equipment table
        dashboardMapper.updateEquipmentMetrics(equipmentId, healthScore, accumulatedRunHours, remainingLife, replacementDate);
    }

    // ──────────────────────────────────────────────────────────────
    //  Private: Health Score calculation
    // ──────────────────────────────────────────────────────────────

    private double computeHealthScore(int failureCount, long mttfSec, long mttrSec, long mtbfSec,
            int alarmCount24h, double sensorPenalty) {
        double failurePenalty = 0;
        double repairPenalty = 0;
        if (failureCount > 0) {
            failurePenalty = Math.min(30.0, ((double) TARGET_MTBF_SEC / Math.max(mtbfSec, 1)) * 10.0);
            repairPenalty = Math.min(20.0, ((double) mttrSec / TARGET_MTTR_SEC) * 10.0);
        }
        double alarmPenalty = Math.min(20.0, alarmCount24h * 3.0);
        double score = 100.0 - failurePenalty - repairPenalty - alarmPenalty - sensorPenalty;
        return Math.max(0.0, Math.min(100.0, score));
    }

    private double computeSensorPenalty(String equipmentId) {
        String prefix = getPrefix(equipmentId);
        Map<String, Object> result = switch (prefix) {
            case "PLF" -> dashboardMapper.getPanelFeederAbnormalCount(equipmentId);
            case "JIG" -> dashboardMapper.getBodyJigAbnormalCount(equipmentId);
            case "ROB" -> dashboardMapper.getRobotAbnormalCount(equipmentId);
            case "WLD" -> dashboardMapper.getSpotWelderAbnormalCount(equipmentId);
            case "SLR" -> dashboardMapper.getSealerAbnormalCount(equipmentId);
            case "CNV" -> dashboardMapper.getConveyorAbnormalCount(equipmentId);
            default -> Collections.emptyMap(); // VSI excluded per spec
        };
        if (result == null || result.isEmpty()) return 0.0;
        long total = toLong(result.get("totalCount"));
        if (total == 0) return 0.0;
        long abnormal = toLong(result.get("abnormalCount"));
        double ratio = (double) abnormal / total;
        return Math.min(20.0, ratio * 20.0);
    }

    private String getPrefix(String equipmentId) {
        if (equipmentId == null || equipmentId.isBlank()) return "";
        int idx = equipmentId.indexOf('-');
        return idx > 0 ? equipmentId.substring(0, idx) : equipmentId.substring(0, Math.min(3, equipmentId.length()));
    }

    // ──────────────────────────────────────────────────────────────
    //  Private: Remaining life / replacement date
    // ──────────────────────────────────────────────────────────────

    private double computeRemainingLife(double accumulatedRunHours, Integer expectedLifetimeHours, double healthScore) {
        if (expectedLifetimeHours == null || expectedLifetimeHours == 0) return 100.0;
        double ratio = 1.0 - accumulatedRunHours / expectedLifetimeHours;
        double remaining = ratio * (healthScore / 100.0) * 100.0;
        return Math.max(0.0, Math.min(100.0, remaining));
    }

    private LocalDate computeReplacementDate(double accumulatedRunHours, Integer expectedLifetimeHours,
            Double avgDailyRunHours7d) {
        if (expectedLifetimeHours == null || expectedLifetimeHours == 0) return null;
        if (avgDailyRunHours7d == null || avgDailyRunHours7d <= 0) return null;
        double remainingHours = expectedLifetimeHours - accumulatedRunHours;
        if (remainingHours <= 0) return LocalDate.now();
        long daysUntil = (long) Math.ceil(remainingHours / avgDailyRunHours7d);
        return LocalDate.now().plusDays(daysUntil);
    }
}
