package com.medour.dto;

import jakarta.validation.constraints.NotNull;

public record UpdatePreferencesRequest(@NotNull Boolean pinnedSidebar) {
}
