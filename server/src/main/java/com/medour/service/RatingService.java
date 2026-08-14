package com.medour.service;

import com.medour.exception.RatingAlreadyExistsException;
import com.medour.model.Appointment;
import com.medour.model.AppointmentStatus;
import com.medour.model.Rating;
import com.medour.repository.AppointmentRepository;
import com.medour.repository.RatingRepository;
import com.medour.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

@Service
public class RatingService {

  private final AppointmentRepository appointmentRepository;
  private final RatingRepository ratingRepository;
  private final UserRepository userRepository;

  public RatingService(AppointmentRepository appointmentRepository,
                       RatingRepository ratingRepository,
                       UserRepository userRepository) {
    this.appointmentRepository = appointmentRepository;
    this.ratingRepository = ratingRepository;
    this.userRepository = userRepository;
  }

  @Transactional
  public Rating submitRating(Long appointmentId, int value, Long callerPatientId) {
    Appointment appointment = appointmentRepository.findById(appointmentId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found"));

    if (!appointment.getPatient().getId().equals(callerPatientId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your appointment");
    }

    if (appointment.getStatus() != AppointmentStatus.COMPLETED) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only completed appointments may be rated");
    }

    if (ratingRepository.findByAppointmentId(appointmentId).isPresent()) {
      throw new RatingAlreadyExistsException();
    }

    Rating rating = Rating.builder()
        .appointment(appointment)
        .patient(appointment.getPatient())
        .doctor(appointment.getDoctor())
        .value(value)
        .build();
    try {
      rating = ratingRepository.save(rating);
    } catch (DataIntegrityViolationException ex) {
      // unique constraint on appointment_id violated by concurrent insert
      throw new RatingAlreadyExistsException();
    }

    // Recalculate doctor's average rating after insert
    Long doctorId = appointment.getDoctor().getId();
    double avg = ratingRepository.findAverageValueByDoctorId(doctorId).orElse(0.0);
    var doctor = appointment.getDoctor();
    doctor.setAverageRating(BigDecimal.valueOf(avg));
    userRepository.save(doctor);

    return rating;
  }
}
