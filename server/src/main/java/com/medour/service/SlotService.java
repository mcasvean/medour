package com.medour.service;

import com.medour.dto.SlotDto;
import com.medour.model.AppointmentStatus;
import com.medour.model.SlotState;
import com.medour.repository.AppointmentRepository;
import com.medour.repository.SlotReservationRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class SlotService {

  private static final LocalTime FIRST_SLOT = LocalTime.of(8, 0);
  private static final LocalTime LAST_SLOT_START = LocalTime.of(19, 30);

  private final SlotReservationRepository slotReservationRepository;
  private final AppointmentRepository appointmentRepository;

  public SlotService(SlotReservationRepository slotReservationRepository,
      AppointmentRepository appointmentRepository) {
    this.slotReservationRepository = slotReservationRepository;
    this.appointmentRepository = appointmentRepository;
  }

  public List<SlotDto> getSlotsForDoctor(Long doctorId, LocalDate date) {
    return getSlotsForDoctor(doctorId, date, LocalDateTime.now());
  }

  // package-private to allow time injection in tests
  List<SlotDto> getSlotsForDoctor(Long doctorId, LocalDate date, LocalDateTime now) {
    LocalDate today = now.toLocalDate();

    if (date.isBefore(today)) {
      return List.of();
    }

    LocalTime startFrom = FIRST_SLOT;
    if (date.equals(today)) {
      LocalTime current = now.toLocalTime();
      LocalTime candidate = FIRST_SLOT;
      while (!candidate.isAfter(LAST_SLOT_START) && candidate.isBefore(current)) {
        candidate = candidate.plusMinutes(30);
      }
      startFrom = candidate;
    }

    List<SlotDto> slots = new ArrayList<>();
    LocalTime t = startFrom;
    while (!t.isAfter(LAST_SLOT_START)) {
      SlotState state = deriveState(doctorId, date, t, now);
      slots.add(new SlotDto(t.toString(), t.plusMinutes(30).toString(), state));
      t = t.plusMinutes(30);
    }
    return slots;
  }

  private SlotState deriveState(Long doctorId, LocalDate date, LocalTime startTime, LocalDateTime now) {
    if (slotReservationRepository.existsByDoctorIdAndDateAndStartTimeAndExpiresAtAfter(
        doctorId, date, startTime, now)) {
      return SlotState.LOCKED;
    }
    if (appointmentRepository.existsByDoctorIdAndScheduledDateAndStartTimeAndStatus(
        doctorId, date, startTime, AppointmentStatus.OPEN)) {
      return SlotState.UNAVAILABLE;
    }
    return SlotState.AVAILABLE;
  }
}
