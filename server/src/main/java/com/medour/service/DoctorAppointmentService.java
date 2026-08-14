package com.medour.service;

import com.medour.dto.AppointmentStatusEventDto;
import com.medour.dto.DoctorAppointmentDto;
import com.medour.model.Appointment;
import com.medour.model.AppointmentStatus;
import com.medour.repository.AppointmentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

@Service
public class DoctorAppointmentService {

  private static final Set<String> ALLOWED_NEW_STATUSES = Set.of("CANCELED", "COMPLETED");

  private final AppointmentRepository appointmentRepository;
  private final SseService sseService;

  public DoctorAppointmentService(AppointmentRepository appointmentRepository, SseService sseService) {
    this.appointmentRepository = appointmentRepository;
    this.sseService = sseService;
  }

  @Transactional(readOnly = true)
  public List<DoctorAppointmentDto> getAppointments(Long doctorId) {
    return appointmentRepository.findByDoctorIdOrderByScheduledDateAsc(doctorId).stream()
        .map(a -> new DoctorAppointmentDto(
            a.getId(),
            a.getScheduledDate(),
            a.getStartTime(),
            a.getPatient().getFirstName(),
            a.getPatient().getSurname(),
            a.getPatient().getDeletedAt() != null,
            a.getStatus().name(),
            a.getCreatedAt(),
            a.getWherebyRoomUrl()))
        .toList();
  }

  @Transactional
  public void updateStatus(Long appointmentId, Long doctorId, String newStatus) {
    Appointment appointment = appointmentRepository.findById(appointmentId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found"));
    if (!appointment.getDoctor().getId().equals(doctorId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not the owning doctor");
    }
    if (appointment.getStatus() != AppointmentStatus.OPEN) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Appointment is not in OPEN status");
    }
    if (!ALLOWED_NEW_STATUSES.contains(newStatus)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status value");
    }
    appointment.setStatus(AppointmentStatus.valueOf(newStatus));
    appointmentRepository.save(appointment);
    sseService.broadcastAppointmentStatus(new AppointmentStatusEventDto(appointmentId, newStatus, null, null));
  }
}
