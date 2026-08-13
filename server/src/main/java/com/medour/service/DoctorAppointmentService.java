package com.medour.service;

import com.medour.dto.DoctorAppointmentDto;
import com.medour.repository.AppointmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DoctorAppointmentService {

  private final AppointmentRepository appointmentRepository;

  public DoctorAppointmentService(AppointmentRepository appointmentRepository) {
    this.appointmentRepository = appointmentRepository;
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
}
