---
title: "Video Consultation Join Button"
type: "feature"
created: "2026-08-13"
status: "done"
review_loop_iteration: 0
baseline_commit: "f4ca93b3dd695bc4410cd5aa1f7a38d2620c1b32"
context: []
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Appointment cards show no way to join a video consultation. The `wherebyRoomUrl` field is missing from the patient history DTO and there is no time-window check or Join button in the UI.

**Approach:** Add `wherebyRoomUrl` to `PatientAppointmentDto` and its service mapping; add a client-side `isJoinActive(scheduledDate, startTime, status)` helper that returns true when the appointment is OPEN and the current time is within 10 minutes before `scheduledStart`; render a Join button on OPEN appointment cards that is active only in that window, and on click opens `wherebyRoomUrl` in a new tab.

## Boundaries & Constraints

**Always:**
- Join button is only ever rendered for appointments with `status === 'OPEN'`
- The join window activates at `scheduledStart − 10 minutes` and the check is purely client-side; the server does not validate timing
- An active Join button opens `wherebyRoomUrl` in a new browser tab (`window.open(url, '_blank', 'noopener')`) — no navigation within the SPA
- A disabled Join button is visible but non-clickable (not hidden) — the patient can see they will be able to join soon
- If `wherebyRoomUrl` is null (dev-mode appointments), the Join button should not be rendered at all
- The join window upper bound: `now <= scheduledStart + 30 minutes` — this covers the full 30-minute consultation slot; after that window the auto-cancel job fires anyway

**Ask First:**
- (none)

**Never:**
- No server-side endpoint for join authorisation in this story
- The Join button is NOT added to the doctor view in this story (Story 3.5)
- Do not navigate within the SPA on join — always open a new tab

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|---|---|---|---|
| OPEN appointment, inside join window | `now ∈ [start − 10min, start + 30min]` | Join button rendered and active | N/A |
| OPEN appointment, outside join window | `now < start − 10min` | Join button rendered but disabled | N/A |
| Non-OPEN appointment | status = COMPLETED / CANCELED / AUTO_CANCELED | Join button not rendered | N/A |
| Null `wherebyRoomUrl` | dev-mode appointment | Join button not rendered | N/A |
| Click active Join button | valid URL | Opens in new tab, no SPA navigation | N/A |

</frozen-after-approval>

## Code Map

- `server/src/main/java/com/medour/dto/PatientAppointmentDto.java` -- add `String wherebyRoomUrl` component
- `server/src/main/java/com/medour/service/PatientAppointmentService.java:27` -- add `a.getWherebyRoomUrl()` to the DTO mapping
- `server/src/test/java/com/medour/service/PatientAppointmentServiceTest.java` -- update DTO construction to include wherebyRoomUrl; add test asserting wherebyRoomUrl is mapped correctly
- `client/src/stores/appointmentStore.ts` -- extend `PatientAppointment` interface with `wherebyRoomUrl: string | null`; add standalone `isJoinActive(scheduledDate, startTime, status)` export function: returns `status === 'OPEN' && now >= start - 10min && now <= start + 30min`
- `client/src/views/PatientAppointmentsView.vue` -- import `isJoinActive`; add Join button to each card: `v-if="appt.status === 'OPEN' && appt.wherebyRoomUrl"`, `:disabled="!isJoinActive(appt.scheduledDate, appt.startTime, appt.status)"`, `@click="join(appt.wherebyRoomUrl)"`; `join(url)` calls `window.open(url, '_blank', 'noopener')`

## Tasks & Acceptance

**Execution:**

- [ ] `server/src/main/java/com/medour/dto/PatientAppointmentDto.java` -- add `String wherebyRoomUrl` as last component
- [ ] `server/src/main/java/com/medour/service/PatientAppointmentService.java` -- add `a.getWherebyRoomUrl()` as last arg to `PatientAppointmentDto` constructor call
- [ ] `server/src/test/java/com/medour/service/PatientAppointmentServiceTest.java` -- update `Appointment.builder()` calls to set `.wherebyRoomUrl("https://whereby.com/test-room")`; update DTO assertions to verify `result.get(0).wherebyRoomUrl()` equals the expected URL; add test asserting null `wherebyRoomUrl` maps to null in DTO
- [ ] `client/src/stores/appointmentStore.ts` -- add `wherebyRoomUrl: string | null` to `PatientAppointment` interface; export `function isJoinActive(scheduledDate: string, startTime: string, status: string): boolean` — parse `scheduledDate + 'T' + startTime` to a `Date`, compute `windowStart = start - 10min`, `windowEnd = start + 30min`; return `status === 'OPEN' && now >= windowStart && now <= windowEnd`
- [ ] `client/src/views/PatientAppointmentsView.vue` -- import `isJoinActive` from store; add `function join(url: string) { window.open(url, '_blank', 'noopener') }` in script; on each card after the card-body: `<button v-if="appt.status === 'OPEN' && appt.wherebyRoomUrl" :disabled="!isJoinActive(appt.scheduledDate, appt.startTime, appt.status)" @click="join(appt.wherebyRoomUrl!)">Join</button>`

**Acceptance Criteria:**

- Given an OPEN appointment and current time is between `scheduledStart − 10 min` and `scheduledStart + 30 min`, the Join button is active and clickable.
- Given an OPEN appointment and current time is before `scheduledStart − 10 min`, the Join button is rendered but disabled.
- Given a non-OPEN appointment, no Join button is rendered.
- Given a null `wherebyRoomUrl`, no Join button is rendered.
- Given the patient clicks an active Join button, `wherebyRoomUrl` opens in a new browser tab.

## Verification

**Commands:**
- `cd server && ./mvnw test` -- expected: all 50 tests pass (PatientAppointmentServiceTest updated)
- `cd client && npm run test` -- expected: 33 existing + 2 new `isJoinActive` unit tests pass
- `cd client && npm run build` -- expected: zero TypeScript errors

## Spec Change Log

**Review loop 1 patches applied:**
- `PatientAppointmentsView.vue` — `window.open` now uses `'noopener,noreferrer'` to prevent referrer leakage to the Whereby room URL
- `appointmentStore.test.ts` — added third `isJoinActive` test case asserting COMPLETED, CANCELED, and AUTO_CANCELED all return `false` even when inside the join window
