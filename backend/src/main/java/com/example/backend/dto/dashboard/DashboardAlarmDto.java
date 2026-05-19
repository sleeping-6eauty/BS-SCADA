package com.example.backend.dto.dashboard;

public class DashboardAlarmDto {
    private Long alarmId;
    private String equipmentId;
    private String timestamp;
    private String alarmType;
    private String alarmText;
    private String alarmMemo;
    private String alarmStatus;

    public Long getAlarmId() { return alarmId; }
    public void setAlarmId(Long alarmId) { this.alarmId = alarmId; }

    public String getEquipmentId() { return equipmentId; }
    public void setEquipmentId(String equipmentId) { this.equipmentId = equipmentId; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public String getAlarmType() { return alarmType; }
    public void setAlarmType(String alarmType) { this.alarmType = alarmType; }

    public String getAlarmText() { return alarmText; }
    public void setAlarmText(String alarmText) { this.alarmText = alarmText; }

    public String getAlarmMemo() { return alarmMemo; }
    public void setAlarmMemo(String alarmMemo) { this.alarmMemo = alarmMemo; }

    public String getAlarmStatus() { return alarmStatus; }
    public void setAlarmStatus(String alarmStatus) { this.alarmStatus = alarmStatus; }
}
