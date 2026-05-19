package com.example.backend.mail.dto;

import java.time.LocalDateTime;
import java.time.LocalDate;

public class MailReport {

	private Long mailId;
	private Long alarmId;
	private String reportType;
	private Long recipientUserId;
	private String recipientEmail;
	private String recipientName;
	private LocalDate sourceReportDate;
	private LocalDateTime timestamp;
	private String mailText;

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

	public String getReportType() {
		return reportType;
	}

	public void setReportType(String reportType) {
		this.reportType = reportType;
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

	public LocalDate getSourceReportDate() {
		return sourceReportDate;
	}

	public void setSourceReportDate(LocalDate sourceReportDate) {
		this.sourceReportDate = sourceReportDate;
	}

	public LocalDateTime getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(LocalDateTime timestamp) {
		this.timestamp = timestamp;
	}

	public String getMailText() {
		return mailText;
	}

	public void setMailText(String mailText) {
		this.mailText = mailText;
	}
}
