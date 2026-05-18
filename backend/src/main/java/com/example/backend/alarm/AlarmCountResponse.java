package com.example.backend.alarm;

public record AlarmCountResponse(
	String equipmentId,
	int days,
	long count
) {
}
