package com.example.backend.alarm;

import java.util.List;

public record AlarmStatisticsResponse(
	String period,
	List<AlarmStatisticsItem> items
) {
}
