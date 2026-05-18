package com.example.backend.common;

public record PageMeta(
	int page,
	int size,
	long totalElements,
	int totalPages
) {
}
