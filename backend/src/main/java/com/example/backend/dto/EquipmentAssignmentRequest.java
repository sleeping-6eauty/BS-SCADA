package com.example.backend.dto;

import jakarta.validation.constraints.NotBlank;

public class EquipmentAssignmentRequest {
    @NotBlank
    private String equipmentId;

    public String getEquipmentId() {
        return equipmentId;
    }

    public void setEquipmentId(String equipmentId) {
        this.equipmentId = equipmentId;
    }
}
