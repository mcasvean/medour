package com.medour.repository;

import com.medour.model.SlotReservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public interface SlotReservationRepository extends JpaRepository<SlotReservation, Long> {

  // only count reservations that have not yet expired
  long countByDoctorIdAndDateAndExpiresAtAfter(Long doctorId, LocalDate date, LocalDateTime now);

  boolean existsByDoctorIdAndDateAndStartTimeAndExpiresAtAfter(
      Long doctorId, LocalDate date, LocalTime startTime, LocalDateTime now);
}
