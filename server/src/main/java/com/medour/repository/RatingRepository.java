package com.medour.repository;

import com.medour.model.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RatingRepository extends JpaRepository<Rating, Long> {

  Optional<Rating> findByAppointmentId(Long appointmentId);

  @Query("SELECT ROUND(AVG(r.value), 1) FROM Rating r WHERE r.doctor.id = :doctorId")
  Optional<Double> findAverageValueByDoctorId(@Param("doctorId") Long doctorId);
}
