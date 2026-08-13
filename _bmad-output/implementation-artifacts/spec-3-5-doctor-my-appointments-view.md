---
title: "Doctor My Appointments View"
type: "feature"
created: "2026-08-13"
status: "done"
review_loop_iteration: 0
baseline_commit: "fde73511e1862e42ba3b11a1b7367fb842437ff8"
context: []
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Doctors have no way to view their appointment schedule. No endpoint, view, or route exists for doctor appointment management.

**Approach:** Add `GET /api/v1/appointments/doctor/my` (DOCTOR-only) returning all the authenticated doctor's appointments; build `DoctorAppointmentsView.vue` at `/appointments/doctor` with Upcoming (OPEN) and Past (terminal-status) tabs; add `requiresDoctor` router meta and guard check; action buttons (Join, Cancel, Complete) are stubs in this story — Story 3.6 wires them.

## Boundaries & Constraints

**Always:**

- Only DOCTOR role accesses `/appointments/doctor` and `GET /api/v1/appointments/doctor/my`; Spring Security `hasRole("DOCTOR")` guards the endpoint
- Upcoming tab: OPEN appointments ordered by scheduledDate ascending (soonest first)
- Past tab: COMPLETED, CANCELED, AUTO_CANCELED appointments ordered by scheduledDate descending (most recent first)
- Card fields: scheduledDate, startTime, patientFirstName + patientSurname, patientRemoved badge (if patient.deletedAt != null), status badge (colours from epic context), createdAt small label, wherebyRoomUrl (for Story 3.6 Join button)
- `requiresDoctor: true` meta added to RouteMeta interface; guard: `if (to.meta.requiresDoctor && auth.user?.role !== 'DOCTOR') return '/'`
- Action buttons (Cancel, Complete, Join) are rendered as disabled placeholders in this story; Story 3.6 wires their logic
- Burger menu: add "My Appointments" link for DOCTOR role (beside Account Info, Change Password in the nav)

**Ask First:**

- (none)

**Never:**

- No status-change actions in this story (Story 3.6)
- PATIENT and ADMIN cannot access the doctor appointment view

## I/O & Edge-Case Matrix

| Scenario                     | Input / State                 | Expected Output / Behavior        | Error Handling  |
| ---------------------------- | ----------------------------- | --------------------------------- | --------------- |
| Doctor with appointments     | `GET /appointments/doctor/my` | 200 + list                        | N/A             |
| Patient soft-deleted         | patient.deletedAt != null     | Card shows name + "Removed" badge | N/A             |
| Non-doctor accesses endpoint | PATIENT or ADMIN JWT          | 403                               | Spring Security |

</frozen-after-approval>

## Code Map

- `server/src/main/java/com/medour/repository/AppointmentRepository.java` -- add `List<Appointment> findByDoctorIdOrderByScheduledDateAsc(Long doctorId)` and `findByDoctorIdOrderByScheduledDateDesc(Long doctorId)` — or a single `findByDoctorId(Long doctorId)` and sort client-side
- `server/src/main/java/com/medour/dto/DoctorAppointmentDto.java` -- NEW record: `Long id, LocalDate scheduledDate, LocalTime startTime, String patientFirstName, String patientSurname, boolean patientRemoved, String status, LocalDateTime createdAt, String wherebyRoomUrl`
- `server/src/main/java/com/medour/service/DoctorAppointmentService.java` -- NEW `@Service`; `@Transactional(readOnly=true) getAppointments(Long doctorId)` returns list of `DoctorAppointmentDto`
- `server/src/main/java/com/medour/controller/DoctorAppointmentController.java` -- NEW `@RestController("/api/v1/appointments/doctor")`; `GET /my` DOCTOR-only → 200 + list
- `server/src/main/java/com/medour/config/SecurityConfig.java` -- add `hasRole("DOCTOR")` for `GET /api/v1/appointments/doctor/my`
- `server/src/test/java/com/medour/service/DoctorAppointmentServiceTest.java` -- NEW: 2 tests — patientRemoved mapping
- `server/src/test/java/com/medour/controller/DoctorAppointmentControllerTest.java` -- NEW: 1 test — GET /my returns 200
- `client/src/stores/appointmentStore.ts` -- add `DoctorAppointment` interface; `doctorAppointments` state; `fetchDoctorAppointments()` action
- `client/src/router/index.ts` -- add `requiresDoctor?: boolean` to RouteMeta; guard check; `/appointments/doctor` route
- `client/src/App.vue` -- add "My Appointments" link for DOCTOR role in nav
- `client/src/views/DoctorAppointmentsView.vue` -- NEW: Upcoming/Past tabs; appointment cards with all required fields; disabled action button stubs

## Tasks & Acceptance

**Execution:**

- [ ] `server/src/main/java/com/medour/repository/AppointmentRepository.java` -- add `List<Appointment> findByDoctorId(Long doctorId)`
- [ ] `server/src/main/java/com/medour/dto/DoctorAppointmentDto.java` -- NEW record: `Long id, LocalDate scheduledDate, LocalTime startTime, String patientFirstName, String patientSurname, boolean patientRemoved, String status, LocalDateTime createdAt, String wherebyRoomUrl`
- [ ] `server/src/main/java/com/medour/service/DoctorAppointmentService.java` -- NEW `@Service`; inject `AppointmentRepository`; `@Transactional(readOnly=true) getAppointments(Long doctorId)`: call `findByDoctorId(doctorId)`, map each to `DoctorAppointmentDto` with `patientRemoved = a.getPatient().getDeletedAt() != null`
- [ ] `server/src/main/java/com/medour/controller/DoctorAppointmentController.java` -- NEW `@RestController @RequestMapping("/api/v1/appointments/doctor")`; inject `DoctorAppointmentService`; `@GetMapping("/my") getAppointments(Authentication auth)` → `ResponseEntity.ok(service.getAppointments(parseUserId(auth)))`; private `parseUserId` helper
- [ ] `server/src/main/java/com/medour/config/SecurityConfig.java` -- add `.requestMatchers(GET, "/api/v1/appointments/doctor/my").hasRole("DOCTOR")` before `anyRequest().authenticated()`
- [ ] `server/src/test/java/com/medour/service/DoctorAppointmentServiceTest.java` -- NEW: test active patient → patientRemoved=false; test deleted patient → patientRemoved=true
- [ ] `server/src/test/java/com/medour/controller/DoctorAppointmentControllerTest.java` -- NEW `@WebMvcTest(DoctorAppointmentController.class) @AutoConfigureMockMvc(addFilters=false)`; `@MockBean DoctorAppointmentService`; `@MockBean JwtUtil`; `@WithMockUser(username="1", roles="DOCTOR") GET /doctor/my` → 200 + list
- [ ] `client/src/stores/appointmentStore.ts` -- add `interface DoctorAppointment { id, scheduledDate, startTime, patientFirstName, patientSurname, patientRemoved, status, createdAt, wherebyRoomUrl: string|null }`; add `doctorAppointments: DoctorAppointment[]` state; add `fetchDoctorAppointments()` action calling `api.get('/appointments/doctor/my')`
- [ ] `client/src/router/index.ts` -- add `requiresDoctor?: boolean` to RouteMeta; add guard `if (to.meta.requiresDoctor && auth.user?.role !== 'DOCTOR') return '/'`; add `{ path: '/appointments/doctor', component: ..., meta: { requiresAuth: true, requiresDoctor: true } }`
- [ ] `client/src/App.vue` -- add `<RouterLink v-else-if="authStore.user?.role === 'DOCTOR'" to="/appointments/doctor">My Appointments</RouterLink>` in the nav alongside Account Info (for DOCTOR role only)
- [ ] `client/src/views/DoctorAppointmentsView.vue` -- NEW: `activeTab` ref ('upcoming'|'past'); computed `upcoming` = appointments where status=OPEN; computed `past` = others; render two tab buttons + conditional card list per tab; each card shows patientFirstName+surname + patientRemoved badge, date/time, status badge, createdAt; disabled "Join"/"Cancel"/"Complete" buttons as placeholders (Story 3.6 enables them)

**Acceptance Criteria:**

- Given a DOCTOR user, when `/appointments/doctor` is visited, all appointments are visible split into Upcoming and Past tabs.
- Given an appointment whose patient is soft-deleted, the card shows the patient's name and a "Removed" badge.
- Given a PATIENT or ADMIN user, when `GET /api/v1/appointments/doctor/my` is called, the server returns 403.
- Given a non-doctor user navigates to `/appointments/doctor`, the router guard redirects to `/`.

## Verification

**Commands:**

- `cd server && ./mvnw test` -- expected: 58 tests pass (55 existing + 2 DoctorAppointmentServiceTest + 1 DoctorAppointmentControllerTest)
- `cd client && npm run test` -- expected: 36 existing + 2 new router guard tests for /appointments/doctor = 38
- `cd client && npm run build` -- expected: zero TypeScript errors

## Spec Change Log

**Review loop 1 patches applied:**

- `AppointmentRepository` — `findByDoctorId` renamed to `findByDoctorIdOrderByScheduledDateAsc` so results have deterministic order
- `DoctorAppointmentService` — uses ordered query method
- `DoctorAppointmentServiceTest` — both tests now use `findByDoctorIdOrderByScheduledDateAsc`; deleted-patient test adds `createdAt` and `wherebyRoomUrl` to fixture
- `DoctorAppointmentsView.vue` — removed disabled Join/Cancel/Complete stub buttons (Story 3.6 adds them); added `errorMessage` ref and catch block for fetch failures; error message rendered in template
