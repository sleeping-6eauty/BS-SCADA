package com.example.backend.mail.dto;

public class DailyAlarmStatusCount {

	private long totalCount;
	private long openCount;
	private long inProgressCount;
	private long resolvedCount;

	public long getTotalCount() {
		return totalCount;
	}

	public void setTotalCount(long totalCount) {
		this.totalCount = totalCount;
	}

	public long getOpenCount() {
		return openCount;
	}

	public void setOpenCount(long openCount) {
		this.openCount = openCount;
	}

	public long getInProgressCount() {
		return inProgressCount;
	}

	public void setInProgressCount(long inProgressCount) {
		this.inProgressCount = inProgressCount;
	}

	public long getResolvedCount() {
		return resolvedCount;
	}

	public void setResolvedCount(long resolvedCount) {
		this.resolvedCount = resolvedCount;
	}
}
