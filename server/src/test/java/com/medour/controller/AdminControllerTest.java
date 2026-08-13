package com.medour.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medour.dto.AdminSetPasswordRequest;
import com.medour.dto.AdminUserDto;
import com.medour.security.JwtUtil;
import com.medour.service.AdminUserService;
import com.medour.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminController.class)
class AdminControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private JwtUtil jwtUtil;

  @MockBean
  private UserService userService;

  @MockBean
  private AdminUserService adminUserService;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  @WithMockUser(username = "1", roles = "ADMIN")
  void getUsers_asAdmin_returns200WithArray() throws Exception {
    var dto = new AdminUserDto(1L, "a@b.com", "Alice", "Smith", "PATIENT",
        null, null, null, null, null, null, false, false);
    when(adminUserService.getAllUsers()).thenReturn(List.of(dto));

    mockMvc.perform(get("/api/v1/admin/users"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].email").value("a@b.com"))
        .andExpect(jsonPath("$[0].isDeleted").value(false));
  }

  @Test
  @WithMockUser(username = "1", roles = "ADMIN")
  void adminSetPassword_valid_returns204() throws Exception {
    var req = new AdminSetPasswordRequest("TempPass123!");
    doNothing().when(userService).adminSetPassword(eq(2L), any());

    mockMvc.perform(post("/api/v1/admin/users/2/password")
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isNoContent());
  }
}
