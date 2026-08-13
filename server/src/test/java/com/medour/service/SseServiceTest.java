package com.medour.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medour.dto.AppointmentStatusEventDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SseServiceTest {

  @Test
  void broadcastAppointmentStatus_serializesCorrectDtoAndHandlesUnconnectedEmitter() throws Exception {
    ObjectMapper mockMapper = mock(ObjectMapper.class);
    when(mockMapper.writeValueAsString(any())).thenReturn("{\"appointmentId\":1,\"newStatus\":\"CANCELED\"}");

    SseService service = new SseService(mockMapper);
    service.subscribe(); // registers a live (but unconnected) emitter

    AppointmentStatusEventDto dto = new AppointmentStatusEventDto(1L, "CANCELED");

    // send() on an unconnected emitter throws; broadcastAppointmentStatus must
    // catch it
    assertThatNoException().isThrownBy(() -> service.broadcastAppointmentStatus(dto));

    // verifies the correct DTO was serialized (not the slot DTO or raw string)
    verify(mockMapper).writeValueAsString(eq(dto));
  }
}
