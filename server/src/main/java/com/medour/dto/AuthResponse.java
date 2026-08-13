package com.medour.dto;

public record AuthResponse(String token, Long id, String email, String firstName, String surname, String role,
    boolean mustChangePassword) {
}
