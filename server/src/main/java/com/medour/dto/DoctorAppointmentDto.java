package com.medour.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record DoctorAppointmentDto(
    Long id,
    LocalDate scheduledDate,
    LocalTime startTime,
    String patientFirstName,
    String patientSurname,
    boolean patientRemoved,
    String status,
    LocalDateTime createdAt,
    String wherebyRoomUrl) {
}
