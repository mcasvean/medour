package com.medour.controller;

import com.medour.dto.AdminSetPasswordRequest;
import com.medour.dto.AdminUserDto;
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

  public AdminController(UserService userService, AdminUserService adminUserService) {
    this.userService = userService;
    this.adminUserService = adminUserService;
  }

  @GetMapping("/users")
  public ResponseEntity<List<AdminUserDto>> getUsers() {
    return ResponseEntity.ok(adminUserService.getAllUsers());
  }

  @PostMapping("/users/{id}/password")
  public ResponseEntity<Void> adminSetPassword(@PathVariable Long id,
      @Valid @RequestBody AdminSetPasswordRequest req) {
    userService.adminSetPassword(id, req);
    return ResponseEntity.noContent().build();
  }
}
