package com.medour.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SubmitRatingRequest(
    @NotNull Long appointmentId,
    @Min(1) @Max(10) int value) {
}
