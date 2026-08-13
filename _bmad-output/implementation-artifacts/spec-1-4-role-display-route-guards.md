---
title: "Role Display & Route Guards"
type: "feature"
created: "2026-08-13"
status: "done"
review_loop_iteration: 0
baseline_commit: "e115ffc9fa6c02178fb03db41754ad1f7e4b6d50"
context: []
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** After login the user sees no indication of who they are or their role, any URL is reachable regardless of auth state or role, and the server returns Spring's default HTML body for 403 responses instead of the app's JSON format.

**Approach:** Add a persistent app header to `App.vue` showing the user's name and role with a burger-menu of role-conditional navigation links; wire a global Vue Router `beforeEach` guard enforcing authentication and admin-role requirements; add a `/admin/users` stub route restricted to admins; and tighten `SecurityConfig` to restrict `/api/v1/admin/**` to ADMIN role with a JSON `accessDeniedHandler`.

## Boundaries & Constraints

**Always:**

- Header renders only when `authStore.isAuthenticated` is true; `/login` and `/register` show no header
- Navigation guard is a single global `router.beforeEach`; no per-component guard
- Unauthenticated access to any `requiresAuth` route → redirect to `/login`
- Authenticated access to a `guestOnly` route (`/login`, `/register`) → redirect to `/`
- Non-admin access to any `requiresAdmin` route → redirect to `/`
- Both Vue Router guard AND Spring Security `hasRole("ADMIN")` must enforce admin routes independently — removing either leaves the route exposed (architecture invariant)
- `/api/v1/admin/**` returns `{ "error": "Forbidden" }` with 403 JSON, not Spring's default HTML page
- `/admin/users` is a stub page in this story — content is added in Epic 4
- Logout calls `authStore.clearAuth()` then `router.push('/login')`
- Burger menu items: Account Info + Change Password (all authenticated roles); Users (admin only); Logout (all)
- `/account` and `/change-password` routes are NOT created in this story — links appear but routes are added in Stories 1.5 and 1.6

**Ask First:**

- (none)

**Never:**

- No server-side role check added to non-admin endpoints in this story
- No per-route component-level navigation guards — global guard only

## I/O & Edge-Case Matrix

| Scenario                                | Input / State                                        | Expected Output / Behavior                                             | Error Handling |
| --------------------------------------- | ---------------------------------------------------- | ---------------------------------------------------------------------- | -------------- |
| Unauthenticated → protected route       | No token; navigate to `/`                            | Guard redirects to `/login`                                            | N/A            |
| Authenticated → guest-only route        | Valid token; navigate to `/login`                    | Guard redirects to `/`                                                 | N/A            |
| PATIENT → `/admin/users`                | Authenticated as PATIENT; navigate to `/admin/users` | Guard redirects to `/`                                                 | N/A            |
| ADMIN → `/admin/users`                  | Authenticated as ADMIN; navigate to `/admin/users`   | Admin stub page renders                                                | N/A            |
| Server admin endpoint — non-admin token | PATIENT JWT hits `GET /api/v1/admin/**`              | 403 `{ "error": "Forbidden" }`                                         | N/A            |
| Logout                                  | Authenticated user clicks Logout                     | `clearAuth()` called; all localStorage cleared; redirected to `/login` | N/A            |

</frozen-after-approval>

## Code Map

- `server/src/main/java/com/medour/config/SecurityConfig.java:40` -- add `requestMatchers("/api/v1/admin/**").hasRole("ADMIN")` before `anyRequest().authenticated()`; extend `exceptionHandling` with `accessDeniedHandler` returning JSON 403
- `client/src/router/index.ts:1` -- export `routes` array; add `meta` to existing routes; add `/admin/users` route; add `router.beforeEach` guard using `useAuthStore`
- `client/src/views/AdminUsersView.vue` -- NEW stub component (placeholder for Epic 4)
- `client/src/App.vue:1` -- rewrite: header with user name + role when authenticated; burger toggle `<nav>` with role-conditional links; logout action; `<RouterView />` below
- `client/src/stores/authStore.ts:8` -- `User.role` is `string`; guard compares `=== 'ADMIN'`; no change needed
- `client/src/router/__tests__/index.test.ts` -- NEW: 4 guard behaviour tests using `createMemoryHistory()` + mocked `useAuthStore`

## Tasks & Acceptance

**Execution:**

- [x] `server/src/main/java/com/medour/config/SecurityConfig.java` -- add `.requestMatchers("/api/v1/admin/**").hasRole("ADMIN")` between the auth permitAll matchers and `.anyRequest().authenticated()`; extend `exceptionHandling` to also set `accessDeniedHandler((req, res, ex) -> { res.setContentType("application/json"); res.setStatus(SC_FORBIDDEN); res.getWriter().write("{\"error\":\"Forbidden\"}"); })`; add `SC_FORBIDDEN` to static imports
- [x] `client/src/router/index.ts` -- export `routes` as a named `const routes` array; add `meta: { requiresAuth: true }` to `/`; add `meta: { guestOnly: true }` to `/login` and `/register`; add `{ path: '/admin/users', component: () => import('../views/AdminUsersView.vue'), meta: { requiresAuth: true, requiresAdmin: true } }`; add `router.beforeEach((to) => { const auth = useAuthStore(); if (to.meta.requiresAuth && !auth.isAuthenticated) return '/login'; if (to.meta.requiresAdmin && auth.user?.role !== 'ADMIN') return '/'; if (to.meta.guestOnly && auth.isAuthenticated) return '/'; })`; import `useAuthStore`
- [x] `client/src/views/AdminUsersView.vue` -- NEW: `<template><div>Admin: Users</div></template>`
- [x] `client/src/App.vue` -- rewrite template to: `<header v-if="authStore.isAuthenticated">` containing user's `firstName` + role text + `<button @click="menuOpen = !menuOpen">☰</button>`; `<nav v-show="menuOpen">` with `<RouterLink to="/account">Account Info</RouterLink>`, `<RouterLink to="/change-password">Change Password</RouterLink>`, `<RouterLink v-if="isAdmin" to="/admin/users">Users</RouterLink>`, `<button @click="logout">Logout</button>`; `</header>`; `<RouterView />`; script: `useAuthStore()`, `useRouter()`; `isAdmin` computed = `authStore.user?.role === 'ADMIN'`; `menuOpen` ref; `logout()` calls `authStore.clearAuth()` then `router.push('/login')`
- [x] `client/src/router/__tests__/index.test.ts` -- NEW: `vi.mock('../stores/authStore', ...)` returning configurable `isAuthenticated`/`user`; build `testRouter` with `createRouter({ history: createMemoryHistory(), routes })`; test: (1) no-token push to `/` → current path is `/login`; (2) authenticated push to `/login` → current path is `/`; (3) PATIENT push to `/admin/users` → current path is `/`; (4) ADMIN push to `/admin/users` → current path is `/admin/users`; add `setActivePinia(createPinia())` in `beforeEach`

**Acceptance Criteria:**

- Given an unauthenticated user, when any route requiring auth is navigated to, then the Vue Router guard redirects to `/login` before the page renders.
- Given an authenticated user, when `/login` or `/register` is navigated to, then the guard redirects to `/`.
- Given an authenticated PATIENT user, when `/admin/users` is navigated to, then the guard redirects to `/`, and the admin stub page never renders.
- Given an authenticated ADMIN user, when `/admin/users` is navigated to, then the admin stub page renders.
- Given an authenticated ADMIN user, when logged in, then the header shows their first name, role, and a Users nav link.
- Given an authenticated PATIENT user, when logged in, then the header shows their first name and role but no Users link.
- Given any authenticated user, when Logout is clicked, then `authStore.clearAuth()` is called and the user is redirected to `/login`.
- Given a non-admin JWT, when `GET /api/v1/admin/**` is called, then the server returns 403 with body `{ "error": "Forbidden" }`.

## Verification

**Commands:**

- `cd server && ./mvnw test` -- expected: all 13 tests pass
- `cd client && npm run test` -- expected: 4 existing API tests + 4 new router guard tests pass (8 total)
- `cd client && npm run build` -- expected: zero TypeScript errors

## Spec Change Log

**Review loop 1 patches applied:**

- `App.vue` — added `watch(() => route.path, ...)` to close burger menu on navigation; `logout()` also sets `menuOpen = false`; imported `useRoute`
- `SecurityConfig.java` — `accessDeniedHandler` now sets `charset UTF-8` and flushes the writer

## Suggested Review Order

**Security — role enforcement**

- Admin route matcher order and accessDeniedHandler JSON 403 with charset + flush
  [`SecurityConfig.java:42`](../../server/src/main/java/com/medour/config/SecurityConfig.java#L42)

**Router guard — all three redirect cases**

- beforeEach: requiresAuth → /login; requiresAdmin → /; guestOnly → /
  [`router/index.ts:40`](../../client/src/router/index.ts#L40)

- RouteMeta augmentation that makes meta fields type-safe
  [`router/index.ts:5`](../../client/src/router/index.ts#L5)

**App header — role display and role-conditional nav**

- isAdmin computed controls Users link; watch closes menu on route change; logout clears auth then pushes /login
  [`App.vue:17`](../../client/src/App.vue#L17)

- Header conditional on isAuthenticated; burger toggle; nav structure
  [`App.vue:2`](../../client/src/App.vue#L2)

**Admin stub route**

- New /admin/users route with requiresAuth + requiresAdmin meta
  [`router/index.ts:30`](../../client/src/router/index.ts#L30)

- Stub page (Epic 4 populates content)
  [`AdminUsersView.vue:1`](../../client/src/views/AdminUsersView.vue#L1)

**Tests**

- 4 guard behaviour tests: unauthenticated → /login; auth on guestOnly → /; patient on admin → /; admin on admin → stays
  [`router/__tests__/index.test.ts:1`](../../client/src/router/__tests__/index.test.ts#L1)
