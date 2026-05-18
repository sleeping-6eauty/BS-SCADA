package com.example.backend.mail.dto;

import jakarta.validation.constraints.NotBlank;

public class MailReportCreateRequest {

	private Long alarmId;

	@NotBlank(message = "mailText is required")
	private String mailText;

	public Long getAlarmId() {
		return alarmId;
	}

	public void setAlarmId(Long alarmId) {
		this.alarmId = alarmId;
	}

	public String getMailText() {
		return mailText;
	}

	public void setMailText(String mailText) {
		this.mailText = mailText;
	}
}
