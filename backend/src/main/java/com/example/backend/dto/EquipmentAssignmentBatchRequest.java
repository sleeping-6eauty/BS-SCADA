package com.example.backend.dto;

import java.util.List;

import jakarta.validation.constraints.NotNull;

public class EquipmentAssignmentBatchRequest {

    @NotNull(message = "equipmentIds cannot be null")
    private List<String> equipmentIds;

    public List<String> getEquipmentIds() {
        return equipmentIds;
    }

    public void setEquipmentIds(List<String> equipmentIds) {
        this.equipmentIds = equipmentIds;
    }
}