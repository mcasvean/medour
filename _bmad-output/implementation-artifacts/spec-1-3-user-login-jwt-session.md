---
title: "User Login & JWT Session"
type: "feature"
created: "2026-08-13"
status: "done"
review_loop_iteration: 0
baseline_commit: "fe2947916bfe3eec8b8e08122e9bc11117a08d23"
context: []
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** There is no login endpoint and no JWT validation on protected routes. The client auth state is broken after a forced 401 (Pinia store stays stale), `isAuthenticated` can diverge from the token, there is no logout action, and a page refresh wipes `authStore.user` even when a valid token is in localStorage.

**Approach:** Add `POST /api/v1/auth/login`, a `JwtAuthFilter` that validates Bearer tokens and populates Spring Security context, update `SecurityConfig` to require authentication on all non-public routes, and harden `authStore` (computed `isAuthenticated`, `clearAuth()` action, user persisted to localStorage for page-refresh rehydration).

## Boundaries & Constraints

**Always:**

- Wrong email, wrong password, and soft-deleted user MUST all return exactly `{ "error": "Invalid credentials" }` with HTTP 401 — never distinguish between failure modes (prevents user enumeration)
- `/api/v1/health`, `POST /api/v1/auth/register`, `POST /api/v1/auth/login` are public; every other endpoint requires a valid JWT
- `SecurityContextHolder` is populated with `UsernamePasswordAuthenticationToken(userId, null, [SimpleGrantedAuthority("ROLE_" + role)])` so Spring Security role guards work in Story 1.4
- `AuthResponse` gains `Boolean mustChangePassword`; the client stores it in `authStore.user` but Story 1.3 takes no action on it (forced rotation is Story 1.6)
- `isAuthenticated` MUST be a Pinia getter (`!!state.token`) — never plain writable state
- On 401 intercept: call `authStore.clearAuth()` — not just `localStorage.clear()` — so Pinia and localStorage stay in sync
- `User` object is serialised to `localStorage` key `auth_user` in `setAuth()`; restored in store initialisation to survive page refresh
- `JwtAuthFilter` returns 401 immediately when an `Authorization` header is present but the token is invalid/expired; it proceeds without setting context when no header is present

**Ask First:**

- (none)

**Never:**

- No `/me` endpoint in this story — user rehydration is localStorage-only
- `mustChangePassword=true` does NOT trigger a redirect in this story
- No plaintext password or `passwordHash` in any API response

## I/O & Edge-Case Matrix

| Scenario                                | Input / State                                                 | Expected Output / Behavior                                         | Error Handling              |
| --------------------------------------- | ------------------------------------------------------------- | ------------------------------------------------------------------ | --------------------------- |
| Valid login                             | `POST /auth/login` correct email + password, `deletedAt=null` | 200 + `AuthResponse` with token, user fields, `mustChangePassword` | N/A                         |
| Wrong password                          | `POST /auth/login` valid email, wrong password                | 401 `{ "error": "Invalid credentials" }`                           | Same body for all 401 cases |
| Unknown email                           | `POST /auth/login` email not in DB                            | 401 `{ "error": "Invalid credentials" }`                           | Same body                   |
| Soft-deleted user                       | `POST /auth/login` email where `deletedAt != null`            | 401 `{ "error": "Invalid credentials" }`                           | Same body                   |
| Protected route — valid token           | Any non-public endpoint with valid `Authorization: Bearer`    | Spring Security grants access                                      | N/A                         |
| Protected route — invalid/expired token | `Authorization` header present, token invalid                 | Filter returns 401 before reaching controller                      | N/A                         |
| Protected route — no token              | No `Authorization` header                                     | Spring Security returns 401                                        | N/A                         |
| Page refresh                            | Token + user JSON in localStorage                             | `authStore.token` and `.user` rehydrated; `isAuthenticated` true   | N/A                         |

</frozen-after-approval>

## Code Map

- `server/src/main/java/com/medour/security/JwtUtil.java:18` -- add `parseToken(String token)` returning `Jws<Claims>` (throws `JwtException` on failure); used by `JwtAuthFilter`
- `server/src/main/java/com/medour/security/JwtAuthFilter.java` -- NEW `@Component OncePerRequestFilter`; injects `JwtUtil`; reads `Authorization: Bearer` header; on valid token sets `UsernamePasswordAuthenticationToken` in `SecurityContextHolder`; on invalid token sends 401; on missing header proceeds
- `server/src/main/java/com/medour/config/SecurityConfig.java:22` -- inject `JwtAuthFilter`; change `anyRequest().permitAll()` → public/protected split; add `addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)`
- `server/src/main/java/com/medour/dto/LoginRequest.java` -- NEW DTO: `@NotBlank @Email email`, `@NotBlank password`
- `server/src/main/java/com/medour/dto/AuthResponse.java:3` -- add `Boolean mustChangePassword` component to the record; update all existing construction sites
- `server/src/main/java/com/medour/exception/InvalidCredentialsException.java` -- NEW `RuntimeException`; mapped to 401
- `server/src/main/java/com/medour/exception/GlobalExceptionHandler.java:14` -- add `@ExceptionHandler(InvalidCredentialsException.class)` → 401 `{ "error": "Invalid credentials" }`
- `server/src/main/java/com/medour/service/UserService.java:75` -- add `@Transactional(readOnly=true) login(LoginRequest req)`: find by email → missing/deleted/bad-password all throw `InvalidCredentialsException`; return `AuthResponse` including `user.getMustChangePassword()`
- `server/src/main/java/com/medour/controller/AuthController.java:24` -- add `POST /login` calling `userService.login()` → 200 `AuthResponse`
- `server/src/test/java/com/medour/controller/AuthControllerTest.java:40` -- add login tests; update existing `AuthResponse(...)` constructors to include `false` for `mustChangePassword`
- `client/src/stores/authStore.ts:1` -- add `mustChangePassword` to `User`; remove `isAuthenticated` from state; add `getters.isAuthenticated`; update `setAuth` to persist `auth_user` JSON; add `clearAuth()`; restore user from `auth_user` in initial state
- `client/src/api/index.ts:23` -- replace `localStorage.clear()` with `useAuthStore().clearAuth()` in the 401 handler (call `useAuthStore()` lazily inside the handler)
- `client/src/router/index.ts:8` -- add `/login` route
- `client/src/views/LoginView.vue` -- NEW: email + password refs, loading flag; `POST /auth/login`; on success `authStore.setAuth()` → `router.push('/')`; on 401 show "Invalid credentials"; disable button while loading

## Tasks & Acceptance

**Execution:**

- [x] `server/src/main/java/com/medour/security/JwtUtil.java` -- add `public Jws<Claims> parseToken(String token)` using `Jwts.parser().verifyWith(key).build().parseSignedClaims(token)` where `key = Keys.hmacShaKeyFor(secret.getBytes(UTF_8))`; throws `JwtException` if expired or tampered
- [x] `server/src/main/java/com/medour/security/JwtAuthFilter.java` -- NEW `@Component`; `doFilterInternal`: extract header; if null proceed; call `jwtUtil.parseToken()`; on `JwtException` write 401 and return; on success extract `sub` (principal) and `role` claim, call `SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(sub, null, List.of(new SimpleGrantedAuthority("ROLE_" + role))))`; then proceed
- [x] `server/src/main/java/com/medour/config/SecurityConfig.java` -- inject `JwtAuthFilter`; replace `anyRequest().permitAll()` with `.requestMatchers(GET, "/api/v1/health").permitAll()`, `.requestMatchers(POST, "/api/v1/auth/register", "/api/v1/auth/login").permitAll()`, `.anyRequest().authenticated()`; add `.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)`; add `http.exceptionHandling(e -> e.authenticationEntryPoint((req, res, ex) -> res.sendError(SC_UNAUTHORIZED)))` so missing-token protected routes return 401 not 403
- [x] `server/src/main/java/com/medour/dto/LoginRequest.java` -- NEW: `@Data @NoArgsConstructor @AllArgsConstructor`; `@NotBlank @Email String email`; `@NotBlank String password`
- [x] `server/src/main/java/com/medour/dto/AuthResponse.java` -- add `Boolean mustChangePassword` as last record component; fix `UserService.register()` to pass `saved.getMustChangePassword()` as the final argument
- [x] `server/src/main/java/com/medour/exception/InvalidCredentialsException.java` -- NEW `public class InvalidCredentialsException extends RuntimeException {}`
- [x] `server/src/main/java/com/medour/exception/GlobalExceptionHandler.java` -- add handler returning `ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid credentials"))`
- [x] `server/src/main/java/com/medour/service/UserService.java` -- add `@Transactional(readOnly = true) public AuthResponse login(LoginRequest req)`: find by email (absent → throw); check `user.getDeletedAt() != null` → throw; `!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())` → throw; return `new AuthResponse(token, id, email, firstName, surname, role, mustChangePassword)`
- [x] `server/src/main/java/com/medour/controller/AuthController.java` -- add `@PostMapping("/login") public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req)` calling `userService.login(req)`; returns 200
- [x] `server/src/test/java/com/medour/controller/AuthControllerTest.java` -- update existing `new AuthResponse(...)` calls to include `false`; add 4 login tests: valid creds → 200 + token field; wrong-password service throw → 401 + error message; unknown-email service throw → 401; mock soft-deleted throw → 401
- [x] `client/src/stores/authStore.ts` -- add `mustChangePassword: boolean` to `User` interface; restructure store to `defineStore('auth', { state, getters: { isAuthenticated: (s) => !!s.token }, actions: { setAuth, clearAuth } })`; `setAuth` calls `localStorage.setItem('auth_user', JSON.stringify(user))`; `clearAuth` calls `localStorage.removeItem('token')` + `removeItem('auth_user')` and nulls state; initial state restores user from `localStorage.getItem('auth_user')`
- [x] `client/src/api/index.ts` -- in the 401 handler import and lazily call `useAuthStore()`; replace `localStorage.clear(); router.push('/login')` with `useAuthStore().clearAuth(); router.push('/login')`
- [x] `client/src/router/index.ts` -- add `{ path: '/login', component: () => import('../views/LoginView.vue') }`
- [x] `client/src/views/LoginView.vue` -- NEW: `email`, `password`, `errorMessage`, `loading` refs; form submits to `api.post('/auth/login', { email, password })`; on success map `res.data` to `User` shape and call `authStore.setAuth(res.data.token, user)` then `router.push('/')`; on 401 set `errorMessage = 'Invalid credentials'`; button disabled while `loading`

**Acceptance Criteria:**

- Given the `/login` route, when email and correct password are submitted, then a JWT is stored in localStorage, `authStore.isAuthenticated` is true, `authStore.user` is populated, and the browser navigates to `/`.
- Given the same session after a page refresh, then `authStore.user` is restored from localStorage and `isAuthenticated` remains true.
- Given wrong credentials, when login is submitted, then a 401 is returned and the form shows "Invalid credentials" with no navigation.
- Given a soft-deleted account, when login is submitted, then the response is identical to wrong credentials — 401 with the same error body.
- Given a valid JWT on a protected route, when the request is made, then `JwtAuthFilter` populates `SecurityContextHolder` and the controller is reached.
- Given an expired/tampered token, when a protected route is requested, then the filter returns 401 before the controller is invoked.
- Given a forced 401 on any authenticated request, then `authStore.clearAuth()` is called, both localStorage keys are removed, the Pinia store is nulled, and the router redirects to `/login`.

## Design Notes

`isAuthenticated` is a getter rather than state to prevent the state-divergence bug where `localStorage.clear()` in the interceptor leaves `isAuthenticated=true` in the running Pinia store instance. A getter always reflects the token truthfully.

All three login failure modes throw the same `InvalidCredentialsException` so the response body is identical regardless of which check failed — this prevents user enumeration via differing error messages or timing (standard OWASP A07).

`JwtAuthFilter` sends 401 immediately when a bearer token is present but invalid (rather than clearing context and continuing) so the caller gets explicit feedback that their token is bad, as distinct from having no token at all.

## Verification

**Commands:**

- `cd server && ./mvnw test` -- expected: all tests pass (existing 8 + new 4 login controller tests)
- `cd client && npm run build` -- expected: zero TypeScript errors

## Spec Change Log

**Review loop 1 patches applied:**

- `authStore.ts` — `JSON.parse` wrapped in `loadStoredUser()` with try/catch + `{ mustChangePassword: false, ...parsed }` spread default; avoids app-crash on corrupt localStorage and undefined-field on stale data
- `AuthResponse.java` — `Boolean` changed to primitive `boolean`; avoids nullable boxed type for a required flag
- `AuthControllerTest.java` — added `$.mustChangePassword` assertion to `login_validCredentials_returns200WithToken`
- `UserServiceTest.java` — added `login_softDeletedUser_throwsInvalidCredentials` to cover the service-layer deleted_at guard
- `api/__tests__/index.test.ts` — added `setActivePinia(createPinia())` in `beforeEach` of the 401 interceptor describe block; fixes broken test caused by `useAuthStore()` requiring an active Pinia instance
- `api/index.ts` — restored explanatory comment about the concurrent-401 gate

## Suggested Review Order

**JWT filter — security entry point**

- Filter extracts Bearer header, calls parseToken, populates SecurityContextHolder or returns 401
  [`JwtAuthFilter.java:30`](../../server/src/main/java/com/medour/security/JwtAuthFilter.java#L30)

- parseToken validates signature and expiry, throws JwtException on failure
  [`JwtUtil.java:29`](../../server/src/main/java/com/medour/security/JwtUtil.java#L29)

**Security config — public/protected split**

- anyRequest().authenticated() replaces permitAll; authenticationEntryPoint returns 401 (not 403) for missing token
  [`SecurityConfig.java:35`](../../server/src/main/java/com/medour/config/SecurityConfig.java#L35)

**Login service — credential validation**

- find by email → deleted_at check → password match; all failures throw same exception (prevents user enumeration)
  [`UserService.java:74`](../../server/src/main/java/com/medour/service/UserService.java#L74)

**Client auth store — state hardening**

- loadStoredUser wraps JSON.parse in try/catch and spreads mustChangePassword default for stale data
  [`authStore.ts:12`](../../client/src/stores/authStore.ts#L12)

- isAuthenticated is a getter; clearAuth nulls state and removes both localStorage keys
  [`authStore.ts:23`](../../client/src/stores/authStore.ts#L23)

**401 interceptor — Pinia-aware auth clearing**

- useAuthStore().clearAuth() replaces localStorage.clear() so Pinia and localStorage stay in sync
  [`api/index.ts:25`](../../client/src/api/index.ts#L25)

**Login view**

- Explicit field mapping from response to User shape; loading flag disables button; 401 shows inline error
  [`LoginView.vue:36`](../../client/src/views/LoginView.vue#L36)

**Contract change**

- AuthResponse gains boolean mustChangePassword as seventh component; all callers updated
  [`AuthResponse.java:3`](../../server/src/main/java/com/medour/dto/AuthResponse.java#L3)

**Tests**

- Controller slice: 4 login tests + mustChangePassword assertion on success path
  [`AuthControllerTest.java:86`](../../server/src/test/java/com/medour/controller/AuthControllerTest.java#L86)

- Service test: soft-delete guard verified at service layer without mocking
  [`UserServiceTest.java:44`](../../server/src/test/java/com/medour/service/UserServiceTest.java#L44)

- API interceptor test: Pinia instance set up so clearAuth() is exercised end-to-end
  [`index.test.ts:49`](../../client/src/api/__tests__/index.test.ts#L49)
