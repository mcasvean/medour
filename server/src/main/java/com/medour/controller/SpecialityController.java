package com.medour.controller;

import com.medour.dto.SpecialityRequest;
import com.medour.dto.SpecialityResponse;
import com.medour.service.SpecialityService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/specialities")
public class SpecialityController {

  private final SpecialityService specialityService;

  public SpecialityController(SpecialityService specialityService) {
    this.specialityService = specialityService;
  }

  @GetMapping
  public ResponseEntity<List<SpecialityResponse>> list() {
    return ResponseEntity.ok(specialityService.findAll());
  }

  @PostMapping
  public ResponseEntity<SpecialityResponse> create(@Valid @RequestBody SpecialityRequest req) {
    return ResponseEntity.status(HttpStatus.CREATED).body(specialityService.create(req.getName()));
  }

  @PutMapping("/{id}")
  public ResponseEntity<SpecialityResponse> update(@PathVariable Long id, @Valid @RequestBody SpecialityRequest req) {
    return ResponseEntity.ok(specialityService.update(id, req.getName()));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    specialityService.delete(id);
    return ResponseEntity.ok().build();
  }
}
