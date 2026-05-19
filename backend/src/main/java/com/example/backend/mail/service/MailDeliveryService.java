package com.example.backend.mail.service;

import java.util.Properties;

import com.example.backend.mail.dto.MailReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

@Service
public class MailDeliveryService {

	private static final Logger logger = LoggerFactory.getLogger(MailDeliveryService.class);

	private final String smtpHost;
	private final int smtpPort;
	private final String smtpUsername;
	private final String smtpPassword;
	private final boolean smtpAuth;
	private final boolean smtpStarttlsEnable;
	private final String from;

	public MailDeliveryService(
		@Value("${smtp.host:}") String smtpHost,
		@Value("${smtp.port:587}") int smtpPort,
		@Value("${smtp.username:}") String smtpUsername,
		@Value("${smtp.password:}") String smtpPassword,
		@Value("${smtp.auth:true}") boolean smtpAuth,
		@Value("${smtp.starttls-enable:true}") boolean smtpStarttlsEnable,
		@Value("${mail.report.from:no-reply@localhost}") String from
	) {
		this.smtpHost = smtpHost;
		this.smtpPort = smtpPort;
		this.smtpUsername = smtpUsername;
		this.smtpPassword = smtpPassword;
		this.smtpAuth = smtpAuth;
		this.smtpStarttlsEnable = smtpStarttlsEnable;
		this.from = from;
	}

	public void send(MailReport mailReport) {
		if (mailReport.getRecipientEmail() == null || mailReport.getRecipientEmail().isBlank()) {
			logger.info("mail report delivery skipped because recipient email is empty: mailId={}", mailReport.getMailId());
			return;
		}

		if (smtpHost == null || smtpHost.isBlank()) {
			logger.warn("mail report delivery skipped because SMTP is not configured: mailId={}", mailReport.getMailId());
			return;
		}

		JavaMailSenderImpl mailSender = createMailSender();
		SimpleMailMessage message = new SimpleMailMessage();
		message.setFrom(from);
		message.setTo(mailReport.getRecipientEmail());
		message.setSubject(subject(mailReport));
		message.setText(mailReport.getMailText());

		try {
			mailSender.send(message);
			logger.info("mail report delivered: mailId={}, recipient={}", mailReport.getMailId(), mailReport.getRecipientEmail());
		} catch (MailException exception) {
			logger.error(
				"failed to deliver mail report: mailId={}, recipient={}",
				mailReport.getMailId(),
				mailReport.getRecipientEmail(),
				exception
			);
		}
	}

	private String subject(MailReport mailReport) {
		if (mailReport.getMailSubject() != null && !mailReport.getMailSubject().isBlank()) {
			return mailReport.getMailSubject();
		}
		String reportType = mailReport.getReportType() == null ? "MAIL" : mailReport.getReportType();
		if ("DAILY".equalsIgnoreCase(reportType)) {
			return "[BS-SCADA] Daily alarm report";
		}
		if ("ALARM".equalsIgnoreCase(reportType)) {
			return "[BS-SCADA] Low health alarm report";
		}
		return "[BS-SCADA] Mail report";
	}

	private JavaMailSenderImpl createMailSender() {
		JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
		mailSender.setHost(smtpHost);
		mailSender.setPort(smtpPort);
		if (smtpUsername != null && !smtpUsername.isBlank()) {
			mailSender.setUsername(smtpUsername);
		}
		if (smtpPassword != null && !smtpPassword.isBlank()) {
			mailSender.setPassword(smtpPassword);
		}

		Properties properties = mailSender.getJavaMailProperties();
		properties.put("mail.smtp.auth", String.valueOf(smtpAuth && smtpUsername != null && !smtpUsername.isBlank()));
		properties.put("mail.smtp.starttls.enable", String.valueOf(smtpStarttlsEnable));
		return mailSender;
	}
}
