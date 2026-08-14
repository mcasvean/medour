---
title: "Rating Submission on Completed Appointments"
type: "feature"
created: "2026-08-14"
status: "done"
review_loop_iteration: 0
baseline_commit: "74d1df52704b06592f93a1eff9b8d54ae3c6c649"
context: []
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Patients cannot currently rate their doctors after a completed appointment, leaving no quality signal for other patients or for doctors.

**Approach:** Add a `ratings` table and JPA entity, expose `POST /api/v1/ratings`, extend `PatientAppointmentDto` with the patient's existing rating (if any), and render a 1–10 numeric input on Completed appointment cards in the patient history view that persists the rating and recalculates `users.average_rating`.

## Boundaries & Constraints

**Always:**

- One row per appointment in `ratings`; inserting when a row for that `appointment_id` already exists is an error (use 5-2 for edits).
- Rating value must be an integer 1–10 inclusive; reject outside range with 400.
- Only the authenticated patient whose `id` matches the appointment's `patient_id` may submit a rating; all other callers receive 403.
- After every successful insert, recalculate `ROUND(AVG(value), 1)` from all ratings for that doctor and persist to `users.average_rating`.
- The `ratings` schema is auto-created from the JPA entity (Hibernate ddl-auto: update) — no SQL migration file needed.
- Rating input is shown **only** on cards where `status === 'COMPLETED'`.
- `@Transactional` on the service method that writes rating + updates average.

**Ask First:** None identified.

**Never:**

- Do not allow a doctor or admin to submit a rating (403).
- Do not show a rating input on non-Completed appointment cards.
- Do not hard-delete appointment rows in this story.
- Do not expose `average_rating` recalculation to the client — server computes it.

## I/O & Edge-Case Matrix

| Scenario                   | Input / State                                                                                  | Expected Output / Behavior                                          | Error Handling                                    |
| -------------------------- | ---------------------------------------------------------------------------------------------- | ------------------------------------------------------------------- | ------------------------------------------------- |
| New rating                 | Patient POSTs `{appointmentId, value: 7}` for their COMPLETED appointment with no prior rating | 201; `ratings` row inserted; `users.average_rating` updated         | —                                                 |
| Re-open already-rated card | Patient GETs `/appointments/my`                                                                | `PatientAppointmentDto` includes `ratingValue: 7`, `ratingId: <id>` | —                                                 |
| Value out of range         | `value: 0` or `value: 11`                                                                      | 400                                                                 | Validation error                                  |
| Duplicate submission       | Patient POSTs for same `appointmentId` again                                                   | 409 Conflict                                                        | Return error                                      |
| Wrong patient              | Patient B POSTs for Patient A's appointment                                                    | 403                                                                 | —                                                 |
| Doctor/admin caller        | Any role other than PATIENT                                                                    | 403                                                                 | —                                                 |
| Non-COMPLETED appointment  | Patient POSTs for an OPEN appointment                                                          | 400                                                                 | Reject — only completed appointments may be rated |

</frozen-after-approval>

## Code Map

- `server/src/main/java/com/medour/model/Rating.java` -- **CREATE** — JPA entity: `id`, `appointment` (ManyToOne), `patient` (ManyToOne User), `doctor` (ManyToOne User), `value` (Integer)
- `server/src/main/java/com/medour/repository/RatingRepository.java` -- **CREATE** — `findByAppointmentId(Long)`, `findAverageValueByDoctorId(Long)` (JPQL `SELECT ROUND(AVG(r.value), 1)`)
- `server/src/main/java/com/medour/service/RatingService.java` -- **CREATE** — `submitRating(Long appointmentId, int value, Long callerPatientId)`: validate ownership, status, duplicates; insert; recalculate and persist `user.averageRating`
- `server/src/main/java/com/medour/controller/RatingController.java` -- **CREATE** — `POST /api/v1/ratings` → 201; secured to ROLE_PATIENT
- `server/src/main/java/com/medour/dto/SubmitRatingRequest.java` -- **CREATE** — `appointmentId: Long`, `value: int` (validated `@Min(1) @Max(10)`)
- `server/src/main/java/com/medour/dto/RatingResponse.java` -- **CREATE** — `id: Long`, `value: int`
- `server/src/main/java/com/medour/dto/PatientAppointmentDto.java` -- **EXTEND** — add `ratingValue: Integer` (nullable), `ratingId: Long` (nullable)
- `server/src/main/java/com/medour/service/PatientAppointmentService.java` -- **EXTEND** — when mapping appointments to DTOs, left-join ratings to populate `ratingValue`/`ratingId`
- `server/src/main/java/com/medour/model/User.java` -- read-only reference — `averageRating` field (BigDecimal) already exists at line 59
- `server/src/main/java/com/medour/model/Appointment.java` -- read-only reference — `status` is `AppointmentStatus` enum; `COMPLETED` value already present
- `client/src/stores/appointmentStore.ts` -- **EXTEND** — add `ratingValue: number | null`, `ratingId: number | null` to `PatientAppointment` interface; add `submitRating(appointmentId, value)` action that calls `api.post('/ratings', {appointmentId, value})` and updates the matching appointment in state
- `client/src/views/PatientAppointmentsView.vue` -- **EXTEND** — for each card where `status === 'COMPLETED'`: render numeric input (1–10) pre-filled with `appointment.ratingValue ?? ''`; Save button calls `appointmentStore.submitRating`; disable input/button after successful submission (ratingId becomes non-null)

## Tasks & Acceptance

**Execution:**

- [x] `server/src/main/java/com/medour/model/Rating.java` -- CREATE entity with `@Entity @Table(name="ratings")`, fields: `id`, `appointment (ManyToOne)`, `patient (ManyToOne)`, `doctor (ManyToOne)`, `value INTEGER` -- source of truth for all ratings; Hibernate auto-creates table
- [x] `server/src/main/java/com/medour/repository/RatingRepository.java` -- CREATE `JpaRepository<Rating, Long>` with `findByAppointmentId(Long)` and JPQL `SELECT ROUND(AVG(r.value), 1) FROM Rating r WHERE r.doctor.id = :doctorId` -- enables ownership check and average calculation
- [x] `server/src/main/java/com/medour/dto/SubmitRatingRequest.java` -- CREATE record/class with `appointmentId: Long`, `value: int` annotated `@Min(1) @Max(10)` -- validated request body
- [x] `server/src/main/java/com/medour/dto/RatingResponse.java` -- CREATE with `id: Long`, `value: int` -- response body for 201
- [x] `server/src/main/java/com/medour/service/RatingService.java` -- CREATE `@Transactional submitRating(appointmentId, value, callerPatientId)`: load appointment; assert `appointment.patient.id == callerPatientId` (else 403); assert `status == COMPLETED` (else 400); assert no existing rating (else 409); save Rating; recalculate average via repository; persist to `user.averageRating`; return saved entity -- enforces all business rules atomically
- [x] `server/src/main/java/com/medour/controller/RatingController.java` -- CREATE `POST /api/v1/ratings` calling `ratingService.submitRating`; annotate `@PreAuthorize("hasRole('PATIENT')")` -- exposes endpoint secured to patients only
- [x] `server/src/main/java/com/medour/dto/PatientAppointmentDto.java` -- EXTEND — add nullable `ratingValue: Integer` and `ratingId: Long` fields -- surfaces existing rating to the client
- [x] `server/src/main/java/com/medour/service/PatientAppointmentService.java` -- EXTEND DTO mapping — for each appointment, call `ratingRepository.findByAppointmentId(appointment.id)` and set `ratingValue`/`ratingId` if present -- populates pre-fill data
- [x] `client/src/stores/appointmentStore.ts` -- EXTEND `PatientAppointment` interface with `ratingValue: number | null` and `ratingId: number | null`; add `submitRating(appointmentId: number, value: number)` that POSTs and mutates the matching item in `patientAppointments` -- keeps UI in sync without re-fetch
- [x] `client/src/views/PatientAppointmentsView.vue` -- ADD conditional rating widget: `v-if="appointment.status === 'COMPLETED'"`, numeric input bound to local ref pre-filled from `appointment.ratingValue`, Save button disabled when `appointment.ratingId !== null`; on submit call `appointmentStore.submitRating` -- patient-facing UI for the feature

**Acceptance Criteria:**

- Given a patient with a COMPLETED appointment and no prior rating, when they open the history page, then a 1–10 numeric input and Save button are visible on that card.
- Given a patient with a COMPLETED appointment and a prior rating, when they open the history page, then the input shows the existing value and the Save button is disabled.
- Given a patient submits a valid rating (1–10), then the server returns 201, a `ratings` row is inserted, and `users.average_rating` reflects the updated average.
- Given a value outside 1–10, the server returns 400.
- Given a duplicate submission for the same appointment, the server returns 409.
- Given a doctor or admin calls `POST /api/v1/ratings`, the server returns 403.
- Given a patient calls `POST /api/v1/ratings` for another patient's appointment, the server returns 403.
- Given an appointment whose status is not COMPLETED, the server returns 400.
- Given an appointment with non-COMPLETED status, no rating input is rendered on the card.

## Verification

**Commands:**

- `cd server && ./mvnw test` -- expected: all tests pass, no compile errors
- `cd client && npm run build` -- expected: TypeScript compile succeeds with no errors
- `cd client && npm run test` -- expected: all vitest tests pass

## Suggested Review Order

**Schema & data model**

- `@UniqueConstraint` on `appointment_id` enforces one-row-per-appointment at DB level.
  [`Rating.java:14`](../../server/src/main/java/com/medour/model/Rating.java#L14)

**Business logic**

- `submitRating` enforces ownership, COMPLETED status, and duplicate prevention atomically.
  [`RatingService.java:33`](../../server/src/main/java/com/medour/service/RatingService.java#L33)

- `DataIntegrityViolationException` catch maps concurrent-insert race to 409.
  [`RatingService.java:56`](../../server/src/main/java/com/medour/service/RatingService.java#L56)

- Average recalculation persisted to `users.average_rating` after every insert.
  [`RatingService.java:64`](../../server/src/main/java/com/medour/service/RatingService.java#L64)

**API surface**

- `POST /api/v1/ratings` entry point; `@PreAuthorize` + URL-level security.
  [`RatingController.java:24`](../../server/src/main/java/com/medour/controller/RatingController.java#L24)

- URL-level `hasRole("PATIENT")` added for `/api/v1/ratings`.
  [`SecurityConfig.java:47`](../../server/src/main/java/com/medour/config/SecurityConfig.java#L47)

- `PatientAppointmentDto` extended with `ratingValue` / `ratingId` for prefill.
  [`PatientAppointmentDto.java:14`](../../server/src/main/java/com/medour/dto/PatientAppointmentDto.java#L14)

- History mapping left-joins rating per appointment.
  [`PatientAppointmentService.java:30`](../../server/src/main/java/com/medour/service/PatientAppointmentService.java#L30)

**UI**

- Rating widget conditionally rendered only on COMPLETED cards; disabled after submit.
  [`PatientAppointmentsView.vue:41`](../../client/src/views/PatientAppointmentsView.vue#L41)

- `saveRating`: in-flight guard, NaN/range check, error feedback.
  [`PatientAppointmentsView.vue:79`](../../client/src/views/PatientAppointmentsView.vue#L79)

- `submitRating` store action: POSTs and mutates `ratingValue`/`ratingId` in state.
  [`appointmentStore.ts:185`](../../client/src/stores/appointmentStore.ts#L185)

**Tests**

- Service: happy path, wrong-patient 403, non-COMPLETED 400, duplicate 409, not-found 404, average_rating verify.
  [`RatingServiceTest.java:61`](../../server/src/test/java/com/medour/service/RatingServiceTest.java#L61)

- Controller: 201, below-min 400, above-max 400, duplicate 409, wrong-patient 403, unauthenticated 401.
  [`RatingControllerTest.java:58`](../../server/src/test/java/com/medour/controller/RatingControllerTest.java#L58)

- Store action: POST call, state mutation on success, rejection propagated on failure.
  [`appointmentStore.test.ts:301`](../../client/src/stores/__tests__/appointmentStore.test.ts#L301)
