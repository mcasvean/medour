---
title: "Doctor Search & Filter"
type: "feature"
created: "2026-08-13"
status: "done"
review_loop_iteration: 0
baseline_commit: "7394088baa58633dffe131b9015003c7aeae5590"
context: []
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Patients have no way to discover doctors or available slots. The `doctorStore` is empty, there are no appointment or slot-reservation entities, and there is no booking entry point in the UI.

**Approach:** Introduce the `Appointment` and `SlotReservation` JPA entities (establishing the DB schema for Epic 2); add `users.average_rating` to the `User` entity; implement `GET /api/v1/doctors` with optional filters (speciality, county, city, date); build the `BookingSearchView` at `/booking` where a patient can enter filters and see a list of matching doctors with their profile details.

## Boundaries & Constraints

**Always:**
- Only PATIENT role can access `/booking` — the router guard redirects DOCTOR and ADMIN to `/`
- `GET /api/v1/doctors` returns doctors where `role = DOCTOR` AND `deleted_at IS NULL`
- If a `date` query param is provided, only return doctors that have at least one slot not taken by a `slot_reservations` row or a confirmed `appointments` row on that date (24 fixed slots, 08:00–20:00, 30-minute intervals)
- If no filters are applied, all active doctors are returned
- `average_rating` is `null` until Epic 5 writes it; the DTO and UI handle null gracefully
- `Appointment` entity and `SlotReservation` entity are created in this story; they are used by later stories — their schema must match the architecture spine exactly
- The `unique constraint` on `slot_reservations(doctor_id, date, start_time)` must be declared via `@Table(uniqueConstraints = ...)` on the entity
- `doctorStore` holds the filters and search results; `BookingSearchView` reads and writes through it

**Ask First:**
- (none)

**Never:**
- No slot grid in this story (Story 2.2)
- No booking flow in this story (Story 2.3)
- No SSE in this story (Story 2.2)
- Doctors and admins cannot access the booking flow

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|---|---|---|---|
| No filters | `GET /api/v1/doctors` | All active doctors returned | N/A |
| Speciality filter | `GET /api/v1/doctors?speciality=Cardiology` | Only DOCTOR users with that speciality | N/A |
| Date filter | `GET /api/v1/doctors?date=2026-09-01` | Doctors with ≥1 free slot on that date | N/A |
| Date + speciality | Combined params | Intersection of both filters | N/A |
| No matching doctors | All filters applied; no match | Empty list `[]`; client shows empty-state message | N/A |
| Non-patient accesses `/booking` | DOCTOR or ADMIN role | Router guard redirects to `/` | N/A |

</frozen-after-approval>

## Code Map

- `server/src/main/java/com/medour/model/AppointmentStatus.java` -- NEW enum: OPEN, COMPLETED, CANCELED, AUTO_CANCELED
- `server/src/main/java/com/medour/model/Appointment.java` -- NEW `@Entity @Table(name="appointments")`; FK to patient+doctor (ManyToOne User); scheduledDate, startTime, status, createdAt (auto-set), wherebyRoomUrl
- `server/src/main/java/com/medour/model/SlotReservation.java` -- NEW `@Entity @Table(name="slot_reservations", uniqueConstraints=@UniqueConstraint(columnNames={"doctor_id","date","start_time"}))`; doctor (FK User), date, startTime, reservedByPatient (FK User), reservedAt, expiresAt
- `server/src/main/java/com/medour/repository/AppointmentRepository.java` -- NEW; `countByDoctorIdAndScheduledDateAndStatus*` query methods
- `server/src/main/java/com/medour/repository/SlotReservationRepository.java` -- NEW; `countByDoctorIdAndDate` method
- `server/src/main/java/com/medour/model/User.java:55` -- add `BigDecimal averageRating` nullable field (`@Column(name="average_rating", precision=3, scale=1)`)
- `server/src/main/java/com/medour/dto/DoctorSearchResult.java` -- NEW record: id, firstName, surname, speciality, county, city, BigDecimal averageRating (nullable)
- `server/src/main/java/com/medour/service/DoctorService.java` -- NEW; `@Transactional(readOnly=true) searchDoctors(String speciality, String county, String city, LocalDate date)` — query + optional date-availability filter
- `server/src/main/java/com/medour/controller/DoctorController.java` -- NEW `@RestController("/api/v1/doctors")`; `GET /` with optional `@RequestParam` filters → list of DoctorSearchResult
- `server/src/test/java/com/medour/controller/DoctorControllerTest.java` -- NEW `@WebMvcTest(DoctorController.class)` with `@MockBean DoctorService` and `@MockBean JwtUtil`; 2 tests: no-filter → 200 + list; with speciality filter → 200 + filtered
- `client/src/stores/doctorStore.ts` -- populate: `doctors` list, `filters` object, `loading`, `searchDoctors()` action calling `GET /api/v1/doctors`
- `client/src/router/index.ts` -- add `requiresPatient?: boolean` to RouteMeta; add guard check `if (to.meta.requiresPatient && auth.user?.role !== 'PATIENT') return '/'`; add `/booking` route with `{ requiresAuth: true, requiresPatient: true }`
- `client/src/views/BookingSearchView.vue` -- NEW: filter form (speciality, county, city, date); submit calls `doctorStore.searchDoctors()`; renders doctor cards from `doctorStore.doctors`; empty-state message when list is empty

## Tasks & Acceptance

**Execution:**

- [ ] `server/src/main/java/com/medour/model/AppointmentStatus.java` -- NEW enum with 4 values
- [ ] `server/src/main/java/com/medour/model/Appointment.java` -- NEW entity; `@ManyToOne @JoinColumn(name="patient_id") User patient`; `@ManyToOne @JoinColumn(name="doctor_id") User doctor`; `LocalDate scheduledDate`; `LocalTime startTime`; `@Enumerated(STRING) AppointmentStatus status`; `@CreationTimestamp LocalDateTime createdAt`; `String wherebyRoomUrl`; Lombok `@Data @Builder @NoArgsConstructor @AllArgsConstructor`
- [ ] `server/src/main/java/com/medour/model/SlotReservation.java` -- NEW entity; `@Table(name="slot_reservations", uniqueConstraints=@UniqueConstraint(columnNames={"doctor_id","date","start_time"}))`; `@ManyToOne @JoinColumn(name="doctor_id") User doctor`; `LocalDate date`; `LocalTime startTime @Column(name="start_time")`; `@ManyToOne @JoinColumn(name="reserved_by_patient_id") User reservedByPatient`; `LocalDateTime reservedAt`; `LocalDateTime expiresAt`; Lombok `@Data @Builder @NoArgsConstructor @AllArgsConstructor`
- [ ] `server/src/main/java/com/medour/repository/AppointmentRepository.java` -- NEW; `long countByDoctorIdAndScheduledDateAndStatusIn(Long doctorId, LocalDate date, List<AppointmentStatus> statuses)`
- [ ] `server/src/main/java/com/medour/repository/SlotReservationRepository.java` -- NEW; `long countByDoctorIdAndDate(Long doctorId, LocalDate date)`
- [ ] `server/src/main/java/com/medour/model/User.java` -- add `@Column(name="average_rating", precision=3, scale=1) BigDecimal averageRating` field (nullable, no default)
- [ ] `server/src/main/java/com/medour/dto/DoctorSearchResult.java` -- NEW record: `Long id, String firstName, String surname, String speciality, String county, String city, BigDecimal averageRating`
- [ ] `server/src/main/java/com/medour/service/DoctorService.java` -- NEW `@Service`; inject `UserRepository`, `AppointmentRepository`, `SlotReservationRepository`; `searchDoctors(String speciality, String county, String city, LocalDate date)`: query `userRepository.findAll()` filtered where `role=DOCTOR` and `deletedAt=null`; apply speciality/county/city filters (case-insensitive contains, skip if param is blank/null); if `date` not null: for each doctor, compute `takenSlots = countByDoctorIdAndDate(reservations) + countByDoctorIdAndScheduledDateAndStatusIn(appointments, [OPEN, CANCELED, AUTO_CANCELED excluded — count only OPEN])` — actually: taken = reservations + appointments with status OPEN; keep only doctors where takenSlots < 24; map to `DoctorSearchResult`
- [ ] `server/src/main/java/com/medour/controller/DoctorController.java` -- NEW `@RestController @RequestMapping("/api/v1/doctors")`; `@GetMapping("/") getAll(@RequestParam Optional<String> speciality, @RequestParam Optional<String> county, @RequestParam Optional<String> city, @RequestParam Optional<LocalDate> date)` → `ResponseEntity.ok(doctorService.searchDoctors(...))`
- [ ] `server/src/test/java/com/medour/controller/DoctorControllerTest.java` -- NEW; `@WebMvcTest(DoctorController.class) @AutoConfigureMockMvc(addFilters=false)`; `@MockBean DoctorService`; `@MockBean JwtUtil`; tests: (1) `GET /api/v1/doctors/` no params → 200 + non-empty list; (2) `GET /api/v1/doctors/?speciality=Cardiology` → 200 + asserts one doctor result with matching speciality
- [ ] `client/src/stores/doctorStore.ts` -- rewrite: `interface DoctorSearchResult { id, firstName, surname, speciality, county, city, averageRating: number | null }`; state: `{ doctors: DoctorSearchResult[], loading: boolean, filters: { speciality: string, county: string, city: string, date: string } }`; action `searchDoctors()`: call `api.get('/doctors/', { params: nonEmpty(filters) })`, set `doctors`
- [ ] `client/src/router/index.ts` -- add `requiresPatient?: boolean` to `RouteMeta`; in `setupGuard` add `if (to.meta.requiresPatient && auth.user?.role !== 'PATIENT') return '/'`; add `{ path: '/booking', component: () => import('../views/BookingSearchView.vue'), meta: { requiresAuth: true, requiresPatient: true } }`
- [ ] `client/src/views/BookingSearchView.vue` -- NEW: inputs for speciality, county, city, date bound to `doctorStore.filters`; Search button calls `doctorStore.searchDoctors()`; calls `searchDoctors()` on `onMounted`; renders doctor cards showing name, speciality, county, city, average rating (or "No rating" if null); shows "No doctors found" when list is empty

**Acceptance Criteria:**

- Given a DOCTOR or ADMIN user, when `/booking` is navigated to, the router guard redirects them to `/`.
- Given a PATIENT on `/booking`, when the page loads, all active doctors are shown by default.
- Given the speciality filter is set to "Cardiology", when Search is clicked, only doctors with that speciality are returned.
- Given a date filter is set and a doctor has all 24 slots taken (reserved or confirmed), when the search runs, that doctor is not included in results.
- Given filters that match no doctors, when the search runs, an empty-state message is shown.

## Verification

**Commands:**
- `cd server && ./mvnw test` -- expected: 26 tests pass (24 existing + 2 new DoctorControllerTest)
- `cd client && npm run test` -- expected: all 14 tests pass; router guard test for `/booking` with PATIENT passes; router guard for non-patient redirects passes
- `cd client && npm run build` -- expected: zero TypeScript errors

## Spec Change Log

**Review loop 1 patches applied:**
- `SlotReservationRepository` — `countByDoctorIdAndDate` replaced by `countByDoctorIdAndDateAndExpiresAtAfter` so expired reservations are not counted as active slots
- `DoctorService.searchDoctors()` — date filter now counts `OPEN + COMPLETED` appointments (COMPLETED slots are occupied); uses non-expired reservation count; added `LocalDateTime` import
- `DoctorControllerTest` — speciality-filter test now uses `eq("Cardiology"), isNull(), isNull(), isNull()` instead of unconstrained `any()` matchers, verifying param forwarding
- `BookingSearchView.vue` — date input gets `:min="today"` to prevent past-date searches
- `DoctorServiceTest.java` — NEW: 3 unit tests covering active-doctor filtering, speciality substring match, and date-availability exclusion of fully-booked doctor

## Suggested Review Order

**Search service — core filter logic**

- Role/deletedAt guard + speciality/county/city case-insensitive contains + date availability threshold
  [`DoctorService.java:33`](../../server/src/main/java/com/medour/service/DoctorService.java#L33)

- Date filter: non-expired reservations + OPEN/COMPLETED appointments < 24 slots
  [`DoctorService.java:40`](../../server/src/main/java/com/medour/service/DoctorService.java#L40)

**New entities (DB schema for Epic 2)**

- SlotReservation with @UniqueConstraint on (doctor_id, date, start_time)
  [`SlotReservation.java:1`](../../server/src/main/java/com/medour/model/SlotReservation.java#L1)

- Appointment entity: FKs, LocalDate/LocalTime, status enum, @CreationTimestamp
  [`Appointment.java:1`](../../server/src/main/java/com/medour/model/Appointment.java#L1)

**Repository query methods**

- countByDoctorIdAndDateAndExpiresAtAfter — only non-expired reservations counted
  [`SlotReservationRepository.java:10`](../../server/src/main/java/com/medour/repository/SlotReservationRepository.java#L10)

- countByDoctorIdAndScheduledDateAndStatusIn
  [`AppointmentRepository.java:1`](../../server/src/main/java/com/medour/repository/AppointmentRepository.java#L1)

**Controller & Router**

- Controller query param binding; constrained test verifies speciality forwarding
  [`DoctorController.java:24`](../../server/src/main/java/com/medour/controller/DoctorController.java#L24)

- requiresPatient guard; /booking route
  [`router/index.ts:55`](../../client/src/router/index.ts#L55)

**Client search page**

- Filter form with :min date; doctor cards; empty-state
  [`BookingSearchView.vue:1`](../../client/src/views/BookingSearchView.vue#L1)

**Tests**

- DoctorServiceTest: active-only, speciality match, date-availability exclusion
  [`DoctorServiceTest.java:1`](../../server/src/test/java/com/medour/service/DoctorServiceTest.java#L1)

- Router guard: PATIENT ok, DOCTOR/ADMIN redirected to /
  [`router/__tests__/index.test.ts:70`](../../client/src/router/__tests__/index.test.ts#L70)
