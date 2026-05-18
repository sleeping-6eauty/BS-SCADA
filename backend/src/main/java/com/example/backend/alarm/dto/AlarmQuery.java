package com.example.backend.alarm.dto;

import java.time.LocalDateTime;

public record AlarmQuery(
	String equipmentId,
	String status,
	String type,
	LocalDateTime from,
	LocalDateTime to,
	int page,
	int size
) {
}
