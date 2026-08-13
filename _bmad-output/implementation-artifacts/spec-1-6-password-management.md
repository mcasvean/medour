---
title: "Password Management"
type: "feature"
created: "2026-08-13"
status: "done"
review_loop_iteration: 0
baseline_commit: "5701e0e886110221dccd04574156a35b09456ff5"
context: []
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Users cannot change their own password, admins cannot issue temporary passwords, and the `mustChangePassword` flag has no enforcement — a user with `mustChangePassword=true` can still navigate freely.

**Approach:** Add `POST /api/v1/users/me/password` (self-change, verifies current password) and `POST /api/v1/admin/users/{id}/password` (admin sets a temp password, forces rotation); add `ChangePasswordView.vue`; extend the router guard so any authenticated user with `mustChangePassword=true` is redirected to `/change-password` until they complete rotation.

## Boundaries & Constraints

**Always:**

- Self-change requires the current password to be verified; a wrong current password returns 403 `{ "error": "Wrong password" }` — same endpoint, different error from "Invalid credentials"
- Admin set-password never requires the current password; it always sets `mustChangePassword=true` on the target user
- Admin cannot view the new password in any response — 204 No Content for both endpoints
- New passwords are bcrypt-hashed before persistence; plaintext never stored
- Forced rotation: if `auth.user?.mustChangePassword === true`, the guard redirects every `requiresAuth` route (except `/change-password` itself) to `/change-password`
- On a successful self-change: `authStore.updateUser({ mustChangePassword: false })` is called so the guard lifts immediately without a page reload; then navigate to `/`
- The `/change-password` route has `requiresAuth: true` but is exempted from the forced-rotation redirect (it is the destination)
- `authStore.updateUser` type is extended to also accept `mustChangePassword` as an optional partial field

**Ask First:**

- (none)

**Never:**

- No current-password verification for admin set-password
- No password validation rules beyond @NotBlank in this story (Story 1.6 spec item — deliberately deferred from Story 1.2)
- No admin viewing another user's current password — response is always 204, no body
- Admin endpoint `POST /api/v1/admin/users/{id}/password` is protected by the existing SecurityConfig `hasRole("ADMIN")` matcher on `/api/v1/admin/**` — no additional Spring Security annotation needed

## I/O & Edge-Case Matrix

| Scenario                           | Input / State                                                           | Expected Output / Behavior                                     | Error Handling                                    |
| ---------------------------------- | ----------------------------------------------------------------------- | -------------------------------------------------------------- | ------------------------------------------------- |
| Valid self-change                  | `POST /me/password`; correct `currentPassword`; non-blank `newPassword` | 204; user's `passwordHash` updated; `mustChangePassword=false` | N/A                                               |
| Wrong current password             | `POST /me/password`; wrong `currentPassword`                            | 403 `{ "error": "Wrong password" }`                            | `WrongPasswordException` → GlobalExceptionHandler |
| Blank new password                 | `POST /me/password`; `newPassword: ""`                                  | 400 via `@NotBlank` Bean Validation                            | N/A                                               |
| Admin sets temp password           | `POST /admin/users/{id}/password`; valid `newPassword`                  | 204; target user's hash updated; `mustChangePassword=true`     | N/A                                               |
| mustChangePassword=true navigation | Authenticated; `mustChangePassword=true`; navigate to `/`               | Guard redirects to `/change-password`                          | N/A                                               |
| Confirm mismatch                   | Client: `newPassword !== confirmPassword`                               | Form shows error; no API call made                             | Client-side only                                  |
| Forced rotation complete           | User successfully changes password on `/change-password`                | `mustChangePassword=false` in store; navigates to `/`          | N/A                                               |

</frozen-after-approval>

## Code Map

- `server/src/main/java/com/medour/dto/ChangePasswordRequest.java` -- NEW: `@NotBlank currentPassword`, `@NotBlank newPassword`
- `server/src/main/java/com/medour/dto/AdminSetPasswordRequest.java` -- NEW: `@NotBlank newPassword`
- `server/src/main/java/com/medour/exception/WrongPasswordException.java` -- NEW RuntimeException → 403
- `server/src/main/java/com/medour/exception/GlobalExceptionHandler.java` -- add handler for `WrongPasswordException` → 403 `{ "error": "Wrong password" }`
- `server/src/main/java/com/medour/service/UserService.java` -- add `changePassword(Long userId, ChangePasswordRequest)` and `adminSetPassword(Long targetId, AdminSetPasswordRequest)`
- `server/src/main/java/com/medour/controller/UserController.java` -- add `POST /me/password` → 204
- `server/src/main/java/com/medour/controller/AdminController.java` -- NEW `@RestController("/api/v1/admin")`; `POST /users/{id}/password` → 204
- `server/src/test/java/com/medour/controller/UserControllerTest.java` -- add 3 password tests: valid change → 204; wrong current → 403; blank new → 400
- `server/src/test/java/com/medour/controller/AdminControllerTest.java` -- NEW; test `POST /admin/users/{id}/password` → 204
- `server/src/test/java/com/medour/service/UserServiceTest.java` -- add service-layer tests: wrong-password throws; admin set sets `mustChangePassword=true`
- `client/src/stores/authStore.ts:43` -- extend `updateUser` type to `Partial<Pick<User, 'firstName' | 'surname' | 'mustChangePassword'>>`
- `client/src/router/index.ts:40` -- add `/change-password` route; add forced-rotation check to `setupGuard`
- `client/src/views/ChangePasswordView.vue` -- NEW: `currentPassword`, `newPassword`, `confirmPassword` refs; client-side confirm check; `POST /users/me/password`; on 200 call `authStore.updateUser({ mustChangePassword: false })` then navigate `/`; on 403 show "Current password is incorrect"

## Tasks & Acceptance

**Execution:**

- [x] `server/src/main/java/com/medour/dto/ChangePasswordRequest.java` -- NEW: `@Data @NoArgsConstructor @AllArgsConstructor`; `@NotBlank String currentPassword`; `@NotBlank String newPassword`
- [x] `server/src/main/java/com/medour/dto/AdminSetPasswordRequest.java` -- NEW: `@Data @NoArgsConstructor @AllArgsConstructor`; `@NotBlank String newPassword`
- [x] `server/src/main/java/com/medour/exception/WrongPasswordException.java` -- NEW `public class WrongPasswordException extends RuntimeException {}`
- [x] `server/src/main/java/com/medour/exception/GlobalExceptionHandler.java` -- add `@ExceptionHandler(WrongPasswordException.class)` → `ResponseEntity.status(403).body(Map.of("error", "Wrong password"))`
- [x] `server/src/main/java/com/medour/service/UserService.java` -- add `@Transactional changePassword(Long userId, ChangePasswordRequest req)`: load via `findByIdAndDeletedAtIsNull` (404 if absent); `passwordEncoder.matches` check → throw `WrongPasswordException` if false; hash and set new `passwordHash`; set `mustChangePassword=false`; save; add `@Transactional adminSetPassword(Long targetId, AdminSetPasswordRequest req)`: load via `findByIdAndDeletedAtIsNull` (404 if absent); hash and set new `passwordHash`; set `mustChangePassword=true`; save
- [x] `server/src/main/java/com/medour/controller/UserController.java` -- add `@PostMapping("/me/password") changePassword(Authentication auth, @Valid @RequestBody ChangePasswordRequest req)` → call `userService.changePassword(parseUserId(auth), req)` → `ResponseEntity.noContent().build()`
- [x] `server/src/main/java/com/medour/controller/AdminController.java` -- NEW `@RestController @RequestMapping("/api/v1/admin")`; inject `UserService`; `@PostMapping("/users/{id}/password") adminSetPassword(@PathVariable Long id, @Valid @RequestBody AdminSetPasswordRequest req)` → `userService.adminSetPassword(id, req)` → `ResponseEntity.noContent().build()`
- [x] `server/src/test/java/com/medour/controller/UserControllerTest.java` -- add 3 tests: `changePassword_valid_returns204` (mock `userService.changePassword` does nothing, assert 204); `changePassword_wrongPassword_returns403` (mock throws `WrongPasswordException`, assert 403 + error body); `changePassword_blankNewPassword_returns400` (assert 400)
- [x] `server/src/test/java/com/medour/controller/AdminControllerTest.java` -- NEW `@WebMvcTest(AdminController.class)` with `@MockBean JwtUtil` and `@MockBean UserService`; `@WithMockUser(username="1", roles="ADMIN")`; test `POST /admin/users/2/password` valid → 204
- [x] `server/src/test/java/com/medour/service/UserServiceTest.java` -- add `changePassword_wrongCurrentPassword_throwsWrongPassword` (stub user+wrong match → assert `WrongPasswordException`); add `adminSetPassword_setsMustChangePasswordTrue` (stub user → verify `save` called with `mustChangePassword=true`)
- [x] `client/src/stores/authStore.ts` -- change `updateUser` signature from `Pick<User, 'firstName' | 'surname'>` to `Partial<Pick<User, 'firstName' | 'surname' | 'mustChangePassword'>>`
- [x] `client/src/router/index.ts` -- add `{ path: '/change-password', component: () => import('../views/ChangePasswordView.vue'), meta: { requiresAuth: true } }`; in `setupGuard` add after the `requiresAuth` redirect: `if (to.meta.requiresAuth && auth.user?.mustChangePassword && to.path !== '/change-password') return '/change-password'`
- [x] `client/src/views/ChangePasswordView.vue` -- NEW: `currentPassword`, `newPassword`, `confirmPassword`, `errorMessage`, `saving` refs; on submit validate `newPassword === confirmPassword` (client-side, show error if not); call `api.post('/users/me/password', { currentPassword, newPassword })`; on success call `authStore.updateUser({ mustChangePassword: false })` then `router.push('/')`; on 403 set `errorMessage = 'Current password is incorrect'`; button disabled while `saving`

**Acceptance Criteria:**

- Given correct current password and a new password, when `POST /me/password` is called, then the server responds 204, the new password is hashed and saved, and `mustChangePassword` is set to false.
- Given the wrong current password, when `POST /me/password` is called, then the server returns 403 with `{ "error": "Wrong password" }` and the password is not changed.
- Given an admin user, when `POST /admin/users/{id}/password` is called, then the server responds 204, the target user's password is updated, and `mustChangePassword` is set to true.
- Given an authenticated user with `mustChangePassword=true`, when any `requiresAuth` route (other than `/change-password`) is navigated to, then the router guard redirects to `/change-password`.
- Given the user successfully submits a new password on `/change-password`, then `authStore.user.mustChangePassword` becomes false, the guard lifts, and the user is navigated to `/`.
- Given mismatched `newPassword` and `confirmPassword`, then the form shows a client-side error and no API call is made.

## Verification

**Commands:**

- `cd server && ./mvnw test` -- expected: 22 tests pass (17 existing + 3 UserController password tests + 1 AdminController + 1 UserService wrong-password + 1 UserService adminSetPassword)
- `cd client && npm run test` -- expected: 12 tests pass (no new client unit tests added here)
- `cd client && npm run build` -- expected: zero TypeScript errors

## Spec Change Log

**Review loop 1 patches applied:**

- `UserService.java` — fixed stray formatting artifact in `seedAdmin` method (signature and body were on same line)
- `router/__tests__/index.test.ts` — mock type extended with `mustChangePassword?: boolean`; 2 new guard cases: mustChangePassword=true navigates to `/` → redirected to `/change-password`; mustChangePassword=true navigating to `/change-password` → stays
- `UserServiceTest.java` — added `changePassword_success_clearsMustChangePassword` to verify `mustChangePassword` becomes `false` after a successful password change (service-layer test)

## Suggested Review Order

**Self-service password change (entry point)**

- Verify current password, hash new one, clear forced-rotation flag
  [`UserService.java:143`](../../server/src/main/java/com/medour/service/UserService.java#L143)

- Controller endpoint extracts userId via parseUserId, returns 204
  [`UserController.java:32`](../../server/src/main/java/com/medour/controller/UserController.java#L32)

- WrongPasswordException handler → 403
  [`GlobalExceptionHandler.java:25`](../../server/src/main/java/com/medour/exception/GlobalExceptionHandler.java#L25)

**Admin temp-password reset**

- Sets new hash + mustChangePassword=true; uses findByIdAndDeletedAtIsNull
  [`UserService.java:157`](../../server/src/main/java/com/medour/service/UserService.java#L157)

- Admin controller endpoint under /api/v1/admin (auto-restricted by SecurityConfig path rule)
  [`AdminController.java:19`](../../server/src/main/java/com/medour/controller/AdminController.java#L19)

**Forced-rotation router guard**

- After requiresAuth check, mustChangePassword=true → redirect to /change-password (exempt self)
  [`router/index.ts:50`](../../client/src/router/index.ts#L50)

**ChangePasswordView**

- Confirm mismatch client-side; POST /me/password; 403 → inline error; success clears flag + navigates
  [`ChangePasswordView.vue:40`](../../client/src/views/ChangePasswordView.vue#L40)

**Tests**

- Service: wrong-password throws + success clears mustChangePassword
  [`UserServiceTest.java:75`](../../server/src/test/java/com/medour/service/UserServiceTest.java#L75)

- Controller: 204 valid, 403 wrong, 400 blank
  [`UserControllerTest.java:83`](../../server/src/test/java/com/medour/controller/UserControllerTest.java#L83)

- Router guard: forced-rotation redirect + exempt /change-password
  [`router/__tests__/index.test.ts:50`](../../client/src/router/__tests__/index.test.ts#L50)
