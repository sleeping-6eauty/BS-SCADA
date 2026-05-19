package com.example.backend.mail.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.NoSuchElementException;

import com.example.backend.mail.dto.AlarmContext;
import com.example.backend.mail.dto.DailyMailRecipientSummary;
import com.example.backend.mail.dto.DailyMailReportItem;
import com.example.backend.mail.dto.MailReport;
import com.example.backend.mail.dto.MailReportCreateRequest;
import com.example.backend.mail.dto.RagSimilarCase;
import com.example.backend.mail.mapper.MailReportMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MailReportService {

	private static final int SIMILAR_CASE_LIMIT = 3;

	private final MailReportMapper mailReportMapper;
	private final ZoneId reportZoneId;

	public MailReportService(
		MailReportMapper mailReportMapper,
		@Value("${mail.report.timezone:Asia/Seoul}") String reportTimezone
	) {
		this.mailReportMapper = mailReportMapper;
		this.reportZoneId = ZoneId.of(reportTimezone);
	}

	public MailReport create(MailReportCreateRequest request) {
		MailReport mailReport = new MailReport();
		mailReport.setAlarmId(request.getAlarmId());
		mailReport.setReportType(request.getAlarmId() == null ? "MANUAL" : "ALARM");
		mailReport.setTimestamp(LocalDateTime.now());
		mailReport.setMailText(buildMailTextWithRag(request));

		mailReportMapper.insert(mailReport);
		return mailReport;
	}

	public List<MailReport> findAll(Long alarmId) {
		return mailReportMapper.findAll(alarmId);
	}

	public MailReport findById(long mailId) {
		MailReport report = mailReportMapper.findById(mailId);
		if (report == null) {
			throw new NoSuchElementException("mail report not found: " + mailId);
		}
		return report;
	}

	public List<MailReport> createDailySummaryReport() {
		LocalDate reportDate = LocalDate.now(reportZoneId).minusDays(1);
		LocalDateTime from = reportDate.atStartOfDay();
		LocalDateTime to = reportDate.plusDays(1).atStartOfDay();

		List<DailyMailRecipientSummary> summaries = mailReportMapper.findDailyRecipientSummaries(reportDate, from, to);
		return summaries.stream()
			.map(summary -> createDailySummaryReport(summary, reportDate, from, to))
			.toList();
	}

	private MailReport createDailySummaryReport(
		DailyMailRecipientSummary summary,
		LocalDate reportDate,
		LocalDateTime from,
		LocalDateTime to
	) {
		List<DailyMailReportItem> items = mailReportMapper.findDailyReportItems(summary.getRecipientUserId(), from, to);
		MailReport mailReport = new MailReport();
		mailReport.setAlarmId(null);
		mailReport.setReportType("DAILY");
		mailReport.setRecipientUserId(summary.getRecipientUserId());
		mailReport.setRecipientEmail(summary.getRecipientEmail());
		mailReport.setRecipientName(summary.getRecipientName());
		mailReport.setSourceReportDate(reportDate);
		mailReport.setTimestamp(LocalDateTime.now(reportZoneId));
		mailReport.setMailText(buildDailySummaryText(reportDate, summary, items));

		mailReportMapper.insert(mailReport);
		return mailReport;
	}

	public List<MailReport> createLowHealthAlarmReportsForAlarm(long alarmId, double healthScoreThreshold) {
		List<AlarmContext> alarms = mailReportMapper.findLowHealthAlarmRecipientsByAlarmId(alarmId, healthScoreThreshold);
		return alarms.stream()
			.map(this::createLowHealthAlarmReport)
			.toList();
	}

	public List<MailReport> createPendingLowHealthAlarmReports(double healthScoreThreshold, int limit) {
		List<AlarmContext> alarms = mailReportMapper.findLowHealthAlarmRecipientsWithoutMailReport(healthScoreThreshold, limit);
		return alarms.stream()
			.map(this::createLowHealthAlarmReport)
			.toList();
	}

	private MailReport createLowHealthAlarmReport(AlarmContext alarm) {
		MailReport mailReport = new MailReport();
		mailReport.setAlarmId(alarm.getAlarmId());
		mailReport.setReportType("ALARM");
		mailReport.setRecipientUserId(alarm.getRecipientUserId());
		mailReport.setRecipientEmail(alarm.getRecipientEmail());
		mailReport.setRecipientName(alarm.getRecipientName());
		mailReport.setTimestamp(LocalDateTime.now(reportZoneId));
		mailReport.setMailText(buildLowHealthAlarmText(alarm));

		mailReportMapper.insert(mailReport);
		return mailReport;
	}

	private String buildMailTextWithRag(MailReportCreateRequest request) {
		String baseText = request.getMailText().trim();
		if (request.getAlarmId() == null) {
			return baseText;
		}

		AlarmContext alarm = mailReportMapper.findAlarmContextById(request.getAlarmId());
		if (alarm == null) {
			return baseText + "\n\n[RAG Response]\n- Alarm context not found, so only base content was sent.";
		}

		List<RagSimilarCase> similarCases = mailReportMapper.findSimilarResolvedCases(
			alarm.getAlarmId(),
			alarm.getEquipmentId(),
			alarm.getAlarmType(),
			SIMILAR_CASE_LIMIT
		);

		return baseText + "\n\n" + buildRagSection(alarm, similarCases);
	}

	private String buildLowHealthAlarmText(AlarmContext alarm) {
		StringBuilder builder = new StringBuilder();
		builder.append("[Low Health Alarm Report]\n");
		builder.append("- Recipient: ").append(nullSafe(alarm.getRecipientName()))
			.append(" <").append(nullSafe(alarm.getRecipientEmail())).append(">\n");
		builder.append("- Alarm ID: ").append(alarm.getAlarmId()).append("\n");
		builder.append("- Equipment: ").append(nullSafe(alarm.getEquipmentId())).append("\n");
		builder.append("- Health score: ").append(alarm.getHealthScore() == null ? "UNKNOWN" : alarm.getHealthScore()).append("\n");
		builder.append("- Alarm type: ").append(nullSafe(alarm.getAlarmType())).append("\n");
		builder.append("- Alarm status: ").append(nullSafe(alarm.getAlarmStatus())).append("\n");
		builder.append("- Detected at: ").append(formatTime(alarm.getTimestamp())).append("\n\n");

		List<RagSimilarCase> similarCases = mailReportMapper.findSimilarResolvedCases(
			alarm.getAlarmId(),
			alarm.getEquipmentId(),
			alarm.getAlarmType(),
			SIMILAR_CASE_LIMIT
		);
		builder.append(buildRagSection(alarm, similarCases));
		return builder.toString();
	}

	private String buildRagSection(AlarmContext alarm, List<RagSimilarCase> similarCases) {
		StringBuilder builder = new StringBuilder();
		builder.append("[RAG Response]\n");
		builder.append("- Base alarm: ").append(nullSafe(alarm.getEquipmentId()))
			.append(" / ").append(nullSafe(alarm.getAlarmType()))
			.append(" / status ").append(nullSafe(alarm.getAlarmStatus())).append("\n");

		if (similarCases.isEmpty()) {
			builder.append("- No similar RESOLVED cases found. Use default incident handling flow.\n");
			builder.append("- Recommended: safety check -> equipment restart validation -> owner escalation");
			return builder.toString();
		}

		builder.append("- Recommended actions based on ").append(similarCases.size()).append(" similar RESOLVED cases\n");
		for (int i = 0; i < similarCases.size(); i++) {
			RagSimilarCase similar = similarCases.get(i);
			builder.append(i + 1).append(". ");
			builder.append("Case#").append(similar.getAlarmId()).append(" ");
			builder.append("(").append(formatTime(similar.getTimestamp())).append(") ");
			builder.append(trimText(similar.getAlarmMemo(), 90)).append("\n");
		}
		builder.append("- Common recommendation: replay similar actions, then monitor recurrence for 30 minutes.");
		return builder.toString();
	}

	private String buildDailySummaryText(LocalDate reportDate, DailyMailRecipientSummary summary, List<DailyMailReportItem> items) {
		StringBuilder builder = new StringBuilder();
		builder.append("[Daily Summary Report] ").append(reportDate).append("\n");
		builder.append("- Recipient: ").append(nullSafe(summary.getRecipientName()))
			.append(" <").append(nullSafe(summary.getRecipientEmail())).append(">\n");
		builder.append("- Window: ").append(reportDate).append(" 00:00 ~ 23:59").append("\n");
		builder.append("- Mail reports generated: ").append(summary.getReportCount()).append("\n");
		builder.append("- Low health alarms:\n");
		if (items == null || items.isEmpty()) {
			builder.append("  No generated alarm mail reports.\n");
		} else {
			for (DailyMailReportItem item : items) {
				builder.append("  - Mail#").append(item.getMailId())
					.append(" / Alarm#").append(item.getAlarmId())
					.append(" / Equipment ").append(nullSafe(item.getEquipmentId()))
					.append(" / ").append(nullSafe(item.getAlarmType()))
					.append(" / health ").append(item.getHealthScore() == null ? "UNKNOWN" : item.getHealthScore())
					.append("\n");
			}
		}
		builder.append("- Generated at: ").append(formatTime(LocalDateTime.now(reportZoneId)));
		return builder.toString();
	}

	private String trimText(String source, int maxLength) {
		if (source == null || source.isBlank()) {
			return "No memo";
		}
		String normalized = source.replaceAll("\\s+", " ").trim();
		if (normalized.length() <= maxLength) {
			return normalized;
		}
		return normalized.substring(0, maxLength) + "...";
	}

	private String formatTime(LocalDateTime time) {
		if (time == null) {
			return "Unknown time";
		}
		return time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
	}

	private String nullSafe(String value) {
		return value == null || value.isBlank() ? "UNKNOWN" : value;
	}
}
