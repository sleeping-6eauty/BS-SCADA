package com.example.backend.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.dto.ApiResponse;
import com.example.backend.dto.DashboardSummary;
import com.example.backend.dto.EquipmentResponse;
import com.example.backend.dto.dashboard.DashboardAlarmDto;
import com.example.backend.dto.dashboard.DashboardOverviewDto;
import com.example.backend.dto.dashboard.EquipmentStatusCardDto;
import com.example.backend.dto.dashboard.LineOeeDto;
import com.example.backend.dto.dashboard.SummaryGenerateResponse;
import com.example.backend.service.DashboardService;
import com.example.backend.service.EquipmentService;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final EquipmentService equipmentService;
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public DashboardController(DashboardService dashboardService, EquipmentService equipmentService,
            SimpMessagingTemplate messagingTemplate) {
        this.dashboardService = dashboardService;
        this.equipmentService = equipmentService;
        this.messagingTemplate = messagingTemplate;
    }

    // GET /api/dashboard/overview?lineNo=1&from=...&to=...
    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<DashboardOverviewDto>> getOverview(
            @RequestParam(required = false) String lineNo,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        DashboardOverviewDto overview = dashboardService.getOverview(lineNo, from, to);
        return ResponseEntity.ok(new ApiResponse<>(true, "Dashboard overview retrieved", overview));
    }

    // GET /api/dashboard/lines/{lineNo}/oee?from=...&to=...
    @GetMapping("/lines/{lineNo}/oee")
    public ResponseEntity<ApiResponse<LineOeeDto>> getLineOee(
            @PathVariable String lineNo,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        LineOeeDto dto = dashboardService.getLineOeeDto(lineNo, from, to);
        return ResponseEntity.ok(new ApiResponse<>(true, "Line OEE retrieved", dto));
    }

    // GET /api/dashboard/alarms/recent?limit=10
    @GetMapping("/alarms/recent")
    public ResponseEntity<ApiResponse<List<DashboardAlarmDto>>> getRecentAlarms(
            @RequestParam(defaultValue = "10") int limit) {
        List<DashboardAlarmDto> alarms = dashboardService.getRecentAlarms(limit);
        return ResponseEntity.ok(new ApiResponse<>(true, "Recent alarms retrieved", alarms));
    }

    // GET /api/dashboard/equipment-status
    @GetMapping("/equipment-status")
    public ResponseEntity<ApiResponse<List<EquipmentStatusCardDto>>> getEquipmentStatus() {
        List<EquipmentStatusCardDto> cards = dashboardService.getEquipmentStatusCards();
        return ResponseEntity.ok(new ApiResponse<>(true, "Equipment status retrieved", cards));
    }

    // POST /api/dashboard/summaries/generate?from=...&to=...
    @PostMapping("/summaries/generate")
    public ResponseEntity<ApiResponse<SummaryGenerateResponse>> generateSummaries(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        SummaryGenerateResponse response = dashboardService.generateSummaries(from, to);
        messagingTemplate.convertAndSend("/topic/dashboard/summary", response);
        return ResponseEntity.ok(new ApiResponse<>(true, "Summary generation completed", response));
    }

    // ── Backward-compat endpoints (not in spec but kept for existing callers) ──

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummary> getSummary() {
        return ResponseEntity.ok(dashboardService.getSummary());
    }

    @GetMapping("/lines/oee")
    public ResponseEntity<ApiResponse<List<LineOeeDto>>> getAllLineOee(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        List<LineOeeDto> lineOees = dashboardService.getLineOee(from, to);
        return ResponseEntity.ok(new ApiResponse<>(true, "Line OEE metrics retrieved", lineOees));
    }

    @GetMapping("/alarms")
    public ResponseEntity<ApiResponse<List<DashboardAlarmDto>>> getAlarms(
            @RequestParam(defaultValue = "20") int limit) {
        List<DashboardAlarmDto> alarms = dashboardService.getRecentAlarms(limit);
        return ResponseEntity.ok(new ApiResponse<>(true, "Recent alarms retrieved", alarms));
    }

    @GetMapping("/status-cards")
    public ResponseEntity<ApiResponse<List<EquipmentStatusCardDto>>> getStatusCards() {
        List<EquipmentStatusCardDto> cards = dashboardService.getEquipmentStatusCards();
        return ResponseEntity.ok(new ApiResponse<>(true, "Equipment status cards retrieved", cards));
    }

    @GetMapping("/summaries")
    public ResponseEntity<ApiResponse<SummaryGenerateResponse>> getSummaryCounts(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        SummaryGenerateResponse response = dashboardService.generateSummary(from, to);
        return ResponseEntity.ok(new ApiResponse<>(true, "Summary counts retrieved", response));
    }

    @GetMapping("/layout")
    public ResponseEntity<ApiResponse<List<EquipmentResponse>>> getLayout() {
        List<EquipmentResponse> responses = equipmentService.listEquipments(null, null, null).stream()
                .map(e -> new EquipmentResponse(e.getEquipmentId(), e.getEquipmentName(),
                        e.getLineNo(), e.getZone(), e.getManufacturer(), e.getCycleTime(),
                        e.getHealthScore(), e.getRemainingLife(), e.getReplacementDate()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(new ApiResponse<>(true, "Layout equipment status retrieved", responses));
    }

    // GET /api/dashboard/oee?from=...&to=...  — 전체 라인 OEE/가동률/성능률/품질률 평균
    @GetMapping("/oee")
    public ResponseEntity<ApiResponse<LineOeeDto>> getOee(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        List<LineOeeDto> lineOees = dashboardService.getLineOee(from, to);
        int n = lineOees.size();
        LineOeeDto total = new LineOeeDto();
        total.setLineNo("ALL");
        if (n > 0) {
            total.setOee(         lineOees.stream().mapToDouble(LineOeeDto::getOee).average().orElse(0.0));
            total.setAvailability(lineOees.stream().mapToDouble(LineOeeDto::getAvailability).average().orElse(0.0));
            total.setPerformance( lineOees.stream().mapToDouble(LineOeeDto::getPerformance).average().orElse(0.0));
            total.setQuality(     lineOees.stream().mapToDouble(LineOeeDto::getQuality).average().orElse(0.0));
            total.setFrom(lineOees.get(0).getFrom());
            total.setTo(lineOees.get(0).getTo());
        }
        return ResponseEntity.ok(new ApiResponse<>(true, "OEE metric retrieved", total));
    }
}
