---
spec: medour
version: 1.0.0
date: 2026-08-12
slug: medour
sources:
  - ../planning-artifacts/prds/prd-bmad-med-2026-08-11/prd.md
companions: []
assumptions:
  - Whereby API supports programmatic per-appointment room creation
  - Auto-cancel background job uses Spring @Scheduled; single-instance deployment assumed for v1
open_questions: []
---

# SPEC — medour

## Why

Patients wait on hold to book medical appointments while clinic staff are consumed by inbound calls. medour replaces phone booking with self-serve online scheduling: patients book independently and concurrently, doctors manage their own schedule, and the clinic eliminates the call-volume bottleneck.

---

## Capabilities

### CAP-1 — User Registration & Authentication

**Intent:** Users register via one of two role-specific forms (Patient, Doctor), are issued a JWT on login, and every route is protected by server-side role guards.

**Success:** A new user can register, log in, and access only the routes their role permits. Attempting an admin-only route as a non-admin returns 403 or redirects to home. Tokens expire after 1 hour and the client auto-logs out.

---

### CAP-2 — Appointment Booking with Real-Time Slot Management

**Intent:** Authenticated patients can search available slots by date range, speciality, county/city, and doctor, then book a 30-minute slot through a multi-step flow. Slot state — Available, Locked (booking in progress, DB-persisted), Unavailable (confirmed) — updates in real time across all connected clients.

**Success:** Two patients searching the same slot simultaneously see it transition to Locked when either begins booking. Completing the booking makes it Unavailable. Abandoning the flow releases the lock. No double-booking is possible.

---

### CAP-3 — Video Consultation via Whereby

**Intent:** Patients and doctors can join a video+audio call for an Open appointment using an embedded Whereby room created at booking time. The Join button is only active in the window from 10 minutes before the scheduled start until the scheduled start time.

**Success:** Both parties can enter the Whereby room via the in-app Join button within the allowed window. The button is absent or disabled outside that window and for any non-Open appointment.

---

### CAP-4 — Appointment Lifecycle & Auto-Cancellation

**Intent:** Appointments carry one of four statuses — Open, Completed, Canceled, Auto-Canceled. A scheduled job sets any Open appointment to Auto-Canceled when 10 minutes have passed since its scheduled start without the doctor manually changing status.

**Success:** Manually set: doctor marks Completed or Canceled and the status persists immediately. Automatically set: an appointment that starts and is not actioned transitions to Auto-Canceled within the next job cycle. All terminal statuses are displayed with visually distinct labels and block the Join action.

---

### CAP-5 — Doctor Appointment Management

**Intent:** Doctors see their own appointments in a tabbed view (Upcoming / Past) and can Join, Cancel, or Complete Open future appointments.

**Success:** A doctor can see all their appointments sorted by tab. For any Open future appointment, Join/Cancel/Complete actions are available. Past appointments and terminal-status appointments are read-only.

---

### CAP-6 — Patient Appointment History & Doctor Rating

**Intent:** Patients see a full history of their appointments with status labels. After a Completed appointment, the patient can submit or edit a 1–10 rating for the doctor. The doctor's average rating is recomputed on each submission and displayed with a colour-coded badge (1–5.00 orange, 5.01–8.00 light blue, 8.01–10 light green).

**Success:** Patient history shows all appointments with createdAt timestamps and status badges. A rating input appears on Completed appointment cards. Editing a rating immediately updates the doctor's displayed average. Only the submitting patient can edit their own rating.

---

### CAP-7 — Admin Panel

**Intent:** Admin can create, view, edit, and delete any user (patient, doctor, or admin), including role assignment and temporary password reset. Admin can delete any appointment. All destructive actions require a confirmation modal. Deleted users' appointment records are retained.

**Success:** Admin can perform full user lifecycle operations and appointment deletion from the Users section (admin-only burger menu item, server-side guarded). No appointment record is lost when its patient or doctor is deleted.

---

### CAP-8 — User Profile Management

**Intent:** Every authenticated user can view and edit their own profile fields and change their own password from the burger menu. The admin can edit any user's profile and set a temporary password that forces rotation on next login.

**Success:** Profile changes persist immediately. Password self-change requires the current password. Admin-set temporary passwords enforce a must-change prompt on the user's next login before any other action is permitted.

---

## Constraints

- **Backend:** Spring Boot (Java)
- **Database:** PostgreSQL; appointment records survive user deletion via nullable foreign key or snapshot pattern — no cascade delete
- **Authentication:** JWT stateless, 1-hour expiry, bcrypt password hashing; Spring Security enforces role guards server-side on all protected routes
- **Video:** Whereby third-party only; one room created per appointment at booking time; no custom WebRTC
- **Slots:** Fixed 30-minute intervals 08:00–20:00; slot lock/unlock operations must be atomic at the database level to prevent concurrent double-booking
- **Notifications:** In-app only (v1); no email sent; email field captured and stored for future use
- **Deletions:** Hard delete of appointments and users by admin only; patients and doctors may only change appointment status
- **Admin seed:** Initial admin credentials stored as plain text in `application.properties` / `application.yml`; must be rotated post-setup
- **Real-time:** Slot state changes and appointment status updates delivered to connected clients within 1–2 seconds via WebSocket or SSE

---

## Non-Goals (v1)

- Email or push notifications
- Payment, billing, or prescriptions
- Medical records beyond appointment history
- Multi-clinic / multi-tenant support
- Mobile native application
- Doctor-managed availability calendar (manual slot blocking)
- Appointment rescheduling (cancel and rebook is the workaround)

---

## Success Signal

- A patient completes a booking from login to confirmed appointment in under 3 minutes without a phone call
- Zero double-bookings occur under concurrent load due to atomic slot locking
- A doctor manages their full appointment schedule — viewing, joining, and closing — entirely within the app
- Admin can create, edit, delete any user and any appointment without database access
