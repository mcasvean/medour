package com.medour.service;

import com.medour.dto.AppointmentStatusEventDto;
import com.medour.dto.DoctorAppointmentDto;
import com.medour.model.Appointment;
import com.medour.model.AppointmentStatus;
import com.medour.model.User;
import com.medour.repository.AppointmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DoctorAppointmentServiceTest {

  @Mock
  private AppointmentRepository appointmentRepository;

  @Mock
  private SseService sseService;

  @InjectMocks
  private DoctorAppointmentService doctorAppointmentService;

  @Test
  void getAppointments_activePatient_patientRemovedFalse() {
    User patient = User.builder().id(5L).firstName("Jane").surname("Doe").deletedAt(null).build();
    User doctor = User.builder().id(2L).build();
    Appointment appt = Appointment.builder()
        .id(1L).patient(patient).doctor(doctor)
        .scheduledDate(LocalDate.of(2026, 9, 1))
        .startTime(LocalTime.of(10, 0))
        .status(AppointmentStatus.OPEN)
        .wherebyRoomUrl("https://whereby.com/room")
        .build();
    when(appointmentRepository.findByDoctorIdOrderByScheduledDateAsc(2L)).thenReturn(List.of(appt));

    List<DoctorAppointmentDto> result = doctorAppointmentService.getAppointments(2L);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).patientRemoved()).isFalse();
    assertThat(result.get(0).patientFirstName()).isEqualTo("Jane");
    assertThat(result.get(0).status()).isEqualTo("OPEN");
  }

  @Test
  void getAppointments_deletedPatient_patientRemovedTrue() {
    User patient = User.builder().id(5L).firstName("Jane").surname("Doe")
        .deletedAt(LocalDateTime.now()).build();
    User doctor = User.builder().id(2L).build();
    Appointment appt = Appointment.builder()
        .id(2L).patient(patient).doctor(doctor)
        .scheduledDate(LocalDate.of(2026, 9, 1))
        .startTime(LocalTime.of(10, 0))
        .status(AppointmentStatus.CANCELED)
        .createdAt(LocalDateTime.of(2026, 8, 1, 12, 0))
        .wherebyRoomUrl(null)
        .build();
    when(appointmentRepository.findByDoctorIdOrderByScheduledDateAsc(2L)).thenReturn(List.of(appt));

    List<DoctorAppointmentDto> result = doctorAppointmentService.getAppointments(2L);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).patientRemoved()).isTrue();
  }

  @Test
  void updateStatus_cancelOpenAppointment_statusCanceledAndSseFired() {
    User doctor = User.builder().id(2L).build();
    Appointment appt = Appointment.builder()
        .id(10L).doctor(doctor).status(AppointmentStatus.OPEN).build();
    when(appointmentRepository.findById(10L)).thenReturn(Optional.of(appt));
    when(appointmentRepository.save(any())).thenReturn(appt);

    doctorAppointmentService.updateStatus(10L, 2L, "CANCELED");

    assertThat(appt.getStatus()).isEqualTo(AppointmentStatus.CANCELED);
    ArgumentCaptor<AppointmentStatusEventDto> captor = ArgumentCaptor.forClass(AppointmentStatusEventDto.class);
    verify(sseService).broadcastAppointmentStatus(captor.capture());
    assertThat(captor.getValue().appointmentId()).isEqualTo(10L);
    assertThat(captor.getValue().newStatus()).isEqualTo("CANCELED");
  }

  @Test
  void updateStatus_wrongDoctor_throws403() {
    User owner = User.builder().id(2L).build();
    Appointment appt = Appointment.builder()
        .id(10L).doctor(owner).status(AppointmentStatus.OPEN).build();
    when(appointmentRepository.findById(10L)).thenReturn(Optional.of(appt));

    assertThatThrownBy(() -> doctorAppointmentService.updateStatus(10L, 99L, "CANCELED"))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
            .isEqualTo(HttpStatus.FORBIDDEN));
  }

  @Test
  void updateStatus_nonOpenAppointment_throws409() {
    User doctor = User.builder().id(2L).build();
    Appointment appt = Appointment.builder()
        .id(10L).doctor(doctor).status(AppointmentStatus.CANCELED).build();
    when(appointmentRepository.findById(10L)).thenReturn(Optional.of(appt));

    assertThatThrownBy(() -> doctorAppointmentService.updateStatus(10L, 2L, "COMPLETED"))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(e -> {
          ResponseStatusException ex = (ResponseStatusException) e;
          assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
          assertThat(ex.getReason()).isEqualTo("Appointment is not in OPEN status");
        });
  }
}
