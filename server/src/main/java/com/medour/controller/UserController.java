package com.medour.controller;

import com.medour.dto.ChangePasswordRequest;
import com.medour.dto.UpdatePreferencesRequest;
import com.medour.dto.UpdateProfileRequest;
import com.medour.dto.UserPreferenceDto;
import com.medour.dto.UserProfileResponse;
import com.medour.service.UserPreferenceService;
import com.medour.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

  private final UserService userService;
  private final UserPreferenceService userPreferenceService;

  public UserController(UserService userService, UserPreferenceService userPreferenceService) {
    this.userService = userService;
    this.userPreferenceService = userPreferenceService;
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

  @PostMapping("/me/password")
  public ResponseEntity<Void> changePassword(Authentication auth,
      @Valid @RequestBody ChangePasswordRequest req) {
    userService.changePassword(parseUserId(auth), req);
    return ResponseEntity.noContent().build();
  }

  @PatchMapping(value = "/me/profile-picture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<UserProfileResponse> uploadProfilePicture(Authentication auth,
      @RequestParam("file") MultipartFile file) {
    return ResponseEntity.ok(userService.updateProfilePicture(parseUserId(auth), file));
  }

  @DeleteMapping("/me/profile-picture")
  public ResponseEntity<Void> removeProfilePicture(Authentication auth) {
    userService.removeProfilePicture(parseUserId(auth));
    return ResponseEntity.ok().build();
  }

  @GetMapping("/me/preferences")
  public ResponseEntity<UserPreferenceDto> getPreferences(Authentication auth) {
    return ResponseEntity.ok(userPreferenceService.getOrCreate(parseUserId(auth)));
  }

  @PatchMapping("/me/preferences")
  public ResponseEntity<UserPreferenceDto> updatePreferences(Authentication auth,
      @Valid @RequestBody UpdatePreferencesRequest req) {
    return ResponseEntity.ok(userPreferenceService.update(parseUserId(auth), req));
  }

  private long parseUserId(Authentication auth) {
    try {
      return Long.parseLong(auth.getName());
    } catch (NumberFormatException e) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid principal");
    }
  }
}
