package com.example.backend.mail.analysis;

import java.util.List;

import com.example.backend.mail.dto.AlarmContext;
import com.example.backend.mail.dto.RagSimilarCase;

public interface AlarmAnalysisService {

	AlarmAnalysisResult analyze(AlarmContext alarm, List<RagSimilarCase> similarCases);
}
