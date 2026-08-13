package com.medour.controller;

import com.medour.dto.DoctorAppointmentDto;
import com.medour.security.JwtUtil;
import com.medour.service.DoctorAppointmentService;
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

@WebMvcTest(DoctorAppointmentController.class)
class DoctorAppointmentControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private JwtUtil jwtUtil;

  @MockBean
  private DoctorAppointmentService doctorAppointmentService;

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
}
