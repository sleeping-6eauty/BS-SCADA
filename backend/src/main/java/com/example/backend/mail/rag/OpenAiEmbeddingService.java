package com.example.backend.mail.rag;

import java.io.IOException;
import java.net.URI;
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
public class OpenAiEmbeddingService implements EmbeddingService {

	private static final Logger logger = LoggerFactory.getLogger(OpenAiEmbeddingService.class);

	private final ObjectMapper objectMapper;
	private final HttpClient httpClient;
	private final String baseUrl;
	private final String apiKey;
	private final String model;
	private final int dimensions;

	public OpenAiEmbeddingService(
		ObjectMapper objectMapper,
		@Value("${openai.base-url:https://api.openai.com/v1}") String baseUrl,
		@Value("${openai.api-key:}") String apiKey,
		@Value("${openai.embedding-model:text-embedding-3-small}") String model,
		@Value("${openai.embedding-dimensions:0}") int dimensions
	) {
		this.objectMapper = objectMapper;
		this.httpClient = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(10))
			.build();
		this.baseUrl = trimTrailingSlash(baseUrl);
		this.apiKey = apiKey;
		this.model = model;
		this.dimensions = dimensions;
	}

	@Override
	public String providerName() {
		return "openai";
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

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("model", model);
		body.put("input", input);
		body.put("encoding_format", "float");
		if (dimensions > 0) {
			body.put("dimensions", dimensions);
		}

		try {
			HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(baseUrl + "/embeddings"))
				.timeout(Duration.ofSeconds(30))
				.header("Authorization", "Bearer " + apiKey)
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body), StandardCharsets.UTF_8))
				.build();

			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
			if (response.statusCode() < 200 || response.statusCode() >= 300) {
				logger.warn("OpenAI embedding request failed: status={}, error={}", response.statusCode(), errorMessage(response.body()));
				return Optional.empty();
			}

			JsonNode embeddingNode = objectMapper.readTree(response.body())
				.path("data")
				.path(0)
				.path("embedding");
			if (!embeddingNode.isArray()) {
				logger.warn("OpenAI embedding response did not contain an embedding vector");
				return Optional.empty();
			}

			List<Double> vector = new ArrayList<>();
			for (JsonNode value : embeddingNode) {
				vector.add(value.asDouble());
			}
			return vector.isEmpty() ? Optional.empty() : Optional.of(vector);
		} catch (IOException exception) {
			logger.warn("failed to call OpenAI embedding API", exception);
			return Optional.empty();
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			logger.warn("OpenAI embedding request interrupted", exception);
			return Optional.empty();
		} catch (RuntimeException exception) {
			logger.warn("failed to parse OpenAI embedding response", exception);
			return Optional.empty();
		}
	}

	private String trimTrailingSlash(String value) {
		if (value == null || value.isBlank()) {
			return "https://api.openai.com/v1";
		}
		String trimmed = value.trim();
		return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
	}

	private String errorMessage(String responseBody) {
		if (responseBody == null || responseBody.isBlank()) {
			return "empty response body";
		}
		try {
			JsonNode error = objectMapper.readTree(responseBody).path("error");
			String code = text(error.path("code"));
			String type = text(error.path("type"));
			String message = text(error.path("message"));
			String summary = "code=" + code + ", type=" + type + ", message=" + message;
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
