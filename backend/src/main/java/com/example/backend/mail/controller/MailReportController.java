package com.example.backend.mail.controller;

import java.util.List;

import com.example.backend.common.ApiResponse;
import com.example.backend.mail.dto.MailReport;
import com.example.backend.mail.dto.MailReportCreateRequest;
import com.example.backend.mail.service.MailReportService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mail-reports")
public class MailReportController {

	private final MailReportService mailReportService;

	public MailReportController(MailReportService mailReportService) {
		this.mailReportService = mailReportService;
	}

	@PostMapping
	public ApiResponse<MailReport> create(@Valid @RequestBody MailReportCreateRequest request) {
		return ApiResponse.ok("mail report created", mailReportService.create(request));
	}

	@PostMapping("/daily")
	public ApiResponse<List<MailReport>> createDailySummary() {
		return ApiResponse.ok("daily mail report created", mailReportService.createDailySummaryReport());
	}

	@GetMapping
	public ApiResponse<List<MailReport>> findAll(@RequestParam(required = false) Long alarmId) {
		return ApiResponse.ok("mail reports", mailReportService.findAll(alarmId));
	}

	@GetMapping("/{mailId}")
	public ApiResponse<MailReport> findById(@PathVariable long mailId) {
		return ApiResponse.ok("mail report", mailReportService.findById(mailId));
	}
}
