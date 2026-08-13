package com.medour.controller;

import com.medour.dto.DoctorAppointmentDto;
import com.medour.security.JwtUtil;
import com.medour.service.DoctorAppointmentService;
import com.medour.service.SseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DoctorAppointmentController.class)
class DoctorAppointmentControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private JwtUtil jwtUtil;

  @MockBean
  private DoctorAppointmentService doctorAppointmentService;

  @MockBean
  private SseService sseService;

  @Test
  @WithMockUser(username = "1", roles = "DOCTOR")
  void getAppointments_asDoctor_returns200WithList() throws Exception {
    var dto = new DoctorAppointmentDto(
        20L,
        LocalDate.of(2026, 9, 10),
        LocalTime.of(9, 0),
        "Alice",
        "Smith",
        false,
        "OPEN",
        LocalDateTime.of(2026, 8, 1, 12, 0),
        "https://whereby.com/room");
    given(doctorAppointmentService.getAppointments(1L)).willReturn(List.of(dto));

    mockMvc.perform(get("/api/v1/appointments/doctor/my"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(20))
        .andExpect(jsonPath("$[0].status").value("OPEN"))
        .andExpect(jsonPath("$[0].patientRemoved").value(false));
  }

  @Test
  @WithMockUser(username = "1", roles = "DOCTOR")
  void updateStatus_validCancel_returns200WithStatus() throws Exception {
    mockMvc.perform(patch("/api/v1/appointments/doctor/10/status")
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"newStatus\":\"CANCELED\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CANCELED"));
  }

  @Test
  @WithMockUser(username = "1", roles = "DOCTOR")
  void updateStatus_invalidNewStatus_returns400() throws Exception {
    org.mockito.Mockito.doThrow(
        new org.springframework.web.server.ResponseStatusException(
            org.springframework.http.HttpStatus.BAD_REQUEST, "Invalid status value"))
        .when(doctorAppointmentService).updateStatus(10L, 1L, "OPEN");

    mockMvc.perform(patch("/api/v1/appointments/doctor/10/status")
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"newStatus\":\"OPEN\"}"))
        .andExpect(status().isBadRequest());
  }
}
