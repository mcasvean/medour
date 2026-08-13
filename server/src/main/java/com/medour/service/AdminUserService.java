package com.medour.service;

import com.medour.dto.AdminUserDto;
import com.medour.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminUserService {

  private final UserRepository userRepository;

  public AdminUserService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Transactional(readOnly = true)
  public List<AdminUserDto> getAllUsers() {
    return userRepository.findAll().stream()
        .map(u -> new AdminUserDto(
            u.getId(),
            u.getEmail(),
            u.getFirstName(),
            u.getSurname(),
            u.getRole().name(),
            u.getSpeciality(),
            u.getCounty(),
            u.getCity(),
            u.getAge(),
            u.getGender(),
            u.getAddress(),
            u.getMustChangePassword(),
            u.getDeletedAt() != null
        ))
        .toList();
  }
}
