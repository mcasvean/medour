package com.medour.repository;

import com.medour.model.Appointment;
import com.medour.model.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

  long countByDoctorIdAndScheduledDateAndStatusIn(Long doctorId, LocalDate date, List<AppointmentStatus> statuses);

  boolean existsByDoctorIdAndScheduledDateAndStartTimeAndStatus(
      Long doctorId, LocalDate scheduledDate, LocalTime startTime, AppointmentStatus status);

  List<Appointment> findByPatientIdOrderByScheduledDateDesc(Long patientId);

  List<Appointment> findByStatus(AppointmentStatus status);
}
