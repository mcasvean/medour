package com.medour.controller;

import com.medour.dto.DoctorSearchResult;
import com.medour.service.DoctorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/doctors")
public class DoctorController {

  private final DoctorService doctorService;

  public DoctorController(DoctorService doctorService) {
    this.doctorService = doctorService;
  }

  @GetMapping("/")
  public ResponseEntity<List<DoctorSearchResult>> getAll(
      @RequestParam Optional<String> speciality,
      @RequestParam Optional<String> county,
      @RequestParam Optional<String> city,
      @RequestParam Optional<LocalDate> date) {
    return ResponseEntity.ok(doctorService.searchDoctors(
        speciality.orElse(null),
        county.orElse(null),
        city.orElse(null),
        date.orElse(null)));
  }
}
