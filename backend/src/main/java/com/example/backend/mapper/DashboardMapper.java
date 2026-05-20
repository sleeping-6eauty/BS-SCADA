package com.example.backend.mapper;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.backend.dto.dashboard.DashboardAlarmDto;
import com.example.backend.dto.dashboard.EquipmentStatusCardDto;

@Mapper
public interface DashboardMapper {

    // Status duration calculation (overlap with from~to window) — per equipment
    Map<String, Object> getStatusDurations(
            @Param("equipmentId") String equipmentId,
            @Param("from") String from,
            @Param("to") String to);

    // Status duration calculation — per line (joins equipment_status_log with equipment table)
    Map<String, Object> getLineStatusDurations(
            @Param("lineNo") String lineNo,
            @Param("from") String from,
            @Param("to") String to);

    // Vision quality counts for a line in from~to window
    Map<String, Object> getVisionQualityCounts(
            @Param("lineNo") String lineNo,
            @Param("from") String from,
            @Param("to") String to);

    // ideal_cycle_time_sec from line_config
    Double getIdealCycleTime(@Param("lineNo") String lineNo);

    // All line_nos from line_config
    List<String> getAllLineNos();

    // line_summary lookup
    Map<String, Object> getLineSummary(
            @Param("lineNo") String lineNo,
            @Param("from") String from,
            @Param("to") String to);

    int deleteLineSummary(
            @Param("lineNo") String lineNo,
            @Param("from") String from,
            @Param("to") String to);

    int insertLineSummary(
            @Param("lineNo") String lineNo,
            @Param("summaryStartTime") String summaryStartTime,
            @Param("summaryEndTime") String summaryEndTime,
            @Param("plannedTimeSec") long plannedTimeSec,
            @Param("runTimeSec") long runTimeSec,
            @Param("idleTimeSec") long idleTimeSec,
            @Param("stopTimeSec") long stopTimeSec,
            @Param("alarmTimeSec") long alarmTimeSec,
            @Param("totalCount") long totalCount,
            @Param("goodCount") long goodCount,
            @Param("badCount") long badCount,
            @Param("availability") double availability,
            @Param("performance") double performance,
            @Param("quality") double quality,
            @Param("oee") double oee);

    // Equipment failure count (ALARM overlap) in from~to
    int getEquipmentFailureCount(
            @Param("equipmentId") String equipmentId,
            @Param("from") String from,
            @Param("to") String to);

    // 7-day rolling stats for MTTF/MTTR/MTBF
    int getFailureCount7d(@Param("equipmentId") String equipmentId);
    Long getRunTimeSec7d(@Param("equipmentId") String equipmentId);
    Long getAlarmTimeSec7d(@Param("equipmentId") String equipmentId);

    // Total accumulated RUN hours (all time)
    Double getTotalAccumulatedRunHours(@Param("equipmentId") String equipmentId);

    // Average daily RUN hours over last 7 days
    Double getAvgDailyRunHours7d(@Param("equipmentId") String equipmentId);

    // Latest status for a single equipment
    String getLatestEquipmentStatus(@Param("equipmentId") String equipmentId);

    // Alarm count in last 24 hours
    int getAlarmCount24h(@Param("equipmentId") String equipmentId);

    // Recent alarms for dashboard
    List<DashboardAlarmDto> getRecentAlarms(@Param("limit") int limit);

    // Equipment status cards (latest status + active alarm count)
    List<EquipmentStatusCardDto> getEquipmentStatusCards();

    // Equipment status counts by line (for overview)
    Map<String, Object> getEquipmentStatusCountsByLine(@Param("lineNo") String lineNo);

    // Average health score for a line
    Double getAvgHealthScoreByLine(@Param("lineNo") String lineNo);

    // Active alarm count for a line
    int getActiveAlarmCountByLine(@Param("lineNo") String lineNo);

    // equipment_summary lookup
    Map<String, Object> getEquipmentSummaryFromDb(
            @Param("equipmentId") String equipmentId,
            @Param("from") String from,
            @Param("to") String to);

    int deleteEquipmentSummary(
            @Param("equipmentId") String equipmentId,
            @Param("from") String from,
            @Param("to") String to);

    int insertEquipmentSummary(
            @Param("equipmentId") String equipmentId,
            @Param("summaryStartTime") String summaryStartTime,
            @Param("summaryEndTime") String summaryEndTime,
            @Param("runTimeSec") long runTimeSec,
            @Param("idleTimeSec") long idleTimeSec,
            @Param("stopTimeSec") long stopTimeSec,
            @Param("alarmTimeSec") long alarmTimeSec,
            @Param("availability") double availability,
            @Param("performance") double performance,
            @Param("quality") double quality,
            @Param("oee") double oee,
            @Param("mttfSec") long mttfSec,
            @Param("mtbfSec") long mtbfSec,
            @Param("mttrSec") long mttrSec,
            @Param("healthScore") double healthScore,
            @Param("remainingLife") double remainingLife,
            @Param("replacementDate") LocalDate replacementDate);

    // Update equipment table metrics
    int updateEquipmentMetrics(
            @Param("equipmentId") String equipmentId,
            @Param("healthScore") double healthScore,
            @Param("accumulatedRunHours") double accumulatedRunHours,
            @Param("remainingLife") double remainingLife,
            @Param("replacementDate") LocalDate replacementDate);

    // Sensor abnormal counts (last 1 hour from NOW)
    Map<String, Object> getPanelFeederAbnormalCount(@Param("equipmentId") String equipmentId);
    Map<String, Object> getBodyJigAbnormalCount(@Param("equipmentId") String equipmentId);
    Map<String, Object> getRobotAbnormalCount(@Param("equipmentId") String equipmentId);
    Map<String, Object> getSpotWelderAbnormalCount(@Param("equipmentId") String equipmentId);
    Map<String, Object> getSealerAbnormalCount(@Param("equipmentId") String equipmentId);
    Map<String, Object> getConveyorAbnormalCount(@Param("equipmentId") String equipmentId);
}
