<!-- bmad:context -->
<!-- Verified 2026-08-12, greenfield — no code committed yet. Managed by bmad-project-context; refresh after first code is committed. -->

## medour

Online medical appointment booking platform. Monorepo: `client/` is Vue 3 + Vite + TypeScript SPA; `server/` is Spring Boot + PostgreSQL REST API. Planning artifacts live in `_bmad-output/`. Architecture decisions in `_bmad-output/planning-artifacts/architecture/architecture-medour-2026-08-12/ARCHITECTURE-SPINE.md`.

## Policy

- `main` is protected — never delete it; direct push without PR is permitted.
- Never compromise security or break a working build. Before touching auth, role guards, or JWT handling, re-read AD-3 and AD-15 in the architecture spine.

## Where things are

- Capabilities contract (SPEC): `_bmad-output/specs/spec-medour/SPEC.md`
- All binding architecture decisions: `_bmad-output/planning-artifacts/architecture/architecture-medour-2026-08-12/ARCHITECTURE-SPINE.md`
- PRD: `_bmad-output/planning-artifacts/prds/prd-bmad-med-2026-08-11/prd.md`

## Running and verifying

TODO — no code committed yet; verify and replace on first refresh.

- Dev client (Vite): `cd client && npm run dev`
- Dev server (Spring Boot): `cd server && ./mvnw spring-boot:run`
- Build client: `cd client && npm run build`

## Conventions that differ from defaults

**TypeScript (client/):**

- `const` and `let` only — never `var`.
- `===` for all equality checks — avoid `==`.
- Trailing commas on multi-line arrays, objects, and parameter lists.
- `camelCase` for all variable, function, and parameter names. No single-letter or abstract names (`x`, `val`, `data`) — name what the value represents.
- Avoid `any`; prefer explicit types or `unknown` with a type guard.
- Short inline comment on any non-obvious logic block — one line maximum.
- All HTTP calls through the shared Axios instance in `client/src/api/` — never use `fetch` or `XMLHttpRequest` directly.
- All shared state through Pinia stores in `client/src/stores/` — never use component-local state for cross-component data.

**Java (server/):**

- Controllers call services only; services call repositories — no controller → repository direct calls.
- Users are never hard-deleted — soft-delete via `deleted_at`. Never write a hard-delete on the users table.
- `@Transactional` on all service methods that write to the database.
- All DB access through JPA repositories extending `JpaRepository`.

## Known pitfalls

- Slot locking uses the `slot_reservations` table unique constraint — never rely on application-level checks alone; the DB constraint is the source of truth.
- SSE must fire on every `slot_reservations` INSERT and DELETE, not only on appointment save.
- Admin-only routes are guarded by both Vue Router navigation guards and Spring Security — both must enforce independently; removing either leaves the route exposed.
- Login for a soft-deleted user must return the same generic error as a wrong password — never expose deletion status through a distinct error message or code.

<!-- /bmad:context -->
