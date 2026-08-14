package com.medour.service;

import com.medour.dto.AppointmentStatusEventDto;
import com.medour.dto.PatientAppointmentDto;
import com.medour.dto.SlotEventDto;
import com.medour.model.Appointment;
import com.medour.model.AppointmentStatus;
import com.medour.model.Rating;
import com.medour.model.SlotState;
import com.medour.repository.AppointmentRepository;
import com.medour.repository.RatingRepository;
import com.medour.repository.SlotReservationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
public class PatientAppointmentService {

  private final AppointmentRepository appointmentRepository;
  private final RatingRepository ratingRepository;
  private final SlotReservationRepository slotReservationRepository;
  private final SseService sseService;

  public PatientAppointmentService(AppointmentRepository appointmentRepository,
      RatingRepository ratingRepository,
      SlotReservationRepository slotReservationRepository,
      SseService sseService) {
    this.appointmentRepository = appointmentRepository;
    this.ratingRepository = ratingRepository;
    this.slotReservationRepository = slotReservationRepository;
    this.sseService = sseService;
  }

  @Transactional(readOnly = true)
  public List<PatientAppointmentDto> getHistory(Long patientId) {
    return appointmentRepository.findByPatientIdOrderByScheduledDateDesc(patientId).stream()
        .map(a -> {
          Optional<Rating> rating = ratingRepository.findByAppointmentId(a.getId());
          return toDto(a, rating);
        })
        .toList();
  }

  @Transactional
  public PatientAppointmentDto reschedule(Long appointmentId, Long patientId,
      LocalDate newDate, LocalTime newStartTime) {
    Appointment appointment = appointmentRepository.findById(appointmentId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found"));

    if (!appointment.getPatient().getId().equals(patientId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }

    if (appointment.getStatus() != AppointmentStatus.OPEN) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Only OPEN appointments can be rescheduled.");
    }

    if (newDate.isBefore(LocalDate.now())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot reschedule to a past date.");
    }

    if (newDate.equals(appointment.getScheduledDate()) && newStartTime.equals(appointment.getStartTime())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New slot is the same as the current appointment slot.");
    }

    Long doctorId = appointment.getDoctor().getId();
    boolean slotLocked = slotReservationRepository.existsByDoctorIdAndDateAndStartTimeAndExpiresAtAfter(
        doctorId, newDate, newStartTime, LocalDateTime.now());
    boolean slotBooked = appointmentRepository.existsByDoctorIdAndScheduledDateAndStartTimeAndStatus(
        doctorId, newDate, newStartTime, AppointmentStatus.OPEN);
    if (slotLocked || slotBooked) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Selected slot is not available.");
    }

    LocalDate oldDate = appointment.getScheduledDate();
    LocalTime oldStartTime = appointment.getStartTime();

    appointment.setScheduledDate(newDate);
    appointment.setStartTime(newStartTime);
    appointmentRepository.save(appointment);

    sseService.broadcast(new SlotEventDto(doctorId, oldDate.toString(), oldStartTime.toString(), SlotState.AVAILABLE));
    sseService.broadcast(new SlotEventDto(doctorId, newDate.toString(), newStartTime.toString(), SlotState.LOCKED));
    sseService.broadcastAppointmentStatus(
        new AppointmentStatusEventDto(appointmentId, "OPEN", newDate.toString(), newStartTime.toString()));

    Optional<Rating> rating = ratingRepository.findByAppointmentId(appointmentId);
    return toDto(appointment, rating);
  }

  private PatientAppointmentDto toDto(Appointment a, Optional<Rating> rating) {
    return new PatientAppointmentDto(
        a.getId(),
        a.getDoctor().getId(),
        a.getScheduledDate(),
        a.getStartTime(),
        a.getDoctor().getFirstName(),
        a.getDoctor().getSurname(),
        a.getDoctor().getSpeciality(),
        a.getDoctor().getDeletedAt() != null,
        a.getStatus().name(),
        a.getCreatedAt(),
        a.getWherebyRoomUrl(),
        rating.map(Rating::getValue).orElse(null),
        rating.map(Rating::getId).orElse(null));
  }
}

