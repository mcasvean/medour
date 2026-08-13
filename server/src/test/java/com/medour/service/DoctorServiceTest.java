package com.medour.service;

import com.medour.dto.DoctorSearchResult;
import com.medour.model.AppointmentStatus;
import com.medour.model.Role;
import com.medour.model.User;
import com.medour.repository.AppointmentRepository;
import com.medour.repository.SlotReservationRepository;
import com.medour.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DoctorServiceTest {

  @Mock
  private UserRepository userRepository;
  @Mock
  private AppointmentRepository appointmentRepository;
  @Mock
  private SlotReservationRepository slotReservationRepository;

  @InjectMocks
  private DoctorService doctorService;

  @Test
  void searchDoctors_noFilter_returnsOnlyActiveDoctors() {
    User doctor = activeDoctor(1L, "Cardiology");
    User patient = User.builder().id(2L).role(Role.PATIENT).build();
    User deletedDoctor = User.builder().id(3L).role(Role.DOCTOR).deletedAt(LocalDateTime.now()).build();
    when(userRepository.findAll()).thenReturn(List.of(doctor, patient, deletedDoctor));

    List<DoctorSearchResult> results = doctorService.searchDoctors(null, null, null, null);

    assertThat(results).hasSize(1);
    assertThat(results.get(0).id()).isEqualTo(1L);
  }

  @Test
  void searchDoctors_specialityFilter_returnsMatchingDoctors() {
    User cardio = activeDoctor(1L, "Cardiology");
    User neuro = activeDoctor(2L, "Neurology");
    when(userRepository.findAll()).thenReturn(List.of(cardio, neuro));

    List<DoctorSearchResult> results = doctorService.searchDoctors("cardio", null, null, null);

    assertThat(results).hasSize(1);
    assertThat(results.get(0).speciality()).isEqualTo("Cardiology");
  }

  @Test
  void searchDoctors_dateFilter_excludesFullyBookedDoctor() {
    User busy = activeDoctor(1L, "Cardiology");
    User free = activeDoctor(2L, "Neurology");
    LocalDate date = LocalDate.of(2026, 9, 1);
    when(userRepository.findAll()).thenReturn(List.of(busy, free));
    // busy doctor has all 24 slots taken
    when(slotReservationRepository.countByDoctorIdAndDateAndExpiresAtAfter(eq(1L), eq(date), any())).thenReturn(20L);
    when(appointmentRepository.countByDoctorIdAndScheduledDateAndStatusIn(eq(1L), eq(date), any())).thenReturn(4L);
    // free doctor has 0 slots taken
    when(slotReservationRepository.countByDoctorIdAndDateAndExpiresAtAfter(eq(2L), eq(date), any())).thenReturn(0L);
    when(appointmentRepository.countByDoctorIdAndScheduledDateAndStatusIn(eq(2L), eq(date), any())).thenReturn(0L);

    List<DoctorSearchResult> results = doctorService.searchDoctors(null, null, null, date);

    assertThat(results).hasSize(1);
    assertThat(results.get(0).id()).isEqualTo(2L);
  }

  private User activeDoctor(Long id, String speciality) {
    return User.builder()
        .id(id).role(Role.DOCTOR).speciality(speciality)
        .firstName("Doc").surname("Tor")
        .build();
  }
}
