package com.example.backend.dto.dashboard;

public class LineOeeDto {
    private String lineNo;
    private double oee;
    private double availability;
    private double performance;
    private double quality;
    private String from;
    private String to;

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

    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }

    public String getTo() { return to; }
    public void setTo(String to) { this.to = to; }
}
