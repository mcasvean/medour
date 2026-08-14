package com.medour.service;

import com.medour.dto.AdminSetPasswordRequest;
import com.medour.dto.AuthResponse;
import com.medour.dto.ChangePasswordRequest;
import com.medour.dto.LoginRequest;
import com.medour.dto.RegisterRequest;
import com.medour.dto.UpdateProfileRequest;
import com.medour.dto.UserProfileResponse;
import com.medour.exception.EmailAlreadyUsedException;
import com.medour.exception.InvalidCredentialsException;
import com.medour.exception.WrongPasswordException;
import com.medour.model.Role;
import com.medour.model.User;
import com.medour.repository.UserRepository;
import com.medour.security.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Base64;
import java.util.List;

@Service
public class UserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtUtil jwtUtil;

  public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtUtil = jwtUtil;
  }

  @Transactional
  public AuthResponse register(RegisterRequest req) {
    if (userRepository.findByEmail(req.getEmail()).isPresent()) {
      throw new EmailAlreadyUsedException();
    }

    Role role;
    try {
      role = Role.valueOf(req.getRole());
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid role");
    }
    if (role == Role.ADMIN) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot self-register as ADMIN");
    }

    if (role == Role.DOCTOR) {
      if (req.getCounty() == null || req.getCounty().isBlank()
          || req.getSpeciality() == null || req.getSpeciality().isBlank()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
            "county and speciality are required for DOCTOR");
      }
    }

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
        .speciality(req.getSpeciality())
        .role(role)
        .mustChangePassword(false)
        .build();

    User saved = userRepository.save(user);
    String token = jwtUtil.generateToken(saved);

    return new AuthResponse(token, saved.getId(), saved.getEmail(),
        saved.getFirstName(), saved.getSurname(), saved.getRole().name(), saved.getMustChangePassword(),
        saved.getProfilePicture());
  }

  @Transactional(readOnly = true)
  public AuthResponse login(LoginRequest req) {
    User user = userRepository.findByEmail(req.getEmail())
        .orElseThrow(InvalidCredentialsException::new);
    if (user.getDeletedAt() != null) {
      throw new InvalidCredentialsException();
    }
    if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
      throw new InvalidCredentialsException();
    }
    String token = jwtUtil.generateToken(user);
    return new AuthResponse(token, user.getId(), user.getEmail(),
        user.getFirstName(), user.getSurname(), user.getRole().name(), user.getMustChangePassword(),
        user.getProfilePicture());
  }

  @Transactional(readOnly = true)
  public UserProfileResponse getProfile(Long userId) {
    User user = userRepository.findByIdAndDeletedAtIsNull(userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    return toProfileResponse(user);
  }

  @Transactional
  public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest req) {
    User user = userRepository.findByIdAndDeletedAtIsNull(userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    if (user.getRole() == Role.DOCTOR) {
      if (req.getCounty() == null || req.getCounty().isBlank()
          || req.getSpeciality() == null || req.getSpeciality().isBlank()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
            "county and speciality are required for DOCTOR");
      }
    }
    user.setFirstName(req.getFirstName());
    user.setSurname(req.getSurname());
    user.setAge(req.getAge());
    user.setGender(req.getGender());
    user.setCity(req.getCity());
    user.setAddress(req.getAddress());
    user.setCounty(req.getCounty());
    user.setSpeciality(req.getSpeciality());
    return toProfileResponse(userRepository.save(user));
  }

  private UserProfileResponse toProfileResponse(User user) {
    return new UserProfileResponse(
        user.getId(),
        user.getEmail(),
        user.getFirstName(),
        user.getSurname(),
        user.getRole().name(),
        user.getAge(),
        user.getGender(),
        user.getCity(),
        user.getAddress(),
        user.getCounty(),
        user.getSpeciality(),
        user.getProfilePicture());
  }

  @Transactional
  public UserProfileResponse updateProfilePicture(Long userId, MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty.");
    }
    if (file.getSize() > 512 * 1024) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File too large. Maximum size is 512 KB.");
    }
    String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
    if (!List.of("image/jpeg", "image/png").contains(contentType)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only JPEG and PNG images are accepted.");
    }
    User user = userRepository.findByIdAndDeletedAtIsNull(userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    try {
      String encoded = Base64.getEncoder().encodeToString(file.getBytes());
      user.setProfilePicture("data:" + file.getContentType() + ";base64," + encoded);
    } catch (IOException e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read file");
    }
    return toProfileResponse(userRepository.save(user));
  }

  @Transactional
  public void removeProfilePicture(Long userId) {
    User user = userRepository.findByIdAndDeletedAtIsNull(userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    user.setProfilePicture(null);
    userRepository.save(user);
  }

  @Transactional
  public void changePassword(Long userId, ChangePasswordRequest req) {
    User user = userRepository.findByIdAndDeletedAtIsNull(userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    if (!passwordEncoder.matches(req.getCurrentPassword(), user.getPasswordHash())) {
      throw new WrongPasswordException();
    }
    user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
    user.setMustChangePassword(false);
    userRepository.save(user);
  }

  @Transactional
  public void adminSetPassword(Long targetId, AdminSetPasswordRequest req) {
    User user = userRepository.findByIdAndDeletedAtIsNull(targetId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
    user.setMustChangePassword(true);
    userRepository.save(user);
  }

  @Transactional
  public void seedAdmin(String email, String rawPassword) {
    if (userRepository.existsByRole(Role.ADMIN)) {
      return;
    }
    User admin = User.builder()
        .email(email)
        .passwordHash(passwordEncoder.encode(rawPassword))
        .firstName("Admin")
        .surname("User")
        .role(Role.ADMIN)
        .mustChangePassword(false)
        .build();
    userRepository.save(admin);
  }
}
