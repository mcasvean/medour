package com.medour.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record PatientAppointmentDto(
    Long id,
    LocalDate scheduledDate,
    LocalTime startTime,
    String doctorFirstName,
    String doctorSurname,
    String doctorSpeciality,
    boolean doctorRemoved,
    String status,
    LocalDateTime createdAt,
    String wherebyRoomUrl) {
}
