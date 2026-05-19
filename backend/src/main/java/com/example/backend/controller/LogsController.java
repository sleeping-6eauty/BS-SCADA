package com.example.backend.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.dto.ApiResponse;
import com.example.backend.dto.LogEntryDto;
import com.example.backend.service.LogService;

@RestController
@RequestMapping("/api/logs")
public class LogsController {

    private final LogService logService;

    @Autowired
    public LogsController(LogService logService) {
        this.logService = logService;
    }

    @GetMapping("/panel-feeders")
    public ResponseEntity<ApiResponse<List<LogEntryDto>>> getPanelFeederLogs(
            @RequestParam(required = false) String equipmentId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return wrapLogs(logService.findPanelFeederLogs(equipmentId, from, to), page, size);
    }

    @PostMapping("/panel-feeders")
    public ResponseEntity<LogEntryDto> createPanelFeederLog(@Validated @RequestBody LogEntryDto request) {
        return ResponseEntity.ok(request);
    }

    @GetMapping("/body-jigs")
    public ResponseEntity<ApiResponse<List<LogEntryDto>>> getBodyJigLogs(@RequestParam(required = false) String equipmentId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return wrapLogs(logService.findBodyJigLogs(equipmentId, from, to), page, size);
    }

    @PostMapping("/body-jigs")
    public ResponseEntity<LogEntryDto> createBodyJigLog(@Validated @RequestBody LogEntryDto request) {
        return ResponseEntity.ok(request);
    }

    @GetMapping("/robots")
    public ResponseEntity<ApiResponse<List<LogEntryDto>>> getRobotLogs(@RequestParam(required = false) String equipmentId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return wrapLogs(logService.findRobotLogs(equipmentId, from, to), page, size);
    }

    @PostMapping("/robots")
    public ResponseEntity<LogEntryDto> createRobotLog(@Validated @RequestBody LogEntryDto request) {
        return ResponseEntity.ok(request);
    }

    @GetMapping("/spot-welders")
    public ResponseEntity<ApiResponse<List<LogEntryDto>>> getSpotWelderLogs(@RequestParam(required = false) String equipmentId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return wrapLogs(logService.findSpotWelderLogs(equipmentId, from, to), page, size);
    }

    @PostMapping("/spot-welders")
    public ResponseEntity<LogEntryDto> createSpotWelderLog(@Validated @RequestBody LogEntryDto request) {
        return ResponseEntity.ok(request);
    }

    @GetMapping("/sealers")
    public ResponseEntity<ApiResponse<List<LogEntryDto>>> getSealerLogs(@RequestParam(required = false) String equipmentId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return wrapLogs(logService.findSealerLogs(equipmentId, from, to), page, size);
    }

    @PostMapping("/sealers")
    public ResponseEntity<LogEntryDto> createSealerLog(@Validated @RequestBody LogEntryDto request) {
        return ResponseEntity.ok(request);
    }

    @GetMapping("/conveyors")
    public ResponseEntity<ApiResponse<List<LogEntryDto>>> getConveyorLogs(@RequestParam(required = false) String equipmentId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return wrapLogs(logService.findConveyorLogs(equipmentId, from, to), page, size);
    }

    @PostMapping("/conveyors")
    public ResponseEntity<LogEntryDto> createConveyorLog(@Validated @RequestBody LogEntryDto request) {
        return ResponseEntity.ok(request);
    }

    @GetMapping("/vision-inspections")
    public ResponseEntity<ApiResponse<List<LogEntryDto>>> getVisionInspectionLogs(@RequestParam(required = false) String equipmentId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return wrapLogs(logService.findVisionInspectionLogs(equipmentId, from, to), page, size);
    }

    @PostMapping("/vision-inspections")
    public ResponseEntity<LogEntryDto> createVisionInspectionLog(@Validated @RequestBody LogEntryDto request) {
        return ResponseEntity.ok(request);
    }

    @GetMapping("/equipments/{equipmentId}/latest")
    public ResponseEntity<ApiResponse<List<LogEntryDto>>> getLatestByEquipment(@PathVariable String equipmentId) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Latest logs retrieved", logService.findLatestByEquipmentCode(equipmentId)));
    }

    private ResponseEntity<ApiResponse<List<LogEntryDto>>> wrapLogs(List<LogEntryDto> logs, int page, int size) {
        int from = Math.min(page * size, logs.size());
        int to = Math.min(from + size, logs.size());
        return ResponseEntity.ok(new ApiResponse<>(true, "Log list retrieved", logs.subList(from, to)));
    }
}
