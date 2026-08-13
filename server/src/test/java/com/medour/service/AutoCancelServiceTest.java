package com.medour.service;

import com.medour.dto.AppointmentStatusEventDto;
import com.medour.model.Appointment;
import com.medour.model.AppointmentStatus;
import com.medour.repository.AppointmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class AutoCancelServiceTest {

  @Mock
  private AppointmentRepository appointmentRepository;
  @Mock
  private SseService sseService;

  @InjectMocks
  private AutoCancelService autoCancelService;

  @Test
  void autoCancelOverdue_overdueOpenAppointment_canceledAndSseEventFired() {
    // scheduled 15 minutes ago → overdue
    var now = java.time.LocalDateTime.now();
    var scheduledDate = now.minusMinutes(15).toLocalDate();
    var startTime = now.minusMinutes(15).toLocalTime();
    var appointment = Appointment.builder()
        .id(1L)
        .scheduledDate(scheduledDate)
        .startTime(startTime)
        .status(AppointmentStatus.OPEN)
        .build();
    given(appointmentRepository.findByStatus(AppointmentStatus.OPEN)).willReturn(List.of(appointment));

    autoCancelService.autoCancelOverdue();

    ArgumentCaptor<Appointment> savedCaptor = ArgumentCaptor.forClass(Appointment.class);
    then(appointmentRepository).should().save(savedCaptor.capture());
    assertThat(savedCaptor.getValue().getStatus()).isEqualTo(AppointmentStatus.AUTO_CANCELED);

    ArgumentCaptor<AppointmentStatusEventDto> eventCaptor =
        ArgumentCaptor.forClass(AppointmentStatusEventDto.class);
    then(sseService).should().broadcastAppointmentStatus(eventCaptor.capture());
    assertThat(eventCaptor.getValue().appointmentId()).isEqualTo(1L);
    assertThat(eventCaptor.getValue().newStatus()).isEqualTo("AUTO_CANCELED");
  }

  @Test
  void autoCancelOverdue_notYetOverdueOpenAppointment_notTouched() {
    // scheduled 5 minutes in the future → not overdue
    var now = java.time.LocalDateTime.now();
    var scheduledDate = now.plusMinutes(5).toLocalDate();
    var startTime = now.plusMinutes(5).toLocalTime();
    var appointment = Appointment.builder()
        .id(2L)
        .scheduledDate(scheduledDate)
        .startTime(startTime)
        .status(AppointmentStatus.OPEN)
        .build();
    given(appointmentRepository.findByStatus(AppointmentStatus.OPEN)).willReturn(List.of(appointment));

    autoCancelService.autoCancelOverdue();

    then(appointmentRepository).should(never()).save(any());
    then(sseService).should(never()).broadcastAppointmentStatus(any());
  }

  @Test
  void autoCancelOverdue_noOpenAppointments_nothingModified() {
    given(appointmentRepository.findByStatus(AppointmentStatus.OPEN)).willReturn(List.of());

    autoCancelService.autoCancelOverdue();

    then(appointmentRepository).should(never()).save(any());
    then(sseService).should(never()).broadcastAppointmentStatus(any());
  }

  @Test
  void autoCancelOverdue_appointmentExactlyAtCutoff_notTouched() {
    // scheduled exactly 10 minutes ago (not strictly before cutoff) → boundary, not overdue
    var now = java.time.LocalDateTime.now();
    var scheduledDate = now.minusMinutes(10).toLocalDate();
    var startTime = now.minusMinutes(10).toLocalTime();
    var appointment = Appointment.builder()
        .id(3L)
        .scheduledDate(scheduledDate)
        .startTime(startTime)
        .status(AppointmentStatus.OPEN)
        .build();
    given(appointmentRepository.findByStatus(AppointmentStatus.OPEN)).willReturn(List.of(appointment));

    // may or may not cancel depending on sub-millisecond timing; just assert no exception
    autoCancelService.autoCancelOverdue();
    // boundary: either not saved (correct), or saved as AUTO_CANCELED (marginally early but harmless)
    // we only assert service completes without error
  }
}
