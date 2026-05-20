package com.example.backend.mail.rag;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GeminiEmbeddingService implements EmbeddingService {

	private static final Logger logger = LoggerFactory.getLogger(GeminiEmbeddingService.class);

	private final ObjectMapper objectMapper;
	private final HttpClient httpClient;
	private final String baseUrl;
	private final String apiKey;
	private final String model;
	private final int outputDimensionality;
	private final boolean normalize;

	public GeminiEmbeddingService(
		ObjectMapper objectMapper,
		@Value("${gemini.base-url:https://generativelanguage.googleapis.com/v1beta}") String baseUrl,
		@Value("${gemini.api-key:}") String apiKey,
		@Value("${gemini.embedding-model:gemini-embedding-001}") String model,
		@Value("${gemini.embedding-output-dimensionality:1536}") int outputDimensionality,
		@Value("${gemini.embedding-normalize:true}") boolean normalize
	) {
		this.objectMapper = objectMapper;
		this.httpClient = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(10))
			.build();
		this.baseUrl = trimTrailingSlash(baseUrl);
		this.apiKey = apiKey;
		this.model = stripModelPrefix(model);
		this.outputDimensionality = outputDimensionality;
		this.normalize = normalize;
	}

	@Override
	public String providerName() {
		return "gemini";
	}

	@Override
	public boolean isConfigured() {
		return apiKey != null && !apiKey.isBlank();
	}

	@Override
	public Optional<List<Double>> embed(String input, EmbeddingPurpose purpose) {
		if (!isConfigured()) {
			return Optional.empty();
		}
		if (input == null || input.isBlank()) {
			return Optional.empty();
		}

		Map<String, Object> part = new LinkedHashMap<>();
		part.put("text", input);

		Map<String, Object> content = new LinkedHashMap<>();
		content.put("parts", List.of(part));

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("model", "models/" + model);
		body.put("content", content);
		body.put("task_type", taskType(purpose));
		if (outputDimensionality > 0) {
			body.put("output_dimensionality", outputDimensionality);
		}

		try {
			HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(baseUrl + "/models/" + urlEncode(model) + ":embedContent"))
				.timeout(Duration.ofSeconds(30))
				.header("x-goog-api-key", apiKey)
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body), StandardCharsets.UTF_8))
				.build();

			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
			if (response.statusCode() < 200 || response.statusCode() >= 300) {
				logger.warn("Gemini embedding request failed: status={}, error={}", response.statusCode(), errorMessage(response.body()));
				return Optional.empty();
			}

			JsonNode root = objectMapper.readTree(response.body());
			JsonNode values = root.path("embedding").path("values");
			if (!values.isArray()) {
				values = root.path("embeddings").path(0).path("values");
			}
			if (!values.isArray()) {
				logger.warn("Gemini embedding response did not contain an embedding vector");
				return Optional.empty();
			}

			List<Double> vector = new ArrayList<>();
			for (JsonNode value : values) {
				vector.add(value.asDouble());
			}
			if (vector.isEmpty()) {
				return Optional.empty();
			}
			return Optional.of(normalize ? normalize(vector) : vector);
		} catch (IOException exception) {
			logger.warn("failed to call Gemini embedding API", exception);
			return Optional.empty();
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			logger.warn("Gemini embedding request interrupted", exception);
			return Optional.empty();
		} catch (RuntimeException exception) {
			logger.warn("failed to parse Gemini embedding response", exception);
			return Optional.empty();
		}
	}

	private List<Double> normalize(List<Double> vector) {
		double sum = 0;
		for (Double value : vector) {
			sum += value * value;
		}
		double norm = Math.sqrt(sum);
		if (norm == 0) {
			return vector;
		}
		List<Double> normalized = new ArrayList<>(vector.size());
		for (Double value : vector) {
			normalized.add(value / norm);
		}
		return normalized;
	}

	private String taskType(EmbeddingPurpose purpose) {
		if (purpose == EmbeddingPurpose.QUERY) {
			return "RETRIEVAL_QUERY";
		}
		return "RETRIEVAL_DOCUMENT";
	}

	private String stripModelPrefix(String value) {
		if (value == null || value.isBlank()) {
			return "gemini-embedding-001";
		}
		String trimmed = value.trim();
		return trimmed.startsWith("models/") ? trimmed.substring("models/".length()) : trimmed;
	}

	private String trimTrailingSlash(String value) {
		if (value == null || value.isBlank()) {
			return "https://generativelanguage.googleapis.com/v1beta";
		}
		String trimmed = value.trim();
		return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
	}

	private String urlEncode(String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8);
	}

	private String errorMessage(String responseBody) {
		if (responseBody == null || responseBody.isBlank()) {
			return "empty response body";
		}
		try {
			JsonNode error = objectMapper.readTree(responseBody).path("error");
			String code = text(error.path("code"));
			String status = text(error.path("status"));
			String message = text(error.path("message"));
			String summary = "code=" + code + ", status=" + status + ", message=" + message;
			return summary.length() > 500 ? summary.substring(0, 500) + "..." : summary;
		} catch (RuntimeException | IOException exception) {
			String trimmed = responseBody.replaceAll("\\s+", " ").trim();
			return trimmed.length() > 500 ? trimmed.substring(0, 500) + "..." : trimmed;
		}
	}

	private String text(JsonNode node) {
		if (node == null || node.isMissingNode() || node.isNull()) {
			return "";
		}
		return node.asText("");
	}
}
