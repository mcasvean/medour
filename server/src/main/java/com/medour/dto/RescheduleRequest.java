package com.medour.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

public record RescheduleRequest(
    @NotNull @JsonFormat(pattern = "yyyy-MM-dd") LocalDate scheduledDate,
    @NotNull @JsonDeserialize(using = LocalTimeDeserializer.class) LocalTime startTime
) {}
