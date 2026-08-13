package com.medour.controller;

import com.medour.dto.AdminSetPasswordRequest;
import com.medour.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

  private final UserService userService;

  public AdminController(UserService userService) {
    this.userService = userService;
  }

  @PostMapping("/users/{id}/password")
  public ResponseEntity<Void> adminSetPassword(@PathVariable Long id,
      @Valid @RequestBody AdminSetPasswordRequest req) {
    userService.adminSetPassword(id, req);
    return ResponseEntity.noContent().build();
  }
}
