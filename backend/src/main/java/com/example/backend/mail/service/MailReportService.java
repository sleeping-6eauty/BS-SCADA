package com.example.backend.mail.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import com.example.backend.mail.analysis.AlarmAnalysisResult;
import com.example.backend.mail.analysis.AlarmAnalysisService;
import com.example.backend.mail.dto.AlarmContext;
import com.example.backend.mail.dto.DailyMailRecipientSummary;
import com.example.backend.mail.dto.DailyMailReportItem;
import com.example.backend.mail.dto.MailReport;
import com.example.backend.mail.dto.MailReportCreateRequest;
import com.example.backend.mail.dto.RagSimilarCase;
import com.example.backend.mail.mapper.MailReportMapper;
import com.example.backend.mail.rag.VectorRagService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MailReportService {

	private static final int SIMILAR_CASE_LIMIT = 3;

	private final MailReportMapper mailReportMapper;
	private final MailDeliveryService mailDeliveryService;
	private final VectorRagService vectorRagService;
	private final AlarmAnalysisService alarmAnalysisService;
	private final ZoneId reportZoneId;

	public MailReportService(
		MailReportMapper mailReportMapper,
		MailDeliveryService mailDeliveryService,
		VectorRagService vectorRagService,
		AlarmAnalysisService alarmAnalysisService,
		@Value("${mail.report.timezone:Asia/Seoul}") String reportTimezone
	) {
		this.mailReportMapper = mailReportMapper;
		this.mailDeliveryService = mailDeliveryService;
		this.vectorRagService = vectorRagService;
		this.alarmAnalysisService = alarmAnalysisService;
		this.reportZoneId = ZoneId.of(reportTimezone);
	}

	public MailReport create(MailReportCreateRequest request) {
		MailReport mailReport = new MailReport();
		mailReport.setAlarmId(request.getAlarmId());
		mailReport.setReportType(request.getAlarmId() == null ? "MANUAL" : "ALARM");
		mailReport.setTimestamp(LocalDateTime.now());
		mailReport.setMailText(buildMailTextWithRag(request));

		mailReportMapper.insert(mailReport);
		mailDeliveryService.send(mailReport);
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
		mailDeliveryService.send(mailReport);
		return mailReport;
	}

	public List<MailReport> createLowHealthAlarmReportsForAlarm(long alarmId, double healthScoreThreshold) {
		List<AlarmContext> alarms = mailReportMapper.findLowHealthAlarmRecipientsByAlarmId(alarmId, healthScoreThreshold);
		return createLowHealthAlarmReports(alarms);
	}

	public List<MailReport> createPendingLowHealthAlarmReports(double healthScoreThreshold, int limit) {
		List<AlarmContext> alarms = mailReportMapper.findLowHealthAlarmRecipientsWithoutMailReport(healthScoreThreshold, limit);
		return createLowHealthAlarmReports(alarms);
	}

	private List<MailReport> createLowHealthAlarmReports(List<AlarmContext> alarmRecipients) {
		Map<Long, List<AlarmContext>> recipientsByAlarm = new LinkedHashMap<>();
		for (AlarmContext alarmRecipient : alarmRecipients) {
			if (alarmRecipient.getAlarmId() == null) {
				continue;
			}
			recipientsByAlarm.computeIfAbsent(alarmRecipient.getAlarmId(), key -> new ArrayList<>()).add(alarmRecipient);
		}

		List<MailReport> reports = new ArrayList<>();
		for (List<AlarmContext> recipients : recipientsByAlarm.values()) {
			reports.add(createLowHealthAlarmReport(recipients.get(0), recipients));
		}
		return reports;
	}

	private MailReport createLowHealthAlarmReport(AlarmContext alarm, List<AlarmContext> recipients) {
		MailReport mailReport = new MailReport();
		mailReport.setAlarmId(alarm.getAlarmId());
		mailReport.setReportType("ALARM");
		mailReport.setRecipientUserId(alarm.getRecipientUserId());
		mailReport.setRecipientEmail(alarm.getRecipientEmail());
		mailReport.setRecipientName(alarm.getRecipientName());
		mailReport.setTimestamp(LocalDateTime.now(reportZoneId));
		mailReport.setMailSubject(buildLowHealthAlarmSubject(alarm));
		mailReport.setMailText(buildLowHealthAlarmText(alarm));

		mailReportMapper.insert(mailReport);
		for (AlarmContext recipient : recipients) {
			mailReport.setRecipientUserId(recipient.getRecipientUserId());
			mailReport.setRecipientEmail(recipient.getRecipientEmail());
			mailReport.setRecipientName(recipient.getRecipientName());
			mailDeliveryService.send(mailReport);
		}
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

		List<RagSimilarCase> similarCases = vectorRagService.findSimilarCases(alarm, SIMILAR_CASE_LIMIT);
		AlarmAnalysisResult analysis = alarmAnalysisService.analyze(alarm, similarCases);
		return baseText + "\n\n" + buildRagSection(alarm, similarCases) + "\n\n" + buildPlainAnalysisSection(analysis);
	}

	private String buildLowHealthAlarmText(AlarmContext alarm) {
		List<RagSimilarCase> similarCases = vectorRagService.findSimilarCases(alarm, SIMILAR_CASE_LIMIT);
		AlarmAnalysisResult analysis = alarmAnalysisService.analyze(alarm, similarCases);

		StringBuilder builder = new StringBuilder();
		builder.append("안녕하세요, 설비통합관리시스템입니다.\n\n");
		builder.append("금일 ").append(nullSafe(alarm.getEquipmentId()))
			.append(" 설비에서 이상 상태가 감지되어 아래와 같이 설비 이상 알림 메일을 송부드립니다.\n\n");
		builder.append("1. 이상 발생 개요\n\n");
		builder.append("- 발생 시간: ").append(formatTime(alarm.getTimestamp())).append("\n\n");
		builder.append("- 대상 설비: ").append(nullSafe(alarm.getEquipmentId())).append("\n\n");
		builder.append("- 이상 유형: ").append(nullSafe(alarm.getAlarmType())).append("\n\n");
		builder.append("- 현재 상태: ").append(nullSafe(alarm.getAlarmStatus())).append("\n\n");
		builder.append("- Health score: ").append(alarm.getHealthScore() == null ? "UNKNOWN" : alarm.getHealthScore()).append("\n\n");
		builder.append("- AI 판단 결과: ").append(trimText(analysis.getAiJudgment(), 140)).append("\n\n");
		builder.append("2. 주요 감지 내용\n\n");
		appendBullets(builder, analysis.getDetectionDetails());
		builder.append("3. 예상 영향\n\n");
		appendBullets(builder, analysis.getExpectedImpacts());
		builder.append("4. RAG 기반 유사 사례\n\n");
		builder.append(buildKoreanRagSection(similarCases));
		builder.append("\n\n5. MCP/LLM 분석 기반 추천 조치\n\n");
		appendBullets(builder, analysis.getRecommendedActions());
		return builder.toString();
	}

	private String buildLowHealthAlarmSubject(AlarmContext alarm) {
		return "[설비 이상 알림] " + nullSafe(alarm.getEquipmentId()) + " " + nullSafe(alarm.getAlarmType()) + " 감지";
	}

	private String buildKoreanRagSection(List<RagSimilarCase> similarCases) {
		StringBuilder builder = new StringBuilder();
		if (similarCases == null || similarCases.isEmpty()) {
			builder.append("과거 유사 사례 검색 결과, 참고할 수 있는 해결 사례가 아직 없습니다.\n\n");
			builder.append("- 유사 사례 1: 점검 가능한 유사 사례가 없습니다.\n\n");
			builder.append("  - 추천 방안: 안전 확인 후 설비 상태 점검\n");
			builder.append("  - 조치 내용: 설비 로그 확인 및 육안 점검\n");
			return builder.toString();
		}

		builder.append("과거 유사 사례 검색 결과, 아래 사례와 유사한 패턴이 확인되었습니다.\n\n");
		for (int i = 0; i < similarCases.size(); i++) {
			RagSimilarCase similar = similarCases.get(i);
			builder.append("- 유사 사례 ").append(i + 1).append(": ")
				.append(nullSafe(similar.getEquipmentId())).append(" / ")
				.append(nullSafe(similar.getAlarmType())).append(" / ")
				.append(formatTime(similar.getTimestamp())).append("\n\n");
			builder.append("  - 추천 방안: ").append(trimText(similar.getAlarmMemo(), 80)).append("\n");
			builder.append("  - 조치 내용: 과거 RESOLVED 처리 내용을 참고하여 점검\n");
			if (similar.getSimilarityScore() != null) {
				builder.append("  - 유사도 점수: ").append(String.format("%.4f", similar.getSimilarityScore())).append("\n");
			}
			if (i < similarCases.size() - 1) {
				builder.append("\n");
			}
		}
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
			builder.append(trimText(similar.getAlarmMemo(), 90));
			if (similar.getSimilarityScore() != null) {
				builder.append(" (score ").append(String.format("%.4f", similar.getSimilarityScore())).append(")");
			}
			builder.append("\n");
		}
		builder.append("- Common recommendation: replay similar actions, then monitor recurrence for 30 minutes.");
		return builder.toString();
	}

	private String buildPlainAnalysisSection(AlarmAnalysisResult analysis) {
		StringBuilder builder = new StringBuilder();
		builder.append("[MCP/LLM Analysis]\n");
		builder.append("- AI judgment: ").append(trimText(analysis.getAiJudgment(), 140)).append("\n");
		builder.append("- Detection details: ").append(String.join(" / ", analysis.getDetectionDetails())).append("\n");
		builder.append("- Expected impacts: ").append(String.join(" / ", analysis.getExpectedImpacts())).append("\n");
		builder.append("- Recommended actions: ").append(String.join(" / ", analysis.getRecommendedActions()));
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

	private void appendBullets(StringBuilder builder, List<String> items) {
		if (items == null || items.isEmpty()) {
			builder.append("- 확인 가능한 분석 내용이 없습니다.\n\n");
			return;
		}
		for (String item : items) {
			builder.append("- ").append(trimText(item, 140)).append("\n\n");
		}
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
