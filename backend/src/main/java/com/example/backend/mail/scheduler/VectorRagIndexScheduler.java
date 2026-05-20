package com.example.backend.mail.scheduler;

import com.example.backend.mail.rag.VectorRagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "rag.vector.enabled", havingValue = "true")
public class VectorRagIndexScheduler {

	private static final Logger logger = LoggerFactory.getLogger(VectorRagIndexScheduler.class);

	private final VectorRagService vectorRagService;
	private final int indexLimit;

	public VectorRagIndexScheduler(
		VectorRagService vectorRagService,
		@Value("${rag.vector.index-limit:200}") int indexLimit
	) {
		this.vectorRagService = vectorRagService;
		this.indexLimit = indexLimit;
	}

	@Scheduled(
		initialDelayString = "${rag.vector.initial-delay-ms:10000}",
		fixedDelayString = "${rag.vector.index-delay-ms:300000}"
	)
	public void indexResolvedAlarmCases() {
		try {
			int indexed = vectorRagService.indexResolvedAlarmCases(indexLimit);
			if (indexed > 0) {
				logger.info("resolved alarm vectors indexed: {}", indexed);
			}
		} catch (RuntimeException exception) {
			logger.error("failed to index resolved alarm vectors", exception);
		}
	}
}
