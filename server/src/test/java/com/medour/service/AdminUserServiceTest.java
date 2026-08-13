package com.medour.service;

import com.medour.dto.AdminUserDto;
import com.medour.model.Role;
import com.medour.model.User;
import com.medour.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

  @Mock
  private UserRepository userRepository;

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
}
