package com.example.backend.dto.equipment;

import java.time.LocalDate;

public class EquipmentMetricsDto {
    private double availability;
    private long failureCount;
    private long mttfSec;
    private long mttrSec;
    private long mtbfSec;
    private double healthScore;
    private double remainingLife;
    private LocalDate replacementDate;
    private long alarmCount24h;
    private double sensorAbnormalRatio;

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

    public long getAlarmCount24h() { return alarmCount24h; }
    public void setAlarmCount24h(long alarmCount24h) { this.alarmCount24h = alarmCount24h; }

    public double getSensorAbnormalRatio() { return sensorAbnormalRatio; }
    public void setSensorAbnormalRatio(double sensorAbnormalRatio) { this.sensorAbnormalRatio = sensorAbnormalRatio; }
}
