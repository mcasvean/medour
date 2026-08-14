---
title: "Patient Appointment Reschedule"
type: "feature"
created: "2026-08-14"
status: "ready-for-dev"
review_loop_iteration: 0
baseline_commit: ""
context: ["spec-6-1-toast-notification-system"]
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Once a patient books an appointment, there is no way to change its date or time without cancelling it entirely and re-booking. This creates friction and a poor user experience for schedule changes.

**Approach:**

- Add `PATCH /api/v1/appointments/patient/{id}/reschedule` (PATIENT only) accepting `{ scheduledDate, startTime }` for the new slot.
- The server validates: appointment must be OPEN; patient must own the appointment; new slot must be available (not already in `slot_reservations` or `appointments` as OPEN for that doctor/date/time); new date must not be in the past.
- The old `slot_reservations` row is deleted (releasing the old slot); a new `slot_reservations` row is inserted for the new slot (re-locking it); the appointment's `scheduledDate` and `startTime` are updated in the same transaction.
- SSE `broadcastSlotChange` is fired twice (old slot → AVAILABLE, new slot → LOCKED), then `broadcastAppointmentStatus` is fired so the doctor's view updates.
- In `PatientAppointmentsView.vue`, OPEN appointment cards gain a "Reschedule" button that opens a modal/dialog. The modal lets the patient pick a new date and time slot (same step-form pattern as the slot grid from Story 2.2 — reuse `SlotGrid.vue` for the doctor already booked). Confirming sends the PATCH request and shows a success toast.

## Boundaries & Constraints

**Always:**

- Only OPEN appointments can be rescheduled; any other status returns 409 `{"error": "Only OPEN appointments can be rescheduled."}`.
- Only the owning patient can reschedule; wrong patient returns 403.
- The new slot must be available: if `slot_reservations` already has a row for `(doctor_id, new_date, new_start_time)` OR an OPEN appointment already exists for that combination, return 409 `{"error": "Selected slot is not available."}`.
- The new `scheduledDate` must be today or in the future; past dates return 400 `{"error": "Cannot reschedule to a past date."}`.
- The old slot reservation is deleted and the new one is inserted atomically within the same `@Transactional` method.
- Whereby room URL on the appointment is **not** changed — the same room is used for the rescheduled time.
- SSE events: fire `AVAILABLE` for the old slot, then `LOCKED` for the new slot, then `appointment-status` (with new date/time embedded) so other clients see the change.
- The doctor's `DoctorAppointmentsView` is updated via SSE — no additional polling.

**Ask First:**

- (none)

**Never:**

- Patients cannot reschedule to a slot with a different doctor — the same doctor is kept.
- Doctors and admins cannot use this endpoint to reschedule a patient's appointment.
- Do not cancel and re-create the appointment — update `scheduledDate` and `startTime` in place.

## I/O & Edge-Case Matrix

| Scenario                       | Input / State                                           | Expected Output / Behavior                                             | Error Handling  |
| ------------------------------ | ------------------------------------------------------- | ---------------------------------------------------------------------- | --------------- |
| Valid reschedule               | Patient reschedules own OPEN appt to a free future slot | 200; appointment updated; old slot freed; new slot reserved; SSE fired | —               |
| Non-OPEN appointment           | Appointment is CANCELED                                 | 409 `"Only OPEN appointments can be rescheduled."`                     | —               |
| Wrong patient                  | Different patient's appointment id                      | 403                                                                    | Ownership check |
| New slot already locked        | `slot_reservations` row exists for new slot             | 409 `"Selected slot is not available."`                                | —               |
| New slot already booked (OPEN) | An OPEN appointment exists for doctor/date/time         | 409 `"Selected slot is not available."`                                | —               |
| Past new date                  | `scheduledDate` is yesterday                            | 400 `"Cannot reschedule to a past date."`                              | —               |
| Same slot as current           | Patient picks the same date and time                    | 400 `"New slot is the same as the current appointment slot."`          | —               |

</frozen-after-approval>

## Code Map

**Backend:**

- `server/src/main/java/com/medour/dto/RescheduleRequest.java` — **NEW** record: `@NotNull LocalDate scheduledDate`, `@NotNull LocalTime startTime`
- `server/src/main/java/com/medour/service/PatientAppointmentService.java` — **EXTEND** add `@Transactional reschedule(Long appointmentId, Long patientId, LocalDate newDate, LocalTime newStartTime)`: find appointment (404); check patient ownership (403); check status OPEN (409); check newDate not before today (400); check newDate+newTime != current (400); check slot available in `slotReservationRepository` and `appointmentRepository` (409 if taken); delete old `slot_reservations` row; insert new `slot_reservations` row; update `appointment.scheduledDate` and `appointment.startTime`; save; fire SSE `broadcastSlotChange` for old slot → AVAILABLE; fire SSE `broadcastSlotChange` for new slot → LOCKED; fire `broadcastAppointmentStatus` with updated appointment data
- `server/src/main/java/com/medour/controller/PatientAppointmentController.java` — **EXTEND** add `@PatchMapping("/{id}/reschedule")` accepting `@Valid @RequestBody RescheduleRequest`, extracts patientId from auth → delegates to service → 200
- `server/src/main/java/com/medour/config/SecurityConfig.java` — **EXTEND** `PATCH /api/v1/appointments/patient/**` already requires PATIENT; verify it covers this new path
- `server/src/test/java/com/medour/service/PatientAppointmentServiceTest.java` — **EXTEND** add 5 tests: valid reschedule → updated + SSE; non-OPEN → 409; wrong patient → 403; slot taken → 409; past date → 400

**Frontend:**

- `client/src/stores/appointmentStore.ts` — **EXTEND** add `rescheduleAppointment(id: number, scheduledDate: string, startTime: string)` action: `PATCH /appointments/patient/${id}/reschedule`; on success, find appointment in `patientAppointments` and update `scheduledDate` and `startTime` in place
- `client/src/views/PatientAppointmentsView.vue` — **EXTEND** add "Reschedule" `VBtn` (outlined, small) on each OPEN appointment card; clicking opens a `VDialog` with a `SlotGrid` for the same doctor; when a slot is selected, confirm button calls `appointmentStore.rescheduleAppointment(...)`, then shows success toast and closes dialog; error shows error toast

## Tasks & Acceptance

**Execution:**

- [ ] `server/src/main/java/com/medour/dto/RescheduleRequest.java` — NEW record with `@NotNull LocalDate scheduledDate` and `@NotNull LocalTime startTime`
- [ ] `server/src/main/java/com/medour/service/PatientAppointmentService.java` — inject `SlotReservationRepository`; implement `reschedule(...)` with all validation checks, slot swap, and SSE broadcasts
- [ ] `server/src/main/java/com/medour/controller/PatientAppointmentController.java` — add `@PatchMapping("/{id}/reschedule")` mapped to service; return 200 with updated appointment DTO
- [ ] `server/src/main/java/com/medour/config/SecurityConfig.java` — confirm `PATCH /api/v1/appointments/patient/**` is covered for PATIENT role
- [ ] `server/src/test/java/com/medour/service/PatientAppointmentServiceTest.java` — add 5 new tests covering the cases in the edge-case matrix
- [ ] `client/src/stores/appointmentStore.ts` — add `rescheduleAppointment(id, scheduledDate, startTime)` action with in-place update of `patientAppointments`
- [ ] `client/src/views/PatientAppointmentsView.vue` — add Reschedule button on OPEN cards; implement reschedule dialog with SlotGrid for the appointment's doctor; wire confirm to store action with toast feedback

## Acceptance Criteria

- Given a patient clicks "Reschedule" on an OPEN appointment, a dialog opens showing a slot grid for the same doctor.
- Given the patient selects a new free slot and confirms, the appointment's date and time are updated, a success toast is shown, and the doctor's view reflects the new time via SSE.
- Given the patient tries to reschedule to an already-locked slot, a 409 error toast is shown.
- Given the patient tries to reschedule a non-OPEN appointment, a 409 error toast is shown.
- Given a different patient attempts to reschedule via the API, a 403 is returned.
- Given the patient selects a past date, a 400 error is shown.
- The Whereby room URL is unchanged after reschedule.

## Verification

**Commands:**

- `cd server && ./mvnw test` — expected: all existing tests + 5 new PatientAppointmentService tests pass
- `cd client && npm run build` — expected: zero TypeScript errors
- `cd client && npm run test` — expected: all existing tests pass
