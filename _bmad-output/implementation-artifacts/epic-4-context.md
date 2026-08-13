# Epic 4 Context: Admin Panel & User Management

<!-- Compiled from planning artifacts. Edit freely. Regenerate with compile-epic-context if planning docs change. -->

## Goal

Give the admin full visibility and control over all users and appointments. The admin can view, create, edit, and soft-delete any user (including role assignment and temporary password reset) and can view and delete any appointment. All destructive actions require a confirmation step. Soft-deleted users retain their data and show a "Removed" badge on appointment cards.

## Stories

- Story 4.1: Admin User List & Detail View
- Story 4.2: Admin Add/Edit User
- Story 4.3: Admin Delete User (Soft-Delete)
- Story 4.4: Admin Appointment Management

## Requirements & Constraints

- Admin user list includes ALL users (patients, doctors, other admins) — including soft-deleted ones.
- Soft-deleted users are visually distinct in the list (e.g., greyed-out row, "Deleted" badge).
- Clicking a user row expands the full profile (all editable fields + read-only id and email).
- User deletion is soft-delete only: `deleted_at = now()`; `passwordHash`, appointments, and other data are never removed. Users table never has hard DELETEs.
- Deletion requires a confirmation modal before the API call is made.
- Soft-deleted user login returns the same generic "Invalid credentials" error (already implemented in Story 1.3).
- Admin can set any user's role (PATIENT, DOCTOR, ADMIN) when adding or editing.
- Admin can set a temporary password (`must_change_password = true`) for any user — endpoint already exists from Story 1.6 (`POST /api/v1/admin/users/{id}/password`).
- Admin appointment view: list of all appointments across all users; admin can delete any appointment (confirmation required); patients/doctors cannot delete.

## Technical Decisions

**Endpoints (all under `/api/v1/admin/**`, auto-restricted to ADMIN by SecurityConfig):**
- `GET /api/v1/admin/users` — all users (no filter for deleted_at)
- `POST /api/v1/admin/users` — create a new user with any role
- `PUT /api/v1/admin/users/{id}` — update a user's profile + role
- `DELETE /api/v1/admin/users/{id}` — soft-delete (sets deleted_at)
- `GET /api/v1/admin/appointments` — all appointments
- `DELETE /api/v1/admin/appointments/{id}` — hard-delete an appointment (admin-only)

**New DTOs:** `AdminUserDto` (full user profile + isDeleted flag), `AdminUserCreateRequest`, `AdminUserUpdateRequest`, `AdminAppointmentDto`.

**Client stores:** `userStore` holds `adminUsers` list and CRUD actions; `appointmentStore` may gain an `adminAppointments` list, or a new `adminStore` manages appointment admin.

**Router:** `/admin/users` already exists (stub from Story 1.4). Stories 4.1–4.4 populate it progressively.
