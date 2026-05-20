package com.example.backend.service;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.backend.dto.ControlStatusResponse;
import com.example.backend.mapper.DashboardMapper;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class EquipmentControlService {

    private static final String NODE_RED_BASE = "http://localhost:1880";
    private static final int TIMEOUT_MS = 3000;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DashboardMapper dashboardMapper;

    private final Map<String, String>  lastCommand   = new ConcurrentHashMap<>();
    private final Map<String, Integer> lastFrequency = new ConcurrentHashMap<>();

    @Autowired
    public EquipmentControlService(DashboardMapper dashboardMapper) {
        this.dashboardMapper = dashboardMapper;
    }

    public void turnOn(String equipmentId, int frequency) {
        if (!"CNV-001".equals(equipmentId)) return;

        Map<String, Object> freqPayload = Map.of(
                "value",    List.of(frequency),
                "fc",       16,
                "unitid",   1,
                "address",  4,
                "quantity", 1
        );
        post("/modbus/frequency", freqPayload);
        post("/modbus/on", null);

        lastCommand.put(equipmentId, "on");
        lastFrequency.put(equipmentId, frequency);
    }

    public void turnOff(String equipmentId) {
        if (!"CNV-001".equals(equipmentId)) return;

        post("/modbus/off", null);

        lastCommand.put(equipmentId, "off");
    }

    public ControlStatusResponse getStatus(String equipmentId) {
        String status = dashboardMapper.getLatestEquipmentStatus(equipmentId);
        return new ControlStatusResponse(
                equipmentId,
                status != null ? status : "UNKNOWN",
                lastCommand.get(equipmentId),
                lastFrequency.get(equipmentId)
        );
    }

    private void post(String path, Object body) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(NODE_RED_BASE + path).openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);

            if (body != null) {
                byte[] json = objectMapper.writeValueAsBytes(body);
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json");
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(json);
                }
            }

            try { conn.getResponseCode(); } catch (Exception ignored) {}
            conn.disconnect();
        } catch (Exception ignored) {}
    }
}
