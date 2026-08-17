---
title: "Configurations Page & User Preferences"
type: "feature"
created: "2026-08-17"
status: "done"
review_loop_iteration: 0
baseline_commit: "c187fb6d908f3c50e7543383eac608ce84697f8a"
context: []
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** User preferences (starting with sidebar pinning) are stored only in `localStorage` with no per-user persistence and no management UI. The current pin toggle is buried inside the drawer and couples state directly to `App.vue`.

**Approach:** Introduce a `user_preferences` table (one row per user, extensible columns), two new API endpoints (`GET`/`PATCH /api/v1/users/me/preferences`), a new Pinia `preferencesStore`, a `/configurations` route/view with a table of toggleable features, and refactor App.vue to drive sidebar pinning from the store instead of `localStorage`. The nav drawer loses the pin icon; the hamburger is hidden on desktop when `pinnedSidebar` is `true`.

## Boundaries & Constraints

**Always:**

- `user_preferences` table: columns `id` (PK), `user_id` (BIGINT, FK → `users.id`, UNIQUE), `pinned_sidebar` (BOOLEAN NOT NULL DEFAULT false). Hibernate `ddl-auto: update` creates it automatically — no migration file needed.
- `GET /api/v1/users/me/preferences` — lazily creates the row with all defaults if it does not yet exist; always returns 200 + `UserPreferenceDto { pinnedSidebar: boolean }`.
- `PATCH /api/v1/users/me/preferences` — accepts `UpdatePreferencesRequest { Boolean pinnedSidebar }` (not-null validated); updates and returns 200 + `UserPreferenceDto`.
- Both endpoints are authenticated; user ID obtained via `Long.parseLong(auth.getName())` (existing pattern in `UserController`).
- Frontend `preferencesStore` holds `pinnedSidebar: boolean` and exposes `fetchPreferences()` and `updatePreferences(patch)`. `fetchPreferences()` is called from `App.vue` when `authStore.isAuthenticated` becomes `true` (via `watch`).
- `App.vue` removes: `_pinnedStored` IIFE, `pinned` ref, `togglePin` function, `medour_sidebar_pinned` localStorage key, and the pin icon `VBtn` inside the drawer header. The `pinned` logic is replaced by `preferencesStore.pinnedSidebar`.
- Burger menu (`VAppBarNavIcon`) hidden on desktop (md and above) when `preferencesStore.pinnedSidebar` is `true`; always visible on mobile (sm and below) regardless of preference.
- Nav drawer gains a new `VListItem` for "Configurations" (`mdi-cog-outline`, `/configurations`) placed between "Account Info" and "Change Password".
- `ConfigurationsView.vue`: a `VTable` (or `VCard`-wrapped list) with two columns — "Functionality" and "Enabled". First row: "Pinned sidebar" + `VSwitch` (no label, inset, hide-details). Toggling immediately calls `preferencesStore.updatePreferences({ pinnedSidebar })` and shows a success toast via `toastStore`.
- Route `/configurations` is `requiresAuth`; accessible to all roles.

**Ask First:**

- Whether `PATCH /api/v1/users/me/preferences` should accept a full-replacement body or a partial-patch body. Spec assumes full-replacement (only `pinnedSidebar` exists now, so both are equivalent — decide before adding future fields).

**Never:**

- Do not add `preferences` to the `User` entity — keep preferences in a separate table for extensibility.
- Do not add a role guard to `/configurations` — it is a personal settings page for all roles.
- Do not clear `medour_sidebar_pinned` from existing users' localStorage explicitly — it is simply ignored after the refactor.
- Do not show the pin toggle icon in the drawer after this story — the Configurations page is the sole control point.

## I/O & Edge-Case Matrix

| Scenario                         | Input / State                                    | Expected Output / Behavior                                                 | Error Handling |
| -------------------------------- | ------------------------------------------------ | -------------------------------------------------------------------------- | -------------- |
| First login — no preferences row | `GET /me/preferences`                            | 200 + `{ pinnedSidebar: false }`, row created                              | —              |
| Toggle pinned sidebar ON         | `PATCH /me/preferences { pinnedSidebar: true }`  | 200 + `{ pinnedSidebar: true }`; sidebar becomes persistent; success toast | —              |
| Toggle OFF                       | `PATCH /me/preferences { pinnedSidebar: false }` | 200 + `{ pinnedSidebar: false }`; sidebar returns to temporary             | —              |
| Desktop user, pinnedSidebar true | Authenticated, md+ viewport                      | Hamburger hidden; drawer always open                                       | —              |
| Mobile user, pinnedSidebar true  | Authenticated, sm viewport                       | Hamburger visible; drawer behaves as temporary                             | —              |
| PATCH fails (network)            | Axios error                                      | Error toast; switch reverts to previous value                              | —              |

</frozen-after-approval>

## Code Map

- `server/src/main/java/com/medour/entity/UserPreference.java` — NEW `@Entity @Table("user_preferences")`: `Long id`, `Long userId` (`@Column(unique=true)`), `boolean pinnedSidebar` (default false)
- `server/src/main/java/com/medour/repository/UserPreferenceRepository.java` — NEW `JpaRepository<UserPreference, Long>`; add `Optional<UserPreference> findByUserId(Long userId)`
- `server/src/main/java/com/medour/dto/UserPreferenceDto.java` — NEW record: `boolean pinnedSidebar`
- `server/src/main/java/com/medour/dto/UpdatePreferencesRequest.java` — NEW record: `@NotNull Boolean pinnedSidebar`
- `server/src/main/java/com/medour/service/UserPreferenceService.java` — NEW `@Service`; inject `UserPreferenceRepository`; `getOrCreate(Long userId)` (find-or-insert with defaults); `update(Long userId, UpdatePreferencesRequest)` → set fields, save, return dto
- `server/src/main/java/com/medour/controller/UserController.java` — **EXTEND**: add `GET /preferences` → `preferenceService.getOrCreate(userId)` and `PATCH /preferences` → `preferenceService.update(userId, req)`
- `client/src/stores/preferencesStore.ts` — NEW Pinia store; state `pinnedSidebar: boolean`; actions `fetchPreferences()` (GET), `updatePreferences(patch)` (PATCH, updates state and calls toastStore on success)
- `client/src/views/ConfigurationsView.vue` — NEW view; `VTable` with "Functionality" / "Enabled" columns; single row "Pinned sidebar" + `VSwitch` bound to `preferencesStore.pinnedSidebar`; calls `updatePreferences` on change; shows toast
- `client/src/router/index.ts` — **EXTEND**: add `{ path: '/configurations', component: ConfigurationsView, meta: { requiresAuth: true } }`
- `client/src/App.vue` — **REFACTOR**: remove `_pinnedStored`, `pinned`, `togglePin`, pin `VBtn` in drawer; add `preferencesStore`; replace `pinned` with `preferencesStore.pinnedSidebar`; add `watch(authStore, ...)` to call `fetchPreferences` on login; add hamburger `v-show` condition; add Configurations `VListItem`

## Tasks & Acceptance

**Execution:**

- [x] `server/src/main/java/com/medour/entity/UserPreference.java` — NEW entity with id, userId (unique), pinnedSidebar (default false)
- [x] `server/src/main/java/com/medour/repository/UserPreferenceRepository.java` — NEW JpaRepository with `findByUserId`
- [x] `server/src/main/java/com/medour/dto/UserPreferenceDto.java` — NEW record with `pinnedSidebar`
- [x] `server/src/main/java/com/medour/dto/UpdatePreferencesRequest.java` — NEW record with `@NotNull Boolean pinnedSidebar`
- [x] `server/src/main/java/com/medour/service/UserPreferenceService.java` — NEW service; `getOrCreate` (lazy insert), `update` (find-or-create + set + save)
- [x] `server/src/main/java/com/medour/controller/UserController.java` — add GET `/preferences` and PATCH `/preferences` endpoints; use `parseUserId(auth)` pattern
- [x] `client/src/stores/preferencesStore.ts` — NEW store; `fetchPreferences()` GETs and sets state; `updatePreferences(patch)` PATCHes, updates state, calls toastStore; reverts on error
- [x] `client/src/views/ConfigurationsView.vue` — NEW view; VTable with Pinned sidebar row + VSwitch; import preferencesStore
- [x] `client/src/router/index.ts` — add `/configurations` route (requiresAuth, lazy import)
- [x] `client/src/App.vue` — remove localStorage pin logic and pin icon; add `preferencesStore`; `watch(authStore.isAuthenticated)` to fetch preferences on login; replace `pinned` ref with `preferencesStore.pinnedSidebar`; hamburger `v-show`; Configurations nav item

**Acceptance Criteria:**

- Given a user logs in for the first time, a `user_preferences` row is created with `pinned_sidebar = false`; the sidebar is not pinned.
- Given a user visits `/configurations` and toggles "Pinned sidebar" ON, a PATCH is sent, the sidebar immediately becomes persistent, and a success toast appears.
- Given the PATCH fails, the switch reverts to its previous value and an error toast appears.
- Given the user logs out and logs back in, the sidebar pinning state is restored from the backend (not from localStorage).
- Given a desktop user with `pinnedSidebar = true`, the hamburger icon is not visible.
- Given a mobile user with `pinnedSidebar = true`, the hamburger icon is still visible.
- Given any authenticated role (PATIENT, DOCTOR, ADMIN), the `/configurations` route is accessible.

## Verification

**Commands:**

- `cd server && ./mvnw test` -- expected: all tests pass; no regressions
- `cd client && npm run build` -- expected: zero TypeScript errors
- `cd client && npx vitest run` -- expected: all tests pass

## Suggested Review Order

**DB + BE contract**

- New entity — table shape, nullable constraints, and lazy-create pattern
  [`UserPreference.java:1`](../../server/src/main/java/com/medour/entity/UserPreference.java#L1)

- `getOrCreate` (lazy insert on first access) and `update` (find-or-create + set)
  [`UserPreferenceService.java:20`](../../server/src/main/java/com/medour/service/UserPreferenceService.java#L20)

- Two new endpoints wired into existing `UserController`
  [`UserController.java:61`](../../server/src/main/java/com/medour/controller/UserController.java#L61)

**FE store + optimistic update**

- `fetchPreferences` (called on login) and `updatePreferences` (optimistic `$patch` + revert)
  [`preferencesStore.ts:1`](../../client/src/stores/preferencesStore.ts#L1)

**FE view**

- VTable with `:model-value` + `@update:model-value` pattern (avoids v-model direct-store mutation)
  [`ConfigurationsView.vue:1`](../../client/src/views/ConfigurationsView.vue#L1)

**App.vue wiring**

- Hamburger `v-show`, drawer `:temporary`, and `watch(isAuthenticated)` that fetches + opens drawer
  [`App.vue:7`](../../client/src/App.vue#L7)

- Watcher that syncs drawer open/close state when `pinnedSidebar` changes
  [`App.vue:188`](../../client/src/App.vue#L188)

**Routing**

- `/configurations` route added (requiresAuth, lazy-loaded)
  [`router/index.ts:70`](../../client/src/router/index.ts#L70)

## Design Notes

The `preferencesStore.updatePreferences(patch)` action must optimistically update the local state BEFORE the API call, then revert on error — this keeps the VSwitch responsive. Pattern: `const prev = pinnedSidebar; pinnedSidebar = patch.value; try { await api.patch(...) } catch { pinnedSidebar = prev; toastStore.showError(...) }`.

For the Configurations view, wrap the table in a `VContainer` + `VCard` consistent with other views. The table header row uses `text-caption text-medium-emphasis` styling; the switch column uses `justify="center"`.
