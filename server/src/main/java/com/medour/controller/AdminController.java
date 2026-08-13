package com.medour.controller;

import com.medour.dto.AdminAppointmentDto;
import com.medour.dto.AdminSetPasswordRequest;
import com.medour.dto.AdminUserCreateRequest;
import com.medour.dto.AdminUserDto;
import com.medour.dto.AdminUserUpdateRequest;
import com.medour.service.AdminAppointmentService;
import com.medour.service.AdminUserService;
import com.medour.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

  private final UserService userService;
  private final AdminUserService adminUserService;
  private final AdminAppointmentService adminAppointmentService;

  public AdminController(UserService userService, AdminUserService adminUserService,
      AdminAppointmentService adminAppointmentService) {
    this.userService = userService;
    this.adminUserService = adminUserService;
    this.adminAppointmentService = adminAppointmentService;
  }

  @GetMapping("/users")
  public ResponseEntity<List<AdminUserDto>> getUsers() {
    return ResponseEntity.ok(adminUserService.getAllUsers());
  }

  @PostMapping("/users")
  public ResponseEntity<AdminUserDto> createUser(@Valid @RequestBody AdminUserCreateRequest req) {
    return ResponseEntity.status(201).body(adminUserService.createUser(req));
  }

  @PutMapping("/users/{id}")
  public ResponseEntity<AdminUserDto> updateUser(@PathVariable Long id,
      @Valid @RequestBody AdminUserUpdateRequest req) {
    return ResponseEntity.ok(adminUserService.updateUser(id, req));
  }

  @DeleteMapping("/users/{id}")
  public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
    adminUserService.deleteUser(id);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/users/{id}/password")
  public ResponseEntity<Void> adminSetPassword(@PathVariable Long id,
      @Valid @RequestBody AdminSetPasswordRequest req) {
    userService.adminSetPassword(id, req);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/appointments")
  public ResponseEntity<List<AdminAppointmentDto>> getAppointments() {
    return ResponseEntity.ok(adminAppointmentService.getAllAppointments());
  }

  @DeleteMapping("/appointments/{id}")
  public ResponseEntity<Void> deleteAppointment(@PathVariable Long id) {
    adminAppointmentService.deleteAppointment(id);
    return ResponseEntity.noContent().build();
  }
}
