package com.example.backend.alarm.dto;

import java.time.LocalDateTime;

public record Alarm(
	Long alarmId,
	Long logId,
	String equipmentId,
	LocalDateTime timestamp,
	String alarmType,
	String alarmMemo,
	String alarmStatus,
	String equipmentName,
	String location,
	String severity,
	String manager,
	String currentStatus,
	String recommendedAction
) {
}
