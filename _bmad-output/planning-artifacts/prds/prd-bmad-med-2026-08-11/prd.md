---
title: medour — Medical Appointment Booking Platform
status: final
created: 2026-08-11
updated: 2026-08-11
---

# medour — Product Requirements Document

## 1. Problem Statement & Vision

Patients booking medical appointments by phone face unpredictable wait times and are bottlenecked by clinic staff availability. Clinics spend significant staff time on inbound calls that block parallel throughput and scale poorly.

**medour** replaces phone-based appointment booking with a self-serve web platform. Patients book independently at any hour; multiple bookings can be created simultaneously. Doctors manage their own schedule without administrative overhead. The clinic reduces call volume and handles no-shows automatically.

---

## 2. Users & Roles

Three roles exist in the system. Role is assigned at account creation and displayed visibly in the application header.

| Role        | Assigned                                  | Default on signup |
| ----------- | ----------------------------------------- | ----------------- |
| **Patient** | Default on self-registration              | ✅                |
| **Doctor**  | Set by admin (upgrade from patient)       | —                 |
| **Admin**   | Set by admin; one seed account at startup | —                 |

A default admin account is seeded at application startup (credentials defined in configuration). All roles are stored in the database and enforced server-side on every request.

---

## 3. Authentication & Account Management

### 3.1 Login & Registration

- The login page is the first and only screen accessible without authentication. All other routes require a valid session.
- The registration page presents two distinct buttons: **Register as Patient** and **Register as Doctor**. Each opens a role-specific form. The role is set automatically by the button chosen — there is no role field on the registration form.
- **Patient registration form:** email, password, first name, surname, age, gender, city, address.
- **Doctor registration form:** email, password, first name, surname, age, gender, city, address, county, speciality.
- Passwords are stored hashed (bcrypt or equivalent). No plaintext passwords are stored or returned via any API.
- Authentication uses a stateless JWT token. Tokens are valid for **1 hour** from issuance. After expiry the session is terminated and the user is automatically logged out and redirected to the login page.

### 3.2 My Preferences (header burger menu)

Every authenticated user has a header burger menu. Menu items are **conditionally rendered** based on the authenticated user's role:

- **Account Info** — view and edit own profile fields _(all roles)_
- **Change Password** — change own password _(all roles)_
- **Users** — access the admin user management panel _(admin only — not rendered for patient or doctor)_

All admin-only routes (e.g. `/admin/users`) are protected by a server-side role guard in addition to the conditional UI rendering. Manually entering an admin route URL as a non-admin user results in a 403 / redirect to home.

#### Profile fields by role

| Field      | Patient           | Doctor            | Admin             |
| ---------- | ----------------- | ----------------- | ----------------- |
| First name | ✅                | ✅                | ✅                |
| Surname    | ✅                | ✅                | ✅                |
| Email      | ✅ (display only) | ✅ (display only) | ✅ (display only) |
| Age        | ✅                | ✅                | ✅                |
| Gender     | ✅                | ✅                | ✅                |
| City       | ✅                | ✅                | ✅                |
| Address    | ✅                | ✅                | —                 |
| County     | —                 | ✅                | —                 |
| Speciality | —                 | ✅                | —                 |
| Rating     | —                 | display only      | —                 |
| Role       | display only      | display only      | display only      |

Patients and doctors cannot modify their own role. Role is set at registration (by button choice) and can only be changed afterwards by an admin.

Each user can edit only their own profile. Admin can edit any user's profile (including role). All changes are persisted to the database immediately on save.

### 3.3 Password Management

- **Self-change:** any user can change their own password from My Preferences. Requires the current password to be entered first.
- **Admin reset:** admin can set a new temporary password for any user (for forgotten password cases). The temporary password is marked as requiring rotation — the user must change it on next login before accessing the app. Admin cannot view any user's actual password at any time.

---

## 4. Feature Groups

### FR-1 — Appointment Booking (Patient)

**FR-1.1** — Patient can browse available appointment slots filtered by:

- Date range (interval)
- Medical speciality
- Specific doctor (default: all doctors)
- County / city

**FR-1.2** — Appointment creation is a multi-step flow:

1. Select date and time interval
2. Select speciality
3. Select county
4. Select city
5. (Doctor list filters accordingly) Select or confirm doctor
6. Confirm and save

**FR-1.3** — Time slots are 30-minute intervals from **08:00** to **20:00** inclusive. Each slot is rendered with a clear visual label (e.g. "08:00 – 08:30").

**FR-1.4** — Each slot displays one of three states with distinct visual styling:

| State           | Meaning                                                                                       | Interaction                 |
| --------------- | --------------------------------------------------------------------------------------------- | --------------------------- |
| **Available**   | No appointment exists for this slot                                                           | Selectable                  |
| **Locked**      | Another patient is currently in the process of booking this slot (selected but not yet saved) | Disabled, visually distinct |
| **Unavailable** | A confirmed appointment already exists for this slot                                          | Disabled, visually distinct |

**FR-1.5** — When a patient selects a slot, its state transitions to **Locked** immediately and is persisted to the database. The lock is released if the patient cancels the booking flow or navigates away without saving. On save, the slot transitions to **Unavailable**.

**FR-1.6** — Slot state changes (Available → Locked → Unavailable and back) are pushed to all connected patients in real time (WebSocket or SSE).

**FR-1.7** — A patient cannot book a slot already in **Locked** or **Unavailable** state.

**FR-1.8** — After a confirmed appointment is saved it is visible in the patient's appointment history with status **Open**.

**FR-1.9** — Every appointment record stores a `createdAt` timestamp (the exact date and time the booking was saved). This timestamp is displayed on every appointment card in a small, unobtrusive position (e.g. bottom corner of the card).

---

### FR-2 — Patient Appointment History

**FR-2.1** — Patients have a dedicated view listing all their appointments (past and future).

**FR-2.2** — Appointment cards display: date, time, doctor name, speciality, status label (styled badge), and `createdAt` timestamp (small, in card corner).

**FR-2.3** — Appointment statuses visible to patient:

| Status        | Label style      |
| ------------- | ---------------- |
| Open          | Neutral / blue   |
| Completed     | Success / green  |
| Canceled      | Warning / orange |
| Auto-Canceled | Muted / grey     |

**FR-2.4** — Status changes made by a doctor are reflected in the patient's view in real time (same WebSocket/SSE channel).

---

### FR-3 — Video Consultation (Join)

**FR-3.1** — Each Open appointment has a **Join** button that becomes active exactly **10 minutes before** the scheduled start time.

**FR-3.2** — The Join button is disabled and not clickable outside this window.

**FR-3.3** — Joining opens a video+audio call session via **Whereby** (embedded in-app iframe or redirect to Whereby room). Each appointment maps to a unique Whereby room URL generated at appointment creation time.

**FR-3.4** — Both the patient and the assigned doctor can join the same room using their respective Join buttons.

**FR-3.5** — Appointments with status **Canceled**, **Completed**, or **Auto-Canceled** never show an active Join button regardless of time.

---

### FR-4 — Auto-Cancellation

**FR-4.1** — A scheduled background job runs continuously (or at regular short intervals). For any appointment whose scheduled start time has passed by more than **10 minutes** and whose status is still **Open**, the status is automatically updated to **Auto-Canceled**.

**FR-4.2** — Auto-Canceled status is treated identically to Canceled for all display, joining, and filtering purposes.

---

### FR-5 — Doctor Appointment Management

**FR-5.1** — Doctors have a dedicated **My Appointments** section with two tabs:

- **Upcoming** — future appointments with status Open
- **Past** — appointments whose scheduled time has passed (all terminal statuses)

**FR-5.2** — Each appointment card in the doctor view shows: patient name, date, time, speciality, current status badge, and `createdAt` timestamp (small, in card corner).

**FR-5.3** — For **Open** future appointments, the doctor can:

- **Join** (same 10-minute window rule as FR-3.1)
- **Cancel** the appointment (sets status to Canceled)
- **Complete** the appointment (sets status to Completed)

**FR-5.4** — Status changes made by the doctor are persisted immediately and reflected in real time in the patient's view.

**FR-5.5** — Past appointments with terminal statuses (Completed, Canceled, Auto-Canceled) are displayed read-only with styled status badges. No actions are available on them.

---

### FR-6 — Admin Panel

**FR-6.1 — User Management**

Admin has a dedicated users list showing all patients and doctors. Each row is expandable/clickable to reveal full user details.

Admin can:

- **View** any user's full profile
- **Edit** any user's profile fields (including role change)
- **Add** a new user (patient or doctor) — same form as edit with empty fields; role is selectable by admin
- **Delete** a user — requires a confirmation modal before executing
- **Set a temporary password** for any user (forgotten password flow)

**FR-6.2 — User deletion rules**

When a user is deleted:

- The user account is removed from the users table
- All appointments linked to that user are **retained** in the appointments table
- Appointments store a snapshot of patient name, doctor name, and relevant details at booking time in a separate relational structure, so appointment records remain readable after user deletion
- No cascade delete on appointments

**FR-6.3 — Appointment Management**

Admin can view all appointments across all users and **delete** any appointment. Deletion requires a confirmation modal.

Patients and doctors cannot delete appointments — they can only change status within their allowed actions.

**FR-6.4 — Confirmation modals**

All destructive actions (user delete, appointment delete) require a confirmation modal before execution. The modal clearly states what will be deleted.

---

### FR-7 — User Profile Fields & Forms

#### Registration (self-service)

Fields: email, password, first name, surname.

#### My Preferences — Account Info (self-edit)

Fields as per role table in §3.2. Role is always display-only for self-edit.

#### Admin create/edit user form

All profile fields visible and editable. Role field is a dropdown (patient / doctor / admin). Email field is included on all forms but is display-only after account creation (cannot be changed post-registration).

---

### FR-8 — Doctor Rating

**FR-8.1** — After an appointment reaches **Completed** status, the patient who booked it can submit a rating for the doctor. The rating field appears on the completed appointment card in the patient's history view.

**FR-8.2** — Rating values are integers from **1 to 10**. The patient can edit their submitted rating at any time after submission.

**FR-8.3** — Each doctor's **average rating** is calculated from all ratings received on their completed appointments. The average is recomputed on every new or edited rating submission.

**FR-8.4** — The doctor's average rating is displayed on their profile and on any doctor listing/search results card. Rating display uses a colour-coded style based on value:

| Range        | Colour theme |
| ------------ | ------------ |
| 1.00 – 5.00  | Orangeish    |
| 5.01 – 8.00  | Light blue   |
| 8.01 – 10.00 | Light green  |

**FR-8.5** — Only the patient who booked a specific completed appointment can rate that appointment. One rating per appointment. The patient can edit their own submitted rating at any time. Doctors and admins cannot submit or edit ratings.

**FR-8.6** — The rating input on the appointment card uses a visually distinct style consistent with the colour theme defined in FR-8.4 (e.g. colour-coded number input or star-equivalent scale).

---

## 5. Non-Functional Requirements

**NFR-1 — Real-time updates:** Slot state changes and appointment status changes must be pushed to connected clients within 1–2 seconds using WebSocket or Server-Sent Events.

**NFR-2 — Authentication security:** Passwords stored as hashed values only. JWT tokens expire after **1 hour**; expiry triggers automatic logout on the client. No plaintext credential storage anywhere (except the initial admin seed credentials in the application config file, which must be changed post-setup).

**NFR-3 — Data integrity:** Appointments must persist independently of user accounts. Foreign key references to users must allow nullable/soft-delete pattern so appointment records survive user deletion.

**NFR-4 — Concurrent slot locking:** The locking mechanism for slots must be atomic (database-level) to prevent two patients from locking the same slot simultaneously.

**NFR-5 — Auto-cancel reliability:** The background job for auto-cancellation must be reliable across server restarts (e.g. persisted job state or idempotent re-check on startup).

**NFR-6 — Video provider:** Whereby is the primary video provider. The integration must be encapsulated so the provider can be swapped without restructuring appointment data.

---

## 6. Constraints

- **Backend:** Java — Spring Boot
- **Database:** PostgreSQL
- **Authentication:** JWT-based stateless auth, 1-hour token expiry
- **Video:** Whereby (third-party embedded); one unique Whereby room created per appointment at booking time; no custom WebRTC implementation
- **Admin seed credentials:** stored in plain text in the application config file (`application.properties` or `application.yml`) for v1; must be changed post-setup
- **Notifications:** In-app only for v1. No email sending. Email field captured and stored for future use.
- **Appointments:** Status-only changes for patients and doctors; hard delete only by admin
- **Slot intervals:** Fixed 30-minute slots, 08:00–20:00, no configurable schedule in v1
- **Route access control:** Admin-only routes enforced server-side (Spring Security role-based guards) in addition to UI-level conditional rendering

---

## 7. Non-Goals (v1)

- Email notifications of any kind
- SMS / push notifications
- Payment or billing
- Prescription management
- Medical records or patient history beyond appointment history
- Multi-clinic / multi-tenant support
- Mobile native app (web only)
- Doctor availability calendar management (doctors cannot block slots manually in v1)
- Appointment rescheduling (cancel + rebook is the workaround)

---

## 8. Success Signal

- A patient can book an appointment from login to confirmed booking in under 3 minutes without calling anyone
- A doctor can see, join, and close their appointments entirely from within the app
- Zero double-bookings occur due to the real-time slot locking mechanism
- Admin can manage all users and appointments without developer access to the database

---

## 9. Open Questions

| #    | Question                             | Owner                  | Revisit condition                                                        |
| ---- | ------------------------------------ | ---------------------- | ------------------------------------------------------------------------ |
| OQ-1 | ~~Which specific Java framework?~~   | ~~Architecture phase~~ | **Resolved: Spring Boot**                                                |
| OQ-2 | ~~Which database?~~                  | ~~Architecture phase~~ | **Resolved: PostgreSQL**                                                 |
| OQ-3 | ~~Whereby room strategy?~~           | ~~Architecture phase~~ | **Resolved: one room per appointment**                                   |
| OQ-4 | ~~Admin seed credentials delivery?~~ | ~~Architecture phase~~ | **Resolved: plain text in config file for v1**                           |
| OQ-5 | ~~Doctor rating ownership?~~         | ~~Future iteration~~   | **Resolved: patient rates after completed appointment, 1–10, editable**  |
| OQ-6 | ~~Admin edit patient ratings?~~      | ~~Future iteration~~   | **Resolved: no — only the submitting patient can edit their own rating** |
