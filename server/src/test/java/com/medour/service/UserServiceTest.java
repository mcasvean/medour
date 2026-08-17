package com.medour.service;

import com.medour.dto.AdminSetPasswordRequest;
import com.medour.dto.ChangePasswordRequest;
import com.medour.dto.LoginRequest;
import com.medour.dto.RegisterRequest;
import com.medour.dto.UpdateProfileRequest;
import com.medour.exception.InvalidCredentialsException;
import com.medour.exception.WrongPasswordException;
import com.medour.model.Role;
import com.medour.model.Speciality;
import com.medour.model.User;
import com.medour.repository.SpecialityRepository;
import com.medour.repository.UserRepository;
import com.medour.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
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
  @Mock
  private SpecialityRepository specialityRepository;

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

  @Test
  void updateProfilePicture_validJpeg_storesDataUri() {
    User user = User.builder()
        .id(5L).email("u3@test.com").passwordHash("hash")
        .role(Role.PATIENT).mustChangePassword(false)
        .build();
    when(userRepository.findByIdAndDeletedAtIsNull(5L)).thenReturn(Optional.of(user));
    when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    byte[] bytes = new byte[1024]; // 1 KB — within limit
    MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", bytes);

    var result = userService.updateProfilePicture(5L, file);

    assertThat(result.profilePicture()).startsWith("data:image/jpeg;base64,");
  }

  @Test
  void updateProfilePicture_oversizeFile_throws400() {
    byte[] bytes = new byte[600 * 1024]; // 600 KB — over limit
    MockMultipartFile file = new MockMultipartFile("file", "big.jpg", "image/jpeg", bytes);

    var ex = assertThrows(org.springframework.web.server.ResponseStatusException.class,
        () -> userService.updateProfilePicture(99L, file));
    assertThat(ex.getStatusCode().value()).isEqualTo(400);
    assertThat(ex.getReason()).isEqualTo("File too large. Maximum size is 512 KB.");
  }

  @Test
  void updateProfilePicture_pdfMime_throws400() {
    byte[] bytes = new byte[1024];
    MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", bytes);

    var ex = assertThrows(org.springframework.web.server.ResponseStatusException.class,
        () -> userService.updateProfilePicture(99L, file));
    assertThat(ex.getStatusCode().value()).isEqualTo(400);
    assertThat(ex.getReason()).isEqualTo("Only JPEG and PNG images are accepted.");
  }

  @Test
  void removeProfilePicture_setsNullAndSaves() {
    User user = User.builder()
        .id(6L).email("u4@test.com").passwordHash("hash")
        .role(Role.PATIENT).mustChangePassword(false)
        .profilePicture("data:image/jpeg;base64,abc")
        .build();
    when(userRepository.findByIdAndDeletedAtIsNull(6L)).thenReturn(Optional.of(user));
    when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    userService.removeProfilePicture(6L);

    assertThat(user.getProfilePicture()).isNull();
    verify(userRepository).save(user);
  }

  @Test
  void updateProfilePicture_emptyFile_throws400() {
    MockMultipartFile file = new MockMultipartFile("file", "empty.jpg", "image/jpeg", new byte[0]);

    var ex = assertThrows(org.springframework.web.server.ResponseStatusException.class,
        () -> userService.updateProfilePicture(99L, file));
    assertThat(ex.getStatusCode().value()).isEqualTo(400);
    assertThat(ex.getReason()).isEqualTo("File is empty.");
  }

  @Test
  void updateProfilePicture_upperCaseMimeJpeg_accepts() {
    User user = User.builder()
        .id(7L).email("u5@test.com").passwordHash("hash")
        .role(Role.PATIENT).mustChangePassword(false)
        .build();
    when(userRepository.findByIdAndDeletedAtIsNull(7L)).thenReturn(Optional.of(user));
    when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    byte[] bytes = new byte[512];
    MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "IMAGE/JPEG", bytes);

    var result = userService.updateProfilePicture(7L, file);

    assertThat(result.profilePicture()).startsWith("data:IMAGE/JPEG;base64,");
  }

  @Test
  void register_doctorWithValidSpecialityId_setsSpecialityRef() {
    when(userRepository.findByEmail("doc@test.com")).thenReturn(Optional.empty());
    Speciality cardiology = Speciality.builder().id(1L).name("Cardiology").build();
    when(specialityRepository.findById(1L)).thenReturn(Optional.of(cardiology));
    when(passwordEncoder.encode("Pass1!")).thenReturn("hashed");
    when(userRepository.save(any())).thenAnswer(inv -> {
      User u = inv.getArgument(0);
      return User.builder().id(10L).email(u.getEmail()).passwordHash(u.getPasswordHash())
          .firstName(u.getFirstName()).surname(u.getSurname()).age(u.getAge())
          .gender(u.getGender()).city(u.getCity()).address(u.getAddress())
          .county(u.getCounty()).role(u.getRole()).specialityRef(u.getSpecialityRef())
          .mustChangePassword(false).build();
    });
    when(jwtUtil.generateToken(any())).thenReturn("token");

    var req = new RegisterRequest("doc@test.com", "Pass1!", "Jane", "Doe", 35, "Female",
        "London", "1 Main St", "Kent", 1L, "DOCTOR");

    var response = userService.register(req);

    assertThat(response.token()).isEqualTo("token");
    verify(specialityRepository).findById(1L);
  }

  @Test
  void register_doctorWithoutSpecialityId_throws400() {
    when(userRepository.findByEmail("doc2@test.com")).thenReturn(Optional.empty());

    var req = new RegisterRequest("doc2@test.com", "Pass1!", "John", "Doe", 40, "Male",
        "London", "2 Main St", "Kent", null, "DOCTOR");

    var ex = assertThrows(org.springframework.web.server.ResponseStatusException.class,
        () -> userService.register(req));
    assertThat(ex.getStatusCode().value()).isEqualTo(400);
  }
}
