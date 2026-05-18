package com.example.backend.alarm;

import java.util.List;

public record AlarmEquipmentDetailResponse(
	EquipmentInfo equipment,
	List<AlarmLogEntry> alarms
) {
}
