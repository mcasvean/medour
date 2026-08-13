package com.medour.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {

  @NotBlank
  private String firstName;

  @NotBlank
  private String surname;

  private Integer age;

  private String gender;

  private String city;

  private String address;

  private String county;

  private String speciality;
}
