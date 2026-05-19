package com.example.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class LogEntryDto {
    // Common
    private Long logId;
    private String equipmentId;
    private String timestamp;
    private String status;
    private String lineNo;

    // Panel feeder
    private Double motorCurrentA;
    private Double vacuumPressureKpa;
    private Double positionErrorMm;

    // Body jig
    private Double clampPressureBar;
    private Double pneumaticPressureBar;
    private Double clampPositionMm;

    // Robot
    private Double robotSwivel;
    private Double robotHorizontal;
    private Double robotVertical;
    private Double toolOffsetErrorMm;
    private Double axisTempMaxC;

    // Spot welder
    private Double weldVoltageDc;
    private Double weldCurrentDc;
    private Double weldVoltageAc;
    private Double weldCurrentAc;
    private Double weldSpeed;

    // Sealer
    private Double dispensePressureBar;
    private Double sealerTemperatureC;
    private Double flowRateMlS;

    // Conveyor
    private Double motorTemperatureC;
    private Double movingSpeedMS;

    // Vision inspection
    private String bodyQuality;
    private String bodyId;

    public Long getLogId() { return logId; }
    public void setLogId(Long logId) { this.logId = logId; }

    public String getEquipmentId() { return equipmentId; }
    public void setEquipmentId(String equipmentId) { this.equipmentId = equipmentId; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getLineNo() { return lineNo; }
    public void setLineNo(String lineNo) { this.lineNo = lineNo; }

    public Double getMotorCurrentA() { return motorCurrentA; }
    public void setMotorCurrentA(Double motorCurrentA) { this.motorCurrentA = motorCurrentA; }

    public Double getVacuumPressureKpa() { return vacuumPressureKpa; }
    public void setVacuumPressureKpa(Double vacuumPressureKpa) { this.vacuumPressureKpa = vacuumPressureKpa; }

    public Double getPositionErrorMm() { return positionErrorMm; }
    public void setPositionErrorMm(Double positionErrorMm) { this.positionErrorMm = positionErrorMm; }

    public Double getClampPressureBar() { return clampPressureBar; }
    public void setClampPressureBar(Double clampPressureBar) { this.clampPressureBar = clampPressureBar; }

    public Double getPneumaticPressureBar() { return pneumaticPressureBar; }
    public void setPneumaticPressureBar(Double pneumaticPressureBar) { this.pneumaticPressureBar = pneumaticPressureBar; }

    public Double getClampPositionMm() { return clampPositionMm; }
    public void setClampPositionMm(Double clampPositionMm) { this.clampPositionMm = clampPositionMm; }

    public Double getRobotSwivel() { return robotSwivel; }
    public void setRobotSwivel(Double robotSwivel) { this.robotSwivel = robotSwivel; }

    public Double getRobotHorizontal() { return robotHorizontal; }
    public void setRobotHorizontal(Double robotHorizontal) { this.robotHorizontal = robotHorizontal; }

    public Double getRobotVertical() { return robotVertical; }
    public void setRobotVertical(Double robotVertical) { this.robotVertical = robotVertical; }

    public Double getToolOffsetErrorMm() { return toolOffsetErrorMm; }
    public void setToolOffsetErrorMm(Double toolOffsetErrorMm) { this.toolOffsetErrorMm = toolOffsetErrorMm; }

    public Double getAxisTempMaxC() { return axisTempMaxC; }
    public void setAxisTempMaxC(Double axisTempMaxC) { this.axisTempMaxC = axisTempMaxC; }

    public Double getWeldVoltageDc() { return weldVoltageDc; }
    public void setWeldVoltageDc(Double weldVoltageDc) { this.weldVoltageDc = weldVoltageDc; }

    public Double getWeldCurrentDc() { return weldCurrentDc; }
    public void setWeldCurrentDc(Double weldCurrentDc) { this.weldCurrentDc = weldCurrentDc; }

    public Double getWeldVoltageAc() { return weldVoltageAc; }
    public void setWeldVoltageAc(Double weldVoltageAc) { this.weldVoltageAc = weldVoltageAc; }

    public Double getWeldCurrentAc() { return weldCurrentAc; }
    public void setWeldCurrentAc(Double weldCurrentAc) { this.weldCurrentAc = weldCurrentAc; }

    public Double getWeldSpeed() { return weldSpeed; }
    public void setWeldSpeed(Double weldSpeed) { this.weldSpeed = weldSpeed; }

    public Double getDispensePressureBar() { return dispensePressureBar; }
    public void setDispensePressureBar(Double dispensePressureBar) { this.dispensePressureBar = dispensePressureBar; }

    public Double getSealerTemperatureC() { return sealerTemperatureC; }
    public void setSealerTemperatureC(Double sealerTemperatureC) { this.sealerTemperatureC = sealerTemperatureC; }

    public Double getFlowRateMlS() { return flowRateMlS; }
    public void setFlowRateMlS(Double flowRateMlS) { this.flowRateMlS = flowRateMlS; }

    public Double getMotorTemperatureC() { return motorTemperatureC; }
    public void setMotorTemperatureC(Double motorTemperatureC) { this.motorTemperatureC = motorTemperatureC; }

    public Double getMovingSpeedMS() { return movingSpeedMS; }
    public void setMovingSpeedMS(Double movingSpeedMS) { this.movingSpeedMS = movingSpeedMS; }

    public String getBodyQuality() { return bodyQuality; }
    public void setBodyQuality(String bodyQuality) { this.bodyQuality = bodyQuality; }

    public String getBodyId() { return bodyId; }
    public void setBodyId(String bodyId) { this.bodyId = bodyId; }
}
