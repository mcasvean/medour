---
title: "Appointment Confirmation & Whereby Room Creation"
type: "feature"
created: "2026-08-13"
status: "done"
review_loop_iteration: 0
baseline_commit: "85cac87e52585eb0019c5a5096821063d22e7904"
context: []
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Appointments created in Story 2.3 have no Whereby video room — `wherebyRoomUrl` is always null, making video consultations impossible.

**Approach:** Add a `WherebyService` that calls the Whereby REST API (`POST /v1/meetings`) to create a room; inject it into `AppointmentService.createAppointment()` so the room is created before the appointment is saved; if the API call fails, the entire appointment transaction rolls back. Add `wherebyRoomUrl` to `AppointmentCreatedResponse`.

## Boundaries & Constraints

**Always:**

- The Whereby call happens **inside** the `@Transactional` method, before `appointmentRepository.save()`. If it throws, `@Transactional` rolls back — no orphaned appointment rows
- `whereby.api-url` and `whereby.api-key` are externalised to `application.yml` via `@Value("${whereby.api-url}")` / `@Value("${whereby.api-key:}")`
- If `whereby.api-key` is blank (local dev with no key), `WherebyService.createRoom()` returns a generated placeholder URL (`https://whereby.com/dev-room-<UUID>`) instead of calling the API; this allows bookings to succeed in dev without a Whereby account
- If the Whereby API call fails (non-empty key but API error), throw `WherebyException` which is caught by `GlobalExceptionHandler` → 502 `{ "error": "Video room creation failed" }`
- `AppointmentCreatedResponse` gains `String wherebyRoomUrl` as a new component
- Use Spring `RestTemplateBuilder` to construct the `RestTemplate` so tests can inject a mock
- The `roomMode` sent to Whereby is `"group_hd"` per the architecture; `endDate` is the scheduled date + 1 day (ISO-8601 UTC)

**Ask First:**

- (none)

**Never:**

- Do not store the Whereby API key in source control; always use `${WHEREBY_API_KEY:}` with empty default
- Whereby failure must not silently produce an appointment without a room URL — either a valid URL or a rollback

## I/O & Edge-Case Matrix

| Scenario                                     | Input / State                                   | Expected Output / Behavior                                            | Error Handling             |
| -------------------------------------------- | ----------------------------------------------- | --------------------------------------------------------------------- | -------------------------- |
| Valid Whereby key, API succeeds              | `POST /appointments`; Whereby returns `roomUrl` | 201 + `AppointmentCreatedResponse` with `wherebyRoomUrl`              | N/A                        |
| Whereby API fails (non-2xx or network error) | `POST /appointments`; Whereby throws            | 502 `{ "error": "Video room creation failed" }`; no appointment saved | Rollback; WherebyException |
| No Whereby key (local dev)                   | `whereby.api-key` is blank                      | 201 + placeholder URL `https://whereby.com/dev-room-<UUID>`           | N/A                        |

</frozen-after-approval>

## Code Map

- `server/src/main/resources/application.yml` -- add `whereby.api-url` and `whereby.api-key` properties
- `server/src/main/java/com/medour/exception/WherebyException.java` -- NEW RuntimeException → 502
- `server/src/main/java/com/medour/exception/GlobalExceptionHandler.java` -- add `@ExceptionHandler(WherebyException.class)` → 502 `{ "error": "Video room creation failed" }`
- `server/src/main/java/com/medour/service/WherebyService.java` -- NEW `@Service`; inject `RestTemplateBuilder`; `@Value` for api-url and api-key; `createRoom(LocalDate scheduledDate)` → returns String roomUrl; if api-key blank → placeholder URL; else call Whereby API
- `server/src/main/java/com/medour/service/AppointmentService.java:83` -- inject `WherebyService`; call `wherebyService.createRoom(reservation.getDate())` before saving appointment; set `appointment.setWherebyRoomUrl(roomUrl)` in the builder
- `server/src/main/java/com/medour/dto/AppointmentCreatedResponse.java` -- add `String wherebyRoomUrl` component
- `server/src/test/java/com/medour/service/WherebyServiceTest.java` -- NEW: test dev-mode (blank key → placeholder URL); test API success path (mock RestTemplate returns roomUrl); test API failure path (mock RestTemplate throws → WherebyException)
- `server/src/test/java/com/medour/service/AppointmentServiceTest.java` -- add: Whereby failure → appointment NOT saved (verify `appointmentRepository.save` not called)

## Tasks & Acceptance

**Execution:**

- [ ] `server/src/main/resources/application.yml` -- add: `whereby.api-url: ${WHEREBY_API_URL:https://api.whereby.dev/v1/meetings}` and `whereby.api-key: ${WHEREBY_API_KEY:}`
- [ ] `server/src/main/java/com/medour/exception/WherebyException.java` -- NEW `public class WherebyException extends RuntimeException { public WherebyException(String msg, Throwable cause) { super(msg, cause); } }`
- [ ] `server/src/main/java/com/medour/exception/GlobalExceptionHandler.java` -- add handler: `@ExceptionHandler(WherebyException.class)` → `ResponseEntity.status(502).body(Map.of("error", "Video room creation failed"))`
- [ ] `server/src/main/java/com/medour/service/WherebyService.java` -- NEW `@Service`; constructor injects `RestTemplateBuilder` and builds `this.restTemplate = builder.build()`; fields `@Value("${whereby.api-url}") String apiUrl` and `@Value("${whereby.api-key:}") String apiKey`; `createRoom(LocalDate scheduledDate)`: if `apiKey.isBlank()` return `"https://whereby.com/dev-room-" + UUID.randomUUID()`; else: set headers (`Authorization: Bearer <apiKey>`, `Content-Type: application/json`), body (`roomMode: "group_hd"`, `endDate: scheduledDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toString()`), call `restTemplate.postForEntity(apiUrl, request, Map.class)`, extract `response.getBody().get("roomUrl")` as String; on any exception throw `new WherebyException("...", e)`
- [ ] `server/src/main/java/com/medour/service/AppointmentService.java` -- add `WherebyService wherebyService` to constructor; in `createAppointment()` call `String roomUrl = wherebyService.createRoom(reservation.getDate())` after the expiry check and before building the `Appointment`; set `.wherebyRoomUrl(roomUrl)` in the builder
- [ ] `server/src/main/java/com/medour/dto/AppointmentCreatedResponse.java` -- add `String wherebyRoomUrl` as last record component; update all construction sites in `AppointmentService.createAppointment()`
- [ ] `server/src/test/java/com/medour/service/WherebyServiceTest.java` -- NEW `@ExtendWith(MockitoExtension.class)`; mock `RestTemplate`; inject via reflection or provide factory; test: (1) blank key → returns URL starting with `https://whereby.com/dev-room-`; (2) non-blank key, successful API → returns roomUrl from mocked response; (3) non-blank key, API throws `RestClientException` → `WherebyException` thrown
- [ ] `server/src/test/java/com/medour/service/AppointmentServiceTest.java` -- add test: stub `wherebyService.createRoom()` to throw `WherebyException`; call `createAppointment()`; assert `WherebyException` thrown AND `appointmentRepository.save()` never called

**Acceptance Criteria:**

- Given a valid Whereby API key and successful API call, when a patient confirms a booking, then the appointment is saved with a non-null `wherebyRoomUrl` and the response includes it.
- Given the Whereby API returns an error, when a patient confirms a booking, then a 502 is returned and no appointment row is created.
- Given `whereby.api-key` is blank (local dev), when a patient confirms a booking, then a placeholder Whereby URL is generated and the appointment is saved normally.

## Verification

**Commands:**

- `cd server && ./mvnw test` -- expected: 44 tests pass (40 existing + 3 WherebyServiceTest + 1 AppointmentServiceTest)
- `cd client && npm run build` -- expected: zero TypeScript errors (AppointmentCreatedResponse gains wherebyRoomUrl)

## Spec Change Log

**Review loop 1 patches applied:**

- `WherebyService.createRoom()` — added null check on response body / `roomUrl` field; throws `WherebyException("Missing roomUrl in Whereby response")` if absent, preventing null from being persisted
- `AppointmentControllerTest.confirm_validReservation_returns201` — added `$.wherebyRoomUrl` JSON path assertion
- `AppointmentControllerTest` — added `confirm_wherebyFails_returns502` test verifying 502 + `{ "error": "Video room creation failed" }` from `GlobalExceptionHandler`
- `appointmentStore` — added `wherebyRoomUrl: null` state; `confirmBooking()` now reads and stores `response.data.wherebyRoomUrl`
- `BookingSearchView.vue` — done-step panel shows the Whereby room URL as a link

## Suggested Review Order

**Whereby integration (entry point)**

- Dev-mode placeholder vs live API call; null roomUrl guard; any exception → WherebyException
  [`WherebyService.java:39`](../../server/src/main/java/com/medour/service/WherebyService.java#L39)

**AppointmentService — Whereby wired in**

- createRoom() called before appointmentRepository.save(); rolled back if WherebyException thrown
  [`AppointmentService.java:97`](../../server/src/main/java/com/medour/service/AppointmentService.java#L97)

**Error mapping**

- WherebyException → 502 Bad Gateway via GlobalExceptionHandler
  [`GlobalExceptionHandler.java:39`](../../server/src/main/java/com/medour/exception/GlobalExceptionHandler.java#L39)

**Response & client**

- AppointmentCreatedResponse gains wherebyRoomUrl field
  [`AppointmentCreatedResponse.java:1`](../../server/src/main/java/com/medour/dto/AppointmentCreatedResponse.java#L1)

- appointmentStore.wherebyRoomUrl stored on confirm; displayed as link in done-step
  [`appointmentStore.ts:108`](../../client/src/stores/appointmentStore.ts#L108)

**Tests**

- WherebyServiceTest: blank key, live key, API error
  [`WherebyServiceTest.java:1`](../../server/src/test/java/com/medour/service/WherebyServiceTest.java#L1)

- AppointmentControllerTest: $.wherebyRoomUrl asserted; 502 path tested
  [`AppointmentControllerTest.java:72`](../../server/src/test/java/com/medour/controller/AppointmentControllerTest.java#L72)

- AppointmentServiceTest: Whereby throws → save never called
  [`AppointmentServiceTest.java:78`](../../server/src/test/java/com/medour/service/AppointmentServiceTest.java#L78)
