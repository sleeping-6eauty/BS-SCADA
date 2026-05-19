package com.example.backend.mail.dto;

public class DailyMailReportItem {

	private Long mailId;
	private Long alarmId;
	private String equipmentId;
	private String alarmType;
	private Float healthScore;

	public Long getMailId() {
		return mailId;
	}

	public void setMailId(Long mailId) {
		this.mailId = mailId;
	}

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

	public Float getHealthScore() {
		return healthScore;
	}

	public void setHealthScore(Float healthScore) {
		this.healthScore = healthScore;
	}
}
