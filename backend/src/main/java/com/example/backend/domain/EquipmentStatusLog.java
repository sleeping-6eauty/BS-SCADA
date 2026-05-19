package com.example.backend.domain;

import java.time.LocalDateTime;

public class EquipmentStatusLog {
    private Long statusLogId;
    private String equipmentId;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer durationSec;
    private LocalDateTime createdAt;

    public Long getStatusLogId() { return statusLogId; }
    public void setStatusLogId(Long statusLogId) { this.statusLogId = statusLogId; }

    public String getEquipmentId() { return equipmentId; }
    public void setEquipmentId(String equipmentId) { this.equipmentId = equipmentId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public Integer getDurationSec() { return durationSec; }
    public void setDurationSec(Integer durationSec) { this.durationSec = durationSec; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
