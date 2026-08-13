package com.medour.dto;

import com.medour.model.SlotState;

public record SlotEventDto(Long doctorId, String date, String startTime, SlotState state) {
}
