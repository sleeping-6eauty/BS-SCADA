package com.example.backend.alarm.service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public final class DateTimeParser {

	private DateTimeParser() {
	}

	public static LocalDateTime parse(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}

		String normalized = value.trim().replace(' ', 'T');
		try {
			return LocalDateTime.parse(normalized, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
		} catch (DateTimeParseException ignored) {
			try {
				return OffsetDateTime.parse(normalized, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toLocalDateTime();
			} catch (DateTimeParseException exception) {
				throw new IllegalArgumentException("date-time format must be ISO-8601: " + value);
			}
		}
	}
}
