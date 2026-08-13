package com.medour.controller;

import com.medour.dto.DoctorSearchResult;
import com.medour.dto.SlotDto;
import com.medour.service.DoctorService;
import com.medour.service.SlotService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/doctors")
public class DoctorController {

  private final DoctorService doctorService;
  private final SlotService slotService;

  public DoctorController(DoctorService doctorService, SlotService slotService) {
    this.doctorService = doctorService;
    this.slotService = slotService;
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

  @GetMapping("/{id}/slots")
  public ResponseEntity<List<SlotDto>> getSlots(
      @PathVariable Long id,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
    return ResponseEntity.ok(slotService.getSlotsForDoctor(id, date));
  }
}
