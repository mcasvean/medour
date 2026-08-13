package com.medour.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "slot_reservations", uniqueConstraints = @UniqueConstraint(columnNames = { "doctor_id", "date",
    "start_time" }))
public class SlotReservation {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "doctor_id")
  private User doctor;

  private LocalDate date;

  @Column(name = "start_time")
  private LocalTime startTime;

  @ManyToOne
  @JoinColumn(name = "reserved_by_patient_id")
  private User reservedByPatient;

  private LocalDateTime reservedAt;

  private LocalDateTime expiresAt;
}
