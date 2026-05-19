package com.example.backend.mail.mapper;

import java.util.List;

import com.example.backend.mail.dto.AlarmContext;
import com.example.backend.mail.dto.DailyAlarmStatusCount;
import com.example.backend.mail.dto.DailyCountItem;
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
			report_type,
			<if test="recipientUserId != null">
				recipient_user_id,
			</if>
			<if test="recipientEmail != null">
				recipient_email,
			</if>
			<if test="recipientName != null">
				recipient_name,
			</if>
			<if test="sourceReportDate != null">
				source_report_date,
			</if>
			`timestamp`,
			mail_text
		</trim>
		VALUES
		<trim prefix="(" suffix=")" suffixOverrides=",">
			<if test="alarmId != null">
				#{alarmId},
			</if>
			COALESCE(#{reportType}, 'ALARM'),
			<if test="recipientUserId != null">
				#{recipientUserId},
			</if>
			<if test="recipientEmail != null">
				#{recipientEmail},
			</if>
			<if test="recipientName != null">
				#{recipientName},
			</if>
			<if test="sourceReportDate != null">
				#{sourceReportDate},
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
			report_type AS reportType,
			recipient_user_id AS recipientUserId,
			recipient_email AS recipientEmail,
			recipient_name AS recipientName,
			source_report_date AS sourceReportDate,
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
			report_type AS reportType,
			recipient_user_id AS recipientUserId,
			recipient_email AS recipientEmail,
			recipient_name AS recipientName,
			source_report_date AS sourceReportDate,
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
			al.alarm_id AS alarmId,
			al.equipment_id AS equipmentId,
			al.alarm_type AS alarmType,
			al.alarm_status AS alarmStatus,
			al.alarm_memo AS alarmMemo,
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
		  AND (u.status IS NULL OR u.status = 'active')
		  AND NOT EXISTS (
		  	SELECT 1
		  	FROM mail_report mr
		  	WHERE mr.alarm_id = al.alarm_id
		  	  AND mr.recipient_user_id = u.user_id
		  	  AND mr.report_type = 'ALARM'
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
		  AND (u.status IS NULL OR u.status = 'active')
		  AND NOT EXISTS (
		  	SELECT 1
		  	FROM mail_report mr
		  	WHERE mr.alarm_id = al.alarm_id
		  	  AND mr.recipient_user_id = u.user_id
		  	  AND mr.report_type = 'ALARM'
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
			mr.recipient_user_id AS recipientUserId,
			mr.recipient_email AS recipientEmail,
			mr.recipient_name AS recipientName,
			COUNT(*) AS reportCount
		FROM mail_report mr
		WHERE mr.report_type = 'ALARM'
		  AND mr.recipient_user_id IS NOT NULL
		  AND mr.`timestamp` >= #{from}
		  AND mr.`timestamp` < #{to}
		  AND NOT EXISTS (
		  	SELECT 1
		  	FROM mail_report daily
		  	WHERE daily.report_type = 'DAILY'
		  	  AND daily.recipient_user_id = mr.recipient_user_id
		  	  AND daily.source_report_date = #{reportDate}
		  )
		GROUP BY mr.recipient_user_id, mr.recipient_email, mr.recipient_name
		ORDER BY mr.recipient_user_id
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
		WHERE mr.report_type = 'ALARM'
		  AND mr.recipient_user_id = #{recipientUserId}
		  AND mr.`timestamp` >= #{from}
		  AND mr.`timestamp` < #{to}
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
			alarm_memo AS alarmMemo,
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

	@Select("""
		SELECT
			COUNT(*) AS totalCount,
			SUM(CASE WHEN alarm_status = 'OPEN' THEN 1 ELSE 0 END) AS openCount,
			SUM(CASE WHEN alarm_status = 'IN_PROGRESS' THEN 1 ELSE 0 END) AS inProgressCount,
			SUM(CASE WHEN alarm_status = 'RESOLVED' THEN 1 ELSE 0 END) AS resolvedCount
		FROM alarm_log
		WHERE `timestamp` >= #{from}
		  AND `timestamp` < #{to}
		""")
	DailyAlarmStatusCount countDailyStatus(
		@Param("from") java.time.LocalDateTime from,
		@Param("to") java.time.LocalDateTime to
	);

	@Select("""
		SELECT
			COALESCE(equipment_id, 'UNKNOWN') AS label,
			COUNT(*) AS value
		FROM alarm_log
		WHERE `timestamp` >= #{from}
		  AND `timestamp` < #{to}
		GROUP BY equipment_id
		ORDER BY value DESC
		LIMIT #{limit}
		""")
	List<DailyCountItem> findTopEquipmentsByPeriod(
		@Param("from") java.time.LocalDateTime from,
		@Param("to") java.time.LocalDateTime to,
		@Param("limit") int limit
	);

	@Select("""
		SELECT
			COALESCE(alarm_type, 'UNKNOWN') AS label,
			COUNT(*) AS value
		FROM alarm_log
		WHERE `timestamp` >= #{from}
		  AND `timestamp` < #{to}
		GROUP BY alarm_type
		ORDER BY value DESC
		LIMIT #{limit}
		""")
	List<DailyCountItem> findTopAlarmTypesByPeriod(
		@Param("from") java.time.LocalDateTime from,
		@Param("to") java.time.LocalDateTime to,
		@Param("limit") int limit
	);
}
