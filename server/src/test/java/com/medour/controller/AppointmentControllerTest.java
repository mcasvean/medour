package com.medour.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medour.dto.AppointmentCreatedResponse;
import com.medour.dto.CreateAppointmentRequest;
import com.medour.dto.ReserveSlotRequest;
import com.medour.dto.ReserveSlotResponse;
import com.medour.security.JwtUtil;
import com.medour.service.AppointmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AppointmentController.class)
class AppointmentControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private JwtUtil jwtUtil;

  @MockBean
  private AppointmentService appointmentService;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  @WithMockUser(username = "1", roles = "PATIENT")
  void reserve_validSlot_returns201() throws Exception {
    var req = new ReserveSlotRequest(10L, LocalDate.of(2026, 9, 1), LocalTime.of(10, 0));
    given(appointmentService.reserveSlot(eq(1L), any())).willReturn(new ReserveSlotResponse(42L));

    mockMvc.perform(post("/api/v1/slots/reserve")
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.reservationId").value(42));
  }

  @Test
  @WithMockUser(username = "1", roles = "PATIENT")
  void cancel_ownReservation_returns204() throws Exception {
    doNothing().when(appointmentService).cancelReservation(eq(1L), eq(5L));

    mockMvc.perform(delete("/api/v1/slots/reserve/5")
        .with(csrf()))
        .andExpect(status().isNoContent());
  }

  @Test
  @WithMockUser(username = "1", roles = "PATIENT")
  void confirm_validReservation_returns201() throws Exception {
    var req = new CreateAppointmentRequest(42L);
    var resp = new AppointmentCreatedResponse(
        1L, 10L, LocalDate.of(2026, 9, 1), LocalTime.of(10, 0), "OPEN");
    given(appointmentService.createAppointment(eq(1L), any())).willReturn(resp);

    mockMvc.perform(post("/api/v1/appointments")
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(1));
  }
}
