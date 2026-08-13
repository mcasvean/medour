package com.medour.service;

import com.medour.dto.AppointmentStatusEventDto;
import com.medour.model.AppointmentStatus;
import com.medour.repository.AppointmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AutoCancelService {

  private final AppointmentRepository appointmentRepository;
  private final SseService sseService;

  public AutoCancelService(AppointmentRepository appointmentRepository, SseService sseService) {
    this.appointmentRepository = appointmentRepository;
    this.sseService = sseService;
  }

  @Transactional
  public void autoCancelOverdue() {
    LocalDateTime cutoff = LocalDateTime.now().minusMinutes(10);

    appointmentRepository.findByStatus(AppointmentStatus.OPEN).stream()
        .filter(a -> a.getScheduledDate().atTime(a.getStartTime()).isBefore(cutoff))
        .forEach(a -> {
          a.setStatus(AppointmentStatus.AUTO_CANCELED);
          appointmentRepository.save(a);
          sseService.broadcastAppointmentStatus(
              new AppointmentStatusEventDto(a.getId(), AppointmentStatus.AUTO_CANCELED.name()));
        });
  }
}
