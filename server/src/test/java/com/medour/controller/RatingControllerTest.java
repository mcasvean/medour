package com.medour.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medour.dto.SubmitRatingRequest;
import com.medour.exception.RatingAlreadyExistsException;
import com.medour.model.Appointment;
import com.medour.model.AppointmentStatus;
import com.medour.model.Rating;
import com.medour.model.User;
import com.medour.security.JwtUtil;
import com.medour.service.RatingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RatingController.class)
class RatingControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private JwtUtil jwtUtil;

  @MockBean
  private RatingService ratingService;

  private Rating buildRating(long id, int value) {
    User patient = User.builder().id(1L).build();
    User doctor = User.builder().id(10L).build();
    Appointment appt = Appointment.builder()
        .id(5L).patient(patient).doctor(doctor)
        .scheduledDate(LocalDate.of(2026, 9, 1))
        .startTime(LocalTime.of(10, 0))
        .status(AppointmentStatus.COMPLETED)
        .build();
    return Rating.builder().id(id).appointment(appt).patient(patient).doctor(doctor).value(value).build();
  }

  @Test
  @WithMockUser(username = "1", roles = "PATIENT")
  void submitRating_valid_returns201() throws Exception {
    given(ratingService.submitRating(anyLong(), anyInt(), anyLong()))
        .willReturn(buildRating(99L, 7));

    mockMvc.perform(post("/api/v1/ratings")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new SubmitRatingRequest(5L, 7))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(99))
        .andExpect(jsonPath("$.value").value(7));
  }

  @Test
  @WithMockUser(username = "1", roles = "PATIENT")
  void submitRating_valueAboveMax_returns400() throws Exception {
    mockMvc.perform(post("/api/v1/ratings")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new SubmitRatingRequest(5L, 11))))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(username = "1", roles = "PATIENT")
  void submitRating_valueOutOfRange_returns400() throws Exception {
    mockMvc.perform(post("/api/v1/ratings")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new SubmitRatingRequest(5L, 0))))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(username = "1", roles = "PATIENT")
  void submitRating_duplicate_returns409() throws Exception {
    given(ratingService.submitRating(anyLong(), anyInt(), anyLong()))
        .willThrow(new RatingAlreadyExistsException());

    mockMvc.perform(post("/api/v1/ratings")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new SubmitRatingRequest(5L, 7))))
        .andExpect(status().isConflict());
  }

  @Test
  @WithMockUser(username = "1", roles = "PATIENT")
  void submitRating_wrongPatient_returns403() throws Exception {
    given(ratingService.submitRating(anyLong(), anyInt(), anyLong()))
        .willThrow(new ResponseStatusException(FORBIDDEN, "Not your appointment"));

    mockMvc.perform(post("/api/v1/ratings")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new SubmitRatingRequest(5L, 7))))
        .andExpect(status().isForbidden());
  }

  @Test
  void submitRating_unauthenticated_returns401() throws Exception {
    mockMvc.perform(post("/api/v1/ratings")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new SubmitRatingRequest(5L, 7))))
        .andExpect(status().isUnauthorized());
  }
}
