package com.example.backend.dto;

public class ControlStatusResponse {

    private String equipmentId;
    private String currentStatus;   // RUN / STOP / IDLE / ALARM / UNKNOWN
    private String lastCommand;     // "on" / "off" / null
    private Integer lastFrequency;  // 마지막으로 전달된 주파수 (off 이후에도 유지)

    public ControlStatusResponse(String equipmentId, String currentStatus,
            String lastCommand, Integer lastFrequency) {
        this.equipmentId = equipmentId;
        this.currentStatus = currentStatus;
        this.lastCommand = lastCommand;
        this.lastFrequency = lastFrequency;
    }

    public String getEquipmentId()  { return equipmentId; }
    public String getCurrentStatus() { return currentStatus; }
    public String getLastCommand()  { return lastCommand; }
    public Integer getLastFrequency() { return lastFrequency; }
}
