package com.medour.service;

import com.medour.dto.AppointmentCreatedResponse;
import com.medour.dto.CreateAppointmentRequest;
import com.medour.dto.ReserveSlotRequest;
import com.medour.dto.ReserveSlotResponse;
import com.medour.dto.SlotEventDto;
import com.medour.exception.SlotAlreadyReservedException;
import com.medour.model.Appointment;
import com.medour.model.AppointmentStatus;
import com.medour.model.SlotReservation;
import com.medour.model.SlotState;
import com.medour.model.User;
import com.medour.repository.AppointmentRepository;
import com.medour.repository.SlotReservationRepository;
import com.medour.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
public class AppointmentService {

  private final SlotReservationRepository slotReservationRepository;
  private final AppointmentRepository appointmentRepository;
  private final UserRepository userRepository;
  private final SseService sseService;
  private final WherebyService wherebyService;

  public AppointmentService(SlotReservationRepository slotReservationRepository,
      AppointmentRepository appointmentRepository,
      UserRepository userRepository,
      SseService sseService,
      WherebyService wherebyService) {
    this.slotReservationRepository = slotReservationRepository;
    this.appointmentRepository = appointmentRepository;
    this.userRepository = userRepository;
    this.sseService = sseService;
    this.wherebyService = wherebyService;
  }

  @Transactional
  public ReserveSlotResponse reserveSlot(Long patientId, ReserveSlotRequest req) {
    User patient = userRepository.findById(patientId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    User doctor = userRepository.findByIdAndDeletedAtIsNull(req.getDoctorId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    LocalDateTime now = LocalDateTime.now();
    SlotReservation reservation = SlotReservation.builder()
        .doctor(doctor)
        .date(req.getDate())
        .startTime(req.getStartTime())
        .reservedByPatient(patient)
        .reservedAt(now)
        .expiresAt(now.plusMinutes(10))
        .build();
    try {
      SlotReservation saved = slotReservationRepository.save(reservation);
      sseService.broadcast(new SlotEventDto(
          doctor.getId(), req.getDate().toString(), req.getStartTime().toString(), SlotState.LOCKED));
      return new ReserveSlotResponse(saved.getId());
    } catch (DataIntegrityViolationException e) {
      throw new SlotAlreadyReservedException();
    }
  }

  @Transactional
  public void cancelReservation(Long patientId, Long reservationId) {
    SlotReservation reservation = slotReservationRepository.findById(reservationId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    if (!reservation.getReservedByPatient().getId().equals(patientId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
    slotReservationRepository.delete(reservation);
    sseService.broadcast(new SlotEventDto(
        reservation.getDoctor().getId(),
        reservation.getDate().toString(),
        reservation.getStartTime().toString(),
        SlotState.AVAILABLE));
  }

  @Transactional
  public AppointmentCreatedResponse createAppointment(Long patientId, CreateAppointmentRequest req) {
    SlotReservation reservation = slotReservationRepository.findById(req.reservationId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    if (!reservation.getReservedByPatient().getId().equals(patientId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
    if (reservation.getExpiresAt().isBefore(LocalDateTime.now())) {
      slotReservationRepository.delete(reservation);
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Reservation expired");
    }
    String roomUrl = wherebyService.createRoom(reservation.getDate());
    Appointment appointment = Appointment.builder()
        .patient(reservation.getReservedByPatient())
        .doctor(reservation.getDoctor())
        .scheduledDate(reservation.getDate())
        .startTime(reservation.getStartTime())
        .status(AppointmentStatus.OPEN)
        .wherebyRoomUrl(roomUrl)
        .build();
    Appointment saved = appointmentRepository.save(appointment);
    slotReservationRepository.delete(reservation);
    sseService.broadcast(new SlotEventDto(
        reservation.getDoctor().getId(),
        reservation.getDate().toString(),
        reservation.getStartTime().toString(),
        SlotState.UNAVAILABLE));
    return new AppointmentCreatedResponse(
        saved.getId(),
        reservation.getDoctor().getId(),
        reservation.getDate(),
        reservation.getStartTime(),
        AppointmentStatus.OPEN.name(),
        saved.getWherebyRoomUrl());
  }
}
