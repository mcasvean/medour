---
title: "Admin Delete User (Soft-Delete)"
type: "feature"
created: "2026-08-13"
status: "done"
review_loop_iteration: 0
baseline_commit: "2aa9188b5e3a3c0559f78746e0f10992164b402a"
context: []
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** The admin cannot delete users. There is no `DELETE /api/v1/admin/users/{id}` endpoint and no delete button in the UI.

**Approach:** Add `DELETE /api/v1/admin/users/{id}` that soft-deletes by setting `deleted_at = now()` (never a hard DELETE); show a confirmation modal in `AdminUsersView` before calling the API; on success refresh the user list so the row displays the "Deleted" badge.

## Boundaries & Constraints

**Always:**
- Soft-delete only: `users.deleted_at = LocalDateTime.now()`; the row stays in the DB; appointments are untouched
- The endpoint is auto-protected by the existing SecurityConfig `/api/v1/admin/**` rule
- Client shows a confirmation step before the API call — use a native `window.confirm()` dialog
- An already-soft-deleted user can be re-deleted (idempotent: sets `deleted_at` to the current time again); this is fine because the login check only tests `deletedAt != null`
- After successful deletion, `userStore.fetchAdminUsers()` is called to refresh the list
- Admin deleting their own account is permitted by the API (they will be auto-logged-out on the next 401 after their token expires or they refresh)

**Ask First:**
- (none)

**Never:**
- No hard DELETE on the users table
- No cascade-delete of appointments — they are retained and still show the user name + "Removed" badge in appointment views

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|---|---|---|---|
| Delete active user | `DELETE /admin/users/{id}` | 204; `deleted_at` set; user still in list with "Deleted" badge | N/A |
| Delete non-existent user | Unknown id | 404 | N/A |
| Cancel confirmation | User clicks Cancel | No API call; nothing changes | N/A |

</frozen-after-approval>

## Code Map

- `server/src/main/java/com/medour/service/AdminUserService.java` -- add `@Transactional deleteUser(Long userId)`: find by id (404 if absent); set `deletedAt = LocalDateTime.now()`; save
- `server/src/main/java/com/medour/controller/AdminController.java` -- add `@DeleteMapping("/users/{id}") deleteUser(@PathVariable Long id, Authentication auth)` → `adminUserService.deleteUser(id)` → 204 No Content
- `server/src/test/java/com/medour/service/AdminUserServiceTest.java` -- add 2 tests: deleteUser active user → deletedAt set; deleteUser unknown id → 404
- `server/src/test/java/com/medour/controller/AdminControllerTest.java` -- add 1 test: `DELETE /users/1` → 204
- `client/src/stores/userStore.ts` -- add `deleteAdminUser(id: number)` action: `api.delete(\`/admin/users/\${id}\`)` then `fetchAdminUsers()`
- `client/src/views/AdminUsersView.vue` -- add "Delete" button per row; `deleteUser(user)` function: `window.confirm(...)` → if confirmed call `userStore.deleteAdminUser(user.id)`; show `errorMessage` on failure

## Tasks & Acceptance

**Execution:**

- [ ] `server/src/main/java/com/medour/service/AdminUserService.java` -- add `@Transactional deleteUser(Long userId)`: find via `findById` (throw 404 ResponseStatusException if absent); set `user.setDeletedAt(LocalDateTime.now())`; save
- [ ] `server/src/main/java/com/medour/controller/AdminController.java` -- add `@DeleteMapping("/users/{id}") deleteUser(@PathVariable Long id)` calling `adminUserService.deleteUser(id)` → `ResponseEntity.noContent().build()`
- [ ] `server/src/test/java/com/medour/service/AdminUserServiceTest.java` -- add test: active user → deletedAt becomes non-null after delete; non-existent id → 404 ResponseStatusException
- [ ] `server/src/test/java/com/medour/controller/AdminControllerTest.java` -- add test: `@WithMockUser(roles="ADMIN") DELETE /api/v1/admin/users/1 with csrf` → 204
- [ ] `client/src/stores/userStore.ts` -- add `async deleteAdminUser(id: number)` action: `await api.delete(\`/admin/users/\${id}\`)`; then `await fetchAdminUsers()`
- [ ] `client/src/views/AdminUsersView.vue` -- add "Delete" button per user row; `async function deleteUser(user)` calls `window.confirm(\`Delete \${user.firstName} \${user.surname}?\`)` → if true: call `userStore.deleteAdminUser(user.id)` with try/catch; set `errorMessage` on failure

**Acceptance Criteria:**

- Given the admin clicks Delete and confirms, the user's `deleted_at` is set and the row shows the "Deleted" badge on refresh.
- Given the admin clicks Delete and cancels the confirmation, no API call is made.
- Given the admin deletes a non-existent user ID via API, a 404 is returned.
- Given a deleted user's appointments exist, they are untouched and still visible.

## Verification

**Commands:**
- `cd server && ./mvnw test` -- expected: 76 tests pass (73 existing + 2 AdminUserServiceTest + 1 AdminControllerTest)
- `cd client && npm run build` -- expected: zero TypeScript errors

## Spec Change Log

**Review loop 1 patches applied:**
- `AdminUsersView.vue` — added separate `deleteError` ref (shown inline above the list, never hiding the list); `deleteUser` clears `deleteError` before confirming
- `userStore.deleteAdminUser` — post-delete `fetchAdminUsers()` wrapped in its own try/catch so a refresh failure doesn't propagate as a delete failure
- `AdminUserServiceTest` — added `verify(userRepository).save(user)` to `deleteUser_activeUser` test; added missing `import static org.mockito.Mockito.verify`
