---
stepsCompleted: ["step-01", "step-02", "step-03", "step-04"]
inputDocuments:
  - "../prds/prd-bmad-med-2026-08-11/prd.md"
  - "../architecture/architecture-medour-2026-08-12/ARCHITECTURE-SPINE.md"
---

# medour - Epic Breakdown

## Overview

This document provides the complete epic and story breakdown for medour, decomposing the requirements from the PRD and Architecture into implementable stories.

## Requirements Inventory

### Functional Requirements

FR1: Login page is the first and only unauthenticated screen; all other routes require a valid session
FR2: Registration page offers two buttons — Register as Patient and Register as Doctor — each opening a role-specific form
FR3: Patient registration form captures: email, password, first name, surname, age, gender, city, address
FR4: Doctor registration form captures: email, password, first name, surname, age, gender, city, address, county, speciality
FR5: JWT issued on login with 1-hour expiry; client auto-logs out and redirects to /login on expiry
FR6: User role displayed visibly in the application header
FR7: Burger menu renders: Account Info and Change Password for all roles; Users panel for admin only (not rendered for patient/doctor)
FR8: Admin-only routes are server-side guarded by Spring Security; non-admin access returns 403/redirect
FR9: User profile is editable via Account Info; field set varies by role (see PRD §3.2 table)
FR10: Role is display-only for patients and doctors; only admin can change a user's role
FR11: Password self-change requires the current password as verification
FR12: Admin can set a temporary password for any user; user must change it on next login before any other action
FR13: Admin cannot view any user's actual password
FR14: A default admin account is seeded at application startup from application config credentials
FR15: Patient can search available slots filtered by date range, speciality, doctor, county, and city
FR16: All doctors are included in search results by default when no specific doctor is selected
FR17: Appointment creation is a multi-step flow: select date/time interval → speciality → county → city → confirm doctor → save
FR18: Time slots are 30-minute intervals from 08:00 to 20:00 inclusive, rendered with labels (e.g. "08:00 – 08:30")
FR19: Each slot displays one of three states with distinct visual styling: Available (selectable), Locked (disabled — in-progress booking), Unavailable (disabled — confirmed appointment)
FR20: Selecting a slot immediately transitions it to Locked and persists the lock to the database
FR21: Slot lock is released when the patient cancels the booking flow or navigates away without saving
FR22: Slot state changes (Available/Locked/Unavailable) are pushed to all connected clients in real time via SSE
FR23: Patient cannot initiate booking on a Locked or Unavailable slot
FR24: Confirmed appointment is visible in patient history with status Open immediately after save
FR25: Every appointment stores a createdAt timestamp displayed small in a card corner on all appointment views
FR26: Patient has a dedicated appointment history view listing all past and future appointments
FR27: Appointment cards show: date, time, doctor name, speciality, status badge (styled), createdAt
FR28: Status badges: Open (blue), Completed (green), Canceled (orange), Auto-Canceled (grey)
FR29: Appointment status changes by a doctor are reflected in real time in the patient's view
FR30: Join button becomes active exactly 10 minutes before the scheduled start time
FR31: Join button is disabled and non-clickable outside the 10-minute window
FR32: Joining opens the appointment's unique Whereby video+audio room embedded/redirected in-app
FR33: Both patient and the assigned doctor can join the same Whereby room via their respective Join buttons
FR34: Appointments with status Canceled, Completed, or Auto-Canceled never show an active Join button
FR35: A scheduled background job sets any Open appointment to Auto-Canceled when 10+ minutes have passed since its scheduled start without a status change
FR36: Auto-Canceled status is functionally identical to Canceled for display, joining, and filtering purposes
FR37: Doctor has a My Appointments section with two tabs: Upcoming (Open future) and Past (all terminal-status)
FR38: Doctor appointment card shows: patient name, date, time, speciality, status badge, createdAt
FR39: Doctor can Join, Cancel, or Complete any Open future appointment from their view
FR40: Status changes made by doctor persist immediately and update the patient's view in real time
FR41: Past appointments and terminal-status appointments in doctor view are read-only with status badges
FR42: Admin sees a user management panel listing all patients and doctors; each row is expandable for full details
FR43: Admin can view, edit, add, and delete any user, including role assignment and temporary password reset
FR44: Admin add-user form is the same as the edit form with empty fields; role is selectable (patient/doctor/admin)
FR45: User deletion requires a confirmation modal; deleted user's appointments are retained
FR46: Soft-delete: deleted_at timestamp set on deletion; user data stays in DB and remains readable on appointment cards with a "Removed" badge
FR47: Login attempt with soft-deleted account credentials returns the same generic invalid-credentials error as a wrong password
FR48: Admin can view and delete any appointment (confirmation modal required); patients and doctors cannot delete appointments
FR49: Patient can submit a 1–10 rating for a doctor after their appointment reaches Completed status
FR50: Rating input appears on the completed appointment card in patient history
FR51: Patient can edit their own submitted rating at any time
FR52: Doctor's average_rating (DECIMAL, 1 decimal) is stored on the users table and recalculated on every rating submit or edit
FR53: Average rating displayed on doctor profile and search results with colour-coded badge: 1–5.00 orange, 5.01–8.00 light blue, 8.01–10 light green
FR54: Only the submitting patient can submit or edit a rating; doctors and admins cannot

### NonFunctional Requirements

NFR1: Slot state changes and appointment status changes must be pushed to connected clients within 1–2 seconds via SSE
NFR2: Passwords stored as bcrypt hashes only; JWT tokens expire after 1 hour; no plaintext credential storage (except admin seed in config file)
NFR3: Appointments must persist independently of user accounts; users are soft-deleted, never hard-deleted
NFR4: Slot lock and unlock operations must be atomic at the database level (unique constraint on slot_reservations) to prevent concurrent double-booking
NFR5: Auto-cancel background job must be reliable across server restarts (idempotent re-check on startup)
NFR6: Whereby integration must be encapsulated so the provider can be swapped without restructuring appointment data

### Additional Requirements

- Monorepo structure: client/ (Vue 3 + Vite + TypeScript) and server/ (Spring Boot Java) as independent build units
- BE packages: controller/, service/, repository/, model/, config/, security/ — controllers call services only, services call repositories
- SSE via Spring SseEmitter (BE) and EventSource API (FE); server-initiated only
- JWT stored in localStorage; Axios request interceptor injects Authorization: Bearer; response interceptor catches 401 and triggers logout
- Pinia stores by domain: authStore, appointmentStore, userStore, doctorStore
- slot_reservations table (doctor_id, date, start_time, reserved_by_patient_id, reserved_at, expires_at) with unique constraint on (doctor_id, date, start_time); SSE fires on every INSERT and DELETE
- Soft-delete via users.deleted_at TIMESTAMP NULL; all user queries filter WHERE deleted_at IS NULL except appointment history display
- Whereby room created via API at booking save time; URL stored in appointments.whereby_room_url
- users.average_rating DECIMAL(3,1); recalculated via ROUND(AVG(value), 1) on every rating change
- @Scheduled fixed-rate every 60 seconds for auto-cancel job
- Vue Router 4 with role-based navigation guards; admin routes require ROLE_ADMIN
- All endpoints under /api/v1/ REST; Spring Security validates role from JWT on every request
- All DB access through JPA repositories extending JpaRepository
- Initial admin credentials in application.properties / application.yml plain text (must be changed post-setup)
- ratings table: id, appointment_id, patient_id, doctor_id, value INTEGER 1–10

### UX Design Requirements

N/A — no UX design contract was produced for this project.

### FR Coverage Map

FR1: Epic 1 — Login page is only unauthenticated screen
FR2: Epic 1 — Dual registration buttons (Patient / Doctor)
FR3: Epic 1 — Patient registration form fields
FR4: Epic 1 — Doctor registration form fields
FR5: Epic 1 — JWT 1-hour expiry + auto-logout
FR6: Epic 1 — Role displayed in header
FR7: Epic 1 — Burger menu with conditional items
FR8: Epic 1 — Admin-only routes guarded by Spring Security
FR9: Epic 1 — Profile fields editable via Account Info
FR10: Epic 1 — Role display-only for patient/doctor
FR11: Epic 1 — Password self-change requires current password
FR12: Epic 1 — Admin sets temporary password; forces rotation on next login
FR13: Epic 1 — Admin cannot view passwords
FR14: Epic 1 — Admin seed account from config
FR15: Epic 2 — Patient searches slots by filters
FR16: Epic 2 — All doctors included in search by default
FR17: Epic 2 — Multi-step booking flow
FR18: Epic 2 — 30-min slots 08:00–20:00 with labels
FR19: Epic 2 — Three slot states with distinct styling
FR20: Epic 2 — Selecting a slot locks it in DB immediately
FR21: Epic 2 — Lock released on cancel or navigation away
FR22: Epic 2 — Slot state changes pushed via SSE in real time
FR23: Epic 2 — Cannot book Locked or Unavailable slot
FR24: Epic 2 — Saved appointment shows as Open in patient history
FR25: Epic 2 — createdAt timestamp on every appointment card
FR26: Epic 3 — Patient appointment history view
FR27: Epic 3 — Appointment card fields for patient view
FR28: Epic 3 — Status badges with colours
FR29: Epic 3 — Status changes reflected in real time
FR30: Epic 3 — Join button active 10 min before start
FR31: Epic 3 — Join button disabled outside window
FR32: Epic 3 — Whereby room opens on Join
FR33: Epic 3 — Both patient and doctor can join same room
FR34: Epic 3 — Non-Open appointments never show active Join
FR35: Epic 3 — Auto-cancel job sets Open → Auto-Canceled after 10 min
FR36: Epic 3 — Auto-Canceled identical to Canceled for display/join/filter
FR37: Epic 3 — Doctor My Appointments with Upcoming/Past tabs
FR38: Epic 3 — Doctor appointment card fields
FR39: Epic 3 — Doctor can Join/Cancel/Complete Open future appointments
FR40: Epic 3 — Doctor status changes update patient view in real time
FR41: Epic 3 — Past appointments are read-only
FR42: Epic 4 — Admin user management panel (list + expand)
FR43: Epic 4 — Admin view/edit/add/delete users + role + temp password
FR44: Epic 4 — Admin add-user form
FR45: Epic 4 — User deletion confirmation modal; appointments retained
FR46: Epic 4 — Soft-delete + "Removed" badge on appointment cards
FR47: Epic 4 — Soft-deleted user login returns generic error
FR48: Epic 4 — Admin appointment delete with confirmation; patients/doctors cannot delete
FR49: Epic 5 — Patient submits 1–10 rating after Completed appointment
FR50: Epic 5 — Rating input on completed appointment card
FR51: Epic 5 — Patient can edit own rating
FR52: Epic 5 — average_rating DECIMAL(3,1) recalculated on every change
FR53: Epic 5 — Colour-coded rating badge on doctor profile and search results
FR54: Epic 5 — Only submitting patient can edit rating

## Epic List

### Epic 1: Foundation & Authentication

Users can register as a patient or doctor, log in, manage their profile, and change their password. All role-based access controls are in place. The admin seed account exists and admin can manage user credentials.
**FRs covered:** FR1, FR2, FR3, FR4, FR5, FR6, FR7, FR8, FR9, FR10, FR11, FR12, FR13, FR14
**NFRs:** NFR2

### Epic 2: Appointment Booking & Real-Time Slot Management

Patients can search for doctors by filters, see live slot availability, and complete a multi-step booking. Concurrent patients see slot states update in real time with no double-bookings possible.
**FRs covered:** FR15, FR16, FR17, FR18, FR19, FR20, FR21, FR22, FR23, FR24, FR25
**NFRs:** NFR1, NFR4

### Epic 3: Appointment Lifecycle, History & Video Consultation

Patients see their full appointment history with real-time status. Doctors manage appointments (upcoming/past), join calls, and change statuses. The auto-cancel job handles no-shows.
**FRs covered:** FR26, FR27, FR28, FR29, FR30, FR31, FR32, FR33, FR34, FR35, FR36, FR37, FR38, FR39, FR40, FR41
**NFRs:** NFR5, NFR6

### Epic 4: Admin Panel & User Management

Admin can create, view, edit, and delete any user or appointment. Soft-delete keeps data intact. Deleted users show "Removed" on appointment cards. All destructive actions require confirmation.
**FRs covered:** FR42, FR43, FR44, FR45, FR46, FR47, FR48
**NFRs:** NFR3

### Epic 5: Doctor Rating System

Patients rate doctors 1–10 after completed appointments. Ratings are editable. Each doctor's average updates instantly with 1 decimal and renders with a colour-coded badge.
**FRs covered:** FR49, FR50, FR51, FR52, FR53, FR54

---

## Epic 1: Foundation & Authentication

Users can register as a patient or doctor, log in, manage their profile, and change their password. All role-based access controls are in place. The admin seed account exists and admin can manage user credentials.

### Story 1.1: Project Scaffold & Development Environment

As a developer,
I want the monorepo scaffolded with a running Vue+Vite+TS client and Spring Boot server connected to PostgreSQL,
So that all team members can run the application locally and start building features.

**Acceptance Criteria:**

**Given** the repo is cloned,
**When** `cd client && npm run dev` is run,
**Then** the Vue app starts on localhost without errors.

**Given** the repo is cloned,
**When** `cd server && ./mvnw spring-boot:run` is run,
**Then** the Spring Boot server starts and connects to PostgreSQL without errors.

**Given** the server is running,
**When** GET /api/v1/health is called,
**Then** 200 OK is returned.

**And** the Axios instance with auth interceptors, Pinia stores skeleton (authStore, appointmentStore, userStore, doctorStore), Vue Router base config, and Spring Security base config are in place with no compilation errors.

---

### Story 1.2: User Registration (Patient & Doctor)

As a new user,
I want to register as a patient or doctor using a role-specific form,
So that I have an account with the appropriate role and profile data.

**Acceptance Criteria:**

**Given** the registration page,
**When** I click "Register as Patient",
**Then** the patient form is shown with fields: email, password, first name, surname, age, gender, city, address.

**Given** the registration page,
**When** I click "Register as Doctor",
**Then** the doctor form is shown with the same fields plus county and speciality.

**Given** a completed patient form with valid data,
**When** I submit,
**Then** an account is created with role PATIENT, a JWT is issued, and I am redirected to the app as a logged-in user.

**Given** a completed doctor form with valid data,
**When** I submit,
**Then** an account is created with role DOCTOR.

**Given** a duplicate email,
**When** I submit either form,
**Then** an error is shown and no account is created.

**And** the admin seed account from application config credentials exists on application startup.

---

### Story 1.3: User Login & JWT Session

As a registered user,
I want to log in with my email and password and stay logged in for one hour,
So that I can access the application securely without re-entering credentials constantly.

**Acceptance Criteria:**

**Given** an unauthenticated user,
**When** they visit any route other than /login or /register,
**Then** they are redirected to /login.

**Given** valid credentials,
**When** I submit the login form,
**Then** a JWT is stored in localStorage and I am redirected to the home page.

**Given** invalid credentials or credentials for a soft-deleted account,
**When** I submit the login form,
**Then** the same generic "Invalid credentials" error is shown with no distinction between cases.

**Given** a JWT that has been in localStorage for 1 hour,
**When** any API call is made,
**Then** the server returns 401, the Axios response interceptor clears localStorage, and the user is redirected to /login.

---

### Story 1.4: Role Display & Route Guards

As an authenticated user,
I want my role displayed in the header and appropriate menu items rendered for my role,
So that I always know my access level and cannot access routes outside my permissions.

**Acceptance Criteria:**

**Given** a logged-in user,
**Then** their role is visibly displayed in the application header.

**Given** a logged-in patient or doctor,
**When** the burger menu is opened,
**Then** Account Info and Change Password are shown and the Users item is NOT rendered.

**Given** a logged-in admin,
**When** the burger menu is opened,
**Then** Account Info, Change Password, and Users are all shown.

**Given** a non-admin authenticated user,
**When** they manually navigate to /admin/users,
**Then** they are redirected to home.

**Given** a non-admin JWT,
**When** any admin-only API endpoint is called,
**Then** Spring Security returns 403.

---

### Story 1.5: My Preferences — Account Info

As a logged-in user,
I want to view and edit my profile fields from the burger menu,
So that my account information stays accurate.

**Acceptance Criteria:**

**Given** a logged-in patient,
**When** I open Account Info,
**Then** I see editable fields: first name, surname, age, gender, city, address; and display-only fields: email, role.

**Given** a logged-in doctor,
**When** I open Account Info,
**Then** I also see editable county and speciality fields.

**Given** I edit one or more editable fields and click save,
**When** the request is sent,
**Then** the changes are persisted to the database and the updated values are shown immediately.

**Given** any role,
**When** a patient or doctor submits a request to change their own role,
**Then** the server rejects it with 403.

---

### Story 1.6: Password Management

As a user,
I want to change my own password securely, and as an admin I want to reset any user's password,
So that accounts remain protected and users can recover access.

**Acceptance Criteria:**

**Given** any logged-in user on the Change Password screen,
**When** I submit with the correct current password and a new password,
**Then** the password is updated and I remain logged in.

**Given** an incorrect current password,
**When** I submit the change password form,
**Then** an error is shown and the password is not changed.

**Given** a logged-in admin,
**When** I set a temporary password for any user,
**Then** that user's must_change_password flag is set to true in the database.

**Given** a user with must_change_password = true,
**When** they log in successfully,
**Then** they are redirected to a forced password change screen and cannot access any other page until they set a new password.

**And** at no point is any user's password visible in the admin panel or returned by any API.

---

## Epic 2: Appointment Booking & Real-Time Slot Management

Patients can search for doctors by filters, see live slot availability, and complete a multi-step booking. Concurrent patients see slot states update in real time with no double-bookings possible.

### Story 2.1: Doctor Search & Filter

As a patient,
I want to search available doctors by date range, speciality, county, and city,
So that I can find the right appointment slot for my needs.

**Acceptance Criteria:**

**Given** a logged-in patient on the booking page,
**When** I apply filters (date range, speciality, county, city),
**Then** only matching doctors with available slots for that criteria are shown.

**Given** no filters are applied,
**When** the booking page loads,
**Then** all doctors are shown by default.

**Given** filters that match no doctors,
**When** the search runs,
**Then** an appropriate empty-state message is displayed.

---

### Story 2.2: Slot Availability Display

As a patient,
I want to see time slots for a selected doctor displayed with clear Available, Locked, and Unavailable visual states,
So that I immediately know which slots I can book.

**Acceptance Criteria:**

**Given** a doctor is selected and a date chosen,
**When** the slot grid renders,
**Then** 30-minute slots from 08:00 to 20:00 are shown with time labels (e.g. "08:00 – 08:30").

**Given** a slot has no reservation and no confirmed appointment,
**Then** it renders in Available style and is selectable.

**Given** a slot has an active slot_reservations row,
**Then** it renders in Locked style (disabled, visually distinct from Available).

**Given** a slot has a confirmed appointment,
**Then** it renders in Unavailable style (disabled, visually distinct from both others).

**Given** another patient locks or releases a slot while I am viewing the page,
**When** the SSE event arrives,
**Then** the slot's visual state updates in real time without a page refresh.

---

### Story 2.3: Multi-Step Appointment Booking Flow

As a patient,
I want to complete a multi-step booking flow that locks my chosen slot while I fill in details,
So that I secure the slot before someone else takes it.

**Acceptance Criteria:**

**Given** I click an Available slot,
**When** the selection is processed,
**Then** a slot_reservations row is inserted atomically, an SSE event pushes Locked state to all connected clients, and I advance to the next step.

**Given** two patients click the same Available slot simultaneously,
**When** both inserts are attempted,
**Then** exactly one succeeds (unique constraint enforced) and the other receives a slot-unavailable error.

**Given** I am mid-booking and navigate away or click cancel,
**When** the cancellation is processed,
**Then** the slot_reservations row is deleted, an SSE event releases the slot to Available, and other patients can book it.

**Given** I complete all steps (speciality, county, city, doctor confirmation) and click Save,
**When** the save is processed,
**Then** the slot_reservations row is deleted, a confirmed appointment is created with status Open, and an SSE event pushes Unavailable state to all connected clients.

---

### Story 2.4: Appointment Confirmation & Whereby Room Creation

As a patient,
I want my confirmed appointment saved with a createdAt timestamp and a Whereby video room created at booking time,
So that the consultation room is ready before join time.

**Acceptance Criteria:**

**Given** a completed booking flow on Save,
**When** the appointment is created,
**Then** it is saved with status Open, a createdAt timestamp, and a unique Whereby room URL stored in appointments.whereby_room_url.

**Given** the Whereby API call fails during save,
**When** the error occurs,
**Then** the entire appointment save is rolled back and an error is shown to the patient.

**Given** a newly confirmed appointment,
**When** I navigate to my appointment history,
**Then** the appointment card shows date, time, doctor name, speciality, Open status badge, and createdAt timestamp in a small corner label.

---

## Epic 3: Appointment Lifecycle, History & Video Consultation

Patients see their full appointment history with real-time status updates. Doctors manage appointments (upcoming/past tabs, join/cancel/complete). The Join button activates at the right time. The auto-cancel job closes no-show appointments automatically.

### Story 3.1: Patient Appointment History View

As a patient,
I want to see all my past and future appointments in one view with styled status badges,
So that I have a clear record of my medical schedule.

**Acceptance Criteria:**

**Given** a logged-in patient,
**When** I open appointment history,
**Then** all my appointments (past and future) are listed.

**Given** an appointment card,
**Then** it shows: date, time, doctor name, speciality, styled status badge (Open=blue, Completed=green, Canceled=orange, Auto-Canceled=grey), and createdAt in a small corner label.

**Given** an appointment whose doctor has been soft-deleted,
**Then** the card still shows the doctor's name alongside a "Removed" badge.

---

### Story 3.2: Real-Time Appointment Status Updates

As a patient,
I want appointment status changes made by a doctor to appear immediately in my history view,
So that I always see the current state without refreshing.

**Acceptance Criteria:**

**Given** I am viewing my appointment history and a doctor cancels or completes one of my appointments,
**When** the SSE event arrives,
**Then** the appointment's status badge updates in real time without a page refresh.

**Given** an appointment is set to Auto-Canceled by the background job,
**When** the SSE event fires,
**Then** the grey "Auto-Canceled" badge appears on my card in real time.

---

### Story 3.3: Video Consultation Join Button

As a patient or doctor,
I want a Join button that becomes active only in the 10-minute window before my appointment,
So that I can start the video consultation at the right time and not before.

**Acceptance Criteria:**

**Given** an Open appointment and the current time is between appointment_start - 10 minutes and appointment_start,
**Then** the Join button is active and clickable.

**Given** the current time is outside that window,
**Then** the Join button is disabled or absent.

**Given** I click an active Join button,
**Then** the Whereby room URL opens (embedded or redirect) with camera and microphone access prompted.

**Given** both patient and doctor click Join for the same appointment,
**Then** both enter the same Whereby room.

**Given** an appointment with status Canceled, Completed, or Auto-Canceled,
**Then** the Join button is never active regardless of time.

---

### Story 3.4: Auto-Cancellation Background Job

As the system,
I want Open appointments that are 10+ minutes past their scheduled start to be automatically set to Auto-Canceled,
So that no-shows are handled without manual intervention.

**Acceptance Criteria:**

**Given** the @Scheduled job runs every 60 seconds,
**When** any Open appointment's scheduled_start + 10 minutes < NOW(),
**Then** its status is set to Auto-Canceled and an SSE event is fired to all connected clients.

**Given** the server restarts,
**When** it comes back up,
**Then** the job runs immediately and catches any auto-cancellations missed during downtime.

**Given** an appointment already in a terminal status (Completed, Canceled, Auto-Canceled),
**When** the job runs,
**Then** it is not modified.

---

### Story 3.5: Doctor My Appointments View

As a doctor,
I want to see my appointments in Upcoming and Past tabs,
So that I can manage my schedule clearly.

**Acceptance Criteria:**

**Given** a logged-in doctor,
**When** I navigate to My Appointments,
**Then** I see two tabs: Upcoming (Open future appointments) and Past (all terminal-status past appointments).

**Given** an appointment in the Upcoming tab,
**Then** the card shows: patient name, date, time, speciality, Open status badge, createdAt, and action buttons (Join, Cancel, Complete).

**Given** an appointment in the Past tab,
**Then** the card is read-only with the appropriate styled status badge.

**Given** an appointment whose patient has been soft-deleted,
**Then** the card still shows the patient's name alongside a "Removed" badge.

---

### Story 3.6: Doctor Appointment Actions

As a doctor,
I want to join, cancel, or complete my Open future appointments,
So that I control the state of my schedule.

**Acceptance Criteria:**

**Given** an Open future appointment,
**When** I click Cancel,
**Then** the status is set to Canceled, persisted to the database, and the patient's view updates in real time via SSE.

**Given** an Open future appointment,
**When** I click Complete,
**Then** the status is set to Completed, persisted, and the patient's view updates in real time via SSE.

**Given** an Open appointment within the 10-minute Join window,
**When** I click Join,
**Then** the Whereby room opens (same behaviour as patient Join).

**Given** an appointment with any terminal status (Canceled, Completed, Auto-Canceled),
**Then** the Cancel, Complete, and Join buttons are absent.

---

## Epic 4: Admin Panel & User Management

Admin can create, view, edit, and delete any user or appointment. Soft-delete keeps data intact. Deleted users show "Removed" on appointment cards. All destructive actions require confirmation.

### Story 4.1: Admin User List & Detail View

As an admin,
I want to see all patients and doctors in a list with expandable details,
So that I have a complete view of every user in the system.

**Acceptance Criteria:**

**Given** a logged-in admin on the Users panel,
**When** the page loads,
**Then** a list of all users (patients and doctors, including soft-deleted) is shown.

**Given** I click a user row,
**Then** the full user profile expands showing all their fields.

**Given** a soft-deleted user in the list,
**Then** they are visually marked as "Removed".

---

### Story 4.2: Admin Add & Edit User

As an admin,
I want to create new users and edit any existing user's profile including their role,
So that I can manage the user base directly without requiring self-service registration.

**Acceptance Criteria:**

**Given** I click "Add User",
**Then** a form opens with all profile fields empty and a role dropdown (patient/doctor/admin).

**Given** I complete the form with valid data and save,
**Then** the new user is created with the selected role and all fields persisted to the database.

**Given** I open an existing user's detail and edit any field including role,
**When** I save,
**Then** changes are persisted immediately and reflected in the list.

**Given** I submit a form with a duplicate email,
**Then** an error is shown and the save is rejected.

---

### Story 4.3: Admin Delete User (Soft-Delete)

As an admin,
I want to delete a user with a confirmation step,
So that I can remove access without losing appointment history or related data.

**Acceptance Criteria:**

**Given** I click delete on any user,
**Then** a confirmation modal appears naming the user before executing.

**Given** I confirm the deletion,
**Then** the user's deleted_at is set to NOW() and the user row remains in the database (soft-delete; no hard DELETE).

**Given** a soft-deleted user attempts to log in,
**Then** they receive the same generic invalid-credentials error as a wrong password — no distinction.

**Given** any appointment card that references a soft-deleted user (patient or doctor),
**Then** the user's name is shown alongside a "Removed" badge.

---

### Story 4.4: Admin Appointment Management

As an admin,
I want to view all appointments across all users and delete any of them with a confirmation step,
So that I have full oversight and data control.

**Acceptance Criteria:**

**Given** I am on the admin appointments view,
**Then** all appointments system-wide are listed with patient name, doctor name, date, time, status badge, and createdAt.

**Given** I click delete on any appointment,
**Then** a confirmation modal appears naming the appointment before executing.

**Given** I confirm the deletion,
**Then** the appointment is permanently removed from the database.

**Given** a patient or doctor attempts to call the appointment-delete API endpoint,
**Then** Spring Security returns 403.

---

## Epic 5: Doctor Rating System

Patients rate doctors 1–10 after completed appointments. Ratings are editable. Each doctor’s average updates with 1 decimal and renders with a colour-coded badge.

### Story 5.1: Rating Submission on Completed Appointments

As a patient,
I want to submit a 1–10 rating for a doctor after my appointment is completed,
So that I can provide feedback that helps other patients make informed choices.

**Acceptance Criteria:**

**Given** a patient viewing a Completed appointment card in their history,
**Then** a rating input (1–10) is visible on the card.

**Given** an appointment whose status is not Completed,
**Then** no rating input is shown.

**Given** I select a value and submit,
**When** the rating is saved,
**Then** a row is inserted into the ratings table (one per appointment) and the doctor’s average_rating is recalculated as ROUND(AVG(value), 1).

**Given** I have already submitted a rating for this appointment,
**When** I open the card,
**Then** my existing rating value is pre-filled in the input.

---

### Story 5.2: Rating Editing

As a patient,
I want to edit a rating I already submitted,
So that I can correct it if my assessment changes.

**Acceptance Criteria:**

**Given** I have a previously submitted rating on a Completed appointment card,
**When** I change the value and save,
**Then** my ratings row is updated and the doctor’s average_rating is recalculated immediately.

**Given** a doctor or admin attempts to submit or update a rating via the API,
**Then** the server returns 403.

**Given** a patient attempts to update a rating for an appointment that belongs to a different patient,
**Then** the server returns 403.

---

### Story 5.3: Colour-Coded Doctor Rating Display

As any user,
I want to see a doctor’s average rating displayed with a colour-coded badge on their profile and in search results,
So that I can quickly assess their quality at a glance.

**Acceptance Criteria:**

**Given** a doctor with at least one rating,
**Then** their average_rating DECIMAL(3,1) is displayed as a badge on their profile and in appointment search results (e.g. “8.9”).

**Given** average_rating is between 1.00 and 5.00,
**Then** the badge renders in an orange colour scheme.

**Given** average_rating is between 5.01 and 8.00,
**Then** the badge renders in a light blue colour scheme.

**Given** average_rating is between 8.01 and 10.00,
**Then** the badge renders in a light green colour scheme.

**Given** a doctor has no ratings yet,
**Then** no rating badge is shown.
