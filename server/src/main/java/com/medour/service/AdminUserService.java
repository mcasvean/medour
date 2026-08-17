package com.medour.service;

import com.medour.dto.AdminUserCreateRequest;
import com.medour.dto.AdminUserDto;
import com.medour.dto.AdminUserUpdateRequest;
import com.medour.exception.EmailAlreadyUsedException;
import com.medour.model.Role;
import com.medour.model.Speciality;
import com.medour.model.User;
import com.medour.repository.SpecialityRepository;
import com.medour.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdminUserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final SpecialityRepository specialityRepository;

  public AdminUserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
      SpecialityRepository specialityRepository) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.specialityRepository = specialityRepository;
  }

  @Transactional(readOnly = true)
  public List<AdminUserDto> getAllUsers() {
    return userRepository.findAll().stream()
        .map(this::toDto)
        .toList();
  }

  @Transactional
  public AdminUserDto createUser(AdminUserCreateRequest req) {
    if (userRepository.findByEmail(req.getEmail()).isPresent()) {
      throw new EmailAlreadyUsedException();
    }
    Role role = parseRole(req.getRole());
    Speciality specialityRef = resolveSpeciality(role, req.getSpecialityId());
    User user = User.builder()
        .email(req.getEmail())
        .passwordHash(passwordEncoder.encode(req.getPassword()))
        .firstName(req.getFirstName())
        .surname(req.getSurname())
        .age(req.getAge())
        .gender(req.getGender())
        .city(req.getCity())
        .address(req.getAddress())
        .county(req.getCounty())
        .specialityRef(specialityRef)
        .role(role)
        .mustChangePassword(false)
        .build();
    return toDto(userRepository.save(user));
  }

  @Transactional
  public void deleteUser(Long userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    user.setDeletedAt(LocalDateTime.now());
    userRepository.save(user);
  }

  @Transactional
  public AdminUserDto updateUser(Long userId, AdminUserUpdateRequest req) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    Role role = parseRole(req.getRole());
    Speciality specialityRef = resolveSpeciality(role, req.getSpecialityId());
    user.setFirstName(req.getFirstName());
    user.setSurname(req.getSurname());
    user.setAge(req.getAge());
    user.setGender(req.getGender());
    user.setCity(req.getCity());
    user.setAddress(req.getAddress());
    user.setCounty(req.getCounty());
    user.setSpecialityRef(specialityRef);
    user.setRole(role);
    return toDto(userRepository.save(user));
  }

  private Role parseRole(String roleStr) {
    try {
      return Role.valueOf(roleStr);
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid role");
    }
  }

  private Speciality resolveSpeciality(Role role, Long specialityId) {
    if (role == Role.DOCTOR) {
      if (specialityId == null) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
            "Speciality is required for doctor registration.");
      }
      return specialityRepository.findById(specialityId)
          .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
              "Selected speciality does not exist."));
    }
    return specialityId != null
        ? specialityRepository.findById(specialityId).orElse(null)
        : null;
  }

  private void validateDoctorFields(Role role, String county, String speciality) {
    if (role == Role.DOCTOR) {
      if (county == null || county.isBlank() || speciality == null || speciality.isBlank()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
            "county and speciality are required for DOCTOR");
      }
    }
  }

  private AdminUserDto toDto(User u) {
    Speciality ref = u.getSpecialityRef();
    return new AdminUserDto(
        u.getId(),
        u.getEmail(),
        u.getFirstName(),
        u.getSurname(),
        u.getRole().name(),
        ref != null ? ref.getId() : null,
        ref != null ? ref.getName() : null,
        u.getCounty(),
        u.getCity(),
        u.getAge(),
        u.getGender(),
        u.getAddress(),
        u.getMustChangePassword(),
        u.getDeletedAt() != null);
  }
}
