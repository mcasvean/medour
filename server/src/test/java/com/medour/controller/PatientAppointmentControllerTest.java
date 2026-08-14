package com.medour.controller;

import com.medour.dto.PatientAppointmentDto;
import com.medour.security.JwtUtil;
import com.medour.service.PatientAppointmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PatientAppointmentController.class)
class PatientAppointmentControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private JwtUtil jwtUtil;

  @MockBean
  private PatientAppointmentService patientAppointmentService;

  @Test
  @WithMockUser(username = "1", roles = "PATIENT")
  void getHistory_asPatient_returns200WithList() throws Exception {
    var dto = new PatientAppointmentDto(
        10L,
        20L,
        LocalDate.of(2026, 9, 1),
        LocalTime.of(10, 0),
        "John",
        "Smith",
        "Cardiology",
        false,
        "OPEN",
        LocalDateTime.of(2026, 8, 1, 12, 0),
        "https://whereby.com/test-room",
        null,
        null);
    given(patientAppointmentService.getHistory(1L)).willReturn(List.of(dto));

    mockMvc.perform(get("/api/v1/appointments/my"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(10))
        .andExpect(jsonPath("$[0].status").value("OPEN"))
        .andExpect(jsonPath("$[0].doctorRemoved").value(false));
  }

  @Test
  @WithMockUser(username = "1", roles = "PATIENT")
  void getHistory_noAppointments_returns200WithEmptyList() throws Exception {
    given(patientAppointmentService.getHistory(1L)).willReturn(List.of());

    mockMvc.perform(get("/api/v1/appointments/my"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(0));
  }
}
