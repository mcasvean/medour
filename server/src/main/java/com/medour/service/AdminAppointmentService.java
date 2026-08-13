package com.medour.service;

import com.medour.dto.AdminAppointmentDto;
import com.medour.repository.AppointmentRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class AdminAppointmentService {

  private final AppointmentRepository appointmentRepository;

  public AdminAppointmentService(AppointmentRepository appointmentRepository) {
    this.appointmentRepository = appointmentRepository;
  }

  @Transactional(readOnly = true)
  public List<AdminAppointmentDto> getAllAppointments() {
    return appointmentRepository
        .findAll(Sort.by(Sort.Direction.DESC, "scheduledDate", "startTime"))
        .stream()
        .map(a -> new AdminAppointmentDto(
            a.getId(),
            a.getPatient().getFirstName() + " " + a.getPatient().getSurname(),
            a.getDoctor().getFirstName() + " " + a.getDoctor().getSurname(),
            a.getScheduledDate(),
            a.getStartTime(),
            a.getStatus().name(),
            a.getWherebyRoomUrl()
        ))
        .toList();
  }

  @Transactional
  public void deleteAppointment(Long id) {
    appointmentRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    appointmentRepository.deleteById(id);
  }
}
