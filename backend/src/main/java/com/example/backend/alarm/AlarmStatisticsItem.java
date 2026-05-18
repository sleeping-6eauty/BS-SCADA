package com.example.backend.alarm;

public class AlarmStatisticsItem {

	private String label;
	private Long value;

	public AlarmStatisticsItem() {
	}

	public AlarmStatisticsItem(String label, Long value) {
		this.label = label;
		this.value = value;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public Long getValue() {
		return value;
	}

	public void setValue(Long value) {
		this.value = value;
	}
}
