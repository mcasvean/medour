package com.medour.controller;

import com.medour.dto.RatingResponse;
import com.medour.dto.SubmitRatingRequest;
import com.medour.model.Rating;
import com.medour.service.RatingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/ratings")
public class RatingController {

  private final RatingService ratingService;

  public RatingController(RatingService ratingService) {
    this.ratingService = ratingService;
  }

  @PostMapping
  @PreAuthorize("hasRole('PATIENT')")
  public ResponseEntity<RatingResponse> submitRating(
      @Valid @RequestBody SubmitRatingRequest request,
      Authentication auth) {
    long patientId = parseUserId(auth);
    Rating rating = ratingService.submitRating(request.appointmentId(), request.value(), patientId);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(new RatingResponse(rating.getId(), rating.getValue()));
  }

  private long parseUserId(Authentication auth) {
    try {
      return Long.parseLong(auth.getName());
    } catch (NumberFormatException e) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid principal");
    }
  }
}
