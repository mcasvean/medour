---
title: "Rating Editing"
type: "feature"
created: "2026-08-14"
status: "done"
review_loop_iteration: 0
baseline_commit: "205353212a08fd01647cc8ffc6613f653a670b06"
context: []
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Patients can submit a rating once but have no way to correct it if their assessment changes; the UI permanently disables the input after the first submission.

**Approach:** Add `PATCH /api/v1/ratings/{id}` for updating an existing rating; update the store to call PATCH when a `ratingId` exists; remove the disabled-when-rated constraint from the UI so patients can always adjust their rating.

## Boundaries & Constraints

**Always:**

- Only the patient whose `patient_id` matches the rating row may update it; anyone else receives 403.
- Doctors and admins calling the PATCH endpoint receive 403.
- Value must be an integer 1–10 inclusive; reject outside range with 400.
- On every successful update, recalculate `ROUND(AVG(value), 1)` and persist to `users.average_rating`.
- `@Transactional` on the service method that writes rating + updates average.
- The input field and Save button on the rating widget must remain enabled for COMPLETED appointments regardless of whether a rating has already been submitted.

**Ask First:** None identified.

**Never:**

- Do not allow updating a rating for an appointment that belongs to a different patient.
- Do not create a new ratings row via the PATCH endpoint — only update an existing one.
- Do not add a separate "edit mode" toggle; the widget is always in editable state for COMPLETED appointments.

## I/O & Edge-Case Matrix

| Scenario                 | Input / State                                         | Expected Output / Behavior                                        | Error Handling   |
| ------------------------ | ----------------------------------------------------- | ----------------------------------------------------------------- | ---------------- |
| Patient edits own rating | Patient PATCHes `/ratings/{id}` with `{value: 9}`     | 200; `ratings.value` updated; `users.average_rating` recalculated | —                |
| Wrong patient            | Patient B PATCHes rating belonging to Patient A       | 403                                                               | —                |
| Doctor/admin caller      | Any role other than PATIENT                           | 403                                                               | —                |
| Value out of range       | `value: 0` or `value: 11`                             | 400                                                               | Validation error |
| Rating not found         | PATCHes `/ratings/9999`                               | 404                                                               | —                |
| UI re-submit after edit  | Patient changes value in widget and clicks Save again | Calls PATCH; `ratingValue` updated in state                       | —                |

</frozen-after-approval>

## Code Map

- `server/src/main/java/com/medour/service/RatingService.java` -- **EXTEND** — add `updateRating(ratingId, value, callerPatientId)`: load rating by id (404 if missing); assert `rating.patient.id == callerPatientId` (else 403); assert value 1–10 (else 400); update `rating.value`; recalculate and persist `users.average_rating`
- `server/src/main/java/com/medour/dto/UpdateRatingRequest.java` -- **CREATE** — record with `value: int` annotated `@Min(1) @Max(10)`
- `server/src/main/java/com/medour/controller/RatingController.java` -- **EXTEND** — add `PATCH /api/v1/ratings/{id}` calling `ratingService.updateRating`; `@PreAuthorize("hasRole('PATIENT')")`
- `server/src/main/java/com/medour/config/SecurityConfig.java` -- **EXTEND** — add `PATCH /api/v1/ratings/**` to PATIENT-allowed matchers
- `client/src/stores/appointmentStore.ts` -- **EXTEND** — update `submitRating` to branch: if `ratingId` is non-null call `api.patch('/ratings/{ratingId}', {value})` instead of POST; keep state mutation identical
- `client/src/views/PatientAppointmentsView.vue` -- **EXTEND** — remove `appt.ratingId !== null` from the `:disabled` conditions on input and Save button (keep only `submittingRating` guard)
- `server/src/main/java/com/medour/repository/RatingRepository.java` -- read-only reference — `findByAppointmentId` already exists; PATCH uses `findById` from `JpaRepository`

## Tasks & Acceptance

**Execution:**

- [ ] `server/src/main/java/com/medour/dto/UpdateRatingRequest.java` -- CREATE record `(value: int @Min(1) @Max(10))` -- validated request body for PATCH
- [ ] `server/src/main/java/com/medour/service/RatingService.java` -- ADD `@Transactional updateRating(Long ratingId, int value, Long callerPatientId)`: load via `ratingRepository.findById` (404 if absent); assert ownership (403); update value; recalculate and persist average; return updated Rating -- enforces all edit business rules atomically
- [ ] `server/src/main/java/com/medour/controller/RatingController.java` -- ADD `@PatchMapping("/{id}") @PreAuthorize("hasRole('PATIENT')")` handler calling `ratingService.updateRating`; return 200 with `RatingResponse` -- exposes edit endpoint secured to patients only
- [ ] `server/src/main/java/com/medour/config/SecurityConfig.java` -- ADD `PATCH /api/v1/ratings/**` to PATIENT-allowed request matchers alongside existing POST matcher -- URL-level enforcement for the new endpoint
- [ ] `client/src/stores/appointmentStore.ts` -- EXTEND `submitRating`: if `appt.ratingId != null` call `api.patch('/ratings/' + appt.ratingId, {value})`, else POST; update state identically -- single action handles both submit and edit
- [ ] `client/src/views/PatientAppointmentsView.vue` -- REMOVE `appt.ratingId !== null` from `:disabled` on input and Save button; only `submittingRating[appt.id]` disables during in-flight -- allows editing a previously submitted rating
- [ ] `server/src/test/java/com/medour/service/RatingServiceTest.java` -- ADD tests: valid edit → 200 + average recalculated; wrong-patient edit → 403; rating not found → 404
- [ ] `server/src/test/java/com/medour/controller/RatingControllerTest.java` -- ADD tests: valid PATCH → 200; value out of range → 400; wrong-patient → 403; unauthenticated → 401
- [ ] `client/src/stores/__tests__/appointmentStore.test.ts` -- ADD test: when `ratingId` is non-null, `submitRating` calls PATCH and updates state

**Acceptance Criteria:**

- Given a patient with a previously submitted rating on a Completed appointment card, when they change the value and click Save, the PATCH endpoint is called, the server returns 200, and the displayed value updates.
- Given a doctor or admin calls `PATCH /api/v1/ratings/{id}`, the server returns 403.
- Given a patient calls `PATCH /api/v1/ratings/{id}` for a rating belonging to a different patient, the server returns 403.
- Given a value outside 1–10 on the PATCH endpoint, the server returns 400.
- Given a non-existent rating id on the PATCH endpoint, the server returns 404.
- Given a COMPLETED appointment card with an existing rating, the input field and Save button remain enabled (not disabled).

## Verification

**Commands:**

- `cd server && ./mvnw test` -- expected: all tests pass, no compile errors
- `cd client && npm run build` -- expected: TypeScript compile succeeds with no errors
- `cd client && npm run test` -- expected: all vitest tests pass
