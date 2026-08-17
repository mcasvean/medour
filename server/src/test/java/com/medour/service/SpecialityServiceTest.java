package com.medour.service;

import com.medour.dto.SpecialityResponse;
import com.medour.model.Speciality;
import com.medour.repository.SpecialityRepository;
import com.medour.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpecialityServiceTest {

  @Mock
  private SpecialityRepository specialityRepository;
  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private SpecialityService specialityService;

  @Test
  void create_valid_savesAndReturnsResponse() {
    when(specialityRepository.findByNameIgnoreCase("Cardiology")).thenReturn(Optional.empty());
    Speciality saved = Speciality.builder().id(1L).name("Cardiology").build();
    when(specialityRepository.save(any())).thenReturn(saved);

    SpecialityResponse result = specialityService.create("Cardiology");

    assertThat(result.id()).isEqualTo(1L);
    assertThat(result.name()).isEqualTo("Cardiology");
  }

  @Test
  void create_duplicate_throwsConflict() {
    Speciality existing = Speciality.builder().id(1L).name("Cardiology").build();
    when(specialityRepository.findByNameIgnoreCase("cardiology")).thenReturn(Optional.of(existing));

    assertThrows(ResponseStatusException.class, () -> specialityService.create("cardiology"));
    verify(specialityRepository, never()).save(any());
  }

  @Test
  void delete_unused_deletesSuccessfully() {
    Speciality speciality = Speciality.builder().id(1L).name("Cardiology").build();
    when(specialityRepository.findById(1L)).thenReturn(Optional.of(speciality));
    when(userRepository.existsBySpecialityIgnoreCaseAndDeletedAtIsNull("Cardiology")).thenReturn(false);

    specialityService.delete(1L);

    verify(specialityRepository).delete(speciality);
  }

  @Test
  void delete_inUse_throwsConflict() {
    Speciality speciality = Speciality.builder().id(1L).name("Cardiology").build();
    when(specialityRepository.findById(1L)).thenReturn(Optional.of(speciality));
    when(userRepository.existsBySpecialityIgnoreCaseAndDeletedAtIsNull("Cardiology")).thenReturn(true);

    assertThrows(ResponseStatusException.class, () -> specialityService.delete(1L));
    verify(specialityRepository, never()).delete(any());
  }

  @Test
  void update_valid_savesAndReturnsUpdatedResponse() {
    Speciality speciality = Speciality.builder().id(1L).name("Cardiology").build();
    when(specialityRepository.findById(1L)).thenReturn(Optional.of(speciality));
    when(specialityRepository.findByNameIgnoreCase("Cardiologie")).thenReturn(Optional.empty());
    Speciality updated = Speciality.builder().id(1L).name("Cardiologie").build();
    when(specialityRepository.save(any())).thenReturn(updated);

    SpecialityResponse result = specialityService.update(1L, "Cardiologie");

    assertThat(result.name()).isEqualTo("Cardiologie");
  }

  @Test
  void findAll_returnsListSortedByName() {
    List<Speciality> sorted = List.of(
        Speciality.builder().id(2L).name("Cardiology").build(),
        Speciality.builder().id(1L).name("Neurology").build());
    when(specialityRepository.findAllByOrderByNameAsc()).thenReturn(sorted);

    List<SpecialityResponse> result = specialityService.findAll();

    assertThat(result).extracting(SpecialityResponse::name)
        .containsExactly("Cardiology", "Neurology");
  }
}
