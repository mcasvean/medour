package com.medour.exception;

public class SlotAlreadyReservedException extends RuntimeException {
  public SlotAlreadyReservedException() {
    super("Slot already reserved");
  }
}
