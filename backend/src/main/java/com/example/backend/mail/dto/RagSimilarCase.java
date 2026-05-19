package com.example.backend.mail.dto;

import java.time.LocalDateTime;

public class RagSimilarCase {

	private Long alarmId;
	private String equipmentId;
	private String alarmType;
	private String alarmStatus;
	private String alarmMemo;
	private LocalDateTime timestamp;

	public Long getAlarmId() {
		return alarmId;
	}

	public void setAlarmId(Long alarmId) {
		this.alarmId = alarmId;
	}

	public String getEquipmentId() {
		return equipmentId;
	}

	public void setEquipmentId(String equipmentId) {
		this.equipmentId = equipmentId;
	}

	public String getAlarmType() {
		return alarmType;
	}

	public void setAlarmType(String alarmType) {
		this.alarmType = alarmType;
	}

	public String getAlarmStatus() {
		return alarmStatus;
	}

	public void setAlarmStatus(String alarmStatus) {
		this.alarmStatus = alarmStatus;
	}

	public String getAlarmMemo() {
		return alarmMemo;
	}

	public void setAlarmMemo(String alarmMemo) {
		this.alarmMemo = alarmMemo;
	}

	public LocalDateTime getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(LocalDateTime timestamp) {
		this.timestamp = timestamp;
	}
}
