package com.medour.service;

import com.medour.exception.RatingAlreadyExistsException;
import com.medour.model.Appointment;
import com.medour.model.AppointmentStatus;
import com.medour.model.Rating;
import com.medour.model.User;
import com.medour.repository.AppointmentRepository;
import com.medour.repository.RatingRepository;
import com.medour.repository.UserRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RatingServiceTest {

  @Mock
  private AppointmentRepository appointmentRepository;

  @Mock
  private RatingRepository ratingRepository;

  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private RatingService ratingService;

  private User patient(long id) {
    return User.builder().id(id).build();
  }

  private User doctor(long id) {
    return User.builder().id(id).build();
  }

  private Appointment completedAppointment(long id, User p, User d) {
    return Appointment.builder()
        .id(id)
        .patient(p)
        .doctor(d)
        .scheduledDate(LocalDate.of(2026, 9, 1))
        .startTime(LocalTime.of(10, 0))
        .status(AppointmentStatus.COMPLETED)
        .build();
  }

  @Test
  void submitRating_newRating_returns201AndUpdatesAverage() {
    User p = patient(1L);
    User d = doctor(10L);
    Appointment appt = completedAppointment(5L, p, d);
    Rating saved = Rating.builder().id(99L).appointment(appt).patient(p).doctor(d).value(7).build();

    when(appointmentRepository.findById(5L)).thenReturn(Optional.of(appt));
    when(ratingRepository.findByAppointmentId(5L)).thenReturn(Optional.empty());
    when(ratingRepository.save(any(Rating.class))).thenReturn(saved);
    when(ratingRepository.findAverageValueByDoctorId(10L)).thenReturn(Optional.of(7.0));

    Rating result = ratingService.submitRating(5L, 7, 1L);

    assertThat(result.getId()).isEqualTo(99L);
    assertThat(result.getValue()).isEqualTo(7);
    verify(userRepository).save(argThat(u -> BigDecimal.valueOf(7.0).compareTo(u.getAverageRating()) == 0));
  }

  @Test
  void submitRating_wrongPatient_throws403() {
    User p = patient(1L);
    User d = doctor(10L);
    Appointment appt = completedAppointment(5L, p, d);

    when(appointmentRepository.findById(5L)).thenReturn(Optional.of(appt));

    assertThatThrownBy(() -> ratingService.submitRating(5L, 7, 2L))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("403");
  }

  @Test
  void submitRating_nonCompletedAppointment_throws400() {
    User p = patient(1L);
    User d = doctor(10L);
    Appointment appt = Appointment.builder()
        .id(5L).patient(p).doctor(d)
        .status(AppointmentStatus.OPEN)
        .scheduledDate(LocalDate.of(2026, 9, 1))
        .startTime(LocalTime.of(10, 0))
        .build();

    when(appointmentRepository.findById(5L)).thenReturn(Optional.of(appt));

    assertThatThrownBy(() -> ratingService.submitRating(5L, 7, 1L))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("400");
  }

  @Test
  void submitRating_duplicate_throws409() {
    User p = patient(1L);
    User d = doctor(10L);
    Appointment appt = completedAppointment(5L, p, d);
    Rating existing = Rating.builder().id(1L).appointment(appt).patient(p).doctor(d).value(5).build();

    when(appointmentRepository.findById(5L)).thenReturn(Optional.of(appt));
    when(ratingRepository.findByAppointmentId(5L)).thenReturn(Optional.of(existing));

    assertThatThrownBy(() -> ratingService.submitRating(5L, 7, 1L))
        .isInstanceOf(RatingAlreadyExistsException.class);
  }

  @Test
  void submitRating_appointmentNotFound_throws404() {
    when(appointmentRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> ratingService.submitRating(99L, 7, 1L))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("404");
  }

  @Test
  void updateRating_validEdit_returnsRatingAndUpdatesAverage() {
    User p = patient(1L);
    User d = doctor(10L);
    Appointment appt = completedAppointment(5L, p, d);
    Rating existing = Rating.builder().id(99L).appointment(appt).patient(p).doctor(d).value(7).build();

    when(ratingRepository.findById(99L)).thenReturn(Optional.of(existing));
    when(ratingRepository.findAverageValueByDoctorId(10L)).thenReturn(Optional.of(9.0));

    Rating result = ratingService.updateRating(99L, 9, 1L);

    assertThat(result.getValue()).isEqualTo(9);
    verify(ratingRepository).save(existing);
    verify(userRepository).save(argThat(u -> BigDecimal.valueOf(9.0).compareTo(u.getAverageRating()) == 0));
  }

  @Test
  void updateRating_wrongPatient_throws403() {
    User p = patient(1L);
    User d = doctor(10L);
    Appointment appt = completedAppointment(5L, p, d);
    Rating existing = Rating.builder().id(99L).appointment(appt).patient(p).doctor(d).value(7).build();

    when(ratingRepository.findById(99L)).thenReturn(Optional.of(existing));

    assertThatThrownBy(() -> ratingService.updateRating(99L, 9, 2L))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("403");
  }

  @Test
  void updateRating_notFound_throws404() {
    when(ratingRepository.findById(9999L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> ratingService.updateRating(9999L, 9, 1L))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("404");
  }
}
