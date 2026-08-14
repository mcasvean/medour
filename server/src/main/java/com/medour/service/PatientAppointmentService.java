package com.medour.service;

import com.medour.dto.PatientAppointmentDto;
import com.medour.model.Rating;
import com.medour.repository.AppointmentRepository;
import com.medour.repository.RatingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class PatientAppointmentService {

  private final AppointmentRepository appointmentRepository;
  private final RatingRepository ratingRepository;

  public PatientAppointmentService(AppointmentRepository appointmentRepository,
      RatingRepository ratingRepository) {
    this.appointmentRepository = appointmentRepository;
    this.ratingRepository = ratingRepository;
  }

  @Transactional(readOnly = true)
  public List<PatientAppointmentDto> getHistory(Long patientId) {
    return appointmentRepository.findByPatientIdOrderByScheduledDateDesc(patientId).stream()
        .map(a -> {
          Optional<Rating> rating = ratingRepository.findByAppointmentId(a.getId());
          return new PatientAppointmentDto(
              a.getId(),
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
        })
        .toList();
  }
}
