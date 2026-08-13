---
title: "Real-Time Appointment Status Updates"
type: "feature"
created: "2026-08-13"
status: "done"
review_loop_iteration: 0
baseline_commit: "b1851af0ccdca5a9afad6a114ea71fe26e93d975"
context: []
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** The patient appointment history is static — status changes made by doctors (Stories 3.5/3.6) or the auto-cancel job (Story 3.4) are invisible until the patient refreshes. The SSE channel only carries slot events; appointment status events are not yet defined or wired.

**Approach:** Add `AppointmentStatusEventDto` and `SseService.broadcastAppointmentStatus()` that sends a named `"appointment-status"` SSE event; extend `appointmentStore` with `connectAppointmentSse()` / `disconnectAppointmentSse()` that listen for that named event on the existing `/api/v1/sse/slots` channel; call them from `PatientAppointmentsView` on mount/unmount. Stories 3.4 and 3.6 will trigger the broadcast; this story wires the plumbing and receiver.

## Boundaries & Constraints

**Always:**

- Named event `"appointment-status"` on the existing `/api/v1/sse/slots` SSE channel — no new endpoint needed
- `SseEmitter.event().name("appointment-status").data(json)` is the send pattern; the client listens with `EventSource.addEventListener("appointment-status", handler)`
- SSE event payload: `{ "appointmentId": Long, "newStatus": String }` — sufficient for the client to locate and patch the appointment card
- `appointmentStore.connectAppointmentSse()` opens a **separate** `EventSource` from the slot-update one, stored in `_appointmentEventSource`; both can be open simultaneously (slot grid and history view on different pages)
- On receiving an event, the handler finds the matching appointment in `patientAppointments` by `appointmentId` and updates its `status`; unknown IDs are silently ignored
- `broadcastAppointmentStatus()` follows the same pattern as `broadcast()`: catch all exceptions on send, remove dead emitters
- The method is defined here but first called by Stories 3.4 and 3.6 — no caller in this story

**Ask First:**

- (none)

**Never:**

- Do not merge the slot and appointment SSE handlers into the same `EventSource.onmessage` — named events (`addEventListener`) are semantically distinct from the default event
- Do not fire any `broadcastAppointmentStatus()` call in this story (Stories 3.4 / 3.6 do that)

## I/O & Edge-Case Matrix

| Scenario                            | Input / State                                                       | Expected Output / Behavior                        | Error Handling |
| ----------------------------------- | ------------------------------------------------------------------- | ------------------------------------------------- | -------------- |
| Appointment status event received   | SSE `"appointment-status"` for appointment in `patientAppointments` | Matching card's status badge updates in real time | N/A            |
| Unknown appointment ID in event     | appointmentId not in `patientAppointments`                          | Event silently ignored                            | N/A            |
| Malformed JSON in SSE event         | Unparseable data                                                    | try/catch, silently dropped                       | N/A            |
| `disconnectAppointmentSse()` called | `_appointmentEventSource` open                                      | EventSource closed and nulled                     | N/A            |

</frozen-after-approval>

## Code Map

- `server/src/main/java/com/medour/dto/AppointmentStatusEventDto.java` -- NEW record: `Long appointmentId, String newStatus`
- `server/src/main/java/com/medour/service/SseService.java:31` -- add `broadcastAppointmentStatus(AppointmentStatusEventDto event)` sending `SseEmitter.event().name("appointment-status").data(json)` to all live emitters
- `server/src/test/java/com/medour/service/SseServiceTest.java` -- NEW: test that `broadcastAppointmentStatus` sends event with name "appointment-status"
- `client/src/stores/appointmentStore.ts` -- add `_appointmentEventSource: null as EventSource|null`; add `connectAppointmentSse()` and `disconnectAppointmentSse()` actions; `connectAppointmentSse` opens EventSource, adds listener for `"appointment-status"` named event, JSON-parses payload, finds appointment in `patientAppointments` by id, updates status
- `client/src/views/PatientAppointmentsView.vue` -- call `appointmentStore.connectAppointmentSse()` on mount; call `appointmentStore.disconnectAppointmentSse()` on unmount

## Tasks & Acceptance

**Execution:**

- [ ] `server/src/main/java/com/medour/dto/AppointmentStatusEventDto.java` -- NEW record: `Long appointmentId, String newStatus`
- [ ] `server/src/main/java/com/medour/service/SseService.java` -- add method: `public void broadcastAppointmentStatus(AppointmentStatusEventDto event)` — same JSON-serialize-then-send pattern as `broadcast()`, but use `SseEmitter.event().name("appointment-status").data(json)` so clients can distinguish it via `addEventListener`
- [ ] `server/src/test/java/com/medour/service/SseServiceTest.java` -- NEW `@ExtendWith(MockitoExtension.class)`; inject a real `SseService` with mocked `ObjectMapper` or use a real one; add one live `SseEmitter`; call `broadcastAppointmentStatus(new AppointmentStatusEventDto(1L, "CANCELED"))`; assert the emitter received a send call (capture via spy or verify no exception) — or use a real emitter and verify the sent event name is `"appointment-status"`
- [ ] `client/src/stores/appointmentStore.ts` -- add `_appointmentEventSource: null as EventSource | null` to state; add `connectAppointmentSse()` action: `disconnectAppointmentSse()`; open new `EventSource('/api/v1/sse/slots')`; `addEventListener('appointment-status', (e) => { try { const p = JSON.parse(e.data); const appt = this.patientAppointments.find(a => a.id === p.appointmentId); if (appt) appt.status = p.newStatus; } catch {} })`; store in `_appointmentEventSource`; add `disconnectAppointmentSse()` action: close and null `_appointmentEventSource`
- [ ] `client/src/views/PatientAppointmentsView.vue` -- import `onUnmounted`; in `onMounted` call `appointmentStore.connectAppointmentSse()`; add `onUnmounted(() => appointmentStore.disconnectAppointmentSse())`

**Acceptance Criteria:**

- Given a patient viewing their appointment history and a doctor changes an appointment's status, when the SSE `"appointment-status"` event arrives, then the corresponding card's status badge updates without a page refresh.
- Given an `"appointment-status"` event for an appointment ID not in the patient's list, then the event is silently ignored.
- Given the patient navigates away from the history view, then `disconnectAppointmentSse()` is called and the EventSource is closed.

## Verification

**Commands:**

- `cd server && ./mvnw test` -- expected: 50 tests pass (49 existing + 1 SseServiceTest)
- `cd client && npm run test` -- expected: all 29 tests pass + 2 new appointmentStore tests for SSE
- `cd client && npm run build` -- expected: zero TypeScript errors

## Spec Change Log

**Review loop 1 patches applied:**

- `appointmentStore.test.ts` — added `disconnectAppointmentSse` test (close + null); added malformed JSON test (no throw, status unchanged)
- Known limitation noted: `SseServiceTest` cannot verify the `"appointment-status"` event name without a real HTTP SSE connection; `SseEmitter.SseEventBuilder` does not expose the name field for assertion. The client-side tests exercise the named-event handler directly and provide the practical regression coverage.
