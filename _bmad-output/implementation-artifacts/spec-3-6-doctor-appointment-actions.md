---
title: "Doctor Appointment Actions"
type: "feature"
created: "2026-08-13"
status: "done"
review_loop_iteration: 0
baseline_commit: "6d672aadfd03c6985fd7ec4acf6ecacc6cdeb85d"
context: []
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** The doctor appointment view (Story 3.5) is read-only. Doctors cannot cancel or complete appointments, and there is no Join button on the doctor side. Status changes by a doctor must also update the patient's history view via SSE.

**Approach:** Add `PATCH /api/v1/appointments/doctor/{id}/status` (DOCTOR only) that accepts `{ "newStatus": "CANCELED" | "COMPLETED" }`, validates ownership and OPEN status, persists the change, and fires an SSE `"appointment-status"` event. Wire Cancel, Complete, and Join buttons on the Upcoming tab cards in `DoctorAppointmentsView`.

## Boundaries & Constraints

**Always:**
- Only OPEN appointments can be canceled or completed; any other current status returns 409
- Only the owning doctor can update an appointment; a mismatched doctorId returns 403
- Valid new statuses for this endpoint: `CANCELED` or `COMPLETED`; any other value returns 400
- SSE `broadcastAppointmentStatus` is called after the DB commit (within the `@Transactional` method as done in Story 3.2 pattern)
- Join button uses the same `isJoinActive()` helper (imported from `appointmentStore`) and `window.open(wherebyRoomUrl, '_blank', 'noopener,noreferrer')`; only shown on OPEN cards with a non-null `wherebyRoomUrl`
- Cancel and Complete buttons are only shown on OPEN cards in the Upcoming tab; Past tab cards are read-only
- After a successful status update the client patches `doctorAppointments` locally — the matching appointment's status is updated without re-fetching

**Ask First:**
- (none)

**Never:**
- Doctors cannot change status to OPEN once it has moved to a terminal state
- No admin status-change endpoint in this story (Story 4.4)

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|---|---|---|---|
| Doctor cancels own OPEN appointment | `PATCH /doctor/{id}/status` `{ "newStatus": "CANCELED" }` | 200; status CANCELED; SSE event fired | N/A |
| Doctor completes own OPEN appointment | `newStatus: COMPLETED` | 200; status COMPLETED; SSE event fired | N/A |
| Non-OPEN appointment | appointment status is CANCELED | 409 `{ "error": "Appointment is not in OPEN status" }` | N/A |
| Wrong doctor | appointment belongs to different doctor | 403 | Ownership check |
| Invalid new status | `newStatus: OPEN` | 400 | Validation |

</frozen-after-approval>

## Code Map

- `server/src/main/java/com/medour/dto/StatusUpdateRequest.java` -- NEW record: `String newStatus`
- `server/src/main/java/com/medour/service/DoctorAppointmentService.java` -- add `@Transactional updateStatus(Long appointmentId, Long doctorId, String newStatus)`: find appointment (404), check ownership (403), check OPEN (409), validate newStatus in {CANCELED, COMPLETED} (400), set status, save, broadcast
- `server/src/main/java/com/medour/controller/DoctorAppointmentController.java` -- add `PATCH /{id}/status` calling `service.updateStatus(id, parseUserId(auth), req.newStatus())` → 200 `{ "status": newStatus }`
- `server/src/main/java/com/medour/config/SecurityConfig.java` -- add `PATCH /api/v1/appointments/doctor/**` requires DOCTOR
- `server/src/test/java/com/medour/service/DoctorAppointmentServiceTest.java` -- add 3 tests: cancel OPEN → CANCELED + SSE; wrong doctor → 403; non-OPEN → 409
- `server/src/test/java/com/medour/controller/DoctorAppointmentControllerTest.java` -- add test: PATCH /{id}/status valid → 200; invalid status → 400
- `client/src/stores/appointmentStore.ts` -- add `updateDoctorAppointmentStatus(id, newStatus)` action: PATCH `/appointments/doctor/${id}/status`, then patch matching appointment in `doctorAppointments`
- `client/src/views/DoctorAppointmentsView.vue` -- import `isJoinActive`; add Cancel, Complete, Join buttons on OPEN cards; each button calls the appropriate store action or opens the room; Join button uses `isJoinActive` check

## Tasks & Acceptance

**Execution:**

- [ ] `server/src/main/java/com/medour/dto/StatusUpdateRequest.java` -- NEW record: `String newStatus`
- [ ] `server/src/main/java/com/medour/service/DoctorAppointmentService.java` -- add `@Transactional updateStatus(Long appointmentId, Long doctorId, String newStatus)`: find by id (404 if absent); check `appointment.getDoctor().getId().equals(doctorId)` (403 if false); check `appointment.getStatus() == OPEN` (409 `"Appointment is not in OPEN status"` if false); if newStatus not in {CANCELED, COMPLETED} → 400; set status to `AppointmentStatus.valueOf(newStatus)`, save; `sseService.broadcastAppointmentStatus(new AppointmentStatusEventDto(appointmentId, newStatus))`; inject `SseService` into constructor
- [ ] `server/src/main/java/com/medour/controller/DoctorAppointmentController.java` -- inject `DoctorAppointmentService`; add `@PatchMapping("/{id}/status") updateStatus(@PathVariable Long id, @RequestBody StatusUpdateRequest req, Authentication auth)` → `service.updateStatus(id, parseUserId(auth), req.newStatus())` → 200 `Map.of("status", req.newStatus())`
- [ ] `server/src/main/java/com/medour/config/SecurityConfig.java` -- add `.requestMatchers(org.springframework.http.HttpMethod.PATCH, "/api/v1/appointments/doctor/**").hasRole("DOCTOR")` to the auth chain
- [ ] `server/src/test/java/com/medour/service/DoctorAppointmentServiceTest.java` -- inject `@Mock SseService`; add 3 tests: (1) cancel OPEN appointment → status CANCELED + `broadcastAppointmentStatus` called; (2) different doctor → 403 ResponseStatusException; (3) non-OPEN appointment → 409 ResponseStatusException
- [ ] `server/src/test/java/com/medour/controller/DoctorAppointmentControllerTest.java` -- add `@MockBean SseService`; add tests: (1) `@WithMockUser(username="1", roles="DOCTOR") PATCH /{id}/status` with `newStatus=CANCELED` → 200 + `$.status`; (2) `newStatus=OPEN` → 400
- [ ] `client/src/stores/appointmentStore.ts` -- add `updateDoctorAppointmentStatus(id: number, newStatus: string)`: call `api.patch(\`/appointments/doctor/\${id}/status\`, { newStatus })`; find appointment in `doctorAppointments` by id and set `status = newStatus`
- [ ] `client/src/views/DoctorAppointmentsView.vue` -- import `isJoinActive`; add `function cancelAppointment(id)`, `function completeAppointment(id)`, `function joinConsultation(url)` in script; on each OPEN card append: Cancel button (`v-if="appt.status === 'OPEN'"` calling `cancelAppointment(appt.id)`), Complete button (same condition), Join button (`v-if="appt.status === 'OPEN' && appt.wherebyRoomUrl"`, `:disabled="!isJoinActive(...)"`, calls `joinConsultation(appt.wherebyRoomUrl)`)

**Acceptance Criteria:**

- Given a doctor clicks Cancel on an OPEN appointment, the status changes to CANCELED locally and via SSE the patient's appointment history view updates in real time.
- Given a doctor clicks Complete on an OPEN appointment, the status changes to COMPLETED in the same way.
- Given a doctor clicks Join within the 10-minute window, the Whereby room opens in a new tab.
- Given a non-OPEN appointment, Cancel and Complete buttons are not shown.
- Given a doctor attempts to update an appointment they don't own, the server returns 403.
- Given an invalid newStatus value, the server returns 400.

## Verification

**Commands:**
- `cd server && ./mvnw test` -- expected: 63 tests pass (58 existing + 3 DoctorAppointmentServiceTest + 2 DoctorAppointmentControllerTest)
- `cd client && npm run build` -- expected: zero TypeScript errors

## Spec Change Log

**Review loop 1 patches applied:**
- `StatusUpdateRequest` — added `@NotBlank` on `newStatus`; `DoctorAppointmentController` `@PatchMapping` now uses `@Valid @RequestBody`
- `appointmentStore.updateDoctorAppointmentStatus` — narrowed type from `string` to `'CANCELED' | 'COMPLETED'`; added catch block writing to `errorMessage`
- `DoctorAppointmentsView` — added `connectAppointmentSse()` on mount and `disconnectAppointmentSse()` on unmount so the doctor's card list also receives real-time status updates (e.g., auto-cancel fires while doctor is on the page)
