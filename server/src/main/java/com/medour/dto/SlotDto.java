package com.medour.dto;

import com.medour.model.SlotState;

public record SlotDto(String startTime, String endTime, SlotState state) {
}
