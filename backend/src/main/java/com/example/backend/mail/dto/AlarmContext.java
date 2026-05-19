package com.example.backend.mail.dto;

import java.time.LocalDateTime;

public class AlarmContext {

	private Long alarmId;
	private String equipmentId;
	private String alarmType;
	private String alarmStatus;
	private String alarmMemo;
	private Float healthScore;
	private Long recipientUserId;
	private String recipientEmail;
	private String recipientName;
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

	public Float getHealthScore() {
		return healthScore;
	}

	public void setHealthScore(Float healthScore) {
		this.healthScore = healthScore;
	}

	public Long getRecipientUserId() {
		return recipientUserId;
	}

	public void setRecipientUserId(Long recipientUserId) {
		this.recipientUserId = recipientUserId;
	}

	public String getRecipientEmail() {
		return recipientEmail;
	}

	public void setRecipientEmail(String recipientEmail) {
		this.recipientEmail = recipientEmail;
	}

	public String getRecipientName() {
		return recipientName;
	}

	public void setRecipientName(String recipientName) {
		this.recipientName = recipientName;
	}

	public LocalDateTime getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(LocalDateTime timestamp) {
		this.timestamp = timestamp;
	}
}
