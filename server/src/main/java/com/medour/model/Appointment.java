package com.medour.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "appointments")
public class Appointment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "patient_id")
  private User patient;

  @ManyToOne
  @JoinColumn(name = "doctor_id")
  private User doctor;

  private LocalDate scheduledDate;

  private LocalTime startTime;

  @Enumerated(EnumType.STRING)
  private AppointmentStatus status;

  @CreationTimestamp
  private LocalDateTime createdAt;

  private String wherebyRoomUrl;
}
