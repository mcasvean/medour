package com.medour.service;

import com.medour.dto.AdminUserCreateRequest;
import com.medour.dto.AdminUserDto;
import com.medour.dto.AdminUserUpdateRequest;
import com.medour.exception.EmailAlreadyUsedException;
import com.medour.model.Role;
import com.medour.model.User;
import com.medour.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private PasswordEncoder passwordEncoder;

  @InjectMocks
  private AdminUserService adminUserService;

  @Test
  void getAllUsers_activeUser_isDeletedFalse() {
    User active = User.builder()
        .id(1L).email("active@test.com").passwordHash("hash")
        .firstName("Alice").surname("Smith")
        .role(Role.PATIENT).mustChangePassword(false)
        .deletedAt(null)
        .build();
    when(userRepository.findAll()).thenReturn(List.of(active));

    List<AdminUserDto> result = adminUserService.getAllUsers();

    assertThat(result).hasSize(1);
    assertThat(result.get(0).isDeleted()).isFalse();
  }

  @Test
  void getAllUsers_softDeletedUser_isDeletedTrue() {
    User deleted = User.builder()
        .id(2L).email("gone@test.com").passwordHash("hash")
        .firstName("Bob").surname("Jones")
        .role(Role.DOCTOR).mustChangePassword(false)
        .deletedAt(LocalDateTime.now())
        .build();
    when(userRepository.findAll()).thenReturn(List.of(deleted));

    List<AdminUserDto> result = adminUserService.getAllUsers();

    assertThat(result).hasSize(1);
    assertThat(result.get(0).isDeleted()).isTrue();
  }

  @Test
  void createUser_validPatient_savedAndReturnsDto() {
    var req = new AdminUserCreateRequest("p@test.com", "Pass1!", "Pat", "Ient",
        null, null, null, null, null, null, "PATIENT");
    when(userRepository.findByEmail("p@test.com")).thenReturn(Optional.empty());
    when(passwordEncoder.encode("Pass1!")).thenReturn("hashed");
    User saved = User.builder().id(5L).email("p@test.com").passwordHash("hashed")
        .firstName("Pat").surname("Ient").role(Role.PATIENT).mustChangePassword(false).build();
    when(userRepository.save(any(User.class))).thenReturn(saved);

    AdminUserDto result = adminUserService.createUser(req);

    assertThat(result.email()).isEqualTo("p@test.com");
    assertThat(result.role()).isEqualTo("PATIENT");
    assertThat(result.id()).isEqualTo(5L);
  }

  @Test
  void createUser_doctorMissingCounty_throwsBadRequest() {
    var req = new AdminUserCreateRequest("doc@test.com", "Pass1!", "Doc", "Tor",
        null, null, null, null, null, "Cardiology", "DOCTOR");
    when(userRepository.findByEmail("doc@test.com")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> adminUserService.createUser(req))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("county and speciality are required for DOCTOR");
  }

  @Test
  void updateUser_validRoleChange_returnsUpdatedDto() {
    User existing = User.builder()
        .id(3L).email("u@test.com").passwordHash("hash")
        .firstName("Old").surname("Name").role(Role.PATIENT).mustChangePassword(false)
        .build();
    when(userRepository.findById(3L)).thenReturn(Optional.of(existing));
    when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

    var req = new AdminUserUpdateRequest("Old", "Name", null, null, null, null, null, null, "ADMIN");
    AdminUserDto result = adminUserService.updateUser(3L, req);

    assertThat(result.role()).isEqualTo("ADMIN");
    assertThat(result.email()).isEqualTo("u@test.com");
  }

  @Test
  void createUser_duplicateEmail_throwsEmailAlreadyUsedException() {
    var req = new AdminUserCreateRequest("dup@test.com", "Pass1!", "Dup", "User",
        null, null, null, null, null, null, "PATIENT");
    when(userRepository.findByEmail("dup@test.com"))
        .thenReturn(Optional.of(User.builder().id(1L).build()));

    assertThatThrownBy(() -> adminUserService.createUser(req))
        .isInstanceOf(EmailAlreadyUsedException.class);
  }

  @Test
  void deleteUser_activeUser_deletedAtBecomesNonNull() {
    User user = User.builder()
        .id(10L).email("del@test.com").passwordHash("hash")
        .firstName("Del").surname("User").role(Role.PATIENT).mustChangePassword(false)
        .deletedAt(null)
        .build();
    when(userRepository.findById(10L)).thenReturn(Optional.of(user));
    when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

    adminUserService.deleteUser(10L);

    assertThat(user.getDeletedAt()).isNotNull();
    verify(userRepository).save(user);
  }

  @Test
  void deleteUser_nonExistentId_throws404() {
    when(userRepository.findById(999L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> adminUserService.deleteUser(999L))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode().value()).isEqualTo(404));
  }

  @Test
  void updateUser_nonExistentId_throws404() {
    when(userRepository.findById(999L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> adminUserService.updateUser(999L,
        new AdminUserUpdateRequest("F", "S", null, null, null, null, null, null, "PATIENT")))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode().value()).isEqualTo(404));
  }
}
