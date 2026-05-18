package com.example.backend.alarm;

public record AlarmMemoResponse(
	Long alarmId,
	String alarmMemo,
	String alarmStatus
) {
}
