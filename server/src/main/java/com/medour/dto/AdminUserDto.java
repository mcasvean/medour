package com.medour.dto;

public record AdminUserDto(
        Long id,
        String email,
        String firstName,
        String surname,
        String role,
        Long specialityId,
        String specialityName,
        String county,
        String city,
        Integer age,
        String gender,
        String address,
        boolean mustChangePassword,
        boolean isDeleted) {
}
