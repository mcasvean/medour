package com.medour.service;

import com.medour.dto.AdminSetPasswordRequest;
import com.medour.dto.ChangePasswordRequest;
import com.medour.dto.LoginRequest;
import com.medour.dto.UpdateProfileRequest;
import com.medour.exception.InvalidCredentialsException;
import com.medour.exception.WrongPasswordException;
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

  @Test
  void changePassword_wrongCurrentPassword_throwsWrongPassword() {
    User user = User.builder()
        .id(3L).email("u@test.com").passwordHash("hashed")
        .role(Role.PATIENT).mustChangePassword(false)
        .build();
    when(userRepository.findByIdAndDeletedAtIsNull(3L)).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("wrongPass", "hashed")).thenReturn(false);

    assertThrows(WrongPasswordException.class,
        () -> userService.changePassword(3L, new ChangePasswordRequest("wrongPass", "newPass")));
  }

  @Test
  void changePassword_success_clearsMustChangePassword() {
    User user = User.builder()
        .id(3L).email("u@test.com").passwordHash("hashed")
        .role(Role.PATIENT).mustChangePassword(true)
        .build();
    when(userRepository.findByIdAndDeletedAtIsNull(3L)).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("correct", "hashed")).thenReturn(true);
    when(passwordEncoder.encode("newPass")).thenReturn("newHashed");
    when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    userService.changePassword(3L, new ChangePasswordRequest("correct", "newPass"));

    assertThat(user.getMustChangePassword()).isFalse();
  }

  @Test
  void adminSetPassword_setsMustChangePasswordTrue() {
    User user = User.builder()
        .id(4L).email("u2@test.com").passwordHash("hashed")
        .role(Role.PATIENT).mustChangePassword(false)
        .build();
    when(userRepository.findByIdAndDeletedAtIsNull(4L)).thenReturn(Optional.of(user));
    when(passwordEncoder.encode("TempPass123!")).thenReturn("newHashed");
    when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    userService.adminSetPassword(4L, new AdminSetPasswordRequest("TempPass123!"));

    verify(userRepository).save(any(User.class));
    assertThat(user.getMustChangePassword()).isTrue();
  }
}
