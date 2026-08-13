package com.medour.service;

import com.medour.dto.ReserveSlotRequest;
import com.medour.exception.SlotAlreadyReservedException;
import com.medour.model.SlotReservation;
import com.medour.model.User;
import com.medour.repository.AppointmentRepository;
import com.medour.repository.SlotReservationRepository;
import com.medour.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

  @Mock
  private SlotReservationRepository slotReservationRepository;
  @Mock
  private AppointmentRepository appointmentRepository;
  @Mock
  private UserRepository userRepository;
  @Mock
  private SseService sseService;

  @InjectMocks
  private AppointmentService appointmentService;

  @Test
  void reserveSlot_duplicateSlot_throwsSlotAlreadyReservedException() {
    var patient = User.builder().id(1L).build();
    var doctor = User.builder().id(10L).build();
    var req = new ReserveSlotRequest(10L, LocalDate.of(2026, 9, 1), LocalTime.of(10, 0));
    given(userRepository.findById(1L)).willReturn(Optional.of(patient));
    given(userRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(doctor));
    given(slotReservationRepository.save(any()))
        .willThrow(new DataIntegrityViolationException("unique constraint"));

    assertThrows(SlotAlreadyReservedException.class,
        () -> appointmentService.reserveSlot(1L, req));
  }

  @Test
  void cancelReservation_wrongPatient_throwsForbidden() {
    var otherPatient = User.builder().id(99L).build();
    var reservation = SlotReservation.builder()
        .id(5L)
        .reservedByPatient(otherPatient)
        .build();
    given(slotReservationRepository.findById(5L)).willReturn(Optional.of(reservation));

    var ex = assertThrows(ResponseStatusException.class,
        () -> appointmentService.cancelReservation(1L, 5L));
    assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }
}
