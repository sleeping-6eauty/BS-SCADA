package com.example.backend.common;

import java.util.List;

public record PageResponse<T>(
	boolean success,
	List<T> data,
	PageMeta meta
) {
}
