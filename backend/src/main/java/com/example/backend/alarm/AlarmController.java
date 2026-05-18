package com.example.backend.alarm;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.common.ApiResponse;
import com.example.backend.common.PageResponse;

@RestController
@RequestMapping("/api/alarms")
public class AlarmController {

	private final AlarmService alarmService;

	public AlarmController(AlarmService alarmService) {
		this.alarmService = alarmService;
	}

	@GetMapping
	public PageResponse<Alarm> getAlarms(
		@RequestParam(required = false) String equipmentId,
		@RequestParam(required = false) String status,
		@RequestParam(required = false) String type,
		@RequestParam(required = false) String from,
		@RequestParam(required = false) String to,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "10") int size
	) {
		return alarmService.findAlarms(
			new AlarmQuery(equipmentId, status, type, DateTimeParser.parse(from), DateTimeParser.parse(to), page, size)
		);
	}

	@GetMapping("/statistics")
	public ApiResponse<AlarmStatisticsResponse> getStatistics(@RequestParam(defaultValue = "DAY") String period) {
		return new ApiResponse<>(true, "alarm statistics", alarmService.statistics(period));
	}

	@GetMapping("/log")
	public ApiResponse<List<AlarmLogRow>> getAlarmLogRows() {
		return new ApiResponse<>(true, "alarm log", alarmService.findAlarmLogRows());
	}

	@GetMapping("/{equipmentId}/count")
	public ApiResponse<AlarmCountResponse> getAlarmCount(
		@PathVariable String equipmentId,
		@RequestParam(defaultValue = "7") int days
	) {
		return new ApiResponse<>(true, "alarm count", alarmService.countByEquipment(equipmentId, days));
	}

	@GetMapping("/detail/{equipmentId}")
	public ApiResponse<AlarmEquipmentDetailResponse> getEquipmentDetail(@PathVariable String equipmentId) {
		return new ApiResponse<>(true, "alarm detail", alarmService.getEquipmentDetail(equipmentId));
	}

	@PatchMapping("/memo/{alarmId}")
	public ApiResponse<AlarmMemoResponse> updateAlarmMemo(
		@PathVariable long alarmId,
		@RequestBody AlarmMemoRequest request
	) {
		return new ApiResponse<>(true, "alarm memo updated", alarmService.updateAlarmMemo(alarmId, request));
	}
}
