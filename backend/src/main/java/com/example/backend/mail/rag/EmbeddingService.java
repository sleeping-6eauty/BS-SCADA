package com.example.backend.mail.rag;

import java.util.List;
import java.util.Optional;

public interface EmbeddingService {

	String providerName();

	boolean isConfigured();

	Optional<List<Double>> embed(String input, EmbeddingPurpose purpose);
}
