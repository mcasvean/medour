---
title: "Speciality Dropdown in Doctor Forms"
type: "feature"
created: "2026-08-14"
status: "ready-for-dev"
review_loop_iteration: 0
baseline_commit: ""
context: ["spec-8-1-speciality-management-admin"]
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** After Story 8.1 creates the `specialities` table and its admin CRUD API, doctor registration and the account info form still display a free-text input for speciality. The canonical speciality list is not used, so inconsistencies remain.

**Approach:**

- Replace the free-text `VTextField` for speciality in `DoctorRegisterForm.vue` and `AccountView.vue` with a `VSelect` populated by `GET /api/v1/specialities`.
- Migrate the `users.speciality` column to a foreign-key relationship: add `speciality_id BIGINT NULL REFERENCES specialities(id)` to `users`; retain the old `speciality VARCHAR` column for now but stop writing to it (it becomes effectively deprecated).
- Update all relevant DTOs, services, and responses to use `specialityId` / `specialityName` (joined from the `specialities` table).
- Doctor search (`BookingSearchView`) and appointment cards already display `speciality` as text — update them to read from the new field name `specialityName` in the DTO.
- The admin user edit form (`AdminUserForm`) also receives a speciality `VSelect` for doctor users.

## Boundaries & Constraints

**Always:**

- `speciality_id` is nullable on `users` — existing doctors without a mapped speciality have `null` until they update their profile.
- All new doctor registrations must supply a `specialityId`; the registration endpoint validates it is present and exists in `specialities` table.
- `DoctorRegisterForm` and `AccountView` (for role DOCTOR) load specialities from `GET /api/v1/specialities` on mount and display them in a `VSelect`.
- `AdminUserForm` shows the speciality `VSelect` only when `role === 'DOCTOR'`.
- Doctor search filter in `BookingSearchView` uses the same speciality list for its filter dropdown (currently the search already has a speciality text field — replace with the speciality list).
- The old `users.speciality` column is left in the DB schema (not dropped) — it is simply no longer read or written by the application after this story.
- All API doctor responses expose `specialityName: string | null` (resolved by joining to `specialities`); DTO no longer exposes the raw `speciality` text field.

**Ask First:**

- (none)

**Never:**

- Do not drop the `users.speciality` column — leave it for a future cleanup migration.
- Do not block saving a doctor profile if `speciality_id` is null (for existing doctors editing other fields) — only new registrations require it.
- Do not seed specialities in this story.

## I/O & Edge-Case Matrix

| Scenario                                    | Input / State                                         | Expected Output / Behavior                                                   | Error Handling |
| ------------------------------------------- | ----------------------------------------------------- | ---------------------------------------------------------------------------- | -------------- |
| Doctor registers with valid speciality      | `specialityId` references an existing speciality      | Account created; `users.speciality_id` set                                   | —              |
| Doctor registers with no speciality         | `specialityId` absent or null                         | 400 `{"error": "Speciality is required for doctor registration."}`           | —              |
| Doctor registers with invalid speciality id | `specialityId = 999` (non-existent)                   | 400 `{"error": "Selected speciality does not exist."}`                       | —              |
| Doctor updates profile, changes speciality  | Selects a new speciality from the dropdown            | `speciality_id` updated; `specialityName` in response reflects new selection | —              |
| Doctor updates profile, clears speciality   | Clears VSelect (sets to null)                         | `speciality_id` set to null; no error                                        | —              |
| Search filter by speciality                 | Patient selects "Cardiology" from dropdown            | Results filtered correctly using `speciality_id` join                        | —              |
| Speciality list loads in form               | Form mounted                                          | VSelect populated with sorted speciality names from API                      | —              |
| Existing doctor with old text speciality    | `speciality_id` is null, `speciality` has legacy text | Profile loads with empty VSelect; no crash                                   | —              |

</frozen-after-approval>

## Code Map

**Backend:**

- `server/src/main/java/com/medour/model/User.java` — **EXTEND** add `@ManyToOne @JoinColumn(name = "speciality_id") private Speciality specialityRef;` (keep existing `String speciality` field but mark it as deprecated in a comment — not dropped)
- `server/src/main/java/com/medour/dto/DoctorRegistrationRequest.java` — **EXTEND** add `Long specialityId`; add `@NotNull` for validation on the doctor registration path
- `server/src/main/java/com/medour/dto/UserProfileResponse.java` — **EXTEND** replace `String speciality` with `Long specialityId` and `String specialityName` (populated from `user.getSpecialityRef()`)
- `server/src/main/java/com/medour/dto/DoctorSearchResult.java` — **EXTEND** replace `String speciality` with `String specialityName`
- `server/src/main/java/com/medour/service/AuthService.java` — **EXTEND** on doctor registration: validate `specialityId` is not null (400); find `Speciality` by id (400 if not found); set `user.setSpecialityRef(speciality)`
- `server/src/main/java/com/medour/service/UserService.java` — **EXTEND** on doctor profile update: if `specialityId` is provided, validate and set `specialityRef`; if null, set `specialityRef = null`; map `specialityId`/`specialityName` in response DTO
- `server/src/main/java/com/medour/service/DoctorService.java` — **EXTEND** update `DoctorSearchResult` mapping to read from `user.getSpecialityRef().getName()` (null-safe)
- `server/src/test/java/com/medour/service/AuthServiceTest.java` — **EXTEND** add 2 tests: register doctor with valid specialityId → specialityRef set; register doctor without specialityId → 400

**Frontend:**

- `client/src/stores/doctorStore.ts` — **EXTEND** update `DoctorSearchResult` type: replace `speciality: string` with `specialityName: string | null`; update `BookingSearchView` speciality filter to use speciality list from API instead of free text
- `client/src/stores/userStore.ts` — **EXTEND** update `UserProfile` type: add `specialityId: number | null` and `specialityName: string | null`; update account save payload to include `specialityId`
- `client/src/views/DoctorRegisterForm.vue` — **EXTEND** fetch specialities from `GET /api/v1/specialities` on mount; replace `<VTextField v-model="speciality" ...>` with `<VSelect v-model="specialityId" :items="specialities" item-title="name" item-value="id" label="Speciality" ...>`; emit `specialityId` instead of `speciality` string
- `client/src/views/AccountView.vue` — **EXTEND** in the doctor section, replace speciality `VTextField` with `VSelect`; fetch specialities on mount; bind to `profile.specialityId`
- `client/src/components/AdminUserForm.vue` — **EXTEND** replace speciality `VTextField` with `VSelect` when `role === 'DOCTOR'`; load specialities on mount or via prop
- `client/src/views/BookingSearchView.vue` — **EXTEND** replace free-text speciality filter field with a `VSelect` loaded from the specialities list; pass `specialityName` (or `specialityId`) as the filter param

## Tasks & Acceptance

**Execution:**

- [ ] `server/src/main/java/com/medour/model/User.java` — add `specialityRef` ManyToOne field
- [ ] `server/src/main/java/com/medour/dto/DoctorRegistrationRequest.java` — add `Long specialityId`
- [ ] `server/src/main/java/com/medour/dto/UserProfileResponse.java` — replace `speciality` with `specialityId` + `specialityName`
- [ ] `server/src/main/java/com/medour/dto/DoctorSearchResult.java` — replace `speciality` with `specialityName`
- [ ] `server/src/main/java/com/medour/service/AuthService.java` — validate and set `specialityRef` on doctor registration
- [ ] `server/src/main/java/com/medour/service/UserService.java` — handle `specialityId` in profile updates; map to response
- [ ] `server/src/main/java/com/medour/service/DoctorService.java` — read `specialityName` from relation in search results
- [ ] `server/src/test/java/com/medour/service/AuthServiceTest.java` — add 2 tests for doctor registration with/without specialityId
- [ ] `client/src/stores/doctorStore.ts` — update type; update filter logic
- [ ] `client/src/stores/userStore.ts` — update types and API payload
- [ ] `client/src/views/DoctorRegisterForm.vue` — replace text field with VSelect; load specialities on mount
- [ ] `client/src/views/AccountView.vue` — replace text field with VSelect for doctor section
- [ ] `client/src/components/AdminUserForm.vue` — replace text field with VSelect for doctor role
- [ ] `client/src/views/BookingSearchView.vue` — replace free-text filter with VSelect from specialities API

## Acceptance Criteria

- Given the doctor registration form opens, the speciality field is a dropdown populated with specialities from the DB sorted alphabetically.
- Given a doctor registers without selecting a speciality, a 400 error is returned by the server.
- Given a doctor selects a valid speciality and registers, `speciality_id` is set on the user record.
- Given an existing doctor opens their Account Info, the speciality dropdown shows their current selection.
- Given a doctor updates their speciality to a new value and saves, the updated `specialityName` is visible in their profile and in search results.
- Given the booking search filter is opened, the speciality filter is a dropdown, not a free-text input.
- Given an admin edits a doctor user, the speciality field is a dropdown.
- Given an admin edits a patient user, the speciality dropdown is not shown.

## Verification

**Commands:**

- `cd server && ./mvnw test` — expected: all existing tests + 2 new AuthService tests pass
- `cd client && npm run build` — expected: zero TypeScript errors
- `cd client && npm run test` — expected: all existing tests pass
