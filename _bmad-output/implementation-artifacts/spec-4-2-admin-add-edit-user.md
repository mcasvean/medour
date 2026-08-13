---
title: "Admin Add/Edit User"
type: "feature"
created: "2026-08-13"
status: "done"
review_loop_iteration: 0
baseline_commit: "6e252756f96d2f68d2a4e4301dbd2dba9b1772fd"
context: []
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** The admin can view users but cannot create new ones or edit existing profiles and roles. The "Add User" and "Edit" actions are missing from both the API and the UI.

**Approach:** Add `POST /api/v1/admin/users` (create user) and `PUT /api/v1/admin/users/{id}` (update profile + role); wire "Add User" and "Edit" buttons in `AdminUsersView` that open a shared form component pre-filled for edits or empty for create; on success refresh the admin user list.

## Boundaries & Constraints

**Always:**

- Admin create sets any role (PATIENT, DOCTOR, ADMIN); no self-registration restriction
- Email is unique — duplicate email on create returns 409 `{ "error": "Email already in use" }`
- `county` and `speciality` are required when role is DOCTOR; 400 returned if absent for DOCTOR
- Password for new users is required in `AdminUserCreateRequest`; password is hashed before saving
- `PUT /api/v1/admin/users/{id}` does NOT change email or password — only profile fields + role
- Soft-deleted users can be updated (their `deletedAt` is not cleared by this endpoint)
- After create or update, `userStore.fetchAdminUsers()` is re-called to refresh the list
- Both endpoints are auto-protected by the existing SecurityConfig `/api/v1/admin/**` rule

**Ask First:**

- (none)

**Never:**

- No hard-delete in this story (Story 4.3)
- Never return `passwordHash` in any response
- Email change via PUT is not supported (Story 4.2 scope excludes it)

## I/O & Edge-Case Matrix

| Scenario                     | Input / State                            | Expected Output / Behavior                | Error Handling              |
| ---------------------------- | ---------------------------------------- | ----------------------------------------- | --------------------------- |
| Create valid patient         | POST with role=PATIENT + all base fields | 201 + `AdminUserDto`; user created        | N/A                         |
| Create doctor missing county | POST with role=DOCTOR, no county         | 400                                       | Service validation          |
| Duplicate email              | POST with existing email                 | 409 `{ "error": "Email already in use" }` | `EmailAlreadyUsedException` |
| Update role to ADMIN         | PUT {id} with role=ADMIN                 | 200 + updated `AdminUserDto`              | N/A                         |
| PUT non-existent user        | PUT unknown id                           | 404                                       | N/A                         |

</frozen-after-approval>

## Code Map

- `server/src/main/java/com/medour/dto/AdminUserCreateRequest.java` -- NEW: `@NotBlank @Email email`, `@NotBlank password`, `@NotBlank firstName`, `@NotBlank surname`, nullable `age`, `gender`, `city`, `address`, `county`, `speciality`, `@NotBlank role`
- `server/src/main/java/com/medour/dto/AdminUserUpdateRequest.java` -- NEW: `@NotBlank firstName`, `@NotBlank surname`, nullable `age`, `gender`, `city`, `address`, `county`, `speciality`, `@NotBlank role` (no email, no password)
- `server/src/main/java/com/medour/service/AdminUserService.java` -- add `createUser(AdminUserCreateRequest)` and `updateUser(Long userId, AdminUserUpdateRequest)` methods; inject `PasswordEncoder`
- `server/src/main/java/com/medour/controller/AdminController.java` -- add `POST /users` → 201 + `AdminUserDto`; `PUT /users/{id}` → 200 + `AdminUserDto`
- `server/src/test/java/com/medour/service/AdminUserServiceTest.java` -- add 3 tests: create patient; create doctor with county/speciality; update role
- `server/src/test/java/com/medour/controller/AdminControllerTest.java` -- add 2 tests: POST /users → 201; PUT /users/{id} → 200
- `client/src/stores/userStore.ts` -- add `createAdminUser(data)` and `updateAdminUser(id, data)` actions
- `client/src/views/AdminUsersView.vue` -- add "Add User" button opening `AdminUserForm`; "Edit" button per row opening form pre-filled; on save call store action + refresh list
- `client/src/components/AdminUserForm.vue` -- NEW: all user fields (email/password only for create); role select; submit calls parent callback

## Tasks & Acceptance

**Execution:**

- [x] `server/src/main/java/com/medour/dto/AdminUserCreateRequest.java` -- NEW: `@Data @NoArgsConstructor @AllArgsConstructor`; annotated fields as above
- [x] `server/src/main/java/com/medour/dto/AdminUserUpdateRequest.java` -- NEW: same but without email/password fields
- [x] `server/src/main/java/com/medour/service/AdminUserService.java` -- inject `PasswordEncoder`; add `@Transactional createUser(req)`: check email unique (throw `EmailAlreadyUsedException`); validate DOCTOR fields; hash password; save; return `AdminUserDto`; add `@Transactional updateUser(Long userId, req)`: load user (404 if absent); validate DOCTOR fields; update all non-email fields; update role; save; return `AdminUserDto`
- [x] `server/src/main/java/com/medour/controller/AdminController.java` -- add `@PostMapping("/users") createUser(@Valid @RequestBody AdminUserCreateRequest req)` → `ResponseEntity.status(201).body(adminUserService.createUser(req))`; add `@PutMapping("/users/{id}") updateUser(@PathVariable Long id, @Valid @RequestBody AdminUserUpdateRequest req)` → `ResponseEntity.ok(adminUserService.updateUser(id, req))`
- [x] `server/src/test/java/com/medour/service/AdminUserServiceTest.java` -- add mock `PasswordEncoder`; add test: valid patient create → saved + returns dto; add test: DOCTOR missing county → 400; add test: updateUser valid → role changed + returns dto
- [x] `server/src/test/java/com/medour/controller/AdminControllerTest.java` -- add mock for `createUser` / `updateUser`; test `POST /users` → 201 + `$.email`; test `PUT /users/1` → 200 + `$.role`
- [x] `client/src/stores/userStore.ts` -- add `createAdminUser(data: Omit<AdminUser, 'id' | 'isDeleted'> & { password: string })` action calling `api.post('/admin/users', data)`; add `updateAdminUser(id: number, data: Partial<AdminUser>)` action calling `api.put(\`/admin/users/\${id}\`, data)`; both call `fetchAdminUsers()` on success
- [x] `client/src/components/AdminUserForm.vue` -- NEW: `defineProps<{ user?: AdminUser, mode: 'create' | 'edit' }>`; reactive form bound to all fields (email + password only for create); role `<select>`; submit emits `save` with payload; cancel emits `cancel`
- [x] `client/src/views/AdminUsersView.vue` -- add `showForm`, `formMode`, `editingUser` refs; "Add User" button sets `showForm=true, formMode='create'`; "Edit" button per row sets `showForm=true, formMode='edit', editingUser=user`; show `<AdminUserForm>` overlay when `showForm=true`; on `@save` call appropriate store action then `showForm=false`

**Acceptance Criteria:**

- Given the admin clicks "Add User" and fills in valid fields, a new user is created and appears in the list.
- Given the admin fills in a DOCTOR form without county, a 400 error is shown.
- Given the admin tries to create with a duplicate email, a 409 error is shown.
- Given the admin clicks "Edit" on a user and changes the role, the updated user is shown in the list.
- Given the admin edits a non-existent user ID directly via API, a 404 is returned.

## Verification

**Commands:**

- `cd server && ./mvnw test` -- expected: 71 tests pass (66 existing + 3 AdminUserServiceTest + 2 AdminControllerTest)
- `cd client && npm run build` -- expected: zero TypeScript errors

## Spec Change Log

**Review loop 1 patches applied:**

- `AdminUsersView.vue` — `formSaveError` ref added; passed as `:save-error` prop to `AdminUserForm`; `:key` on form to force remount when `editingUser` changes; `handleSave` writes to `formSaveError` not the page-level `errorMessage`
- `AdminUserForm.vue` — accepts `saveError?: string` prop; displays `saveError || errorMessage` in the form error div
- `AdminUserServiceTest` — added `createUser_duplicateEmail_throwsEmailAlreadyUsedException` and `updateUser_nonExistentId_throws404` tests
