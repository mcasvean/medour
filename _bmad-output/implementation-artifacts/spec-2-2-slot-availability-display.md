---
title: "Slot Availability Display"
type: "feature"
created: "2026-08-13"
status: "done"
review_loop_iteration: 0
baseline_commit: "ad32b33f85175b8194705e482e1cb74958d5badf"
context: []
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** After finding a doctor in search, patients have no way to see that doctor's available time slots. There is no slot grid, no slot state derivation, and no SSE infrastructure for real-time updates.

**Approach:** Add `GET /api/v1/doctors/{id}/slots?date=` that returns 24 fixed 30-minute slots with derived state (AVAILABLE, LOCKED, UNAVAILABLE); set up the `SseService` and `GET /api/v1/sse/slots` streaming endpoint; populate `appointmentStore` with slot state and SSE connection management; add a `SlotGrid.vue` component that renders the grid with visual state; clicking a doctor card in `BookingSearchView` selects them and shows their slot grid for the chosen date.

## Boundaries & Constraints

**Always:**
- 24 slots per day, generated from 08:00 to 19:30 in 30-minute steps (last slot: 19:30–20:00)
- Slot state is derived — never stored as a column. Derivation order (first match wins): (1) non-expired `slot_reservations` row → LOCKED; (2) OPEN `appointments` row → UNAVAILABLE; (3) else → AVAILABLE
- SSE endpoint `GET /api/v1/sse/slots` is added to `permitAll` in SecurityConfig — slot state is not sensitive data; all clients subscribe regardless of auth
- SSE events are broadcast to ALL active emitters (not filtered by doctor/date on the server); the client filters events by `doctorId` and `date` before applying state updates
- SSE event payload: `{ "doctorId": Long, "date": "YYYY-MM-DD", "startTime": "HH:MM", "state": "AVAILABLE"|"LOCKED"|"UNAVAILABLE" }`
- `SseService` holds emitters in a `CopyOnWriteArrayList`; dead emitters (on send failure) are removed immediately
- `SlotGrid.vue` is read-only in this story — clicking a slot does nothing (Story 2.3 adds selection)
- Clicking a doctor card in `BookingSearchView` sets `appointmentStore.selectedDoctorId` and fetches slots for `doctorStore.filters.date` (or today if date filter is empty)
- `appointmentStore.connectSse(doctorId, date)` opens an `EventSource` to `/api/v1/sse/slots`; `disconnectSse()` closes it; the store cleans up on date or doctor change

**Ask First:**
- (none)

**Never:**
- No slot selection or lock in this story (Story 2.3)
- No SSE events broadcast yet (Story 2.3 triggers them on lock/unlock); Story 2.2 only establishes the subscription infrastructure
- SSE is server-to-client only — `EventSource` sends no data back

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|---|---|---|---|
| All slots free | `GET /slots?date=2026-09-01`; no reservations or appointments | 24 slots all AVAILABLE | N/A |
| Slot locked | `slot_reservations` non-expired row for slot 10:00 | That slot returns LOCKED | N/A |
| Slot unavailable | OPEN appointment for slot 14:00 | That slot returns UNAVAILABLE | N/A |
| Expired reservation | `slot_reservations` row with `expiresAt` < now for slot 08:00 | That slot returns AVAILABLE | N/A |
| SSE subscribe | `GET /api/v1/sse/slots` | HTTP 200 with `Content-Type: text/event-stream` emitter returned | N/A |
| SSE event received | `appointmentStore` receives event for active doctorId+date | Matching slot's state updated in store | Unknown events ignored |

</frozen-after-approval>

## Code Map

- `server/src/main/java/com/medour/model/SlotState.java` -- NEW enum: AVAILABLE, LOCKED, UNAVAILABLE
- `server/src/main/java/com/medour/dto/SlotDto.java` -- NEW record: String startTime ("HH:MM"), String endTime ("HH:MM"), SlotState state
- `server/src/main/java/com/medour/dto/SlotEventDto.java` -- NEW record: Long doctorId, String date, String startTime, SlotState state (JSON payload for SSE events)
- `server/src/main/java/com/medour/repository/SlotReservationRepository.java` -- add `existsByDoctorIdAndDateAndStartTimeAndExpiresAtAfter(Long, LocalDate, LocalTime, LocalDateTime)`
- `server/src/main/java/com/medour/repository/AppointmentRepository.java` -- add `existsByDoctorIdAndScheduledDateAndStartTimeAndStatus(Long, LocalDate, LocalTime, AppointmentStatus)`
- `server/src/main/java/com/medour/service/SlotService.java` -- NEW; generates 24 fixed slots; derives state per slot from repositories
- `server/src/main/java/com/medour/service/SseService.java` -- NEW; `CopyOnWriteArrayList<SseEmitter>`; `subscribe()` → registers and returns emitter; `broadcast(SlotEventDto)` → sends JSON to all live emitters
- `server/src/main/java/com/medour/controller/DoctorController.java:28` -- add `GET /{id}/slots?date=` endpoint calling `slotService.getSlotsForDoctor(id, date)`
- `server/src/main/java/com/medour/controller/SseController.java` -- NEW `@RestController("/api/v1/sse")`; `GET /slots` → `sseService.subscribe()`
- `server/src/main/java/com/medour/config/SecurityConfig.java:43` -- add `GET /api/v1/sse/slots` to permitAll
- `server/src/test/java/com/medour/service/SlotServiceTest.java` -- NEW: 3 tests covering all-free, locked, unavailable, expired-reservation-is-free
- `server/src/test/java/com/medour/controller/SseControllerTest.java` -- NEW: 1 test: GET /sse/slots → 200 + event-stream content type
- `client/src/stores/appointmentStore.ts` -- populate: `selectedDoctorId: number|null`, `selectedDate: string`, `slots: SlotDisplay[]`, `connectSse(id, date)`, `disconnectSse()`, `fetchSlots(id, date)` action
- `client/src/views/BookingSearchView.vue` -- clicking a doctor card calls `appointmentStore.fetchSlots(id, selectedDate)` and `appointmentStore.connectSse(id, selectedDate)`, sets `selectedDoctorId`
- `client/src/components/SlotGrid.vue` -- NEW: takes `slots` prop; renders 24 slots in a grid with CSS class per state (available/locked/unavailable); read-only (no click handler)

## Tasks & Acceptance

**Execution:**

- [ ] `server/src/main/java/com/medour/model/SlotState.java` -- NEW enum: AVAILABLE, LOCKED, UNAVAILABLE
- [ ] `server/src/main/java/com/medour/dto/SlotDto.java` -- NEW record: `String startTime, String endTime, SlotState state`
- [ ] `server/src/main/java/com/medour/dto/SlotEventDto.java` -- NEW record: `Long doctorId, String date, String startTime, SlotState state`
- [ ] `server/src/main/java/com/medour/repository/SlotReservationRepository.java` -- add `boolean existsByDoctorIdAndDateAndStartTimeAndExpiresAtAfter(Long doctorId, LocalDate date, LocalTime startTime, LocalDateTime now)`
- [ ] `server/src/main/java/com/medour/repository/AppointmentRepository.java` -- add `boolean existsByDoctorIdAndScheduledDateAndStartTimeAndStatus(Long doctorId, LocalDate date, LocalTime startTime, AppointmentStatus status)`
- [ ] `server/src/main/java/com/medour/service/SlotService.java` -- NEW `@Service`; inject both repositories; `getSlotsForDoctor(Long doctorId, LocalDate date)`: loop `LocalTime t = 08:00` stepping by 30 min until `t.equals(20:00)` is reached; for each slot: check reservation existence → LOCKED, else check OPEN appointment existence → UNAVAILABLE, else AVAILABLE; return `List<SlotDto>` with `t.toString()` for startTime and `t.plusMinutes(30).toString()` for endTime
- [ ] `server/src/main/java/com/medour/service/SseService.java` -- NEW `@Service`; `private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>()`; `subscribe()`: create `new SseEmitter(0L)` (no timeout), add to list, add onCompletion/onTimeout/onError callbacks that remove from list, return emitter; `broadcast(SlotEventDto event)`: serialize event to JSON, iterate emitters, call `emitter.send(...)`, on `IOException` remove emitter
- [ ] `server/src/main/java/com/medour/controller/DoctorController.java` -- inject `SlotService`; add `@GetMapping("/{id}/slots") getSlots(@PathVariable Long id, @RequestParam @DateTimeFormat(iso=DATE) LocalDate date)` → `ResponseEntity.ok(slotService.getSlotsForDoctor(id, date))`
- [ ] `server/src/main/java/com/medour/controller/SseController.java` -- NEW `@RestController @RequestMapping("/api/v1/sse")`; inject `SseService`; `@GetMapping("/slots") subscribe()` → `sseService.subscribe()`
- [ ] `server/src/main/java/com/medour/config/SecurityConfig.java` -- add `.requestMatchers(GET, "/api/v1/sse/slots").permitAll()` to the existing permitAll chain
- [ ] `server/src/test/java/com/medour/service/SlotServiceTest.java` -- NEW; mock repositories; test: (1) all slots free when both repos return false → all 24 AVAILABLE; (2) reservation for 10:00 → that slot LOCKED; (3) OPEN appointment for 14:00 → that slot UNAVAILABLE; (4) expired reservation for 08:00 (exists-call returns false) → AVAILABLE
- [ ] `server/src/test/java/com/medour/controller/SseControllerTest.java` -- NEW `@WebMvcTest(SseController.class) @AutoConfigureMockMvc(addFilters=false)`; `@MockBean SseService`; mock `subscribe()` returns `new SseEmitter()`; assert `GET /api/v1/sse/slots` → 200 + `text/event-stream` content type
- [ ] `client/src/stores/appointmentStore.ts` -- add: `interface SlotDisplay { startTime: string, endTime: string, state: 'AVAILABLE'|'LOCKED'|'UNAVAILABLE' }`; state: `selectedDoctorId: number|null`, `selectedDate: string`, `slots: SlotDisplay[]`; action `fetchSlots(doctorId, date)`: GET `/doctors/${doctorId}/slots?date=${date}`, set `slots`; action `connectSse(doctorId, date)`: open `new EventSource('/api/v1/sse/slots')`, on message parse JSON, if event.doctorId matches `selectedDoctorId` and event.date matches `selectedDate`, find slot by startTime and update its state; action `disconnectSse()`: close EventSource if open
- [ ] `client/src/views/BookingSearchView.vue` -- add "Select" button to each doctor card; on click: set `appointmentStore.selectedDoctorId = doctor.id`, call `appointmentStore.fetchSlots(doctor.id, dateToUse)`, call `appointmentStore.connectSse(doctor.id, dateToUse)`, where `dateToUse = doctorStore.filters.date || today`; display `<SlotGrid>` below doctor list when `appointmentStore.selectedDoctorId` is set
- [ ] `client/src/components/SlotGrid.vue` -- NEW: `defineProps<{ slots: SlotDisplay[] }>`; renders slot list with `:class="{ available: s.state === 'AVAILABLE', locked: s.state === 'LOCKED', unavailable: s.state === 'UNAVAILABLE' }"`; shows time label `{{ s.startTime }} – {{ s.endTime }}` and state text

**Acceptance Criteria:**

- Given a date with no reservations or appointments, when `GET /slots?date=` is called, then all 24 slots are returned with state AVAILABLE.
- Given a non-expired `slot_reservations` row for a time slot, when that slot is queried, then it returns LOCKED.
- Given an OPEN appointment for a time slot, when that slot is queried, then it returns UNAVAILABLE.
- Given an expired `slot_reservations` row, when the slot is queried, then it returns AVAILABLE.
- Given `GET /api/v1/sse/slots`, when called, then the response has content-type `text/event-stream`.
- Given a patient selects a doctor in `BookingSearchView`, when a date is set, then the slot grid renders 24 slots with visual states.
- Given an SSE event arrives for the selected doctor and date, when processed by `appointmentStore`, then the matching slot's state updates.

## Verification

**Commands:**
- `cd server && ./mvnw test` -- expected: 36 tests pass (29 existing + 4 SlotServiceTest + 1 SseControllerTest + 2 DoctorControllerTest.getSlots-related assertions)
- `cd client && npm run test` -- expected: all 17 tests pass
- `cd client && npm run build` -- expected: zero TypeScript errors

## Spec Change Log

**Review loop 1 patches applied:**
- `SseService.broadcast()` — catches `Exception` (not just `IOException`) so `IllegalStateException` from completed emitters also triggers removal
- `appointmentStore.connectSse()` — SSE `onmessage` handler wraps `JSON.parse` in try/catch; malformed events are silently dropped
- `DoctorControllerTest` — added `getSlots_validIdAndDate_returns200WithSlotList` verifying `@DateTimeFormat` param binding and response JSON
- `stores/__tests__/appointmentStore.test.ts` — NEW: 4 unit tests covering `fetchSlots` API call, SSE matching state update, SSE non-matching ignore, and `disconnectSse` cleanup

## Suggested Review Order

**Slot state derivation (core logic)**

- 24-slot generator, LOCKED→UNAVAILABLE→AVAILABLE derivation order
  [`SlotService.java:32`](../../server/src/main/java/com/medour/service/SlotService.java#L32)

- Repository methods: non-expired reservation check + OPEN appointment check
  [`SlotReservationRepository.java:14`](../../server/src/main/java/com/medour/repository/SlotReservationRepository.java#L14)

**SSE infrastructure**

- CopyOnWriteArrayList emitter registry; broadcast catches all exceptions
  [`SseService.java:21`](../../server/src/main/java/com/medour/service/SseService.java#L21)

- SSE endpoint; GET /api/v1/sse/slots in SecurityConfig permitAll
  [`SseController.java:1`](../../server/src/main/java/com/medour/controller/SseController.java#L1)

**Client slot store**

- fetchSlots, connectSse (JSON try/catch in handler), disconnectSse
  [`appointmentStore.ts:25`](../../client/src/stores/appointmentStore.ts#L25)

**Slot grid component**

- Read-only 24-slot grid with CSS state classes
  [`SlotGrid.vue:1`](../../client/src/components/SlotGrid.vue#L1)

**Tests**

- SlotServiceTest: all-free, locked, unavailable, expired
  [`SlotServiceTest.java:1`](../../server/src/test/java/com/medour/service/SlotServiceTest.java#L1)

- DoctorControllerTest: getSlots binding verification
  [`DoctorControllerTest.java:66`](../../server/src/test/java/com/medour/controller/DoctorControllerTest.java#L66)

- appointmentStore.test.ts: fetch, SSE match/ignore, disconnect
  [`stores/__tests__/appointmentStore.test.ts:1`](../../client/src/stores/__tests__/appointmentStore.test.ts#L1)
