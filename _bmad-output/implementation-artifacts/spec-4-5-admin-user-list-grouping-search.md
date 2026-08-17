---
title: "Admin User List — Grouping and Per-Section Search"
type: "feature"
created: "2026-08-17"
status: "in-review"
review_loop_iteration: 0
baseline_commit: "fca775b19cea7d288dac181c694bb302d11f96bb"
context: []
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** The admin users page renders every user in a single uniform grid with no visual grouping; admins cannot quickly scan by role or search within a role group.

**Approach:** Split the flat list into three labelled sections — Admins (full-width row, no search), Doctors (grid with search field), Patients (grid with search field) — all driven by computed filtering of the existing `userStore.adminUsers` state; no new API calls.

## Boundaries & Constraints

**Always:**

- Section order is fixed: Admins first (full-width row), then Doctors, then Patients.
- Admin cards span full width (`cols="12"`) regardless of screen size — there is typically only one or very few admins.
- Doctor and Patient cards keep the current `cols="12" md="6" lg="4"` grid.
- Each of the Doctor and Patient sections has its own independent search field that filters in real time by `firstName` or `surname` (case-insensitive substring match); the search state is local to the component (two `ref<string>` values).
- Section subheaders use a consistent Vuetify style — `VDivider` + label or `<h2>` with `text-subtitle-1 font-weight-bold` — matching the existing page typography.
- A section with zero matching users after search shows a small "No users found" inline message, not an empty space.
- If a section has zero users at all (before search), the section is hidden entirely (including its subheader).
- All existing card content, actions, expansion panels, badges, and dialogs remain unchanged.
- `userStore.adminUsers` is the only data source; no new store state, actions, or API calls.

**Ask First:**

- (none)

**Never:**

- No server-side search or pagination.
- Do not add a search field to the Admins section.
- Do not change any card layout, detail fields, Edit/Delete buttons, or dialog behaviour.
- No new routes or navigation changes.

## I/O & Edge-Case Matrix

| Scenario                    | Input / State                     | Expected Output / Behavior                                                           | Error Handling |
| --------------------------- | --------------------------------- | ------------------------------------------------------------------------------------ | -------------- |
| Search matches no doctors   | doctorSearch = "zzz"              | Doctors section shows "No users found" inline; Patients section unaffected           | N/A            |
| Search matches subset       | patientSearch = "ion"             | Only patients whose firstName or surname contains "ion" (case-insensitive) are shown | N/A            |
| Section has no users at all | adminUsers has no PATIENT records | Patients section (subheader + search + grid) is not rendered                         | N/A            |
| Search field cleared        | user deletes search text          | Full unfiltered list for that section is restored immediately                        | N/A            |

</frozen-after-approval>

## Code Map

- `client/src/views/AdminUsersView.vue` -- sole file to change; currently renders a flat `v-for="user in userStore.adminUsers"` grid; add two `ref<string>` search vars (`doctorSearch`, `patientSearch`) and three computed arrays (`admins`, `filteredDoctors`, `filteredPatients`) replacing the single loop; restructure template into three sections
- `client/src/stores/userStore.ts` -- read-only; `AdminUser.role` values are `'ADMIN'`, `'DOCTOR'`, `'PATIENT'`; `AdminUser.firstName` and `AdminUser.surname` are the search targets

## Tasks & Acceptance

**Execution:**

- [x] `client/src/views/AdminUsersView.vue` -- in `<script setup>`: add `import { ref, computed, onMounted } from 'vue'` (already partially imported — adjust); add `const doctorSearch = ref('')` and `const patientSearch = ref('')`; add computed `admins` = `userStore.adminUsers.filter(u => u.role === 'ADMIN')`; add computed `filteredDoctors` = filter by `DOCTOR` then by `doctorSearch` substring on `firstName` or `surname`; add computed `filteredPatients` = same pattern for `PATIENT` and `patientSearch`
- [x] `client/src/views/AdminUsersView.vue` -- in `<template>`: replace the single `VRow v-for` with three sections; **Admins section**: render only when `admins.length > 0`; subheader label "Admins"; one `VRow` with `VCol cols="12"` cards; **Doctors section**: render only when `userStore.adminUsers.some(u => u.role === 'DOCTOR')`; subheader "Doctors"; `VTextField` bound to `doctorSearch` (label "Search doctors…", prepend-inner-icon `mdi-magnify`, variant outlined, density compact, clearable, hide-details, `max-width: 320px`, class `mb-4`); `VRow` iterating `filteredDoctors`; show "No users found." `text-caption text-medium-emphasis` when `filteredDoctors.length === 0`; **Patients section**: mirror Doctors pattern with `patientSearch` and `filteredPatients`

**Acceptance Criteria:**

- Given the admin loads the page, when users of all three roles exist, then Admins appear first full-width, followed by a Doctors subheader with search, followed by a Patients subheader with search.
- Given no PATIENT users exist, when the page loads, then the Patients section (subheader, search field, grid) is not rendered.
- Given the admin types in the Doctors search field, when the text matches a subset of doctors by first name or surname, then only matching doctor cards are shown; the Patients section is unaffected.
- Given the search field is cleared, when the input is empty, then all doctors (or patients) are shown again.
- Given a search term matches no doctors, when the grid would be empty, then a "No users found." message is shown in place of the grid.

## Verification

**Commands:**

- `cd client && npm run build` -- expected: zero TypeScript errors, zero lint errors

**Manual checks (if no CLI):**

- Navigate to `/admin/users`: confirm three labelled sections appear in correct order.
- Type in Doctors search field: confirm real-time filtering; confirm Patients section is unaffected.
- Clear Doctors search: confirm all doctors reappear.
