package com.example.backend.mail.analysis;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
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
import org.springframework.stereotype.Service;

@Service
public class GeminiAlarmAnalysisService implements AlarmAnalysisService {

	private static final Logger logger = LoggerFactory.getLogger(GeminiAlarmAnalysisService.class);

	private final ObjectMapper objectMapper;
	private final HttpClient httpClient;
	private final AlarmAnalysisContextProvider contextProvider;
	private final boolean enabled;
	private final String baseUrl;
	private final String apiKey;
	private final String model;
	private final double temperature;

	public GeminiAlarmAnalysisService(
		ObjectMapper objectMapper,
		AlarmAnalysisContextProvider contextProvider,
		@Value("${mail.analysis.llm.enabled:true}") boolean enabled,
		@Value("${gemini.base-url:https://generativelanguage.googleapis.com/v1beta}") String baseUrl,
		@Value("${gemini.api-key:}") String apiKey,
		@Value("${gemini.analysis-model:gemini-2.0-flash}") String model,
		@Value("${gemini.analysis-temperature:0.2}") double temperature
	) {
		this.objectMapper = objectMapper;
		this.contextProvider = contextProvider;
		this.enabled = enabled;
		this.baseUrl = trimTrailingSlash(baseUrl);
		this.apiKey = apiKey;
		this.model = stripModelPrefix(model);
		this.temperature = temperature;
		this.httpClient = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(10))
			.build();
	}

	@Override
	public AlarmAnalysisResult analyze(AlarmContext alarm, List<RagSimilarCase> similarCases) {
		AlarmAnalysisResult fallback = fallback(alarm);
		if (!enabled) {
			return fallback;
		}
		if (apiKey == null || apiKey.isBlank()) {
			logger.warn("LLM alarm analysis skipped because Gemini API key is not configured");
			return fallback;
		}

		try {
			HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(baseUrl + "/models/" + urlEncode(model) + ":generateContent"))
				.timeout(Duration.ofSeconds(40))
				.header("x-goog-api-key", apiKey)
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody(alarm, similarCases)), StandardCharsets.UTF_8))
				.build();

			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
			if (response.statusCode() < 200 || response.statusCode() >= 300) {
				logger.warn("Gemini alarm analysis request failed: status={}, error={}", response.statusCode(), errorMessage(response.body()));
				return fallback;
			}

			String text = extractText(response.body());
			if (text == null || text.isBlank()) {
				logger.warn("Gemini alarm analysis response was empty");
				return fallback;
			}

			AlarmAnalysisResult result = objectMapper.readValue(stripJsonFence(text), AlarmAnalysisResult.class);
			return fillDefaults(result, fallback);
		} catch (IOException exception) {
			logger.warn("failed to call Gemini alarm analysis API", exception);
			return fallback;
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			logger.warn("Gemini alarm analysis request interrupted", exception);
			return fallback;
		} catch (RuntimeException exception) {
			logger.warn("failed to parse Gemini alarm analysis response", exception);
			return fallback;
		}
	}

	private Map<String, Object> requestBody(AlarmContext alarm, List<RagSimilarCase> similarCases) {
		Map<String, Object> part = new LinkedHashMap<>();
		part.put("text", prompt(alarm, similarCases));

		Map<String, Object> content = new LinkedHashMap<>();
		content.put("role", "user");
		content.put("parts", List.of(part));

		Map<String, Object> generationConfig = new LinkedHashMap<>();
		generationConfig.put("temperature", temperature);
		generationConfig.put("responseMimeType", "application/json");

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("contents", List.of(content));
		body.put("generationConfig", generationConfig);
		return body;
	}

	private String prompt(AlarmContext alarm, List<RagSimilarCase> similarCases) {
		return """
			당신은 제조 설비 이상 알림을 분석하는 설비 운영 AI입니다.
			아래 MCP context와 RAG 유사 사례를 바탕으로 담당자에게 보낼 분석 내용을 한국어로 작성하세요.

			규칙:
			- 반드시 JSON 객체만 반환하세요. Markdown, 설명문, 코드블록은 금지입니다.
			- 과장하지 말고 입력 데이터에서 추론 가능한 내용만 작성하세요.
			- 담당자가 바로 확인할 수 있는 짧은 문장으로 작성하세요.
			- detectionDetails, expectedImpacts, recommendedActions는 각각 2~4개 항목으로 작성하세요.
			- recommendedActions에는 RAG 유사 사례가 있으면 그 조치 내용을 반영하세요.

			반환 형식:
			{
			  "aiJudgment": "문장",
			  "detectionDetails": ["문장", "문장"],
			  "expectedImpacts": ["문장", "문장"],
			  "recommendedActions": ["문장", "문장"]
			}

			MCP context:
			""" + "\n" + contextProvider.buildContext(alarm, similarCases);
	}

	private AlarmAnalysisResult fallback(AlarmContext alarm) {
		AlarmAnalysisResult result = new AlarmAnalysisResult();
		result.setAiJudgment("정상 운전 조건에서 벗어난 비정상 패턴이 감지되었습니다.");
		result.setDetectionDetails(List.of(
			detailText(alarm),
			"현재 알람 상태와 설비 건전도 점수를 기준으로 담당자 확인이 필요합니다.",
			"해당 설비에는 추가 점검 또는 조치가 필요할 수 있습니다."
		));
		result.setExpectedImpacts(List.of(
			"설비 품질 저하 가능성",
			"후속 공정 지연 또는 재작업 가능성 증가",
			"장시간 방치 시 운영 리스크 증가 가능성"
		));
		result.setRecommendedActions(List.of(
			"안전 상태를 먼저 확인한 뒤 설비 로그와 현장 상태를 점검하세요.",
			"동일 알람 재발 여부를 확인하고 필요 시 설비 담당자에게 에스컬레이션하세요."
		));
		return result;
	}

	private AlarmAnalysisResult fillDefaults(AlarmAnalysisResult result, AlarmAnalysisResult fallback) {
		if (result == null) {
			return fallback;
		}
		if (result.getAiJudgment() == null || result.getAiJudgment().isBlank()) {
			result.setAiJudgment(fallback.getAiJudgment());
		}
		if (result.getDetectionDetails().isEmpty()) {
			result.setDetectionDetails(fallback.getDetectionDetails());
		}
		if (result.getExpectedImpacts().isEmpty()) {
			result.setExpectedImpacts(fallback.getExpectedImpacts());
		}
		if (result.getRecommendedActions().isEmpty()) {
			result.setRecommendedActions(fallback.getRecommendedActions());
		}
		return result;
	}

	private String extractText(String responseBody) throws IOException {
		JsonNode parts = objectMapper.readTree(responseBody)
			.path("candidates")
			.path(0)
			.path("content")
			.path("parts");
		if (!parts.isArray()) {
			return null;
		}
		StringBuilder builder = new StringBuilder();
		for (JsonNode part : parts) {
			if (part.path("text").isTextual()) {
				builder.append(part.path("text").asText());
			}
		}
		return builder.toString();
	}

	private String stripJsonFence(String text) {
		String trimmed = text.trim();
		if (trimmed.startsWith("```")) {
			trimmed = trimmed.replaceFirst("^```json\\s*", "");
			trimmed = trimmed.replaceFirst("^```\\s*", "");
			trimmed = trimmed.replaceFirst("\\s*```$", "");
		}
		return trimmed.trim();
	}

	private String detailText(AlarmContext alarm) {
		if (alarm.getAlarmText() != null && !alarm.getAlarmText().isBlank()) {
			return alarm.getAlarmText().trim();
		}
		if (alarm.getAlarmMemo() != null && !alarm.getAlarmMemo().isBlank()) {
			return alarm.getAlarmMemo().trim();
		}
		return "상세 알람 내용이 등록되지 않았습니다.";
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

	private String stripModelPrefix(String value) {
		if (value == null || value.isBlank()) {
			return "gemini-2.0-flash";
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
}
