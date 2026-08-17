package com.medour.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medour.dto.ChangePasswordRequest;
import com.medour.dto.UpdateProfileRequest;
import com.medour.dto.UserProfileResponse;
import com.medour.exception.WrongPasswordException;
import com.medour.security.JwtUtil;
import com.medour.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {

  @Autowired
  private MockMvc mockMvc;

  /**
   * Mocking JwtUtil satisfies JwtAuthFilter's dependency while keeping the real
   * filter,
   * which passes unauthenticated requests through the chain so the security
   * wrappers run.
   */
  @MockBean
  private JwtUtil jwtUtil;

  @MockBean
  private UserService userService;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  @WithMockUser(username = "1")
  void getMe_validUser_returns200WithEmail() throws Exception {
    var profile = new UserProfileResponse(1L, "p@test.com", "Pat", "Ient", "PATIENT",
        30, "M", "Cork", "Main St", null, null, null, null);
    given(userService.getProfile(1L)).willReturn(profile);

    mockMvc.perform(get("/api/v1/users/me"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("p@test.com"));
  }

  @Test
  @WithMockUser(username = "1")
  void updateMe_validRequest_returns200WithUpdatedFirstName() throws Exception {
    var req = new UpdateProfileRequest("NewPat", "Ient", 30, "M", "Cork", "Main St", null, null);
    var profile = new UserProfileResponse(1L, "p@test.com", "NewPat", "Ient", "PATIENT",
        30, "M", "Cork", "Main St", null, null, null, null);
    given(userService.updateProfile(eq(1L), any())).willReturn(profile);

    mockMvc.perform(put("/api/v1/users/me")
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.firstName").value("NewPat"));
  }

  @Test
  @WithMockUser(username = "1")
  void updateMe_blankFirstName_returns400() throws Exception {
    var req = new UpdateProfileRequest("", "Ient", null, null, null, null, null, null);

    mockMvc.perform(put("/api/v1/users/me")
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(username = "1")
  void changePassword_valid_returns204() throws Exception {
    var req = new ChangePasswordRequest("oldPass", "newPass");
    doNothing().when(userService).changePassword(eq(1L), any());

    mockMvc.perform(post("/api/v1/users/me/password")
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isNoContent());
  }

  @Test
  @WithMockUser(username = "1")
  void changePassword_wrongPassword_returns403() throws Exception {
    var req = new ChangePasswordRequest("wrongPass", "newPass");
    doThrow(new WrongPasswordException()).when(userService).changePassword(eq(1L), any());

    mockMvc.perform(post("/api/v1/users/me/password")
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error").value("Wrong password"));
  }

  @Test
  @WithMockUser(username = "1")
  void changePassword_blankNewPassword_returns400() throws Exception {
    var req = new ChangePasswordRequest("oldPass", "");

    mockMvc.perform(post("/api/v1/users/me/password")
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isBadRequest());
  }
}
