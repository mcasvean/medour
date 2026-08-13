---
title: "Admin User List & Detail View"
type: "feature"
created: "2026-08-13"
status: "done"
review_loop_iteration: 0
baseline_commit: "39c4bea33ce2b0b979bb3e4e5eb4f8f11293a6d8"
context: []
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** The `/admin/users` view is a stub. There is no endpoint returning all users, no admin user list component, and no expandable detail panel.

**Approach:** Add `GET /api/v1/admin/users` returning every user regardless of `deletedAt`; populate `userStore` with an `adminUsers` list and `fetchAdminUsers()` action; replace the stub `AdminUsersView.vue` with a real list where each row is expandable to show the full user profile, with soft-deleted users visually distinguished.

## Boundaries & Constraints

**Always:**
- `GET /api/v1/admin/users` returns ALL users including soft-deleted ones; no `deleted_at IS NULL` filter
- The endpoint is automatically ADMIN-only via the existing SecurityConfig `/api/v1/admin/**` rule — no additional annotation needed
- `AdminUserDto` exposes: id, email, firstName, surname, role, speciality, county, city, age, gender, address, mustChangePassword, isDeleted (boolean derived from `deletedAt != null`)
- Soft-deleted users are displayed with a visual "Deleted" indicator (e.g., greyed row + badge); they remain in the list
- Clicking a user row toggles an expanded detail panel showing all DTO fields; only one row expanded at a time
- `passwordHash` and `deletedAt` timestamp are never returned to the client — only the derived `isDeleted` boolean
- `userStore` gains `adminUsers: AdminUser[]` state and `fetchAdminUsers()` action
- Admin must be able to navigate from App.vue header → `/admin/users`; the "Users" link already exists in the burger menu from Story 1.4

**Ask First:**
- (none)

**Never:**
- No create/edit/delete actions in this story (Stories 4.2 and 4.3)
- No appointment management in this story (Story 4.4)
- Do not return `passwordHash` or `deletedAt` timestamp in any response

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|---|---|---|---|
| Admin loads page | `GET /api/v1/admin/users` | 200 + all users including soft-deleted | N/A |
| Soft-deleted user in list | `deletedAt != null` | Row shows user data + visual "Deleted" indicator | N/A |
| Click user row | row not expanded | Detail panel opens showing all profile fields | N/A |
| Click expanded row | row already expanded | Detail panel closes | N/A |
| Non-admin call | PATIENT or DOCTOR JWT | 403 (SecurityConfig) | N/A |

</frozen-after-approval>

## Code Map

- `server/src/main/java/com/medour/dto/AdminUserDto.java` -- NEW record: `Long id, String email, String firstName, String surname, String role, String speciality, String county, String city, Integer age, String gender, String address, boolean mustChangePassword, boolean isDeleted`
- `server/src/main/java/com/medour/service/AdminUserService.java` -- NEW `@Service`; inject `UserRepository`; `@Transactional(readOnly=true) getAllUsers()` → `userRepository.findAll()` mapped to `AdminUserDto`
- `server/src/main/java/com/medour/controller/AdminController.java` -- add `GET /users` → `ResponseEntity.ok(adminUserService.getAllUsers())`
- `server/src/test/java/com/medour/service/AdminUserServiceTest.java` -- NEW: 2 tests — active user mapped with isDeleted=false; soft-deleted user mapped with isDeleted=true
- `server/src/test/java/com/medour/controller/AdminControllerTest.java` -- add 1 test: `@WithMockUser(roles="ADMIN") GET /users` → 200 + array
- `client/src/stores/userStore.ts` -- add `interface AdminUser { id, email, firstName, surname, role, speciality, county, city, age, gender, address, mustChangePassword, isDeleted }`; add `adminUsers: AdminUser[]` state; add `fetchAdminUsers()` action
- `client/src/views/AdminUsersView.vue` -- replace stub with: fetch on mount; list of rows with user name + role + "Deleted" badge if isDeleted; toggled detail panel per row; uses `expandedUserId` ref

## Tasks & Acceptance

**Execution:**

- [ ] `server/src/main/java/com/medour/dto/AdminUserDto.java` -- NEW record with 13 fields; derive `isDeleted` from call site, not from entity field
- [ ] `server/src/main/java/com/medour/service/AdminUserService.java` -- NEW `@Service`; `getAllUsers()`: `userRepository.findAll().stream().map(u -> new AdminUserDto(u.getId(), u.getEmail(), u.getFirstName(), u.getSurname(), u.getRole().name(), u.getSpeciality(), u.getCounty(), u.getCity(), u.getAge(), u.getGender(), u.getAddress(), u.getMustChangePassword(), u.getDeletedAt() != null)).toList()`
- [ ] `server/src/main/java/com/medour/controller/AdminController.java` -- inject `AdminUserService`; add `@GetMapping("/users") getUsers()` → `ResponseEntity.ok(adminUserService.getAllUsers())`
- [ ] `server/src/test/java/com/medour/service/AdminUserServiceTest.java` -- NEW; 2 tests: active user → isDeleted=false; deleted user → isDeleted=true
- [ ] `server/src/test/java/com/medour/controller/AdminControllerTest.java` -- add `@MockBean AdminUserService`; add test: `@WithMockUser(roles="ADMIN") GET /api/v1/admin/users` → 200 + array length
- [ ] `client/src/stores/userStore.ts` -- rewrite to add `AdminUser` interface; state `adminUsers: AdminUser[]`; action `fetchAdminUsers()` calling `api.get('/admin/users')`
- [ ] `client/src/views/AdminUsersView.vue` -- replace stub with full view: `onMounted` calls `userStore.fetchAdminUsers()`; renders list; each item has click toggle on `expandedUserId`; when expanded shows all profile fields in a detail panel; "Deleted" badge when `user.isDeleted`

**Acceptance Criteria:**

- Given an admin navigates to `/admin/users`, all users (including soft-deleted) are listed.
- Given a soft-deleted user is in the list, their row shows a "Deleted" badge.
- Given the admin clicks a user row, the detail panel opens with all profile fields.
- Given the admin clicks the same row again, the detail panel closes.
- Given a PATIENT or DOCTOR JWT calls `GET /api/v1/admin/users`, the server returns 403.

## Verification

**Commands:**
- `cd server && ./mvnw test` -- expected: 66 tests pass (63 existing + 2 AdminUserServiceTest + 1 AdminControllerTest)
- `cd client && npm run build` -- expected: zero TypeScript errors

## Spec Change Log

**Review loop 1 patches applied:**
- `AdminUsersView.vue` — `onMounted` now awaits `fetchAdminUsers()` with `loading` ref, `errorMessage` ref, and try/catch/finally; loading spinner and error message rendered
- `AdminUsersView.vue` — `@click.stop` added to `.user-detail` div so clicking inside the panel doesn't collapse the row
- `AdminControllerTest` — added `$.email` and `$.isDeleted` field assertions to the GET /users test
