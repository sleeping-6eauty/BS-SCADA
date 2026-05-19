package com.example.backend.alarm.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;

public class AlarmCreateRequest {

	private Long logId;

	@NotBlank(message = "equipmentId is required")
	private String equipmentId;

	private LocalDateTime timestamp;

	@NotBlank(message = "alarmType is required")
	private String alarmType;

	private String alarmMemo;
	private String alarmStatus;

	public Long getLogId() {
		return logId;
	}

	public void setLogId(Long logId) {
		this.logId = logId;
	}

	public String getEquipmentId() {
		return equipmentId;
	}

	public void setEquipmentId(String equipmentId) {
		this.equipmentId = equipmentId;
	}

	public LocalDateTime getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(LocalDateTime timestamp) {
		this.timestamp = timestamp;
	}

	public String getAlarmType() {
		return alarmType;
	}

	public void setAlarmType(String alarmType) {
		this.alarmType = alarmType;
	}

	public String getAlarmMemo() {
		return alarmMemo;
	}

	public void setAlarmMemo(String alarmMemo) {
		this.alarmMemo = alarmMemo;
	}

	public String getAlarmStatus() {
		return alarmStatus;
	}

	public void setAlarmStatus(String alarmStatus) {
		this.alarmStatus = alarmStatus;
	}
}
