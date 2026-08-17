---
title: "Admin Reset User Password — UI"
type: "feature"
created: "2026-08-17"
status: "done"
review_loop_iteration: 0
baseline_commit: "a86d9c668da03522f429528bfd2237c8052284ba"
context: []
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** The backend endpoint `POST /api/v1/admin/users/{id}/password` is already implemented and accepts `{ newPassword }`, but the admin edit-user form has no UI surface for it — admins cannot reset a user's password from the panel.

**Approach:** Add a visually separated "Reset Password" section at the bottom of `AdminUserForm.vue` (edit mode only) with a warning label and a new-password field; wire a `resetAdminUserPassword` store action that calls the existing endpoint; in `AdminUsersView.vue`'s `handleSave`, call the password reset endpoint when the field is filled, independently of the profile update call.

## Boundaries & Constraints

**Always:**

- The reset-password section is shown in **edit mode only** — create mode already has a required password field.
- The section is visually separated from the rest of the form with a `VDivider` and a warning-coloured header or alert label (e.g. `VAlert type="warning"` or a `VCardTitle color="warning"`) that reads: _"Reset Password — This will immediately replace the user's current password."_
- The new-password field includes a show/hide toggle (same pattern as the existing create-mode password field in `AdminUserForm.vue`).
- The field is **optional** — if left blank, no password-reset call is made and no error is shown.
- The password reset is fired as a **separate** request after the profile update (`PUT /admin/users/{id}`), not merged into the same payload.
- Both operations must succeed independently; a profile-update success followed by a password-reset failure shows separate feedback (success toast for update, error toast for password reset).
- `userStore` gains a new `resetAdminUserPassword(id: number, newPassword: string): Promise<void>` action that calls `POST /api/v1/admin/users/{id}/password` with `{ newPassword }`.
- The `AdminUserForm.vue` emits the new-password value via the existing `save` event payload as `newPassword?: string` (only set when the field is non-empty).
- All HTTP calls go through the shared Axios instance in `client/src/api/`.

**Ask First:**

- Whether the password-reset field should have a client-side minimum length constraint (e.g. 8 characters), or whether validation is left entirely to the backend.

**Never:**

- Do not merge `newPassword` into the `PUT /admin/users/{id}` payload — the profile update and password reset are distinct endpoints.
- Do not show the reset-password section in create mode.
- Do not add a separate "Reset Password" submit button — the existing Save button drives both calls when the field is filled.

## I/O & Edge-Case Matrix

| Scenario                      | Input / State                                | Expected Output / Behavior                                                        | Error Handling                                                 |
| ----------------------------- | -------------------------------------------- | --------------------------------------------------------------------------------- | -------------------------------------------------------------- |
| Edit with password filled     | Save button clicked, `newPassword` non-empty | Profile `PUT` fires first; on success, password `POST` fires; success toast shown | Password POST error → error toast; profile save still succeeds |
| Edit with password empty      | Save button clicked, `newPassword` blank     | Only profile `PUT` fires; no password-reset call                                  | —                                                              |
| Backend rejects weak password | `newPassword` fails backend validation       | Error toast with server message; form stays open                                  | —                                                              |
| User opens edit form          | Any user                                     | Reset-password section visible and empty; no pre-filled value                     | —                                                              |

</frozen-after-approval>

## Code Map

- `client/src/components/AdminUserForm.vue` — **EXTEND** edit-mode template: add `VDivider` + warning section + new-password `VTextField` with show/hide toggle; add `newPassword` to emitted `save` payload (only when non-empty); add `showNewPassword` ref
- `client/src/views/AdminUsersView.vue` — **EXTEND** `handleSave()`: after successful `userStore.updateAdminUser(...)`, check for `payload.newPassword` and call `userStore.resetAdminUserPassword(id, payload.newPassword)` if set; show separate toasts for profile update and password reset
- `client/src/stores/userStore.ts` — **EXTEND**: add `resetAdminUserPassword(id: number, newPassword: string): Promise<void>` action that `POST`s to `/api/v1/admin/users/${id}/password` with `{ newPassword }`
- `server/src/main/java/com/medour/controller/AdminController.java` — **READ-ONLY**: `POST /users/{id}/password` accepts `AdminSetPasswordRequest { @NotBlank String newPassword }`, returns 204; already implemented
- `server/src/main/java/com/medour/service/UserService.java` — **READ-ONLY**: `adminSetPassword(Long id, AdminSetPasswordRequest)` already implemented

## Tasks & Acceptance

**Execution:**

- [x] `client/src/stores/userStore.ts` — add `resetAdminUserPassword(id, newPassword)` action; POST to `/api/v1/admin/users/${id}/password` with `{ newPassword }`; throw on non-2xx so caller can handle separately
- [x] `client/src/components/AdminUserForm.vue` — in edit-mode template: after the last field group, add `VDivider`, then a `VAlert type="warning" variant="tonal"` with the warning text, then a `VTextField` (type password, show/hide toggle via `showNewPassword` ref); append `...(form.newPassword ? { newPassword: form.newPassword } : {})` to the `save` emit payload
- [x] `client/src/views/AdminUsersView.vue` — in `handleSave()` edit path: after `updateAdminUser` succeeds, if `payload.newPassword` is set call `resetAdminUserPassword`; each call shows its own success/error toast via `toastStore`

**Acceptance Criteria:**

- Given the admin opens the edit form for any user, the Reset Password section is visible, empty, and below a divider.
- Given the admin leaves the new-password field blank and saves, only the profile `PUT` fires — no password-reset request is made.
- Given the admin fills in a new password and saves, both `PUT /admin/users/{id}` and `POST /admin/users/{id}/password` are called; a success toast appears for each.
- Given the password reset call returns an error, an error toast is shown and the profile-update success toast is still displayed.
- Given the admin opens the edit form in create mode, the Reset Password section does not appear.

## Verification

**Commands:**

- `cd client && npm run build` -- expected: zero type errors, clean build
- `cd client && npx vitest run` -- expected: all existing tests pass; no regressions

## Suggested Review Order

**Orchestration — two-step save flow**

- Entry point: edit-mode save dispatches profile update then optional password reset
  [`AdminUsersView.vue:338`](../../client/src/views/AdminUsersView.vue#L338)

- `newPassword` destructured out of payload before the profile PUT — keeps reset separate
  [`AdminUsersView.vue:349`](../../client/src/views/AdminUsersView.vue#L349)

- Error path returns early, leaving the form open so the admin can retry
  [`AdminUsersView.vue:362`](../../client/src/views/AdminUsersView.vue#L362)

**Store action**

- Single POST; throws on non-2xx so the caller controls toast/retry logic
  [`userStore.ts:44`](../../client/src/stores/userStore.ts#L44)

**Form UI & payload construction**

- Reset-password section rendered only in edit mode, below a divider
  [`AdminUserForm.vue:99`](../../client/src/components/AdminUserForm.vue#L99)

- Whitespace trimmed; field omitted from payload when blank
  [`AdminUserForm.vue:192`](../../client/src/components/AdminUserForm.vue#L192)

**Tests**

- Store action: correct endpoint and throws on error
  [`userStore.test.ts:24`](../../client/src/stores/__tests__/userStore.test.ts#L24)
