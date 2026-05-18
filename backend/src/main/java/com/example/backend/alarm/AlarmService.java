package com.example.backend.alarm;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import com.example.backend.common.PageMeta;
import com.example.backend.common.PageResponse;

@Service
public class AlarmService {

	private final AlarmMapper alarmMapper;
	private final Map<Long, Alarm> fallbackAlarms = new ConcurrentHashMap<>();

	public AlarmService(ObjectProvider<AlarmMapper> alarmMapperProvider) {
		this.alarmMapper = alarmMapperProvider.getIfAvailable();
		seedFallbackAlarms();
	}

	public PageResponse<Alarm> findAlarms(AlarmQuery query) {
		int page = normalizedPage(query.page());
		int size = normalizedSize(query.size());

		if (alarmMapper != null) {
			long total = alarmMapper.countAlarms(query.equipmentId(), query.status(), query.type(), query.from(), query.to());
			List<Alarm> alarms = alarmMapper.findAlarms(
				query.equipmentId(),
				query.status(),
				query.type(),
				query.from(),
				query.to(),
				size,
				page * size
			);

			return new PageResponse<>(true, alarms, pageMeta(page, size, total));
		}

		List<Alarm> filtered = fallbackAlarms.values().stream()
			.filter(alarm -> matchesEquipment(alarm, query.equipmentId()))
			.filter(alarm -> matchesStatus(alarm, query.status()))
			.filter(alarm -> matchesType(alarm, query.type()))
			.filter(alarm -> query.from() == null || alarm.timestamp() == null || !alarm.timestamp().isBefore(query.from()))
			.filter(alarm -> query.to() == null || alarm.timestamp() == null || !alarm.timestamp().isAfter(query.to()))
			.sorted(Comparator.comparing(Alarm::timestamp, Comparator.nullsLast(Comparator.reverseOrder())))
			.toList();

		return pageFallback(filtered, page, size);
	}

	public AlarmStatisticsResponse statistics(String period) {
		String normalized = normalizePeriod(period);

		if (alarmMapper != null) {
			List<AlarmStatisticsItem> items = switch (normalized) {
				case "DAY" -> alarmMapper.countByDay();
				case "WEEK" -> alarmMapper.countByWeek();
				case "MONTH" -> alarmMapper.countByMonth();
				default -> throw new IllegalArgumentException("period must be one of DAY, WEEK, MONTH");
			};
			return new AlarmStatisticsResponse(normalized, items);
		}

		Function<Alarm, String> classifier = switch (normalized) {
			case "DAY" -> alarm -> alarm.timestamp() == null ? "UNKNOWN" : alarm.timestamp().toLocalDate().toString();
			case "WEEK" -> alarm -> alarm.timestamp() == null ? "UNKNOWN" : weekStart(alarm.timestamp().toLocalDate()).toString();
			case "MONTH" -> alarm -> alarm.timestamp() == null
				? "UNKNOWN"
				: "%04d-%02d".formatted(alarm.timestamp().getYear(), alarm.timestamp().getMonthValue());
			default -> throw new IllegalArgumentException("period must be one of DAY, WEEK, MONTH");
		};

		Map<String, Long> counts = fallbackAlarms.values().stream()
			.collect(Collectors.groupingBy(classifier, LinkedHashMap::new, Collectors.counting()));

		List<AlarmStatisticsItem> items = counts.entrySet().stream()
			.map(entry -> new AlarmStatisticsItem(entry.getKey(), entry.getValue()))
			.toList();

		return new AlarmStatisticsResponse(normalized, items);
	}

	public AlarmCountResponse countByEquipment(String equipmentId, int days) {
		int normalizedDays = normalizeDays(days);
		long count = alarmMapper == null
			? fallbackAlarms.values().stream()
				.filter(alarm -> matchesEquipment(alarm, equipmentId))
				.filter(alarm -> alarm.timestamp() != null)
				.filter(alarm -> !alarm.timestamp().isBefore(java.time.LocalDateTime.now().minusDays(normalizedDays)))
				.count()
			: alarmMapper.countByEquipmentWithinDays(equipmentId, normalizedDays);

		return new AlarmCountResponse(equipmentId, normalizedDays, count);
	}

	public List<AlarmLogRow> findAlarmLogRows() {
		if (alarmMapper != null) {
			return alarmMapper.findAlarmLogRows();
		}

		return fallbackAlarms.values().stream()
			.sorted(Comparator.comparing(Alarm::timestamp, Comparator.nullsLast(Comparator.reverseOrder())))
			.map(this::toFallbackLogRow)
			.toList();
	}

	public AlarmEquipmentDetailResponse getEquipmentDetail(String equipmentId) {
		if (alarmMapper != null) {
			EquipmentInfo equipment = alarmMapper.findEquipment(equipmentId);
			if (equipment == null) {
				throw new NoSuchElementException("equipment not found: " + equipmentId);
			}
			return new AlarmEquipmentDetailResponse(equipment, alarmMapper.findAlarmLogsByEquipment(equipmentId));
		}

		EquipmentInfo equipment = new EquipmentInfo();
		equipment.setEquipmentId(equipmentId);
		List<AlarmLogEntry> alarms = fallbackAlarms.values().stream()
			.filter(alarm -> matchesEquipment(alarm, equipmentId))
			.map(this::toFallbackLogEntry)
			.toList();
		return new AlarmEquipmentDetailResponse(equipment, alarms);
	}

	public AlarmMemoResponse updateAlarmMemo(long alarmId, AlarmMemoRequest request) {
		if (request == null) {
			throw new IllegalArgumentException("request body is required");
		}

		if (alarmMapper != null) {
			int updatedRows = alarmMapper.updateAlarmMemo(alarmId, request.alarmMemo(), request.alarmStatus());
			if (updatedRows == 0) {
				throw new NoSuchElementException("alarm not found: " + alarmId);
			}
			AlarmLogEntry updated = alarmMapper.findAlarmLogById(alarmId);
			return new AlarmMemoResponse(alarmId, updated.getAlarmMemo(), updated.getAlarmStatus());
		}

		Alarm alarm = getFallbackAlarm(alarmId);
		Alarm updated = new Alarm(
			alarm.alarmId(),
			alarm.logId(),
			alarm.equipmentId(),
			alarm.timestamp(),
			alarm.alarmType(),
			request.alarmMemo() == null ? alarm.alarmMemo() : request.alarmMemo(),
			request.alarmStatus() == null ? alarm.alarmStatus() : request.alarmStatus(),
			alarm.equipmentName(),
			alarm.location(),
			alarm.severity(),
			alarm.manager(),
			alarm.currentStatus(),
			alarm.recommendedAction()
		);
		fallbackAlarms.put(alarmId, updated);
		return new AlarmMemoResponse(alarmId, updated.alarmMemo(), updated.alarmStatus());
	}

	private String normalizePeriod(String period) {
		return period == null || period.isBlank()
			? "DAY"
			: period.trim().toUpperCase(Locale.ROOT);
	}

	private int normalizeDays(int days) {
		return days == 30 ? 30 : 7;
	}

	private LocalDate weekStart(LocalDate date) {
		return date.minusDays(date.getDayOfWeek().getValue() - 1L);
	}

	private boolean matchesEquipment(Alarm alarm, String equipmentId) {
		return equipmentId == null
			|| equipmentId.isBlank()
			|| (alarm.equipmentId() != null && alarm.equipmentId().equalsIgnoreCase(equipmentId))
			|| (alarm.equipmentName() != null && alarm.equipmentName().equalsIgnoreCase(equipmentId));
	}

	private boolean matchesStatus(Alarm alarm, String status) {
		return status == null
			|| status.isBlank()
			|| (alarm.alarmStatus() != null && alarm.alarmStatus().equalsIgnoreCase(status));
	}

	private boolean matchesType(Alarm alarm, String type) {
		return type == null
			|| type.isBlank()
			|| (alarm.alarmType() != null
				&& alarm.alarmType().toLowerCase(Locale.ROOT).contains(type.toLowerCase(Locale.ROOT)));
	}

	private PageResponse<Alarm> pageFallback(List<Alarm> source, int page, int size) {
		int total = source.size();
		int fromIndex = Math.min(page * size, total);
		int toIndex = Math.min(fromIndex + size, total);

		return new PageResponse<>(true, new ArrayList<>(source.subList(fromIndex, toIndex)), pageMeta(page, size, total));
	}

	private PageMeta pageMeta(int page, int size, long total) {
		int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / size);
		return new PageMeta(page, size, total, totalPages);
	}

	private int normalizedPage(int requestedPage) {
		return Math.max(requestedPage, 0);
	}

	private int normalizedSize(int requestedSize) {
		return requestedSize <= 0 ? 10 : Math.min(requestedSize, 100);
	}

	private void seedFallbackAlarms() {
		List<Alarm> seedData = AlarmSeedData.create();
		seedData.forEach(alarm -> fallbackAlarms.put(alarm.alarmId(), alarm));
	}

	private Alarm getFallbackAlarm(long alarmId) {
		Alarm alarm = fallbackAlarms.get(alarmId);
		if (alarm == null) {
			throw new NoSuchElementException("alarm not found: " + alarmId);
		}
		return alarm;
	}

	private AlarmLogRow toFallbackLogRow(Alarm alarm) {
		AlarmLogRow row = new AlarmLogRow();
		row.setCreatedAt(alarm.timestamp());
		row.setEquipmentId(alarm.equipmentId());
		row.setAlarmType(alarm.alarmType());
		row.setAlarmStatus(alarm.alarmStatus());
		return row;
	}

	private AlarmLogEntry toFallbackLogEntry(Alarm alarm) {
		AlarmLogEntry entry = new AlarmLogEntry();
		entry.setAlarmId(alarm.alarmId());
		entry.setLogId(alarm.logId());
		entry.setEquipmentId(alarm.equipmentId());
		entry.setTimestamp(alarm.timestamp());
		entry.setAlarmType(alarm.alarmType());
		entry.setAlarmMemo(alarm.alarmMemo());
		entry.setAlarmStatus(alarm.alarmStatus());
		entry.setCreatedAt(alarm.timestamp());
		entry.setUpdatedAt(alarm.timestamp());
		return entry;
	}
}
