package com.medour.service;

import com.medour.dto.LoginRequest;
import com.medour.dto.UpdateProfileRequest;
import com.medour.exception.InvalidCredentialsException;
import com.medour.model.Role;
import com.medour.model.User;
import com.medour.repository.UserRepository;
import com.medour.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock
  private UserRepository userRepository;
  @Mock
  private PasswordEncoder passwordEncoder;
  @Mock
  private JwtUtil jwtUtil;

  @InjectMocks
  private UserService userService;

  @Test
  void seedAdmin_doesNotSave_whenAdminAlreadyExists() {
    when(userRepository.existsByRole(Role.ADMIN)).thenReturn(true);

    userService.seedAdmin("admin@medour.com", "Admin1234!");

    verify(userRepository, never()).save(any());
  }

  @Test
  void login_softDeletedUser_throwsInvalidCredentials() {
    User deleted = User.builder()
        .id(1L).email("gone@test.com").passwordHash("hash")
        .role(Role.PATIENT).mustChangePassword(false)
        .deletedAt(LocalDateTime.now())
        .build();
    when(userRepository.findByEmail("gone@test.com")).thenReturn(Optional.of(deleted));

    assertThrows(InvalidCredentialsException.class,
        () -> userService.login(new LoginRequest("gone@test.com", "any")));
  }

  @Test
  void updateProfile_doctorMissingCounty_throwsBadRequest() {
    User doctor = User.builder()
        .id(2L).email("doc@test.com").passwordHash("hash")
        .role(Role.DOCTOR).mustChangePassword(false)
        .build();
    when(userRepository.findByIdAndDeletedAtIsNull(2L)).thenReturn(Optional.of(doctor));

    var req = new UpdateProfileRequest("Doc", "Tor", null, null, null, null, null, null);

    var ex = assertThrows(org.springframework.web.server.ResponseStatusException.class,
        () -> userService.updateProfile(2L, req));
    assertThat(ex.getStatusCode().value()).isEqualTo(400);
  }
}
