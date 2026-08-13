package com.medour.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medour.dto.AuthResponse;
import com.medour.exception.EmailAlreadyUsedException;
import com.medour.exception.InvalidCredentialsException;
import com.medour.security.JwtAuthFilter;
import com.medour.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private JwtAuthFilter jwtAuthFilter;

  @MockBean
  private UserService userService;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  void register_validPatient_returns200WithToken() throws Exception {
    var response = new AuthResponse("tok-123", 1L, "p@test.com", "Pat", "Ient", "PATIENT", false);
    given(userService.register(any())).willReturn(response);

    mockMvc.perform(post("/api/v1/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(buildRequest("PATIENT", null, null))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.token").value("tok-123"));
  }

  @Test
  void register_validDoctor_returns200() throws Exception {
    var response = new AuthResponse("tok-456", 2L, "d@test.com", "Doc", "Tor", "DOCTOR", false);
    given(userService.register(any())).willReturn(response);

    mockMvc.perform(post("/api/v1/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(buildRequest("DOCTOR", "Galway", "Cardiology"))))
        .andExpect(status().isCreated());
  }

  @Test
  void register_duplicateEmail_returns409() throws Exception {
    given(userService.register(any())).willThrow(new EmailAlreadyUsedException());

    mockMvc.perform(post("/api/v1/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(buildRequest("PATIENT", null, null))))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error").value("Email already in use"));
  }

  @Test
  void register_missingDoctorFields_returns400() throws Exception {
    given(userService.register(any())).willThrow(
        new ResponseStatusException(HttpStatus.BAD_REQUEST, "county and speciality are required for DOCTOR"));

    mockMvc.perform(post("/api/v1/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(buildRequest("DOCTOR", null, null))))
        .andExpect(status().isBadRequest());
  }

  @Test
  void login_validCredentials_returns200WithToken() throws Exception {
    var response = new AuthResponse("tok-789", 1L, "p@test.com", "Pat", "Ient", "PATIENT", false);
    given(userService.login(any())).willReturn(response);

    mockMvc.perform(post("/api/v1/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(Map.of("email", "p@test.com", "password", "Password1!"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").value("tok-789"))
        .andExpect(jsonPath("$.mustChangePassword").value(false));
  }

  @Test
  void login_wrongPassword_returns401() throws Exception {
    given(userService.login(any())).willThrow(new InvalidCredentialsException());

    mockMvc.perform(post("/api/v1/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(Map.of("email", "p@test.com", "password", "wrongpass"))))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value("Invalid credentials"));
  }

  @Test
  void login_unknownEmail_returns401() throws Exception {
    given(userService.login(any())).willThrow(new InvalidCredentialsException());

    mockMvc.perform(post("/api/v1/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(Map.of("email", "nobody@test.com", "password", "Password1!"))))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value("Invalid credentials"));
  }

  @Test
  void login_softDeletedUser_returns401() throws Exception {
    given(userService.login(any())).willThrow(new InvalidCredentialsException());

    mockMvc.perform(post("/api/v1/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(Map.of("email", "deleted@test.com", "password", "Password1!"))))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value("Invalid credentials"));
  }

  private Map<String, Object> buildRequest(String role, String county, String speciality) {
    Map<String, Object> req = new HashMap<>();
    req.put("email", "test@medour.com");
    req.put("password", "Password1!");
    req.put("firstName", "First");
    req.put("surname", "Last");
    req.put("age", 30);
    req.put("gender", "M");
    req.put("city", "Dublin");
    req.put("address", "1 Main St");
    req.put("role", role);
    if (county != null)
      req.put("county", county);
    if (speciality != null)
      req.put("speciality", speciality);
    return req;
  }
}

