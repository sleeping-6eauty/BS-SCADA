package com.example.backend.common;

import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class DatabaseHealthController {

	private final ObjectProvider<JdbcTemplate> jdbcTemplateProvider;

	public DatabaseHealthController(ObjectProvider<JdbcTemplate> jdbcTemplateProvider) {
		this.jdbcTemplateProvider = jdbcTemplateProvider;
	}

	@GetMapping
	public ApiResponse<Map<String, Object>> checkApplication() {
		return new ApiResponse<>(true, "application running", Map.of("status", "UP"));
	}

	@GetMapping("/db")
	public ResponseEntity<ApiResponse<Map<String, Object>>> checkDatabase() {
		JdbcTemplate jdbcTemplate = jdbcTemplateProvider.getIfAvailable();
		if (jdbcTemplate == null) {
			return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
				.body(new ApiResponse<>(false, "database is not configured", Map.of("connected", false)));
		}

		try {
			Integer connectionCheck = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
			String databaseName = jdbcTemplate.queryForObject("SELECT DATABASE()", String.class);
			String mysqlVersion = jdbcTemplate.queryForObject("SELECT VERSION()", String.class);

			return ResponseEntity.ok(new ApiResponse<>(
				true,
				"database connected",
				Map.<String, Object>of(
					"connected", connectionCheck != null && connectionCheck == 1,
					"database", databaseName == null ? "" : databaseName,
					"version", mysqlVersion == null ? "" : mysqlVersion
				)
			));
		} catch (Exception exception) {
			return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
				.body(new ApiResponse<>(false, "database connection failed: " + exception.getMessage(), Map.of("connected", false)));
		}
	}
}
