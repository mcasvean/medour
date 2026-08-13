package com.medour.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record AppointmentCreatedResponse(
    Long id,
    Long doctorId,
    LocalDate scheduledDate,
    LocalTime startTime,
    String status
) {
}
