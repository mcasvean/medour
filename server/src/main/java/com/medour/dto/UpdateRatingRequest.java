package com.medour.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record UpdateRatingRequest(@Min(1) @Max(10) int value) {
}
