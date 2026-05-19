package com.example.backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest(properties = {
	"spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration"
})
class BackendApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void contextLoads() {
	}

	@Test
	void alarmApisReturnExpectedContract() throws Exception {
		mockMvc.perform(get("/api/alarms")
				.param("page", "0")
				.param("size", "3"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data[0].alarmId").exists())
			.andExpect(jsonPath("$.meta.totalElements").value(8));

		mockMvc.perform(get("/api/alarms/statistics")
				.param("period", "DAY"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.period").value("DAY"))
			.andExpect(jsonPath("$.data.items[0].label").exists());

		mockMvc.perform(get("/api/alarms/log"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data[0].equipmentId").exists());

		mockMvc.perform(get("/api/alarms/RBT-A1"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data[0].equipmentId").value("RBT-A1"));

		mockMvc.perform(get("/api/alarms/detail/RBT-A1"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.equipment.equipmentId").value("RBT-A1"));
	}

	@Test
	void alarmMemoCanBeUpdated() throws Exception {
		mockMvc.perform(patch("/api/alarms/memo/1")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "alarmMemo": "Cooling issue handled.",
					  "alarmStatus": "RESOLVED"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.alarmId").value(1))
			.andExpect(jsonPath("$.data.alarmMemo").value("Cooling issue handled."))
			.andExpect(jsonPath("$.data.alarmStatus").value("RESOLVED"));
	}
}
