package com.medour.repository;

import com.medour.model.SlotReservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

public interface SlotReservationRepository extends JpaRepository<SlotReservation, Long> {

  long countByDoctorIdAndDateAndExpiresAtAfter(Long doctorId, LocalDate date, LocalDateTime now);

  boolean existsByDoctorIdAndDateAndStartTimeAndExpiresAtAfter(
      Long doctorId, LocalDate date, LocalTime startTime, LocalDateTime now);

  Optional<SlotReservation> findByIdAndReservedByPatientId(Long id, Long patientId);
}
