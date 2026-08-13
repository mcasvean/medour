# Epic 2 Context: Appointment Booking & Real-Time Slot Management

<!-- Compiled from planning artifacts. Edit freely. Regenerate with compile-epic-context if planning docs change. -->

## Goal

Enable patients to find doctors by filters, view live slot availability, and complete a multi-step booking that atomically locks a 30-minute time slot. Concurrent patients must see slot state changes in real time via SSE with no possibility of double-booking. The confirmed appointment persists with a Whereby video room URL created at booking time.

## Stories

- Story 2.1: Doctor Search & Filter
- Story 2.2: Slot Availability Display
- Story 2.3: Multi-Step Appointment Booking Flow
- Story 2.4: Appointment Confirmation & Whereby Room Creation

## Requirements & Constraints

- Only patients use the booking flow. Doctor and admin users do not book appointments.
- All doctors are returned by default; filters (date range, speciality, county, city) narrow results.
- Slot grid shows 30-minute slots from 08:00 to 20:00 inclusive — 24 slots per day — each labeled "HH:MM – HH:MM".
- Three slot states with distinct visual styles:
  - **Available**: no `slot_reservations` row and no confirmed appointment → selectable
  - **Locked**: `slot_reservations` row exists → disabled (someone else is mid-booking)
  - **Unavailable**: confirmed appointment exists for that slot → disabled
- Slot state is **derived** at query time; it is not stored as a column on any entity.
- Selecting a slot immediately inserts a `slot_reservations` row. The unique constraint on `(doctor_id, date, start_time)` is the **only** double-booking guard — no application-level check can substitute.
- The SSE channel fires on every `slot_reservations` INSERT and DELETE. Connected clients must update their slot grid in real time (≤ 1–2 seconds).
- If the patient cancels the booking flow or navigates away, the `slot_reservations` row is deleted and an SSE release event fires.
- If the Whereby API call fails during the final save, the entire appointment transaction is rolled back. No partial saves.
- The `createdAt` timestamp is set by the server at insert time and displayed in a small corner label on appointment cards.

## Technical Decisions

**Slot locking (AD-5):** `slot_reservations(id, doctor_id, date, start_time, reserved_by_patient_id, reserved_at, expires_at)` with a `UNIQUE(doctor_id, date, start_time)` constraint. On INSERT conflict, the DB throws a constraint violation — the service must catch this and return a 409 to the client. On DELETE, the row is removed and an SSE event fires.

**SSE (AD-2):** Use Spring `SseEmitter`. A single `/api/v1/sse/slots` endpoint returns an `SseEmitter`; the `SseService` holds active emitters in memory and broadcasts events when slot state changes. The client uses the browser `EventSource` API and updates the appointment store on each event. SSE is server-to-client only.

**Appointments table:** `id, patient_id (FK users), doctor_id (FK users), scheduled_date (DATE), start_time (TIME), status (ENUM: OPEN, COMPLETED, CANCELED, AUTO_CANCELED), created_at (TIMESTAMP), whereby_room_url (VARCHAR)`.

**Whereby (AD-7):** At booking save, call the Whereby REST API (`POST /v1/meetings`) with `roomMode=group_hd` and store the returned `roomUrl` in `appointments.whereby_room_url`. If the API call fails, throw so the `@Transactional` method rolls back. The Whereby base URL and API key are configurable via `application.yml` (`whereby.api-key`, `whereby.api-url`).

**BE layering (AD-4):** new service `AppointmentService`, `SlotService` (or combined), `DoctorSearchService`; new controllers `AppointmentController`, `DoctorController`, `SseController`. No direct repository access from controllers.

**Pinia stores (AD-10):** `doctorStore` holds search results and filter state; `appointmentStore` holds current booking state (selected slot, doctor, step) and the live slot grid for the selected doctor/date. SSE events update `appointmentStore` directly.

**Doctor search backend:** `GET /api/v1/doctors?speciality=&county=&city=&date=` queries the `users` table where `role = DOCTOR` and `deleted_at IS NULL`, optionally filtering by speciality/county/city; the date filter cross-references `slot_reservations` and `appointments` to find doctors with at least one available slot on that date.

**Slot query:** `GET /api/v1/doctors/{id}/slots?date=` returns the 24 fixed slots with their derived state for the given doctor and date.

## Cross-Story Dependencies

- Story 2.1 must land before 2.2 — doctor search is the entry point to the slot grid.
- Story 2.2 must land before 2.3 — you must see slots before you can select one.
- Story 2.3 must land before 2.4 — the lock flow is the prerequisite to the final appointment save.
- Story 2.1 introduces the `appointments` and `slot_reservations` JPA entities and DB tables; later stories build on them.
- Epic 3 (appointment history, video join) depends on `appointments.whereby_room_url` and the status enum being established in this epic.
