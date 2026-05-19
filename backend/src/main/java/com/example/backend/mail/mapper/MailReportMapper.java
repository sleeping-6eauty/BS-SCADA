package com.example.backend.mail.mapper;

import java.util.List;

import com.example.backend.mail.dto.AlarmContext;
import com.example.backend.mail.dto.DailyAlarmStatusCount;
import com.example.backend.mail.dto.DailyCountItem;
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
			`timestamp` AS timestamp
		FROM alarm_log
		WHERE alarm_id = #{alarmId}
		""")
	AlarmContext findAlarmContextById(@Param("alarmId") long alarmId);

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
