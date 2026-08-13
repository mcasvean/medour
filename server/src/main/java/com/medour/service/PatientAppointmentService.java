package com.medour.service;

import com.medour.dto.PatientAppointmentDto;
import com.medour.repository.AppointmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PatientAppointmentService {

  private final AppointmentRepository appointmentRepository;

  public PatientAppointmentService(AppointmentRepository appointmentRepository) {
    this.appointmentRepository = appointmentRepository;
  }

  @Transactional(readOnly = true)
  public List<PatientAppointmentDto> getHistory(Long patientId) {
    return appointmentRepository.findByPatientIdOrderByScheduledDateDesc(patientId).stream()
        .map(a -> new PatientAppointmentDto(
            a.getId(),
            a.getScheduledDate(),
            a.getStartTime(),
            a.getDoctor().getFirstName(),
            a.getDoctor().getSurname(),
            a.getDoctor().getSpeciality(),
            a.getDoctor().getDeletedAt() != null,
            a.getStatus().name(),
            a.getCreatedAt()))
        .toList();
  }
}
