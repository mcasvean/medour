---
title: "Patient Appointment History View"
type: "feature"
created: "2026-08-13"
status: "done"
review_loop_iteration: 0
baseline_commit: "6ef34c02794638b8239245ba0317b9214120b556"
context: []
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Patients have no way to view their booked appointments after booking them in Story 2.4. The appointment history endpoint does not exist, and the patient appointment history view is missing from the router.

**Approach:** Add `GET /api/v1/appointments/my` returning all the authenticated patient's appointments (ordered newest-first) with doctor details; build `PatientAppointmentsView.vue` at `/appointments` displaying appointment cards with date, time, doctor name, speciality, status badge, and createdAt; show a "Removed" badge alongside the doctor name when the doctor is soft-deleted.

## Boundaries & Constraints

**Always:**

- Only PATIENT role accesses `/appointments` and `GET /api/v1/appointments/my`; Spring Security `hasRole("PATIENT")` guards the endpoint
- All appointments (past and future) are returned — no filtering in this story
- Appointment card fields: scheduledDate, startTime, doctor firstName + surname, doctor speciality, status badge (colour-coded), createdAt (small corner label)
- If `appointment.doctor.deletedAt != null`, render a "Removed" badge next to the doctor name; the name is still shown
- Status badge colours: OPEN=blue, COMPLETED=green, CANCELED=orange, AUTO_CANCELED=grey
- `AppointmentRepository` gains `findByPatientIdOrderByScheduledDateDesc(Long patientId)` to fetch history
- The `/appointments` route requires `{ requiresAuth: true, requiresPatient: true }` — the router guard already enforces this via the existing `requiresPatient` meta flag

**Ask First:**

- (none)

**Never:**

- No pagination in this story
- No real-time updates in this story (Story 3.2)
- No Join button in this story (Story 3.3)
- No doctor appointment view in this story (Story 3.5)

## I/O & Edge-Case Matrix

| Scenario                      | Input / State                | Expected Output / Behavior                          | Error Handling  |
| ----------------------------- | ---------------------------- | --------------------------------------------------- | --------------- |
| Patient with appointments     | `GET /appointments/my`       | 200 + list of `PatientAppointmentDto`, newest first | N/A             |
| Patient with no appointments  | `GET /appointments/my`       | 200 + `[]`                                          | N/A             |
| Doctor with soft-deleted      | Doctor's `deletedAt != null` | Card shows doctor name + "Removed" badge            | N/A             |
| Non-patient accesses endpoint | DOCTOR or ADMIN JWT          | 403                                                 | Spring Security |

</frozen-after-approval>

## Code Map

- `server/src/main/java/com/medour/repository/AppointmentRepository.java` -- add `List<Appointment> findByPatientIdOrderByScheduledDateDesc(Long patientId)`
- `server/src/main/java/com/medour/dto/PatientAppointmentDto.java` -- NEW record: id, scheduledDate, startTime, doctorFirstName, doctorSurname, doctorSpeciality, doctorRemoved (boolean), status (String), createdAt
- `server/src/main/java/com/medour/service/PatientAppointmentService.java` -- NEW `@Service`; `getHistory(Long patientId)` → fetch + map to `PatientAppointmentDto`
- `server/src/main/java/com/medour/controller/PatientAppointmentController.java` -- NEW `@RestController("/api/v1/appointments")`; `GET /my` PATIENT-only → 200 + list
- `server/src/main/java/com/medour/config/SecurityConfig.java` -- add `hasRole("PATIENT")` for `GET /api/v1/appointments/my`
- `server/src/test/java/com/medour/controller/PatientAppointmentControllerTest.java` -- NEW; 2 tests: list returns 200; non-patient returns 403
- `client/src/stores/appointmentStore.ts` -- add `patientAppointments: PatientAppointment[]` state; `fetchPatientAppointments()` action
- `client/src/router/index.ts` -- add `/appointments` route with `{ requiresAuth: true, requiresPatient: true }`
- `client/src/views/PatientAppointmentsView.vue` -- NEW: fetches on mount; renders appointment cards with all required fields; empty-state message

## Tasks & Acceptance

**Execution:**

- [ ] `server/src/main/java/com/medour/repository/AppointmentRepository.java` -- add `List<Appointment> findByPatientIdOrderByScheduledDateDesc(Long patientId)`
- [ ] `server/src/main/java/com/medour/dto/PatientAppointmentDto.java` -- NEW record: `Long id, LocalDate scheduledDate, LocalTime startTime, String doctorFirstName, String doctorSurname, String doctorSpeciality, boolean doctorRemoved, String status, LocalDateTime createdAt`
- [ ] `server/src/main/java/com/medour/service/PatientAppointmentService.java` -- NEW `@Service`; `@Transactional(readOnly=true) getHistory(Long patientId)`: call `findByPatientIdOrderByScheduledDateDesc(patientId)`; map each to `PatientAppointmentDto` where `doctorRemoved = appointment.getDoctor().getDeletedAt() != null`
- [ ] `server/src/main/java/com/medour/controller/PatientAppointmentController.java` -- NEW `@RestController @RequestMapping("/api/v1/appointments")`; inject `PatientAppointmentService`; `@GetMapping("/my") getHistory(Authentication auth)` → `ResponseEntity.ok(service.getHistory(parseUserId(auth)))`; include private `parseUserId` helper (same pattern as UserController)
- [ ] `server/src/main/java/com/medour/config/SecurityConfig.java` -- add `.requestMatchers(GET, "/api/v1/appointments/my").hasRole("PATIENT")` before `anyRequest().authenticated()`
- [ ] `server/src/test/java/com/medour/controller/PatientAppointmentControllerTest.java` -- NEW `@WebMvcTest(PatientAppointmentController.class) @AutoConfigureMockMvc(addFilters=false)`; `@MockBean PatientAppointmentService`; `@MockBean JwtUtil`; test (1): `@WithMockUser(username="1") GET /my` → 200 + list size; test (2): `@WithMockUser(roles="DOCTOR") GET /my` → 403 (with filters enabled via separate test class or security-test annotations if needed — if too complex, just verify response shape for the happy path)
- [ ] `client/src/stores/appointmentStore.ts` -- add `interface PatientAppointment { id: number, scheduledDate: string, startTime: string, doctorFirstName: string, doctorSurname: string, doctorSpeciality: string, doctorRemoved: boolean, status: string, createdAt: string }`; add `patientAppointments: PatientAppointment[]` to state; add action `fetchPatientAppointments()` calling `api.get('/appointments/my')`
- [ ] `client/src/router/index.ts` -- add `{ path: '/appointments', component: () => import('../views/PatientAppointmentsView.vue'), meta: { requiresAuth: true, requiresPatient: true } }`
- [ ] `client/src/views/PatientAppointmentsView.vue` -- NEW: on mount call `appointmentStore.fetchPatientAppointments()`; render list of appointment cards showing scheduledDate, startTime, doctorFirstName+surname (+ "Removed" badge if `doctorRemoved`), doctorSpeciality, status badge with `:class` based on status value, createdAt in small corner label; show empty-state message when list is empty

**Acceptance Criteria:**

- Given a patient with booked appointments, when `/appointments` is visited, all appointments appear with correct date, time, doctor name, and status badge.
- Given an appointment whose doctor is soft-deleted, the card shows the doctor's name and a "Removed" badge.
- Given a patient with no appointments, an empty-state message is displayed.
- Given a DOCTOR or ADMIN user, when `GET /api/v1/appointments/my` is called, the server returns 403.
- Given the status is OPEN/COMPLETED/CANCELED/AUTO_CANCELED, the badge renders with the corresponding colour class.

## Verification

**Commands:**

- `cd server && ./mvnw test` -- expected: 47 tests pass (45 existing + 2 PatientAppointmentControllerTest)
- `cd client && npm run test` -- expected: all 26 tests pass; 1 new router guard test for /appointments
- `cd client && npm run build` -- expected: zero TypeScript errors

## Spec Change Log

**Review loop 1 patches applied:**

- `PatientAppointmentServiceTest.java` — NEW: 2 unit tests verifying `doctorRemoved=false` for active doctor and `doctorRemoved=true` for soft-deleted doctor
- `router/__tests__/index.test.ts` — added 2 guard tests: unauthenticated → /login; DOCTOR → / for `/appointments` route
- `appointmentStore.test.ts` — added `fetchPatientAppointments` test verifying API call and state population
