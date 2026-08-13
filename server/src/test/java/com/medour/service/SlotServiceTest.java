package com.medour.service;

import com.medour.dto.SlotDto;
import com.medour.model.AppointmentStatus;
import com.medour.model.SlotState;
import com.medour.repository.AppointmentRepository;
import com.medour.repository.SlotReservationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SlotServiceTest {

  @Mock
  private SlotReservationRepository slotReservationRepository;
  @Mock
  private AppointmentRepository appointmentRepository;

  @InjectMocks
  private SlotService slotService;

  private static final LocalDate DATE = LocalDate.of(2026, 9, 1);
  private static final Long DOCTOR_ID = 1L;

  @Test
  void allSlotsFree_returns24Available() {
    when(slotReservationRepository.existsByDoctorIdAndDateAndStartTimeAndExpiresAtAfter(
        eq(DOCTOR_ID), eq(DATE), any(LocalTime.class), any())).thenReturn(false);
    when(appointmentRepository.existsByDoctorIdAndScheduledDateAndStartTimeAndStatus(
        eq(DOCTOR_ID), eq(DATE), any(LocalTime.class), eq(AppointmentStatus.OPEN))).thenReturn(false);

    List<SlotDto> slots = slotService.getSlotsForDoctor(DOCTOR_ID, DATE);

    assertThat(slots).hasSize(24);
    assertThat(slots).allMatch(s -> s.state() == SlotState.AVAILABLE);
  }

  @Test
  void activeReservationForSlot_returnsLocked() {
    LocalTime lockedSlot = LocalTime.of(10, 0);
    when(slotReservationRepository.existsByDoctorIdAndDateAndStartTimeAndExpiresAtAfter(
        eq(DOCTOR_ID), eq(DATE), any(LocalTime.class), any())).thenReturn(false);
    when(slotReservationRepository.existsByDoctorIdAndDateAndStartTimeAndExpiresAtAfter(
        eq(DOCTOR_ID), eq(DATE), eq(lockedSlot), any())).thenReturn(true);
    when(appointmentRepository.existsByDoctorIdAndScheduledDateAndStartTimeAndStatus(
        eq(DOCTOR_ID), eq(DATE), any(LocalTime.class), eq(AppointmentStatus.OPEN))).thenReturn(false);

    List<SlotDto> slots = slotService.getSlotsForDoctor(DOCTOR_ID, DATE);

    SlotDto locked = slots.stream()
        .filter(s -> s.startTime().equals("10:00")).findFirst().orElseThrow();
    assertThat(locked.state()).isEqualTo(SlotState.LOCKED);
    assertThat(slots.stream().filter(s -> !s.startTime().equals("10:00")))
        .allMatch(s -> s.state() == SlotState.AVAILABLE);
  }

  @Test
  void openAppointmentForSlot_returnsUnavailable() {
    LocalTime takenSlot = LocalTime.of(14, 0);
    when(slotReservationRepository.existsByDoctorIdAndDateAndStartTimeAndExpiresAtAfter(
        eq(DOCTOR_ID), eq(DATE), any(LocalTime.class), any())).thenReturn(false);
    when(appointmentRepository.existsByDoctorIdAndScheduledDateAndStartTimeAndStatus(
        eq(DOCTOR_ID), eq(DATE), any(LocalTime.class), eq(AppointmentStatus.OPEN))).thenReturn(false);
    when(appointmentRepository.existsByDoctorIdAndScheduledDateAndStartTimeAndStatus(
        eq(DOCTOR_ID), eq(DATE), eq(takenSlot), eq(AppointmentStatus.OPEN))).thenReturn(true);

    List<SlotDto> slots = slotService.getSlotsForDoctor(DOCTOR_ID, DATE);

    SlotDto unavailable = slots.stream()
        .filter(s -> s.startTime().equals("14:00")).findFirst().orElseThrow();
    assertThat(unavailable.state()).isEqualTo(SlotState.UNAVAILABLE);
    assertThat(slots.stream().filter(s -> !s.startTime().equals("14:00")))
        .allMatch(s -> s.state() == SlotState.AVAILABLE);
  }

  @Test
  void expiredReservation_reservationExistReturnsFalse_returnsAvailable() {
    // expired reservation: existsByDoctorIdAndDateAndStartTimeAndExpiresAtAfter returns false
    // because the expiresAt is before now — the repo method won't match, so it returns false
    when(slotReservationRepository.existsByDoctorIdAndDateAndStartTimeAndExpiresAtAfter(
        eq(DOCTOR_ID), eq(DATE), any(LocalTime.class), any())).thenReturn(false);
    when(appointmentRepository.existsByDoctorIdAndScheduledDateAndStartTimeAndStatus(
        eq(DOCTOR_ID), eq(DATE), any(LocalTime.class), eq(AppointmentStatus.OPEN))).thenReturn(false);

    List<SlotDto> slots = slotService.getSlotsForDoctor(DOCTOR_ID, DATE);

    SlotDto first = slots.stream()
        .filter(s -> s.startTime().equals("08:00")).findFirst().orElseThrow();
    assertThat(first.state()).isEqualTo(SlotState.AVAILABLE);
  }
}
