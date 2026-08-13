---
title: "My Preferences — Account Info"
type: "feature"
created: "2026-08-13"
status: "done"
review_loop_iteration: 0
baseline_commit: "2214964278918baef22fced58161f56b4798af03"
context: []
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Authenticated users have no way to view or edit their profile fields — first name, surname, contact details, and (for doctors) county and speciality. The `/account` nav link leads nowhere.

**Approach:** Add `GET /api/v1/users/me` and `PUT /api/v1/users/me` endpoints that read the authenticated user's ID from Spring Security context; build `AccountView.vue` at `/account` that pre-fills a form from the API, shows role as read-only, shows county/speciality fields only for DOCTOR, and on save pushes the updated firstName/surname back to `authStore`.

## Boundaries & Constraints

**Always:**

- The principal stored by `JwtAuthFilter` is the user ID string (`sub` claim); controllers extract it via `authentication.getName()`
- `email` and `role` are display-only on the form — never updated via this endpoint
- `county` and `speciality` are required for DOCTOR; must return 400 if absent or blank when role is DOCTOR; ignored for PATIENT/ADMIN
- `passwordHash`, `deletedAt`, and `mustChangePassword` are never returned in any profile response
- On successful save, `authStore.updateUser({ firstName, surname })` is called to keep the header in sync
- The `/account` route requires `{ requiresAuth: true }` (all authenticated roles may access it)

**Ask First:**

- (none)

**Never:**

- No email change in this story
- No role change in this story (admin role-change is Story 4.2)
- No admin-editing-another-user endpoint in this story (Story 4.2)

## I/O & Edge-Case Matrix

| Scenario                            | Input / State                                         | Expected Output / Behavior                                               | Error Handling                                           |
| ----------------------------------- | ----------------------------------------------------- | ------------------------------------------------------------------------ | -------------------------------------------------------- |
| GET profile                         | Valid JWT; user exists                                | 200 + `UserProfileResponse` with all non-sensitive fields                | N/A                                                      |
| PUT profile — patient               | Valid JWT + PATIENT; valid base fields                | 200 + updated `UserProfileResponse`; county/speciality ignored           | N/A                                                      |
| PUT profile — doctor                | Valid JWT + DOCTOR; base fields + county + speciality | 200 + updated response                                                   | N/A                                                      |
| PUT profile — doctor missing county | Valid JWT + DOCTOR; county blank/absent               | 400 `{ "error": "..." }`                                                 | Service throws, GlobalExceptionHandler maps              |
| PUT profile — blank firstName       | Any role; `firstName: ""`                             | 400 via `@NotBlank` Bean Validation                                      | GlobalExceptionHandler `MethodArgumentNotValidException` |
| Page load                           | User navigates to /account                            | Form pre-filled with current profile from GET /users/me                  | N/A                                                      |
| Save success                        | User submits valid form                               | 200; form stays; success message shown; authStore.user.firstName updated | N/A                                                      |

</frozen-after-approval>

## Code Map

- `server/src/main/java/com/medour/dto/UserProfileResponse.java` -- NEW record: id, email, firstName, surname, role, age, gender, city, address, county, speciality
- `server/src/main/java/com/medour/dto/UpdateProfileRequest.java` -- NEW DTO: @NotBlank firstName, @NotBlank surname; nullable age (Integer), gender, city, address, county, speciality
- `server/src/main/java/com/medour/service/UserService.java:80` -- add `getProfile(Long userId)` and `@Transactional updateProfile(Long userId, UpdateProfileRequest req)` methods
- `server/src/main/java/com/medour/controller/UserController.java` -- NEW `@RestController("/api/v1/users")`; `GET /me` and `PUT /me`; extract user ID from `Authentication.getName()`
- `server/src/test/java/com/medour/controller/UserControllerTest.java` -- NEW `@WebMvcTest(UserController.class)` with `@MockBean UserService` and `@MockBean JwtAuthFilter`
- `client/src/stores/authStore.ts:38` -- add `updateUser(updates: Pick<User, 'firstName' | 'surname'>)` action that patches `this.user` and writes `auth_user` to localStorage
- `client/src/router/index.ts:17` -- add `{ path: '/account', component: () => import('../views/AccountView.vue'), meta: { requiresAuth: true } }`
- `client/src/views/AccountView.vue` -- NEW: fetches profile on mount; form with role-display + editable base fields + conditional doctor fields; submit calls PUT; updates authStore on success; shows success/error messages

## Tasks & Acceptance

**Execution:**

- [x] `server/src/main/java/com/medour/dto/UserProfileResponse.java` -- NEW record: `Long id, String email, String firstName, String surname, String role, Integer age, String gender, String city, String address, String county, String speciality`
- [x] `server/src/main/java/com/medour/dto/UpdateProfileRequest.java` -- NEW: `@Data @NoArgsConstructor @AllArgsConstructor`; `@NotBlank String firstName`; `@NotBlank String surname`; nullable `Integer age`, `String gender`, `String city`, `String address`, `String county`, `String speciality`
- [x] `server/src/main/java/com/medour/service/UserService.java` -- add `@Transactional(readOnly=true) getProfile(Long userId)`: find by id (throw 404 ResponseStatusException if absent), return `UserProfileResponse`; add `@Transactional updateProfile(Long userId, UpdateProfileRequest req)`: load user, if DOCTOR validate county+speciality (throw 400 if blank), update all editable fields, save, return `UserProfileResponse`
- [x] `server/src/main/java/com/medour/controller/UserController.java` -- NEW `@RestController @RequestMapping("/api/v1/users")`; inject `UserService`; `@GetMapping("/me") getMe(Authentication auth)` → `userService.getProfile(Long.parseLong(auth.getName()))` → 200; `@PutMapping("/me") updateMe(Authentication auth, @Valid @RequestBody UpdateProfileRequest req)` → `userService.updateProfile(Long.parseLong(auth.getName()), req)` → 200
- [x] `server/src/test/java/com/medour/controller/UserControllerTest.java` -- NEW `@WebMvcTest(UserController.class) @AutoConfigureMockMvc(addFilters=false)`; `@MockBean UserService`; `@MockBean JwtAuthFilter`; tests: (1) `GET /me` → 200 + email field; (2) `PUT /me` valid → 200 + updated firstName; (3) `PUT /me` blank firstName → 400
- [x] `client/src/stores/authStore.ts` -- add `updateUser(updates: Pick<User, 'firstName' | 'surname'>)` action: `if (this.user) { this.user = { ...this.user, ...updates }; localStorage.setItem('auth_user', JSON.stringify(this.user)); }`
- [x] `client/src/router/index.ts` -- add `{ path: '/account', component: () => import('../views/AccountView.vue'), meta: { requiresAuth: true } }` to `routes`
- [x] `client/src/views/AccountView.vue` -- NEW: `profile` ref (holds full API response); `loading`, `saving`, `successMessage`, `errorMessage` refs; on `onMounted` call `api.get('/users/me')` and populate `profile`; form fields: email (readonly), role (readonly), firstName, surname, age, gender, city, address, county+speciality (`v-if="profile.role === 'DOCTOR'"`); on submit call `api.put('/users/me', { firstName, surname, age, gender, city, address, county, speciality })`; on success call `authStore.updateUser({ firstName: profile.firstName, surname: profile.surname })` and set `successMessage`; button disabled while `saving`

**Acceptance Criteria:**

- Given an authenticated user, when `/account` is navigated to, then the form is pre-populated with their current profile data from the server.
- Given a PATIENT user on the account page, then county and speciality fields are not shown.
- Given a DOCTOR user on the account page, then county and speciality fields are shown and required.
- Given valid form data submitted, then the server returns 200, the header updates to reflect the new firstName if changed, and a success message is shown.
- Given a blank firstName submitted, then the server returns 400 and the form shows an error.
- Given a DOCTOR submitting without county, then the server returns 400.
- Email and role fields are displayed but cannot be edited.

## Verification

**Commands:**

- `cd server && ./mvnw test` -- expected: 16 tests pass (13 existing + 3 new UserControllerTest)
- `cd client && npm run test` -- expected: 8 tests pass (no new client tests in this story)
- `cd client && npm run build` -- expected: zero TypeScript errors

## Spec Change Log

**Review loop 1 patches applied:**

- `UserRepository` — added `findByIdAndDeletedAtIsNull(Long id)`; `getProfile` and `updateProfile` use it so soft-deleted users cannot read or update their own profile
- `UserController` — extracted `parseUserId(Authentication)` helper that wraps `Long.parseLong` in a try/catch returning 401 on `NumberFormatException`
- `router/__tests__/index.test.ts` — added 5th guard test: unauthenticated push to `/account` → redirected to `/login`
- `stores/__tests__/authStore.test.ts` — NEW: 3 unit tests covering `updateUser` merge logic, field preservation, and null-user no-op
- `UserServiceTest` — added `updateProfile_doctorMissingCounty_throwsBadRequest`; imports `UpdateProfileRequest` and AssertJ

## Suggested Review Order

**Profile read/write endpoints**

- Controller extracts userId via `parseUserId`; delegates to service; no role-specific logic at controller layer
  [`UserController.java:21`](../../server/src/main/java/com/medour/controller/UserController.java#L21)

- `getProfile` and `updateProfile` both use `findByIdAndDeletedAtIsNull` — soft-deleted users get 404
  [`UserService.java:95`](../../server/src/main/java/com/medour/service/UserService.java#L95)

- DOCTOR county/speciality validation in `updateProfile`
  [`UserService.java:106`](../../server/src/main/java/com/medour/service/UserService.java#L106)

**Repository**

- New `findByIdAndDeletedAtIsNull` method
  [`UserRepository.java:12`](../../server/src/main/java/com/medour/repository/UserRepository.java#L12)

**Client account view**

- Pre-fill on mount, DOCTOR-conditional fields, success handler calls `authStore.updateUser`
  [`AccountView.vue:1`](../../client/src/views/AccountView.vue#L1)

**Store action**

- `updateUser` spreads updates, guards null user, persists to localStorage
  [`authStore.ts:43`](../../client/src/stores/authStore.ts#L43)

**Tests**

- `UserControllerTest` — GET/PUT success + @NotBlank validation
  [`UserControllerTest.java:1`](../../server/src/test/java/com/medour/controller/UserControllerTest.java#L1)

- `UserServiceTest` — DOCTOR validation + soft-delete login
  [`UserServiceTest.java:44`](../../server/src/test/java/com/medour/service/UserServiceTest.java#L44)

- `authStore.test.ts` — updateUser merge, field preservation, null guard
  [`stores/__tests__/authStore.test.ts:1`](../../client/src/stores/__tests__/authStore.test.ts#L1)
