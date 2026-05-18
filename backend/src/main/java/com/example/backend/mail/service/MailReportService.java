package com.example.backend.mail.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.StringJoiner;

import com.example.backend.mail.dto.AlarmContext;
import com.example.backend.mail.dto.DailyAlarmStatusCount;
import com.example.backend.mail.dto.DailyCountItem;
import com.example.backend.mail.dto.MailReport;
import com.example.backend.mail.dto.MailReportCreateRequest;
import com.example.backend.mail.dto.RagSimilarCase;
import com.example.backend.mail.mapper.MailReportMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MailReportService {

	private static final int SIMILAR_CASE_LIMIT = 3;
	private static final int TOP_LIMIT = 3;

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

	public MailReport createDailySummaryReport() {
		LocalDate reportDate = LocalDate.now(reportZoneId).minusDays(1);
		LocalDateTime from = reportDate.atStartOfDay();
		LocalDateTime to = reportDate.plusDays(1).atStartOfDay();

		DailyAlarmStatusCount status = mailReportMapper.countDailyStatus(from, to);
		List<DailyCountItem> topEquipments = mailReportMapper.findTopEquipmentsByPeriod(from, to, TOP_LIMIT);
		List<DailyCountItem> topTypes = mailReportMapper.findTopAlarmTypesByPeriod(from, to, TOP_LIMIT);

		MailReport mailReport = new MailReport();
		mailReport.setAlarmId(null);
		mailReport.setTimestamp(LocalDateTime.now(reportZoneId));
		mailReport.setMailText(buildDailySummaryText(reportDate, status, topEquipments, topTypes));

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

	private String buildDailySummaryText(
		LocalDate reportDate,
		DailyAlarmStatusCount status,
		List<DailyCountItem> topEquipments,
		List<DailyCountItem> topTypes
	) {
		DailyAlarmStatusCount safeStatus = status == null ? new DailyAlarmStatusCount() : status;
		StringBuilder builder = new StringBuilder();
		builder.append("[Daily Summary Report] ").append(reportDate).append("\n");
		builder.append("- Window: ").append(reportDate).append(" 00:00 ~ 23:59").append("\n");
		builder.append("- Total alarms: ").append(safeStatus.getTotalCount()).append("\n");
		builder.append("- By status: OPEN ").append(safeStatus.getOpenCount()).append(" / IN_PROGRESS ")
			.append(safeStatus.getInProgressCount()).append(" / RESOLVED ").append(safeStatus.getResolvedCount()).append("\n");
		builder.append("- Top equipments TOP").append(TOP_LIMIT).append(": ").append(joinTopItems(topEquipments)).append("\n");
		builder.append("- Top alarm types TOP").append(TOP_LIMIT).append(": ").append(joinTopItems(topTypes)).append("\n");
		builder.append("- Generated at: ").append(formatTime(LocalDateTime.now(reportZoneId)));
		return builder.toString();
	}

	private String joinTopItems(List<DailyCountItem> items) {
		if (items == null || items.isEmpty()) {
			return "No data";
		}
		StringJoiner joiner = new StringJoiner(", ");
		for (DailyCountItem item : items) {
			joiner.add(nullSafe(item.getLabel()) + " (" + item.getValue() + ")");
		}
		return joiner.toString();
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
