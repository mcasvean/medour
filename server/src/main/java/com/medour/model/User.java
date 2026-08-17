package com.medour.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(unique = true, nullable = false)
  private String email;

  @Column(name = "password_hash", nullable = false)
  private String passwordHash;

  @Column(name = "first_name", nullable = false)
  private String firstName;

  @Column(nullable = false)
  private String surname;

  private Integer age;

  private String gender;

  private String city;

  private String address;

  private String county;

  private String speciality; // deprecated — superseded by specialityRef; no longer written by application

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "speciality_id")
  private Speciality specialityRef;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Role role;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  @Column(name = "must_change_password", nullable = false)
  @Builder.Default
  private Boolean mustChangePassword = false;

  @Column(name = "average_rating", precision = 3, scale = 1)
  private BigDecimal averageRating;

  @Column(name = "profile_picture", columnDefinition = "TEXT")
  private String profilePicture;
}
