package com.example.backend.alarm.dto;

public record AlarmCountResponse(
	String equipmentId,
	int days,
	long count
) {
}
