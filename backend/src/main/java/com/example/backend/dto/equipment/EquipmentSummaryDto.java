package com.example.backend.dto.equipment;

import java.time.LocalDate;

public class EquipmentSummaryDto {
    private String equipmentId;
    private String equipmentName;
    private String lineNo;
    private String zone;
    private long runTimeSec;
    private long idleTimeSec;
    private long stopTimeSec;
    private long alarmTimeSec;
    private double availability;
    private long failureCount;
    private long mttfSec;
    private long mttrSec;
    private long mtbfSec;
    private double healthScore;
    private double remainingLife;
    private LocalDate replacementDate;

    public String getEquipmentId() { return equipmentId; }
    public void setEquipmentId(String equipmentId) { this.equipmentId = equipmentId; }

    public String getEquipmentName() { return equipmentName; }
    public void setEquipmentName(String equipmentName) { this.equipmentName = equipmentName; }

    public String getLineNo() { return lineNo; }
    public void setLineNo(String lineNo) { this.lineNo = lineNo; }

    public String getZone() { return zone; }
    public void setZone(String zone) { this.zone = zone; }

    public long getRunTimeSec() { return runTimeSec; }
    public void setRunTimeSec(long runTimeSec) { this.runTimeSec = runTimeSec; }

    public long getIdleTimeSec() { return idleTimeSec; }
    public void setIdleTimeSec(long idleTimeSec) { this.idleTimeSec = idleTimeSec; }

    public long getStopTimeSec() { return stopTimeSec; }
    public void setStopTimeSec(long stopTimeSec) { this.stopTimeSec = stopTimeSec; }

    public long getAlarmTimeSec() { return alarmTimeSec; }
    public void setAlarmTimeSec(long alarmTimeSec) { this.alarmTimeSec = alarmTimeSec; }

    public double getAvailability() { return availability; }
    public void setAvailability(double availability) { this.availability = availability; }

    public long getFailureCount() { return failureCount; }
    public void setFailureCount(long failureCount) { this.failureCount = failureCount; }

    public long getMttfSec() { return mttfSec; }
    public void setMttfSec(long mttfSec) { this.mttfSec = mttfSec; }

    public long getMttrSec() { return mttrSec; }
    public void setMttrSec(long mttrSec) { this.mttrSec = mttrSec; }

    public long getMtbfSec() { return mtbfSec; }
    public void setMtbfSec(long mtbfSec) { this.mtbfSec = mtbfSec; }

    public double getHealthScore() { return healthScore; }
    public void setHealthScore(double healthScore) { this.healthScore = healthScore; }

    public double getRemainingLife() { return remainingLife; }
    public void setRemainingLife(double remainingLife) { this.remainingLife = remainingLife; }

    public LocalDate getReplacementDate() { return replacementDate; }
    public void setReplacementDate(LocalDate replacementDate) { this.replacementDate = replacementDate; }
}
