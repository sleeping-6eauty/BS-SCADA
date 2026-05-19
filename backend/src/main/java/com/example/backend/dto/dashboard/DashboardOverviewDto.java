package com.example.backend.dto.dashboard;

import java.util.Map;

public class DashboardOverviewDto {
    private String lineNo;
    private double oee;
    private double availability;
    private double performance;
    private double quality;
    private long runTimeSec;
    private long idleTimeSec;
    private long stopTimeSec;
    private long alarmTimeSec;
    private long totalCount;
    private long goodCount;
    private long badCount;
    private long activeAlarmCount;
    private double avgHealthScore;
    private Map<String, Long> equipmentStatusCounts;

    public String getLineNo() { return lineNo; }
    public void setLineNo(String lineNo) { this.lineNo = lineNo; }

    public double getOee() { return oee; }
    public void setOee(double oee) { this.oee = oee; }

    public double getAvailability() { return availability; }
    public void setAvailability(double availability) { this.availability = availability; }

    public double getPerformance() { return performance; }
    public void setPerformance(double performance) { this.performance = performance; }

    public double getQuality() { return quality; }
    public void setQuality(double quality) { this.quality = quality; }

    public long getRunTimeSec() { return runTimeSec; }
    public void setRunTimeSec(long runTimeSec) { this.runTimeSec = runTimeSec; }

    public long getIdleTimeSec() { return idleTimeSec; }
    public void setIdleTimeSec(long idleTimeSec) { this.idleTimeSec = idleTimeSec; }

    public long getStopTimeSec() { return stopTimeSec; }
    public void setStopTimeSec(long stopTimeSec) { this.stopTimeSec = stopTimeSec; }

    public long getAlarmTimeSec() { return alarmTimeSec; }
    public void setAlarmTimeSec(long alarmTimeSec) { this.alarmTimeSec = alarmTimeSec; }

    public long getTotalCount() { return totalCount; }
    public void setTotalCount(long totalCount) { this.totalCount = totalCount; }

    public long getGoodCount() { return goodCount; }
    public void setGoodCount(long goodCount) { this.goodCount = goodCount; }

    public long getBadCount() { return badCount; }
    public void setBadCount(long badCount) { this.badCount = badCount; }

    public long getActiveAlarmCount() { return activeAlarmCount; }
    public void setActiveAlarmCount(long activeAlarmCount) { this.activeAlarmCount = activeAlarmCount; }

    public double getAvgHealthScore() { return avgHealthScore; }
    public void setAvgHealthScore(double avgHealthScore) { this.avgHealthScore = avgHealthScore; }

    public Map<String, Long> getEquipmentStatusCounts() { return equipmentStatusCounts; }
    public void setEquipmentStatusCounts(Map<String, Long> equipmentStatusCounts) { this.equipmentStatusCounts = equipmentStatusCounts; }
}
