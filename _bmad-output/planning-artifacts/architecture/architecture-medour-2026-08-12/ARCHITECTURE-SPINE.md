---
status: final
updated: 2026-08-12
project: medour
sources:
  - ../../../../specs/spec-medour/SPEC.md
---

# ARCHITECTURE-SPINE — medour

## Paradigm

**REST SPA + SSE push.** A Vue 3 SPA in `client/` consumes a Spring Boot REST API in `server/`. Server-initiated real-time events (slot state, appointment status) are delivered via SSE. The two units share one repository but are independently buildable. All state mutations go through the REST API; the SSE channel carries push notifications only.

---

## Structural Overview

```
medour/
├── client/          # Vue 3 + Vite + TypeScript SPA
│   ├── src/
│   │   ├── stores/  # Pinia stores (authStore, appointmentStore, userStore, doctorStore)
│   │   ├── views/   # Route-level components
│   │   ├── components/
│   │   ├── router/  # Vue Router + navigation guards
│   │   └── api/     # Axios instance + interceptors
│   └── vite.config.ts
└── server/          # Spring Boot Java application
    └── src/main/java/com/medour/
        ├── controller/
        ├── service/
        ├── repository/
        ├── model/
        ├── config/
        └── security/
```

---

## Architecture Decisions

### AD-1 — Monorepo, two bounded units

**Binds:** all code lives in one git repository; `client/` and `server/` are the only top-level source roots.
**Prevents:** mixing Spring components into the Vue tree or vice versa.
**Rule:** no cross-tree imports at build time; the boundary is enforced by the separate build tools (Vite / Maven).

---

### AD-2 — SSE for real-time push

**Binds:** Spring `SseEmitter` on the server; browser `EventSource` API on the client. Server fires an event on every slot state change and appointment status change.
**Prevents:** WebSocket / STOMP infrastructure in v1.
**Rule:** the SSE channel is server-to-client only. Clients never send data through it.

---

### AD-3 — JWT in localStorage, Bearer header

**Binds:** on login, the server issues a JWT (1-hour expiry); the Vue app stores it in `localStorage`; Axios injects it as `Authorization: Bearer <token>` on every request; a 401 response triggers client-side logout and redirect to `/login`.
**Prevents:** httpOnly cookie auth in v1.
**Rule:** no route may succeed server-side without a valid, non-expired token (enforced by Spring Security). `[SECURITY NOTE]` upgrade to httpOnly cookie flagged for v2.

---

### AD-4 — BE layered by technical role

**Binds:** Spring components live in `controller/`, `service/`, `repository/`, `model/`, `config/`, `security/`.
**Prevents:** feature-folder organisation.
**Rule:** controllers call services only; services call repositories; repositories extend `JpaRepository`; no controller-to-repository direct calls.

---

### AD-5 — Slot locking via `slot_reservations` table

**Binds:** a dedicated table `slot_reservations(id, doctor_id, date, start_time, reserved_by_patient_id, reserved_at, expires_at)` with a unique constraint on `(doctor_id, date, start_time)`. Slot state is derived: Available = no matching row; Locked = row exists with null appointment_id; Unavailable = confirmed appointment exists for that slot.
**Prevents:** optimistic locking on the appointment row; Redis TTL keys.
**Rule:** every slot lock/unlock operation is a single atomic DB write. On INSERT success, the service fires an SSE event to all active client connections. On DELETE (cancel or expiry), it fires a release event. The auto-cancel job also cleans expired `slot_reservations` rows.

---

### AD-6 — Soft-delete on users

**Binds:** `users.deleted_at TIMESTAMP NULL`. Null = active. Non-null = soft-deleted. No hard `DELETE` on the users table.
**Prevents:** hard delete with snapshot columns.
**Rule:** all user-facing queries filter `WHERE deleted_at IS NULL` except appointment history display, which reads the name even for soft-deleted users and renders a "Removed" badge. Login for a soft-deleted account returns the same generic invalid-credentials error as a wrong password — no distinct error revealing deletion status.

---

### AD-7 — Whereby room created at booking time

**Binds:** when an appointment is saved, the service calls the Whereby REST API to create a room and stores the returned URL in `appointments.whereby_room_url`.
**Prevents:** lazy room creation at Join click.
**Rule:** if the Whereby API call fails, the appointment save is rolled back and an error is returned to the patient. The Join button on both patient and doctor views opens `whereby_room_url` in the allowed time window only.

---

### AD-8 — Stored doctor rating, 1 decimal

**Binds:** `users.average_rating DECIMAL(3,1)`. On every patient rating insert or edit, the service recalculates `ROUND(AVG(value), 1)` from the `ratings` table for that doctor and persists it.
**Prevents:** AVG computed at query time; 2+ decimal precision.
**Rule:** the `ratings` table stores one row per appointment (patient_id, appointment_id, doctor_id, value INTEGER 1–10). Only the submitting patient may insert or update their own row.

---

### AD-9 — Auto-cancel via Spring `@Scheduled`

**Binds:** a `@Scheduled(fixedRate = 60000)` job queries `WHERE status = 'OPEN' AND scheduled_start + INTERVAL '10 minutes' < NOW()`, sets status to `AUTO_CANCELED`, and fires an SSE event per affected appointment.
**Prevents:** Quartz / DB-backed scheduler in v1.
**Rule:** single-instance deployment assumed. Multi-instance scheduling (idempotency, distributed lock) is deferred.

---

### AD-10 — Pinia stores by domain

**Binds:** `authStore` (token, current user, role), `appointmentStore` (patient and doctor appointment lists, slot state), `userStore` (admin user management), `doctorStore` (doctor search + ratings).
**Prevents:** Vuex; a single monolithic store.
**Rule:** stores do not call each other directly; cross-store data flows through route-level composables.

---

### AD-11 — Axios with interceptors

**Binds:** a single Axios instance shared across all API calls. Request interceptor: attach `Authorization: Bearer` from `authStore`. Response interceptor: on 401, call `authStore.logout()` and push to `/login`.
**Prevents:** native `fetch` API; per-request manual header injection.
**Rule:** no component makes a direct `fetch` or `XMLHttpRequest` call; all HTTP goes through the shared Axios instance.

---

### AD-12 — REST API, `/api/v1/` base path [ADOPTED]

**Binds:** all server endpoints under `/api/v1/`; JSON request and response bodies.
**Rule:** no GraphQL, no RPC in v1.

---

### AD-13 — Vue Router with role guards [ADOPTED]

**Binds:** Vue Router 4; every protected route has a navigation guard that checks `authStore.token` and `authStore.role`. Admin-only routes (`/admin/**`) redirect non-admin users to home. Unauthenticated users redirect to `/login`.
**Rule:** route guards are the FE enforcement layer; Spring Security is the BE enforcement layer. Both must enforce role restrictions independently.

---

### AD-14 — Spring Data JPA + Hibernate [ADOPTED]

**Binds:** all DB access through JPA repositories; Hibernate as ORM.
**Rule:** no raw JDBC except where JPA cannot express the query.

---

### AD-15 — Spring Security, role claims in JWT [ADOPTED]

**Binds:** Spring Security secures all `/api/**` routes. User role (`ROLE_PATIENT`, `ROLE_DOCTOR`, `ROLE_ADMIN`) is embedded in the JWT claims and re-validated server-side on every request.
**Prevents:** client-side role enforcement as the sole guard.
**Rule:** admin-only endpoints return 403 for any non-admin JWT, regardless of what the FE sent.

---

## Key Data Tables (seed)

| Table               | Key columns                                                                                                                                                                         |
| ------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `users`             | `id`, `email`, `password_hash`, `first_name`, `surname`, `role`, `deleted_at`, `average_rating`, `speciality`, `city`, `county`, `age`, `gender`, `address`, `must_change_password` |
| `appointments`      | `id`, `patient_id`, `doctor_id`, `scheduled_date`, `start_time`, `status`, `created_at`, `whereby_room_url`                                                                         |
| `slot_reservations` | `id`, `doctor_id`, `date`, `start_time`, `reserved_by_patient_id`, `reserved_at`, `expires_at`                                                                                      |
| `ratings`           | `id`, `appointment_id`, `patient_id`, `doctor_id`, `value`                                                                                                                          |

---

## Deferred

| Item                                                      | Condition to revisit                                |
| --------------------------------------------------------- | --------------------------------------------------- |
| `httpOnly` cookie auth + CSRF                             | v2 security hardening sprint                        |
| JWT refresh tokens                                        | when 1-hour hard-expiry causes UX friction          |
| Multi-instance auto-cancel (Quartz / DB-backed scheduler) | when horizontal scaling is needed                   |
| Redis-based slot locking                                  | if DB lock contention becomes measurable under load |
| Email notifications                                       | when notification feature is scoped                 |
| Whereby API error retry / fallback                        | if Whereby availability SLA requires it             |
