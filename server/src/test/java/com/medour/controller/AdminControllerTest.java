package com.medour.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medour.dto.AdminSetPasswordRequest;
import com.medour.dto.AdminUserCreateRequest;
import com.medour.dto.AdminUserDto;
import com.medour.dto.AdminUserUpdateRequest;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

  @Test
  @WithMockUser(username = "1", roles = "ADMIN")
  void createUser_valid_returns201WithEmail() throws Exception {
    var req = new AdminUserCreateRequest("new@test.com", "Pass1!", "New", "User",
        null, null, null, null, null, null, "PATIENT");
    var dto = new AdminUserDto(10L, "new@test.com", "New", "User", "PATIENT",
        null, null, null, null, null, null, false, false);
    when(adminUserService.createUser(any())).thenReturn(dto);

    mockMvc.perform(post("/api/v1/admin/users")
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.email").value("new@test.com"));
  }

  @Test
  @WithMockUser(username = "1", roles = "ADMIN")
  void deleteUser_asAdmin_returns204() throws Exception {
    doNothing().when(adminUserService).deleteUser(1L);

    mockMvc.perform(delete("/api/v1/admin/users/1")
        .with(csrf()))
        .andExpect(status().isNoContent());

    verify(adminUserService).deleteUser(1L);
  }

  @Test
  @WithMockUser(username = "1", roles = "ADMIN")
  void updateUser_valid_returns200WithRole() throws Exception {
    var req = new AdminUserUpdateRequest("Alice", "Smith", null, null, null, null, null, null, "ADMIN");
    var dto = new AdminUserDto(1L, "a@b.com", "Alice", "Smith", "ADMIN",
        null, null, null, null, null, null, false, false);
    when(adminUserService.updateUser(eq(1L), any())).thenReturn(dto);

    mockMvc.perform(put("/api/v1/admin/users/1")
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role").value("ADMIN"));
  }
}
