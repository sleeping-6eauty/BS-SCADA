package com.example.backend.dto.dashboard;

public class SummaryGenerateResponse {
    private String message;
    private String from;
    private String to;
    private int lineSummaryCount;
    private int equipmentSummaryCount;

    public SummaryGenerateResponse() {}

    public SummaryGenerateResponse(String message, String from, String to, int lineSummaryCount, int equipmentSummaryCount) {
        this.message = message;
        this.from = from;
        this.to = to;
        this.lineSummaryCount = lineSummaryCount;
        this.equipmentSummaryCount = equipmentSummaryCount;
    }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }

    public String getTo() { return to; }
    public void setTo(String to) { this.to = to; }

    public int getLineSummaryCount() { return lineSummaryCount; }
    public void setLineSummaryCount(int lineSummaryCount) { this.lineSummaryCount = lineSummaryCount; }

    public int getEquipmentSummaryCount() { return equipmentSummaryCount; }
    public void setEquipmentSummaryCount(int equipmentSummaryCount) { this.equipmentSummaryCount = equipmentSummaryCount; }
}
