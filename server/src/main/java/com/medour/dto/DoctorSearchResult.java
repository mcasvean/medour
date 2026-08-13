package com.medour.dto;

import java.math.BigDecimal;

public record DoctorSearchResult(Long id, String firstName, String surname, String speciality, String county,
    String city, BigDecimal averageRating) {
}
