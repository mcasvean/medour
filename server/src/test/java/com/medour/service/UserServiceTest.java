package com.medour.service;

import com.medour.model.Role;
import com.medour.repository.UserRepository;
import com.medour.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

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
}
