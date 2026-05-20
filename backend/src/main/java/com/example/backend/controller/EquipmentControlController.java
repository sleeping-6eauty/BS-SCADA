package com.example.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.dto.ApiResponse;
import com.example.backend.dto.ControlOnRequest;
import com.example.backend.dto.ControlStatusResponse;
import com.example.backend.service.EquipmentControlService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/equipments/{equipmentId}/control")
public class EquipmentControlController {

    private final EquipmentControlService controlService;

    @Autowired
    public EquipmentControlController(EquipmentControlService controlService) {
        this.controlService = controlService;
    }

    // POST /api/equipments/{equipmentId}/control/on
    // body: { "frequency": 150 }
    @PostMapping("/on")
    public ResponseEntity<ApiResponse<Void>> turnOn(
            @PathVariable String equipmentId,
            @RequestBody @Valid ControlOnRequest request) {
        controlService.turnOn(equipmentId, request.getFrequency());
        return ResponseEntity.ok(new ApiResponse<>(true, "Equipment started", null));
    }

    // POST /api/equipments/{equipmentId}/control/off
    @PostMapping("/off")
    public ResponseEntity<ApiResponse<Void>> turnOff(
            @PathVariable String equipmentId) {
        controlService.turnOff(equipmentId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Equipment stopped", null));
    }

    // GET /api/equipments/{equipmentId}/control/status
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<ControlStatusResponse>> getStatus(
            @PathVariable String equipmentId) {
        ControlStatusResponse status = controlService.getStatus(equipmentId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Control status retrieved", status));
    }
}
