package com.example.backend.alarm;

import java.time.LocalDateTime;
import java.util.List;

final class AlarmSeedData {

	private AlarmSeedData() {
	}

	static List<Alarm> create() {
		return List.of(
			new Alarm(1L, 1001L, "RBT-A1", LocalDateTime.of(2024, 5, 24, 10, 25, 33), "Motor Overheat",
				"Motor temperature exceeded the allowed range.", "IN_PROGRESS", "Robot A1",
				"Zone A - Line 1", "High", "Kim Jiwoo", "Motor temperature: 82.4 C (limit: 80 C)",
				"Check motor cooling fan and inspect worn parts."),
			new Alarm(2L, 1002L, "NTR-B2", LocalDateTime.of(2024, 5, 24, 10, 18, 17), "Torque Overload",
				"Nutrunner torque exceeded the configured threshold.", "ACK", "Nutrunner B2",
				"Zone B - Line 2", "Medium", "Lee Sujin", "Torque peak: 118 Nm (limit: 110 Nm)",
				"Inspect torque calibration and spindle condition."),
			new Alarm(3L, 1003L, "PRS-C1", LocalDateTime.of(2024, 5, 24, 9, 31, 5), "Pressure Sensor Fault",
				"Hydraulic pressure sensor reported an abnormal value.", "OPEN", "Press C1",
				"Zone C - Line 1", "High", "Park Minho", "Hydraulic pressure: unstable signal",
				"Verify sensor wiring and replace the pressure sensor if needed."),
			new Alarm(4L, 1004L, "CNV-D1", LocalDateTime.of(2024, 5, 24, 8, 47, 21), "Maintenance Overdue",
				"Scheduled maintenance window has passed.", "RESOLVED", "Conveyor D1",
				"Zone D - Line 1", "Medium", "Choi Daeun", "Maintenance completed",
				"Continue routine monitoring."),
			new Alarm(5L, 1005L, "RBT-A2", LocalDateTime.of(2024, 5, 24, 7, 22, 11), "Vision Recognition",
				"Vision recognition confidence dropped below threshold.", "RESOLVED", "Robot A2",
				"Zone A - Line 2", "Low", "Kim Jiwoo", "Recognition confidence: 86%",
				"Clean lens and check lighting conditions."),
			new Alarm(6L, 1006L, "NTR-B1", LocalDateTime.of(2024, 5, 23, 16, 15, 44), "Vibration Excess",
				"Vibration level exceeded warning threshold.", "RESOLVED", "Nutrunner B1",
				"Zone B - Line 1", "High", "Lee Sujin", "Vibration: 7.2 mm/s",
				"Inspect bearing and tighten fixture bolts."),
			new Alarm(7L, 1007L, "AGV-F1", LocalDateTime.of(2024, 5, 23, 14, 3, 33), "Battery Warning",
				"AGV battery level is low.", "RESOLVED", "AGV F1",
				"Zone F - Logistics", "Medium", "Park Minho", "Battery: 18%",
				"Move AGV to charging station."),
			new Alarm(8L, 1008L, "WLD-E1", LocalDateTime.of(2024, 5, 23, 14, 11, 9), "Weld Spark Warning",
				"Welding spark pattern deviated from normal range.", "RESOLVED", "Welding E1",
				"Zone E - Line 1", "Low", "Choi Daeun", "Spark variance: warning",
				"Check electrode wear and welding parameters.")
		);
	}
}
