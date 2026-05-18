package com.example.backend.alarm.dto;

public record AlarmMemoResponse(
	Long alarmId,
	String alarmMemo,
	String alarmStatus
) {
}
