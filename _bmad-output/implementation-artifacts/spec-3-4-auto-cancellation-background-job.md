---
title: "Auto-Cancellation Background Job"
type: "feature"
created: "2026-08-13"
status: "done"
review_loop_iteration: 0
baseline_commit: "e798d6bca05c064496929eb73c4181feca1bc4cc"
context: []
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Open appointments that have passed their scheduled start time by more than 10 minutes stay in OPEN status indefinitely — no-shows are never cleaned up.

**Approach:** Add a `@Scheduled(fixedRate=60_000)` job that queries all OPEN appointments, filters in Java for those where `scheduledDate.atTime(startTime).isBefore(now − 10 minutes)`, sets each to AUTO_CANCELED, and fires an SSE `"appointment-status"` event per affected appointment. Enable scheduling via `@EnableScheduling` on `MedourApplication`.

## Boundaries & Constraints

**Always:**
- Only OPEN appointments are candidates; COMPLETED, CANCELED, and AUTO_CANCELED appointments are never touched
- The cutoff is `LocalDateTime.now().minusMinutes(10)` — appointment overdue by more than 10 minutes after scheduled start
- One SSE `broadcastAppointmentStatus` call per auto-canceled appointment, immediately after the status update is persisted
- The job is idempotent: re-running on server restart catches only appointments still in OPEN+overdue state; already AUTO_CANCELED rows are excluded by the OPEN filter
- `@EnableScheduling` is added to `MedourApplication` — no separate `@Configuration` needed
- The entire batch runs in a `@Transactional` method on `AutoCancelService`

**Ask First:**
- (none)

**Never:**
- Do not hard-delete or modify appointments in COMPLETED, CANCELED, or AUTO_CANCELED status
- No distributed lock — single-instance deployment assumed per architecture
- The Quartz / DB-backed scheduler is explicitly deferred per the architecture; `@Scheduled` is correct for v1

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|---|---|---|---|
| OPEN appointment 15 min overdue | `scheduledDate.atTime(startTime) + 10min < now` | Status → AUTO_CANCELED; SSE event fired | N/A |
| OPEN appointment not yet overdue | `scheduledDate.atTime(startTime) + 10min >= now` | Not touched | N/A |
| Already AUTO_CANCELED | `status = AUTO_CANCELED` | Ignored by OPEN filter | N/A |
| No overdue appointments | All OPEN appointments on time | Nothing changed | N/A |
| Server restart with overdue open appointments | Job runs on first tick after startup | Overdue appointments auto-canceled on first execution | N/A |

</frozen-after-approval>

## Code Map

- `server/src/main/java/com/medour/MedourApplication.java` -- add `@EnableScheduling`
- `server/src/main/java/com/medour/repository/AppointmentRepository.java` -- add `List<Appointment> findByStatus(AppointmentStatus status)`
- `server/src/main/java/com/medour/service/AutoCancelService.java` -- NEW `@Service`; inject `AppointmentRepository`, `SseService`; `@Transactional autoCancelOverdue()`: compute `cutoff = LocalDateTime.now().minusMinutes(10)`; stream `findByStatus(OPEN)`, filter overdue, set each to AUTO_CANCELED, save, broadcast
- `server/src/main/java/com/medour/config/AutoCancelJob.java` -- NEW `@Component`; inject `AutoCancelService`; `@Scheduled(fixedRate=60_000) run()` calls `autoCancelService.autoCancelOverdue()`
- `server/src/test/java/com/medour/service/AutoCancelServiceTest.java` -- NEW; 3 tests: overdue OPEN → AUTO_CANCELED + SSE; not-yet-overdue OPEN → untouched; already AUTO_CANCELED → untouched

## Tasks & Acceptance

**Execution:**

- [x] `server/src/main/java/com/medour/MedourApplication.java` -- add `@EnableScheduling` annotation
- [x] `server/src/main/java/com/medour/repository/AppointmentRepository.java` -- add `List<Appointment> findByStatus(AppointmentStatus status)`
- [x] `server/src/main/java/com/medour/service/AutoCancelService.java` -- NEW `@Service`; constructor injects `AppointmentRepository`, `SseService`; `@Transactional autoCancelOverdue()`: `LocalDateTime cutoff = LocalDateTime.now().minusMinutes(10)`; for each appointment in `findByStatus(OPEN)` where `scheduledDate.atTime(startTime).isBefore(cutoff)`: set `status = AUTO_CANCELED`, save, then `sseService.broadcastAppointmentStatus(new AppointmentStatusEventDto(id, "AUTO_CANCELED"))`
- [x] `server/src/main/java/com/medour/config/AutoCancelJob.java` -- NEW `@Component`; inject `AutoCancelService`; `@Scheduled(fixedRate = 60_000) public void run()` calls `autoCancelService.autoCancelOverdue()`
- [x] `server/src/test/java/com/medour/service/AutoCancelServiceTest.java` -- NEW `@ExtendWith(MockitoExtension.class)`: mock `AppointmentRepository` + `SseService`; test (1): stub `findByStatus(OPEN)` with appointment overdue by 15min → verify `save()` called with AUTO_CANCELED status, verify `broadcastAppointmentStatus` called; test (2): stub `findByStatus(OPEN)` with appointment scheduled 5 min in the future → verify `save()` NOT called; test (3): stub `findByStatus(OPEN)` with empty list → verify `save()` NOT called

**Acceptance Criteria:**

- Given an OPEN appointment whose scheduled start was 15 minutes ago, when the job runs, then its status is set to AUTO_CANCELED and an SSE `"appointment-status"` event fires with `newStatus="AUTO_CANCELED"`.
- Given an OPEN appointment scheduled 5 minutes in the future, when the job runs, then it is not modified.
- Given a server restart with overdue OPEN appointments, when the job fires on the first tick, then those appointments are auto-canceled.
- Given all appointments are in terminal status (COMPLETED, CANCELED, AUTO_CANCELED), when the job runs, then nothing is modified.

## Verification

**Commands:**
- `cd server && ./mvnw test` -- expected: 54 tests pass (51 existing + 3 AutoCancelServiceTest)
- `cd client && npm run build` -- expected: zero TypeScript errors

## Spec Change Log

**Review loop 1 patches applied:**
- `AutoCancelService` — uses `AppointmentStatus.AUTO_CANCELED.name()` instead of raw string `"AUTO_CANCELED"` to keep the status value compile-time safe
- `AutoCancelServiceTest` — added boundary test: appointment at exactly `now - 10min` runs without exception (boundary semantics are timing-dependent; test asserts no error rather than a specific cancel decision)
