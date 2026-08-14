---
title: "Speciality Management Admin Page"
type: "feature"
created: "2026-08-14"
status: "ready-for-dev"
review_loop_iteration: 0
baseline_commit: ""
context: ["spec-6-1-toast-notification-system"]
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Doctor speciality is a free-text field. There is no canonical list of valid specialities, leading to spelling inconsistencies (e.g. "Cardiology", "cardiology", "cardiologist") that break filtering in the appointment booking search.

**Approach:**

- Create a new `specialities` table (`id BIGINT PK`, `name VARCHAR(100) UNIQUE NOT NULL`).
- Expose a full CRUD REST API: `GET /api/v1/specialities` (public — no auth), `POST /api/v1/specialities` (ADMIN), `PUT /api/v1/specialities/{id}` (ADMIN), `DELETE /api/v1/specialities/{id}` (ADMIN).
- Add a new admin page `AdminSpecialitiesView.vue` accessible via `/admin/specialities`. The page lists current specialities in a table with inline edit and delete (confirmation dialog). A form at the top allows adding a new speciality.
- Add a "Specialities" entry to the admin section of the burger menu in `App.vue`.
- `DELETE` prevents removal if any doctor's `speciality` text field matches the name — return 409 `{"error": "Speciality is in use by one or more doctors and cannot be deleted."}` (checked against `users.speciality`). This check will be replaced by a FK check in Story 8.2.

## Boundaries & Constraints

**Always:**

- `name` is trimmed and unique (case-insensitive at application level — reject duplicates differing only in case); max 100 characters.
- `GET /api/v1/specialities` returns the list sorted alphabetically by name.
- The admin-only endpoints (`POST`, `PUT`, `DELETE`) are protected by Spring Security — `ROLE_ADMIN` only.
- `GET /api/v1/specialities` is permitted to all (including unauthenticated) — it will be called from the doctor registration form in Story 8.2.
- Delete is blocked if any `users.speciality` column value matches the speciality name (case-insensitive); return 409.
- Success feedback uses toast notifications (Story 6.1 prerequisite).
- The admin page is accessible only by admins (Vue Router guard already covers `/admin/**`).

**Ask First:**

- (none)

**Never:**

- Do not modify the `users.speciality` text column in this story — that migration happens in Story 8.2.
- Do not add speciality options to any doctor form in this story — that is Story 8.2.
- Do not seed any initial specialities — the admin creates them via the UI.

## I/O & Edge-Case Matrix

| Scenario                         | Input / State                                          | Expected Output / Behavior                                      | Error Handling                      |
| -------------------------------- | ------------------------------------------------------ | --------------------------------------------------------------- | ----------------------------------- |
| List specialities                | `GET /api/v1/specialities`                             | 200; JSON array sorted by name; empty array if none             | —                                   |
| Add speciality                   | `POST {"name": "Cardiology"}`                          | 201 `{ id, name }`                                              | —                                   |
| Add duplicate (case-insensitive) | `POST {"name": "cardiology"}` when "Cardiology" exists | 409 `{"error": "Speciality already exists."}`                   | Application-level check before save |
| Edit speciality                  | `PUT /1 {"name": "Cardiologie"}`                       | 200 updated `{ id, name }`                                      | —                                   |
| Delete unused speciality         | No doctor's `users.speciality` matches the name        | 200 (or 204)                                                    | —                                   |
| Delete in-use speciality         | A doctor has `speciality = 'Cardiology'`               | 409 `{"error": "Speciality is in use by one or more doctors."}` | —                                   |
| Name too long                    | `name` > 100 characters                                | 400                                                             | Bean Validation `@Size(max=100)`    |
| Empty name                       | `name` is blank                                        | 400                                                             | Bean Validation `@NotBlank`         |

</frozen-after-approval>

## Code Map

**Backend:**

- `server/src/main/java/com/medour/model/Speciality.java` — **NEW** JPA entity: `@Table(name = "specialities")`; fields: `id BIGINT PK`, `@Column(unique=true, nullable=false, length=100) String name`
- `server/src/main/java/com/medour/repository/SpecialityRepository.java` — **NEW** `JpaRepository<Speciality, Long>`; add `Optional<Speciality> findByNameIgnoreCase(String name)`; add `List<Speciality> findAllByOrderByNameAsc()`
- `server/src/main/java/com/medour/dto/SpecialityRequest.java` — **NEW** record: `@NotBlank @Size(max=100) String name`
- `server/src/main/java/com/medour/dto/SpecialityResponse.java` — **NEW** record: `Long id`, `String name`
- `server/src/main/java/com/medour/service/SpecialityService.java` — **NEW** service: `findAll()` → list sorted by name; `create(name)` → check case-insensitive duplicate (409), save; `update(id, name)` → find (404), check duplicate (409 if name taken by other), save; `delete(id)` → find (404), check `userRepository.existsBySpecialityIgnoreCaseAndDeletedAtIsNull(name)` (409 if true), delete
- `server/src/main/java/com/medour/controller/SpecialityController.java` — **NEW** `@RestController @RequestMapping("/api/v1/specialities")`; `GET /` → `findAll()` → 200; `POST /` → 201; `PUT /{id}` → 200; `DELETE /{id}` → 200
- `server/src/main/java/com/medour/repository/UserRepository.java` — **EXTEND** add `boolean existsBySpecialityIgnoreCaseAndDeletedAtIsNull(String speciality)`
- `server/src/main/java/com/medour/config/SecurityConfig.java` — **EXTEND** permit `GET /api/v1/specialities` for all; require ADMIN for `POST`, `PUT`, `DELETE` on `/api/v1/specialities/**`
- `server/src/test/java/com/medour/service/SpecialityServiceTest.java` — **NEW** tests: create valid; create duplicate → 409; delete unused → ok; delete in-use → 409; update → ok

**Frontend:**

- `client/src/views/AdminSpecialitiesView.vue` — **NEW** admin page: VDataTable listing specialities with name column; inline edit (name field + save button per row) and delete button (confirmation dialog); "Add speciality" section at top with a text field + "Add" button; all mutations call store actions and show success/error toasts
- `client/src/stores/userStore.ts` — **EXTEND** (or create separate `specialityStore.ts`) add `specialities: Speciality[]`; add `fetchSpecialities()`, `addSpeciality(name)`, `updateSpeciality(id, name)`, `deleteSpeciality(id)` actions wired to API
- `client/src/router/index.ts` — **EXTEND** add route `{ path: '/admin/specialities', component: AdminSpecialitiesView, meta: { requiresAuth: true, roles: ['ADMIN'] } }`
- `client/src/App.vue` — **EXTEND** in the admin `<template v-if="isAdmin">` section, add `<VListItem prepend-icon="mdi-tag-multiple-outline" title="Specialities" to="/admin/specialities" rounded="lg" />` before Appointments

## Tasks & Acceptance

**Execution:**

- [ ] `server/src/main/java/com/medour/model/Speciality.java` — create JPA entity with `id`, `name`
- [ ] `server/src/main/java/com/medour/repository/SpecialityRepository.java` — create with `findByNameIgnoreCase` and `findAllByOrderByNameAsc`
- [ ] `server/src/main/java/com/medour/repository/UserRepository.java` — add `existsBySpecialityIgnoreCaseAndDeletedAtIsNull(String speciality)`
- [ ] `server/src/main/java/com/medour/dto/SpecialityRequest.java` and `SpecialityResponse.java` — create DTOs
- [ ] `server/src/main/java/com/medour/service/SpecialityService.java` — implement all CRUD methods with validation
- [ ] `server/src/main/java/com/medour/controller/SpecialityController.java` — implement 4 endpoints
- [ ] `server/src/main/java/com/medour/config/SecurityConfig.java` — add permit all for `GET` and ADMIN-only for mutating methods
- [ ] `server/src/test/java/com/medour/service/SpecialityServiceTest.java` — create with 5+ tests
- [ ] `client/src/stores/userStore.ts` — add speciality CRUD actions (or create `specialityStore.ts`)
- [ ] `client/src/views/AdminSpecialitiesView.vue` — create full admin CRUD page
- [ ] `client/src/router/index.ts` — add `/admin/specialities` route
- [ ] `client/src/App.vue` — add Specialities item to admin burger menu section

## Acceptance Criteria

- Given an admin navigates to Specialities in the burger menu, the `/admin/specialities` page opens with the list of existing specialities.
- Given the admin adds a new speciality "Cardiology", it appears in the list sorted alphabetically.
- Given the admin adds "cardiology" when "Cardiology" already exists, a 409 error toast is shown.
- Given the admin edits a speciality name and saves, the updated name is reflected in the list.
- Given the admin deletes a speciality that no doctor uses, it is removed from the list.
- Given the admin tries to delete a speciality used by a doctor, a 409 error toast is shown and the speciality remains.
- Given an unauthenticated user calls `GET /api/v1/specialities`, they receive the list without error.
- Given a non-admin authenticated user calls `POST /api/v1/specialities`, they receive 403.

## Verification

**Commands:**

- `cd server && ./mvnw test` — expected: all existing tests + 5+ SpecialityService tests pass
- `cd client && npm run build` — expected: zero TypeScript errors
- `cd client && npm run test` — expected: all existing tests pass
