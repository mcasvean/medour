package com.medour.dto;

public record UserProfileResponse(
    Long id,
    String email,
    String firstName,
    String surname,
    String role,
    Integer age,
    String gender,
    String city,
    String address,
    String county,
    String speciality) {
}
