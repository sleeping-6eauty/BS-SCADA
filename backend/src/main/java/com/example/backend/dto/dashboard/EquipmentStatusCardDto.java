package com.example.backend.dto.dashboard;

public class EquipmentStatusCardDto {
    private String equipmentId;
    private String equipmentName;
    private String lineNo;
    private String zone;
    private String currentStatus;
    private Double healthScore;
    private Long activeAlarmCount;

    public String getEquipmentId() { return equipmentId; }
    public void setEquipmentId(String equipmentId) { this.equipmentId = equipmentId; }

    public String getEquipmentName() { return equipmentName; }
    public void setEquipmentName(String equipmentName) { this.equipmentName = equipmentName; }

    public String getLineNo() { return lineNo; }
    public void setLineNo(String lineNo) { this.lineNo = lineNo; }

    public String getZone() { return zone; }
    public void setZone(String zone) { this.zone = zone; }

    public String getCurrentStatus() { return currentStatus; }
    public void setCurrentStatus(String currentStatus) { this.currentStatus = currentStatus; }

    public Double getHealthScore() { return healthScore; }
    public void setHealthScore(Double healthScore) { this.healthScore = healthScore; }

    public Long getActiveAlarmCount() { return activeAlarmCount; }
    public void setActiveAlarmCount(Long activeAlarmCount) { this.activeAlarmCount = activeAlarmCount; }
}
