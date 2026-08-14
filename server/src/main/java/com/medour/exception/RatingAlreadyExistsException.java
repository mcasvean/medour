package com.medour.exception;

public class RatingAlreadyExistsException extends RuntimeException {
  public RatingAlreadyExistsException() {
    super("Rating already exists for this appointment");
  }
}
