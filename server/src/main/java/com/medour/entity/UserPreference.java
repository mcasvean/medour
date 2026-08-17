package com.medour.entity;

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
@Table(name = "user_preferences")
public class UserPreference {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id", unique = true, nullable = false)
  private Long userId;

  @Column(name = "pinned_sidebar", nullable = false)
  @Builder.Default
  private boolean pinnedSidebar = false;
}
