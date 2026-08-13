# Deferred Work

Items collected during implementation reviews that are real but out of scope for the originating story.

- source_spec: `spec-1-1-project-scaffold-development-environment.md`
  summary: No /login route — router.push('/login') in the 401 interceptor silently no-ops until Story 1.3 adds the route.
  evidence: Edge case hunter finding; router has only the placeholder '/' route in this scaffold.

- source_spec: `spec-1-1-project-scaffold-development-environment.md`
  summary: authStore stays stale (isAuthenticated=true, token populated) after a forced 401 logout because the interceptor only clears localStorage, not the Pinia store.
  evidence: Verification gap finding; the SPA store instance survives navigation so components and guards will see stale auth state until reload or until Story 1.3 redesigns the auth flow.

- source_spec: `spec-1-1-project-scaffold-development-environment.md`
  summary: HealthControllerTest uses @AutoConfigureMockMvc(addFilters=false) so it never validates the assembled SecurityFilterChain.
  evidence: Verification gap finding; a change from anyRequest().permitAll() to anyRequest().authenticated() would not be caught by this test.

- source_spec: `spec-1-1-project-scaffold-development-environment.md`
  summary: spring.jpa.hibernate.ddl-auto is set to 'update', which mutates schema on every boot; Flyway or Liquibase should own migrations.
  evidence: Blind hunter finding; ddl-auto:update will silently drop renamed columns and cause schema drift across environments.

- source_spec: `spec-1-1-project-scaffold-development-environment.md`
  summary: No PasswordEncoder @Bean in SecurityConfig; will throw NoSuchBeanDefinitionException once UserDetailsService is introduced.
  evidence: Blind hunter finding; JJWT and Spring Security auth beans are wired in Story 1.3.

- source_spec: `spec-1-1-project-scaffold-development-environment.md`
  summary: CORS allowed origin is hardcoded to http://localhost:5173 — all cross-origin requests rejected in non-local deployments.
  evidence: Edge case hunter finding; should be externalized to an environment property before any staging or production deployment.

- source_spec: `spec-1-1-project-scaffold-development-environment.md`
  summary: vitest.config.ts is missing the '@/' path alias; any future unit test that uses @/ imports will fail to resolve.
  evidence: Blind hunter finding; no current test uses @/ so it is not broken today.

- source_spec: `spec-1-1-project-scaffold-development-environment.md`
  summary: Concurrent 401 dedup unit test is unreliable due to mock-adapter's async response scheduling; needs an integration test.
  evidence: The localStorage-based gate is correct in production (synchronous clear is visible immediately) but two mock responses arrive in separate event-loop ticks, so each interceptor sees the token as non-null. Requires an e2e or integration test to verify.

- source_spec: `spec-1-2-user-registration-patient-doctor.md`
  summary: isAuthenticated is plain state, not a derived getter — can diverge from token.
  evidence: Blind hunter finding; should become a Pinia getter in Story 1.3 when auth state ownership is fully established.

- source_spec: `spec-1-2-user-registration-patient-doctor.md`
  summary: authStore does not persist user to localStorage — page refresh sets user back to null.
  evidence: Verification gap; any component reading authStore.user after refresh gets null even though isAuthenticated is true. Story 1.3 will add a /me endpoint or token-parsing to rehydrate user.

- source_spec: `spec-1-2-user-registration-patient-doctor.md`
  summary: No clearAuth/logout action in authStore — logout will be ad-hoc until Story 1.3.
  evidence: Blind hunter finding; Story 1.3 will introduce proper session termination.

- source_spec: `spec-1-2-user-registration-patient-doctor.md`
  summary: Token expiry is not checked at store init — an expired token in localStorage makes isAuthenticated true until the first 401.
  evidence: Blind hunter finding; Story 1.3 JWT validation filter and auth guard will handle this.

- source_spec: `spec-1-2-user-registration-patient-doctor.md`
  summary: password has no minimum-length constraint on either client or server.
  evidence: Blind hunter / edge case hunter; flagged as Ask First in spec — deliberately deferred until password policy is defined.

- source_spec: `spec-1-2-user-registration-patient-doctor.md`
  summary: Admin seeded with mustChangePassword=false using the well-known default password — privileged account has no forced rotation in dev.
  evidence: Blind hunter; production deployments must set ADMIN_PASSWORD env var; noted in application.yml comments.

- source_spec: `spec-1-2-user-registration-patient-doctor.md`
  summary: No submit-button loading/disabled state — double-click can submit duplicate registration requests.
  evidence: Blind hunter; UX improvement not in spec scope.

- source_spec: `spec-1-2-user-registration-patient-doctor.md`
  summary: setAuth-before-push ordering is untested — a future refactor could invert them, breaking navigation guards.
  evidence: Verification gap; ordering is correct today but Story 1.4 navigation guards will make this observable and testable.

- source_spec: `spec-1-3-user-login-jwt-session.md`
  summary: JwtAuthFilter is added to the security chain but every existing test bypasses it with addFilters=false — the filter's token-to-SecurityContext path has no integration test.
  evidence: Verification gap; a regression that breaks role claim extraction or JwtException handling would not be caught by the controller slice tests.

- source_spec: `spec-1-3-user-login-jwt-session.md`
  summary: A soft-deleted user who already holds a valid JWT can access protected routes until their token expires (up to 1 hour) — JwtAuthFilter does not check deleted_at on every request.
  evidence: Edge case hunter; a DB check on every request is expensive; the 1-hour window is the accepted trade-off, but it should be documented as a known gap.

- source_spec: `spec-1-3-user-login-jwt-session.md`
  summary: clearAuth() resets only authStore — sibling stores (appointmentStore, doctorStore, userStore) retain stale user-scoped data after logout.
  evidence: Blind hunter; full logout cleanup is within scope of Story 1.5 or a dedicated logout story.

- source_spec: `spec-1-3-user-login-jwt-session.md`
  summary: No accessDeniedHandler configured in SecurityConfig — a 403 (authenticated but unauthorized role) returns Spring's default HTML error page, inconsistent with the custom JSON 401 format.
  evidence: Blind hunter; role-based access control becomes active in Story 1.4, at which point an accessDeniedHandler returning JSON should be added.
