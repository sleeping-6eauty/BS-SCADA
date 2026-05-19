CREATE TABLE IF NOT EXISTS alarm_log (
  alarm_id BIGINT NOT NULL AUTO_INCREMENT,
  log_id BIGINT NULL,
  equipment_id VARCHAR(50) NULL,
  `timestamp` DATETIME NULL,
  alarm_type VARCHAR(50) NULL,
  alarm_memo TEXT NULL,
  alarm_status VARCHAR(50) NULL,
  created_at DATETIME(6) NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (alarm_id),
  INDEX idx_alarm_log_equipment_id (equipment_id),
  INDEX idx_alarm_log_status (alarm_status),
  INDEX idx_alarm_log_type (alarm_type),
  INDEX idx_alarm_log_timestamp (`timestamp`)
);

CREATE TABLE IF NOT EXISTS mail_report (
  mail_id BIGINT NOT NULL AUTO_INCREMENT,
  alarm_id BIGINT NULL,
  `timestamp` DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
  mail_text TEXT NULL,
  PRIMARY KEY (mail_id),
  INDEX idx_mail_report_alarm_id (alarm_id),
  INDEX idx_mail_report_timestamp (`timestamp`),
  CONSTRAINT fk_mail_report_alarm_id
    FOREIGN KEY (alarm_id)
    REFERENCES alarm_log (alarm_id)
    ON DELETE SET NULL
    ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS user_equipment (
  user_id INT NOT NULL,
  equipment_id VARCHAR(50) NOT NULL,
  PRIMARY KEY (user_id, equipment_id)
);

CREATE TABLE IF NOT EXISTS equipment (
  equipment_id VARCHAR(50) NOT NULL,
  equipment_name VARCHAR(50) NULL,
  line_no VARCHAR(50) NULL,
  zone VARCHAR(50) NULL,
  manufacturer VARCHAR(50) NULL,
  cycle_time FLOAT NULL,
  health_score FLOAT NULL,
  remaining_life FLOAT NULL,
  replacement_date DATE NULL,
  PRIMARY KEY (equipment_id)
);
