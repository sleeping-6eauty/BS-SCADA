package com.example.backend.alarm;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AlarmMapper {

	@Select("""
		<script>
		SELECT
			alarm_id AS alarmId,
			log_id AS logId,
			equipment_id AS equipmentId,
			`timestamp` AS timestamp,
			alarm_type AS alarmType,
			alarm_memo AS alarmMemo,
			alarm_status AS alarmStatus,
			NULL AS equipmentName,
			NULL AS location,
			NULL AS severity,
			NULL AS manager,
			NULL AS currentStatus,
			NULL AS recommendedAction
		FROM alarm_log
		<where>
			<if test="equipmentId != null and equipmentId != ''">
				AND equipment_id = #{equipmentId}
			</if>
			<if test="status != null and status != ''">
				AND alarm_status = #{status}
			</if>
			<if test="type != null and type != ''">
				AND alarm_type LIKE CONCAT('%', #{type}, '%')
			</if>
			<if test="from != null">
				AND `timestamp` &gt;= #{from}
			</if>
			<if test="to != null">
				AND `timestamp` &lt;= #{to}
			</if>
		</where>
		ORDER BY `timestamp` DESC
		LIMIT #{size} OFFSET #{offset}
		</script>
		""")
	@ConstructorArgs({
		@Arg(column = "alarmId", javaType = Long.class, name = "alarmId", id = true),
		@Arg(column = "logId", javaType = Long.class, name = "logId"),
		@Arg(column = "equipmentId", javaType = String.class, name = "equipmentId"),
		@Arg(column = "timestamp", javaType = LocalDateTime.class, name = "timestamp"),
		@Arg(column = "alarmType", javaType = String.class, name = "alarmType"),
		@Arg(column = "alarmMemo", javaType = String.class, name = "alarmMemo"),
		@Arg(column = "alarmStatus", javaType = String.class, name = "alarmStatus"),
		@Arg(column = "equipmentName", javaType = String.class, name = "equipmentName"),
		@Arg(column = "location", javaType = String.class, name = "location"),
		@Arg(column = "severity", javaType = String.class, name = "severity"),
		@Arg(column = "manager", javaType = String.class, name = "manager"),
		@Arg(column = "currentStatus", javaType = String.class, name = "currentStatus"),
		@Arg(column = "recommendedAction", javaType = String.class, name = "recommendedAction")
	})
	List<Alarm> findAlarms(
		@Param("equipmentId") String equipmentId,
		@Param("status") String status,
		@Param("type") String type,
		@Param("from") LocalDateTime from,
		@Param("to") LocalDateTime to,
		@Param("size") int size,
		@Param("offset") int offset
	);

	@Select("""
		<script>
		SELECT COUNT(*)
		FROM alarm_log
		<where>
			<if test="equipmentId != null and equipmentId != ''">
				AND equipment_id = #{equipmentId}
			</if>
			<if test="status != null and status != ''">
				AND alarm_status = #{status}
			</if>
			<if test="type != null and type != ''">
				AND alarm_type LIKE CONCAT('%', #{type}, '%')
			</if>
			<if test="from != null">
				AND `timestamp` &gt;= #{from}
			</if>
			<if test="to != null">
				AND `timestamp` &lt;= #{to}
			</if>
		</where>
		</script>
		""")
	long countAlarms(
		@Param("equipmentId") String equipmentId,
		@Param("status") String status,
		@Param("type") String type,
		@Param("from") LocalDateTime from,
		@Param("to") LocalDateTime to
	);

	@Select("""
		SELECT DATE_FORMAT(created_at, '%Y-%m-%d') AS label, COUNT(*) AS value
		FROM alarm_log
		WHERE created_at IS NOT NULL
		GROUP BY DATE_FORMAT(created_at, '%Y-%m-%d')
		ORDER BY MIN(created_at)
		""")
	List<AlarmStatisticsItem> countByDay();

	@Select("""
		SELECT DATE_FORMAT(DATE_SUB(created_at, INTERVAL WEEKDAY(created_at) DAY), '%Y-%m-%d') AS label, COUNT(*) AS value
		FROM alarm_log
		WHERE created_at IS NOT NULL
		GROUP BY DATE_FORMAT(DATE_SUB(created_at, INTERVAL WEEKDAY(created_at) DAY), '%Y-%m-%d')
		ORDER BY MIN(created_at)
		""")
	List<AlarmStatisticsItem> countByWeek();

	@Select("""
		SELECT DATE_FORMAT(created_at, '%Y-%m') AS label, COUNT(*) AS value
		FROM alarm_log
		WHERE created_at IS NOT NULL
		GROUP BY DATE_FORMAT(created_at, '%Y-%m')
		ORDER BY MIN(created_at)
		""")
	List<AlarmStatisticsItem> countByMonth();

	@Select("""
		<script>
		SELECT COUNT(*)
		FROM alarm_log
		WHERE equipment_id = #{equipmentId}
		  AND created_at >=
		  <choose>
		  	<when test="days == 30">
		  		DATE_SUB(NOW(6), INTERVAL 30 DAY)
		  	</when>
		  	<otherwise>
		  		DATE_SUB(NOW(6), INTERVAL 7 DAY)
		  	</otherwise>
		  </choose>
		</script>
		""")
	long countByEquipmentWithinDays(@Param("equipmentId") String equipmentId, @Param("days") int days);

	@Select("""
		SELECT
			al.created_at AS createdAt,
			al.equipment_id AS equipmentId,
			al.alarm_type AS alarmType,
			al.alarm_status AS alarmStatus,
			ue.user_id AS userId
		FROM alarm_log al
		LEFT JOIN user_equipment ue ON ue.equipment_id = al.equipment_id
		ORDER BY al.created_at DESC
		""")
	List<AlarmLogRow> findAlarmLogRows();

	@Select("""
		SELECT
			equipment_id AS equipmentId,
			equipment_name AS equipmentName,
			line_no AS lineNo,
			zone,
			manufacturer,
			cycle_time AS cycleTime,
			health_score AS healthScore,
			remaining_life AS remainingLife,
			replacement_date AS replacementDate
		FROM equipment
		WHERE equipment_id = #{equipmentId}
		""")
	EquipmentInfo findEquipment(@Param("equipmentId") String equipmentId);

	@Select("""
		SELECT
			alarm_id AS alarmId,
			log_id AS logId,
			equipment_id AS equipmentId,
			`timestamp` AS timestamp,
			alarm_type AS alarmType,
			alarm_memo AS alarmMemo,
			alarm_status AS alarmStatus,
			created_at AS createdAt,
			updated_at AS updatedAt
		FROM alarm_log
		WHERE equipment_id = #{equipmentId}
		ORDER BY created_at DESC
		""")
	List<AlarmLogEntry> findAlarmLogsByEquipment(@Param("equipmentId") String equipmentId);

	@Select("""
		SELECT
			alarm_id AS alarmId,
			log_id AS logId,
			equipment_id AS equipmentId,
			`timestamp` AS timestamp,
			alarm_type AS alarmType,
			alarm_memo AS alarmMemo,
			alarm_status AS alarmStatus,
			created_at AS createdAt,
			updated_at AS updatedAt
		FROM alarm_log
		WHERE alarm_id = #{alarmId}
		""")
	AlarmLogEntry findAlarmLogById(@Param("alarmId") long alarmId);

	@Update("""
		UPDATE alarm_log
		SET
			alarm_memo = COALESCE(#{alarmMemo}, alarm_memo),
			alarm_status = COALESCE(#{alarmStatus}, alarm_status),
			updated_at = CURRENT_TIMESTAMP(6)
		WHERE alarm_id = #{alarmId}
		""")
	int updateAlarmMemo(
		@Param("alarmId") long alarmId,
		@Param("alarmMemo") String alarmMemo,
		@Param("alarmStatus") String alarmStatus
	);
}
