---
title: "User Registration (Patient & Doctor)"
type: "feature"
created: "2026-08-12"
status: "done"
review_loop_iteration: 0
baseline_commit: "2f447f96061b2b4b0fdfa062c616eb3e750d5cc1"
context: []
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** No user accounts exist; there is no way to create a patient or doctor account, and the admin seed account is absent on first boot.

**Approach:** Create the `users` table (JPA entity), registration endpoint (`POST /api/v1/auth/register`) handling both patient and doctor roles, JWT issuance on success, and a startup seeder for the admin account. Build the Vue registration flow: role-selection page → role-specific form → on success update `authStore` and navigate home.

## Boundaries & Constraints

**Always:**
- Passwords stored as BCrypt hash only — `PasswordEncoder` bean must be `BCryptPasswordEncoder`
- `county` and `speciality` are required for `DOCTOR`; must be rejected with an error if absent
- Duplicate email returns 409 — no account is created, no password info leaked
- JWT generated at registration (auto-login); same `JwtUtil` will be reused in Story 1.3
- JWT claims: `sub` = user ID, `email`, `role`; 1-hour expiry; signed HS256 with `${jwt.secret}`
- `users.deleted_at` column present from day one even though no soft-delete logic runs in this story
- `users.must_change_password` present, defaults to `false`
- Admin seed runs on every startup; is idempotent (skips if admin already exists)
- All HTTP calls through `client/src/api/index.ts` — no direct `fetch`
- On successful registration the client calls `authStore.setAuth(token, user)` before navigating

**Ask First:**
- If any required field should also be validated server-side beyond null/empty checks (e.g. email format, password strength)

**Never:**
- No login endpoint in this story (Story 1.3)
- No JWT validation filter — SecurityConfig still permits all requests (Story 1.3 adds the filter)
- No route guards (Story 1.4)
- Never return `passwordHash` or any credential in an API response

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|---|---|---|---|
| Valid patient registration | `POST /api/v1/auth/register` with `role=PATIENT` + all base fields | 200 + `AuthResponse` (token, user); `PATIENT` user created in DB | N/A |
| Valid doctor registration | Same with `role=DOCTOR` + `county` + `speciality` | 200 + `AuthResponse`; `DOCTOR` user created | N/A |
| Duplicate email | `POST` with already-registered email | 409 + `{ error: "Email already in use" }` | No user created; no password info in response |
| Admin seed on startup | Server starts; no admin in DB | Admin account created from `application.yml` creds | Idempotent — second startup does nothing |

</frozen-after-approval>

## Code Map

- `server/src/main/java/com/medour/config/SecurityConfig.java:18` -- add `PasswordEncoder` @Bean here
- `server/src/main/resources/application.yml` -- add `jwt.secret` and `admin.*` properties
- `client/src/stores/authStore.ts` -- add `User` interface + `setAuth` action
- `client/src/router/index.ts` -- add `/register` route

## Tasks & Acceptance

**Execution:**
- [x] `server/src/main/java/com/medour/model/Role.java` -- create enum: `PATIENT`, `DOCTOR`, `ADMIN`
- [x] `server/src/main/java/com/medour/model/User.java` -- create `@Entity @Table(name="users")`; fields: `id` (IDENTITY PK), `email` (unique), `passwordHash`, `firstName`, `surname`, `age` (Integer), `gender`, `city`, `address`, `county` (nullable), `speciality` (nullable), `role` (@Enumerated STRING), `deletedAt` (LocalDateTime nullable), `mustChangePassword` (Boolean, default false); use Lombok `@Data @Builder @NoArgsConstructor @AllArgsConstructor`
- [x] `server/src/main/java/com/medour/repository/UserRepository.java` -- interface extending `JpaRepository<User, Long>`; method `Optional<User> findByEmail(String email)`
- [x] `server/src/main/java/com/medour/dto/RegisterRequest.java` -- class with fields: `email`, `password`, `firstName`, `surname`, `age` (Integer), `gender`, `city`, `address`, `county`, `speciality`, `role` (String — "PATIENT" or "DOCTOR"); Lombok `@Data @NoArgsConstructor @AllArgsConstructor`; `@NotBlank` / `@Email` / `@NotNull` on mandatory base fields
- [x] `server/src/main/java/com/medour/dto/AuthResponse.java` -- class/record: `token`, `id` (Long), `email`, `firstName`, `surname`, `role` (String)
- [x] `server/src/main/java/com/medour/security/JwtUtil.java` -- `@Component`; inject `@Value("${jwt.secret}") String secret`; `generateToken(User user)` builds JJWT 0.12.x token: sub=id, claims email+role, issuedAt now, expiration +3600s, signed `Keys.hmacShaKeyFor(secret.getBytes(UTF_8))`
- [x] `server/src/main/java/com/medour/service/UserService.java` -- `@Service`; inject `UserRepository`, `PasswordEncoder`, `JwtUtil`; `@Transactional register(RegisterRequest req)` → validate email unique (throw `EmailAlreadyUsedException` if not), validate doctor fields, hash password, save, return `AuthResponse`; `seedAdmin(email, rawPassword)` — if no `ADMIN` exists, create and save one
- [x] `server/src/main/java/com/medour/controller/AuthController.java` -- `@RestController @RequestMapping("/api/v1/auth")`; `POST /register` with `@Valid @RequestBody RegisterRequest` → `UserService.register()` → 200 `AuthResponse`
- [x] `server/src/main/java/com/medour/exception/EmailAlreadyUsedException.java` -- `RuntimeException`; mapped to 409 via `@ControllerAdvice` handler or `@ResponseStatus`
- [x] `server/src/main/java/com/medour/config/DataSeeder.java` -- `@Component implements CommandLineRunner`; inject `UserService`, `@Value("${admin.email}")`, `@Value("${admin.password}")`; `run()` calls `userService.seedAdmin(email, password)`
- [x] `server/src/main/java/com/medour/config/SecurityConfig.java` -- add `@Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }`
- [x] `server/src/main/resources/application.yml` -- add: `jwt.secret: ${JWT_SECRET:medour-jwt-secret-dev-only-change-in-prod}`, `admin.email: ${ADMIN_EMAIL:admin@medour.com}`, `admin.password: ${ADMIN_PASSWORD:Admin1234!}`
- [x] `client/src/stores/authStore.ts` -- add `User` interface (id, email, firstName, surname, role); replace generic `user` type; add `setAuth(token: string, user: User)` action: sets state + `localStorage.setItem('token', token)`
- [x] `client/src/router/index.ts` -- add `{ path: '/register', component: () => import('../views/RegisterView.vue') }`
- [x] `client/src/views/RegisterView.vue` -- `selectedRole` ref (null | 'PATIENT' | 'DOCTOR'); if null show two buttons "Register as Patient" / "Register as Doctor"; if set render `PatientRegisterForm` or `DoctorRegisterForm`; on form `@submit(payload)` call `api.post('/auth/register', payload)`, on success `authStore.setAuth(res.data.token, res.data)` then `router.push('/')`; on 409 show "Email already in use" error
- [x] `client/src/views/PatientRegisterForm.vue` -- form fields: email, password, firstName, surname, age, gender, city, address; emits `submit` with `{ ...fields, role: 'PATIENT' }`; shows server error prop if passed
- [x] `client/src/views/DoctorRegisterForm.vue` -- all patient fields plus county, speciality; emits `submit` with `{ ...fields, role: 'DOCTOR' }`
- [x] `server/src/test/java/com/medour/controller/AuthControllerTest.java` -- `@WebMvcTest(AuthController.class)` with `@MockBean UserService`; test: valid patient request → 200 + token; valid doctor → 200; duplicate email → 409; missing doctor fields → 400
- [x] `server/src/test/java/com/medour/config/DataSeederTest.java` -- plain JUnit + Mockito; verify `userService.seedAdmin()` called with configured credentials (matrix row 4)

**Acceptance Criteria:**
- Given the `/register` page, when "Register as Patient" is clicked, the patient form with base fields is shown.
- Given the `/register` page, when "Register as Doctor" is clicked, the doctor form with base + county + speciality fields is shown.
- Given a valid patient form submission, the server creates a `PATIENT` user, returns a JWT, the client stores it in `authStore` and localStorage, and navigates to `/`.
- Given a valid doctor form submission, the server creates a `DOCTOR` user.
- Given a duplicate email, the server returns 409 and the client shows "Email already in use" with no user created.
- Given server startup with no admin in the database, an admin account is created from the configured credentials.
- Given a second startup, no duplicate admin is created.

## Verification

**Commands:**
- `cd server && ./mvnw test` -- expected: 8 tests pass (4 AuthControllerTest + 1 HealthControllerTest + 1 DataSeederTest + 1 JwtUtilTest + 1 UserServiceTest)
- `cd client && npm run build` -- expected: zero TS errors

## Suggested Review Order

**Registration security (entry point)**

- Role.valueOf + ADMIN self-registration guard + doctor field validation
  [`UserService.java:30`](../../server/src/main/java/com/medour/service/UserService.java#L30)

- Global exception handler — 409 for duplicate email, 400 for validation errors
  [`GlobalExceptionHandler.java:1`](../../server/src/main/java/com/medour/exception/GlobalExceptionHandler.java#L1)

**Data model**

- User entity — all fields, unique email, deleted_at, must_change_password
  [`User.java:1`](../../server/src/main/java/com/medour/model/User.java#L1)

- RegisterRequest DTO — Bean Validation constraints including @Min(1)/@Max(150) on age
  [`RegisterRequest.java:1`](../../server/src/main/java/com/medour/dto/RegisterRequest.java#L1)

**JWT generation**

- HS256 token — sub=id, role claim, 1h expiry
  [`JwtUtil.java:18`](../../server/src/main/java/com/medour/security/JwtUtil.java#L18)

**Admin seed**

- Idempotent admin creation on startup
  [`DataSeeder.java:1`](../../server/src/main/java/com/medour/config/DataSeeder.java#L1)

- seedAdmin early-return guard when ADMIN already exists
  [`UserService.java:65`](../../server/src/main/java/com/medour/service/UserService.java#L65)

**Client registration flow**

- Role selector → form → API call → setAuth → navigate home; 409 + generic error handling
  [`RegisterView.vue:1`](../../client/src/views/RegisterView.vue#L1)

- authStore: User interface + setAuth action
  [`authStore.ts:1`](../../client/src/stores/authStore.ts#L1)

**Tests**

- JWT claims round-trip test
  [`JwtUtilTest.java:1`](../../server/src/test/java/com/medour/security/JwtUtilTest.java#L1)

- seedAdmin idempotency (no save when admin exists)
  [`UserServiceTest.java:1`](../../server/src/test/java/com/medour/service/UserServiceTest.java#L1)

- Controller slice tests — 201, 409, 400 cases
  [`AuthControllerTest.java:1`](../../server/src/test/java/com/medour/controller/AuthControllerTest.java#L1)
