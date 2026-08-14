package com.medour.dto;

public record AppointmentStatusEventDto(Long appointmentId, String newStatus, String scheduledDate, String startTime) {
}
