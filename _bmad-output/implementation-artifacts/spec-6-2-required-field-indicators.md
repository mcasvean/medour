---
title: "Required Field Visual Indicators"
type: "feature"
created: "2026-08-14"
status: "ready-for-dev"
review_loop_iteration: 0
baseline_commit: ""
context: ["spec-6-1-toast-notification-system"]
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Forms across the application contain required fields that are not visually distinguished from optional ones. Users submit incomplete forms and receive validation errors only after the fact, with no upfront cue about which fields must be filled.

**Approach:** Add a red asterisk (`*`) to the label of every required field across all forms: registration forms, account info, admin user form, and the change-password form. The asterisk is rendered as part of the field label using Vuetify's `label` slot or by appending `*` to the label string with a styled `<span>`. Additionally, replace inline VAlert success/error banners in `AccountView.vue` and `ChangePasswordView.vue` with `toastStore` calls (requires Story 6.1 to be merged first).

## Boundaries & Constraints

**Always:**

- The asterisk must be visually distinct: red (`#EF5350` or `color: error`), positioned after the label text with a small left margin.
- Required fields by form:
  - **PatientRegisterForm**: email, password, first name, surname
  - **DoctorRegisterForm**: email, password, first name, surname, speciality
  - **AccountView** (all roles): first name, surname
  - **AdminUserForm**: email, password (add only), first name, surname, role
  - **ChangePasswordView**: current password, new password, confirm password
- Optional fields are never marked — do not add anything to age, gender, city, address, county.
- Replace inline VAlert banners in `AccountView.vue` and `ChangePasswordView.vue` with `toastStore.show()`/`toastStore.showError()` calls; remove the corresponding `successMessage`/`errorMessage` refs.

**Ask First:**

- (none)

**Never:**

- Do not add HTML5 `required` attribute validation beyond what already exists — Vuetify's `required` prop already handles the native browser constraint.
- Do not change backend validation — it is already enforced server-side.
- Do not add asterisks to read-only / disabled fields (email, role in AccountView).

## I/O & Edge-Case Matrix

| Scenario                           | Input / State                      | Expected Output / Behavior                                                       | Error Handling |
| ---------------------------------- | ---------------------------------- | -------------------------------------------------------------------------------- | -------------- |
| Patient registration form rendered | Any user opens it                  | Email, password, first name, surname labels show red `*`                         | —              |
| Doctor registration form rendered  | Any user opens it                  | Same as patient + speciality shows red `*`                                       | —              |
| Admin user form — add mode         | Admin opens "Add User"             | Email, password, first name, surname, role show red `*`                          | —              |
| Admin user form — edit mode        | Admin opens existing user for edit | Email, first name, surname, role show red `*`; password field hidden or optional | —              |
| AccountView saved successfully     | User saves valid profile           | Success toast fires; no inline VAlert remains                                    | —              |
| AccountView save fails             | Server returns error               | Error toast with server message fires; no inline VAlert remains                  | —              |
| ChangePasswordView success         | Password changed                   | Success toast fires                                                              | —              |

</frozen-after-approval>

## Code Map

- `client/src/views/PatientRegisterForm.vue` — **EXTEND** append `<span class="text-error ml-1">*</span>` inside the `label` slot of VTextField for email, password, first name, surname
- `client/src/views/DoctorRegisterForm.vue` — **EXTEND** same treatment for email, password, first name, surname, speciality (the text field, to be replaced with a select in Story 8.2 — apply asterisk to the label regardless of field type)
- `client/src/views/AccountView.vue` — **EXTEND** append `*` span to first name and surname VTextField labels; import `useToastStore`; replace `successMessage` ref usage with `toastStore.show(...)` and `errorMessage` usage with `toastStore.showError(...)`; remove both refs
- `client/src/components/AdminUserForm.vue` — **EXTEND** append `*` span to email, first name, surname, role field labels; for password: mark required only when `props.mode === 'add'`
- `client/src/views/ChangePasswordView.vue` — **EXTEND** append `*` to current password, new password, confirm password labels; replace any inline error/success VAlert with `toastStore` calls

## Tasks & Acceptance

**Execution:**

- [ ] `client/src/views/PatientRegisterForm.vue` — on VTextField for `email`, `password`, `firstName`, `surname`: use Vuetify's label slot (`<template #label>Label <span class="text-error">*</span></template>`) to append a red asterisk
- [ ] `client/src/views/DoctorRegisterForm.vue` — same label-slot treatment for `email`, `password`, `firstName`, `surname`, `speciality`
- [ ] `client/src/views/AccountView.vue` — label-slot asterisk on `firstName` and `surname` VTextFields; `import { useToastStore }` from `'../stores/toastStore'`; replace `successMessage` assignment with `toastStore.show('Profile updated.', 'success')`; replace `errorMessage` assignment with `toastStore.showError(error)`; delete both `ref()` declarations
- [ ] `client/src/components/AdminUserForm.vue` — label-slot asterisk on `email`, `firstName`, `surname`, `role` VTextField/VSelect; password field label asterisk conditionally rendered with `v-if="props.mode === 'add'"`
- [ ] `client/src/views/ChangePasswordView.vue` — label-slot asterisk on `currentPassword`, `newPassword`, `confirmPassword`; replace inline alert refs with `toastStore` calls

## Acceptance Criteria

- Given the patient registration form is open, email/password/first name/surname labels each display a red asterisk.
- Given the doctor registration form is open, all of the above plus speciality display the asterisk.
- Given an admin edits a user, email/first name/surname/role show the asterisk but password does not (edit mode).
- Given an admin adds a user, password also shows the asterisk (add mode).
- Given the user saves account info successfully, a success toast appears and no inline VAlert is shown.
- Given account info save fails with a server error, an error toast with the server message appears and no inline VAlert is shown.
- Optional fields (age, gender, city, address, county) show no asterisk on any form.

## Verification

**Commands:**

- `cd client && npm run build` — expected: zero TypeScript errors
- `cd client && npm run test` — expected: all existing tests pass
