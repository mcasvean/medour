package com.medour.service;

import com.medour.dto.AppointmentStatusEventDto;
import com.medour.dto.PatientAppointmentDto;
import com.medour.dto.SlotEventDto;
import com.medour.model.Appointment;
import com.medour.model.AppointmentStatus;
import com.medour.model.SlotState;
import com.medour.model.User;
import com.medour.repository.AppointmentRepository;
import com.medour.repository.RatingRepository;
import com.medour.repository.SlotReservationRepository;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatientAppointmentServiceTest {

        @Mock
        private AppointmentRepository appointmentRepository;

        @Mock
        private RatingRepository ratingRepository;

        @Mock
        private SlotReservationRepository slotReservationRepository;

        @Mock
        private SseService sseService;

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
                when(ratingRepository.findByAppointmentId(anyLong())).thenReturn(Optional.empty());

                List<PatientAppointmentDto> result = patientAppointmentService.getHistory(1L);

                assertThat(result).hasSize(1);
                assertThat(result.get(0).doctorRemoved()).isFalse();
                assertThat(result.get(0).status()).isEqualTo("OPEN");
                assertThat(result.get(0).wherebyRoomUrl()).isEqualTo("https://whereby.com/test-room");
                assertThat(result.get(0).ratingValue()).isNull();
                assertThat(result.get(0).ratingId()).isNull();
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
                when(ratingRepository.findByAppointmentId(anyLong())).thenReturn(Optional.empty());

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
                when(ratingRepository.findByAppointmentId(anyLong())).thenReturn(Optional.empty());

                List<PatientAppointmentDto> result = patientAppointmentService.getHistory(1L);

                assertThat(result.get(0).wherebyRoomUrl()).isNull();
        }

        @Test
        void getHistory_completedWithRating_populatesRatingFields() {
                User doctor = User.builder().id(10L).firstName("Doc").surname("Tor")
                                .speciality("Cardiology").deletedAt(null).build();
                User patient = User.builder().id(1L).build();
                Appointment appt = Appointment.builder()
                                .id(4L).patient(patient).doctor(doctor)
                                .scheduledDate(LocalDate.of(2026, 9, 1))
                                .startTime(LocalTime.of(10, 0))
                                .status(AppointmentStatus.COMPLETED)
                                .wherebyRoomUrl(null)
                                .build();
                com.medour.model.Rating rating = com.medour.model.Rating.builder()
                                .id(7L).appointment(appt).patient(patient).doctor(doctor).value(8).build();
                when(appointmentRepository.findByPatientIdOrderByScheduledDateDesc(1L))
                                .thenReturn(List.of(appt));
                when(ratingRepository.findByAppointmentId(4L)).thenReturn(Optional.of(rating));

                List<PatientAppointmentDto> result = patientAppointmentService.getHistory(1L);

                assertThat(result.get(0).ratingValue()).isEqualTo(8);
                assertThat(result.get(0).ratingId()).isEqualTo(7L);
        }

        // ── reschedule tests ──────────────────────────────────────────────────────

        @Test
        void reschedule_validInput_updatesAppointmentAndFiresSse() {
                User patient = User.builder().id(1L).build();
                User doctor = User.builder().id(2L).firstName("Doc").surname("Tor")
                                .speciality("Cardiology").deletedAt(null).build();
                LocalDate oldDate = LocalDate.now().plusDays(1);
                LocalTime oldTime = LocalTime.of(10, 0);
                LocalDate newDate = LocalDate.now().plusDays(2);
                LocalTime newTime = LocalTime.of(11, 0);

                Appointment appt = Appointment.builder()
                                .id(10L).patient(patient).doctor(doctor)
                                .scheduledDate(oldDate).startTime(oldTime)
                                .status(AppointmentStatus.OPEN)
                                .wherebyRoomUrl("https://whereby.com/room")
                                .build();

                when(appointmentRepository.findById(10L)).thenReturn(Optional.of(appt));
                when(slotReservationRepository.existsByDoctorIdAndDateAndStartTimeAndExpiresAtAfter(
                                eq(2L), eq(newDate), eq(newTime), any())).thenReturn(false);
                when(appointmentRepository.existsByDoctorIdAndScheduledDateAndStartTimeAndStatus(
                                eq(2L), eq(newDate), eq(newTime), eq(AppointmentStatus.OPEN))).thenReturn(false);
                when(appointmentRepository.save(any())).thenReturn(appt);
                when(ratingRepository.findByAppointmentId(10L)).thenReturn(Optional.empty());

                PatientAppointmentDto result = patientAppointmentService.reschedule(10L, 1L, newDate, newTime);

                assertThat(appt.getScheduledDate()).isEqualTo(newDate);
                assertThat(appt.getStartTime()).isEqualTo(newTime);
                assertThat(result.scheduledDate()).isEqualTo(newDate);
                assertThat(result.startTime()).isEqualTo(newTime);
                assertThat(result.wherebyRoomUrl()).isEqualTo("https://whereby.com/room");

                ArgumentCaptor<SlotEventDto> slotCaptor = ArgumentCaptor.forClass(SlotEventDto.class);
                verify(sseService, times(2)).broadcast(slotCaptor.capture());
                assertThat(slotCaptor.getAllValues().get(0).state()).isEqualTo(SlotState.AVAILABLE);
                assertThat(slotCaptor.getAllValues().get(1).state()).isEqualTo(SlotState.LOCKED);

                ArgumentCaptor<AppointmentStatusEventDto> statusCaptor = ArgumentCaptor
                                .forClass(AppointmentStatusEventDto.class);
                verify(sseService).broadcastAppointmentStatus(statusCaptor.capture());
                assertThat(statusCaptor.getValue().appointmentId()).isEqualTo(10L);
                assertThat(statusCaptor.getValue().scheduledDate()).isEqualTo(newDate.toString());
                assertThat(statusCaptor.getValue().startTime()).isEqualTo(newTime.toString());
        }

        @Test
        void reschedule_nonOpenAppointment_throws409() {
                User patient = User.builder().id(1L).build();
                User doctor = User.builder().id(2L).build();
                Appointment appt = Appointment.builder()
                                .id(10L).patient(patient).doctor(doctor)
                                .scheduledDate(LocalDate.now().plusDays(1)).startTime(LocalTime.of(10, 0))
                                .status(AppointmentStatus.CANCELED).build();
                when(appointmentRepository.findById(10L)).thenReturn(Optional.of(appt));

                assertThatThrownBy(() -> patientAppointmentService.reschedule(
                                10L, 1L, LocalDate.now().plusDays(2), LocalTime.of(11, 0)))
                                .isInstanceOf(ResponseStatusException.class)
                                .satisfies(e -> {
                                        ResponseStatusException ex = (ResponseStatusException) e;
                                        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                                        assertThat(ex.getReason())
                                                        .isEqualTo("Only OPEN appointments can be rescheduled.");
                                });
        }

        @Test
        void reschedule_wrongPatient_throws403() {
                User owner = User.builder().id(1L).build();
                User doctor = User.builder().id(2L).build();
                Appointment appt = Appointment.builder()
                                .id(10L).patient(owner).doctor(doctor)
                                .scheduledDate(LocalDate.now().plusDays(1)).startTime(LocalTime.of(10, 0))
                                .status(AppointmentStatus.OPEN).build();
                when(appointmentRepository.findById(10L)).thenReturn(Optional.of(appt));

                assertThatThrownBy(() -> patientAppointmentService.reschedule(
                                10L, 99L, LocalDate.now().plusDays(2), LocalTime.of(11, 0)))
                                .isInstanceOf(ResponseStatusException.class)
                                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                                                .isEqualTo(HttpStatus.FORBIDDEN));
        }

        @Test
        void reschedule_slotAlreadyTaken_throws409() {
                User patient = User.builder().id(1L).build();
                User doctor = User.builder().id(2L).build();
                LocalDate newDate = LocalDate.now().plusDays(2);
                LocalTime newTime = LocalTime.of(11, 0);
                Appointment appt = Appointment.builder()
                                .id(10L).patient(patient).doctor(doctor)
                                .scheduledDate(LocalDate.now().plusDays(1)).startTime(LocalTime.of(10, 0))
                                .status(AppointmentStatus.OPEN).build();
                when(appointmentRepository.findById(10L)).thenReturn(Optional.of(appt));
                when(slotReservationRepository.existsByDoctorIdAndDateAndStartTimeAndExpiresAtAfter(
                                eq(2L), eq(newDate), eq(newTime), any())).thenReturn(true);

                assertThatThrownBy(() -> patientAppointmentService.reschedule(10L, 1L, newDate, newTime))
                                .isInstanceOf(ResponseStatusException.class)
                                .satisfies(e -> {
                                        ResponseStatusException ex = (ResponseStatusException) e;
                                        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                                        assertThat(ex.getReason()).isEqualTo("Selected slot is not available.");
                                });
        }

        @Test
        void reschedule_pastDate_throws400() {
                User patient = User.builder().id(1L).build();
                User doctor = User.builder().id(2L).build();
                Appointment appt = Appointment.builder()
                                .id(10L).patient(patient).doctor(doctor)
                                .scheduledDate(LocalDate.now().plusDays(1)).startTime(LocalTime.of(10, 0))
                                .status(AppointmentStatus.OPEN).build();
                when(appointmentRepository.findById(10L)).thenReturn(Optional.of(appt));

                assertThatThrownBy(() -> patientAppointmentService.reschedule(
                                10L, 1L, LocalDate.now().minusDays(1), LocalTime.of(10, 0)))
                                .isInstanceOf(ResponseStatusException.class)
                                .satisfies(e -> {
                                        ResponseStatusException ex = (ResponseStatusException) e;
                                        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                                        assertThat(ex.getReason()).isEqualTo("Cannot reschedule to a past date.");
                                });
        }
}
