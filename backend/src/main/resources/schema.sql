CREATE TABLE IF NOT EXISTS alarm_log (
  alarm_id BIGINT NOT NULL AUTO_INCREMENT,
  log_id BIGINT NULL,
  equipment_id VARCHAR(50) NULL,
  `timestamp` DATETIME NULL,
  alarm_type VARCHAR(50) NULL,
  alarm_memo TEXT NULL,
  alarm_text TEXT NULL,
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
  report_type VARCHAR(30) NULL DEFAULT 'ALARM',
  recipient_user_id BIGINT NULL,
  recipient_email VARCHAR(255) NULL,
  recipient_name VARCHAR(100) NULL,
  source_report_date DATE NULL,
  `timestamp` DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
  mail_text TEXT NULL,
  PRIMARY KEY (mail_id),
  INDEX idx_mail_report_alarm_id (alarm_id),
  INDEX idx_mail_report_recipient_user_id (recipient_user_id),
  INDEX idx_mail_report_report_type (report_type),
  INDEX idx_mail_report_source_report_date (source_report_date),
  INDEX idx_mail_report_timestamp (`timestamp`),
  UNIQUE INDEX uq_mail_report_alarm_recipient_type (alarm_id, recipient_user_id, report_type),
  UNIQUE INDEX uq_mail_report_daily_recipient_date (report_type, recipient_user_id, source_report_date),
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
  expected_lifetime_hours INT NULL,
  accumulated_run_hours DECIMAL(10,2) NULL,
  PRIMARY KEY (equipment_id)
);
