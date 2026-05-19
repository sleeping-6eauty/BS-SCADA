package com.example.backend.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.backend.dto.LogEntryDto;

@Mapper
public interface LogMapper {

    List<LogEntryDto> findPanelFeederLogs(
            @Param("equipmentId") String equipmentId,
            @Param("from") String from,
            @Param("to") String to);

    List<LogEntryDto> findBodyJigLogs(
            @Param("equipmentId") String equipmentId,
            @Param("from") String from,
            @Param("to") String to);

    List<LogEntryDto> findRobotLogs(
            @Param("equipmentId") String equipmentId,
            @Param("from") String from,
            @Param("to") String to);

    List<LogEntryDto> findSpotWelderLogs(
            @Param("equipmentId") String equipmentId,
            @Param("from") String from,
            @Param("to") String to);

    List<LogEntryDto> findSealerLogs(
            @Param("equipmentId") String equipmentId,
            @Param("from") String from,
            @Param("to") String to);

    List<LogEntryDto> findConveyorLogs(
            @Param("equipmentId") String equipmentId,
            @Param("from") String from,
            @Param("to") String to);

    List<LogEntryDto> findVisionInspectionLogs(
            @Param("equipmentId") String equipmentId,
            @Param("from") String from,
            @Param("to") String to);

    List<LogEntryDto> findLatestByEquipmentCode(@Param("equipmentId") String equipmentId);
}
