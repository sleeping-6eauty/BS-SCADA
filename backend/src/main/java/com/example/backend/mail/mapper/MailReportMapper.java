package com.example.backend.mail.mapper;

import java.util.List;

import com.example.backend.mail.dto.AlarmContext;
import com.example.backend.mail.dto.DailyMailRecipientSummary;
import com.example.backend.mail.dto.DailyMailReportItem;
import com.example.backend.mail.dto.MailReport;
import com.example.backend.mail.dto.RagSimilarCase;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface MailReportMapper {

	@Insert("""
		<script>
		INSERT INTO mail_report
		<trim prefix="(" suffix=")" suffixOverrides=",">
			<if test="alarmId != null">
				alarm_id,
			</if>
			`timestamp`,
			mail_text
		</trim>
		VALUES
		<trim prefix="(" suffix=")" suffixOverrides=",">
			<if test="alarmId != null">
				#{alarmId},
			</if>
			COALESCE(#{timestamp}, CURRENT_TIMESTAMP),
			#{mailText}
		</trim>
		</script>
		""")
	@Options(useGeneratedKeys = true, keyProperty = "mailId")
	int insert(MailReport mailReport);

	@Select("""
		<script>
		SELECT
			mail_id AS mailId,
			alarm_id AS alarmId,
			`timestamp` AS timestamp,
			mail_text AS mailText
		FROM mail_report
		<where>
			<if test="alarmId != null">
				alarm_id = #{alarmId}
			</if>
		</where>
		ORDER BY `timestamp` DESC
		</script>
		""")
	List<MailReport> findAll(@Param("alarmId") Long alarmId);

	@Select("""
		SELECT
			mail_id AS mailId,
			alarm_id AS alarmId,
			`timestamp` AS timestamp,
			mail_text AS mailText
		FROM mail_report
		WHERE mail_id = #{mailId}
		""")
	MailReport findById(@Param("mailId") long mailId);

	@Select("""
		SELECT
			alarm_id AS alarmId,
			equipment_id AS equipmentId,
			alarm_type AS alarmType,
			alarm_status AS alarmStatus,
			alarm_memo AS alarmMemo,
			alarm_text AS alarmText,
			NULL AS healthScore,
			NULL AS recipientUserId,
			NULL AS recipientEmail,
			NULL AS recipientName,
			`timestamp` AS timestamp
		FROM alarm_log
		WHERE alarm_id = #{alarmId}
		""")
	AlarmContext findAlarmContextById(@Param("alarmId") long alarmId);

	@Select("""
		SELECT
			alarm_id AS alarmId,
			equipment_id AS equipmentId,
			alarm_type AS alarmType,
			alarm_status AS alarmStatus,
			alarm_memo AS alarmMemo,
			alarm_text AS alarmText,
			NULL AS healthScore,
			NULL AS recipientUserId,
			NULL AS recipientEmail,
			NULL AS recipientName,
			`timestamp` AS timestamp
		FROM alarm_log
		WHERE alarm_status = 'RESOLVED'
		ORDER BY COALESCE(updated_at, created_at, `timestamp`) DESC, alarm_id DESC
		LIMIT #{limit}
		""")
	List<AlarmContext> findResolvedAlarmContextsForIndex(@Param("limit") int limit);

	@Select("""
		SELECT
			al.alarm_id AS alarmId,
			al.equipment_id AS equipmentId,
			al.alarm_type AS alarmType,
			al.alarm_status AS alarmStatus,
			al.alarm_memo AS alarmMemo,
			al.alarm_text AS alarmText,
			e.health_score AS healthScore,
			u.user_id AS recipientUserId,
			u.email AS recipientEmail,
			u.name AS recipientName,
			al.`timestamp` AS timestamp
		FROM alarm_log al
		INNER JOIN equipment e ON e.equipment_id = al.equipment_id
		INNER JOIN user_equipment ue ON ue.equipment_id = al.equipment_id
		INNER JOIN users u ON u.user_id = ue.user_id
		WHERE al.alarm_id = #{alarmId}
		  AND e.health_score < #{threshold}
		  AND al.alarm_status IN ('OPEN', 'IN_PROGRESS')
		  AND (u.status IS NULL OR u.status = 'active')
		  AND NOT EXISTS (
		  	SELECT 1
		  	FROM mail_report mr
		  	WHERE mr.alarm_id = al.alarm_id
		  )
		ORDER BY u.user_id
		""")
	List<AlarmContext> findLowHealthAlarmRecipientsByAlarmId(
		@Param("alarmId") long alarmId,
		@Param("threshold") double threshold
	);

	@Select("""
		SELECT
			al.alarm_id AS alarmId,
			al.equipment_id AS equipmentId,
			al.alarm_type AS alarmType,
			al.alarm_status AS alarmStatus,
			al.alarm_memo AS alarmMemo,
			al.alarm_text AS alarmText,
			e.health_score AS healthScore,
			u.user_id AS recipientUserId,
			u.email AS recipientEmail,
			u.name AS recipientName,
			al.`timestamp` AS timestamp
		FROM alarm_log al
		INNER JOIN equipment e ON e.equipment_id = al.equipment_id
		INNER JOIN user_equipment ue ON ue.equipment_id = al.equipment_id
		INNER JOIN users u ON u.user_id = ue.user_id
		WHERE e.health_score < #{threshold}
		  AND al.alarm_status IN ('OPEN', 'IN_PROGRESS')
		  AND (u.status IS NULL OR u.status = 'active')
		  AND NOT EXISTS (
		  	SELECT 1
		  	FROM mail_report mr
		  	WHERE mr.alarm_id = al.alarm_id
		  )
		ORDER BY COALESCE(al.created_at, al.`timestamp`) ASC, al.alarm_id ASC, u.user_id ASC
		LIMIT #{limit}
		""")
	List<AlarmContext> findLowHealthAlarmRecipientsWithoutMailReport(
		@Param("threshold") double threshold,
		@Param("limit") int limit
	);

	@Select("""
		SELECT
			u.user_id AS recipientUserId,
			u.email AS recipientEmail,
			u.name AS recipientName,
			COUNT(*) AS reportCount
		FROM mail_report mr
		INNER JOIN alarm_log al ON al.alarm_id = mr.alarm_id
		INNER JOIN user_equipment ue ON ue.equipment_id = al.equipment_id
		INNER JOIN users u ON u.user_id = ue.user_id
		WHERE mr.`timestamp` >= #{from}
		  AND mr.`timestamp` < #{to}
		  AND (u.status IS NULL OR u.status = 'active')
		  AND NOT EXISTS (
		  	SELECT 1
		  	FROM mail_report daily
		  	WHERE daily.alarm_id IS NULL
		  	  AND daily.mail_text LIKE CONCAT('%[Daily Summary Report] ', #{reportDate}, '%')
		  	  AND daily.mail_text LIKE CONCAT('%Recipient: ', u.name, ' <', u.email, '>%')
		  )
		GROUP BY u.user_id, u.email, u.name
		ORDER BY u.user_id
		""")
	List<DailyMailRecipientSummary> findDailyRecipientSummaries(
		@Param("reportDate") java.time.LocalDate reportDate,
		@Param("from") java.time.LocalDateTime from,
		@Param("to") java.time.LocalDateTime to
	);

	@Select("""
		SELECT
			mr.mail_id AS mailId,
			mr.alarm_id AS alarmId,
			al.equipment_id AS equipmentId,
			al.alarm_type AS alarmType,
			e.health_score AS healthScore
		FROM mail_report mr
		LEFT JOIN alarm_log al ON al.alarm_id = mr.alarm_id
		LEFT JOIN equipment e ON e.equipment_id = al.equipment_id
		INNER JOIN user_equipment ue ON ue.equipment_id = al.equipment_id
		WHERE ue.user_id = #{recipientUserId}
		  AND mr.`timestamp` >= #{from}
		  AND mr.`timestamp` < #{to}
		  AND mr.alarm_id IS NOT NULL
		ORDER BY mr.`timestamp` ASC
		""")
	List<DailyMailReportItem> findDailyReportItems(
		@Param("recipientUserId") long recipientUserId,
		@Param("from") java.time.LocalDateTime from,
		@Param("to") java.time.LocalDateTime to
	);

	@Select("""
		<script>
		SELECT
			alarm_id AS alarmId,
			equipment_id AS equipmentId,
			alarm_type AS alarmType,
			alarm_status AS alarmStatus,
			COALESCE(alarm_text, alarm_memo) AS alarmMemo,
			`timestamp` AS timestamp
		FROM alarm_log
		WHERE alarm_id != #{alarmId}
		  AND alarm_status = 'RESOLVED'
		<if test="equipmentId != null and equipmentId != ''">
		  AND equipment_id = #{equipmentId}
		</if>
		<if test="alarmType != null and alarmType != ''">
		  AND alarm_type = #{alarmType}
		</if>
		ORDER BY `timestamp` DESC
		LIMIT #{limit}
		</script>
		""")
	List<RagSimilarCase> findSimilarResolvedCases(
		@Param("alarmId") long alarmId,
		@Param("equipmentId") String equipmentId,
		@Param("alarmType") String alarmType,
		@Param("limit") int limit
	);

}
