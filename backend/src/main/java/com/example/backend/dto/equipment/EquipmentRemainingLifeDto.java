package com.example.backend.dto.equipment;

import java.time.LocalDate;

public class EquipmentRemainingLifeDto {
    private String equipmentId;
    private String equipmentName;
    private Float remainingLife;
    private LocalDate replacementDate;

    public EquipmentRemainingLifeDto() {}

    public EquipmentRemainingLifeDto(String equipmentId, String equipmentName, Float remainingLife, LocalDate replacementDate) {
        this.equipmentId = equipmentId;
        this.equipmentName = equipmentName;
        this.remainingLife = remainingLife;
        this.replacementDate = replacementDate;
    }

    public String getEquipmentId() { return equipmentId; }
    public void setEquipmentId(String equipmentId) { this.equipmentId = equipmentId; }

    public String getEquipmentName() { return equipmentName; }
    public void setEquipmentName(String equipmentName) { this.equipmentName = equipmentName; }

    public Float getRemainingLife() { return remainingLife; }
    public void setRemainingLife(Float remainingLife) { this.remainingLife = remainingLife; }

    public LocalDate getReplacementDate() { return replacementDate; }
    public void setReplacementDate(LocalDate replacementDate) { this.replacementDate = replacementDate; }
}
