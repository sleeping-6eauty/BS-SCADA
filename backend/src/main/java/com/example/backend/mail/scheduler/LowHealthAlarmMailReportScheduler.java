package com.example.backend.mail.scheduler;

import java.util.List;

import com.example.backend.mail.dto.MailReport;
import com.example.backend.mail.service.MailReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class LowHealthAlarmMailReportScheduler {

	private static final Logger logger = LoggerFactory.getLogger(LowHealthAlarmMailReportScheduler.class);

	private final MailReportService mailReportService;
	private final double healthScoreThreshold;
	private final int batchSize;

	public LowHealthAlarmMailReportScheduler(
		MailReportService mailReportService,
		@Value("${mail.report.low-health.threshold:70}") double healthScoreThreshold,
		@Value("${mail.report.low-health.batch-size:50}") int batchSize
	) {
		this.mailReportService = mailReportService;
		this.healthScoreThreshold = healthScoreThreshold;
		this.batchSize = batchSize;
	}

	@Scheduled(fixedDelayString = "${mail.report.low-health.scan-delay-ms:30000}")
	public void createPendingReportsForLowHealthAlarms() {
		try {
			List<MailReport> reports = mailReportService.createPendingLowHealthAlarmReports(healthScoreThreshold, batchSize);
			if (!reports.isEmpty()) {
				logger.info("pending low health alarm mail reports generated: {}", reports.size());
			}
		} catch (Exception exception) {
			logger.error("failed to generate pending low health alarm mail reports", exception);
		}
	}
}
