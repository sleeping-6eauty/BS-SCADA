package com.example.backend.mail.scheduler;

import java.util.List;

import com.example.backend.mail.dto.MailReport;
import com.example.backend.mail.service.MailReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DailyMailReportScheduler {

	private static final Logger logger = LoggerFactory.getLogger(DailyMailReportScheduler.class);

	private final MailReportService mailReportService;

	public DailyMailReportScheduler(MailReportService mailReportService) {
		this.mailReportService = mailReportService;
	}

	@Scheduled(
		cron = "${mail.report.daily.cron:0 30 8 * * *}",
		zone = "${mail.report.timezone:Asia/Seoul}"
	)
	public void createDailySummaryReport() {
		try {
			List<MailReport> reports = mailReportService.createDailySummaryReport();
			logger.info("daily mail reports generated: {}", reports.size());
		} catch (Exception exception) {
			logger.error("failed to generate daily mail report", exception);
		}
	}
}
