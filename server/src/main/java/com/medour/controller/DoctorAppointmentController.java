package com.medour.controller;

import com.medour.dto.DoctorAppointmentDto;
import com.medour.dto.StatusUpdateRequest;
import com.medour.service.DoctorAppointmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/appointments/doctor")
public class DoctorAppointmentController {

  private final DoctorAppointmentService doctorAppointmentService;

  public DoctorAppointmentController(DoctorAppointmentService doctorAppointmentService) {
    this.doctorAppointmentService = doctorAppointmentService;
  }

  @GetMapping("/my")
  public ResponseEntity<List<DoctorAppointmentDto>> getAppointments(Authentication auth) {
    return ResponseEntity.ok(doctorAppointmentService.getAppointments(parseUserId(auth)));
  }

  @PatchMapping("/{id}/status")
  public ResponseEntity<Map<String, String>> updateStatus(
      @PathVariable Long id,
      @Valid @RequestBody StatusUpdateRequest req,
      Authentication auth) {
    doctorAppointmentService.updateStatus(id, parseUserId(auth), req.newStatus());
    return ResponseEntity.ok(Map.of("status", req.newStatus()));
  }

  private long parseUserId(Authentication auth) {
    try {
      return Long.parseLong(auth.getName());
    } catch (NumberFormatException e) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid principal");
    }
  }
}
