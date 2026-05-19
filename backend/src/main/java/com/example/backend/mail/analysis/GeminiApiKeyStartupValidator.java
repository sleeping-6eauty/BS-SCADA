package com.example.backend.mail.analysis;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class GeminiApiKeyStartupValidator implements ApplicationRunner {

	private static final Logger logger = LoggerFactory.getLogger(GeminiApiKeyStartupValidator.class);

	private final boolean validationEnabled;
	private final String baseUrl;
	private final String apiKey;

	public GeminiApiKeyStartupValidator(
		@Value("${gemini.api-key-validation.enabled:true}") boolean validationEnabled,
		@Value("${gemini.base-url:https://generativelanguage.googleapis.com/v1beta}") String baseUrl,
		@Value("${gemini.api-key:}") String apiKey
	) {
		this.validationEnabled = validationEnabled;
		this.baseUrl = trimTrailingSlash(baseUrl);
		this.apiKey = apiKey;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (!validationEnabled) {
			logger.info("Gemini API key validation skipped: disabled by config");
			return;
		}
		if (apiKey == null || apiKey.isBlank()) {
			logger.warn("Gemini API key validation failed: GEMINI_API_KEY is empty");
			return;
		}

		HttpClient httpClient = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(10))
			.build();

		try {
			HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(baseUrl + "/models?pageSize=1"))
				.timeout(Duration.ofSeconds(15))
				.header("x-goog-api-key", apiKey)
				.GET()
				.build();

			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			int status = response.statusCode();
			if (status >= 200 && status < 300) {
				logger.info("Gemini API key validation success: API key is valid");
				return;
			}
			if (status == 401 || status == 403) {
				logger.error("Gemini API key validation failed: invalid key or permission denied (status={})", status);
				return;
			}
			if (status == 429) {
				logger.warn("Gemini API key validation warning: key is reachable but quota/rate limit exceeded (status=429)");
				return;
			}
			logger.warn("Gemini API key validation returned unexpected status={}", status);
		} catch (IOException exception) {
			logger.warn("Gemini API key validation failed: I/O error", exception);
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			logger.warn("Gemini API key validation interrupted", exception);
		}
	}

	private String trimTrailingSlash(String value) {
		if (value == null || value.isBlank()) {
			return "https://generativelanguage.googleapis.com/v1beta";
		}
		String trimmed = value.trim();
		return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
	}
}
