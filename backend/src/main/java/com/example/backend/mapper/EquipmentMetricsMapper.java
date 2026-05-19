package com.example.backend.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.backend.domain.EquipmentStatusLog;
import com.example.backend.dto.dashboard.DashboardAlarmDto;
import com.example.backend.dto.equipment.EquipmentSummaryDto;

@Mapper
public interface EquipmentMetricsMapper {

    // equipment_summary lookup (returns null if not found)
    EquipmentSummaryDto findEquipmentSummary(
            @Param("equipmentId") String equipmentId,
            @Param("from") String from,
            @Param("to") String to);

    // equipment_status_log history in from~to window
    List<EquipmentStatusLog> findEquipmentStatusHistory(
            @Param("equipmentId") String equipmentId,
            @Param("from") String from,
            @Param("to") String to);

    // alarm_log for a specific equipment in from~to window
    List<DashboardAlarmDto> findEquipmentAlarms(
            @Param("equipmentId") String equipmentId,
            @Param("from") String from,
            @Param("to") String to);
}
