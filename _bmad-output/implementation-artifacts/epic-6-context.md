# Epic 6 Context: UX Polish & Global Feedback

<!-- Compiled from planning artifacts. Edit freely. Regenerate with compile-epic-context if planning docs change. -->

## Goal

Epic 6 unifies four frontend user experience improvements that make the application feel polished and personalised. It adds a global toast notification system so users always know when actions succeed or fail, marks required form fields with red asterisks for clarity, improves header navigation so the app name navigates home and logout moves to a cleaner burger menu, and introduces a light/dark theme toggle that persists across sessions. No backend changes required.

## Stories

- Story 6.1: Toast Notification System
- Story 6.2: Required Field Visual Indicators
- Story 6.3: Header & Navigation Improvements
- Story 6.4: Light/Dark Mode Toggle

## Requirements & Constraints

**Toast notifications:**

- Max 5 toasts visible simultaneously; oldest is auto-removed when the limit is exceeded.
- Types: `error` (red, alert icon), `success` (green, check icon), `warning` (amber, warning icon).
- Auto-dismiss after 5 000 ms; manual close via ✕ button dismisses immediately.
- Position: fixed, bottom-right, z-index above all elements.
- Error toasts extract the server message from `error.response?.data?.message` or `.error` field; fall back to `'An unexpected error occurred'` if neither exists.
- Single `ToastNotification.vue` component mounted once in `App.vue`; all notifications dispatched through `toastStore`.

**Required field indicators:**

- Red asterisk (`#EF5350` or Vuetify `color: error`) appended to label text of required fields.
- Applied to: PatientRegisterForm (email, password, first name, surname); DoctorRegisterForm (above + speciality); AccountView (first name, surname); AdminUserForm (email, first name, surname, role; password required in add mode only); ChangePasswordView (current, new, confirm password).
- Optional fields (age, gender, city, address, county) show no asterisk on any form.
- Replace existing inline VAlert banners in AccountView and ChangePasswordView with toast calls.

**Header & navigation:**

- App name "Medour" is clickable to `/` when authenticated; plain text and non-clickable when unauthenticated.
- Logout button removed from header `#append` slot entirely.
- New "Sign Out" list item added to burger menu as the last item, styled red (`color="error"`), separated from other items by a `VDivider`.
- Gap between role chip and username text increased from `ga-2` to `ga-4` for better visual breathing room.

**Light/Dark mode:**

- Use Vuetify's `useTheme()` composable to switch between `'light'` and `'dark'` themes at runtime.
- Toggle button: sun icon (`mdi-weather-sunny`) when light is active, moon icon (`mdi-weather-night`) when dark is active.
- Theme preference persisted to `localStorage` under key `'medour_theme'`.
- Restore theme on app load from localStorage; default to `'light'` if no preference stored.
- Remove hard-coded `style="background: #F5F7FA"` from `<VMain>` so Vuetify's dark surface colour applies in dark mode.
- Toggle button visible to both authenticated and unauthenticated users (works on login page too).

## Technical Decisions

**Pinia store for toasts (toastStore):**

- New `toastStore` added alongside existing domain-scoped stores (authStore, appointmentStore, userStore, doctorStore).
- State: `toasts: Toast[]` array with max length 5.
- Interface: `Toast { id: number; message: string; type: 'error' | 'success' | 'warning' }`.
- Actions:
  - `show(message: string, type: 'error' | 'success' | 'warning')`: push with unique id, auto-dismiss via `setTimeout(..., 5000)`, remove oldest if length > 5.
  - `showError(error: unknown)`: extract server message from AxiosError, call `show(message, 'error')`.
  - `dismiss(id: number)`: remove toast by id.

**Component architecture:**

- `ToastNotification.vue` (new): fixed bottom-right component, `v-for` over `toastStore.toasts`, CSS transitions for enter/leave.
- Mount once in `App.vue` (after `</VMain>`), never import per-view.

**Theme integration:**

- In `App.vue` `<script setup>`: import `useTheme` from `'vuetify'`, compute `isDark` from `theme.global.name.value`.
- In `onMounted`, read `localStorage.getItem('medour_theme')` and apply if present.
- Toggle function: flip `theme.global.name.value` and persist to localStorage.
- Header `#append` prepends the toggle button before the role chip div.

**Styling conventions:**

- Required field asterisk: Vuetify label slot (`<template #label>Label <span class="text-error">*</span></template>`).
- Toast colours: error `#EF5350`, success `#4CAF50`, warning `#FB8C00`.
- Logout button: Vuetify `color="error"` for red tint.

## UX & Interaction Patterns

**Toast lifecycle:** action triggers → `toastStore.show(...)` → toast appears bottom-right with icon → auto-fades after 5 s or user clicks ✕. Error context shown in toast; inline VAlert banners in AccountView and ChangePasswordView are replaced.

**Required field signalling:** red asterisks on required field labels across all forms; optional fields have no visual marker.

**Header navigation:** authenticated click on "Medour" → `/`; unauthenticated click → nothing. Logout moved to burger menu bottom.

**Theme UX:** toggle icon in header for all users; choice persists via localStorage; no flicker on next load.

## Cross-Story Dependencies

- Story 6.1 must merge before Story 6.2 (AccountView/ChangePasswordView depend on `toastStore`).
- Stories 6.3 and 6.4 are independent of each other and of 6.1/6.2 — can be developed in parallel.
