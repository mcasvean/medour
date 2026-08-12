# Epic 1 Context: Foundation & Authentication

<!-- Compiled from planning artifacts. Edit freely. Regenerate with compile-epic-context if planning docs change. -->

## Goal

Bootstrap the monorepo with a runnable client/server pair, then layer in the full authentication and authorization surface: role-specific registration, JWT login, route guards, profile editing, and password management (self-service and admin-reset). When this epic is done, every role can register, log in, access only their permitted routes, and manage their own credentials. The admin seed account exists on first boot.

## Stories

- Story 1.1: Project Scaffold & Development Environment
- Story 1.2: User Registration (Patient & Doctor)
- Story 1.3: User Login & JWT Session
- Story 1.4: Role Display & Route Guards
- Story 1.5: My Preferences — Account Info
- Story 1.6: Password Management

## Requirements & Constraints

- Login is the only unauthenticated screen; every other route redirects to /login without a valid session.
- Two registration forms (Patient, Doctor) share a common base field set; the doctor form adds county and speciality.
- JWT expires after 1 hour; the client auto-logs out and redirects to /login on expiry (intercepted at the Axios response level).
- User role is visible in the application header at all times.
- Burger menu items are role-conditional: Account Info + Change Password for all; Users panel for admin only.
- Admin-only routes are guarded server-side by Spring Security (returns 403) AND client-side by Vue Router navigation guards — both must enforce independently.
- Profile fields editable via Account Info vary by role; role itself is display-only for patient/doctor, changeable only by admin.
- Password self-change requires the current password as verification.
- Admin can set a temporary password (must_change_password flag); the user must complete forced rotation before any other action.
- Admin can never view any user's actual password; no plaintext passwords in API responses.
- A default admin account is seeded from application.yml credentials at startup.
- Passwords stored as bcrypt hashes only.
- Soft-delete (deleted_at) applies from the start; login for a soft-deleted account returns the same generic error as wrong credentials.

## Technical Decisions

**Monorepo layout:** `client/` is Vue 3 + Vite + TypeScript; `server/` is Spring Boot Java. The two units share one repo but are built independently (Vite / Maven). No cross-tree imports.

**JWT flow:** Server issues JWT on login; client stores it in `localStorage`; Axios request interceptor injects `Authorization: Bearer <token>` on every request; Axios response interceptor catches 401, clears localStorage, and redirects to `/login`.

**BE layering:** `controller/ → service/ → repository/`. Controllers call services only; services call repositories; repositories extend `JpaRepository`. No controller-to-repository direct calls.

**Pinia stores (skeleton in 1.1, populated in later stories):** `authStore`, `appointmentStore`, `userStore`, `doctorStore`.

**Vue Router 4** with navigation guards enforcing role access on the client. Admin route `/admin/users` redirects non-admins to home.

**Spring Security** validates the JWT role claim on every request. Admin-only endpoints return 403 for non-admin tokens.

**Users table key columns:** `email`, `password_hash` (bcrypt), `role` (PATIENT / DOCTOR / ADMIN), `deleted_at TIMESTAMP NULL`, `must_change_password BOOLEAN`.

**All REST endpoints** under `/api/v1/`.

## Cross-Story Dependencies

- **1.1 is a hard prerequisite** for all other stories — nothing can be built until the scaffold is in place and both apps run locally.
- **1.2 (registration) must precede 1.3 (login)** — login depends on accounts existing and the users table schema being final.
- **1.3 (JWT session) must precede 1.4 (route guards)** — route guards need the auth token and role stored in `authStore`.
- **1.6 (password management) depends on 1.2 and 1.3** — it builds on the users table schema and the authenticated session.
