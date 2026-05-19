package com.example.backend.mail.rag;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.example.backend.mail.dto.AlarmContext;
import com.example.backend.mail.dto.RagSimilarCase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class QdrantVectorStore {

	private static final Logger logger = LoggerFactory.getLogger(QdrantVectorStore.class);

	private final ObjectMapper objectMapper;
	private final HttpClient httpClient;
	private final String baseUrl;
	private final String apiKey;
	private final String collectionName;
	private final int vectorSize;

	public QdrantVectorStore(
		ObjectMapper objectMapper,
		@Value("${qdrant.url:http://localhost:6333}") String baseUrl,
		@Value("${qdrant.api-key:}") String apiKey,
		@Value("${qdrant.collection:alarm_cases}") String collectionName,
		@Value("${rag.vector.dimension:1536}") int vectorSize
	) {
		this.objectMapper = objectMapper;
		this.httpClient = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(10))
			.build();
		this.baseUrl = trimTrailingSlash(baseUrl);
		this.apiKey = apiKey;
		this.collectionName = collectionName;
		this.vectorSize = vectorSize;
	}

	public boolean isConfigured() {
		return baseUrl != null && !baseUrl.isBlank() && collectionName != null && !collectionName.isBlank();
	}

	public boolean ensureCollection() {
		if (!isConfigured()) {
			return false;
		}
		if (collectionExists()) {
			return true;
		}

		Map<String, Object> vectors = new LinkedHashMap<>();
		vectors.put("size", vectorSize);
		vectors.put("distance", "Cosine");

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("vectors", vectors);

		HttpResponse<String> response = send("PUT", collectionPath(), body);
		boolean created = response != null && response.statusCode() >= 200 && response.statusCode() < 300;
		if (!created) {
			logger.warn("failed to create Qdrant collection: collection={}, status={}", collectionName, responseStatus(response));
		}
		return created;
	}

	public boolean upsert(AlarmContext alarm, List<Double> vector) {
		if (alarm == null || alarm.getAlarmId() == null || vector == null || vector.isEmpty()) {
			return false;
		}

		Map<String, Object> point = new LinkedHashMap<>();
		point.put("id", alarm.getAlarmId());
		point.put("vector", vector);
		point.put("payload", payload(alarm));

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("points", List.of(point));

		HttpResponse<String> response = send("PUT", collectionPath() + "/points?wait=true", body);
		boolean upserted = response != null && response.statusCode() >= 200 && response.statusCode() < 300;
		if (!upserted) {
			logger.warn("failed to upsert alarm vector: alarmId={}, status={}", alarm.getAlarmId(), responseStatus(response));
		}
		return upserted;
	}

	public List<RagSimilarCase> search(List<Double> vector, int limit) {
		if (vector == null || vector.isEmpty() || limit <= 0) {
			return List.of();
		}

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("vector", vector);
		body.put("limit", limit);
		body.put("with_payload", true);

		HttpResponse<String> response = send("POST", collectionPath() + "/points/search", body);
		if (response == null || response.statusCode() < 200 || response.statusCode() >= 300) {
			logger.warn("Qdrant search failed: status={}", responseStatus(response));
			return List.of();
		}

		try {
			JsonNode result = objectMapper.readTree(response.body()).path("result");
			if (!result.isArray()) {
				return List.of();
			}

			List<RagSimilarCase> cases = new ArrayList<>();
			for (JsonNode point : result) {
				JsonNode payload = point.path("payload");
				RagSimilarCase similarCase = new RagSimilarCase();
				similarCase.setAlarmId(asLong(payload.path("alarmId")));
				similarCase.setEquipmentId(asText(payload.path("equipmentId")));
				similarCase.setAlarmType(asText(payload.path("alarmType")));
				similarCase.setAlarmStatus(asText(payload.path("alarmStatus")));
				similarCase.setAlarmMemo(firstNonBlank(asText(payload.path("alarmText")), asText(payload.path("alarmMemo"))));
				similarCase.setTimestamp(parseDateTime(asText(payload.path("timestamp"))));
				similarCase.setSimilarityScore(point.path("score").isNumber() ? point.path("score").asDouble() : null);
				cases.add(similarCase);
			}
			return cases;
		} catch (RuntimeException | IOException exception) {
			logger.warn("failed to parse Qdrant search response", exception);
			return List.of();
		}
	}

	private boolean collectionExists() {
		HttpResponse<String> response = send("GET", collectionPath(), null);
		return response != null && response.statusCode() >= 200 && response.statusCode() < 300;
	}

	private Map<String, Object> payload(AlarmContext alarm) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("alarmId", alarm.getAlarmId());
		payload.put("equipmentId", alarm.getEquipmentId());
		payload.put("alarmType", alarm.getAlarmType());
		payload.put("alarmStatus", alarm.getAlarmStatus());
		payload.put("alarmMemo", alarm.getAlarmMemo());
		payload.put("alarmText", alarm.getAlarmText());
		payload.put("timestamp", alarm.getTimestamp() == null ? null : alarm.getTimestamp().toString());
		return payload;
	}

	private HttpResponse<String> send(String method, String path, Object body) {
		try {
			HttpRequest.Builder builder = HttpRequest.newBuilder()
				.uri(URI.create(baseUrl + path))
				.timeout(Duration.ofSeconds(30))
				.header("Content-Type", "application/json");
			if (apiKey != null && !apiKey.isBlank()) {
				builder.header("api-key", apiKey);
			}

			if ("GET".equals(method)) {
				builder.GET();
			} else {
				String json = body == null ? "" : objectMapper.writeValueAsString(body);
				builder.method(method, HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8));
			}
			return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
		} catch (IOException exception) {
			logger.warn("failed to call Qdrant API: method={}, path={}", method, path, exception);
			return null;
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			logger.warn("Qdrant API request interrupted: method={}, path={}", method, path, exception);
			return null;
		} catch (RuntimeException exception) {
			logger.warn("failed to build Qdrant API request: method={}, path={}", method, path, exception);
			return null;
		}
	}

	private String collectionPath() {
		return "/collections/" + URLEncoder.encode(collectionName, StandardCharsets.UTF_8);
	}

	private Long asLong(JsonNode node) {
		return node.isNumber() ? node.asLong() : null;
	}

	private String asText(JsonNode node) {
		if (node == null || node.isMissingNode() || node.isNull()) {
			return null;
		}
		String value = node.asText();
		return value == null || value.isBlank() ? null : value;
	}

	private LocalDateTime parseDateTime(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		try {
			return LocalDateTime.parse(value);
		} catch (RuntimeException exception) {
			return null;
		}
	}

	private String firstNonBlank(String first, String second) {
		if (first != null && !first.isBlank()) {
			return first;
		}
		return second;
	}

	private int responseStatus(HttpResponse<String> response) {
		return response == null ? -1 : response.statusCode();
	}

	private String trimTrailingSlash(String value) {
		if (value == null || value.isBlank()) {
			return "http://localhost:6333";
		}
		String trimmed = value.trim();
		return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
	}
}
