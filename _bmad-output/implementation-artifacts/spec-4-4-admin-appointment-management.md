---
title: "Admin Appointment Management"
type: "feature"
created: "2026-08-13"
status: "done"
review_loop_iteration: 0
baseline_commit: "a26db28778eaca3d89284e8ed7d51596e55ad5d3"
context: []
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** The admin has no way to view or delete appointments. The admin panel currently only covers user management.

**Approach:** Add `GET /api/v1/admin/appointments` and `DELETE /api/v1/admin/appointments/{id}` endpoints backed by a new `AdminAppointmentService`; create a new `AdminAppointmentsView` on the client showing all appointments with a per-row delete-with-confirmation action; register the route and add a nav link for the admin.

## Boundaries & Constraints

**Always:**

- The endpoints are auto-protected by the existing SecurityConfig `/api/v1/admin/**` rule — no additional SecurityConfig change
- Appointment deletion is a hard-delete (`appointmentRepository.deleteById(id)`) — admin-only and intentional per epic spec
- Client shows a confirmation step before the API call using `window.confirm()`
- After successful deletion, `appointmentStore.fetchAdminAppointments()` is called to refresh the list; wrap the refresh in its own try/catch so a refresh failure does not propagate as a delete failure
- The `/admin/appointments` route uses `meta: { requiresAuth: true, requiresAdmin: true }` — both Vue Router guard and Spring Security enforce independently

**Ask First:**

- (none)

**Never:**

- No soft-delete on appointments — the hard-delete is intentional
- No appointment creation or editing in this story
- No patient-side or doctor-side delete capability

## I/O & Edge-Case Matrix

| Scenario                        | Input / State                                    | Expected Output / Behavior                                         | Error Handling |
| ------------------------------- | ------------------------------------------------ | ------------------------------------------------------------------ | -------------- |
| List all appointments           | `GET /api/v1/admin/appointments` (ADMIN)         | 200 + array of `AdminAppointmentDto`, sorted by scheduledDate DESC | N/A            |
| Delete existing appointment     | `DELETE /api/v1/admin/appointments/{id}` (ADMIN) | 204; row removed from DB                                           | N/A            |
| Delete non-existent appointment | Unknown id                                       | 404                                                                | N/A            |
| Cancel confirmation             | User clicks Cancel                               | No API call; list unchanged                                        | N/A            |
| Non-admin access                | PATIENT or DOCTOR JWT                            | 403; Vue Router also redirects non-admin to `/`                    | N/A            |

</frozen-after-approval>

## Code Map

- `server/src/main/java/com/medour/dto/AdminAppointmentDto.java` -- NEW record: id, patientName, doctorName, scheduledDate, startTime, status, wherebyRoomUrl
- `server/src/main/java/com/medour/service/AdminAppointmentService.java` -- NEW @Service: inject AppointmentRepository; map Appointment → AdminAppointmentDto
- `server/src/main/java/com/medour/controller/AdminController.java` -- existing; add AdminAppointmentService dep + 2 new endpoints
- `server/src/main/java/com/medour/repository/AppointmentRepository.java` -- extends JpaRepository; inherits `findAll(Sort)` — no new methods needed
- `server/src/main/java/com/medour/model/Appointment.java` -- entity; fields used: id, patient (User), doctor (User), scheduledDate, startTime, status (AppointmentStatus enum), wherebyRoomUrl
- `server/src/test/java/com/medour/service/AdminAppointmentServiceTest.java` -- NEW: 2 unit tests
- `server/src/test/java/com/medour/controller/AdminControllerTest.java` -- existing; add 2 new tests for appointment endpoints
- `client/src/stores/appointmentStore.ts` -- existing Pinia store; add AdminAppointmentDto interface + adminAppointments state + 2 actions
- `client/src/views/AdminAppointmentsView.vue` -- NEW: table view with delete-with-confirmation
- `client/src/router/index.ts` -- existing routes array; add `/admin/appointments` route
- `client/src/App.vue` -- existing admin nav; add Appointments link after Users link

## Tasks & Acceptance

**Execution:**

- [x] `server/src/main/java/com/medour/dto/AdminAppointmentDto.java` -- NEW Java record: `public record AdminAppointmentDto(Long id, String patientName, String doctorName, java.time.LocalDate scheduledDate, java.time.LocalTime startTime, String status, String wherebyRoomUrl) {}`
- [x] `server/src/main/java/com/medour/service/AdminAppointmentService.java` -- NEW `@Service`: inject `AppointmentRepository`; `@Transactional(readOnly=true) public List<AdminAppointmentDto> getAllAppointments()` calls `appointmentRepository.findAll(Sort.by(Sort.Direction.DESC, "scheduledDate", "startTime"))` and maps each appointment to DTO (patientName = `patient.getFirstName() + " " + patient.getSurname()`; status = `appointment.getStatus().name()`); `@Transactional public void deleteAppointment(Long id)` finds by id (`findById` + throw 404 `ResponseStatusException` if absent), then `appointmentRepository.deleteById(id)`
- [x] `server/src/main/java/com/medour/controller/AdminController.java` -- add `AdminAppointmentService adminAppointmentService` constructor arg; add `@GetMapping("/appointments") ResponseEntity<List<AdminAppointmentDto>> getAppointments()` → `ResponseEntity.ok(adminAppointmentService.getAllAppointments())`; add `@DeleteMapping("/appointments/{id}") ResponseEntity<Void> deleteAppointment(@PathVariable Long id)` → service call → `ResponseEntity.noContent().build()`
- [x] `server/src/test/java/com/medour/service/AdminAppointmentServiceTest.java` -- NEW @ExtendWith(MockitoExtension.class): mock `AppointmentRepository`; test `getAllAppointments_returnsMappedDtos`: stub `findAll(Sort)` with one Appointment → verify returned DTO has correct patientName and status string; test `deleteAppointment_unknownId_throws404`: stub `findById` returning empty → verify `ResponseStatusException` with 404 status
- [x] `server/src/test/java/com/medour/controller/AdminControllerTest.java` -- add 2 tests (mock `AdminAppointmentService`): `getAppointments_asAdmin_returns200WithArray`: stub returns one DTO → `@WithMockUser(roles="ADMIN") GET /api/v1/admin/appointments` → 200 + JSON array; `deleteAppointment_asAdmin_returns204`: `@WithMockUser(roles="ADMIN") DELETE /api/v1/admin/appointments/1 with csrf` → 204
- [x] `client/src/stores/appointmentStore.ts` -- add `export interface AdminAppointmentDto { id: number; patientName: string; doctorName: string; scheduledDate: string; startTime: string; status: string; wherebyRoomUrl: string | null }`; add `adminAppointments: [] as AdminAppointmentDto[]` to state; add `async fetchAdminAppointments()`: `const res = await api.get<AdminAppointmentDto[]>('/admin/appointments'); this.adminAppointments = res.data`; add `async deleteAdminAppointment(id: number)`: `await api.delete('/admin/appointments/' + id)` then `try { await this.fetchAdminAppointments() } catch { /* ignore refresh failure */ }`
- [x] `client/src/views/AdminAppointmentsView.vue` -- NEW: on `onMounted` call `appointmentStore.fetchAdminAppointments()`; render table with columns: Patient, Doctor, Date, Time, Status, Actions; per-row Delete button calls `async function deleteAppointment(appt)`: `if (!window.confirm('Delete appointment for ' + appt.patientName + '?')) return`; try `await appointmentStore.deleteAdminAppointment(appt.id)` catch: set `deleteError`; show `deleteError` inline above table
- [x] `client/src/router/index.ts` -- add `{ path: '/admin/appointments', component: () => import('../views/AdminAppointmentsView.vue'), meta: { requiresAuth: true, requiresAdmin: true } }` to the routes array (after the `/admin/users` entry)
- [x] `client/src/App.vue` -- add `<RouterLink v-if="isAdmin" to="/admin/appointments">Appointments</RouterLink>` after the existing `<RouterLink v-if="isAdmin" to="/admin/users">Users</RouterLink>` in the burger nav

**Acceptance Criteria:**

- Given the admin navigates to `/admin/appointments`, all appointments are displayed with patient name, doctor name, date, time, and status.
- Given the admin clicks Delete and confirms, the appointment row is removed from the DB and the list refreshes.
- Given the admin clicks Delete and cancels, no API call is made and the list is unchanged.
- Given a non-existent appointment id is deleted via API, a 404 is returned.
- Given a PATIENT JWT calls `GET /api/v1/admin/appointments`, the server returns 403.
- Given a non-admin user navigates to `/admin/appointments`, the Vue Router guard redirects to `/`.

## Verification

**Commands:**

- `cd server && ./mvnw test` -- expected: all tests pass (including 2 new AdminAppointmentServiceTest + 2 new AdminControllerTest)
- `cd client && npm run build` -- expected: zero TypeScript errors

## Suggested Review Order

**Security & API contract**

- SecurityConfig blanket `/api/v1/admin/**` rule already protects both endpoints — no new config needed
  [`AdminController.java:57`](../../server/src/main/java/com/medour/controller/AdminController.java#L57)

- Hard-delete is intentional for admin; 404 on unknown id before deleteById
  [`AdminAppointmentService.java:22`](../../server/src/main/java/com/medour/service/AdminAppointmentService.java#L22)

- DTO record — fields sent over wire; wherebyRoomUrl included for completeness
  [`AdminAppointmentDto.java:6`](../../server/src/main/java/com/medour/dto/AdminAppointmentDto.java#L6)

**Client guard & routing**

- Both requiresAuth + requiresAdmin meta on the new route; guard already handles these flags
  [`router/index.ts:36`](../../client/src/router/index.ts#L36)

- Appointments nav link rendered only for admin via isAdmin computed
  [`App.vue:8`](../../client/src/App.vue#L8)

**View & store**

- onMounted with try/catch for load failure; separate fetchError displayed above table
  [`AdminAppointmentsView.vue:35`](../../client/src/views/AdminAppointmentsView.vue#L35)

- window.confirm guard; deleteError isolated from refresh failure
  [`AdminAppointmentsView.vue:46`](../../client/src/views/AdminAppointmentsView.vue#L46)

- deleteAdminAppointment wraps refresh in try/catch — refresh failure does not mask delete success
  [`appointmentStore.ts:191`](../../client/src/stores/appointmentStore.ts#L191)

**Tests**

- Exact Sort matcher + verify; doctorName assertion; happy-path deleteById verify
  [`AdminAppointmentServiceTest.java:38`](../../server/src/test/java/com/medour/service/AdminAppointmentServiceTest.java#L38)

- Controller: GET 200 + DELETE 204 with CSRF and ADMIN role
  [`AdminControllerTest.java:112`](../../server/src/test/java/com/medour/controller/AdminControllerTest.java#L112)

- Router guard tests for /admin/appointments (PATIENT → /; ADMIN → stays)
  [`router/__tests__/index.test.ts:119`](../../client/src/router/__tests__/index.test.ts#L119)

- Store: fetchAdminAppointments populates state; deleteAdminAppointment refresh-failure isolation
  [`appointmentStore.test.ts:290`](../../client/src/stores/__tests__/appointmentStore.test.ts#L290)
