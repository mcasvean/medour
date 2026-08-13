package com.medour.service;

import com.medour.dto.PatientAppointmentDto;
import com.medour.model.Appointment;
import com.medour.model.AppointmentStatus;
import com.medour.model.User;
import com.medour.repository.AppointmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatientAppointmentServiceTest {

  @Mock
  private AppointmentRepository appointmentRepository;

  @InjectMocks
  private PatientAppointmentService patientAppointmentService;

  @Test
  void getHistory_activeDoctor_doctorRemovedFalse() {
    User doctor = User.builder().id(10L).firstName("Doc").surname("Tor")
        .speciality("Cardiology").deletedAt(null).build();
    User patient = User.builder().id(1L).build();
    Appointment appt = Appointment.builder()
        .id(1L).patient(patient).doctor(doctor)
        .scheduledDate(LocalDate.of(2026, 9, 1))
        .startTime(LocalTime.of(10, 0))
        .status(AppointmentStatus.OPEN)
        .wherebyRoomUrl("https://whereby.com/test-room")
        .build();
    when(appointmentRepository.findByPatientIdOrderByScheduledDateDesc(1L))
        .thenReturn(List.of(appt));

    List<PatientAppointmentDto> result = patientAppointmentService.getHistory(1L);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).doctorRemoved()).isFalse();
    assertThat(result.get(0).status()).isEqualTo("OPEN");
    assertThat(result.get(0).wherebyRoomUrl()).isEqualTo("https://whereby.com/test-room");
  }

  @Test
  void getHistory_deletedDoctor_doctorRemovedTrue() {
    User doctor = User.builder().id(10L).firstName("Doc").surname("Tor")
        .speciality("Cardiology").deletedAt(LocalDateTime.now()).build();
    User patient = User.builder().id(1L).build();
    Appointment appt = Appointment.builder()
        .id(2L).patient(patient).doctor(doctor)
        .scheduledDate(LocalDate.of(2026, 9, 1))
        .startTime(LocalTime.of(10, 0))
        .status(AppointmentStatus.CANCELED)
        .wherebyRoomUrl("https://whereby.com/test-room")
        .build();
    when(appointmentRepository.findByPatientIdOrderByScheduledDateDesc(1L))
        .thenReturn(List.of(appt));

    List<PatientAppointmentDto> result = patientAppointmentService.getHistory(1L);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).doctorRemoved()).isTrue();
  }

  @Test
  void getHistory_nullWherebyRoomUrl_mapsToNull() {
    User doctor = User.builder().id(10L).firstName("Doc").surname("Tor")
        .speciality("Cardiology").deletedAt(null).build();
    User patient = User.builder().id(1L).build();
    Appointment appt = Appointment.builder()
        .id(3L).patient(patient).doctor(doctor)
        .scheduledDate(LocalDate.of(2026, 9, 1))
        .startTime(LocalTime.of(10, 0))
        .status(AppointmentStatus.OPEN)
        .wherebyRoomUrl(null)
        .build();
    when(appointmentRepository.findByPatientIdOrderByScheduledDateDesc(1L))
        .thenReturn(List.of(appt));

    List<PatientAppointmentDto> result = patientAppointmentService.getHistory(1L);

    assertThat(result.get(0).wherebyRoomUrl()).isNull();
  }
}
