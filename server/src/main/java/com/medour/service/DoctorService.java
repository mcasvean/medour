package com.medour.service;

import com.medour.dto.DoctorSearchResult;
import com.medour.model.AppointmentStatus;
import com.medour.model.Role;
import com.medour.repository.AppointmentRepository;
import com.medour.repository.SlotReservationRepository;
import com.medour.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class DoctorService {

  private static final int TOTAL_SLOTS_PER_DAY = 24;

  private final UserRepository userRepository;
  private final AppointmentRepository appointmentRepository;
  private final SlotReservationRepository slotReservationRepository;

  public DoctorService(UserRepository userRepository,
      AppointmentRepository appointmentRepository,
      SlotReservationRepository slotReservationRepository) {
    this.userRepository = userRepository;
    this.appointmentRepository = appointmentRepository;
    this.slotReservationRepository = slotReservationRepository;
  }

  @Transactional(readOnly = true)
  public List<DoctorSearchResult> searchDoctors(String speciality, String county, String city, LocalDate date) {
    return userRepository.findAll().stream()
        .filter(u -> u.getRole() == Role.DOCTOR && u.getDeletedAt() == null)
        .filter(u -> isBlank(speciality) || containsIgnoreCase(u.getSpeciality(), speciality))
        .filter(u -> isBlank(county) || containsIgnoreCase(u.getCounty(), county))
        .filter(u -> isBlank(city) || containsIgnoreCase(u.getCity(), city))
        .filter(u -> {
          if (date == null)
            return true;
          long reservations = slotReservationRepository.countByDoctorIdAndDateAndExpiresAtAfter(
              u.getId(), date, LocalDateTime.now());
          long appointments = appointmentRepository.countByDoctorIdAndScheduledDateAndStatusIn(
              u.getId(), date, List.of(AppointmentStatus.OPEN, AppointmentStatus.COMPLETED));
          return (reservations + appointments) < TOTAL_SLOTS_PER_DAY;
        })
        .map(u -> new DoctorSearchResult(u.getId(), u.getFirstName(), u.getSurname(),
            u.getSpeciality(), u.getCounty(), u.getCity(), u.getAverageRating()))
        .toList();
  }

  private static boolean isBlank(String s) {
    return s == null || s.isBlank();
  }

  private static boolean containsIgnoreCase(String field, String filter) {
    return field != null && field.toLowerCase().contains(filter.toLowerCase());
  }
}
