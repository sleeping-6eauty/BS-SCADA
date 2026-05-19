package com.example.backend.dto.equipment;

public class EquipmentNameDto {
    private String equipmentId;
    private String equipmentName;

    public EquipmentNameDto() {}

    public EquipmentNameDto(String equipmentId, String equipmentName) {
        this.equipmentId = equipmentId;
        this.equipmentName = equipmentName;
    }

    public String getEquipmentId() { return equipmentId; }
    public void setEquipmentId(String equipmentId) { this.equipmentId = equipmentId; }

    public String getEquipmentName() { return equipmentName; }
    public void setEquipmentName(String equipmentName) { this.equipmentName = equipmentName; }
}
