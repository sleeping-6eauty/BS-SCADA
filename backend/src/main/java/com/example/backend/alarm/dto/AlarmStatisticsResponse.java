package com.example.backend.alarm.dto;

import java.util.List;

public record AlarmStatisticsResponse(
	String period,
	List<AlarmStatisticsItem> items
) {
}
