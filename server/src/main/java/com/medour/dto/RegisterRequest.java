package com.medour.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

  @NotBlank
  @Email
  private String email;

  @NotBlank
  private String password;

  @NotBlank
  private String firstName;

  @NotBlank
  private String surname;

  @NotNull
  @Min(1)
  @Max(150)
  private Integer age;

  @NotBlank
  private String gender;

  @NotBlank
  private String city;

  @NotBlank
  private String address;

  private String county;

  private Long specialityId;

  @NotBlank
  private String role;
}
