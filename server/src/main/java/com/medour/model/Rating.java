package com.medour.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ratings", uniqueConstraints = @UniqueConstraint(columnNames = "appointment_id"))
public class Rating {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "appointment_id", nullable = false)
  private Appointment appointment;

  @ManyToOne
  @JoinColumn(name = "patient_id", nullable = false)
  private User patient;

  @ManyToOne
  @JoinColumn(name = "doctor_id", nullable = false)
  private User doctor;

  @Column(nullable = false)
  private Integer value;
}
