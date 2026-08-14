# Epic 5 Context: Doctor Rating System

<!-- Compiled from planning artifacts. Edit freely. Regenerate with compile-epic-context if planning docs change. -->

## Goal

Patients can rate doctors 1–10 after an appointment reaches Completed status. Ratings are editable. Each doctor's average is stored as a DECIMAL(3,1) on the `users` table, recalculated on every submit or edit, and rendered with a colour-coded badge on doctor profiles and search results.

## Stories

- Story 5.1: Rating Submission on Completed Appointments
- Story 5.2: Rating Editing
- Story 5.3: Colour-Coded Doctor Rating Display

## Requirements & Constraints

- One rating row per appointment (`ratings` table: `id`, `appointment_id`, `patient_id`, `doctor_id`, `value INTEGER 1–10`).
- Rating input visible only on Completed appointment cards in patient history; hidden for all other statuses.
- Pre-fill the existing value when the patient re-opens a card they already rated.
- On every insert or update, recalculate `ROUND(AVG(value), 1)` across all ratings for that doctor and persist to `users.average_rating DECIMAL(3,1)`.
- Only the submitting patient may submit or edit their own rating; doctor and admin callers receive 403. A patient cannot edit a rating belonging to a different patient.
- Colour badge rules: 1.00–5.00 → orange; 5.01–8.00 → light blue; 8.01–10.00 → light green. No badge when `average_rating IS NULL` (no ratings yet).

## Technical Decisions

- `ratings` table holds the source of truth; `users.average_rating` is a derived, persisted cache (AD-8).
- Average recalculation uses `ROUND(AVG(value), 1)` via JPA query in the service layer, then updates `users.average_rating` in the same transaction (`@Transactional`).
- No new Pinia store needed — rating state lives in `appointmentStore` (appointment cards carry the rating payload) and `doctorStore` (carries `averageRating` used in search results and doctor profile).
- Server endpoints under `/api/v1/ratings/`; secured by Spring Security with role claims from JWT (AD-15).
- All DB access through JPA repositories extending `JpaRepository` (AD-14).
- No SSE event required for rating changes — only `average_rating` updates are visible to other users, and they are fetched on next page load.

## UX & Interaction Patterns

- Rating widget on completed appointment card: numeric input or star/slider control, 1–10 range, with a Save button.
- Badge on doctor search result card and doctor profile page: short numeric display (e.g. "8.9") with background colour matching the tier.
- No badge rendered when a doctor has zero ratings.

## Cross-Story Dependencies

- 5.1 creates the `ratings` table and the POST endpoint — 5.2 depends on the same table and extends it with PUT/PATCH.
- 5.3 consumes `users.average_rating` which is populated by 5.1 and 5.2; search results (Epic 2) and doctor profile cards must be updated to render the badge.
- The `ratings` DB migration must run before 5.1 is deployed; 5.3 can be merged independently once `average_rating` is being populated.
