package com.medour.controller;

import com.medour.dto.DoctorAppointmentDto;
import com.medour.service.DoctorAppointmentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

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

  private long parseUserId(Authentication auth) {
    try {
      return Long.parseLong(auth.getName());
    } catch (NumberFormatException e) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid principal");
    }
  }
}
