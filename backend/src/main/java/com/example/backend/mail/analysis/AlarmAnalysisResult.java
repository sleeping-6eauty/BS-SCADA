package com.example.backend.mail.analysis;

import java.util.ArrayList;
import java.util.List;

public class AlarmAnalysisResult {

	private String aiJudgment;
	private List<String> detectionDetails = new ArrayList<>();
	private List<String> expectedImpacts = new ArrayList<>();
	private List<String> recommendedActions = new ArrayList<>();

	public String getAiJudgment() {
		return aiJudgment;
	}

	public void setAiJudgment(String aiJudgment) {
		this.aiJudgment = aiJudgment;
	}

	public List<String> getDetectionDetails() {
		return detectionDetails;
	}

	public void setDetectionDetails(List<String> detectionDetails) {
		this.detectionDetails = detectionDetails == null ? new ArrayList<>() : detectionDetails;
	}

	public List<String> getExpectedImpacts() {
		return expectedImpacts;
	}

	public void setExpectedImpacts(List<String> expectedImpacts) {
		this.expectedImpacts = expectedImpacts == null ? new ArrayList<>() : expectedImpacts;
	}

	public List<String> getRecommendedActions() {
		return recommendedActions;
	}

	public void setRecommendedActions(List<String> recommendedActions) {
		this.recommendedActions = recommendedActions == null ? new ArrayList<>() : recommendedActions;
	}
}
