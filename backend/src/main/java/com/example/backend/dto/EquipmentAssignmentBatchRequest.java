package com.example.backend.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;

public class EquipmentAssignmentBatchRequest {
    @NotEmpty
    private List<String> equipmentIds;

    public List<String> getEquipmentIds() {
        return equipmentIds;
    }

    public void setEquipmentIds(List<String> equipmentIds) {
        this.equipmentIds = equipmentIds;
    }
}
