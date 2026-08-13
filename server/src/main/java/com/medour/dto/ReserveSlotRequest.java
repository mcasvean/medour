package com.medour.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReserveSlotRequest {
  private Long doctorId;

  @JsonFormat(pattern = "yyyy-MM-dd")
  private LocalDate date;

  @JsonDeserialize(using = LocalTimeDeserializer.class)
  private LocalTime startTime;
}
