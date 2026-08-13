package com.medour.controller;

import com.medour.dto.UpdateProfileRequest;
import com.medour.dto.UserProfileResponse;
import com.medour.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  @GetMapping("/me")
  public ResponseEntity<UserProfileResponse> getMe(Authentication auth) {
    return ResponseEntity.ok(userService.getProfile(parseUserId(auth)));
  }

  @PutMapping("/me")
  public ResponseEntity<UserProfileResponse> updateMe(Authentication auth,
      @Valid @RequestBody UpdateProfileRequest req) {
    return ResponseEntity.ok(userService.updateProfile(parseUserId(auth), req));
  }

  private long parseUserId(Authentication auth) {
    try {
      return Long.parseLong(auth.getName());
    } catch (NumberFormatException e) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid principal");
    }
  }
}
