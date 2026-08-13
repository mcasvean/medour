package com.medour.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record AdminAppointmentDto(
    Long id,
    String patientName,
    String doctorName,
    LocalDate scheduledDate,
    LocalTime startTime,
    String status,
    String wherebyRoomUrl) {
}
