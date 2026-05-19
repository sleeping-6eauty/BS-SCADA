package com.example.backend.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Equipment {
    private String equipmentId;
    private String equipmentName;
    private String lineNo;
    private String zone;
    private String manufacturer;
    private Float cycleTime;
    private Float healthScore;
    private Float remainingLife;
    private LocalDate replacementDate;
    private Integer expectedLifetimeHours;
    private BigDecimal accumulatedRunHours;

    public String getEquipmentId() {
        return equipmentId;
    }

    public void setEquipmentId(String equipmentId) {
        this.equipmentId = equipmentId;
    }

    public String getEquipmentName() {
        return equipmentName;
    }

    public void setEquipmentName(String equipmentName) {
        this.equipmentName = equipmentName;
    }

    public String getLineNo() {
        return lineNo;
    }

    public void setLineNo(String lineNo) {
        this.lineNo = lineNo;
    }

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public Float getCycleTime() {
        return cycleTime;
    }

    public void setCycleTime(Float cycleTime) {
        this.cycleTime = cycleTime;
    }

    public Float getHealthScore() {
        return healthScore;
    }

    public void setHealthScore(Float healthScore) {
        this.healthScore = healthScore;
    }

    public Float getRemainingLife() {
        return remainingLife;
    }

    public void setRemainingLife(Float remainingLife) {
        this.remainingLife = remainingLife;
    }

    public LocalDate getReplacementDate() {
        return replacementDate;
    }

    public void setReplacementDate(LocalDate replacementDate) {
        this.replacementDate = replacementDate;
    }

    public Integer getExpectedLifetimeHours() { return expectedLifetimeHours; }
    public void setExpectedLifetimeHours(Integer expectedLifetimeHours) { this.expectedLifetimeHours = expectedLifetimeHours; }

    public BigDecimal getAccumulatedRunHours() { return accumulatedRunHours; }
    public void setAccumulatedRunHours(BigDecimal accumulatedRunHours) { this.accumulatedRunHours = accumulatedRunHours; }
}
