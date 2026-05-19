package com.example.backend.mail.dto;

public class DailyMailRecipientSummary {

	private Long recipientUserId;
	private String recipientEmail;
	private String recipientName;
	private long reportCount;

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

	public long getReportCount() {
		return reportCount;
	}

	public void setReportCount(long reportCount) {
		this.reportCount = reportCount;
	}
}
