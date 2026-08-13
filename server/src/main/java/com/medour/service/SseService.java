package com.medour.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medour.dto.AppointmentStatusEventDto;
import com.medour.dto.SlotEventDto;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class SseService {

  private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();
  private final ObjectMapper objectMapper;

  public SseService(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public SseEmitter subscribe() {
    SseEmitter emitter = new SseEmitter(0L);
    emitters.add(emitter);
    emitter.onCompletion(() -> emitters.remove(emitter));
    emitter.onTimeout(() -> emitters.remove(emitter));
    emitter.onError(ex -> emitters.remove(emitter));
    return emitter;
  }

  public void broadcast(SlotEventDto event) {
    String json;
    try {
      json = objectMapper.writeValueAsString(event);
    } catch (JsonProcessingException e) {
      return;
    }
    for (SseEmitter emitter : emitters) {
      try {
        emitter.send(SseEmitter.event().data(json));
      } catch (Exception e) {
        emitters.remove(emitter);
      }
    }
  }

  public void broadcastAppointmentStatus(AppointmentStatusEventDto event) {
    String json;
    try {
      json = objectMapper.writeValueAsString(event);
    } catch (JsonProcessingException e) {
      return;
    }
    for (SseEmitter emitter : emitters) {
      try {
        emitter.send(SseEmitter.event().name("appointment-status").data(json));
      } catch (Exception e) {
        emitters.remove(emitter);
      }
    }
  }
}
