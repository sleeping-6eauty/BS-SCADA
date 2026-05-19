package com.example.backend.dto;

public class DashboardSummary {
    private double oee;
    private long totalEquipments;
    private long runningEquipments;
    private long alarmEquipments;
    private double avgHealthScore;

    public DashboardSummary() {}

    public DashboardSummary(double oee, long totalEquipments, long runningEquipments, long alarmEquipments, double avgHealthScore) {
        this.oee = oee;
        this.totalEquipments = totalEquipments;
        this.runningEquipments = runningEquipments;
        this.alarmEquipments = alarmEquipments;
        this.avgHealthScore = avgHealthScore;
    }

    public double getOee() { return oee; }
    public void setOee(double oee) { this.oee = oee; }

    public long getTotalEquipments() { return totalEquipments; }
    public void setTotalEquipments(long totalEquipments) { this.totalEquipments = totalEquipments; }

    public long getRunningEquipments() { return runningEquipments; }
    public void setRunningEquipments(long runningEquipments) { this.runningEquipments = runningEquipments; }

    public long getAlarmEquipments() { return alarmEquipments; }
    public void setAlarmEquipments(long alarmEquipments) { this.alarmEquipments = alarmEquipments; }

    public double getAvgHealthScore() { return avgHealthScore; }
    public void setAvgHealthScore(double avgHealthScore) { this.avgHealthScore = avgHealthScore; }
}
