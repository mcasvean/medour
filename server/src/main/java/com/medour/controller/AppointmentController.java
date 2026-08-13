package com.medour.controller;

import com.medour.dto.AppointmentCreatedResponse;
import com.medour.dto.CreateAppointmentRequest;
import com.medour.dto.ReserveSlotRequest;
import com.medour.dto.ReserveSlotResponse;
import com.medour.service.AppointmentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class AppointmentController {

  private final AppointmentService appointmentService;

  public AppointmentController(AppointmentService appointmentService) {
    this.appointmentService = appointmentService;
  }

  @PostMapping("/api/v1/slots/reserve")
  public ResponseEntity<ReserveSlotResponse> reserveSlot(
      @RequestBody ReserveSlotRequest req, Authentication auth) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(appointmentService.reserveSlot(parseUserId(auth), req));
  }

  @DeleteMapping("/api/v1/slots/reserve/{id}")
  public ResponseEntity<Void> cancelReservation(
      @PathVariable Long id, Authentication auth) {
    appointmentService.cancelReservation(parseUserId(auth), id);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/api/v1/appointments")
  public ResponseEntity<AppointmentCreatedResponse> createAppointment(
      @RequestBody CreateAppointmentRequest req, Authentication auth) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(appointmentService.createAppointment(parseUserId(auth), req));
  }

  private long parseUserId(Authentication auth) {
    try {
      return Long.parseLong(auth.getName());
    } catch (NumberFormatException e) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid principal");
    }
  }
}
