# Epic 3 Context: Appointment Lifecycle, History & Video Consultation

<!-- Compiled from planning artifacts. Edit freely. Regenerate with compile-epic-context if planning docs change. -->

## Goal

Make appointment data visible and actionable for both patients and doctors. Patients see a full appointment history with real-time status updates and styled badges. Doctors manage their appointments (upcoming/past) and can join, cancel, or complete them. The Join button enforces a 10-minute pre-start window. A scheduled background job auto-cancels no-show appointments. SSE carries all status changes to connected clients within 1–2 seconds.

## Stories

- Story 3.1: Patient Appointment History View
- Story 3.2: Real-Time Appointment Status Updates
- Story 3.3: Video Consultation Join Button
- Story 3.4: Auto-Cancellation Background Job
- Story 3.5: Doctor My Appointments View
- Story 3.6: Doctor Appointment Actions

## Requirements & Constraints

- Patient history shows ALL appointments (past and future); no pagination required in v1.
- Appointment card fields for patient: date, time, doctor name, doctor speciality, status badge, createdAt (small corner label).
- If the doctor has been soft-deleted, the card still shows the doctor's name alongside a "Removed" badge.
- Status badge colours: Open=blue, Completed=green, Canceled=orange, Auto-Canceled=grey.
- Doctor history has two tabs: Upcoming (OPEN future appointments) and Past (terminal-status: COMPLETED, CANCELED, AUTO_CANCELED).
- Appointment card fields for doctor: patient name, date, time, speciality, status badge, createdAt. If patient is soft-deleted → "Removed" badge.
- Join button is active only when `now ∈ [scheduledStart − 10 min, scheduledStart]`; disabled otherwise. Clicking opens `wherebyRoomUrl`. Non-OPEN appointments never have an active Join button.
- Doctor can Cancel or Complete any OPEN future appointment; status change broadcasts an SSE event to all clients.
- SSE channel from Story 2.2 is extended: appointment status changes broadcast with event name `"appointment-status"` carrying `{ appointmentId, newStatus }`. Clients listen via `EventSource.addEventListener("appointment-status", ...)`.
- Auto-cancel job: `@Scheduled(fixedRate=60_000)` queries `WHERE status=OPEN AND scheduled_date < NOW() - INTERVAL 10 minutes`; updates to AUTO_CANCELED; fires one SSE `"appointment-status"` event per affected appointment.
- Auto-cancel must be idempotent: re-running on restart only catches still-OPEN overdue appointments; nothing double-cancels.

## Technical Decisions

**New endpoints:**

- `GET /api/v1/appointments/my` (PATIENT) → all patient's appointments ordered by date desc
- `GET /api/v1/appointments/doctor/my` (DOCTOR) → all doctor's appointments
- `PATCH /api/v1/appointments/{id}/status` (DOCTOR) → cancel or complete

**New SSE event type:** `"appointment-status"` with payload `{ "appointmentId": Long, "newStatus": String }`. Broadcast by `SseService.broadcastAppointmentStatus(AppointmentStatusEvent)`.

**New `AppointmentRepository` queries:** `findByPatientIdOrderByScheduledDateDesc`, `findByDoctorIdOrderByScheduledDateDesc`, `findByStatusAndScheduledDateBefore` (for auto-cancel).

**Client stores:** `appointmentStore` holds `patientAppointments` and `doctorAppointments`; SSE `"appointment-status"` events patch matching records in both lists.

**Router:** `/appointments` with `requiresPatient`; `/appointments/doctor` with `requiresDoctor` (new meta flag).

## Cross-Story Dependencies

- Story 3.1 establishes the patient history endpoint and view; 3.2 adds the SSE subscription that keeps it live.
- Story 3.5 establishes the doctor history endpoint and view; 3.6 adds the status-change actions that trigger SSE.
- Story 3.3 (Join button) depends on both 3.1/3.5 views existing and `wherebyRoomUrl` being stored (Epic 2.4).
- Story 3.4 (auto-cancel) requires the SSE broadcast infra from 3.2/3.6 to be in place.
- Epic 4 (soft-delete display) expects the "Removed" badge pattern established in 3.1/3.5 to be reused.
