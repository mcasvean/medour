---
title: "Multi-Step Appointment Booking Flow"
type: "feature"
created: "2026-08-13"
status: "in-progress"
review_loop_iteration: 0
baseline_commit: "7e14398f3d7337252892e3d9c7b8b16f4cf201a4"
context: []
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Patients can see slot availability but have no way to select and lock a slot, go through a confirmation step, or complete a booking. There is no protection against two patients booking the same slot simultaneously.

**Approach:** Add `POST /api/v1/slots/reserve` that atomically inserts a `slot_reservations` row (unique constraint enforces exclusivity); add `DELETE /api/v1/slots/reserve/{id}` for cancellation; add `POST /api/v1/appointments` to convert a reservation to an OPEN appointment. Wire SSE events on each transition. On the client, clicking an Available slot triggers the lock, advances the UI to a confirmation step, and navigating away or cancelling releases the lock.

## Boundaries & Constraints

**Always:**
- The DB unique constraint on `slot_reservations(doctor_id, date, start_time)` is the **only** guarantee against double-booking. A concurrent INSERT that violates it produces a `DataIntegrityViolationException` → 409 `{ "error": "Slot already reserved" }`
- Reservation `expiresAt = LocalDateTime.now().plusMinutes(10)` — gives the patient 10 minutes to confirm
- Both `cancelReservation` and `createAppointment` must verify the reservation belongs to the requesting patient (`reservedByPatientId = currentUserId`); if not → 403
- `createAppointment` is `@Transactional`: delete reservation + insert appointment in a single unit; if either fails, both roll back
- SSE events fire AFTER the DB operation commits: lock → LOCKED; cancel → AVAILABLE; appointment created → UNAVAILABLE
- `wherebyRoomUrl` is `null` in this story (Story 2.4 adds Whereby)
- Clicking an available slot in `SlotGrid` optimistically updates its state to LOCKED in `appointmentStore.slots` before the API call, then confirms on 200 or reverts on error
- When `BookingSearchView` unmounts while `bookingStep === 'confirming'`, it calls `appointmentStore.cancelBooking()` to release the lock
- Only PATIENT role may call reserve/create-appointment endpoints; Spring Security `hasRole("PATIENT")` enforces this on `/api/v1/slots/**` and `POST /api/v1/appointments`

**Ask First:**
- (none)

**Never:**
- No Whereby call in this story (Story 2.4)
- Doctors and admins cannot create appointments via this flow
- A patient cannot reserve a slot that is LOCKED or UNAVAILABLE — the DB constraint handles it, but the client also guards it (only Available slots emit `select`)

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|---|---|---|---|
| Patient locks available slot | `POST /slots/reserve` with valid doctorId/date/startTime | 201 + `{ reservationId }`, SSE LOCKED broadcast | N/A |
| Concurrent lock attempt | Two patients POST at same slot simultaneously | One gets 201; the other gets 409 | DB unique constraint → 409 |
| Patient cancels mid-booking | `DELETE /slots/reserve/{id}` (own reservation) | 204; reservation deleted; SSE AVAILABLE | N/A |
| Patient confirms booking | `POST /appointments` with `reservationId` | 201 + `AppointmentCreatedResponse`; reservation deleted; appointment OPEN; SSE UNAVAILABLE | N/A |
| Wrong-patient cancel/confirm | Other patient's reservationId | 403 | Ownership check in service |
| Navigate away mid-booking | `BookingSearchView` unmounts while confirming | `appointmentStore.cancelBooking()` called automatically; slot released | N/A |

</frozen-after-approval>

## Code Map

- `server/src/main/java/com/medour/exception/SlotAlreadyReservedException.java` -- NEW RuntimeException → 409
- `server/src/main/java/com/medour/exception/GlobalExceptionHandler.java` -- add `@ExceptionHandler(SlotAlreadyReservedException.class)` → 409 `{ "error": "Slot already reserved" }`
- `server/src/main/java/com/medour/dto/ReserveSlotRequest.java` -- NEW: `Long doctorId`, `LocalDate date`, `@DateTimeFormat(iso=TIME) LocalTime startTime`
- `server/src/main/java/com/medour/dto/ReserveSlotResponse.java` -- NEW record: `Long reservationId`
- `server/src/main/java/com/medour/dto/CreateAppointmentRequest.java` -- NEW record: `Long reservationId`
- `server/src/main/java/com/medour/dto/AppointmentCreatedResponse.java` -- NEW record: `Long id`, `Long doctorId`, `LocalDate scheduledDate`, `LocalTime startTime`, `String status`
- `server/src/main/java/com/medour/repository/SlotReservationRepository.java` -- add `Optional<SlotReservation> findByIdAndReservedByPatientId(Long id, Long patientId)`
- `server/src/main/java/com/medour/repository/UserRepository.java` -- add `Optional<User> findByIdAndDeletedAtIsNull(Long id)` (for doctor lookup in service)
- `server/src/main/java/com/medour/service/AppointmentService.java` -- NEW: `reserveSlot(Long patientId, ReserveSlotRequest)`, `cancelReservation(Long patientId, Long reservationId)`, `createAppointment(Long patientId, CreateAppointmentRequest)`; inject SseService for broadcast
- `server/src/main/java/com/medour/controller/AppointmentController.java` -- NEW `@RestController`; `POST /api/v1/slots/reserve` (PATIENT only) → 201; `DELETE /api/v1/slots/reserve/{id}` → 204; `POST /api/v1/appointments` (PATIENT only) → 201
- `server/src/main/java/com/medour/config/SecurityConfig.java` -- add `hasRole("PATIENT")` matchers for `POST /api/v1/slots/reserve` and `POST /api/v1/appointments`
- `server/src/test/java/com/medour/controller/AppointmentControllerTest.java` -- NEW; 3 tests: reserve → 201, cancel → 204, confirm → 201
- `server/src/test/java/com/medour/service/AppointmentServiceTest.java` -- NEW; test: DataIntegrityViolationException on save → SlotAlreadyReservedException; wrong-patient cancel → 403
- `client/src/components/SlotGrid.vue` -- add `@click` on Available slots: `emit('select', s.startTime)`; `defineEmits<{ select: [startTime: string] }>()`
- `client/src/stores/appointmentStore.ts` -- add: `reservationId: null|number`, `bookingStep: 'searching'|'confirming'|'done'`; `lockSlot(doctorId, date, startTime)` → optimistic update + POST reserve; `cancelBooking()` → DELETE + restore slot state; `confirmBooking()` → POST appointments
- `client/src/views/BookingSearchView.vue` -- handle `@select` from `SlotGrid`; when `bookingStep==='confirming'` show confirmation panel (doctor info, date, time, Cancel/Confirm buttons); onUnmounted cancel if confirming

## Tasks & Acceptance

**Execution:**

- [ ] `server/src/main/java/com/medour/exception/SlotAlreadyReservedException.java` -- NEW `extends RuntimeException`
- [ ] `server/src/main/java/com/medour/exception/GlobalExceptionHandler.java` -- add handler → 409 `{ "error": "Slot already reserved" }`
- [ ] `server/src/main/java/com/medour/dto/ReserveSlotRequest.java` -- NEW: `@Data @NoArgsConstructor @AllArgsConstructor`; `Long doctorId`; `@JsonFormat(pattern="yyyy-MM-dd") LocalDate date`; `@JsonDeserialize(using=LocalTimeDeserializer.class) LocalTime startTime`
- [ ] `server/src/main/java/com/medour/dto/ReserveSlotResponse.java` -- NEW record: `Long reservationId`
- [ ] `server/src/main/java/com/medour/dto/CreateAppointmentRequest.java` -- NEW record: `Long reservationId`
- [ ] `server/src/main/java/com/medour/dto/AppointmentCreatedResponse.java` -- NEW record: `Long id, Long doctorId, LocalDate scheduledDate, LocalTime startTime, String status`
- [ ] `server/src/main/java/com/medour/repository/SlotReservationRepository.java` -- add `Optional<SlotReservation> findByIdAndReservedByPatientId(Long id, Long patientId)`
- [ ] `server/src/main/java/com/medour/repository/UserRepository.java` -- add `Optional<User> findByIdAndDeletedAtIsNull(Long id)` (used by service to look up doctor)
- [ ] `server/src/main/java/com/medour/service/AppointmentService.java` -- NEW `@Service`; inject `SlotReservationRepository`, `AppointmentRepository`, `UserRepository`, `SseService`; `@Transactional reserveSlot(Long patientId, req)`: build and save `SlotReservation` (expiresAt=now+10m), catch `DataIntegrityViolationException` → throw `SlotAlreadyReservedException`; broadcast LOCKED; return `ReserveSlotResponse`; `@Transactional cancelReservation(Long patientId, Long reservationId)`: load via `findByIdAndReservedByPatientId` (404 if absent, 403 if wrong patient - use `orElseThrow` with 404, then ownership check throws 403); delete; broadcast AVAILABLE; return 204; `@Transactional createAppointment(Long patientId, req)`: load reservation (404/403 same as above); build `Appointment(patient=patientUser, doctor=reservation.doctor, scheduledDate, startTime, status=OPEN)`; save appointment; delete reservation; broadcast UNAVAILABLE; return `AppointmentCreatedResponse`
- [ ] `server/src/main/java/com/medour/controller/AppointmentController.java` -- NEW `@RestController`; `@PostMapping("/api/v1/slots/reserve")` → 201 + `ReserveSlotResponse`; `@DeleteMapping("/api/v1/slots/reserve/{id}")` → 204; `@PostMapping("/api/v1/appointments")` → 201 + `AppointmentCreatedResponse`; extract patient ID via `parseUserId(Authentication)` (same pattern as UserController)
- [ ] `server/src/main/java/com/medour/config/SecurityConfig.java` -- add `.requestMatchers(POST, "/api/v1/slots/reserve", "/api/v1/appointments").hasRole("PATIENT")` before `anyRequest().authenticated()`
- [ ] `server/src/test/java/com/medour/controller/AppointmentControllerTest.java` -- NEW `@WebMvcTest(AppointmentController.class)` with `@MockBean AppointmentService`, `@MockBean JwtUtil`; 3 tests: `reserve_validSlot_returns201`; `cancel_ownReservation_returns204`; `confirm_validReservation_returns201`
- [ ] `server/src/test/java/com/medour/service/AppointmentServiceTest.java` -- NEW; test (1): `reserveSlot_duplicateSlot_throwsSlotAlreadyReservedException` (stub save throws `DataIntegrityViolationException`); test (2): `cancelReservation_wrongPatient_throwsForbidden` (stub returns reservation with different patientId)
- [ ] `client/src/components/SlotGrid.vue` -- add `defineEmits<{ select: [startTime: string] }>()`; wrap slot cell in `<button>` (or add `@click`) with `v-if="s.state === 'AVAILABLE'"` condition calling `emit('select', s.startTime)`; disable/no-click for LOCKED and UNAVAILABLE
- [ ] `client/src/stores/appointmentStore.ts` -- add to state: `reservationId: null as number|null`, `bookingStep: 'searching' as 'searching'|'confirming'|'done'`; add `lockSlot(startTime)`: optimistically set slot state to LOCKED; call `api.post('/slots/reserve', { doctorId: selectedDoctorId, date: selectedDate, startTime })`; on 200 set `reservationId`; set `bookingStep='confirming'`; on error revert slot state + set errorMessage; add `cancelBooking()`: call `api.delete('/slots/reserve/${reservationId}')`; set slot back to AVAILABLE; set `bookingStep='searching'`; set `reservationId=null`; add `confirmBooking()`: call `api.post('/appointments', { reservationId })`; on 200 set `bookingStep='done'`; set `reservationId=null`
- [ ] `client/src/views/BookingSearchView.vue` -- listen for `@select="onSlotSelected"` event from `SlotGrid`; `onSlotSelected(startTime)` calls `appointmentStore.lockSlot(startTime)`; conditionally render: when `bookingStep==='confirming'`, show confirmation panel with selected doctor name, date, time, and Cancel/Confirm buttons calling `appointmentStore.cancelBooking()` / `appointmentStore.confirmBooking()`; in `onUnmounted`: if `appointmentStore.bookingStep === 'confirming'` call `appointmentStore.cancelBooking()`

**Acceptance Criteria:**

- Given a patient clicks an Available slot, then the slot immediately shows as LOCKED in the grid, the server receives a reservation insert, an SSE LOCKED event broadcasts, and the confirmation panel appears.
- Given two patients click the same Available slot simultaneously, then exactly one receives the confirmation step and the other sees an error (slot already reserved).
- Given a patient on the confirmation panel clicks Cancel, then the reservation is deleted, the slot returns to AVAILABLE via SSE, and the confirmation panel closes.
- Given a patient navigates away from `BookingSearchView` while confirming, then the reservation is automatically deleted.
- Given a patient clicks Confirm, then an OPEN appointment is created in the DB, the reservation is deleted atomically, and an SSE UNAVAILABLE event broadcasts.
- Given a DOCTOR or ADMIN user calls `POST /api/v1/slots/reserve`, then Spring Security returns 403.

## Verification

**Commands:**
- `cd server && ./mvnw test` -- expected: 40 tests pass (35 existing + 3 AppointmentControllerTest + 2 AppointmentServiceTest)
- `cd client && npm run test` -- expected: all 21 tests pass (no new client tests; SlotGrid emit tested via store tests)
- `cd client && npm run build` -- expected: zero TypeScript errors

## Spec Change Log
