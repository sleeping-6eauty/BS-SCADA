package com.example.backend.alarm.dto;

import java.util.List;

public record AlarmEquipmentDetailResponse(
	EquipmentInfo equipment,
	List<AlarmLogEntry> alarms
) {
}
