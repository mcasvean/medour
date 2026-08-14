# Epic 7 Context: Profile & Appointment Enhancements

<!-- Compiled from planning artifacts. Edit freely. Regenerate with compile-epic-context if planning docs change. -->

## Goal

Users gain visual identity through profile pictures that appear as circular avatars in the application header, and patients regain control of their schedules by rescheduling Open appointments to different dates and times without cancelling and re-booking. When this epic is done, every user can upload a personal picture (or leave it blank), and every patient can flexibly adjust their appointment times within the same doctor's availability.

## Stories

- Story 7.1: User Profile Picture
- Story 7.2: Patient Appointment Reschedule

## Requirements & Constraints

**Profile Picture (7.1):**

- File size limit: 512 KB maximum; requests exceeding this return 400 with "File too large. Maximum size is 512 KB."
- MIME types: JPEG and PNG only; other formats return 400 with "Only JPEG and PNG images are accepted."
- Stored as Base64 data-URI in the `profile_picture TEXT NULL` column on users table (e.g. `data:image/jpeg;base64,...`).
- Endpoint is `/api/v1/users/me/profile-picture` (PATCH to upload, DELETE to remove); always applies to the authenticated user only.
- `profilePicture` is included in `UserProfileResponse` DTO and `AuthResponse` DTO (populated after login and after every update).
- On successful upload/removal, `authStore.user.profilePicture` must be updated in place so the header avatar refreshes without requiring re-login.
- Admin cannot change another user's profile picture via this endpoint — only self-management is allowed.

**Appointment Reschedule (7.2):**

- Only OPEN appointments can be rescheduled; any other status returns 409 with "Only OPEN appointments can be rescheduled."
- Only the owning patient can reschedule; wrong patient returns 403.
- New slot must be available: if a `slot_reservations` row already exists for `(doctor_id, new_date, new_start_time)` OR an OPEN appointment already exists for that combination, return 409 with "Selected slot is not available."
- New `scheduledDate` must be today or in the future; past dates return 400 with "Cannot reschedule to a past date."
- Same doctor is kept; patients cannot reschedule to a different doctor.
- Whereby room URL is unchanged after reschedule (same room for new time).

## Technical Decisions

**Profile Picture Storage:** Base64 data-URI stored directly in `users` table (self-contained). Acceptable given the 512 KB cap.

**Reschedule Transaction Model:** Old `slot_reservations` row deleted and new row inserted atomically within a single `@Transactional` method. Appointment `scheduledDate` and `startTime` updated in same transaction. Failure at any step rolls back entirely.

**SSE Event Sequence on Reschedule:** `broadcastSlotChange` for old slot → AVAILABLE; `broadcastSlotChange` for new slot → LOCKED; `broadcastAppointmentStatus` with updated appointment data so the doctor's view updates in real time.

**Slot Locking:** Relies on `slot_reservations` unique constraint on `(doctor_id, date, start_time)`. DB constraint is source of truth — no application-level check alone.

**Auth:** Both stories use JWT-derived authenticated user context. Ownership validated server-side on every mutation.

**Pinia Store Updates:**

- `authStore.user.profilePicture` updated in-place after upload/remove so header avatar refreshes immediately.
- `appointmentStore.patientAppointments` entry patched in-place with new date/time after reschedule.

## UX & Interaction Patterns

**Profile Picture (7.1):** AccountView circular avatar (96px) with picture or icon placeholder. "Upload photo" opens hidden file input; "Remove photo" shown only when picture is set. Both actions show success/error toasts. Header avatar (32px, circular) positioned between role chip and username — `v-if="authStore.user?.profilePicture"` (no placeholder when unset).

**Reschedule (7.2):** OPEN appointment cards in PatientAppointmentsView get a "Reschedule" button. Opens a `VDialog` with `SlotGrid` (reused) filtered to the existing doctor. Confirming a new slot sends the PATCH request; success shows toast and closes dialog; error shows error toast. Appointment card updates in-place via SSE + store patch.

## Cross-Story Dependencies

- 7.1 and 7.2 are independent of each other; can be implemented in parallel.
- Both depend on Story 6.1 (toastStore) for success/error feedback.
- Story 7.2 depends on the `slot_reservations` table, SSE infrastructure, and `SlotGrid` component from Epics 2–3.
- `authStore.updateUser()` must accept `profilePicture` partial update (Story 7.1 extends it).
