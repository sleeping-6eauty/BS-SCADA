package com.example.backend.mail.rag;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.example.backend.mail.dto.AlarmContext;
import com.example.backend.mail.dto.RagSimilarCase;
import com.example.backend.mail.mapper.MailReportMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class VectorRagService {

	private static final Logger logger = LoggerFactory.getLogger(VectorRagService.class);

	private final MailReportMapper mailReportMapper;
	private final List<EmbeddingService> embeddingServices;
	private final QdrantVectorStore vectorStore;
	private final boolean vectorEnabled;
	private final String vectorProvider;

	public VectorRagService(
		MailReportMapper mailReportMapper,
		List<EmbeddingService> embeddingServices,
		QdrantVectorStore vectorStore,
		@Value("${rag.vector.enabled:false}") boolean vectorEnabled,
		@Value("${rag.vector.provider:openai}") String vectorProvider
	) {
		this.mailReportMapper = mailReportMapper;
		this.embeddingServices = embeddingServices;
		this.vectorStore = vectorStore;
		this.vectorEnabled = vectorEnabled;
		this.vectorProvider = vectorProvider == null ? "openai" : vectorProvider.trim().toLowerCase();
	}

	public List<RagSimilarCase> findSimilarCases(AlarmContext alarm, int limit) {
		if (alarm == null || alarm.getAlarmId() == null || limit <= 0) {
			return List.of();
		}

		EmbeddingService embeddingService = embeddingService();
		if (vectorEnabled && embeddingService.isConfigured() && vectorStore.isConfigured()) {
			Optional<List<Double>> embedding = embeddingService.embed(toDocument(alarm), EmbeddingPurpose.QUERY);
			if (embedding.isPresent() && vectorStore.ensureCollection()) {
				List<RagSimilarCase> vectorCases = vectorStore.search(embedding.get(), limit + 1).stream()
					.filter(similar -> !Objects.equals(similar.getAlarmId(), alarm.getAlarmId()))
					.limit(limit)
					.toList();
				if (!vectorCases.isEmpty()) {
					return vectorCases;
				}
			}
		}

		return mailReportMapper.findSimilarResolvedCases(
			alarm.getAlarmId(),
			alarm.getEquipmentId(),
			alarm.getAlarmType(),
			limit
		);
	}

	public int indexResolvedAlarmCases(int limit) {
		if (!vectorEnabled) {
			return 0;
		}
		EmbeddingService embeddingService = embeddingService();
		if (!embeddingService.isConfigured()) {
			logger.warn("vector RAG indexing skipped because {} API key is not configured", embeddingService.providerName());
			return 0;
		}
		if (!vectorStore.isConfigured() || !vectorStore.ensureCollection()) {
			logger.warn("vector RAG indexing skipped because Qdrant is not available");
			return 0;
		}

		List<AlarmContext> resolvedAlarms = mailReportMapper.findResolvedAlarmContextsForIndex(limit);
		int indexed = 0;
		for (AlarmContext alarm : resolvedAlarms) {
			Optional<List<Double>> embedding = embeddingService.embed(toDocument(alarm), EmbeddingPurpose.DOCUMENT);
			if (embedding.isPresent() && vectorStore.upsert(alarm, embedding.get())) {
				indexed++;
			}
		}
		return indexed;
	}

	private EmbeddingService embeddingService() {
		return embeddingServices.stream()
			.filter(service -> service.providerName().equalsIgnoreCase(vectorProvider))
			.findFirst()
			.orElseGet(() -> {
				logger.warn("unknown vector RAG provider '{}', falling back to openai", vectorProvider);
				return embeddingServices.stream()
					.filter(service -> service.providerName().equalsIgnoreCase("openai"))
					.findFirst()
					.orElseThrow(() -> new IllegalStateException("No embedding service is available"));
			});
	}

	private String toDocument(AlarmContext alarm) {
		StringBuilder builder = new StringBuilder();
		builder.append("equipment_id: ").append(nullSafe(alarm.getEquipmentId())).append('\n');
		builder.append("alarm_type: ").append(nullSafe(alarm.getAlarmType())).append('\n');
		builder.append("alarm_status: ").append(nullSafe(alarm.getAlarmStatus())).append('\n');
		builder.append("alarm_memo: ").append(nullSafe(alarm.getAlarmMemo())).append('\n');
		builder.append("alarm_text: ").append(nullSafe(alarm.getAlarmText())).append('\n');
		builder.append("timestamp: ").append(alarm.getTimestamp() == null ? "UNKNOWN" : alarm.getTimestamp());
		return builder.toString();
	}

	private String nullSafe(String value) {
		return value == null || value.isBlank() ? "UNKNOWN" : value.trim();
	}
}
