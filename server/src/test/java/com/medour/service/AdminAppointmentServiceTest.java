package com.medour.service;

import com.medour.dto.AdminAppointmentDto;
import com.medour.model.Appointment;
import com.medour.model.AppointmentStatus;
import com.medour.model.User;
import com.medour.repository.AppointmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAppointmentServiceTest {

  @Mock
  private AppointmentRepository appointmentRepository;

  @InjectMocks
  private AdminAppointmentService adminAppointmentService;

  @Test
  void getAllAppointments_returnsMappedDtos() {
    User patient = User.builder().firstName("Alice").surname("Jones").build();
    User doctor = User.builder().firstName("Dr").surname("Smith").build();
    Appointment appt = Appointment.builder()
        .id(1L)
        .patient(patient)
        .doctor(doctor)
        .scheduledDate(LocalDate.of(2026, 9, 1))
        .startTime(LocalTime.of(10, 0))
        .status(AppointmentStatus.OPEN)
        .wherebyRoomUrl(null)
        .build();
    when(appointmentRepository.findAll(Sort.by(Sort.Direction.DESC, "scheduledDate", "startTime")))
        .thenReturn(List.of(appt));

    List<AdminAppointmentDto> result = adminAppointmentService.getAllAppointments();

    assertThat(result).hasSize(1);
    assertThat(result.get(0).patientName()).isEqualTo("Alice Jones");
    assertThat(result.get(0).doctorName()).isEqualTo("Dr Smith");
    assertThat(result.get(0).status()).isEqualTo("OPEN");
    verify(appointmentRepository).findAll(Sort.by(Sort.Direction.DESC, "scheduledDate", "startTime"));
  }

  @Test
  void deleteAppointment_existingId_deletesRow() {
    Appointment appt = Appointment.builder().id(1L)
        .patient(User.builder().firstName("A").surname("B").build())
        .doctor(User.builder().firstName("D").surname("E").build())
        .scheduledDate(LocalDate.of(2026, 9, 1))
        .startTime(LocalTime.of(10, 0))
        .status(AppointmentStatus.OPEN)
        .build();
    when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appt));

    adminAppointmentService.deleteAppointment(1L);

    verify(appointmentRepository).deleteById(1L);
  }

  @Test
  void deleteAppointment_unknownId_throws404() {
    when(appointmentRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> adminAppointmentService.deleteAppointment(99L))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
            .isEqualTo(HttpStatus.NOT_FOUND));
  }
}
