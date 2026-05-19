package com.example.backend.mail.analysis;

import java.time.format.DateTimeFormatter;
import java.util.List;

import com.example.backend.mail.dto.AlarmContext;
import com.example.backend.mail.dto.RagSimilarCase;
import org.springframework.stereotype.Component;

@Component
public class AlarmAnalysisContextProvider {

	public String buildContext(AlarmContext alarm, List<RagSimilarCase> similarCases) {
		StringBuilder builder = new StringBuilder();
		builder.append("[Current Alarm]\n");
		builder.append("alarm_id: ").append(alarm.getAlarmId()).append('\n');
		builder.append("equipment_id: ").append(nullSafe(alarm.getEquipmentId())).append('\n');
		builder.append("alarm_type: ").append(nullSafe(alarm.getAlarmType())).append('\n');
		builder.append("alarm_status: ").append(nullSafe(alarm.getAlarmStatus())).append('\n');
		builder.append("health_score: ").append(alarm.getHealthScore() == null ? "UNKNOWN" : alarm.getHealthScore()).append('\n');
		builder.append("detected_at: ").append(alarm.getTimestamp() == null ? "UNKNOWN" : alarm.getTimestamp().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append('\n');
		builder.append("alarm_text: ").append(nullSafe(alarm.getAlarmText())).append('\n');
		builder.append("alarm_memo: ").append(nullSafe(alarm.getAlarmMemo())).append("\n\n");

		builder.append("[Similar Resolved Cases]\n");
		if (similarCases == null || similarCases.isEmpty()) {
			builder.append("none\n");
			return builder.toString();
		}

		for (int i = 0; i < similarCases.size(); i++) {
			RagSimilarCase similarCase = similarCases.get(i);
			builder.append(i + 1).append(". ");
			builder.append("alarm_id=").append(similarCase.getAlarmId()).append(", ");
			builder.append("equipment_id=").append(nullSafe(similarCase.getEquipmentId())).append(", ");
			builder.append("alarm_type=").append(nullSafe(similarCase.getAlarmType())).append(", ");
			builder.append("status=").append(nullSafe(similarCase.getAlarmStatus())).append(", ");
			builder.append("score=").append(similarCase.getSimilarityScore() == null ? "UNKNOWN" : similarCase.getSimilarityScore()).append(", ");
			builder.append("resolved_text=").append(nullSafe(similarCase.getAlarmMemo())).append('\n');
		}
		return builder.toString();
	}

	private String nullSafe(String value) {
		return value == null || value.isBlank() ? "UNKNOWN" : value.trim();
	}
}
