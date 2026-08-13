package com.medour.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(EmailAlreadyUsedException.class)
  public ResponseEntity<Map<String, String>> handleEmailAlreadyUsed(EmailAlreadyUsedException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(Map.of("error", "Email already in use"));
  }

  @ExceptionHandler(InvalidCredentialsException.class)
  public ResponseEntity<Map<String, String>> handleInvalidCredentials(InvalidCredentialsException ex) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(Map.of("error", "Invalid credentials"));
  }

  @ExceptionHandler(WrongPasswordException.class)
  public ResponseEntity<Map<String, String>> handleWrongPassword(WrongPasswordException ex) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(Map.of("error", "Wrong password"));
  }

  @ExceptionHandler(SlotAlreadyReservedException.class)
  public ResponseEntity<Map<String, String>> handleSlotAlreadyReserved(SlotAlreadyReservedException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(Map.of("error", "Slot already reserved"));
  }

  @ExceptionHandler(WherebyException.class)
  public ResponseEntity<Map<String, String>> handleWhereby(WherebyException ex) {
    return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
        .body(Map.of("error", "Video room creation failed"));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
    String message = ex.getBindingResult().getFieldErrors().stream()
        .map(e -> e.getField() + ": " + e.getDefaultMessage())
        .collect(Collectors.joining(", "));
    return ResponseEntity.badRequest().body(Map.of("error", message));
  }
}
