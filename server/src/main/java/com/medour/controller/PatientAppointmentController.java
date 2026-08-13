package com.medour.controller;

import com.medour.dto.PatientAppointmentDto;
import com.medour.service.PatientAppointmentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/appointments")
public class PatientAppointmentController {

  private final PatientAppointmentService patientAppointmentService;

  public PatientAppointmentController(PatientAppointmentService patientAppointmentService) {
    this.patientAppointmentService = patientAppointmentService;
  }

  @GetMapping("/my")
  public ResponseEntity<List<PatientAppointmentDto>> getHistory(Authentication auth) {
    return ResponseEntity.ok(patientAppointmentService.getHistory(parseUserId(auth)));
  }

  private long parseUserId(Authentication auth) {
    try {
      return Long.parseLong(auth.getName());
    } catch (NumberFormatException e) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid principal");
    }
  }
}
